package de.codingair.warpsystem.core.proxy.transfer.handlers;

import de.codingair.codingapi.utils.Value;
import de.codingair.packetmanagement.exceptions.Escalation;
import de.codingair.packetmanagement.exceptions.NoConnectionException;
import de.codingair.packetmanagement.exceptions.TimeOutException;
import de.codingair.packetmanagement.handlers.ResponsibleMultiLayerPacketHandler;
import de.codingair.packetmanagement.packets.impl.LongPacket;
import de.codingair.packetmanagement.utils.Direction;
import de.codingair.packetmanagement.utils.Proxy;
import de.codingair.warpsystem.core.proxy.Core;
import de.codingair.warpsystem.core.proxy.base.handlers.ServerHandler;
import de.codingair.warpsystem.core.proxy.features.TeleportHandler;
import de.codingair.warpsystem.core.proxy.redis.RedisCore;
import de.codingair.warpsystem.core.proxy.utils.Player;
import de.codingair.warpsystem.core.proxy.utils.Players;
import de.codingair.warpsystem.core.proxy.utils.Server;
import de.codingair.warpsystem.core.transfer.packets.proxy.TeleportPlayerToCoordsPacket;
import de.codingair.warpsystem.core.transfer.packets.proxy.TeleportPlayerToPlayerPacket;
import de.codingair.warpsystem.core.transfer.packets.spigot.PrepareTeleportPacket;
import de.codingair.warpsystem.core.transfer.utils.PlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class PrepareTeleportPacketHandler implements ResponsibleMultiLayerPacketHandler<PrepareTeleportPacket, LongPacket> {

    @Override
    public boolean answer(@NotNull PrepareTeleportPacket packet, @NotNull Proxy proxy, @NotNull Direction direction) {
        //redis
        //we might not be able to handle this packet!
        return direction == Direction.DOWN || packet.getRecipient() == null || Players.getPlayer(packet.getRecipient()) != null;
    }

    @Override
    public @NotNull CompletableFuture<LongPacket> response(@NotNull PrepareTeleportPacket packet, @NotNull Proxy proxy, @Nullable Object connection, @NotNull Direction direction) {
        Player sender = Players.getPlayer(packet.getSender());

        TeleportHandler handler = Core.getPlugin().getHandler(TeleportHandler.class);

        Server<?> target;
        String targetName;

        if (!packet.isCoordsPacket()) {
            Player targetPlayer = packet.getSender().equalsIgnoreCase(packet.getTarget()) ? sender : Players.getPlayer(packet.getTarget());
            if (targetPlayer == null) {
                //redis
                PlayerData data = Core.getPlugin().getPlayerData().getCache(packet.getTarget());
                if (data != null) {
                    target = Core.getPlugin().getServer(data.getServer());
                    targetName = data.getName();
                } else {
                    target = null;
                    targetName = null;
                }

                if (target == null) return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.PLAYER_NOT_ONLINE.ordinal()));
            } else if (!handler.isAccessible(targetPlayer.getServer())) {
                return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.SERVER_NOT_ONLINE.ordinal()));
            } else {
                target = targetPlayer.getServer();
                targetName = targetPlayer.getName();
            }
        } else {
            if (sender == null) {
                //redis or console
                PlayerData data = Core.getPlugin().getPlayerData().getCache(packet.getSender());
                if (data != null) {
                    target = Core.getPlugin().getServer(data.getServer());
                    targetName = data.getName();
                } else {
                    // console: sender not in cache (e.g. "CONSOLE") - use recipient (player being teleported) to resolve target server
                    PlayerData recipientData = Core.getPlugin().getPlayerData().getCache(packet.getRecipient());
                    if (recipientData != null) {
                        targetName = recipientData.getName();
                        if (packet.getServer() != null) {
                            target = Core.getPlugin().getServer(packet.getServer());
                            if (target == null) return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.SERVER_NOT_ONLINE.ordinal()));
                        } else {
                            target = Core.getPlugin().getServer(recipientData.getServer());
                        }
                    } else {
                        target = null;
                        targetName = null;
                    }
                }

                if (target == null) return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.PLAYER_NOT_ONLINE.ordinal()));
            } else if (!handler.isAccessible(sender.getServer())) {
                return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.SERVER_NOT_ONLINE.ordinal()));
            } else {
                if (packet.getServer() != null) {
                    target = Core.getPlugin().getServer(packet.getServer());
                    if (target == null) return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.SERVER_NOT_ONLINE.ordinal()));
                } else target = sender.getServer();

                targetName = sender.getName();
            }
        }

        String recipient = packet.getRecipient();
        if (recipient == null) {
            //forward to all
            Value<Integer> handled = new Value<>(0);
            Value<Integer> sent = new Value<>(0);

            Core.getServerManager().getOnlineServer().filter(s -> !s.equals(connection) && handler.isAccessible(s)).forEach(s -> {
                handled.setValue(handled.getValue() + s.getOnlineCount());

                //tp all
                s.getOnlinePlayers().forEach(player -> {
                    if (handler.deniesForceTps(player)) return;

                    sent.setValue(sent.getValue() + 1);
                    TeleportPlayerToPlayerPacket ptpPacket = new TeleportPlayerToPlayerPacket(packet.getSender(), player.getName(), targetName, false);
                    Core.getPlugin().dataHandler().send(ptpPacket, target, Direction.DOWN);
                    player.connect(target);
                });
            });

            CompletableFuture<LongPacket> future = new CompletableFuture<>();

            if (direction == Direction.DOWN && RedisCore.ready()) {
                //wait for other proxies
                Core.getPlugin().dataHandler().send(packet
                                .mergeFuture(RedisCore.core().getProxies().size(), (longPacket, longPacket2) -> new LongPacket(longPacket.a() + longPacket2.a())),
                        null, Direction.UP).whenComplete((result, t) -> {
                    long res = 0;
                    if (t != null) {
                        //time out or not even connected?
                        if (!(t instanceof NoConnectionException) && !(t instanceof TimeOutException)) t.printStackTrace();
                    } else res = result.a();
                    future.complete(new LongPacket(((((long) handled.getValue()) << 32) | (sent.getValue() & 0xffffffffL)) + res));
                });
            } else future.complete(new LongPacket((((long) handled.getValue()) << 32) | (sent.getValue() & 0xffffffffL)));

            return future;
        } else {
            //only recipient
            Player player = Players.getPlayer(packet.getRecipient());

            if (player == null) {
                //redis
                if (direction == Direction.DOWN) throw new Escalation(this, Direction.UP, packet, err -> new LongPacket(PrepareTeleportPacket.Result.PLAYER_NOT_ONLINE.ordinal()));
                else {
                    //we'll never get here
                    throw new IllegalStateException();
                }
            } else if (!handler.isAccessible(player.getServer())) {
                //not online/accessible
                return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.PLAYER_NOT_ONLINE.ordinal()));
            } else if (handler.deniesForceTps(player) && !player.equals(sender)) {
                //auto deny
                return CompletableFuture.completedFuture(new LongPacket(PrepareTeleportPacket.Result.TELEPORT_DENIED.ordinal()));
            } else {
                CompletableFuture<LongPacket> future = new CompletableFuture<>();

                ServerHandler.sendPlayerTo(player, target).whenComplete((res, t) -> {
                    if(t != null) t.printStackTrace();
                    else if(res.isConnected()) {
                        if (packet.isCoordsPacket()) {
                            TeleportPlayerToCoordsPacket ptcPacket = new TeleportPlayerToCoordsPacket(
                                    packet.getSender(), player.getName(), null, 0,
                                    packet.getX(), packet.getY(), packet.getZ(),
                                    packet.getYaw(), packet.getPitch(),
                                    packet.getWorld(), packet.getServer(),
                                    false, false, false);

                            Core.getPlugin().dataHandler().send(ptcPacket, target, Direction.DOWN);
                        } else {
                            TeleportPlayerToPlayerPacket ptpPacket = new TeleportPlayerToPlayerPacket(packet.getSender(), player.getName(), targetName, true);
                            Core.getPlugin().dataHandler().send(ptpPacket, target, Direction.DOWN);
                        }

                        future.complete(new LongPacket(PrepareTeleportPacket.Result.SUCCESS.ordinal()));
                        return;
                    }

                    future.complete(new LongPacket(PrepareTeleportPacket.Result.SERVER_NOT_ONLINE.ordinal()));
                });

                return future;
            }
        }
    }
}
