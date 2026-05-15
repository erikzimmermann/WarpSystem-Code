package de.codingair.warpsystem.spigot.base.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.codingair.codingapi.server.AsyncCatcher;
import de.codingair.codingapi.server.events.PlayerWalkEvent;
import de.codingair.codingapi.tools.Location;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.managers.TeleportManager;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportUtils;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.adapters.EmptyAdapter;
import de.codingair.warpsystem.spigot.base.utils.teleport.process.Teleport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TeleportListener implements Listener {
    public static final HashMap<Player, org.bukkit.Location> TELEPORTS = new HashMap<>();
    private static final Cache<String, TeleportData> teleport = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    public static CompletableFuture<org.bukkit.Location> setSpawnPositionOrTeleport(String name, TeleportOptions options) {
        if (options == null) return CompletableFuture.completedFuture(null);
        if (options.getSkip() == null) options.setSkip(true);
        Player player = Bukkit.getPlayer(name);

        if (player != null && player.isOnline()) {
            //teleport
            org.bukkit.Location l = player.getLocation();
            AsyncCatcher.runSync(WarpSystem.getInstance(), () -> WarpSystem.getInstance().getTeleportManager().teleport(player, options, true), l);
            return CompletableFuture.completedFuture(l);
        } else {
            TeleportData data = new TeleportData(options);
            teleport.put(name.toLowerCase(), data);
            return data.getSpawnPosition();
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e) {
        org.bukkit.Location loc = TELEPORTS.remove(e.getPlayer());

        if (loc != null && loc.equals(e.getTo())) {
            e.setCancelled(false);
            e.setTo(loc);
        }
    }

    @EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(PlayerJoinEvent e) {
        TeleportData data = teleport.getIfPresent(e.getPlayer().getName().toLowerCase());

        if (data != null) {
            teleport.invalidate(e.getPlayer().getName().toLowerCase());
            TeleportOptions options = data.getOptions();

            options.setCanMove(true);
            options.setSilent(true);
            options.setSkip(true);

            Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> WarpSystem.getInstance().getTeleportManager().teleport(e.getPlayer(), options), 2L);
        }
    }

    public void onSpawn(PlayerSpawnLocationEvent e) {
        try {
            TeleportData data = teleport.getIfPresent(e.getPlayer().getName().toLowerCase());

            if (data != null) {
                teleport.invalidate(e.getPlayer().getName().toLowerCase());
                TeleportOptions options = data.getOptions();
                org.bukkit.Location l = TeleportUtils.prepareLocation(options.buildLocation(), e.getPlayer(), true).get(1, TimeUnit.SECONDS);

                if (l == null || l.getWorld() == null) {
                    String world = l instanceof Location ? ((Location) l).getWorldName() : null;
                    Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> e.getPlayer().sendMessage(new String[] {" ", Lang.getPrefix() + "§4World " + (world == null ? "" : "'" + world + "' ") + "is missing. Please contact an admin!", " "}), 2L);
                    return;
                }

                if (l.getYaw() == -420 && l.getPitch() == -420) {
                    org.bukkit.Location p = e.getPlayer().getLocation();
                    l.setYaw(p.getYaw());
                    l.setPitch(p.getPitch());
                }

                e.setSpawnLocation(l);

                options.setCanMove(true);
                options.setSilent(true);
                options.setSkip(true);
                options.setDestination(new Destination(new EmptyAdapter()));

                Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> WarpSystem.getInstance().getTeleportManager().teleport(e.getPlayer(), options), 2L);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onMove(PlayerWalkEvent e) {
        Player p = e.getPlayer();

        Teleport t = TeleportManager.getInstance().getTeleport(p);
        if (t == null || t.isCanMove()) return;

        double diff = Math.abs(e.getFrom().getX() - e.getTo().getX()) + Math.abs(e.getFrom().getZ() - e.getTo().getZ());
        double diffY = Math.abs(e.getFrom().getY() - e.getTo().getY());

        boolean inLiquid = p.getLocation().getBlock().isLiquid() || p.getLocation().subtract(0, 1, 0).getBlock().isLiquid();
        if (inLiquid) diffY = 0;

        if (diff + diffY > 0.01) WarpSystem.getInstance().getTeleportManager().cancelTeleport(p);
    }

    @EventHandler
    public void onTeleportDuringTeleportationProcess(PlayerTeleportEvent e) {
        Player p = e.getPlayer();

        Teleport t = TeleportManager.getInstance().getTeleport(p);
        if (t == null || t.isCanMove()) return;
        Location target = t.getDestination().buildLocation();

        // Do not check ender pearl or chorus fruit teleports
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
            || e.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }

        if (target == null) {
            //we gonna switch the server -> cancel the teleport
            e.setCancelled(true);
        } else if (!target.equals(e.getTo())) {
            //not the same teleport -> cancel
            e.setCancelled(true);
        }
    }

    private static class TeleportData {
        private final TeleportOptions options;
        private final CompletableFuture<org.bukkit.Location> spawnPosition = new CompletableFuture<>();

        public TeleportData(TeleportOptions options) {
            this.options = options;
        }

        public TeleportOptions getOptions() {
            return options;
        }

        public CompletableFuture<org.bukkit.Location> getSpawnPosition() {
            return spawnPosition;
        }
    }
}
