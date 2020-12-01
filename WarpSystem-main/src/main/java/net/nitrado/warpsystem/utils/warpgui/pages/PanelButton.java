package net.nitrado.warpsystem.utils.warpgui.pages;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.buttons.Button;
import de.codingair.codingapi.tools.Callback;
import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.ServerPing;
import de.codingair.warpsystem.spigot.base.utils.teleport.Result;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.DestinationType;
import net.nitrado.warpsystem.utils.Exceptions;
import net.nitrado.warpsystem.utils.warpgui.WarpPanel;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PanelButton extends Button {
    private final String name;
    private final String skull;
    private final Material material;
    private final int id;
    private final Player player;

    private final ServerPing ping;
    private final Destination destination;
    private boolean joined = false;

    public PanelButton(String name, String skull, int id, String server, Player player) {
        this.name = name;
        this.skull = skull;
        this.material = null;
        this.id = id;
        this.player = player;

        this.ping = WarpSystem.getInstance().getServerManager().getProperties(server);
        this.destination = new Destination(server, DestinationType.Server);

        joined = server.equalsIgnoreCase(WarpSystem.getInstance().getCurrentServer());
    }

    @Override
    public ItemStack buildItem() {
        ItemBuilder item;
        if(material != null) item = new ItemBuilder(material);
        else item = new ItemBuilder(skull);

        if(id < 21) item.setName("§e" + name + " " + (id < 10 ? "0" : "") + id);
        else {
            item.setType(Material.NETHER_STAR);
            item.setName("§e" + name + " " + (id < 10 ? "0" : "") + id + " §8(§6§lPartner§8)");
            item.addEnchantment(Enchantment.DAMAGE_ALL, 1);
            item.setHideEnchantments(true);
        }

        if(ping == null || !ping.getStatus()) item.addLore("§7Status: §cOffline");
        else {
            item.addLore("§7Status: §aOnline");
            item.addLore("§7Spieler: " + (isFull(ping) ? "§c" : "§a") + ping.getPlayers() + "§8/§770");

            if(joined) item.addLore("", "§7» Bereits beigetreten");
            else {
                if(id >= 21) {
                    if(this.player.hasPermission("group.partner")) item.addLore("", "§7» Speziell für dich");
                    else item.addLore("", "§7» Für die " + WarpPanel.COLOR_NITRADO + "#NitradoFamily");
                } else item.addLore("", "§7» Server wechseln");
            }
        }

        return item.getItem();
    }

    @Override
    public boolean canClick(ClickType type) {
        if(id >= 21 && !player.hasPermission("group.partner")) return false;

        return type == ClickType.LEFT && !joined && ping != null && (!isFull(ping) || player.hasPermission(WarpPanel.PERMISSION_FULL));
    }

    @Override
    public void onClick(GUI gui, InventoryClickEvent e) {
        Player p = gui.getPlayer();

        destination.teleport(p, WarpPanel.COLOR_NITRADO + "Nitrado §8» §7Du wurdest zu §eEvent-" + (id < 10 ? "0" + id : id) + "§7 teleportiert.", "", false, false, 0, new Callback<Result>() {
            @Override
            public void accept(Result result) {
                switch(result) {
                    case ALREADY_ON_TARGET_SERVER:
                        p.sendMessage(WarpPanel.COLOR_NITRADO + "Nitrado §8» §7Du bist §cbereits auf diesem Server§7.");
                        break;

                    case TARGET_SERVER_IS_FULL:
                        p.sendMessage(WarpPanel.COLOR_NITRADO + "Nitrado §8» §7Dieser Server ist §cvoll§7.");
                        break;

                    case SERVER_NOT_AVAILABLE:
                        p.sendMessage(WarpPanel.COLOR_NITRADO + "Nitrado §8» §7Dieser Server ist §coffline§7.");
                        break;

                    case ERROR:
                        p.sendMessage(WarpPanel.PREFIX + "Es ist ein §cFehler §7aufgetreten. §8(" + Exceptions.WARP_2 + ")");
                        break;
                }
            }
        });
    }

    public static boolean isFull(ServerPing ping) {
        if(ping == null) return true;
        return ping.getPlayers() >= 70;
    }
}
