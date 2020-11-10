package de.codingair.warpsystem.spigot.features.signs.guis;

import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.warpsystem.spigot.base.guis.editor.Backup;
import de.codingair.warpsystem.spigot.base.guis.editor.Editor;
import de.codingair.warpsystem.spigot.base.guis.editor.pages.DestinationPage;
import de.codingair.warpsystem.spigot.base.guis.editor.pages.SoundPage;
import de.codingair.warpsystem.spigot.base.language.Lang;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.SoundAction;
import de.codingair.warpsystem.spigot.base.utils.teleport.Origin;
import de.codingair.warpsystem.spigot.features.signs.guis.pages.OptionPage;
import de.codingair.warpsystem.spigot.features.signs.managers.SignManager;
import de.codingair.warpsystem.spigot.features.signs.utils.WarpSign;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WarpSignGUI extends Editor<WarpSign> {

    public WarpSignGUI(Player p, WarpSign sign) {
        this(p, sign, sign.clone().createDestinationIfAbsent().createTeleportSoundIfAbsent());
    }

    private WarpSignGUI(Player p, WarpSign sign, WarpSign clone) {
        super(p, clone, new Backup<WarpSign>(sign) {
                    @Override
                    public void applyTo(WarpSign clone) {
                        sign.apply(clone);

                        if(SignManager.getInstance().getByLocation(sign.getLocation()) == null) {
                            SignManager.getInstance().addWarpSign(sign);
                        }

                        Sign s = (Sign) sign.getLocation().getBlock().getState();
                        sign.setText(s.getLines());
                        sign.setEditing(false);
                        sign.update();
                    }

                    @Override
                    public void cancel(WarpSign value) {
                        sign.setEditing(false);
                        sign.update();
                    }
                }, new ShowIcon(clone),
                new OptionPage(p, clone),
                new DestinationPage(p, getMainTitle(), clone.getDestination(), Origin.WarpSign),
                new SoundPage(p, getMainTitle(), clone.getAction(SoundAction.class).getValue())
        );
    }

    public static String getMainTitle() {
        return Editor.TITLE_COLOR + Lang.get("WarpSigns");
    }

    public static class ShowIcon implements de.codingair.warpsystem.spigot.base.guis.editor.ShowIcon {
        private final WarpSign warpSign;
        private final Sign sign;
        private String[] lines;

        public ShowIcon(WarpSign warpSign) {
            this.warpSign = warpSign;
            this.sign = (Sign) warpSign.getLocation().getBlock().getState();
            this.lines = this.sign.getLines();
        }

        public void applyLines(String[] lines) {
            this.lines = lines;
        }

        @Override
        public ItemStack buildIcon() {
            ItemBuilder builder = new ItemBuilder(getType());

            for(String line : lines) {
                builder.addText("§7'§f" + warpSign.prepareLine(line) + "§7'");
            }

            return builder.getItem();
        }

        private Material getType() {
            if(sign.getType().name().contains("WALL_")) {
                return Material.valueOf(sign.getType().name().replace("WALL_", ""));
            } else return sign.getType();
        }
    }
}
