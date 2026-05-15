package de.codingair.warpsystem.spigot.features.spawn.listeners;

import de.codingair.warpsystem.core.transfer.packets.general.TeleportSpawnPacket;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.features.spawn.managers.SpawnManager;
import de.codingair.warpsystem.spigot.features.spawn.utils.Spawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class SpawnListener implements Listener {
    public void onSpawn(PlayerSpawnLocationEvent e) {
        Spawn spawn = SpawnManager.getInstance().getSpawn();
        if (spawn != null) {
            boolean b = spawn.getUsage() == Spawn.Usage.EVERY_JOIN || spawn.getUsage() == Spawn.Usage.LOCAL_EVERY_JOIN || spawn.getUsage() == Spawn.Usage.GLOBAL_EVERY_JOIN || spawn.getUsage() == Spawn.Usage.GLOBAL_EVERY_PROXY_JOIN;

            if (!e.getPlayer().hasPlayedBefore()) {
                if (b || spawn.getUsage() == Spawn.Usage.FIRST_JOIN || spawn.getUsage() == Spawn.Usage.LOCAL_FIRST_JOIN || spawn.getUsage() == Spawn.Usage.GLOBAL_FIRST_JOIN) {
                    spawn.onJoin(e, true);
                }
            } else if (b) spawn.onJoin(e, false);
        }
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onDeath(PlayerRespawnEvent e) {
        if (WarpSystem.getInstance().isProxyConnected()) {
            String respawn = SpawnManager.getInstance().getRespawnServerCommand();
            if (respawn != null && !respawn.equals(WarpSystem.getInstance().getCurrentServer())) {
                Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> WarpSystem.getDataHandler().send(new TeleportSpawnPacket(e.getPlayer().getName(), true), e.getPlayer()), 2L);
                return;
            }
        }

        Spawn spawn = SpawnManager.getInstance().getSpawn();
        if (spawn != null && spawn.isValid() && spawn.getRespawnUsage() != Spawn.RespawnUsage.DISABLED) {
            Location l = spawn.getLocation();
            if (l != null && l.getWorld() != null) e.setRespawnLocation(spawn.getLocation());
        }
    }
}
