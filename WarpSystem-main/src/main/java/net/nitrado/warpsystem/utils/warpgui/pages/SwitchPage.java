package net.nitrado.warpsystem.utils.warpgui.pages;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.Page;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Button;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Item;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.PageAlreadyOpenedException;
import de.codingair.codingapi.tools.items.ItemBuilder;
import net.nitrado.warpsystem.utils.warpgui.WarpPanel;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class SwitchPage extends Page {
    public SwitchPage(GUI gui) {
        super(gui);
    }

    @Override
    public void buildItems() {
        ItemBuilder builder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setHideName(true);
        ItemStack item = builder.getItem();

        addLine(0, 0, 0, 4, new Item(item));
        addLine(8, 0, 8, 4, new Item(item));

        addButton(2, 2, new Button() {
            @Override
            public ItemStack buildItem() {
                ItemBuilder builder = new ItemBuilder();

                builder.setType(Material.LADDER);
                builder.setName("§3§lJump 'n' Runs");
                builder.addLore("", "§7» Kategorie öffnen");

                return builder.getItem();
            }

            @Override
            public boolean canClick(ClickType type) {
                return type == ClickType.LEFT;
            }

            @Override
            public void onClick(GUI gui, InventoryClickEvent e) {
                try {
                    gui.switchTo(JnRPage.class);
                } catch(PageAlreadyOpenedException ignored) {
                }
            }
        });

        addButton(4, 2, new Button() {
            @Override
            public ItemStack buildItem() {
                ItemBuilder builder = new ItemBuilder();

                builder.setType(Material.COOKIE);
                builder.setName("§5§lEvent Server");
                builder.addLore("", "§7» Kategorie öffnen");

                return builder.getItem();
            }

            @Override
            public boolean canClick(ClickType type) {
                return type == ClickType.LEFT;
            }

            @Override
            public void onClick(GUI gui, InventoryClickEvent e) {
                try {
                    gui.switchTo(ServerPage.class);
                } catch(PageAlreadyOpenedException ignored) {
                }
            }
        });

        addButton(6, 2, new Button() {
            @Override
            public ItemStack buildItem() {
                ItemBuilder builder = new ItemBuilder();

                builder.setType(Material.IRON_SWORD);
                builder.setName("§c§lMinigames");
                builder.addLore("", "§7» Kategorie öffnen");
                builder.setHideStandardLore(true);

                return builder.getItem();
            }

            @Override
            public boolean canClick(ClickType type) {
                return type == ClickType.LEFT;
            }

            @Override
            public void onClick(GUI gui, InventoryClickEvent e) {
                try {
                    gui.switchTo(ArcadePage.class);
                } catch(PageAlreadyOpenedException ignored) {
                }
            }
        });
    }
}
