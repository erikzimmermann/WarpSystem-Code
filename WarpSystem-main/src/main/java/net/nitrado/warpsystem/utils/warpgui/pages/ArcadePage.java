package net.nitrado.warpsystem.utils.warpgui.pages;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.Page;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Button;
import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.ServerPing;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.DestinationType;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ArcadePage extends Page {

    public ArcadePage(GUI gui) {
        super(gui);
    }

    @Override
    public void buildItems() {
        //bb
        addButton(1, 2, new PanelPage.PanelButton("BuildBattle", Material.BONE_BLOCK, 1, "bb01", gui.getPlayer()));
        addButton(4, 2, new PanelPage.PanelButton("BuildBattle", Material.NETHERITE_BLOCK, 2, "bb02", gui.getPlayer()));
        addButton(7, 2, new PanelPage.PanelButton("BuildBattle", Material.GLOWSTONE, 3, "bb03", gui.getPlayer()));

        //ffa
        addButton(1, 4, new PanelPage.PanelButton("Free for All", Material.STONE_SWORD, 1, "ffa01", gui.getPlayer()));
        addButton(3, 4, new PanelPage.PanelButton("Free for All", Material.IRON_SWORD, 2, "ffa02", gui.getPlayer()));
        addButton(5, 4, new PanelPage.PanelButton("Free for All", Material.DIAMOND_SWORD, 3, "ffa03", gui.getPlayer()));
        addButton(7, 4, new PanelPage.PanelButton("Free for All", Material.NETHERITE_SWORD, 4, "ffa04", gui.getPlayer()));
    }
}
