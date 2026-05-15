package de.codingair.warpsystem.spigot.base.managers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.codingair.codingapi.player.MessageAPI;
import de.codingair.codingapi.tools.Callback;
import de.codingair.warpsystem.api.ITeleportManager;
import de.codingair.warpsystem.api.Options;
import de.codingair.warpsystem.api.TeleportService;
import de.codingair.warpsystem.api.destinations.IDestinationBuilder;
import de.codingair.warpsystem.api.destinations.utils.Result;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.DestinationBuilder;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.DestinationType;
import de.codingair.warpsystem.spigot.base.utils.teleport.process.Teleport;
import de.codingair.warpsystem.spigot.base.utils.teleport.process.TeleportDelay;
import de.codingair.warpsystem.spigot.features.globalwarps.managers.GlobalWarpManager;
import de.codingair.warpsystem.spigot.features.simplewarps.SimpleWarp;
import de.codingair.warpsystem.spigot.features.simplewarps.managers.SimpleWarpManager;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TeleportManager implements ITeleportManager {
    public static final String NO_PERMISSION = "%NO_PERMISSION%";
    private static TeleportManager instance;
    private Cache<Player, Teleport> teleports;

    private TeleportManager() {
    }

    public static TeleportManager getInstance() {
        if (instance == null) {
            instance = new TeleportManager();
            TeleportService.setIfAbsent(instance);
        }

        return instance;
    }

    /**
     * Have to be launched after the IconManager (see WarpSign.class - fromJSONString method - need warps and categories)
     */
    public boolean load() {
        this.teleports = CacheBuilder.newBuilder().expireAfterAccess(WarpSystem.opt().getTeleportDelay() + 5, TimeUnit.SECONDS).build();
        return true;
    }

    public void save() {
        WarpSystem.getInstance().getFileManager().getFile("Config").saveConfig();
    }

    @Override
    public synchronized @NotNull CompletableFuture<Result> teleport(@NotNull Player player, @NotNull Options options) {
        if (!(options instanceof TeleportOptions)) throw new IllegalArgumentException("Cannot use option class: " + options.getClass() + ". Please use the original API TeleportService.buildOptions() method to create your own.");

        CompletableFuture<Result> future = new CompletableFuture<>();
        options.addCallback(new Callback<Result>() {
            @Override
            public void accept(Result result) {
                future.complete(result);
            }
        });

        this.teleport(player, (TeleportOptions) options);
        return future;
    }

    public @NotNull Options options() {
        return new TeleportOptions();
    }

    @Override
    public @NotNull IDestinationBuilder destinationBuilder() {
        return new DestinationBuilder();
    }

    public synchronized void teleport(Player player, TeleportOptions options) {
        teleport(player, options, false);
    }

    public synchronized void teleport(Player player, TeleportOptions options, boolean force) {
        if (!force && isTeleporting(player)) {
            Teleport teleport = getTeleport(player);
            long diff = System.currentTimeMillis() - teleport.getStartTime();
            if (diff > 50)
                player.sendMessage(Lang.getPrefix() + Lang.get("Player_Is_Already_Teleporting"));
            return;
        }

        if (options.getOriginalDestination() == null) {
            player.sendMessage(Lang.getPrefix() + Lang.get("WARP_DOES_NOT_EXISTS"));
            return;
        }

        if ((options.getOriginalDestination().getType() == DestinationType.GlobalWarp || options.getOriginalDestination().getType() == DestinationType.Server) && !WarpSystem.getInstance().isProxyConnected()) {
            options.fireCallbacks(Result.NO_CONNECTED_PROXY);
            player.sendMessage(Lang.getPrefix() + Lang.get("Server_Is_Not_Online"));
            return;
        }

        options.addCallback(new Callback<Result>() {
            @Override
            public void accept(Result result) {
                teleports.invalidate(player);
            }
        });

        Teleport t = new Teleport(player, options);
        registerTeleport(player, t);
        t.start();
    }

    public synchronized void registerTeleport(Player player, Teleport t) {
        this.teleports.put(player, t);
    }

    public void invalidate(Player player) {
        this.teleports.invalidate(player);
    }

    public void cancelTeleport(Player player) {
        if (!isTeleporting(player)) return;
        Teleport teleport = getTeleport(player);

        teleport.cancel(Result.CANCELLED_BY_SYSTEM);
        invalidate(player);

        if (WarpSystem.getInstance().getFileManager().getFile("Config").getConfig().getBoolean("WarpSystem.Send.Teleport_Cancel_Message", true)) {
            if (WarpSystem.opt().getDelayDisplay() == TeleportDelay.Display.TITLE) MessageAPI.sendTitle(player, " ", " ", 0, 1, 0);
            MessageAPI.sendActionBar(player, Lang.get("Teleport_Cancelled"));
        }
    }

    public Teleport getTeleport(Player player) {
        Teleport t = teleports.getIfPresent(player);
        return t == null || t.expired() ? null : t;
    }

    public boolean isTeleporting(Player p) {
        return getTeleport(p) != null;
    }

    public Collection<Teleport> getTeleports() {
        return teleports == null ? new ArrayList<>() : teleports.asMap().values();
    }

    public void clear() {
        if (this.teleports != null) this.teleports.invalidateAll();
    }

    @Override
    public @NotNull @Unmodifiable Set<String> servers() {
        return Collections.unmodifiableSet(worlds().keySet());
    }

    @Override
    public @NotNull @Unmodifiable Map<String, Set<String>> worlds() {
        ServerManager man = WarpSystem.getInstance().getServerManager();
        return Collections.unmodifiableMap(man.getWorlds());
    }

    @Override
    public @NotNull @Unmodifiable Set<String> simpleWarps() {
        SimpleWarpManager man = SimpleWarpManager.getInstance();
        if (man == null) return Collections.emptySet();

        return Collections.unmodifiableSet(
                man.getWarps().values().stream()
                        .map(warp -> warp.getName(true))
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public @Nullable Location simpleWarp(@NotNull String id) {
        SimpleWarpManager man = SimpleWarpManager.getInstance();
        if (man == null) return null;

        SimpleWarp warp = man.getWarp(id);
        if (warp == null) return null;
        return warp.getLocation();
    }

    @Override
    public @NotNull @Unmodifiable Map<String, String> globalWarps() {
        GlobalWarpManager man = GlobalWarpManager.getInstance();
        if (man == null) return Collections.emptyMap();

        return Collections.unmodifiableMap(man.getGlobalWarps());
    }
}
