package de.codingair.warpsystem.spigot.base.utils;

import de.codingair.codingapi.files.ConfigFile;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Permissions {
    public static final String PERMISSION_NOTIFY = "warpsystem.notify";
    public static final String PERMISSION_MODIFY = "warpsystem.modify";
    public static final String PERMISSION_MODIFY_WARP_GUI = "warpsystem.modify.warpgui";
    public static final String PERMISSION_MODIFY_SHORTCUTS = "warpsystem.modify.shortcuts";
    public static final String PERMISSION_MODIFY_WARP_SIGNS = "warpsystem.modify.warpsigns";
    public static final String PERMISSION_MODIFY_GLOBAL_WARPS = "warpsystem.modify.globalwarps";
    public static final String PERMISSION_MODIFY_SIMPLE_WARPS = "warpsystem.modify.simplewarps";
    public static final String PERMISSION_MODIFY_PORTALS = "warpsystem.modify.portals";
    public static final String PERMISSION_MODIFY_RANDOM_TELEPORTER = "warpsystem.modify.randomteleport";
    public static final String PERMISSION_MODIFY_PLAYER_WARPS = "warpsystem.modify.playerwarps";
    public static final String PERMISSION_MODIFY_SPAWN = "warpsystem.modify.spawn";

    private static final String PERMISSION_USE_TELEPORT_COMMAND = "warpsystem.use.teleportCommand";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_TP = PERMISSION_USE_TELEPORT_COMMAND + ".tp";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_TP_SILENT = PERMISSION_USE_TELEPORT_COMMAND_TP + ".silent";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_TPHERE = PERMISSION_USE_TELEPORT_COMMAND + ".tphere";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_TPTO = PERMISSION_USE_TELEPORT_COMMAND + ".tpto";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_TP_TOGGLE = PERMISSION_USE_TELEPORT_COMMAND + ".tptoggle";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_TPALL = PERMISSION_USE_TELEPORT_COMMAND + ".tpall";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_TPA_ALL = PERMISSION_USE_TELEPORT_COMMAND + ".tpaall";

    public static final String PERMISSION_WARP_GUI_OTHER = "warpsystem.warpgui.other";
    public static final String PERMISSION_HIDE_ALL_ICONS = "warpgui.hideall";

    public static final String PERMISSION_SIMPLE_WARPS_DIRECT_TELEPORT = "warpsystem.simplewarp.directteleport";
    public static final String PERMISSION_GLOBAL_WARPS_DIRECT_TELEPORT = "warpsystem.globalwarp.directteleport";

    public static final String PERMISSION_RANDOM_TELEPORT_SELECTION_SELF = "warpsystem.randomteleport.selection";
    public static final String PERMISSION_RANDOM_TELEPORT_SELECTION_OTHER = "warpsystem.randomteleport.selection.other";

    public static final String PERMISSION_ByPass_Teleport_Costs = "warpsystem.bypass.teleport.costs";
    public static final String PERMISSION_ByPass_Teleport_Delay = "warpsystem.bypass.teleport.delay";
    public static final String PERMISSION_ByPass_Teleport_Max_Players = "warpsystem.bypass.teleport.maxplayers";
    public static final String PERMISSION_ByPass_Teleport_Cooldown = "warpsystem.bypass.cooldown";

    public static String PERMISSION_USE_TELEPORT_COMMAND_BACK = PERMISSION_USE_TELEPORT_COMMAND + ".back";
    public static final String PERMISSION_USE_TELEPORT_COMMAND_BACK_OTHER = PERMISSION_USE_TELEPORT_COMMAND_BACK + ".other";
    public static String PERMISSION_USE_TELEPORT_COMMAND_BACK_DETECT_DEATHS = PERMISSION_USE_TELEPORT_COMMAND_BACK + ".deaths";

    public static String PERMISSION_USE_TELEPORT_COMMAND_TPA = PERMISSION_USE_TELEPORT_COMMAND + ".tpa";
    public static String PERMISSION_USE_TELEPORT_COMMAND_TP_ACCEPT = PERMISSION_USE_TELEPORT_COMMAND + ".tpaccept";
    public static String PERMISSION_USE_TELEPORT_COMMAND_TP_DENY = PERMISSION_USE_TELEPORT_COMMAND + ".tpdeny";
    public static String PERMISSION_USE_TELEPORT_COMMAND_TPA_TOGGLE = PERMISSION_USE_TELEPORT_COMMAND + ".tpatoggle";
    public static String PERMISSION_USE_TELEPORT_COMMAND_TPA_HERE = PERMISSION_USE_TELEPORT_COMMAND + ".tpahere";

    public static String PERMISSION_USE_WARP_GUI = "warpsystem.use.warpgui";
    public static String PERMISSION_USE_WARP_SIGNS = "warpsystem.use.warpsigns";
    public static String PERMISSION_USE_GLOBAL_WARPS = "warpsystem.use.globalwarps";
    public static String PERMISSION_USE_SIMPLE_WARPS = "warpsystem.use.simplewarps";
    public static String PERMISSION_USE_PLAYER_WARPS = "warpsystem.use.playerwarps";
    public static String PERMISSION_USE_PORTALS = "warpsystem.use.portals";
    public static String PERMISSION_USE_RANDOM_TELEPORTER = "warpsystem.use.randomteleport";
    public static String PERMISSION_USE_RANDOM_TELEPORTER_GO = PERMISSION_USE_RANDOM_TELEPORTER + ".go";
    public static String PERMISSION_USE_SPAWN = "warpsystem.use.spawn";

    private Permissions() {
    }

    public static boolean hasPermission(CommandSender sender, String permission) {
        return permission == null || sender.hasPermission(permission);
    }

    public static void checkPermissions() {
        ConfigFile config = WarpSystem.getInstance().getFileManager().getFile("Config");
        if (config.getConfig().getString("Do_Not_Edit.Last_Version", "0").equals("0")) {
            config.getConfig().set("WarpSystem.Permissions", false);
            config.saveConfig();
        }

        if (!config.getConfig().getBoolean("WarpSystem.Permissions", true)) {
            for (Field f : Permissions.class.getDeclaredFields()) {
                if (!Modifier.isFinal(f.getModifiers()) && f.getName().startsWith("PERMISSION_USE_")) {
                    f.setAccessible(true);
                    try {
                        f.set(null, null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
