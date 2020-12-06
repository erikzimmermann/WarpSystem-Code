package de.codingair.warpsystem.spigot.features.warps.commands;

import de.codingair.codingapi.player.gui.inventory.v2.exceptions.AlreadyOpenedException;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.IsWaitingException;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.NoPageException;
import de.codingair.codingapi.server.commands.builder.BaseComponent;
import de.codingair.codingapi.server.commands.builder.CommandComponent;
import de.codingair.codingapi.server.commands.builder.special.MultiCommandComponent;
import de.codingair.codingapi.server.sounds.Sound;
import de.codingair.warpsystem.spigot.api.WSCommandBuilder;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.language.Lang;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.warps.guis.GWarps;
import de.codingair.warpsystem.spigot.features.warps.managers.IconManager;
import de.codingair.warpsystem.spigot.features.warps.nextlevel.utils.Icon;
import de.codingair.warpsystem.spigot.nitrado.warpgui.WarpPanel;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CWarps extends WSCommandBuilder {
    public CWarps() {
        super("Warps", new BaseComponent(WarpSystem.PERMISSION_USE_WARP_GUI) {
            @Override
            public void noPermission(CommandSender sender, String label, CommandComponent child) {
                sender.sendMessage(Lang.getPrefix() + Lang.get("No_Permission"));
            }

            @Override
            public void onlyFor(boolean player, CommandSender sender, String label, CommandComponent child) {
                sender.sendMessage(Lang.getPrefix() + Lang.get("Only_For_Players"));
            }

            @Override
            public void unknownSubCommand(CommandSender sender, String label, String[] args) {
                run(sender, null);
            }

            @Override
            public boolean runCommand(CommandSender sender, String label, String[] args) {
                run(sender, null);
                return false;
            }
        }.setOnlyPlayers(true));

    }

    public static void run(CommandSender sender, Icon category) {
        Player p = (Player) sender;

        if(!WarpSystem.activated) return;

        try {
            new WarpPanel(p).open();
        } catch(AlreadyOpenedException | NoPageException | IsWaitingException ignored) {
        }
    }
}
