package de.codingair.warpsystem.spigot.api.chatinput;

import de.codingair.codingapi.API;
import de.codingair.codingapi.player.MessageAPI;
import de.codingair.codingapi.server.sounds.SoundData;
import de.codingair.codingapi.utils.ChatColor;
import de.codingair.codingapi.utils.Removable;
import de.codingair.warpsystem.core.transfer.packets.spigot.ChatInputGUITogglePacket;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public abstract class ChatInputGUI implements Removable {
    private final UUID id = UUID.randomUUID();
    private final JavaPlugin plugin;
    private final Player player;
    private final ChatInputListener listener;
    private SoundData openSound;
    private SoundData cancelSound;
    private SoundData submitFinishSound;
    private SoundData submitMistakeSound;
    private BukkitRunnable runnable = null;
    private String title, subTitle = null;
    private boolean actionBarSwitch = false;

    public ChatInputGUI(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;

        this.title = Lang.get("Enter_Something_in_Chat");
        this.title = "§7" + ChatColor.stripColor(this.title);
        if (this.title.endsWith(".")) this.title = this.title.substring(0, this.title.length() - 1);

        if (this.title.contains("\n")) {
            String[] a = this.title.split("\\n");
            this.title = a[0];
            this.subTitle = "§7" + a[1];
        }

        Bukkit.getPluginManager().registerEvents(listener = new ChatInputListener(this), plugin);
    }

    public void open() {
        API.addRemovable(this);

        if (WarpSystem.getInstance().isProxyConnected()) {
            WarpSystem.getDataHandler().send(new ChatInputGUITogglePacket(this.player.getName(), true), player);
        }

        sendTitle(5, 10, 0);
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                sendTitle(0, 15, 0);
            }
        };

        this.runnable.runTaskTimer(this.plugin, 5, 10);
        if (this.openSound != null) SoundUtil.play(player, this.openSound);
    }

    private void sendTitle(int in, int stay, int out) {
        MessageAPI.sendTitle(player, title, subTitle, in, stay, out);
        MessageAPI.sendActionBar(player, (actionBarSwitch ? "§7» " : "§7»  ") + Lang.get("Move_to_cancel") + (actionBarSwitch ? " §7«" : "  §7«"));
        actionBarSwitch = !actionBarSwitch;
    }

    void onInput(String message) {
        if (message != null) message = Lang.whitespace().trimFrom(message);

        ChatInputEvent e = new ChatInputEvent(this, message);
        onEnter(e);

        if (e.isClose()) {
            if (this.submitFinishSound != null) SoundUtil.play(player, this.submitFinishSound);
            close();
            return;
        } else if (e.getNotifier() != null) setTitle(e.getNotifier());

        if (this.submitMistakeSound != null) SoundUtil.play(player, this.submitMistakeSound);
    }

    public abstract void onEnter(ChatInputEvent e);

    public abstract void onClose();

    public void close() {
        destroy();
    }

    @Override
    public void destroy() {
        if (this.runnable != null) {
            this.runnable.cancel();
            this.runnable = null;

            if (WarpSystem.getInstance().isProxyConnected()) {
                WarpSystem.getDataHandler().send(new ChatInputGUITogglePacket(this.player.getName(), false), player);
            }

            sendTitle(0, 0, 5);
            MessageAPI.stopSendingActionBar(player);

            HandlerList.unregisterAll(listener);
            API.removeRemovable(this);

            onClose();
        }
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public UUID getUniqueId() {
        return this.id;
    }

    @Override
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null) return;
        if (title.equals(this.title + (this.subTitle == null ? "" : "\n" + subTitle))) return;

        this.title = title;
        this.subTitle = null;

        if (this.title.contains("\n")) {
            String[] a = this.title.split("\\n");
            this.title = a[0];
            this.subTitle = a[1];
        }

        sendTitle(0, 15, 0);
    }

    public SoundData getOpenSound() {
        return openSound;
    }

    public void setOpenSound(SoundData openSound) {
        this.openSound = openSound;
    }

    public SoundData getCancelSound() {
        return cancelSound;
    }

    public void setCancelSound(SoundData cancelSound) {
        this.cancelSound = cancelSound;
    }

    public SoundData getSubmitFinishSound() {
        return submitFinishSound;
    }

    public void setSubmitFinishSound(SoundData submitFinishSound) {
        this.submitFinishSound = submitFinishSound;
    }

    public SoundData getSubmitMistakeSound() {
        return submitMistakeSound;
    }

    public void setSubmitMistakeSound(SoundData submitMistakeSound) {
        this.submitMistakeSound = submitMistakeSound;
    }
}
