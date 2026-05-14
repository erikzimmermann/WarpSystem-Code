package de.codingair.warpsystem.spigot.base;

import de.codingair.codingapi.API;
import de.codingair.codingapi.files.ConfigFile;
import de.codingair.codingapi.files.FileManager;
import de.codingair.codingapi.files.loader.UTFConfig;
import de.codingair.codingapi.nms.NmsCheck;
import de.codingair.codingapi.server.reflections.IReflection;
import de.codingair.codingapi.server.specification.Type;
import de.codingair.codingapi.server.specification.Version;
import de.codingair.packetmanagement.utils.Proxy;
import de.codingair.warpsystem.api.destinations.utils.Result;
import de.codingair.warpsystem.core.transfer.packets.proxy.SendJarPacket;
import de.codingair.warpsystem.core.transfer.packets.proxy.SetupAssistantStorePacket;
import de.codingair.warpsystem.core.transfer.packets.spigot.RequestInitialPacket;
import de.codingair.warpsystem.core.utils.Manager;
import de.codingair.warpsystem.spigot.api.SpigotAPI;
import de.codingair.warpsystem.spigot.api.events.FakeBlockBreakEvent;
import de.codingair.warpsystem.spigot.api.placeholders.PAPI;
import de.codingair.warpsystem.spigot.base.commands.CWarpSystem;
import de.codingair.warpsystem.spigot.base.listeners.*;
import de.codingair.warpsystem.spigot.base.managers.*;
import de.codingair.warpsystem.spigot.base.setupassistant.SetupAssistantManager;
import de.codingair.warpsystem.spigot.base.setupassistant.utils.SetupAssistantListener;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.Permissions;
import de.codingair.warpsystem.spigot.base.utils.ProxyFeature;
import de.codingair.warpsystem.spigot.base.utils.forwardcompatibility.ConfigTagConverter_v4_2_12;
import de.codingair.warpsystem.spigot.base.utils.options.OptionBundle;
import de.codingair.warpsystem.spigot.base.utils.options.Options;
import de.codingair.warpsystem.spigot.base.utils.options.specific.GeneralOptions;
import de.codingair.warpsystem.spigot.base.utils.options.specific.PortalOptions;
import de.codingair.warpsystem.spigot.base.utils.options.specific.WarpGUIOptions;
import de.codingair.warpsystem.spigot.base.utils.options.specific.WarpSignOptions;
import de.codingair.warpsystem.spigot.base.utils.updates.UpdateNotifier;
import de.codingair.warpsystem.spigot.base.utils.updates.UpdateReader;
import de.codingair.warpsystem.spigot.transfer.JarReceiver;
import de.codingair.warpsystem.spigot.transfer.SpigotHandler;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Level;

public class WarpSystem extends JavaPlugin implements Proxy {
    public static boolean activated = false;
    public static boolean updateAvailable = false;
    private static WarpSystem instance;
    private final Set<ProxyFeature> proxyFeatureList = new HashSet<>();
    private final TeleportManager teleportManager = TeleportManager.getInstance();
    private final FileManager fileManager = new FileManager(this);
    private final HeadManager headManager = new HeadManager();
    private final SetupAssistantManager setupAssistantManager = new SetupAssistantManager();
    private SpigotHandler dataHandler;
    private PlayerDataManager playerDataManager;
    private CooldownManager cooldownManager;
    private OptionBundle options;
    private GeneralOptions generalOptions;
    private boolean useProxy = false;
    private boolean ignoreProxyDetection = false;
    private boolean connectedProxy = false;
    private String proxyPluginVersion = null;
    private String server = null;
    private DataManager dataManager;
    private ServerManager serverManager;
    private UpdateNotifier updateNotifier;
    private boolean old = false;
    private boolean ERROR = true;
    private boolean workingNms = false;
    private boolean shouldSave = true;
    private String oldVersion = null;
    private UTFConfig oldConfig = null;

