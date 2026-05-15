package de.codingair.warpsystem.spigot.features.spawn.managers;

import de.codingair.codingapi.files.ConfigFile;
import de.codingair.codingapi.tools.io.ConfigMask;
import de.codingair.warpsystem.core.transfer.packets.general.SendGlobalSpawnOptionsPacket;
import de.codingair.warpsystem.core.utils.Manager;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.listeners.SpawnLocationEvents;
import de.codingair.warpsystem.spigot.base.setupassistant.annotations.AvailableForSetupAssistant;
import de.codingair.warpsystem.spigot.base.setupassistant.annotations.Function;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.WarpAction;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.adapters.LocationAdapter;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.spawn.commands.CSetSpawn;
import de.codingair.warpsystem.spigot.features.spawn.commands.CSpawn;
import de.codingair.warpsystem.spigot.features.spawn.listeners.SpawnListener;
import de.codingair.warpsystem.spigot.features.spawn.utils.Spawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;

import java.io.File;
import java.util.Objects;

@AvailableForSetupAssistant (type = "Spawn", config = "Config")
@Function (name = "Enabled", defaultValue = "false", config = "Config", configPath = "WarpSystem.Functions.Spawn", clazz = Boolean.class)
@Function (name = "Teleport message", defaultValue = "true", config = "Config", configPath = "WarpSystem.Send.Teleport_Message.Spawn", clazz = Boolean.class)
public class SpawnManager implements Manager {
    private String spawnServerCommand = null, respawnServerCommand = null;
    private boolean spawnServerProxy = false;
    private Spawn spawn;

    public static SpawnManager getInstance() {
        return WarpSystem.getInstance().getDataManager().getManager(FeatureType.SPAWN);
    }

    @Override
    public boolean load(boolean loader) {
        ConfigFile file = WarpSystem.getInstance().getFileManager().loadFile("Teleporters", "/Memory/");

        spawn = new Spawn();
        if (file.getConfig().contains("Spawn")) {
            ConfigMask reader = new ConfigMask(file);
            reader.getSerializable("Spawn", this.spawn);
        }

        if (spawn.getLocation() == null) {
            //import spawn
            Location l = readEssentialsSpawn();
            if (l != null) this.spawn.addAction(new WarpAction(new Destination(new LocationAdapter(l))));
            else this.spawn.addAction(new WarpAction(new Destination(new LocationAdapter(Bukkit.getWorlds().get(0).getSpawnLocation()))));
        }

        SpawnListener listener = new SpawnListener();
        Bukkit.getPluginManager().registerEvents(listener, WarpSystem.getInstance());
        SpawnLocationEvents.register(listener, WarpSystem.getInstance(), EventPriority.HIGH, false, listener::onSpawn);

        new CSetSpawn().register();
        new CSpawn().register();
        return true;
    }

    private Location readEssentialsSpawn() {
        try {
            File target = new File(WarpSystem.getInstance().getDataFolder().getParent() + "/Essentials/spawn.yml");
            if (!target.exists()) return null;

            FileConfiguration config = YamlConfiguration.loadConfiguration(target);
            String world = config.getString("spawns.default.world");
            if (world == null) return null;

            return new de.codingair.codingapi.tools.Location(world,
                    config.getDouble("spawns.default.x"),
                    config.getDouble("spawns.default.y"),
                    config.getDouble("spawns.default.z"),
                    (float) config.getDouble("spawns.default.yaw"),
                    (float) config.getDouble("spawns.default.pitch")
            );
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void save(boolean saver) {
        if (this.spawn != null) {
            ConfigFile file = WarpSystem.getInstance().getFileManager().getFile("Teleporters");
            ConfigMask writer = new ConfigMask(file);
            writer.put("Spawn", this.spawn);
            file.saveConfig();
        }
    }

    @Override
    public void destroy() {
        if (this.spawn != null) this.spawn.destroy();
    }

    public Spawn getSpawn() {
        return spawn;
    }

    public void updateSpawn(Location location) {
        if (this.spawn == null) this.spawn = new Spawn();
        this.spawn.addAction(new WarpAction(new Destination(new LocationAdapter(location))));
    }

    public boolean isSpawnServerProxy() {
        return spawnServerProxy;
    }

    public String getSpawnServerCommand() {
        return spawnServerCommand;
    }

    public void updateGlobalOptions(boolean spawnServerProxy, String spawnServerCommand, String respawnServerCommand, Player connection) {
        if (!Objects.equals(this.spawnServerProxy, spawnServerProxy) || !Objects.equals(this.spawnServerCommand, spawnServerCommand) || !Objects.equals(this.respawnServerCommand, respawnServerCommand)) {
            this.spawnServerProxy = spawnServerProxy;
            this.spawnServerCommand = spawnServerCommand;
            this.respawnServerCommand = respawnServerCommand;

            WarpSystem.getDataHandler().send(new SendGlobalSpawnOptionsPacket(spawnServerProxy, spawnServerCommand, respawnServerCommand), connection);
        }
    }

    public void applyGlobalOptions(String spawn, String respawn) {
        String s = WarpSystem.getInstance().getCurrentServer();

        if (this.spawn != null && this.spawn.getUsage().isBungee() && !Objects.equals(s, spawn)) this.spawn.setUsage(this.spawn.getUsage().getLocal());
        if (this.spawn != null && this.spawn.getRespawnUsage().isBungee() && !Objects.equals(s, respawn)) this.spawn.setRespawnUsage(this.spawn.getRespawnUsage().getLocal());

        this.spawnServerCommand = spawn;
        this.respawnServerCommand = respawn;
    }

    public String getRespawnServerCommand() {
        return respawnServerCommand;
    }
}
