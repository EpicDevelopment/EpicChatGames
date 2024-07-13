package gg.minecrush.reactions.async;

import gg.minecrush.reactions.ReactionManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class AutomaticEvents {

    private final ReactionManager reactionManager;
    private final JavaPlugin plugin;

    private int automaticReactionsInterval;
    private BukkitTask currentTask;

    private static final String[] REACTION_TYPES = {"math", "scramble", "fastest"};

    public AutomaticEvents(ReactionManager reactionManager, JavaPlugin plugin) {
        this.reactionManager = reactionManager;
        this.plugin = plugin;
        loadConfig();
        scheduleAutomaticReactions();
    }

    private void loadConfig() {
        automaticReactionsInterval = plugin.getConfig().getInt("automaticReactionsInterval", 3);
    }

    public synchronized void scheduleAutomaticReactions() {
        cancelCurrentTask();
        plugin.getLogger().info("Scheduling new automatic reaction task.");
        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!reactionManager.isReactionActive()) {
                    Random random = new Random();
                    String type = REACTION_TYPES[random.nextInt(REACTION_TYPES.length)];
                    reactionManager.startReaction(type);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 60 * 20, 20 * automaticReactionsInterval);
    }

    public synchronized void cancelCurrentTask() {
        if (currentTask != null) {
            plugin.getLogger().info("Cancelling current automatic announcements task.");
            try {
                currentTask.cancel();
                plugin.getLogger().info("Current task cancelled successfully.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to cancel automatic announcements task: " + e.getMessage());
            } finally {
                currentTask = null;
            }
        } else {
            plugin.getLogger().info("No automatic announcements task found to cancel.");
        }
    }

    public synchronized void reload() {
        plugin.getLogger().info("Reloading announcements and configuration.");
        loadConfig();
        scheduleAutomaticReactions();
    }
}
