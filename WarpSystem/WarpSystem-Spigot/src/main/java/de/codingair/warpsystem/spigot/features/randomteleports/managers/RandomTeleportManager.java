package de.codingair.warpsystem.spigot.features.randomteleports.managers;

import com.google.common.base.Preconditions;
import de.codingair.codingapi.files.ConfigFile;
import de.codingair.codingapi.files.loader.UTFConfig;
import de.codingair.codingapi.tools.Callback;
import de.codingair.codingapi.tools.Location;
import de.codingair.codingapi.tools.io.ConfigMask;
import de.codingair.codingapi.tools.io.JSON.JSON;
import de.codingair.codingapi.tools.items.XMaterial;
import de.codingair.warpsystem.api.destinations.utils.Result;
import de.codingair.warpsystem.core.transfer.packets.spigot.QueueRTPUsagePacket;
import de.codingair.warpsystem.core.transfer.packets.spigot.RandomTPWorldsPacket;
import de.codingair.warpsystem.core.utils.Manager;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.listeners.SpawnLocationEvents;
import de.codingair.warpsystem.spigot.base.setupassistant.annotations.AvailableForSetupAssistant;
import de.codingair.warpsystem.spigot.base.setupassistant.annotations.Function;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.Permissions;
import de.codingair.warpsystem.spigot.base.utils.ProxyFeature;
import de.codingair.warpsystem.spigot.base.utils.money.Bank;
import de.codingair.warpsystem.spigot.base.utils.teleport.Origin;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.adapters.LocationAdapter;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.randomteleports.commands.CRandomTp;
import de.codingair.warpsystem.spigot.features.randomteleports.listeners.InteractListener;
import de.codingair.warpsystem.spigot.features.randomteleports.listeners.SpawnListener;
import de.codingair.warpsystem.spigot.features.randomteleports.utils.RandomLocationCalculator;
import de.codingair.warpsystem.spigot.features.randomteleports.utils.WorldOption;
import de.codingair.warpsystem.spigot.features.randomteleports.utils.forwardcompatibility.RTPTagConverter_v4_2_2;
import de.codingair.warpsystem.spigot.features.randomteleports.utils.forwardcompatibility.RTPTagConverter_v4_2_6;
import de.codingair.warpsystem.spigot.features.randomteleports.utils.forwardcompatibility.RTPTagConverter_v5_1_1;
import de.codingair.warpsystem.spigot.transfer.handlers.QueueRTPUsagePacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@AvailableForSetupAssistant (type = "Random teleports", config = "RTPConfig")
@Function (name = "Enabled", defaultValue = "true", config = "Config", configPath = "WarpSystem.Functions.RandomTeleports", clazz = Boolean.class)
@Function (name = "Protected regions", defaultValue = "true", configPath = "RandomTeleport.Support.ProtectedRegions", clazz = Boolean.class)
@Function (name = "World border", defaultValue = "true", configPath = "RandomTeleport.Support.WorldBorder", clazz = Boolean.class)
@Function (name = "Block blacklist", defaultValue = "true", configPath = "RandomTeleport.Block_Blacklist.Enabled", description = "§eBlocks §7» §6RTPConfig.yml", clazz = Boolean.class)
@Function (name = "Biome filter", defaultValue = "false", configPath = "RandomTeleport.Support.Biome.Enabled", description = "§eBiomes §7» §6RTPConfig.yml", clazz = Boolean.class)
@Function (name = "Max uses", defaultValue = "4", configPath = "RandomTeleport.Max", description = "§cONLY §rif permissions in the main §eConfig.yml §rare §cdisabled", clazz = Integer.class)
@Function (name = "Free uses", defaultValue = "1", configPath = "RandomTeleport.Free", description = "§cONLY §rif permissions in the main §eConfig.yml §rare §cdisabled", clazz = Integer.class)
@Function (name = "Concurrent teleports", defaultValue = "5", configPath = "RandomTeleport.Concurrent_Teleports", description = "§7Max amount of random teleports running §cconcurrently§7. Teleport will be queued when limit is reached.", clazz = Integer.class)
@Function (name = "PreLoading", defaultValue = "false", configPath = "RandomTeleport.PreLoading.Enabled", description = "§7Pre-loading of random teleport positions. Improves RTP search time significantly.", clazz = Boolean.class, since = "v5.1.3")
public abstract class RandomTeleportManager implements Manager, ProxyFeature {
    protected final List<Material> materialBlackList = new ArrayList<>();
    protected final List<WorldOption> worldOptions = new ArrayList<>();
    protected final HashMap<String, List<String>> worlds = new HashMap<>();
    protected final List<Location> interactBlocks = new ArrayList<>();
    protected final InteractListener listener = new InteractListener();

