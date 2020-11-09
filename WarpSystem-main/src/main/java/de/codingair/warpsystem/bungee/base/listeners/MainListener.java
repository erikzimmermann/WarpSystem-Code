package de.codingair.warpsystem.bungee.base.listeners;

import de.codingair.codingapi.tools.Callback;
import de.codingair.warpsystem.bungee.api.Players;
import de.codingair.warpsystem.bungee.base.WarpSystem;
import de.codingair.warpsystem.bungee.base.language.Lang;
import de.codingair.warpsystem.bungee.base.managers.ServerManager;
import de.codingair.warpsystem.bungee.base.utils.ServerInitializeEvent;
import de.codingair.warpsystem.bungee.base.utils.ServerProvideOptionsEvent;
import de.codingair.warpsystem.spigot.base.utils.ServerPing;
import de.codingair.warpsystem.transfer.packets.bungee.PrepareLoginMessagePacket;
import de.codingair.warpsystem.transfer.packets.bungee.SendUUIDPacket;
import de.codingair.warpsystem.transfer.packets.general.BooleanPacket;
import de.codingair.warpsystem.transfer.packets.general.IntegerPacket;
import de.codingair.warpsystem.transfer.packets.general.PrepareCoordinationTeleportPacket;
import de.codingair.warpsystem.transfer.packets.general.StringPacket;
import de.codingair.warpsystem.transfer.packets.spigot.*;
import de.codingair.warpsystem.transfer.packets.utils.Packet;
import de.codingair.warpsystem.transfer.packets.utils.PacketType;
import de.codingair.warpsystem.transfer.utils.PacketListener;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainListener implements Listener, PacketListener {
    private final Set<ServerInfo> asking = new HashSet<>();

    @EventHandler
    public void onConnect(ServerConnectedEvent e) {
        if(e.getServer().getInfo().getPlayers().size() == 0) {
            //Update it
            WarpSystem.getInstance().getServerManager().sendInitialPacket(e.getServer().getInfo());
        }

        if(asking.contains(e.getServer().getInfo())) {
            ask(e.getPlayer());
        }
    }

    private void ask(ProxiedPlayer player) {
        if(player.hasPermission(WarpSystem.PERMISSION_MODIFY_SYSTEM)) {
            WarpSystem.getInstance().getProxy().getScheduler().schedule(WarpSystem.getInstance(), () -> {
                TextComponent base = new TextComponent(Lang.getPrefix() + "§7Do you want to §cfetch §7a new update from your §cBungeeCord§7? §8[");

                TextComponent extra = new TextComponent("§aYes");
                extra.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {new TextComponent(Lang.get("Click_Hover"))}));
                extra.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warpsystembungee fetch"));

                base.addExtra(extra);
                base.addExtra("§8]");

                player.sendMessage(base);
            }, 4, TimeUnit.SECONDS);
        }
    }

    @EventHandler
    public void onInit(ServerInitializeEvent e) {
        asking.remove(e.getInfo());
    }

    @EventHandler
    public void onOptions(ServerProvideOptionsEvent e) {
        int i = e.getOptions().getUpdateFetching();
        if(i == 1) {
            //ask
            if(WarpSystem.getInstance().getJarManager().fetchPossible(e.getInfo())) {
                asking.add(e.getInfo());

                for(ProxiedPlayer player : e.getInfo().getPlayers()) {
                    ask(player);
                }
            }
        } else if(i == 2) {
            if(WarpSystem.getInstance().getJarManager().fetchPossible(e.getInfo())) WarpSystem.getInstance().getJarManager().sendJar(e.getInfo(), null);
        }
    }

    @Override
    public void onReceive(Packet packet, String extra) {
        ServerInfo server = BungeeCord.getInstance().getServerInfo(extra);

        switch(PacketType.getByObject(packet)) {
            case RequestInitialPacket: {
                WarpSystem.getInstance().getServerManager().sendInitialPacket(server);
                break;
            }

            case RequestUUIDPacket: {
                RequestUUIDPacket p = (RequestUUIDPacket) packet;
                ProxiedPlayer pp = BungeeCord.getInstance().getPlayer(p.getName());

                SendUUIDPacket answer;
                if(pp == null) answer = new SendUUIDPacket(null);
                else answer = new SendUUIDPacket(pp.getUniqueId());

                p.applyAsAnswer(answer);

                WarpSystem.getInstance().getDataHandler().send(answer, server);
                break;
            }

            case MessagePacket: {
                MessagePacket p = (MessagePacket) packet;
                ProxiedPlayer player = BungeeCord.getInstance().getPlayer(p.getPlayer());

                if(player != null) {
                    TextComponent tc = new TextComponent(p.getMessage());
                    tc.setColor(ChatColor.GRAY);
                    player.sendMessage(tc);
                }
                break;
            }

            case RequestServerStatusPacket: {
                RequestServerStatusPacket p = (RequestServerStatusPacket) packet;
                BooleanPacket answer = new BooleanPacket();
                p.applyAsAnswer(answer);

                ServerInfo info = BungeeCord.getInstance().getServerInfo(p.getServer());

                if(info == null) {
                    answer.setValue(false);
                    WarpSystem.getInstance().getDataHandler().send(answer, server);
                } else {
                    info.ping((serverPing, throwable) -> {
                        WarpSystem.getInstance().getServerManager().setStatus(info, throwable == null);
                        answer.setValue(throwable == null);
                        WarpSystem.getInstance().getDataHandler().send(answer, server);
                    });
                }
                break;
            }

            case PrepareServerSwitchPacket: {
                PrepareServerSwitchPacket p = (PrepareServerSwitchPacket) packet;
                IntegerPacket answer = new IntegerPacket();
                p.applyAsAnswer(answer);

                ProxiedPlayer pp = BungeeCord.getInstance().getPlayer(p.getPlayer());
                ServerInfo info = BungeeCord.getInstance().getServerInfo(p.getServer());

                if(pp == null || info == null) {
                    answer.setValue(1);
                    WarpSystem.getInstance().getDataHandler().send(answer, server);
                } else {
                    if(pp.getServer().getInfo() == info) {
                        answer.setValue(2);
                        WarpSystem.getInstance().getDataHandler().send(answer, server);
                        return;
                    }

                    if(WarpSystem.getInstance().getServerManager().isOnline(info)) {
                        ServerPing ping = WarpSystem.getInstance().getServerManager().getLastPing(info);

                        if(ping == null) {
                            answer.setValue(4);
                            WarpSystem.getInstance().getDataHandler().send(answer, server);
                        } else {
                            if(p.isIgnoreLimit() || info.getPlayers().size() < ping.getMaxPlayers()) {
                                answer.setValue(0);
                                WarpSystem.getInstance().getDataHandler().send(answer, server);
                                ServerManager.sendPlayerTo(info, pp, new Callback<ServerInfo>() {
                                    @Override
                                    public void accept(ServerInfo object) {
                                        WarpSystem.getInstance().getDataHandler().send(new PrepareLoginMessagePacket(pp.getName(), p.getMessage()), info);
                                    }
                                });
                            } else {
                                answer.setValue(5);
                                WarpSystem.getInstance().getDataHandler().send(answer, server);
                            }
                        }
                    } else {
                        answer.setValue(3);
                        WarpSystem.getInstance().getDataHandler().send(answer, server);
                    }
                }
                break;
            }

            case PrepareCoordinationTeleportPacket: {
                PrepareCoordinationTeleportPacket p = (PrepareCoordinationTeleportPacket) packet;
                IntegerPacket answer = new IntegerPacket();
                p.applyAsAnswer(answer);

                ProxiedPlayer pp = BungeeCord.getInstance().getPlayer(p.getPlayer());
                ServerInfo target = BungeeCord.getInstance().getServerInfo(p.getServer());

                if(WarpSystem.getInstance().getServerManager().isOnline(target)) {
                    if(target.getPlayers().isEmpty()) {
                        //switch and teleport
                        pp.connect(target, (connected, throwable) -> {
                            if(connected) {
                                answer.setValue(0);
                                PrepareCoordinationTeleportPacket finalCall = p.clone(null);
                                finalCall.setServer(null);
                                WarpSystem.getInstance().getDataHandler().send(finalCall, target);
                            } else {
                                answer.setValue(1);
                            }

                            WarpSystem.getInstance().getDataHandler().send(answer, server);
                        });
                    } else {
                        ServerPing ping = WarpSystem.getInstance().getServerManager().getLastPing(target);

                        if(ping == null) {
                            answer.setValue(4);
                            WarpSystem.getInstance().getDataHandler().send(answer, server);
                        } else {
                            if(p.isIgnoreLimit() || target.getPlayers().size() < ping.getMaxPlayers()) {
                                //prepare and switch
                                PrepareCoordinationTeleportPacket finalCall = p.clone(new Callback<Integer>() {
                                    @Override
                                    public void accept(Integer object) {
                                        answer.setValue(object);
                                        WarpSystem.getInstance().getDataHandler().send(answer, server);
                                    }
                                });
                                finalCall.setServer(null);
                                WarpSystem.getInstance().getDataHandler().send(finalCall, target);
                                pp.connect(target);
                            } else {
                                answer.setValue(3);
                                WarpSystem.getInstance().getDataHandler().send(answer, server);
                            }
                        }
                    }
                } else {
                    answer.setValue(1);
                    WarpSystem.getInstance().getDataHandler().send(answer, server);
                }
                break;
            }

            case RequestFullNamePacket: {
                RequestFullNamePacket p = (RequestFullNamePacket) packet;

                StringPacket answer = new StringPacket();
                p.applyAsAnswer(answer);

                if(p.getName() == null) {
                    answer.setValue(null);
                    WarpSystem.getInstance().getDataHandler().send(answer, server);
                    return;
                }

                ProxiedPlayer pp = Players.getPlayer(p.getName());

                answer.setValue(pp == null ? null : pp.getName());
                WarpSystem.getInstance().getDataHandler().send(answer, server);
            }
        }
    }

    @Override
    public boolean onSend(Packet packet) {
        return false;
    }
}
