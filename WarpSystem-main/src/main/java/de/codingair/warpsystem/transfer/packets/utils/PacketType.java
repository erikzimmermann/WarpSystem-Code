package de.codingair.warpsystem.transfer.packets.utils;

import de.codingair.warpsystem.bungee.base.utils.PacketVanishInfo;
import de.codingair.warpsystem.spigot.base.setupassistant.bungee.SetupAssistantStorePacket;
import de.codingair.warpsystem.spigot.base.setupassistant.bungee.ToggleSetupAssistantPacket;
import de.codingair.warpsystem.spigot.base.utils.cooldown.CooldownDataPacket;
import de.codingair.warpsystem.spigot.base.utils.cooldown.CooldownPacket;
import de.codingair.warpsystem.spigot.features.randomteleports.packets.QueueRTPUsagePacket;
import de.codingair.warpsystem.spigot.features.randomteleports.packets.RandomTPPacket;
import de.codingair.warpsystem.spigot.features.randomteleports.packets.RandomTPWorldsPacket;
import de.codingair.warpsystem.spigot.features.teleportcommand.packets.*;
import de.codingair.warpsystem.transfer.jar.SendJarPacket;
import de.codingair.warpsystem.transfer.packets.bungee.*;
import de.codingair.warpsystem.transfer.packets.general.*;
import de.codingair.warpsystem.transfer.packets.spigot.*;

public enum PacketType {
    InitialPacket(InitialPacket.class),
    RequestInitialPacket(RequestInitialPacket.class),
    RequestServerStatusPacket(RequestServerStatusPacket.class),
    ChatInputGUITogglePacket(ChatInputGUITogglePacket.class),
    SendGlobalSpawnOptionsPacket(SendGlobalSpawnOptionsPacket.class),
    TeleportSpawnPacket(TeleportSpawnPacket.class),
    PacketVanishInfo(PacketVanishInfo.class),
    SendJarPacket(SendJarPacket.class),
    SendOptionsPacket(SendOptionsPacket.class),

    PublishGlobalWarpPacket(PublishGlobalWarpPacket.class),
    GlobalWarpTeleportPacket(GlobalWarpTeleportPacket.class),
    TeleportPacket(GlobalWarpTeleportPacket.class),
    DeleteGlobalWarpPacket(DeleteGlobalWarpPacket.class),
    RequestGlobalWarpNamesPacket(RequestGlobalWarpNamesPacket.class),
    SendGlobalWarpNamesPacket(SendGlobalWarpNamesPacket.class),
    UpdateGlobalWarpPacket(UpdateGlobalWarpPacket.class),
    PerformCommandOnSpigotPacket(PerformCommandOnSpigotPacket.class),
    PerformCommandOnBungeePacket(PerformCommandOnBungeePacket.class),
    RequestUUIDPacket(RequestUUIDPacket.class),
    SendUUIDPacket(SendUUIDPacket.class),
    TeleportPlayerToPlayerPacket(TeleportPlayerToPlayerPacket.class),
    TeleportPlayerToCoordsPacket(TeleportPlayerToCoordsPacket.class),
    PrepareServerSwitchPacket(PrepareServerSwitchPacket.class),
    PrepareLoginMessagePacket(PrepareLoginMessagePacket.class),
    MessagePacket(MessagePacket.class),
    CooldownPacket(CooldownPacket.class),
    CooldownDataPacket(CooldownDataPacket.class),

    TeleportCommandOptions(TeleportCommandOptionsPacket.class),
    TeleportRequestHandledPacket(TeleportRequestHandledPacket.class),
    PrepareTeleportPlayerToPlayerPacket(PrepareTeleportPlayerToPlayerPacket.class),
    PrepareTeleportRequestPacket(PrepareTeleportRequestPacket.class),
    StartTeleportToPlayerPacket(StartTeleportToPlayerPacket.class),
    ToggleForceTeleportsPacket(ToggleForceTeleportsPacket.class),
    PrepareTeleportPacket(PrepareTeleportPacket.class),

    SendPlayerWarpsPacket(SendPlayerWarpsPacket.class),
    RegisterServerForPlayerWarpsPacket(RegisterServerForPlayerWarpsPacket.class),
    MoveLocalPlayerWarpsPacket(MoveLocalPlayerWarpsPacket.class),
    SendPlayerWarpUpdatesPacket(SendPlayerWarpUpdatePacket.class),
    PrepareCoordinationTeleportPacket(PrepareCoordinationTeleportPacket.class),
    SendPlayerWarpOptionsPacket(SendPlayerWarpOptionsPacket.class),
    DeletePlayerWarpPacket(DeletePlayerWarpPacket.class),
    PlayerWarpTeleportProcessPacket(PlayerWarpTeleportProcessPacket.class),

    RandomTPPacket(RandomTPPacket.class),
    RandomTPWorldsPacket(RandomTPWorldsPacket.class),
    QueueRTPUsagePacket(QueueRTPUsagePacket.class),
    ToggleSetupAssistantPacket(ToggleSetupAssistantPacket.class),
    SetupAssistantStorePacket(SetupAssistantStorePacket.class),

    BooleanPacket(BooleanPacket.class),
    IntegerPacket(IntegerPacket.class),
    LongPacket(LongPacket.class),
    StringPacket(StringPacket.class),

    AnswerPacket(AnswerPacket.class),
    RequestFullNamePacket(RequestFullNamePacket.class),

    SendServerPropertiesPacket(SendServerPropertiesPacket.class),
    ;

    private final Class<?> packet;

    PacketType(Class<?> packet) {
        this.packet = packet;
    }

    public static PacketType getById(int id) {
        for(PacketType packetType : values()) {
            if(packetType.getId() == id) return packetType;
        }

        return null;
    }

    public static PacketType getByObject(Object packet) {
        if(packet == null) return null;

        for(PacketType packetType : values()) {
            if(packetType.getPacket().equals(packet.getClass())) return packetType;
        }

        return null;
    }

    public int getId() {
        return ordinal();
    }

    public Class<?> getPacket() {
        return packet;
    }
}