    protected final HashMap<Player, RandomLocationCalculator> calculators = new HashMap<>();
    protected final LinkedList<RandomLocationCalculator> queue = new LinkedList<>();
    protected final Set<RandomLocationCalculator> running = new HashSet<>();

    protected ConfigFile playData = null;

    protected boolean buyable;
    protected double costs;
    protected boolean protectedRegions;
    protected Set<Biome> biomeBlacklist;
    protected WorldOption defValues;
    protected int netherHeight;
    protected int endHeight;
    protected int max;
    protected int free;
    protected int concurrent;
    private RandomLocationCache cache;

    public static RandomTeleportManager getInstance() {
        return WarpSystem.getInstance().getDataManager().getManager(FeatureType.RANDOM_TELEPORTS);
    }

    @Override
    public void preLoad() {
        new RTPTagConverter_v4_2_2();
        new RTPTagConverter_v4_2_6();
        new RTPTagConverter_v5_1_1();
    }

    public abstract RandomLocationCalculator newCalculator(@Nullable Player player, org.bukkit.Location location, double minRange, double maxRange, Callback<RandomLocationCalculator> callback);

    protected RandomLocationCache newCache(boolean preloadingEnabled, int delay, Map<String, Integer> preloadOption) {
        return (player, target) -> CompletableFuture.completedFuture(null);
    }

    public RandomLocationCalculator newCalculator(@Nullable Player player, World world, Callback<RandomLocationCalculator> callback) {
        WorldOption option = getOption(world, defValues);

        org.bukkit.Location start = new Location();
        option.prepareStart(start, world);

        return newCalculator(player, start, option.getMin(), option.getMax(), callback);
    }

