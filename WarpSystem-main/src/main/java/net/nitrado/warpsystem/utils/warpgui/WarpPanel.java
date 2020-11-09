package net.nitrado.warpsystem.utils.warpgui;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.Page;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import net.nitrado.warpsystem.utils.warpgui.pages.SwitchPage;
import org.bukkit.entity.Player;

public class WarpPanel extends GUI {
    public static final String COLOR_NITRADO = "§x§F§F§D§7§4§4"; //#FFD744
    public static final String PERMISSION_FULL = "WarpPanel.Full";

    public WarpPanel(Player player) {
        super(player, WarpSystem.getInstance(), 54, COLOR_NITRADO + "Navigation");

        registerPage(null, true);
    }
}
