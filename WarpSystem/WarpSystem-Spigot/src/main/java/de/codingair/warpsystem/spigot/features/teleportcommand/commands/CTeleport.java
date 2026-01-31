package de.codingair.warpsystem.spigot.features.teleportcommand.commands;

import de.codingair.codingapi.server.commands.builder.BaseComponent;
import de.codingair.codingapi.server.commands.builder.CommandComponent;
import de.codingair.codingapi.tools.items.XMaterial;
import de.codingair.warpsystem.core.transfer.utils.PlayerData;
import de.codingair.warpsystem.spigot.api.WSCommandBuilder;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.Permissions;
import de.codingair.warpsystem.spigot.features.teleportcommand.TeleportCommandManager;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CTeleport extends WSCommandBuilder {
    public CTeleport() {
        super("Teleport", new BaseComponent() {
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
                sender.sendMessage(Lang.getPrefix() + WarpSystem.opt().cmdSug() + Lang.get("Use") + ": /tp <" + WarpSystem.opt().cmdArg() + "player" + WarpSystem.opt().cmdSug() + "> [" + WarpSystem.opt().cmdArg() + "player" + WarpSystem.opt().cmdSug() + "] §c" + Lang.get("Or") + " " + WarpSystem.opt().cmdSug() + "/tp [" + WarpSystem.opt().cmdArg() + "player" + WarpSystem.opt().cmdSug() + "] <" + WarpSystem.opt().cmdArg() + "x" + WarpSystem.opt().cmdSug() + "> <" + WarpSystem.opt().cmdArg() + "y" + WarpSystem.opt().cmdSug() + "> <" + WarpSystem.opt().cmdArg() + "z" + WarpSystem.opt().cmdSug() + ">");
            }

            @Override
            public boolean runCommand(CommandSender sender, String label, String[] args) {
                Player p = (Player) sender;

                if (Permissions.hasPermission(p, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TP)) {
                    List<String> argList = new ArrayList<>(Arrays.asList(args));
                    boolean silentMode = false;
                    if (argList.contains("-silent")) {
                        if (Permissions.hasPermission(p, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TP_SILENT)) {
                            silentMode = true;
                            argList.remove("-silent");
                        } else {
                            p.sendMessage(Lang.getPrefix() + Lang.get("No_Permission"));
                            return true;
                        }
                    }
                    args = argList.toArray(new String[0]);
                    if (!process(p, args, silentMode)) {
                        String bracket = WarpSystem.opt().cmdSug();
                        String arg = WarpSystem.opt().cmdArg();

                        // /tp [player] [<player> | [<x> <y> <z>] [<yaw> <pitch>] [<server> [world] | <world>]

                        TextComponent help = new TextComponent(Lang.getPrefix() + WarpSystem.opt().cmdSug() + Lang.get("Use") + ": " + bracket + "/tp <");

                        TextComponent add = new TextComponent(bracket + "[" + arg + "player" + bracket + "]");
                        add.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {
                                new TextComponent(bracket + "/tp <" + arg + "player" + bracket + ">")
                        }));

                        help.addExtra(add);
                        help.addExtra(bracket + " [");

                        add = new TextComponent(bracket + "<" + arg + "player" + bracket + ">");
                        add.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {
                                new TextComponent(bracket + "/tp <" + arg + "player" + bracket + "> <" + arg + "player" + bracket + ">")
                        }));

                        help.addExtra(add);
                        help.addExtra(bracket + " | [");

                        add = new TextComponent(bracket + "<" + arg + "x" + bracket + "> <" + arg + "y" + bracket + "> <" + arg + "z" + bracket + ">");
                        add.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {
                                new TextComponent(bracket + "/tp [" + arg + "player" + bracket + "] <" + arg + "x" + bracket + "> <" + arg + "y" + bracket + "> <" + arg + "z" + bracket + ">")
                        }));

                        help.addExtra(add);
                        help.addExtra(bracket + "] [");

                        add = new TextComponent(bracket + "<" + arg + "yaw" + bracket + "> <" + arg + "pitch" + bracket + ">");
                        add.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {
                                new TextComponent(bracket + "/tp [" + arg + "player" + bracket + "] [<" + arg + "x" + bracket + "> <" + arg + "y" + bracket + "> <" + arg + "z" + bracket + ">] <" + arg + "yaw" + bracket + "> <" + arg + "pitch" + bracket + ">")
                        }));

                        help.addExtra(add);
                        help.addExtra(bracket + "] [");

                        add = new TextComponent(bracket + "<" + arg + "server" + bracket + "> [" + arg + "world" + bracket + "]");
                        add.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {
                                new TextComponent(bracket + "/tp [" + arg + "player" + bracket + "] [<" + arg + "x" + bracket + "> <" + arg + "y" + bracket + "> <" + arg + "z" + bracket + ">] [<" + arg + "yaw" + bracket + "> <" + arg + "pitch" + bracket + ">] <" + arg + "server" + bracket + "> [" + arg + "world" + bracket + "]")
                        }));

                        help.addExtra(add);
                        help.addExtra(bracket + " | ");

                        add = new TextComponent(bracket + "<" + arg + "world" + bracket + ">");
                        add.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {
                                new TextComponent(bracket + "/tp [" + arg + "player" + bracket + "] [<" + arg + "x" + bracket + "> <" + arg + "y" + bracket + "> <" + arg + "z" + bracket + ">] [<" + arg + "yaw" + bracket + "> <" + arg + "pitch" + bracket + ">] <" + arg + "world" + bracket + ">")
                        }));

                        help.addExtra(add);
                        help.addExtra(bracket + "]]");

                        p.spigot().sendMessage(help);
                    }
                } else if (Permissions.hasPermission(p, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TPTO)) {
                    if (!processTpTo(p, args)) {
                        p.sendMessage(Lang.getPrefix() + WarpSystem.opt().cmdSug() + Lang.get("Use") + ": /tp <" + WarpSystem.opt().cmdArg() + "player" + WarpSystem.opt().cmdSug() + ">");
                    }
                } else noPermission(p, label, this);

                return true;
            }
        }.setOnlyPlayers(true), true);

        setMergeSpaceArguments(false);
        setOwnTabCompleter((commandSender, command, s, args) -> {
            if (Permissions.hasPermission(commandSender, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TP)) {
                if (commandSender instanceof Player) {
                    Player p = (Player) commandSender;
                    Block b = p.getTargetBlock(null, 10);
                    if (b.getType() == XMaterial.COMMAND_BLOCK.parseMaterial()) {
                        return new ArrayList<>();
                    }
                }

                return TeleportCommandManager.handler().suggestTp(args, new ArrayList<>());
            } else if (Permissions.hasPermission(commandSender, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TPTO)) {
                if (commandSender instanceof Player) {
                    Player p = (Player) commandSender;
                    Block b = p.getTargetBlock(null, 10);
                    if (b.getType() == XMaterial.COMMAND_BLOCK.parseMaterial()) {
                        return new ArrayList<>();
                    }
                }

                return TeleportCommandManager.handler().suggestTpTo(args, new ArrayList<>());
            } else return new ArrayList<>();

        });
    }

    private static boolean processTpTo(Player p, String[] args) {
        if (args.length == 0) return false;
        PlayerData data = WarpSystem.getInstance().getPlayerDataManager().getCache(args[0]);
        TeleportCommandManager.handler().tp(p, WarpSystem.getInstance().getPlayerDataManager().getCache(p), data);
        return true;
    }

    private static boolean process(Player p, String[] args, boolean silentMode) {
        if (args.length == 0) return false;

        String name = args[0];
        PlayerData data = null;

        if (name.length() > 1 || !name.equals("~")) {
            data = WarpSystem.getInstance().getPlayerDataManager().getCache(name);
        }

        if (args.length == 1 && data != null) {
            TeleportCommandManager.handler().tp(p, WarpSystem.getInstance().getPlayerDataManager().getCache(p), data, !silentMode);
            return true;
        }

        if (args.length - 1 >= 1) {
            String other = args[1];
            if (other.length() > 1 || !other.equals("~")) {
                //name
                PlayerData otherData = WarpSystem.getInstance().getPlayerDataManager().getCache(other);

                if (otherData != null) {
                    //player name
                    TeleportCommandManager.handler().tp(p, data, otherData, !silentMode);
                    return true;
                }
            }
        }

        return process(p, data, args, silentMode);
    }

    private static boolean process(Player p, PlayerData other, String[] args, boolean silentMode) {
        int i = 0;
        if (other != null) i++;

        Double x = null, y = null, z = null;

        if (args.length - 1 >= i) {
            //x
            x = parseDeep(args[i], p.getLocation().getX());

            if (x != null) {
                //y
                if (args.length - 1 >= i + 1) {
                    y = parseDeep(args[i + 1], p.getLocation().getY());
                    if (y == null) return false;
                } else return false;

                //z
                if (args.length - 1 >= i + 2) {
                    z = parseDeep(args[i + 2], p.getLocation().getZ());
                    if (z == null) {
                        //x and y might be yaw and pitch
                        x = null;
                        y = null;
                    }
                } else {
                    //x and y might be yaw and pitch
                    x = null;
                    y = null;
                }
            }
        }

        return process(p, other, x, y, z, args, silentMode);
    }

    private static boolean process(Player p, PlayerData other, Double x, Double y, Double z, String[] args, boolean silentMode) {
        int i = 0;
        if (other != null) i++;
        if (x != null) i += 3;

        Float yaw = null, pitch = null;

        //yaw
        if (args.length - 1 >= i) {
            yaw = parseDeep(args[i], p.getLocation().getYaw());

            if (yaw != null) {
                if (yaw > 180) yaw = 180F;
                else if (yaw < -180) yaw = -180F;

                //pitch
                if (args.length - 1 >= i + 1) {
                    pitch = parseDeep(args[i + 1], p.getLocation().getPitch());

                    if (pitch != null) {
                        if (pitch > 90) pitch = 90F;
                        else if (pitch < -90) pitch = -90F;
                    } else return false;
                } else return false;
            }
        }

        return process(p, other, x, y, z, yaw, pitch, args, silentMode);
    }

    private static boolean process(Player p, PlayerData other, Double x, Double y, Double z, Float yaw, Float pitch, String[] args, boolean silentMode) {
        int i = 0;
        if (other != null) i++;
        if (x != null) i += 3;
        if (yaw != null) i += 2;

        String world = null, server = null;

        if (args.length > i + 2) return false;

        if (args.length - 1 >= i + 1) {
            server = args[i];
            world = args[i + 1];

            boolean noServer = WarpSystem.getInstance().getServerManager().getProperties(server) == null;
            boolean noWorld = !WarpSystem.getInstance().getServerManager().getWorlds(server).contains(world);

            if (noServer && noWorld) {
                p.sendMessage(Lang.getPrefix() + Lang.get("Player_Server_Or_World_Not_Available"));
                return true;
            } else if (noServer) {
                p.sendMessage(Lang.getPrefix() + Lang.get("Server_Is_Not_Online"));
                return true;
            } else if (noWorld) {
                p.sendMessage(Lang.getPrefix() + Lang.get("World_Not_Exists"));
                return true;
            }
        } else if (args.length - 1 >= i) {
            String s = args[i];

            if (WarpSystem.getInstance().getServerManager().getProperties(s) != null) server = s;
            else {
                World w = Bukkit.getWorld(s);
                if (w != null) world = w.getName();
                else {
                    p.sendMessage(Lang.getPrefix() + Lang.get("Player_Server_Or_World_Not_Available"));
                    return true;
                }
            }
        }

        if (other == null) other = WarpSystem.getInstance().getPlayerDataManager().getCache(p);
        return TeleportCommandManager.handler().tp(p, other, x, y, z, yaw, pitch, server, world, !silentMode);
    }

    private static boolean isNumeric(String s) {
        return Pattern.matches("[-+]?\\d+([.,]\\d+)?", s);
    }

    private static double parse(String s) {
        if (s.isEmpty()) return 0;
        s = s.replace(",", ".");

        try {
            if (s.contains(".")) return Double.parseDouble(s);
            else return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static float parseFloat(String s) {
        if (s.isEmpty()) return 0;
        s = s.replace(",", ".");

        try {
            if (s.contains(".")) return Float.parseFloat(s);
            else return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Double parseDeep(String s, double current) {
        if (s.isEmpty()) return null;
        Double result = null;

        if (s.contains("~")) {
            result = current;
            s = s.replace("~", "");
        }

        if (isNumeric(s)) {
            if (result == null) result = parse(s);
            else result += parse(s);
        } else if (!s.isEmpty()) return null;

        return result;
    }

    private static Float parseDeep(String s, float current) {
        if (s.isEmpty()) return null;
        Float result = null;

        if (s.contains("~")) {
            result = current;
            s = s.replace("~", "");
        }

        if (isNumeric(s)) {
            if (result == null) result = parseFloat(s);
            else result += parseFloat(s);
        } else if (!s.isEmpty()) return null;

        return result;
    }
}
