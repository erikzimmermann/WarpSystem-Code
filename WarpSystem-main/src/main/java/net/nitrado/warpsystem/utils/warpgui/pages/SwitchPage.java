package net.nitrado.warpsystem.utils.warpgui.pages;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.Page;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Button;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Item;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.PageAlreadyOpenedException;
import de.codingair.codingapi.tools.items.ItemBuilder;
import net.nitrado.warpsystem.utils.warpgui.WarpPanel;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SwitchPage extends Page {
    private final Cache<Player, Integer> lastPage = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
    private final Class<? extends Page>[] pages = new Class[] {
            ServerPage.class
    };

    public SwitchPage(GUI gui) {
        super(gui);
        lastPage.put(gui.getPlayer(), 0);
    }

    @Override
    public void buildItems() {
        ItemBuilder builder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setHideName(true);
        ItemStack item = builder.getItem();

        addButton(2, 0, new Item(item));
        addButton(3, 1, new Item(item));
        addButton(4, 1, new Item(item));
        addButton(5, 1, new Item(item));
        addButton(6, 0, new Item(item));

        for(int i = 3; i < 6; i++) {
            int id = i - 3;

            addButton(i, 0, new Button() {
                @Override
                public ItemStack buildItem() {
                    return getItem(lastPage.getIfPresent(gui.getPlayer()), id);
                }

                @Override
                public boolean canClick(ClickType type) {
                    return type == ClickType.LEFT && !Objects.equals(lastPage.getIfPresent(gui.getPlayer()), id);
                }

                @Override
                public void onClick(GUI gui, InventoryClickEvent e) {
                    try {
                        gui.switchTo(pages[id]);
                    } catch(PageAlreadyOpenedException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    private ItemStack getItem(Integer active, int id) {
        ItemBuilder builder = new ItemBuilder();

        if(id == 0) {
            builder.setType(Material.LADDER);
            builder.setName("§b§lJump 'n' Runs");
        } else if(id == 1) {
            builder.setType(Material.COOKIE);
            builder.setName(WarpPanel.COLOR_NITRADO + "§lEvent Server");
        } else if(id == 2) {
            builder.setType(Material.IRON_SWORD);
            builder.setName("§c§lMinigames");
        }

        if(active != id) {
            builder.addLore("", "§7» Seite wechseln");
        } else {
            builder.addEnchantment(Enchantment.DAMAGE_ALL, 1);
            builder.setHideEnchantments(true);
        }

        return builder.getItem();
    }
}
