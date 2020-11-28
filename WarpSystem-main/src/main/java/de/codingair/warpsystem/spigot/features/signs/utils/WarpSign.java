package de.codingair.warpsystem.spigot.features.signs.utils;

import de.codingair.codingapi.tools.Location;
import de.codingair.codingapi.tools.io.lib.JSONArray;
import de.codingair.codingapi.tools.io.utils.DataWriter;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.FeatureObject;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.WarpAction;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.DestinationType;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.warps.managers.IconManager;
import de.codingair.warpsystem.spigot.features.warps.nextlevel.utils.Icon;
import org.bukkit.block.Sign;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WarpSign extends FeatureObject {
    private boolean editing = false;
    private Location location;
    private String[] text = null;

    public WarpSign() {
    }

    public WarpSign(WarpSign sign) {
        super(sign);
        this.location = sign.location;
        this.text = sign.text == null ? null : sign.text.clone();
    }

    public WarpSign(Location location, Destination destination) {
        this(location, destination, null);
    }

    public WarpSign(Location location, Destination destination, String permission) {
        super(permission, false, new WarpAction(destination));
        this.location = location;
    }

    public void editMode() {
        if(this.text != null) {
            Sign s = (Sign) location.getBlock().getState();

            for(int i = 0; i < this.text.length; i++) {
                s.setLine(i, text[i]);
            }

            s.update(true);
        }
    }

    public void update() {
        if(location == null || location.getWorld() == null || text == null) return;

        if(location.getBlock().getState() instanceof Sign) {
            Sign s = (Sign) location.getBlock().getState();

            for(int i = 0; i < this.text.length; i++) {
                s.setLine(i, prepareLine(text[i]));
            }

            s.update(true);
        }
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean read(DataWriter d) throws Exception {
        super.read(d);

        if(d.get("Loc") != null) {
            this.location = Location.getByJSONString(d.getRaw("Loc"));
        } else if(d.get("location") != null) {
            this.location = d.getLocation("location");
        }

        if(d.get("Destination") != null) {
            //New pattern
            Destination destination = new Destination((String) d.get("Destination"));
            addAction(new WarpAction(destination));
        } else if(d.get("Warp") != null) {
            //Old pattern
            Icon warp = ((IconManager) WarpSystem.getInstance().getDataManager().getManager(FeatureType.WARP_GUI)).getIcon(d.get("Warp"));
            if(warp != null) {
                Destination destination = new Destination(warp.getName(), DestinationType.SimpleWarp);
                addAction(new WarpAction(destination));
            }
        }

        if(d.get("Permissions") != null) {
            setPermission(d.get("Permissions"));
        }

        List<String> text = d.getList("text");
        if(text == null || text.isEmpty()) this.text = ((Sign) (location.getBlock().getState())).getLines();
        else this.text = text.toArray(new String[4]);
        repairLines();

        return true;
    }

    private void repairLines() {
        int empty = 0;

        for(String s : this.text) {
            if(s == null || s.isEmpty()) empty++;
        }

        if(empty == 4) {
            this.text = ((Sign) (location.getBlock().getState())).getLines();
        }
    }

    @Override
    public void write(DataWriter d) {
        super.write(d);

        this.location.trim(0);
        d.put("location", this.location);

        JSONArray list = new JSONArray();
        for(String s : this.text) {
            if(s == null) list.add("");
            else list.add(s);
        }
        d.put("text", list);
    }

    @Override
    public void destroy() {
        super.destroy();
        this.location = null;
        this.text = null;
    }

    @Override
    public void apply(FeatureObject object) {
        super.apply(object);

        WarpSign sign = (WarpSign) object;
        this.location = sign.location;
        this.text = sign.text;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        if(!super.equals(o)) return false;
        WarpSign warpSign = (WarpSign) o;
        return Objects.equals(location, warpSign.location) &&
                Arrays.equals(text, warpSign.text);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(location);
        result = 31 * result + Arrays.hashCode(text);
        return result;
    }

    public WarpSign clone() {
        return new WarpSign(this);
    }

    public WarpSign cloneAt(Location location) {
        WarpSign sign = new WarpSign(this);
        sign.location = location;
        return sign;
    }

    public String[] getText() {
        return text;
    }

    public void setText(String[] text) {
        this.text = text;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }
}
