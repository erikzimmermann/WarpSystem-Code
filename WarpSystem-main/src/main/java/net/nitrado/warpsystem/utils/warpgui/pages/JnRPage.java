package net.nitrado.warpsystem.utils.warpgui.pages;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.Page;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Button;
import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.warpsystem.spigot.base.managers.TeleportManager;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.DestinationType;
import net.nitrado.warpsystem.utils.warpgui.WarpPanel;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class JnRPage extends Page {
    public JnRPage(GUI gui, Page basic) {
        super(gui, basic);
        setTitle("Teleporter - §3§lJump & Runs");
    }

    @Override
    public void buildItems() {
        addButton(2, 1, new JnRButton("Abwasserkanal", Material.COBWEB, new Destination("Abwasserkanal", DestinationType.SimpleWarp)));
        addButton(4, 1, new JnRButton("Weihnachten", "14e424b1676feec3a3f8ebade9e7d6a6f71f7756a869f36f7df0fc182d436e", new Destination("Weihnachten", DestinationType.SimpleWarp)));
        addButton(6, 1, new JnRButton("BlauerTunnel", Material.INFESTED_CHISELED_STONE_BRICKS, new Destination("BlauerTunnel", DestinationType.SimpleWarp)));
        addButton(2, 3, new JnRButton("Labyrinth", Material.JUNGLE_LEAVES, new Destination("Labyrinth", DestinationType.SimpleWarp)));
        addButton(3, 3, new JnRButton("Lager", Material.CHEST, new Destination("Lager", DestinationType.SimpleWarp)));
        addButton(5, 3, new JnRButton("Mine", Material.IRON_PICKAXE, new Destination("Mine", DestinationType.SimpleWarp)));
        addButton(6, 3, new JnRButton("Tower", Material.LANTERN, new Destination("Tower", DestinationType.SimpleWarp)));
    }

    private static class JnRButton extends Button {
        private final String name;
        private final Material material;
        private final String skull;
        private final TeleportOptions options;

        public JnRButton(String name, Material material, Destination destination) {
            this.name = name;
            this.material = material;
            this.skull = null;

            this.options = new TeleportOptions(destination, "");
            this.options.setMessage(WarpPanel.COLOR_NITRADO + "Nitrado §8» §7Du wurdest zum Jump 'n' Run §e" + name + "§7 teleportiert.");
        }

        public JnRButton(String name, String skull, Destination destination) {
            this.name = name;
            this.material = null;
            this.skull = skull;

            this.options = new TeleportOptions(destination, "");
            this.options.setMessage(WarpPanel.COLOR_NITRADO + "Nitrado §8» §7Du wurdest zum Jump 'n' Run §e" + name + "§7 teleportiert.");
        }

        @Override
        public ItemStack buildItem() {
            if(material != null) return new ItemBuilder(material).setName("§e" + name).setHideStandardLore(true).addLore("", "§7» Teleportieren").getItem();
            else return new ItemBuilder(skull).setName("§e" + name).setHideStandardLore(true).addLore("", "§7» Teleportieren").getItem();
        }

        @Override
        public boolean canClick(ClickType type) {
            return true;
        }

        @Override
        public void onClick(GUI gui, InventoryClickEvent e) {
            TeleportManager.getInstance().teleport(gui.getPlayer(), options);
        }
    }
}
