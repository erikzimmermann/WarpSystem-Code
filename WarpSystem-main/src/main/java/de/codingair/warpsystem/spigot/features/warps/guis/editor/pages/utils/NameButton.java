package de.codingair.warpsystem.spigot.features.warps.guis.editor.pages.utils;

import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.codingapi.tools.items.XMaterial;
import de.codingair.codingapi.utils.ChatColor;
import de.codingair.codingapi.utils.Value;
import de.codingair.warpsystem.spigot.base.guis.editor.Editor;
import de.codingair.warpsystem.spigot.base.language.Lang;
import de.codingair.warpsystem.spigot.features.warps.managers.IconManager;
import de.codingair.warpsystem.spigot.features.warps.nextlevel.utils.Icon;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class NameButton extends de.codingair.warpsystem.spigot.base.guis.editor.buttons.NameButton {
    private final Icon icon;
    private final String startName;

    public NameButton(int x, int y, Icon icon) {
        super(x, y, icon, ClickType.LEFT);

        this.name = new Value<>(icon.getName());

        this.icon = icon;
        startName = icon.getName();
        update(false);
    }

    @Override
    public boolean canClick(ClickType click) {
        return click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
    }

    @Override
    public ItemStack craftItem() {
        if(name == null || icon == null) return new ItemStack(Material.AIR);

        return new ItemBuilder(XMaterial.NAME_TAG)
                .setName(Editor.ITEM_TITLE_COLOR + Lang.get("Name"))
                .setLore("§3" + Lang.get("Current") + ": " + (name.getValue() == null ? "§c" + Lang.get("Not_Set") : "§7'§f" + super.prepareLine(name.getValue()) + "§7'"),
                        "", (name.getValue() == null ? "§3" + Lang.get("Leftclick") + ": §a" + Lang.get("Set_Name") : "§3" + Lang.get("Leftclick") + ": §a" + Lang.get("Change_Name") + " §8(§e" + (icon.isHideName() ? Lang.get("Show") : Lang.get("Hide")) + "§8)"))
                .getItem();
    }

    @Override
    public void onOtherClick(InventoryClickEvent e) {
        if(e.getClick() == ClickType.SHIFT_LEFT) {
            icon.setHideName(!icon.isHideName());
            update();
            updateShowItem();
        }
    }

    @Override
    public String acceptName(String name) {
        if(name == null) return null;

        name = ChatColor.stripColor(ChatColor.translateAll('&', name));
        if(startName != null && startName.equalsIgnoreCase(name)) return null;

        if(icon.isPage()) {
            if((icon.getName() == null || !icon.getName().equalsIgnoreCase(name)) && IconManager.getInstance().existsPage(name)) {
                return Lang.getPrefix() + Lang.get("Name_Already_Exists");
            }
        } else {
            if((icon.getName() == null || !icon.getNameWithoutColor().equalsIgnoreCase(name)) && IconManager.getInstance().existsIcon(name)) {
                return Lang.getPrefix() + Lang.get("Name_Already_Exists");
            }
        }

        return null;
    }

    public abstract void updateShowItem();
}
