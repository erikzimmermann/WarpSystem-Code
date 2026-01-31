package de.codingair.warpsystem.spigot.versionfactory.handlers;

import de.codingair.codingapi.tools.Callback;
import de.codingair.codingapi.utils.ChatColor;
import de.codingair.warpsystem.core.transfer.packets.general.TeleportBackPacket;
import de.codingair.warpsystem.core.transfer.utils.PlayerData;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.Permissions;
import de.codingair.warpsystem.spigot.base.utils.teleport.Origin;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.adapters.LocationAdapter;
import de.codingair.warpsystem.spigot.features.teleportcommand.TeleportCommandManager;
import de.codingair.warpsystem.spigot.features.teleportcommand.commands.ITeleportCommandHandler;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Pattern;

public class TeleportCommandHandler implements ITeleportCommandHandler {
    public static Number cut(double n) {
        double d = Double.parseDouble(new DecimalFormat("#.##").format(n).replace(",", "."));
        if (d == (int) d) return (int) d;
        else return d;
    }

    @Override
    public void back(Player player) {
        if (WarpSystem.cooldown().checkPlayer(player, Origin.TeleportCommand)) return;

        TeleportCommandManager.getInstance().teleportToLastBackLocation(player, false, false, false).thenAccept(result -> {
            if (result == TeleportBackPacket.Result.NO_LAST_POSITION) player.sendMessage(Lang.getPrefix() + Lang.get("No_last_position_found"));
            else if (result == TeleportBackPacket.Result.PROTECTED_REGION) player.sendMessage(Lang.getPrefix() + Lang.get("Target_Protected_Area"));
            else WarpSystem.cooldown().register(player, Origin.TeleportCommand);
        });
    }

    @Override
    public void back(CommandSender sender, String player) {
        Player p = Bukkit.getPlayer(player);

        if (p == null) {
            if (WarpSystem.getInstance().getPlayerDataManager().getCache(player) != null) {
                TextComponent tc = new TextComponent(Lang.getPrefix() + "§7Teleporting on your entire BungeeCord is a §6premium feature§7!");
                tc.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                Lang.PREMIUM_CHAT(tc, sender, true);
            } else sender.sendMessage(Lang.getPrefix() + Lang.get("Player_is_not_online"));
        } else {
            if (sender instanceof Player && sender.equals(p)) {
                back(p);
                return;
            }

            TeleportCommandManager.getInstance().teleportToLastBackLocation(p, false, false, false).thenAccept(result -> {
                if (result == TeleportBackPacket.Result.NO_LAST_POSITION) sender.sendMessage(Lang.getPrefix() + Lang.get("No_last_position_found"));
                else if (result == TeleportBackPacket.Result.PROTECTED_REGION) sender.sendMessage(Lang.getPrefix() + Lang.get("Target_Protected_Area"));
                else {
                    WarpSystem.cooldown().register(p, Origin.TeleportCommand);
                    sender.sendMessage(Lang.getPrefix() + Lang.get("Teleported_Player_Info").replace("%player%", p.getName()).replace("%warp%", Lang.get("Last_Position")));
                }
            });
        }
    }

    @Override
    public void suggestBack(String[] args, List<String> suggestions) {
        WarpSystem.getInstance().getPlayerDataManager().getCached().forEach(p -> suggestions.add(p.getName()));
    }

