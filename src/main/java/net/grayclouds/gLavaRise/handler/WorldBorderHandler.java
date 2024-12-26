package net.grayclouds.gLavaRise.handler;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;

public class WorldBorderHandler {
    private static Plugin plugin;
    private static BukkitRunnable shrinkTask;
    private static int initialSize;
    private static int shrinkAmount;
    private static int minimumSize;
    private static String shrinkMethod;
    private static int timeInterval;
    private static int playersThreshold;
    private static boolean shrinkEnabled;
    
    public static void init(Plugin p) {
        plugin = p;
        // Initialize with default values for OVERWORLD
        loadConfig(plugin.getServer().getWorlds().get(0));
    }

    private static void loadConfig(World world) {
        FileConfiguration config = plugin.getConfig();
        String worldType = getWorldType(world);
        String basePath = "CONFIG.WORLDS." + worldType + ".BORDER";
        
        initialSize = config.getInt(basePath + ".initial-size", 250);
        shrinkEnabled = config.getBoolean(basePath + ".shrink-enabled", true);
        shrinkAmount = config.getInt(basePath + ".shrink.shrink-amount", 50);
        minimumSize = config.getInt(basePath + ".shrink.minimum-size", 50);
        shrinkMethod = config.getString(basePath + ".shrink.method", "TIME");
        timeInterval = config.getInt(basePath + ".shrink.time-interval", 300);
        playersThreshold = config.getInt(basePath + ".shrink.players-threshold", 5);
    }

    private static String getWorldType(World world) {
        switch (world.getEnvironment()) {
            case NETHER:
                return "NETHER";
            case THE_END:
                return "END";
            default:
                return "OVERWORLD";
        }
    }

    public static void setupWorldBorder(World world, Location center) {
        if (world == null || center == null) return;

        // Load config for this specific world
        loadConfig(world);
        
        // Validate sizes
        if (initialSize <= 0) {
            initialSize = 250; // Default fallback
        }
        if (minimumSize <= 0) {
            minimumSize = 50; // Default fallback
        }
        if (shrinkAmount <= 0) {
            shrinkAmount = 50; // Default fallback
        }

        // Ensure minimum is not larger than initial
        if (minimumSize > initialSize) {
            minimumSize = initialSize;
        }

        WorldBorder border = world.getWorldBorder();
        
        // Set border size (no need to multiply by 2)
        border.setSize(initialSize);  // Remove the * 2
        border.setCenter(center);
        border.setWarningDistance(10);
        border.setWarningTime(5);

        // Only start shrinking if enabled
        if (shrinkEnabled) {
            startBorderShrinking(world);
        }
    }

    private static void startBorderShrinking(World world) {
        if (shrinkTask != null) {
            shrinkTask.cancel();
        }

        if (shrinkMethod.equalsIgnoreCase("TIME")) {
            shrinkTask = new BukkitRunnable() {
                @Override
                public void run() {
                    shrinkBorder(world);
                }
            };
            shrinkTask.runTaskTimer(plugin, timeInterval * 20L, timeInterval * 20L);
        } else if (shrinkMethod.equalsIgnoreCase("PLAYERS")) {
            // Check player count every 20 ticks (1 second)
            shrinkTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (world.getPlayers().size() <= playersThreshold) {
                        shrinkBorder(world);
                    }
                }
            };
            shrinkTask.runTaskTimer(plugin, 20L, 20L);
        }
    }

    private static void shrinkBorder(World world) {
        WorldBorder border = world.getWorldBorder();
        double currentSize = border.getSize();
        double newSize = Math.max(minimumSize, currentSize - shrinkAmount);  // Remove the * 2
        
        if (newSize < currentSize) {
            if (newSize <= minimumSize) {  // Remove the * 2
                String minMsg = plugin.getConfig().getString("CONFIG.MESSAGES.border-minimum", "The border has reached its minimum size!");
                world.getPlayers().forEach(p -> p.sendMessage(minMsg));
                return;
            }

            String shrinkMsg = shrinkMethod.equalsIgnoreCase("PLAYERS") 
                ? plugin.getConfig().getString("CONFIG.MESSAGES.border-shrink-players", "Player count threshold reached! The border is shrinking!")
                : plugin.getConfig().getString("CONFIG.MESSAGES.border-shrink", "The border is shrinking!");
            
            world.getPlayers().forEach(p -> p.sendMessage(shrinkMsg));
            border.setSize(newSize, 10);
        }
    }

    public static void stop() {
        if (shrinkTask != null) {
            shrinkTask.cancel();
            shrinkTask = null;
        }
    }

    public static double getBorderSize(World world) {
        return world.getWorldBorder().getSize();
    }

    public static Location getCenter(World world) {
        return world.getWorldBorder().getCenter();
    }
}
