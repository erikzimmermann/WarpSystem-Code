package de.codingair.warpsystem.spigot.base.utils;

import de.codingair.warpsystem.spigot.base.WarpSystem;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Notifier {
    public static void notifyPlayers(Player player) {
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                notifyPlayers(p);
            }
        } else {
            if (player.hasPermission(Permissions.PERMISSION_NOTIFY) && WarpSystem.updateAvailable) {
                TextComponent tc0 = new TextComponent(Lang.getPrefix() + "§7A new update is available §8[§b" + WarpSystem.getInstance().getUpdateNotifier().getUpdateInfo() + "§8]§7. Download it §7»");
                TextComponent click = new TextComponent("§chere");
                TextComponent tc1 = new TextComponent("§7«!");

                click.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, WarpSystem.getInstance().getUpdateNotifier().getDownload()));
                click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {new TextComponent("§7»Click«")}));

                tc0.addExtra(click);
                tc0.addExtra(tc1);
                tc0.setColor(net.md_5.bungee.api.ChatColor.GRAY);

                player.sendMessage("");
                player.sendMessage("");
                player.spigot().sendMessage(tc0);
                player.sendMessage("");
            }

        }
    }
}
