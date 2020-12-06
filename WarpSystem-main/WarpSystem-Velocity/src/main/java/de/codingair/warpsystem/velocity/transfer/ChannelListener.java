package de.codingair.warpsystem.velocity.transfer;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import de.codingair.warpsystem.base.transfer.packets.utils.Packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ChannelListener {
    protected VelocityDataHandler velocityDataHandler;

    public ChannelListener(VelocityDataHandler velocityDataHandler) {
        this.velocityDataHandler = velocityDataHandler;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if(e.getIdentifier().getId().equals(velocityDataHandler.getRequestChannel())) {
            e.setResult(PluginMessageEvent.ForwardResult.handled());

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

            try {
                ServerConnection s = (ServerConnection) e.getSource();
                Packet packet = velocityDataHandler.produce(in.readUnsignedShort());

                if(packet == null) return;

                packet.read(in);
                this.velocityDataHandler.onReceive(packet, s);
            } catch(IOException e1) {
                e1.printStackTrace();
            }
        }
    }

}
