package de.codingair.warpsystem.spigot.features.warps.guis;

import de.codingair.codingapi.player.gui.inventory.gui.GUI;
import de.codingair.codingapi.player.gui.inventory.gui.InterfaceListener;
import de.codingair.codingapi.player.gui.inventory.gui.Skull;
import de.codingair.codingapi.player.gui.inventory.gui.itembutton.ItemButton;
import de.codingair.codingapi.player.gui.inventory.gui.itembutton.ItemButtonOption;
import de.codingair.codingapi.player.gui.inventory.gui.simple.SyncButton;
import de.codingair.codingapi.server.sounds.Sound;
import de.codingair.codingapi.server.sounds.SoundData;
import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.codingapi.utils.ChatColor;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.guis.editor.Editor;
import de.codingair.warpsystem.spigot.base.guis.editor.StandardButtonOption;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.Permissions;
import de.codingair.warpsystem.spigot.base.utils.SoundUtil;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.Action;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.BoundAction;
import de.codingair.warpsystem.spigot.base.utils.options.specific.WarpGUIOptions;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.warps.managers.IconManager;
import de.codingair.warpsystem.spigot.features.warps.nextlevel.utils.Icon;
import de.codingair.warpsystem.spigot.versionfactory.VFac;
import de.codingair.warpsystem.spigot.versionfactory.VKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GWarps extends GUI {
    private static final IWarpGUI HANDLER = VFac.build(VKey.WarpGUI);
    private final boolean canEdit;
    private final String world;
    private final List<Class<? extends Icon>> hide;
    private Icon page;
    private boolean editing;
    private boolean moving = false, cloning = false;
    private ItemStack cursor = null;
    private int oldSlot = -999;
    private Icon cursorIcon = null;
    private boolean showMenu = true;
    private boolean changingGUI = false;
    private int emptySlots = 0;

    public GWarps(Player p, Icon page, boolean editing) {
        this(p, page, editing, (Class<? extends Icon>[]) null);
    }

    @SafeVarargs
    public GWarps(Player p, Icon page, boolean editing, Class<? extends Icon>... without) {
        this(p, page, editing, true, without);
    }

    @SafeVarargs
    public GWarps(Player p, Icon page, boolean editing, boolean canEdit, Class<? extends Icon>... without) {
        this(p, page, editing, canEdit, p.getLocation().getWorld().getName(), without);
    }

    @SafeVarargs
    public GWarps(Player p, Icon page, boolean editing, boolean canEdit, String world, Class<? extends Icon>... without) {
        super(p, getTitle(page, p), getSize(p), WarpSystem.getInstance(), false);
        this.page = page;
        this.editing = editing;
        this.canEdit = canEdit;
        this.world = IconManager.getInstance().boundToWorld() ? world : null;
        this.hide = without == null ? new ArrayList<>() : Arrays.asList(without);

        setBuffering(true);
        setCanDropItems(true);

        Listener listener;
        Bukkit.getPluginManager().registerEvents(listener = new Listener() {
            @EventHandler
            public void onClick(InventoryClickEvent e) {
                if (!getInventory().equals(e.getInventory())) return;
                if (!p.equals(e.getWhoClicked())) return;

                if (e.getClickedInventory() == e.getView().getBottomInventory() && cloning && cursorIcon == null) {
                    //fast deleting
                    e.setCancelled(true);
                }
            }

            @EventHandler
            public void onClick(InventoryDragEvent e) {
                if (!getInventory().equals(e.getInventory())) return;
                if (!p.equals(e.getWhoClicked())) return;

                if (cloning && cursorIcon == null) {
                    //fast deleting
                    for (Integer rawSlot : e.getRawSlots()) {
                        if (rawSlot > 53) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            @EventHandler
            public void onDrop(PlayerDropItemEvent e) {
                if (!p.equals(e.getPlayer())) return;

                Player p = e.getPlayer();

                if (!p.getName().equals(getPlayer().getName()) || !moving) return;

                if (cursor != null && !cursor.getType().equals(Material.AIR) && cursor.getType().equals(e.getItemDrop().getItemStack().getType())) {
                    e.getItemDrop().remove();
                    HandlerList.unregisterAll(this);
                    cursor = null;
                }
            }

        }, WarpSystem.getInstance());

        addListener(new InterfaceListener() {
            @Override
            public void onInvClickEvent(InventoryClickEvent e) {
                if (!getInventory().equals(e.getInventory())) return;
                if (!p.equals(e.getWhoClicked())) return;

                if (!cloning && cursorIcon != null && cursorIcon.getPage() == GWarps.this.page && cursorIcon.getSlot() == e.getSlot()) {
                    e.getView().setCursor(new ItemStack(Material.AIR));
                    setMoving(false, e.getSlot());
                    Sound.UI_BUTTON_CLICK.playSound(getPlayer(), 0.7F, 1F);
                    e.setCancelled(true);
                }
            }

            @Override
            public void onDropItem(InventoryClickEvent e) {
                if (!getInventory().equals(e.getInventory())) return;
                if (!p.equals(e.getWhoClicked())) return;

                e.setCancelled(true);

                if (moving) {
                    //cancel
                    setMoving(false, oldSlot);
                    e.getView().setCursor(new ItemStack(Material.AIR));
                } else if (cloning) {
                    //cancel
                    cloning = false;
                    oldSlot = -999;
                    cursor = null;
                    cursorIcon = null;

                    e.getView().setCursor(new ItemStack(Material.AIR));
                }
            }

            @Override
            public void onInvOpenEvent(InventoryOpenEvent e) {

            }

            @Override
            public void onInvCloseEvent(InventoryCloseEvent e) {
                if (!getInventory().equals(e.getInventory())) return;
                if (!p.equals(e.getPlayer())) return;

                e.getView().setCursor(new ItemStack(Material.AIR));

                if (changingGUI) {
                    changingGUI = false;
                } else if (!showMenu) {
                    showMenu = true;
                    reinitialize();
                    setTitle(getTitle(GWarps.this.page, getPlayer()));
                    Bukkit.getScheduler().runTask(WarpSystem.getInstance(), () -> open());
                    return;
                }

                if (listener != null) HandlerList.unregisterAll(listener);
            }

            @Override
            public void onInvDragEvent(InventoryDragEvent e) {

            }
        });

        initialize(p);
    }

    public static String getTitle(Icon page, Player player) {
        FileConfiguration config = WarpSystem.getInstance().getFileManager().getFile("Config").getConfig();
        String key = player.hasPermission(Permissions.PERMISSION_MODIFY) ? "Admin" : "User";

        return ChatColor.translateAll('&', (page == null || page.getName() == null ?
                config.getString("WarpSystem.GUI." + key + ".Title.Standard", "&c&nWarps&r") :
                config.getString("WarpSystem.GUI." + key + ".Title.In_Category", "&c&nWarps&r &c@%PAGE%").replace("%PAGE%", page.getNameWithoutColor()).replace("%CATEGORY%", page.getNameWithoutColor())));
    }

    private static int getSize(Player player) {
        return player.hasPermission(Permissions.PERMISSION_MODIFY) ? WarpSystem.getOptions(WarpGUIOptions.class).getAdminSize().getValue() : WarpSystem.getOptions(WarpGUIOptions.class).getUserSize().getValue();
    }

    public void initialize(Player p) {
        IconManager manager = WarpSystem.getInstance().getDataManager().getManager(FeatureType.WARP_GUI);

        ItemButtonOption option = new ItemButtonOption();
        option.setClickSound(new SoundData(Sound.UI_BUTTON_CLICK, 0.7F, 1F));
        option.setOnlyLeftClick(true);

        ItemBuilder noneBuilder;

        if (editing) {
            noneBuilder = HANDLER.getBarrier();
        } else {
            noneBuilder = new ItemBuilder(IconManager.getInstance().getBackground()).setHideName(true).setHideStandardLore(true).setHideEnchantments(true);
        }

        ItemStack none = noneBuilder.getItem();

        if (p.hasPermission(Permissions.PERMISSION_MODIFY_WARP_GUI) && showMenu && canEdit) {
            ItemBuilder builder = new ItemBuilder(Material.NETHER_STAR).setName(Lang.get("Menu_Help"));

            if (editing) {
                builder.setLore("§0", "§3" + Lang.get("Leftclick") + ": §b" + Lang.get("Quit_Edit_Mode"));
            } else {
                builder.setLore("§0", "§3" + Lang.get("Leftclick") + ": §b" + Lang.get("Edit_Mode"));
            }
            builder.addLore("§3" + Lang.get("Shift_Leftclick") + ": §b" + Lang.get("Set_Background"));
            builder.addLore("");
            builder.addLore("§3" + Lang.get("Rightclick") + ": §b" + Lang.get("Show_Icon"));

            builder.addEnchantment(ItemBuilder.anyEnchantment(), 1);
            builder.setHideEnchantments(true);

            addButton(new ItemButton(0, builder.getItem()) {
                @Override
                public void onClick(InventoryClickEvent e) {
                    if (moving || cloning) return;

                    if (e.isLeftClick()) {
                        if (e.isShiftClick()) IconManager.getInstance().setBackground(getPlayer().getInventory().getItem(getPlayer().getInventory().getHeldItemSlot()));
                        else editing = !editing;

                        reinitialize();
                        setTitle(getTitle(GWarps.this.page, getPlayer()));
                    } else {
                        if (!e.isShiftClick()) {
                            showMenu = false;
                            reinitialize();
                            setTitle(getTitle(GWarps.this.page, getPlayer()));
                        }
                    }
                }
            }.setOption(option).setOnlyLeftClick(false));
        }

        int size = getSize(getPlayer());
        if (page != null) {
            addButton(new ItemButton(size - 9, new ItemBuilder(Skull.ArrowLeft).setName("§c" + Lang.get("Back") + (page.getDepth() > 0 ? " §8(§7" + Lang.get("Shift") + "§8)" : "")).getItem()) {
                @Override
                public void onClick(InventoryClickEvent e) {
                    GWarps.this.page = e.isShiftClick() ? null : page.getPage();
                    reinitialize();
                    setTitle(getTitle(GWarps.this.page, getPlayer()));
                }
            }.setOption(option));
        }

        List<Icon> icons = manager.getIcons(page);
        for (Icon icon : icons) {
            if (icon.isPage() || !icon.hasPermission() && (hideAll(p) || hideAll(p, "Warp")) && !editing) continue;
            processIcon(p, icon);
        }

        List<Icon> cIcons = manager.getPages(page);
        for (Icon icon : cIcons) {
            if (!icon.hasPermission() && (hideAll(p) || hideAll(p, "Page")) && !editing) continue;
            processIcon(p, icon);
        }

        emptySlots = 0;
        for (int i = 0; i < size; i++) {
            if (editing) {
                final int slot = i;
                if (slot == oldSlot && cursorIcon != null && !cursorIcon.isPage() && cursorIcon.getPage() == this.page) continue;

                if (getItem(i) == null || getItem(i).getType().equals(Material.AIR)) {
                    emptySlots++;
                    addButton(new ItemButton(i, none.clone()) {
                        @Override
                        public void onClick(InventoryClickEvent clickEvent) {
                            changingGUI = true;
                            boolean closing = HANDLER.handleBarrierClick(clickEvent, p, this, GWarps.this, none, slot);
                            if (!closing) changingGUI = false;
                        }
                    }.setOption(option).setOnlyLeftClick(false));
                }
            } else {
                if (getItem(i) == null || getItem(i).getType().equals(Material.AIR)) setItem(i, none);
            }
        }
    }

    private void processIcon(Player p, Icon icon) {
        BoundAction bound = icon.getAction(Action.BOUND_TO_WORLD);
        if (((bound == null && world == null) || (bound != null && world != null && world.equals(bound.getValue())))
                && (editing || (!icon.hasPermission() || IconManager.getInstance().isShowWithoutPermission() || p.hasPermission(icon.getPermission())))
                && this.cursorIcon != icon) {
            addToGUI(p, icon);
        }
    }

    private void addToGUI(Player p, Icon icon) {
        if (icon.isDisabled() && !editing) return;

        if ((icon.getSlot() == 0 && showMenu && p.hasPermission(Permissions.PERMISSION_MODIFY_WARP_GUI)) || icon.getSlot() >= getSize(getPlayer())) return;

        for (Class<? extends Icon> forbidden : this.hide) {
            if (forbidden.isInstance(icon)) return;
        }

        ItemButtonOption option = new StandardButtonOption();
        SoundData s = option.getClickSound2();

        addButton(new SyncButton(icon.getSlot()) {

            @Override
            public ItemStack craftItem() {
                ItemBuilder iconBuilder = icon.getItemBuilderWithPlaceholders(getPlayer());

                if (editing) HANDLER.modifyEditingIconBuilder(iconBuilder, icon);

                return iconBuilder.getItem();
            }

            @Override
            public boolean canClick(ClickType click) {
                return editing || (!icon.getActions().isEmpty() || icon.isPage()) && click == ClickType.LEFT;
            }

            @Override
            public void onClick(InventoryClickEvent e, Player player) {
                if (editing) {
                    changingGUI = true;
                    boolean closing = HANDLER.onEditingIconClick(e, player, this, icon, s, GWarps.this);
                    if (!closing) changingGUI = false;
                } else if (e.isLeftClick()) {
                    if(icon.hasPermission() && !p.hasPermission(icon.getPermission())) {
                        p.sendMessage(Lang.getPrefix() + Lang.get("No_Permission"));
                        return;
                    }

                    if (!icon.hasAction(Action.SOUND)) SoundUtil.play(player, s);

                    if (icon.isPage()) {
                        GWarps.this.page = icon;
                        reinitialize();
                        setTitle(getTitle(GWarps.this.page, getPlayer()));
                    }

                    icon.perform(p);
                }
            }
        }.setOption(option).setClickSound2(null));
    }

    public void setMoving(boolean moving, int slot) {
        if (!moving) {
            if (oldSlot != slot) {
                getPlayer().sendMessage(Lang.getPrefix() + Lang.get("Success_Icon_Moved"));
            }

            oldSlot = -999;
            cursor = null;
            cursorIcon = null;
            reinitialize();
            setTitle(getTitle(GWarps.this.page, getPlayer()));
        }

        this.moving = moving;
        this.oldSlot = slot;

        if (moving) {
            for (int i = 0; i < getSize(); i++) {
                if (i == slot || getItem(i) == null || getItem(i).getType().equals(Material.AIR)) continue;

                setItem(i, new ItemBuilder(getItem(i)).setLore("", "§3" + Lang.get("Leftclick") + ": §b" + Lang.get("Move_Icon")).getItem());
            }
        }
    }

    private boolean hideAll(Player player) {
        for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
            String perm = effectivePermission.getPermission();
            if (perm.equalsIgnoreCase(Permissions.PERMISSION_HIDE_ALL_ICONS)) return true;
        }
        return false;
    }

    private boolean hideAll(Player player, String type) {
        for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
            String perm = effectivePermission.getPermission();
            if (perm.equalsIgnoreCase(Permissions.PERMISSION_HIDE_ALL_ICONS + "." + type)) return true;
        }
        return false;
    }

    public Icon getPage() {
        return page;
    }

    public void setPage(Icon page) {
        this.page = page;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public boolean isCloning() {
        return cloning;
    }

    public void setCloning(boolean cloning) {
        this.cloning = cloning;
    }

    public ItemStack getCursor() {
        return cursor;
    }

    public void setCursor(ItemStack cursor) {
        this.cursor = cursor;
    }

    public int getOldSlot() {
        return oldSlot;
    }

    public void setOldSlot(int oldSlot) {
        this.oldSlot = oldSlot;
    }

    public Icon getCursorIcon() {
        return cursorIcon;
    }

    public void setCursorIcon(Icon cursorIcon) {
        this.cursorIcon = cursorIcon;
    }

    public boolean isShowMenu() {
        return showMenu;
    }

    public void setShowMenu(boolean showMenu) {
        this.showMenu = showMenu;
    }

    public int getEmptySlots() {
        return emptySlots;
    }

    public void setEmptySlots(int emptySlots) {
        this.emptySlots = emptySlots;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public String getWorld() {
        return world;
    }

    public List<Class<? extends Icon>> getHide() {
        return hide;
    }
}
