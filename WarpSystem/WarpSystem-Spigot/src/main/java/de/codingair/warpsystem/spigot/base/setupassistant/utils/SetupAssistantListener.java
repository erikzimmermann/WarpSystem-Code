package de.codingair.warpsystem.spigot.base.setupassistant.utils;

import de.codingair.codingapi.server.reflections.IReflection;
import de.codingair.codingapi.server.reflections.PacketUtils;
import de.codingair.codingapi.server.specification.Version;
import de.codingair.packetmanagement.handlers.PacketHandler;
import de.codingair.packetmanagement.utils.Direction;
import de.codingair.packetmanagement.utils.Proxy;
import de.codingair.warpsystem.core.transfer.packets.proxy.SetupAssistantStorePacket;
import de.codingair.warpsystem.spigot.api.events.PlayerFinalJoinEvent;
import de.codingair.warpsystem.spigot.base.setupassistant.SetupAssistantManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SetupAssistantListener implements PacketHandler<SetupAssistantStorePacket>, Listener {
    private IReflection.ConstructorAccessor chatPacket = null;
    private Object type = null;

    @EventHandler
    public void onFinalJoin(PlayerFinalJoinEvent e) {
        SetupAssistantManager.getInstance().onJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        SetupAssistant a = SetupAssistantManager.getInstance().getAssistant();
        if (a != null && a.getPlayer().equals(e.getPlayer())) {
            a.onQuit();
        }
    }

    private Object buildComponent(String message) {
        if (chatPacket == null) {
            Class<?> packet = IReflection.getClass(IReflection.ServerPacket.MINECRAFT_PACKAGE, Version.choose("PacketPlayOutChat", 21.11, "ClientboundChatPacket"));

            if (Version.after(15)) {
                Class<?> type = IReflection.getClass(IReflection.ServerPacket.MINECRAFT_PACKAGE, Version.choose("ChatMessageType", 21.11, "ChatType"));
                this.type = type.getEnumConstants()[0];
                chatPacket = IReflection.getConstructor(packet, PacketUtils.IChatBaseComponentClass, type, UUID.class);
            } else {
                chatPacket = IReflection.getConstructor(packet, PacketUtils.IChatBaseComponentClass);
            }
        }

        if (Version.after(15)) {
            return chatPacket.newInstance(PacketUtils.getRawIChatBaseComponent(message), type, UUID.randomUUID());
        } else return chatPacket.newInstance(PacketUtils.getRawIChatBaseComponent(message));
    }

    @Override
    public void process(@NotNull SetupAssistantStorePacket packet, @NotNull Proxy proxy, @Nullable Object connection, @NotNull Direction direction) {
        String message = packet.getMessage();
        SetupAssistant assistant = SetupAssistantManager.getInstance().getAssistant();
        if (assistant != null) assistant.getMessager().queue(buildComponent(message));
    }
}