    public static void updateCommandList() {
        if (Version.after(12)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                IReflection.MethodAccessor updateCommands = IReflection.getMethod(Player.class, "updateCommands");
                updateCommands.invoke(player);
            }
        }
    }

    public static WarpSystem getInstance() {
        return instance;
    }

    public static void log(String message) {
        getInstance().getLogger().info(message);
    }

    public static <E extends Options> E getOptions(Class<? extends E> clazz) {
        if (instance == null) return null;

        for (Options option : getInstance().options.getOptions()) {
            if (option.getClass().equals(clazz)) return (E) option;
        }

        return null;
    }

    public static GeneralOptions opt() {
        return getInstance().generalOptions;
    }

    public static SpigotHandler getDataHandler() {
        return getInstance().dataHandler;
    }

    public static CooldownManager cooldown() {
        return getInstance().cooldownManager;
    }

    @Override
    public void onEnable() {
        if (!checkServerVersion()) return;
        checkNms();

        long start = System.currentTimeMillis();

        instance = this;
        copyConfig();
        preload();

        try {
            API.getInstance().onEnable(this);
            SpigotAPI.getInstance().onEnable(this);

            log(" ");
            log("__________________________________________________________");
            log(" ");
            log("                       WarpSystem [" + getDescription().getVersion() + "]");
            log(" ");
            log("Status:");
            log(" ");
            log("MC-Version: " + Version.fullVersion());
            log(" ");

            ConfigFile config = this.fileManager.loadFile("Config", "/");

            this.useProxy = config.getConfig().getBoolean("WarpSystem.Proxy.Enabled", false);
            this.ignoreProxyDetection = config.getConfig().getBoolean("WarpSystem.Proxy.Ignore_Proxy_Detection", false);
            this.dataHandler = new SpigotHandler(this);
            this.playerDataManager = new PlayerDataManager();
            this.serverManager = new ServerManager();

            this.dataManager = new DataManager();
            this.dataManager.preLoad();

            // make sure to have the config loaded (might be unloaded during preloading)
            this.fileManager.loadFile("Config", "/");

            this.updateNotifier = new UpdateNotifier();
            loadOptions();
            checkOldDirectory();

            Lang.initPreDefinedLanguages(this);

            //load cooldown list
            cooldownManager = new CooldownManager();
            cooldownManager.load();

            //check permission before loading features
            Permissions.checkPermissions();

            new PostWorldManager();

            // load heads before using them
            headManager.onEnable();

            log("Loading features");
            this.dataManager.removeDisabled();

            CWarpSystem cWarpSystem = new CWarpSystem();
            cWarpSystem.register();

            boolean loadingFailed = !this.dataManager.load();
            log(" ");
            log("Loading TeleportManager");
            if (!this.teleportManager.load()) loadingFailed = true;

            oldVersion = config.getConfig().getString("Do_Not_Edit.Last_Version", "0");
            checkBackup(config, loadingFailed);

            String currentVersion = getDescription().getVersion();
            if (!currentVersion.equals(oldVersion)) {
                config.getConfig().set("Do_Not_Edit.Last_Version", currentVersion);
                config.saveConfig();
            }

            Bukkit.getPluginManager().registerEvents(new PlayerDataListener(), this);
            Bukkit.getPluginManager().registerEvents(new TeleportListener(), this);
            Bukkit.getPluginManager().registerEvents(new NotifyListener(), this);
            Bukkit.getPluginManager().registerEvents(new CommandListener(), this);

            dataHandler.onEnable();
            dataHandler.registerHandler(SendJarPacket.class, new JarReceiver());

            Bukkit.getPluginManager().registerEvents(playerDataManager, this);

            Bukkit.getPluginManager().registerEvents(new HeadListener(), this);
            SetupAssistantListener l = new SetupAssistantListener();
            Bukkit.getPluginManager().registerEvents(l, this);
            dataHandler.registerHandler(SetupAssistantStorePacket.class, l);

            this.startAutoSaver();
            afterOnEnable();

            log(" ");
            log("Finished (" + (System.currentTimeMillis() - start) + "ms)");
            log(" ");
            log("__________________________________________________________");
            log(" ");

            PAPI.register();
            PaperLib.suggestPaper(this);

            activated = true;
            if (config.getConfig().getBoolean("WarpSystem.Update_Notifier", true)) UpdateReader.start();

            this.ERROR = false;

            if (!Bukkit.getOnlinePlayers().isEmpty()) this.dataHandler.send(new RequestInitialPacket(), null);
            BungeeBukkitListener packetListener = new BungeeBukkitListener();
            Bukkit.getPluginManager().registerEvents(packetListener, this);

            if (config.getConfig().getBoolean("WarpSystem.Functions.CommandBlocks", true))
                Bukkit.getPluginManager().registerEvents(new CommandBlockListener(), this);
        } catch (Throwable ex) {
            createErrorReport(ex);

            log(" ");
            log("__________________________________________________________");
            log(" ");
            log("                       WarpSystem [" + getDescription().getVersion() + "]");
            log(" ");
            log("       COULD NOT ENABLE CORRECTLY!!");
            log(" ");
            log("       Please contact the author with the ErrorReport.txt");
            log("       file in the plugins/WarpSystem folder.");
            log(" ");
            log(" ");
            log("       Thanks for supporting!");
            log(" ");
            log("__________________________________________________________");
            log(" ");

            this.ERROR = true;
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (!workingNms) {
            getLogger().log(Level.SEVERE, "This Minecraft version appears to be unsupported. Ensure your server .jar file is up to date. If it is, please contact the author with the error message above.");
            getLogger().log(Level.SEVERE, "Here's an invitation to the discord for support: https://discord.gg/DxKMcGjQbp");
            return;
        }
        if (Version.type() == Type.BUKKIT) return;

        API.getInstance().onDisable(this);
        SpigotAPI.getInstance().onDisable();

        setupAssistantManager.onDisable();

        save(false);
        teleportManager.getTeleports().forEach(t -> t.cancel(Result.CANCELLED_BY_SYSTEM));
        teleportManager.clear();

        //Disable all functions
        activated = false;
        connectedProxy = false;
        server = null;
        updateAvailable = false;
        old = false;
        ERROR = true;
        workingNms = false;
        shouldSave = true;
        if (playerDataManager != null) playerDataManager.flush();

        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);

        this.proxyFeatureList.forEach(ProxyFeature::onDisconnect);
        this.proxyFeatureList.clear();

        this.dataHandler.flush();
        dataHandler.onDisable();

        destroy();
    }

    private void checkNms() {
        NmsCheck.test(new Class[]{
                FakeBlockBreakEvent.class
        });
        workingNms = true;
    }

    private void checkBackup(ConfigFile config, boolean createBackup) {
        if (config.getConfig().getBoolean("WarpSystem.Backups", true)) {
            if (oldVersion == null || !oldVersion.equals(getDescription().getVersion())) createBackup();
            else if (createBackup) {
                log(" ");
                log(" ");
                log("Loading with errors > Create backup...");
                if (oldVersion.equals(getDescription().getVersion())) createBackup();
                log("Backup successfully created");
                log(" ");
            }
        }
    }

    private void createErrorReport(Throwable ex) {
        //make error-report

        if (!getDataFolder().exists()) {
            try {
                getDataFolder().createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedWriter writer = null;
        try {
            File log = new File(getDataFolder(), "ErrorReport.txt");
            if (log.exists()) log.delete();

            writer = new BufferedWriter(new FileWriter(log));

            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void preload() {
        new ConfigTagConverter_v4_2_12();
    }

    private void copyConfig() {
        ConfigFile file = this.fileManager.loadFile("Config", "/", false);

        IReflection.FieldAccessor<Map<String, Object>> map = IReflection.getField(MemorySection.class, "map");
        Map<String, Object> copy = new HashMap<>(map.get(file.getConfig()));

        this.oldConfig = (UTFConfig) IReflection.getConstructor(UTFConfig.class).newInstance();
        map.set(oldConfig, copy);

        this.fileManager.unloadFile(file);
    }

    private void afterOnEnable() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            //update command dispatcher for players to synchronize CommandList
            Bukkit.getScheduler().runTask(this, WarpSystem::updateCommandList);
        }, 1);
    }

    private boolean checkServerVersion() {
        if (Version.type() == Type.BUKKIT) {
            shouldSave = false;
            getLogger().info("====================");
            getLogger().log(Level.SEVERE, "This plugin requires at least a Spigot server!");
            getLogger().log(Level.SEVERE, "A fork of Paper does also work.");
            Bukkit.getPluginManager().disablePlugin(this);
            getLogger().info("====================");

            return false;
        }

        return true;
    }

    private void loadOptions() {
        if (this.options == null) this.options = new OptionBundle(generalOptions = new GeneralOptions(), new WarpGUIOptions(), new WarpSignOptions(), new PortalOptions());
        this.options.read();
        for (Options option : this.options.getOptions()) {
            option.write();
        }
    }

    public void reload(boolean save) {
        this.shouldSave = save;
        onDisable();
        onEnable();
    }

    private void startAutoSaver() {
        WarpSystem.log("Starting AutoSaver");
        Bukkit.getScheduler().runTaskTimerAsynchronously(WarpSystem.getInstance(), () -> save(true), 12000, 12000);
    }

    private void destroy() {
        this.dataManager.getManagers().forEach(Manager::destroy);
        this.proxyFeatureList.clear();
        this.fileManager.destroy();
    }

    private void save(boolean saver) {
        if (!this.shouldSave) return;
        try {
            if (!this.ERROR) {
                long start = System.currentTimeMillis();

                if (!saver) {

                    log(" ");
                    log("__________________________________________________________");
                    log(" ");
                    log("                       WarpSystem [" + getDescription().getVersion() + "]");
                    if (updateAvailable) {
                        log(" ");
                        log("New update available [" + updateNotifier.getVersion() + " - " + WarpSystem.this.updateNotifier.getUpdateInfo() + "]. Download it on \n\n" + updateNotifier.getDownload() + "\n");
                    }
                    log(" ");
                    log("Status:");
                    log(" ");
                    log("MC-Version: " + Version.fullVersion());
                    log(" ");
                }

                //save cooldown list
                cooldownManager.save();

                if (!saver) log("Saving options");
                fileManager.getFile("Config").loadConfig();
                this.options.write();

                if (!saver) log("Saving features");
                this.dataManager.save(saver);
                this.teleportManager.save();

                if (!saver) {
                    log(" ");
                    log("Finished (" + (System.currentTimeMillis() - start) + "ms)");
                    log(" ");
                    log("__________________________________________________________");
                    log(" ");
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Error at saving data! Exception: \n\n");
            ex.printStackTrace();
            getLogger().log(Level.SEVERE, "\n");
        }
    }

    private void checkOldDirectory() {
        File file = getDataFolder();

        if (file.exists()) {
            File warps = new File(file, "Memory/Warps.yml");

            if (warps.exists()) {
                old = true;
                renameUnnecessaryFiles();
            }
        }
    }

    private void renameUnnecessaryFiles() {
        File file = getDataFolder();

        new File(file, "Config.yml").renameTo(new File(file, "OldConfig_Update_2.0.yml"));
        new File(file, "Language.yml").renameTo(new File(file, "OldLanguage_Update_2.0.yml"));
    }

    public void createBackup() {
        getDataFolder().mkdir();
        Calendar c = Calendar.getInstance();

        File backupFolder = new File(getDataFolder().getPath() + "/Backups/", c.get(Calendar.YEAR) + "_" + (c.get(Calendar.MONTH) + 1) + "_" + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + "_" + c.get(Calendar.MINUTE) + "_" + c.get(Calendar.SECOND));
        backupFolder.mkdirs();

        for (File file : getDataFolder().listFiles()) {
            if (file.getName().equals("Backups") || file.getName().equals("ErrorReport.txt")) continue;
            File dest = new File(backupFolder, file.getName());

            try {
                if (file.isDirectory()) {
                    copyFolder(file, dest);
                    continue;
                }

                copyFileUsingFileChannels(file, dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyFolder(File source, File dest) throws IOException {
        dest.mkdirs();
        for (File file : source.listFiles()) {
            File copy = new File(dest, file.getName());

            if (file.isDirectory()) {
                copyFolder(file, copy);
                continue;
            }

            copyFileUsingFileChannels(file, copy);
        }
    }

    private void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            if (inputChannel != null && outputChannel != null) {
                inputChannel.close();
                outputChannel.close();
            }
        }
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public boolean isProxyConnected() {
        return connectedProxy;
    }

    public synchronized void setOnProxy(boolean onProxy, Player connection) {
        if (onProxy) this.proxyFeatureList.forEach(proxyFeature -> proxyFeature.onInitiate(connection));
        if (this.connectedProxy == onProxy) return;

        this.connectedProxy = onProxy;
        if (onProxy) {
            this.proxyFeatureList.forEach(proxyFeature -> proxyFeature.onConnect(connection));
        } else {
            this.proxyFeatureList.forEach(ProxyFeature::onDisconnect);
        }
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public boolean isOld() {
        return old;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public String getCurrentServer() {
        return server;
    }

    public void setCurrentServer(String server) {
        this.server = server;
    }

    public UpdateNotifier getUpdateNotifier() {
        return updateNotifier;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public String getProxyPluginVersion() {
        return proxyPluginVersion;
    }

    public void setProxyPluginVersion(String proxyPluginVersion) {
        this.proxyPluginVersion = proxyPluginVersion;
    }

    public Set<ProxyFeature> getProxyFeatureList() {
        return proxyFeatureList;
    }

    public HeadManager getHeadManager() {
        return headManager;
    }

    public SetupAssistantManager getSetupAssistantManager() {
        return setupAssistantManager;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public UTFConfig getOldConfig() {
        return oldConfig;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public boolean ignoreProxyDetection() {
        return ignoreProxyDetection;
    }
}
