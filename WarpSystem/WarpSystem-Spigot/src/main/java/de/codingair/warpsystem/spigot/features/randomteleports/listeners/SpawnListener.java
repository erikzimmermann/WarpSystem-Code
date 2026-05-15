package de.codingair.warpsystem.spigot.features.randomteleports.listeners;

import de.codingair.codingapi.server.AsyncCatcher;
import de.codingair.codingapi.tools.Callback;
import de.codingair.warpsystem.core.transfer.packets.spigot.QueueRTPUsagePacket;
import de.codingair.warpsystem.core.transfer.packets.spigot.RandomTPPacket;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.teleport.Origin;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.adapters.EmptyAdapter;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.adapters.LocationAdapter;
import de.codingair.warpsystem.spigot.features.randomteleports.managers.RandomTeleportManager;
import de.codingair.warpsystem.spigot.features.randomteleports.utils.RandomLocationCalculator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class SpawnListener implements Listener {
    private final HashMap<String, TeleportInfo> teleporting = new HashMap<>();
    private final HashMap<String, RandomLocationCalculator> teleportLocation = new HashMap<>();

    public SpawnListener() {
        WarpSystem.getDataHandler().registerHandler(RandomTPPacket.class, (packet, proxy, connection, direction) -> {
            World w = Bukkit.getWorld(packet.getWorld());

            Player player = Bukkit.getPlayer(packet.getPlayer());
            TeleportInfo teleportInfo = new TeleportInfo(w, packet.getServer(), packet.isByOther());

            if (player == null) {
                teleporting.put(packet.getPlayer(), teleportInfo);
            } else triggerRTP(player, teleportInfo);
        });
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        TeleportInfo teleportInfo = teleporting.get(e.getName());
        if (teleportInfo == null) return;

        search(null, teleportInfo, (c, warning) -> {
            if (c == null || c.getResult() == null) {
                if (warning == null) warning = Lang.getPrefix() + Lang.get("RandomTP_No_Location_Found");
                e.setKickMessage(warning);
                e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            } else teleportLocation.put(e.getName(), c);

            synchronized (teleportInfo) {
                teleportInfo.notify();
            }
        });

        // wait until search position has been found
        synchronized (teleportInfo) {
            try {
                teleportInfo.wait();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void onSpawn(PlayerSpawnLocationEvent e) {
        // runs before PlayerJoinEvent

        TeleportInfo teleportInfo = teleporting.get(e.getPlayer().getName());
        if (teleportInfo != null) {
            RandomLocationCalculator c = teleportLocation.remove(e.getPlayer().getName());

            if (c != null && c.getResult() != null) {
                c.applyUnsafePlayer(e.getPlayer());
                boolean isProtected = c.isProtected(c.getResult());

                if (isProtected) return;

                // increase RTP usage and register cooldown
                Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> {
                    WarpSystem.getDataHandler().send(new QueueRTPUsagePacket(WarpSystem.getInstance().getPlayerDataManager().get(e.getPlayer()), teleportInfo.getServer()), e.getPlayer());
                    if (!teleportInfo.isByOther()) WarpSystem.cooldown().register(e.getPlayer(), Origin.RandomTP);
                }, 20);

                teleporting.remove(e.getPlayer().getName());

                e.setSpawnLocation(c.getResult());

                // send message and particles
                Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> {
                    TeleportOptions options = new TeleportOptions(new Destination(new EmptyAdapter()), "");
                    options.setMessage(Lang.getPrefix() + Lang.get("RandomTP_Teleported"));
                    options.setSkip(true);

                    WarpSystem.getInstance().getTeleportManager().teleport(e.getPlayer(), options);
                }, 2);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        TeleportInfo teleportInfo = teleporting.remove(e.getPlayer().getName());
        if (teleportInfo != null) triggerRTP(e.getPlayer(), teleportInfo);
    }

    private void search(@Nullable Player player, TeleportInfo teleportInfo, BiConsumer<RandomLocationCalculator, String> callback) {
        if (player == null) player = Bukkit.getOnlinePlayers().stream().findAny().orElse(null);
        if (player == null) {
            callback.accept(null, Lang.getPrefix() + Lang.get("RandomTP_No_Location_Found"));
            return;
        }

        if (teleportInfo.getWorld() != null) {
            RandomLocationCalculator c = RandomTeleportManager.getInstance().newCalculator(null, teleportInfo.getWorld(), new Callback<RandomLocationCalculator>() {
                @Override
                public void accept(RandomLocationCalculator c) {
                    callback.accept(c, null);
                }
            });

            Bukkit.getScheduler().runTaskAsynchronously(WarpSystem.getInstance(), c);
        } else {
            callback.accept(null, Lang.getPrefix() + Lang.get("World_Not_Exists"));
        }
    }

    private void triggerRTP(Player player, TeleportInfo teleportInfo) {
        if (teleportInfo.getWorld() != null) {
            search(player, teleportInfo, (c, warning) -> {
                if (c == null || c.getResult() == null) {
                    if (warning == null) warning = Lang.getPrefix() + Lang.get("RandomTP_No_Location_Found");
                    player.sendMessage(warning);
                } else {
                    Bukkit.getScheduler().runTask(WarpSystem.getInstance(), () -> {
                        WarpSystem.getDataHandler().send(new QueueRTPUsagePacket(WarpSystem.getInstance().getPlayerDataManager().get(player), teleportInfo.getServer()), player);
                        if (!teleportInfo.isByOther()) WarpSystem.cooldown().register(player, Origin.RandomTP);

                        TeleportOptions options = new TeleportOptions(new Destination(new LocationAdapter(c.getResult())), "");
                        options.setMessage(Lang.getPrefix() + Lang.get("RandomTP_Teleported"));
                        options.setSkip(true);

                        WarpSystem.getInstance().getTeleportManager().teleport(player, options);
                    });
                }
            });
        } else {
            teleporting.remove(player.getName());
            AsyncCatcher.runSync(WarpSystem.getInstance(), () -> player.sendMessage(Lang.getPrefix() + Lang.get("World_Not_Exists")), player.getLocation());
        }
    }

    private static class TeleportInfo {
        private final World world;
        private final String server;
        private final boolean byOther; //triggered by other person (admin) -> no cooldown

        public TeleportInfo(World world, String server, boolean byOther) {
            this.world = world;
            this.server = server;
            this.byOther = byOther;
        }

        public World getWorld() {
            return world;
        }

        public String getServer() {
            return server;
        }

        public boolean isByOther() {
            return byOther;
        }
    }
}
