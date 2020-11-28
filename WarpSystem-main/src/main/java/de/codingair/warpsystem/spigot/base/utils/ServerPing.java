package de.codingair.warpsystem.spigot.base.utils;

import de.codingair.warpsystem.transfer.serializeable.Serializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ServerPing implements Serializable {
    private boolean status;
    private int players;
    private int maxPlayers;
    private String motd;

    public ServerPing() {
    }

    public ServerPing(ServerPing ping) {
        this.status = ping.status;
        this.players = ping.players;
        this.maxPlayers = ping.maxPlayers;
        this.motd = ping.motd;
    }

    public ServerPing(boolean status, int players, int maxPlayers, String motd) {
        this.status = status;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.motd = motd;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        String motd = (this.status ? "1" : "0") + (this.motd == null ? "" : this.motd);
        out.writeUTF(motd);

        if(this.status) {
            out.writeInt(this.players);
            out.writeInt(this.maxPlayers);
        }
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        String motd = in.readUTF();
        char state = motd.charAt(0);
        this.status = state == '1';

        if(this.status) {
            this.motd = motd.substring(1);
            this.players = in.readInt();
            this.maxPlayers = in.readInt();
        }
    }

    public boolean getStatus() {
        return status;
    }

    public int getPlayers() {
        return players;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getMotd() {
        return motd;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    @Override
    public String toString() {
        return "ServerProperties{" +
                "status=" + status +
                ", players=" + players +
                ", maxPlayers=" + maxPlayers +
                ", motd='" + motd + '\'' +
                '}';
    }
}
