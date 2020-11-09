package de.codingair.warpsystem.spigot.base.managers;

import de.codingair.warpsystem.spigot.api.events.PlayerFinalJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class UUIDManager {

    public UUID get(Player player) {
        return player.getUniqueId();
    }

    public UUIDListener listener() {
        return new UUIDListener();
    }

    public class UUIDListener implements Listener {
        private UUIDListener() {
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onJoin(PlayerJoinEvent e) {
            UUID id = get(e.getPlayer());
            if(id != null) Bukkit.getPluginManager().callEvent(new PlayerFinalJoinEvent(e.getPlayer(), id, true));
        }
    }
}