    private boolean allNull(Object... o) {
        for (Object o1 : o) {
            if (o1 != null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean tp(Player gate, PlayerData player, @Nullable Double x, @Nullable Double y, @Nullable Double z, @Nullable Float yaw, @Nullable Float pitch, @Nullable String server, @Nullable String world) {
        return tp(gate, player, x, y, z, yaw, pitch, server, world, true);
    }

    @Override
    public boolean tp(Player gate, PlayerData player, @Nullable Double x, @Nullable Double y, @Nullable Double z, @Nullable Float yaw, @Nullable Float pitch, @Nullable String server, @Nullable String world, boolean notifyPlayer) {
        if (checkStatusTp(gate, player)) return true;
        Player p = Bukkit.getPlayer(player.getName());
        if(p == null) return false;

        if(server != null && world == null) world = server;
        if (allNull(x, y, z, yaw, pitch, world)) return false; //trigger usage

        if (gate != p && TeleportCommandManager.getInstance().deniesForceTps(p)) {
            gate.sendMessage(Lang.getPrefix() + Lang.get("Teleport_denied").replace("%PLAYER%", p.getName()));
            return true;
        }

        StringBuilder destination = new StringBuilder();

        World w;
        if (world != null) {
            w = Bukkit.getWorld(world);

            if (w == null) {
                gate.sendMessage(Lang.getPrefix() + Lang.get("World_Not_Exists"));
                return true;
            }

            if(w.equals(p.getWorld()) && allNull(x, y, z, yaw, pitch) || !w.equals(p.getWorld())) {
                destination.append(w.getName());
            }
        } else w = gate.getWorld();

        Location l = new Location(w, 0, 0, 0);

        if (x != null && y != null && z != null) {
            l.setX(x);
            l.setY(y);
            l.setZ(z);

            if (destination.length() > 0) destination.append(", ");
            destination.append("x: ").append(cut(x)).append(", y: ").append(cut(y)).append(", z: ").append(cut(z));
        } else if(yaw != null && pitch != null) {
            l = p.getLocation(l);
            l.setWorld(w);
        } else {
            l = w.getSpawnLocation();
        }

        if (yaw != null && pitch != null) {
            l.setYaw(yaw);
            l.setPitch(pitch);

            if (destination.length() > 0) destination.append(", ");
            destination.append("yaw: ").append(cut(yaw)).append(", pitch: ").append(cut(pitch));
        } else {
            l.setYaw(p.getLocation().getYaw());
            l.setPitch(p.getLocation().getPitch());
        }

        if (gate != p) gate.sendMessage(Lang.getPrefix() + Lang.get("Teleported_Player_Info").replace("%player%", p.getName()).replace("%warp%", destination.toString()));
        TeleportOptions options = new TeleportOptions(new Destination(new LocationAdapter(l)), destination.toString(), Origin.TeleportCommand);
        options.setSkip(true);
        options.setMessage(Lang.getPrefix() + (gate == p ? Lang.get("Teleported_To") : Lang.get("Teleported_To_By").replace("%gate%", gate.getName())));
        options.setNotifyPlayer(notifyPlayer);

        WarpSystem.getInstance().getTeleportManager().teleport(p, options);
        return true;
    }

    @Override
    public void tp(Player gate, PlayerData player, PlayerData target) {
        tp(gate, player, target, true);
    }

    @Override
    public void tp(Player gate, PlayerData player, PlayerData target, boolean notifyPlayer) {
        if (checkStatusTp(gate, player)) return;
        if (checkStatusTp(gate, target)) return;

        Player playerP = Bukkit.getPlayer(player.getName());
        Player targetP = Bukkit.getPlayer(target.getName());

        if (playerP == null || targetP == null) {
            gate.sendMessage(Lang.getPrefix() + Lang.get("Player_is_not_online"));
            return;
        }

        if (gate != playerP && TeleportCommandManager.getInstance().deniesForceTps(playerP)) {
            gate.sendMessage(Lang.getPrefix() + Lang.get("Teleport_denied").replace("%PLAYER%", playerP.getName()));
            return;
        }

        if (gate != playerP) gate.sendMessage(Lang.getPrefix() + Lang.get("Teleported_Player_Info").replace("%player%", playerP.getName()).replace("%warp%", targetP.getName()));

        TeleportOptions options = new TeleportOptions(new Destination(new LocationAdapter(targetP.getLocation())), targetP.getName(), Origin.TeleportCommand);
        options.setSkip(true);
        options.setMessage(Lang.getPrefix() + (gate == playerP ? Lang.get("Teleported_To") : Lang.get("Teleported_To_By").replace("%gate%", gate.getName())));
        options.setNotifyPlayer(notifyPlayer);

        WarpSystem.getInstance().getTeleportManager().teleport(playerP, options);
    }

    private boolean checkStatusTp(Player gate, PlayerData data) {
        if (data == null) {
            //offline
            gate.sendMessage(Lang.getPrefix() + Lang.get("Player_is_not_online"));
            return true;
        } else if (Bukkit.getPlayer(data.getName()) == null) {
            //Proxy
            if (Permissions.hasPermission(gate, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TP)) {
                TextComponent tc = new TextComponent(Lang.getPrefix() + "§7Teleporting on your entire proxy is a §6premium feature§7!");
                tc.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                Lang.PREMIUM_CHAT(tc, gate, true);
            } else {
                gate.sendMessage(Lang.getPrefix() + Lang.get("Player_is_not_online"));
            }
            return true;
        } else return false;
    }

    @Override
    public List<String> suggestTp(String[] args, List<String> suggestions) {
        int deep = args.length - 1;
        if (deep < 0) return suggestions;

        String last = args[deep].toLowerCase().trim();
        int name = isNumeric(args[0]) || !WarpSystem.getInstance().getPlayerDataManager().isPresent(args[0]) ? 0 : 1;

        int world;
        if (name == 0 && !isNumeric(args[0])) world = 0;
        else if (args.length >= 2 && !isNumeric(args[1]) && !WarpSystem.getInstance().getPlayerDataManager().isPresent(args[1])) world = 1;
        else world = -1;

        if (world == -1) {
            if (deep <= 4 + name && (isNumeric(last) || last.isEmpty())) {
                //[<x> <y> <z>] or [<yaw> <pitch>]

                if (deep < 1 + name || isNumeric(args[name])) {
                    if (name == 0 || WarpSystem.getInstance().getPlayerDataManager().isPresent(args[0])) {
                        boolean clean = true;
                        for (int i = name; i < 5 + name && i < args.length; i++) {
                            if (!isNumeric(args[i]) && !args[i].isEmpty()) {
                                clean = false;
                                break;
                            }
                        }

                        if (clean) {
                            if (deep < 4 + name || deep == 4 + name && last.isEmpty()) suggestions.add(suggest(last, "~"));
                            if (deep < 3 + name || deep == 3 + name && last.isEmpty()) suggestions.add(suggest(last, "~ ~"));
                            if (deep < 2 + name || deep == 2 + name && last.isEmpty()) suggestions.add(suggest(last, "~ ~ ~"));
                            if (deep < 1 + name || deep == 1 + name && last.isEmpty()) suggestions.add(suggest(last, "~ ~ ~ ~"));
                            if (deep < name || deep == name  && last.isEmpty()) suggestions.add("~ ~ ~ ~ ~");
                        }
                    }
                }
            }

            if (deep == 0 || deep == 1 && !isNumeric(args[0]) && WarpSystem.getInstance().getPlayerDataManager().isPresent(args[0])) {
                WarpSystem.getInstance().getPlayerDataManager().getCached().forEach(e -> suggestions.add(e.getName()));
            }
        }

        if (world == -1 || world == deep) {
            if (deep == 0
                    || deep == 1 && !isNumeric(args[0]) && WarpSystem.getInstance().getPlayerDataManager().isPresent(args[0])
                    || deep == 2 + name && isNumeric(args[name]) && isNumeric(args[1 + name])
                    || deep == 3 + name && isNumeric(args[name]) && isNumeric(args[1 + name]) && isNumeric(args[2 + name])
                    || deep == 5 + name && isNumeric(args[name]) && isNumeric(args[1 + name]) && isNumeric(args[2 + name]) && isNumeric(args[3 + name]) && isNumeric(args[4 + name])) {
                Bukkit.getWorlds().forEach(w -> suggestions.add(w.getName()));
            }
        }

        suggestions.removeIf(s -> !s.toLowerCase().startsWith(last));
        return suggestions;
    }

    @Override
    public List<String> suggestTpTo(String[] args, List<String> suggestions) {
        if (args.length == 1) {
            WarpSystem.getInstance().getPlayerDataManager().getCached().forEach(e -> suggestions.add(e.getName()));

            String name = args[0].toLowerCase();
            suggestions.removeIf(s -> !s.toLowerCase().startsWith(name));
        }

        return suggestions;
    }

    private String suggest(String last, String s) {
        if (last.isEmpty()) return s;
        else return last + " " + s;
    }

    private boolean isNumeric(String s) {
        if (s.isEmpty()) return false;
        s = s.replace("~", "");
        if (s.isEmpty()) return true;
        return Pattern.matches("[-+]?\\d+[.,]?(\\d+)?", s);
    }

    @Override
    public List<String> suggestTpHere(CommandSender sender, String[] args, List<String> suggestions) {
        WarpSystem.getInstance().getPlayerDataManager().getCached().forEach(p -> suggestions.add(p.getName()));
        return suggestions;
    }

    @Override
    public void tpAll(Player player, int alreadyHandled, int alreadySent) {
        player.sendMessage(Lang.getPrefix() + Lang.get("Teleport_all").replace("%AMOUNT%", alreadySent + "").replace("%MAX%", alreadyHandled + ""));
    }

    @Override
    public void tpa(Player player, String argument, Player other, boolean tpToSender) {
        if (other == null) {
            if (Permissions.hasPermission(player, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TP) && WarpSystem.getInstance().getPlayerDataManager().getCache(argument) != null) {
                TextComponent tc = new TextComponent(Lang.getPrefix() + "§7Teleporting on your entire BungeeCord is a §6premium feature§7!");
                tc.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                Lang.PREMIUM_CHAT(tc, player, true);
                return;
            }

            player.sendMessage(Lang.getPrefix() + Lang.get("Player_is_not_online"));
            return;
        }

        if(other.getGameMode() == GameMode.SPECTATOR || !player.canSee(other)) {
            player.sendMessage(Lang.getPrefix() + Lang.get("Player_is_not_online"));
            return;
        }

        TeleportCommandManager.getInstance().invite(player.getName(), tpToSender, new Callback<Long>() {
            @Override
            public void accept(Long result) {
                int handled = (int) (result >> 32);
                int sent = result.intValue();

                if (handled == 0) player.sendMessage(Lang.getPrefix() + Lang.get("Player_is_not_online"));
                else if (handled == -1) player.sendMessage(Lang.getPrefix() + Lang.get("TeleportRequest_denied_sender").replace("%PLAYER%", ChatColor.stripColor(other.getName())));
                else if (handled == -2) player.sendMessage(Lang.getPrefix() + Lang.get("Player_Protected_Area"));
                else if (sent == 0) player.sendMessage(Lang.getPrefix() + Lang.get("TeleportRequest_already_sent"));
                else player.sendMessage(Lang.getPrefix() + Lang.get("TeleportRequest_sent").replace("%PLAYER%", ChatColor.stripColor(other.getName())));
            }
        }, other.getName());
    }

    @Override
    public void suggestTpa(Player player, String[] args, List<String> suggestions, boolean tpToSender) {
        if (Permissions.hasPermission(player, Permissions.PERMISSION_USE_TELEPORT_COMMAND_TP)) {
            WarpSystem.getInstance().getPlayerDataManager().getCached().filter(d -> !d.getName().equals(player.getName()) && !d.isVanished()).filter(d -> {
                Player other = Bukkit.getPlayer(d.getName());

                return (other == null || player.canSee(other)) && !d.isVanished();
            }).forEach(d -> suggestions.add(ChatColor.stripColor(d.getName())));
        } else {
            for (Player other : Bukkit.getOnlinePlayers()) {
                PlayerData data = WarpSystem.getInstance().getPlayerDataManager().getCache(other);

                if (other.getName().equals(player.getName()) || !player.canSee(other) || data.isVanished()) continue;
                suggestions.add(ChatColor.stripColor(other.getName()));
            }
        }
    }

    @Override
    public void tpaAll(Player player) {
        TeleportCommandManager.getInstance().invite(player.getName(), true, new Callback<Long>() {
            @Override
            public void accept(Long result) {
                int handled = (int) (result >> 32);
                int sent = result.intValue();
                player.sendMessage(Lang.getPrefix() + Lang.get("TeleportRequest_All").replace("%RECEIVED%", sent + "").replace("%MAX%", handled + ""));
            }
        }, null, true);
    }
}
