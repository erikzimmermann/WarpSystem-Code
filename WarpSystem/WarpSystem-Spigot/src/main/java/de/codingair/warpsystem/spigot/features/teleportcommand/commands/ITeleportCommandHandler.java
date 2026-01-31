package de.codingair.warpsystem.spigot.features.teleportcommand.commands;

import de.codingair.warpsystem.core.transfer.utils.PlayerData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ITeleportCommandHandler {
    void back(Player player);

    void back(CommandSender sender, String player);

    void suggestBack(String[] args, List<String> suggestions);

    boolean tp(Player gate, PlayerData player, @Nullable Double x, @Nullable Double y, @Nullable Double z, @Nullable Float yaw, @Nullable Float pitch, @Nullable String server, @Nullable String world);

    boolean tp(Player gate, PlayerData player, @Nullable Double x, @Nullable Double y, @Nullable Double z, @Nullable Float yaw, @Nullable Float pitch, @Nullable String server, @Nullable String world, boolean notifyPlayer);

    void tp(Player gate, PlayerData player, PlayerData target);

    void tp(Player gate, PlayerData player, PlayerData target, boolean notifyPlayer);

    List<String> suggestTp(String[] args, List<String> suggestions);

    List<String> suggestTpTo(String[] args, List<String> suggestions);

    List<String> suggestTpHere(CommandSender sender, String[] args, List<String> suggestions);

    void tpAll(Player player, int alreadyHandled, int alreadySent);

    void tpa(Player player, String argument, Player other, boolean tpToSender);

    void suggestTpa(Player player, String[] args, List<String> suggestions, boolean tpToSender);

    void tpaAll(Player player);
}
