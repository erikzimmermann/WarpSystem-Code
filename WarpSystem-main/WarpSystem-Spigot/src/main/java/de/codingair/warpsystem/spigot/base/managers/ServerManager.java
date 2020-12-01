package de.codingair.warpsystem.spigot.base.managers;

import de.codingair.codingapi.API;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.transfer.packets.spigot.utils.ServerPing;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.signs.managers.SignManager;
import de.codingair.warpsystem.transfer.packets.bungee.SendServerPropertiesPacket;
import de.codingair.warpsystem.transfer.packets.utils.Packet;
import de.codingair.warpsystem.transfer.packets.utils.PacketType;
import de.codingair.warpsystem.transfer.utils.PacketListener;
import net.nitrado.warpsystem.utils.warpgui.WarpPanel;
import org.bukkit.Bukkit;

import java.util.HashMap;

public class ServerManager extends PacketListener {
    private final HashMap<String, ServerPing> properties = new HashMap<>();

    public ServerManager() {
        WarpSystem.getInstance().getDataHandler().register(this);
    }

    public ServerPing getProperties(String server) {
        return properties.get(server.toLowerCase());
    }

    @Override
    public void onReceive(Packet packet, String extra) {
        if(packet.getType() == PacketType.SendServerPropertiesPacket) {
            SendServerPropertiesPacket p = (SendServerPropertiesPacket) packet;

            properties.putAll(p.getProperties());
            p.getProperties().clear();
            onUpdate();
        }
    }

    public void onUpdate() {
        Bukkit.getScheduler().runTask(WarpSystem.getInstance(), () -> {
            if(FeatureType.SIGNS.isActive()) SignManager.getInstance().updateAll();

            //NITRADO
            for(WarpPanel panel : API.getRemovables(WarpPanel.class)) {
                panel.getActive().updateItems(false);
            }
        });
    }

    @Override
    public boolean onSend(Packet packet) {
        return false;
    }
}
