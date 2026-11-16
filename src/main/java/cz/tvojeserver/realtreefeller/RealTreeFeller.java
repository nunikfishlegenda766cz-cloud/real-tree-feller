package cz.tvojeserver.realtreefeller;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RealTreeFeller extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("RealTreeFeller byl uspesne zapnut!");
    }

    private boolean isLog(Material mat) {
        String name = mat.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.contains("STEM") || name.contains("HYPHAE");
    }

    private boolean isLeaf(Material mat) {
        String name = mat.name();
        return name.endsWith("_LEAVES") || name.contains("WART_BLOCK") || name.equals(Material.SHROOMLIGHT);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!isLog(block.getType())) return;

        // Ověříme, zda hráč drží sekeru
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.getType().name().endsWith("_AXE")) return;

        // Najdeme celý kmen a listy spojené se stromem
        Set<Block> logs = new HashSet<>();
        Set<Block> leaves = new HashSet<>();
        findTree(block, logs, leaves);

        if (logs.size() < 3) return; // Je to moc malý kousek (nebo keř)

        event.setCancelled(true); // Zrušíme normální rozbití jednoho bloku

        // Seřadíme kmen odshora dolů, aby padal realisticky shora
        List<Block> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort((b1, b2) -> Integer.compare(b2.getY(), b1.getY()));

        // Spustíme animaci padání a kácení
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index < sortedLogs.size()) {
                    Block logBlock = sortedLogs.get(index);
                    Material type = logBlock.getType();
                    
                    // Vytvoříme padající blok (FallingBlock), který padá dolů k zemi
                    logBlock.setType(Material.AIR);
                    logBlock.getWorld().spawnFallingBlock(logBlock.getLocation().add(0.5, 0, 0.5), type.createBlockData());
                    logBlock.getWorld().playSound(logBlock.getLocation(), Sound.BLOCK_WOOD_BREAK, 1f, 0.8f);

                    index++;
                } else {
                    // Když spadnou všechny kmeny, zničíme i listy a dáme hráči suroviny
                    for (Block leaf : leaves) {
                        leaf.breakNaturally(item);
                    }
                    for (Block log : logs) {
                        log.getWorld().dropItemNaturally(log.getLocation(), new ItemStack(log.getType()));
                    }
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 2L); // Každých 20 milisekund (2 tiky) spadně další kus kmene
    }

    private void findTree(Block start, Set<Block> logs, Set<Block> leaves) {
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        logs.add(start);

        while (!queue.isEmpty() && logs.size() < 150) { // Limit pro obří stromy
            Block current = queue.poll();

            // Prohledáme okolí 3x3x3 pro kmen a širší okolí pro listy
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) { // Strom roste hlavně nahoru a do stran
                    for (int z = -1; z <= 1; z++) {
                        Block relative = current.getRelative(x, y, z);

                        if (isLog(relative.getType()) && !logs.contains(relative)) {
                            logs.add(relative);
                            queue.add(relative);
                        } else if (isLeaf(relative.getType()) && !leaves.contains(relative)) {
                            leaves.add(relative);
                        }
                    }
                }
            }
        }
    }
}
