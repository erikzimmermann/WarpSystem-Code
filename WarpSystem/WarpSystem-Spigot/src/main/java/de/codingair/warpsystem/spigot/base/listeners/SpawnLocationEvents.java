package de.codingair.warpsystem.spigot.base.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.function.Consumer;

/**
 * Registers handlers for {@link PlayerSpawnLocationEvent}, preferring Paper's
 * {@code AsyncPlayerSpawnLocationEvent} subclass when available. Paper warns on
 * registrations against the parent event because the registration itself causes
 * the player to be created early; subscribing to the async subclass avoids that.
 * The handler body uses parent-class API only, so it works on plain Spigot too.
 */
public final class SpawnLocationEvents {
    private static final Class<? extends PlayerSpawnLocationEvent> EVENT_CLASS = resolve();

    private SpawnLocationEvents() {
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends PlayerSpawnLocationEvent> resolve() {
        try {
            return (Class<? extends PlayerSpawnLocationEvent>) Class.forName(
                    "com.destroystokyo.paper.event.player.AsyncPlayerSpawnLocationEvent");
        } catch (ClassNotFoundException ignored) {
            return PlayerSpawnLocationEvent.class;
        }
    }

    public static void register(Listener listener, JavaPlugin plugin, EventPriority priority,
                                boolean ignoreCancelled, Consumer<PlayerSpawnLocationEvent> handler) {
        Bukkit.getPluginManager().registerEvent(
                EVENT_CLASS, listener, priority,
                (l, event) -> handler.accept((PlayerSpawnLocationEvent) event),
                plugin, ignoreCancelled);
    }
}
