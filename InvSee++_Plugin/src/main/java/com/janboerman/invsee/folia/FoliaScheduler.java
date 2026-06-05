package com.janboerman.invsee.folia;

import com.janboerman.invsee.spigot.InvseePlusPlus;
import com.janboerman.invsee.spigot.api.Scheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler implementation based on {@link io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler},
 * {@link io.papermc.paper.threadedregions.scheduler.AsyncScheduler} and {@link EntityScheduler}.
 *
 * <p>Folia imposes the following constraints that this class handles transparently:</p>
 * <ul>
 *   <li>{@code runDelayed} and {@code runAtFixedRate} require a delay &gt;= 1 tick.
 *       A delay of 0 is redirected to {@code execute()} or substituted with 1.</li>
 *   <li>The old {@link org.bukkit.scheduler.BukkitScheduler} API is unsupported on Folia;
 *       this class uses the Folia-native schedulers instead.</li>
 * </ul>
 */
public class FoliaScheduler implements Scheduler {

    private final InvseePlusPlus plugin;

    public FoliaScheduler(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void executeSyncPlayer(UUID playerId, Runnable task, Runnable retired) {
        Server server = plugin.getServer();
        Player player = server.getPlayer(playerId);
        if (player != null) {
            EntityScheduler scheduler = player.getScheduler();
            scheduler.run(plugin, scheduledTask -> task.run(), retired);
        } else {
            executeSyncGlobal(task);
        }
    }

    public void executeSyncPlayer(HumanEntity player, Runnable task, Runnable retired) {
        player.getScheduler().run(plugin, scheduledTask -> task.run(), retired);
    }

    @Override
    public void executeSyncGlobal(Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void executeSyncGlobalRepeatedly(Runnable task, long ticksInitialDelay, long ticksPeriod) {
        // Folia requires initialDelayTicks >= 1. If the caller passes 0, run the task immediately
        // and then start the repeating schedule with a period-sized initial delay.
        if (ticksInitialDelay <= 0) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), ticksPeriod, ticksPeriod);
        } else {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), ticksInitialDelay, ticksPeriod);
        }
    }

    @Override
    public void executeAsync(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public void executeLaterGlobal(Runnable task, long delayTicks) {
        // Folia requires delayTicks >= 1. A delay of 0 maps to an immediate execution.
        if (delayTicks <= 0) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
        } else {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
        }
    }

    @Override
    public void executeLaterAsync(Runnable task, long delayTicks) {
        if (delayTicks <= 0) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            // Each Minecraft tick is nominally 50 ms.
            plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks * 50L, TimeUnit.MILLISECONDS);
        }
    }
}
