package net.nitrado.warpsystem.utils.warpgui;

import de.codingair.codingapi.player.gui.inventory.v2.GUI;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.AlreadyOpenedException;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.IsWaitingException;
import de.codingair.codingapi.player.gui.inventory.v2.exceptions.NoPageException;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import net.nitrado.warpsystem.utils.warpgui.pages.*;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class WarpPanel extends GUI {
    public static final String COLOR_NITRADO = "§x§F§F§D§7§4§4"; //#FFD744
    public static final String PERMISSION_FULL = "WarpPanel.Full";

    public WarpPanel(Player player) {
        super(player, WarpSystem.getInstance(), 45, "Navigation");

        BasePage base = new BasePage(this);
        registerPage(new JnRPage(this, base), false);
        registerPage(new ServerPage(this, base), false);
        registerPage(new ArcadePage(this, base), false);
        registerPage(new SwitchPage(this), true);
    }

    @Override
    public void open() throws AlreadyOpenedException, NoPageException, IsWaitingException {
        super.open();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1F);
    }
}
