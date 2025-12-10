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
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings ("UnstableApiUsage")
public class Messager_v1_19 implements Messager {
    private final Player player;
    private final EvictingQueue<Object> queue = EvictingQueue.create(17);
    private final PacketReader reader;
    private final Set<Object> packets = new HashSet<>();
    private final Class<?> oSystemPacketClass = IReflection.getClass(IReflection.ServerPacket.PACKETS, "ClientboundSystemChatPacket");
    private final IReflection.ConstructorAccessor con = IReflection.getConstructor(oSystemPacketClass, BaseComponent[].class, boolean.class);

    public Messager_v1_19(@NotNull Player player, @NotNull SetupAssistant assistant) {
        this.player = player;

        Class<?> iPacketClass = IReflection.getClass(IReflection.ServerPacket.PACKETS, Version.choose("PacketPlayInChat", 21.11, "ServerboundChatPacket"));
        IReflection.FieldAccessor<String> inputText = IReflection.getField(iPacketClass, String.class, 0);

        Class<?> oPlayerPacketClass = IReflection.getClass(IReflection.ServerPacket.PACKETS, "ClientboundPlayerChatPacket");

        reader = new PacketReader(player, "WS-SetupAssistant", WarpSystem.getInstance()) {
            @Override
            public boolean readPacket(Object packet) {
                try {
                    if (packet.getClass().equals(iPacketClass)) {
                        String text = inputText.get(packet);
                        if (text != null) {
                            //forward chat button
                            if (text.startsWith(ChatButton.PREFIX)) return false;

                            assistant.onChat(text);
                            return true;
                        }
                    } else if (packet.getClass().equals(oPlayerPacketClass) || packet.getClass().equals(oSystemPacketClass)) { //got output message from bungee
                        //queue for later
                        queue(packet);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean writePacket(Object packet) {
                if (packet.getClass().equals(oPlayerPacketClass)) {
                    //queue for later
                    queue(packet);
                    return true;
                } else if (packet.getClass().equals(oSystemPacketClass)) {
                    if (packets.remove(packet)) return false;

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
        assert con != null;
        Object packet = con.newInstance(new BaseComponent[] {tc}, /*action-bar?*/ false);
        packets.add(packet);
        PacketUtils.sendPacket(player, packet);
    }

    @Override
    public void sendMessage(@NotNull SimpleMessage message) {
        message.send(player, (p, tc) -> sendMessage(tc));
    }
}
