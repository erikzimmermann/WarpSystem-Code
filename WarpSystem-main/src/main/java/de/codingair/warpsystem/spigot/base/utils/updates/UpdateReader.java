package de.codingair.warpsystem.spigot.base.utils.updates;

import de.codingair.codingapi.utils.Value;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Notifier;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class UpdateReader  {
    public static void start() {
        if(true) return; //NITRADO-EDITION

        Value<BukkitTask> task = new Value<>(null);
        Runnable runnable = () -> {
            WarpSystem.updateAvailable = WarpSystem.getInstance().getUpdateNotifier().read();

            if(WarpSystem.updateAvailable) {
                String v = WarpSystem.getInstance().getUpdateNotifier().getVersion();
                if(!v.startsWith("v")) v = "v" + v;

                WarpSystem.log("-----< WarpSystem >-----");
                WarpSystem.log("New update available [" + v + " - " + WarpSystem.getInstance().getUpdateNotifier().getUpdateInfo() + "].");
                WarpSystem.log("Download it on\n\n" + WarpSystem.getInstance().getUpdateNotifier().getDownload() + "\n");
                WarpSystem.log("------------------------");

                Notifier.notifyPlayers(null);
                task.getValue().cancel();
            }
        };

        task.setValue(Bukkit.getScheduler().runTaskTimerAsynchronously(WarpSystem.getInstance(), runnable, 20L * 5, 5 * 60 * 20L));
    }
}
