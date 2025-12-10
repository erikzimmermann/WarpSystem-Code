package de.codingair.warpsystem.spigot.base.setupassistant.utils.versions;

import com.google.common.collect.EvictingQueue;
import de.codingair.codingapi.player.chat.ChatButton;
import de.codingair.codingapi.player.chat.SimpleMessage;
import de.codingair.codingapi.player.data.PacketReader;
import de.codingair.codingapi.server.reflections.IReflection;
import de.codingair.codingapi.server.reflections.PacketUtils;
import de.codingair.codingapi.server.specification.Version;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.setupassistant.utils.Messager;
import de.codingair.warpsystem.spigot.base.setupassistant.utils.SetupAssistant;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class Messager_v1_17 implements Messager {
    private final Player player;
    private final EvictingQueue<Object> queue = EvictingQueue.create(17);
    private final PacketReader reader;

    public Messager_v1_17(@NotNull Player player, @NotNull SetupAssistant assistant) {
        this.player = player;

        Class<?> iPacketClass = IReflection.getClass(IReflection.ServerPacket.PACKETS, Version.choose("PacketPlayInChat", 21.11, "ServerboundChatPacket"));
        IReflection.FieldAccessor<String> inputText = IReflection.getField(iPacketClass, Version.choose("a", 17, "b"));

        Class<?> oPacketClass = IReflection.getClass(IReflection.ServerPacket.PACKETS, Version.choose("PacketPlayOutChat", 21.11, "ClientboundChatPacket"));
        IReflection.FieldAccessor<?> outputText = IReflection.getField(oPacketClass, Version.choose("components", 17, "a"));

        IReflection.MethodAccessor getText = IReflection.getMethod(PacketUtils.IChatBaseComponentClass, Version.choose("getText", 18, "a"), String.class, new Class[0]);

        reader = new PacketReader(player, "WS-SetupAssistant", WarpSystem.getInstance()) {
            @Override
            public boolean readPacket(Object packet) {
                if (packet.getClass().equals(iPacketClass)) {
                    String text = inputText.get(packet);
                    if (text != null) {
                        //forward chat button
                        if (text.startsWith(ChatButton.PREFIX)) return false;

                        assistant.onChat(text);
                        return true;
                    }
                } else if (packet.getClass().equals(oPacketClass)) { //got output message from bungee
                    //queue for later
                    queue(packet);
                    return true;
                }
                return false;
            }

            @Override
            public boolean writePacket(Object packet) {
                if (packet.getClass().equals(oPacketClass)) {
                    String s = (String) getText.invoke(outputText.get(packet));
                    if (s.startsWith("§f")) s = s.substring(2);

                    //identifier
                    if (s.startsWith(SetupAssistant.IDENTIFIER)) return false;

                    //queue for later
                    queue(packet);
                    return true;
                } else return false;
            }
        };
    }

    @Override
    public void startBlocking() {
        reader.inject();
    }

    @Override
    public void stopBlocking() {
        reader.unInject();
    }

    @Override
    public void queue(@NotNull Object packet) {
        queue.add(packet);
    }

    @Override
    public void flushCache() {
        for (Object o : queue) {
            PacketUtils.sendPacket(player, o);
        }
        queue.clear();
    }

    @Override
    public void sendMessage(@NotNull TextComponent tc) {
        player.spigot().sendMessage(tc);
    }

    @Override
    public void sendMessage(@NotNull SimpleMessage message) {
        message.send(player);
    }
}
