package de.codingair.warpsystem.spigot.features.signs.utils;

import de.codingair.warpsystem.spigot.base.WarpSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NBTHelper {
    private static NBTHelper instance;
    private final NamespacedKey world;
    private final NamespacedKey location;

    private NBTHelper() {
        world = new NamespacedKey(WarpSystem.getInstance(), "warpsign.world");
        location = new NamespacedKey(WarpSystem.getInstance(), "warpsign.location");
    }

    public static void applyNBT(ItemStack item, WarpSign sign) {
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(getInstance().world, PersistentDataType.STRING, sign.getLocation().getWorldName());
        meta.getPersistentDataContainer().set(getInstance().location, PersistentDataType.INTEGER_ARRAY, new int[]{sign.getLocation().getBlockX(), sign.getLocation().getBlockY(), sign.getLocation().getBlockZ()});

        item.setItemMeta(meta);
    }

    public static Location fromSign(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if(container.has(getInstance().world, PersistentDataType.STRING)) {
            String worldName = container.get(getInstance().world, PersistentDataType.STRING);
            int[] location = container.get(getInstance().location, PersistentDataType.INTEGER_ARRAY);

            World world = Bukkit.getWorld(worldName);
            if(world == null) return null;

            return new Location(world, location[0], location[1],location[2]);
        }

        return null;
    }

    public static NBTHelper getInstance() {
        if(instance == null) instance = new NBTHelper();
        return instance;
    }
}
