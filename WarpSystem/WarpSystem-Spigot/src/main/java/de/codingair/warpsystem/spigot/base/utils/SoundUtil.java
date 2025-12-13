package de.codingair.warpsystem.spigot.base.utils;

import de.codingair.codingapi.server.sounds.SoundData;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SoundUtil {
    public static void play(Player player, SoundData data) {
        if (player == null || data == null) return;

        // Try codingapi play first
        try {
            data.play(player);
        } catch (Throwable ex) {
            // ignore, try fallback
            try {
                WarpSystem.getInstance().getLogger().warning("SoundData.play failed for " + (data.getSound() == null ? "null" : data.getSound().name()) + ": " + ex.getMessage());
            } catch (Throwable ex2) {
                ex2.printStackTrace();
            }
        }

        // Fallback: try Bukkit string-based playSound (allows namespaced/normalized sounds)
        try {
            Location loc = player.getLocation();
            if (data.getSound() != null) {
                String name = data.getSound().name();

                // Try raw name
                try {
                    player.playSound(loc, name, data.getVolume(), data.getPitch());
                    return;
                } catch (Throwable ignored) {}

                // Try lower-case and dot notation
                try {
                    String alt = name.toLowerCase().replace('_', '.');
                    player.playSound(loc, alt, data.getVolume(), data.getPitch());
                    return;
                } catch (Throwable ignored) {}

                // Try namespace prefix
                try {
                    String ns = "minecraft:" + name.toLowerCase().replace('_', '.');
                    player.playSound(loc, ns, data.getVolume(), data.getPitch());
                    return;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ex) {
            try {
                String soundName = (data.getSound() == null ? "null" : data.getSound().name());
                WarpSystem.getInstance().getLogger().warning("All fallback sound play attempts failed for " + soundName + ": " + ex.getMessage());
            } catch (Throwable logEx) {
                // If logging fails, do nothing to avoid further issues
            }
        }
    }
}

