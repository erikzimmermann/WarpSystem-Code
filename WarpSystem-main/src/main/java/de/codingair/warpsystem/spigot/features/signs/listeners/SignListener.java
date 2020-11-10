package de.codingair.warpsystem.spigot.features.signs.listeners;

import de.codingair.codingapi.API;
import de.codingair.codingapi.server.events.PlayerPickItemEvent;
import de.codingair.codingapi.server.specification.Version;
import de.codingair.codingapi.tools.Location;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.language.Lang;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.signs.guis.WarpSignGUI;
import de.codingair.warpsystem.spigot.features.signs.managers.SignManager;
import de.codingair.warpsystem.spigot.features.signs.utils.NBTHelper;
import de.codingair.warpsystem.spigot.features.signs.utils.WarpSign;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SignListener implements Listener {
    private final SignManager manager;
    
    public SignListener() {
        manager = WarpSystem.getInstance().getDataManager().getManager(FeatureType.SIGNS);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        if(e.getClickedBlock() == null || e.getClickedBlock().getState() instanceof Sign) {
            Sign s = e.getClickedBlock() == null ? null : (Sign) e.getClickedBlock().getState();

            WarpSign sign = manager.getByLocation(s.getLocation());
            if(sign != null) {
                if(!e.getPlayer().isSneaking() && e.getPlayer().hasPermission(WarpSystem.PERMISSION_MODIFY_WARP_SIGNS) && e.getPlayer().getItemInHand().getType().name().toLowerCase().contains("sign")) {
                    sign.editMode();
                    sign.setEditing(true);
                    Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> new WarpSignGUI(e.getPlayer(), sign).open(), 1L);
                    return;
                }

                if(!WarpSystem.hasPermission(e.getPlayer(), WarpSystem.PERMISSION_USE_WARP_SIGNS)) {
                    e.getPlayer().sendMessage(Lang.getPrefix() + Lang.get("No_Permission"));
                    return;
                }

                sign.perform(e.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        try {
            Sign s = e.getBlock() == null ? null : (Sign) e.getBlock().getState();
            if(s == null) return;
            WarpSign sign = manager.getByLocation(s.getLocation());
            if(sign == null) return;

            if(!e.getPlayer().hasPermission(WarpSystem.PERMISSION_MODIFY_WARP_SIGNS)) {
                e.getPlayer().sendMessage(Lang.getPrefix() + Lang.get("No_Permission"));
                e.setCancelled(true);
            } else if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                e.getPlayer().sendMessage(Lang.getPrefix() + Lang.get("Creative_Mode_Needed"));
                e.setCancelled(true);
            } else {
                for(WarpSignGUI gui : API.getRemovables(WarpSignGUI.class)) {
                    gui.close();
                    gui.getPlayer().sendMessage(Lang.getPrefix() + Lang.get("WarpSign_Removed"));
                }

                manager.removeWarpSign(sign);
                e.getPlayer().sendMessage(Lang.getPrefix() + Lang.get("WarpSign_Removed"));
            }
        } catch(Exception ignored) {
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Block b = e.getBlock();

        if(b.getState() instanceof Sign) {
            if(Version.get().isBiggerThan(13)) {
                ItemStack item = e.getItemInHand();

                org.bukkit.Location location = NBTHelper.fromSign(item);
                
                if(location != null) {
                    WarpSign original = manager.getByLocation(location);

                    if(original != null) {
                        //clone
                        WarpSign clone = original.cloneAt(new Location(b.getLocation()));
                        manager.addWarpSign(clone);
                        clone.update();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPick(PlayerPickItemEvent e) {
        Block b = e.getFrom();

        if(b.getState() instanceof Sign && e.isNBTCopy()) {
            WarpSign sign = manager.getByLocation(b.getLocation());

            if(sign != null) {
                //apply nbt
                NBTHelper.applyNBT(e.getItemStack(), sign);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlace(SignChangeEvent e) {
        if(!e.getPlayer().hasPermission(WarpSystem.PERMISSION_MODIFY_WARP_SIGNS)) return;

        if(e.getLine(0).equalsIgnoreCase("[warps]")) {
            WarpSign sign = new WarpSign(Location.getByLocation(e.getBlock().getLocation()), new Destination());
            Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> new WarpSignGUI(e.getPlayer(), sign).open(), 1L);
        }
    }

}