    @Override
    public boolean load(boolean hide) {
        this.playData = WarpSystem.getInstance().getFileManager().loadFile("PlayData", "/Memory/");

        ConfigFile rtpFile = WarpSystem.getInstance().getFileManager().loadFile("RTPConfig", "/");
        YamlConfiguration config = rtpFile.getConfig();

        if (!hide) WarpSystem.log("  > Loading RandomTeleports");

        this.buyable = config.getBoolean("RandomTeleport.Buyable.Enabled", true);
        this.costs = config.getDouble("RandomTeleport.Buyable.Costs", 500.0);

        this.concurrent = config.getInt("RandomTeleport.Concurrent_Teleports", 5);

        this.max = config.getInt("RandomTeleport.Max", 4);
        this.free = config.getInt("RandomTeleport.Free", 1);

        if (this.defValues != null) this.defValues.destroy();
        this.defValues = new WorldOption("§DEF§");
        ConfigMask w = new ConfigMask(rtpFile, "RandomTeleport.Worlds.Default");
        this.defValues.read(w);

        this.netherHeight = config.getInt("RandomTeleport.Range.Highest_Y.Nether", 126);
        this.endHeight = config.getInt("RandomTeleport.Range.Highest_Y.End", 72);

        this.materialBlackList.clear();
        if (config.getBoolean("RandomTeleport.Block_Blacklist.Enabled", false)) {
            for (String material : config.getStringList("RandomTeleport.Block_Blacklist.List")) {
                Optional<XMaterial> parsed = XMaterial.matchXMaterial(material.toUpperCase().replace(" ", "_"));
                parsed.ifPresent(xMaterial -> {
                    Material m = xMaterial.parseMaterial();

                    if (!materialBlackList.contains(m)) materialBlackList.add(m);
                });
            }
        }

        this.protectedRegions = config.getBoolean("RandomTeleport.Support.ProtectedRegions", true);
        if (config.getBoolean("RandomTeleport.Support.Biome.Enabled", true)) {
            List<String> configBiomes = config.getStringList("RandomTeleport.Support.Biome.Blacklist");
            biomeBlacklist = new HashSet<>();

            if (!configBiomes.isEmpty()) {
                for (String biome : configBiomes) {
                    Biome b = null;
                    for (Biome value : Biome.values()) {
                        if (value.name().equalsIgnoreCase(biome)) {
                            b = value;
                            break;
                        }
                    }

                    if (b == null) WarpSystem.getInstance().getLogger().warning(String.format("Could not find biome '%s' from RTPConfig.", biome));
                    else biomeBlacklist.add(b);
                }
            }
        }

        SpawnListener listener = new SpawnListener();
        Bukkit.getPluginManager().registerEvents(listener, WarpSystem.getInstance());
        SpawnLocationEvents.register(listener, WarpSystem.getInstance(), EventPriority.HIGHEST, false, listener::onSpawn);
        WarpSystem.getDataHandler().registerHandler(QueueRTPUsagePacket.class, new QueueRTPUsagePacketHandler());

        boolean success = true;
        worldOptions.clear();
        List<?> l = config.getList("RandomTeleport.Worlds.Options");
        if (l != null) {
            for (Object data : l) {
                try {
                    JSON json = new JSON((Map<?, ?>) data);
                    for (Object o : json.keySet(false)) {
                        String key = o + "";
                        WorldOption option = new WorldOption(key);
                        json.getSerializable(key, option);
                        worldOptions.add(option);
                    }
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                }
            }
        }

        if (!hide) WarpSystem.log("    ...got " + this.worldOptions.size() + " WorldOption(s)");

        boolean preloadingEnabled = config.getBoolean("RandomTeleport.PreLoading.Enabled");
        int preloadDelay = config.getInt("RandomTeleport.PreLoading.Begin_After_Startup");
        l = config.getList("RandomTeleport.PreLoading.Worlds");
        Map<String, Integer> preloadOptions = new HashMap<>();
        if (l != null) {
            for (Object data : l) {
                try {
                    JSON json = new JSON((Map<?, ?>) data);
                    for (Object o : json.keySet(false)) {
                        String worldName = o + "";
                        int preloadNumber = json.getInteger(worldName, 0);
                        if (preloadNumber > 0) preloadOptions.put(worldName, preloadNumber);
                    }
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                }
            }
        }
        cache = newCache(preloadingEnabled, preloadDelay, preloadOptions);
        if (!hide) WarpSystem.log("    ...preloading is " + (preloadingEnabled ? "enabled" : "disabled"));

        ConfigFile file = WarpSystem.getInstance().getFileManager().loadFile("Teleporters", "/Memory/");
        config = file.getConfig();

        l = config.getList("RandomTeleporter.InteractBlocks");
        if (l != null)
            for (Object s : l) {
                if (s instanceof Map) {
                    JSON json = new JSON((Map<?, ?>) s);
                    Location loc = new Location();
                    try {
                        loc.read(json);
                    } catch (Exception e) {
                        success = false;
                        e.printStackTrace();
                        continue;
                    }

                    this.interactBlocks.add(loc);
                } else if (s instanceof String) {
                    this.interactBlocks.add(Location.getByJSONString((String) s));
                }
            }

        Bukkit.getPluginManager().registerEvents(this.listener, WarpSystem.getInstance());
        new CRandomTp().register();

        if (!hide) WarpSystem.log("    ...got " + this.interactBlocks.size() + " InteractBlock(s)");
        WarpSystem.getInstance().getProxyFeatureList().add(this);

        return success;
    }

    @Override
    public void save(boolean saver) {
        ConfigFile file = WarpSystem.getInstance().getFileManager().getFile("Teleporters");
        YamlConfiguration config = file.getConfig();

        if (!saver) WarpSystem.log("  > Saving RandomTeleports");

        List<JSON> interactBlocks = new ArrayList<>();
        for (Location l : this.interactBlocks) {
            JSON json = new JSON();
            l.trim(0);
            l.write(json);
            interactBlocks.add(json);
        }

        config.set("RandomTeleporter.InteractBlocks", interactBlocks);
        file.saveConfig();
        if (!saver) WarpSystem.log("    ...saved " + interactBlocks.size() + " InteractBlock(s)");
    }

    @Override
    public void onConnect(Player connection) {
        List<String> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world == null) continue;

            WorldOption option = getOption(world, defValues);
            if (option.isDisabled()) continue;

