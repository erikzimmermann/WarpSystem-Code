package de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types;

import de.codingair.codingapi.server.sounds.Sound;
import de.codingair.codingapi.server.sounds.SoundData;
import de.codingair.codingapi.tools.io.utils.DataMask;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.guis.editor.pages.SoundPage;
import de.codingair.warpsystem.spigot.base.utils.SoundUtil;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.Action;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.ActionObject;
import org.bukkit.entity.Player;

public class SoundAction extends ActionObject<SoundData> {
    public SoundAction(SoundData value) {
        super(Action.SOUND, value);
    }

    public SoundAction() {
        this(null);
    }

    @Override
    public boolean perform(Player player) {
        if (getValue() == null) {
            WarpSystem.getInstance().getLogger().warning("Tried to play a null SoundData in SoundAction.perform");
            return false;
        }
        Sound s = getValue().getSound();
        WarpSystem.getInstance().getLogger().info("Playing sound via SoundAction: " + (s == null ? "null" : s.name()) + ", vol=" + getValue().getVolume() + ", pitch=" + getValue().getPitch());
        SoundUtil.play(player, getValue());
        return true;
    }

    @Override
    public ActionObject<SoundData> clone() {
        return new SoundAction(new SoundData(getValue().getSound(), getValue().getVolume(), getValue().getPitch()));
    }

    @Override
    public boolean usable() {
        return getValue() != null && getValue().getSound() != null && !SoundPage.isStandardSound(getValue());
    }

    @Override
    public boolean read(DataMask d) throws Exception {
        String soundName = d.getString("sound", "ENDERMAN_TELEPORT");
        Sound s = safeValueOfSound(soundName);
        setValue(new SoundData(s, d.getFloat("volume"), d.getFloat("pitch")));
        return true;
    }

    private Sound safeValueOfSound(String name) {
        if (name == null) return Sound.ENTITY_ENDERMAN_TELEPORT;
        try {
            return de.codingair.codingapi.server.sounds.Sound.valueOf(name);
        } catch (Exception ex) {
            // try some normalizations
            String n = name.toUpperCase().replace('.', '_').replace('-', '_');
            if (n.contains(":")) n = n.substring(n.indexOf(":") + 1);
            try {
                return Sound.valueOf(n);
            } catch (Exception ex2) {
                return Sound.ENTITY_ENDERMAN_TELEPORT;
            }
        }
    }

    @Override
    public void write(DataMask d) {
        d.put("sound", getValue() == null ? null : getValue().getSound() == null ? null : getValue().getSound().name());
        d.put("volume", getValue() == null ? null : getValue().getVolume());
        d.put("pitch", getValue() == null ? null : getValue().getPitch());
    }
}
