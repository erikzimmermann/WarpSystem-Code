package de.codingair.warpsystem.bungee.base.managers;

import com.google.common.base.Preconditions;
import de.codingair.codingapi.tools.Callback;
import de.codingair.warpsystem.base.transfer.packets.bungee.InitialPacket;
import de.codingair.warpsystem.base.transfer.packets.bungee.SendServerPropertiesPacket;
import de.codingair.warpsystem.base.transfer.packets.spigot.SendOptionsPacket;
import de.codingair.warpsystem.base.transfer.packets.spigot.utils.ServerPing;
import de.codingair.warpsystem.base.transfer.packets.utils.Packet;
import de.codingair.warpsystem.base.transfer.packets.utils.PacketType;
import de.codingair.warpsystem.base.transfer.serializeable.ServerOptions;
import de.codingair.warpsystem.base.transfer.utils.PacketListener;
import de.codingair.warpsystem.bungee.base.WarpSystem;
import de.codingair.warpsystem.bungee.base.utils.ServerInitializeEvent;
import de.codingair.warpsystem.bungee.base.utils.ServerProvideOptionsEvent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ServerManager extends PacketListener implements Listener {
    private final HashMap<ServerInfo, ServerOptions> options = new HashMap<>();
    private final ConcurrentHashMap<ServerInfo, ServerPing> cachedPing = new ConcurrentHashMap<>();
    private final HashMap<ServerInfo, List<Callback<ServerInfo>>> waiting = new HashMap<>();

    public static void sendPlayerTo(ServerInfo server, ProxiedPlayer player, Callback<ServerInfo> c) {
        Preconditions.checkNotNull(server);
        Preconditions.checkNotNull(player);

        if(player.getServer().getInfo().equals(server)) {
            c.accept(server);
        } else {
            if(server.getPlayers().isEmpty()) addCallbackTo(server, c);
            else c.accept(server);
            player.connect(server);
        }
    }

    private static void addCallbackTo(ServerInfo info, Callback<ServerInfo> c) {
        List<Callback<ServerInfo>> l = WarpSystem.getInstance().getServerManager().waiting.computeIfAbsent(info, k -> new ArrayList<>());
        l.add(c);
    }

    public Stream<ServerInfo> getOnlineServer() {
        return cachedPing.entrySet().stream().filter((e) ->  e.getValue() != null && e.getValue().getStatus()).map(Map.Entry::getKey);
    }

    public boolean isOnline(ServerInfo info) {
        ServerPing ping = cachedPing.getOrDefault(info, null);
        return ping != null && ping.getStatus();
    }

    public void run() {
        WarpSystem.proxy().getScheduler().schedule(WarpSystem.getInstance(), () -> {
            for(ServerInfo info : WarpSystem.proxy().getServers().values()) {
                info.ping((serverPing, error) -> cachedPing.compute(info, (server, ping) -> {
                    if(ping == null) ping = new ServerPing(false, 0, 0, null);

                    if(error == null) {
                        ping.setStatus(true);
                        ping.setPlayers(serverPing.getPlayers().getOnline());
                        ping.setMaxPlayers(serverPing.getPlayers().getMax());
                        ping.setMotd(info.getMotd());
                    } else {
                        ping.setStatus(false);
                        ping.setPlayers(0);
                        ping.setMaxPlayers(0);
                        ping.setMotd(null);
                    }

                    return ping;
                }));
            }
        }, 0, 10, TimeUnit.SECONDS);

        WarpSystem.proxy().getScheduler().schedule(WarpSystem.getInstance(), () -> {
            HashMap<String, ServerPing> copy = new HashMap<>();
            for(Map.Entry<ServerInfo, ServerPing> e : cachedPing.entrySet()) {
                copy.put(e.getKey().getName().toLowerCase(), new ServerPing(e.getValue()));
            }

            SendServerPropertiesPacket p = new SendServerPropertiesPacket(copy);
            for(ServerInfo target : WarpSystem.proxy().getServers().values()) {
                if(!target.getPlayers().isEmpty()) {
                    WarpSystem.getInstance().getDataHandler().send(p, target);
                }
            }
        }, 3, 5, TimeUnit.SECONDS);
    }

    public void sendInitialPacket(ServerInfo server) {
        WarpSystem.getInstance().getDataHandler().send(new InitialPacket(WarpSystem.getInstance().getDescription().getVersion(), server.getName()), server);
        WarpSystem.proxy().getPluginManager().callEvent(new ServerInitializeEvent(server));

        List<Callback<ServerInfo>> l = WarpSystem.getInstance().getServerManager().waiting.remove(server);
        if(l != null) {
            l.forEach(c -> c.accept(server));
            l.clear();
        }
    }

    public ServerOptions getOptions(ServerInfo info) {
        if(info == null) return null;
        return options.get(info);
    }

    public ServerPing getLastPing(ServerInfo info) {
        if(info == null) return null;
        return cachedPing.get(info);
    }

    @Override
    public void onReceive(Packet packet, String extra) {
        if(packet.getType() == PacketType.SendOptionsPacket) {
            ServerInfo info = WarpSystem.getInstance().getProxy().getServerInfo(extra);

            SendOptionsPacket p = (SendOptionsPacket) packet;
            options.put(info, p.getOptions());
            p.getOptions().setSameVersion(WarpSystem.getInstance().getDescription().getVersion().equals(p.getOptions().getVersion()));
            WarpSystem.getInstance().getProxy().getPluginManager().callEvent(new ServerProvideOptionsEvent(info, p.getOptions()));
        }
    }

    @Override
    public boolean onSend(Packet packet) {
        return false;
    }
}