            worlds.add(world.getName());
        }

        WarpSystem.getDataHandler().send(new RandomTPWorldsPacket(worlds), connection);
    }

    @Override
    public void onDisconnect() {
    }

    @Override
    public void destroy() {
        this.interactBlocks.clear();
        if (this.biomeBlacklist != null) this.biomeBlacklist.clear();
        this.materialBlackList.clear();
        HandlerList.unregisterAll(this.listener);
    }

    public boolean canBuy(Player player) {
        if (player.isOp()) return true;

        int bought = getInstance().getBoughtTeleports(player);
        int free = getInstance().getFreeTeleportAmount(player);
        int max = getInstance().getMaxTeleportAmount(player);

        return max == -1 || max - free - bought > 0;
    }

    public boolean canTeleport(Player player) {
        if (player.isOp()) return true;

        UUID u = WarpSystem.getInstance().getPlayerDataManager().get(player);
        int bought = getInstance().getBoughtTeleports(u);
        int teleports = getInstance().getTeleports(u);
        int free = getInstance().getFreeTeleportAmount(player);

        return free == -1 || free + bought - teleports > 0;
    }

    public int getMaxTeleportAmount(Player player) {
        if (player.isOp()) return -1;

        if (Permissions.PERMISSION_USE_RANDOM_TELEPORTER != null) {
            int amount = 0;
            for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
                if (!effectivePermission.getValue()) continue;
                String perm = effectivePermission.getPermission();

                if (perm.equals("*") || perm.toLowerCase().startsWith("warpsystem.*")
                        || perm.toLowerCase().startsWith("warpsystem.randomteleport.*")) return -1;

                if (perm.toLowerCase().startsWith("warpsystem.randomteleport.max.")) {
                    String s = perm.substring(30);
                    if (s.equals("*") || s.equalsIgnoreCase("n")) return -1;

                    try {
                        int i = Integer.parseInt(s);
                        if (i > amount) amount = i;
                    } catch (Throwable ignored) {
                    }
                }

            }

            return amount;
        } else return max;
    }

    public int getFreeTeleportAmount(Player player) {
        if (player.isOp()) return -1;

        if (Permissions.PERMISSION_USE_RANDOM_TELEPORTER != null) {
            int amount = 0;
            for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
                if (!effectivePermission.getValue()) continue;
                String perm = effectivePermission.getPermission();

                if (perm.equals("*") || perm.toLowerCase().startsWith("warpsystem.*")
                        || perm.toLowerCase().startsWith("warpsystem.randomteleport.*")) return -1;

                if (perm.toLowerCase().startsWith("warpsystem.randomteleport.free.")) {
                    String s = perm.substring(31);
                    if (s.equals("*") || s.equalsIgnoreCase("n")) return -1;

                    try {
                        int i = Integer.parseInt(s);
                        if (i > amount) amount = i;
                    } catch (Throwable ignored) {
                    }
                }
            }
            return amount;
        } else return free;
    }

    public WorldOption getOption(World world, WorldOption def) {
        for (WorldOption worldOption : this.worldOptions) {
            if (worldOption.getWorldName().equalsIgnoreCase(world.getName())) return worldOption;
        }

        return def;
    }

    public void tryToTeleport(Player player) {
        tryToTeleport(player.getName(), player.getWorld(), false, new Callback<Integer>() {
            @Override
            public void accept(Integer object) {
            }
        });
    }

    public void search(Player player, World target, WorldOption option, Callback<Location> callback) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(option);

        if (option.isDisabled()) {
            callback.accept(null);
            return;
        }

        cache.getAsync(player, target).thenAccept(preloaded -> {
            if (preloaded != null) {
                callback.accept(preloaded);
                return;
            }

            org.bukkit.Location start = new Location();
            option.prepareStart(start, target);

            RandomLocationCalculator t = newCalculator(player, start, option.getMin(), option.getMax(), new Callback<RandomLocationCalculator>() {
                @Override
                public void accept(RandomLocationCalculator t) {
                    Location result = t.getResult();

                    synchronized (running) {
                        running.remove(t);

                        RandomLocationCalculator next = queue.poll();
                        if (next != null) {
                            running.add(next);
                            Bukkit.getScheduler().runTaskAsynchronously(WarpSystem.getInstance(), next);
                        }
                    }

                    if (result != null) {
                        result.setYaw(player.getLocation().getYaw());
                        result.setPitch(player.getLocation().getPitch());
                    }

                    calculators.remove(player);
                    callback.accept(result);
                }
            });

            calculators.put(player, t); //register

            synchronized (running) {
                if (running.size() >= concurrent) queue.add(t);
                else {
                    running.add(t);
                    Bukkit.getScheduler().runTaskAsynchronously(WarpSystem.getInstance(), t);
                }
            }
        });
    }

    public void tryToTeleport(String targetPlayer, World target, boolean force, Callback<Integer> callback) {
        Player player = Bukkit.getPlayerExact(targetPlayer);

        if (player == null) {
            callback.accept(1);
            return;
        }

        RandomLocationCalculator c;
        if ((c = calculators.get(player)) != null) {
            if (System.currentTimeMillis() - c.getLastReaction() > 5000) {
                calculators.remove(player);
                player.sendMessage(Lang.getPrefix() + Lang.get("RandomTP_No_Location_Found"));
            } else player.sendMessage(Lang.getPrefix() + Lang.get("RandomTP_Already_Searching"));

            callback.accept(-1);
            return;
        }

        if (!canTeleport(player) && !force) {
            player.sendMessage(Lang.getPrefix() + Lang.get("RandomTP_No_Teleports_Left"));
            callback.accept(4);
            return;
        }

        WorldOption option = getOption(target, defValues);

        if (option.isDisabled()) {
            player.sendMessage(Lang.getPrefix() + Lang.get("RTP_Not_available_in_this_world"));
            return;
        }

        search(player, target, option, new Callback<Location>() {
            @Override
            public void accept(Location loc) {
                if (loc == null) {
                    //no location found, try again
                    player.sendMessage(Lang.getPrefix() + Lang.get("RandomTP_No_Location_Found"));
                    callback.accept(2);
                } else {
                    //teleported
                    UUID uuid = WarpSystem.getInstance().getPlayerDataManager().get(player);
                    if (!player.isOp()) increaseTeleports(uuid);

                    Bukkit.getScheduler().runTask(WarpSystem.getInstance(), () -> {
                        TeleportOptions options = new TeleportOptions(new Destination(new LocationAdapter(loc)), "", Origin.RandomTP);
                        options.setMessage(Lang.getPrefix() + Lang.get("RandomTP_Teleported"));
                        options.setSkip(true);
                        options.addCallback(new Callback<Result>() {
                            @Override
                            public void accept(Result object) {
                                callback.accept(0);
                            }
                        });

                        WarpSystem.getInstance().getTeleportManager().teleport(player, options);
                    });
                }
            }
        });

        player.sendMessage(Lang.getPrefix() + Lang.get("RandomTP_Searching"));
    }

    public void increaseTeleports(UUID uuid) {
        YamlConfiguration config = playData.getConfig();
        int i = config.getInt("RandomTeleporter." + uuid.toString() + ".Teleports", 0) + 1;
        config.set("RandomTeleporter." + uuid + ".Teleports", i);
        playData.saveConfig();
    }

    public int getTeleports(Player player) {
        return getTeleports(WarpSystem.getInstance().getPlayerDataManager().get(player));
    }

    public int getTeleports(UUID uuid) {
        return playData.getConfig().getInt("RandomTeleporter." + uuid.toString() + ".Teleports", 0);
    }

    public void setBoughtTeleports(UUID uuid, int teleports) {
        YamlConfiguration config = playData.getConfig();
        config.set("RandomTeleporter." + uuid.toString() + ".Bought", teleports);
        playData.saveConfig();
    }

    public int getBoughtTeleports(Player player) {
        return getBoughtTeleports(WarpSystem.getInstance().getPlayerDataManager().get(player));
    }

    public int getBoughtTeleports(UUID uuid) {
        YamlConfiguration config = playData.getConfig();
        return config.getInt("RandomTeleporter." + uuid.toString() + ".Bought", 0);
    }

    public void updateWorlds(HashMap<String, List<String>> data) {
        this.worlds.putAll(data);
    }

    public List<String> getWorlds(@NotNull String server) {
        return this.worlds.getOrDefault(server.toLowerCase(), new ArrayList<>());
    }

    public double getCosts() {
        return costs;
    }

    public WorldOption getDefValues() {
        return defValues;
    }

    public boolean isProtectedRegions() {
        return protectedRegions;
    }

    public Set<Biome> getBiomeBlacklist() {
        return biomeBlacklist;
    }

    public List<Location> getInteractBlocks() {
        return interactBlocks;
    }

    public InteractListener getListener() {
        return listener;
    }

    public boolean isBuyable() {
        return buyable && Bank.isReady();
    }

    public int getNetherHeight() {
        return netherHeight;
    }

    public int getEndHeight() {
        return endHeight;
    }

    public List<Material> getMaterialBlackList() {
        return materialBlackList;
    }

    public boolean hasRegisteredServers() {
        return !this.worlds.isEmpty();
    }

    public Set<String> getServer() {
        return this.worlds.keySet();
    }
}
