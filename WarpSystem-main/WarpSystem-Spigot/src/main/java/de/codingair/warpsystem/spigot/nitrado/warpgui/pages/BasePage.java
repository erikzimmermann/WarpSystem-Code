package de.codingair.warpsystem.spigot.nitrado.warpgui.pages;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.Page;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Button;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Item;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.PageAlreadyOpenedException;
import de.codingair.codingapi.tools.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BasePage extends Page {
    public BasePage(GUI gui) {
        super(gui);
    }

    @Override
    public void buildItems() {
        ItemBuilder builder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setHideName(true);
        ItemStack item = builder.getItem();

        addButton(0, 2, new Button() {
            @Override
            public ItemStack buildItem() {
                return new ItemBuilder("3625902b389ed6c147574e422da8f8f361c8eb57e7631676a72777e7b1d").setName("§8» §cZurück").getItem();
            }

            @Override
            public boolean canClick(ClickType type) {
                return type == ClickType.LEFT;
            }

            @Override
            public void onClick(GUI gui, InventoryClickEvent e) {
                try {
                    gui.switchTo(SwitchPage.class);
                } catch(PageAlreadyOpenedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        addLine(0, 0, 0, 4, new Item(item));
        addLine(8, 0, 8, 4, new Item(item));
    }
}
