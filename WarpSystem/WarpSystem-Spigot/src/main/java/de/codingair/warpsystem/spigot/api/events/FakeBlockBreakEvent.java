package de.codingair.warpsystem.spigot.api.events;

import com.mojang.authlib.GameProfile;
import de.codingair.codingapi.nms.NmsLoader;
import de.codingair.codingapi.player.data.GameProfileUtils;
import de.codingair.codingapi.server.reflections.IReflection;
import de.codingair.codingapi.server.reflections.PacketUtils;
import de.codingair.codingapi.server.specification.Version;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class FakeBlockBreakEvent extends BlockBreakEvent {
    private static final IReflection.ConstructorAccessor PLAYER;
    private static IReflection.MethodAccessor DEFAULT_CLIENT_INFORMATION;
    private static Function<GameProfile, Object> COMMON_LISTENER_COOKIE;
    private static final IReflection.ConstructorAccessor PLAYER_INTERACT_MANAGER;
    private static final IReflection.ConstructorAccessor PLAYER_CONNECTION;
    private static final Function<Player, Object> NETWORK_MANAGER;
    private static Consumer<Object> MUTE_NETWORK_MANAGER;
    private static final Object PROTOCOL_DIRECTION;
    private static final IReflection.FieldAccessor<PermissibleBase> PERMISSION_BASE = IReflection.getField(PacketUtils.CraftPlayerClass, PermissibleBase.class, 0);
    private static final IReflection.FieldAccessor<?> PLAYER_CONNECTION_FIELD = IReflection.getField(PacketUtils.EntityPlayerClass, PacketUtils.PlayerConnectionClass, 0);

    private static final Class<?> PlayerInteractManagerClass = IReflection.getClass(IReflection.ServerPacket.MINECRAFT_PACKAGE("net.minecraft.server.level"), Version.choose("PlayerInteractManager", 21.11, "ServerPlayerGameMode"));

    static {
        Class<?> protocolDirection = IReflection.getClass(IReflection.ServerPacket.PROTOCOL, Version.choose("EnumProtocolDirection", 21.11, "PacketFlow"));

        if (Version.atLeast(20.02)) {
            Class<?> clientInformationClass = IReflection.getClass("net.minecraft.server.level.", "ClientInformation");
            Class<?> commonListenerCookieClass = IReflection.getClass("net.minecraft.server.network.", "CommonListenerCookie");
            DEFAULT_CLIENT_INFORMATION = IReflection.getMethod(clientInformationClass, clientInformationClass, new Class[0]);

            IReflection.MethodAccessor getCookie;
            if (Version.atLeast(20.05)) {
                getCookie = IReflection.getMethod(commonListenerCookieClass, commonListenerCookieClass, new Class[]{GameProfile.class, boolean.class});
                COMMON_LISTENER_COOKIE = (profile) -> getCookie.invoke(null, profile, false);
            } else {
                getCookie = IReflection.getMethod(commonListenerCookieClass, commonListenerCookieClass, new Class[]{GameProfile.class});
                COMMON_LISTENER_COOKIE = (profile) -> getCookie.invoke(null, profile);
            }

            PLAYER_INTERACT_MANAGER = null;
            PLAYER = IReflection.getConstructor(PacketUtils.EntityPlayerClass, PacketUtils.MinecraftServerClass, PacketUtils.WorldServerClass, GameProfile.class, clientInformationClass);
            PLAYER_CONNECTION = IReflection.getConstructor(PacketUtils.PlayerConnectionClass, PacketUtils.MinecraftServerClass, PacketUtils.NetworkManagerClass, PacketUtils.EntityPlayerClass, commonListenerCookieClass);
            PROTOCOL_DIRECTION = protocolDirection.getEnumConstants()[0];

            IReflection.ConstructorAccessor networkManagerCon = IReflection.getConstructor(PacketUtils.NetworkManagerClass, protocolDirection);
            IReflection.FieldAccessor<?> networkManagerField = IReflection.getField(PacketUtils.PlayerConnectionClass, PacketUtils.NetworkManagerClass, 0);
            Class<?> channelClass = IReflection.getClass("io.netty.channel.", "Channel");
            IReflection.FieldAccessor<?> channelField = IReflection.getField(PacketUtils.NetworkManagerClass, channelClass, 0);
            NETWORK_MANAGER = p -> {
                Object man = networkManagerCon.newInstance(PROTOCOL_DIRECTION);
                // forward channel of original player to not run into errors -> MUST be removed
                // after instantiating the player connection
                channelField.set(man, channelField.get(networkManagerField.get(PacketUtils.getPlayerConnection(p))));
                return man;
            };

            MUTE_NETWORK_MANAGER = man -> channelField.set(man, null);
        } else {
            if (Version.between(19, 19.02)) {
                Class<?> profilePublicKeyClass = IReflection.getClass("net.minecraft.world.entity.player.", "ProfilePublicKey");
                PLAYER_INTERACT_MANAGER = null;
                PLAYER = IReflection.getConstructor(PacketUtils.EntityPlayerClass, PacketUtils.MinecraftServerClass, PacketUtils.WorldServerClass, GameProfile.class, profilePublicKeyClass);
            } else if (Version.atLeast(17)) {
                // also for 1.19.3
                PLAYER_INTERACT_MANAGER = null;
                PLAYER = IReflection.getConstructor(PacketUtils.EntityPlayerClass, PacketUtils.MinecraftServerClass, PacketUtils.WorldServerClass, GameProfile.class);
            } else {
                PLAYER_INTERACT_MANAGER = IReflection.getConstructor(PlayerInteractManagerClass, PacketUtils.WorldServerClass);
                PLAYER = IReflection.getConstructor(PacketUtils.EntityPlayerClass, PacketUtils.MinecraftServerClass, PacketUtils.WorldServerClass, GameProfile.class, PlayerInteractManagerClass);
            }

            PLAYER_CONNECTION = IReflection.getConstructor(PacketUtils.PlayerConnectionClass, PacketUtils.MinecraftServerClass, PacketUtils.NetworkManagerClass, PacketUtils.EntityPlayerClass);
            PROTOCOL_DIRECTION = protocolDirection.getEnumConstants()[1];

            IReflection.ConstructorAccessor networkManagerCon = IReflection.getConstructor(PacketUtils.NetworkManagerClass, protocolDirection);
            NETWORK_MANAGER = p -> networkManagerCon.newInstance(PROTOCOL_DIRECTION);
        }
    }

    @NmsLoader
    private FakeBlockBreakEvent() {
        //noinspection DataFlowIssue
        this(null, null);
    }

    public FakeBlockBreakEvent(@NotNull Block theBlock, @NotNull Player player) {
        super(theBlock, player);
    }

    /**
     * The position of the player will be checked for any protection.
     *
     * @param player The player we use to create our fake player to simulate the BlockBreakEvent. We use fake players to prevent player specific particles from plugins like WorldGuard
     * @return true if the location is protected (the event is cancelled).
     */
    public static boolean tryFake(@NotNull Player player) {
        return tryFake(player, player.getLocation());
    }

    /**
     * @param player   The player we use to create our fake player to simulate the BlockBreakEvent. We use fake players to prevent player specific particles from plugins like WorldGuard
     * @param location The Location where we try to build.
     * @return true if the location is protected (the event is cancelled).
     */
    public static boolean tryFake(@NotNull Player player, @NotNull Location location) {
        return tryWithFake(buildFake(player), location);
    }

    /**
     * @param fakePlayer The player who simulate the BlockBreakEvent. Use fake players to prevent player specific particles from plugins like WorldGuard
     * @param location   The Location where we try to build.
     * @return true if the location is protected (the event is cancelled).
     */
    public static boolean tryWithFake(@NotNull Player fakePlayer, @NotNull Location location) {
        FakeBlockBreakEvent event = new FakeBlockBreakEvent(location.getBlock(), fakePlayer);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    @NotNull
    public static Player buildFake(@NotNull Player player) {
        return buildFake(player, player);
    }

    /**
     * @param player       The player which should be copied.
     * @param onlinePlayer An online player which is already initialized (after PlayerSpawnEvent).
     * @return The fake player.
     */
    @NotNull
    public static Player buildFake(@NotNull Player player, @NotNull Player onlinePlayer) {
        GameProfile profile = GameProfileUtils.getGameProfile(player);
        Object fakePlayer = createFakePlayerInstance(player, profile);

        // use another online player to avoid NPEs when not all fields are initialized yet
        PLAYER_CONNECTION_FIELD.set(fakePlayer, createConnectionDump(onlinePlayer, fakePlayer, profile));

        Player craftFakePlayer = (Player) PacketUtils.getBukkitEntity(fakePlayer);

        craftFakePlayer.setGameMode(player.getGameMode());

        //apply permissible base
        PERMISSION_BASE.set(craftFakePlayer, new PermissibleBase(player));

        return craftFakePlayer;
    }

    private static Object createFakePlayerInstance(@NotNull Player player, GameProfile profile) {
        if (PLAYER_INTERACT_MANAGER != null)
            return PLAYER.newInstance(
                    PacketUtils.getMinecraftServer(),
                    PacketUtils.getWorldServer(player.getWorld()),
                    profile,
                    PLAYER_INTERACT_MANAGER.newInstance(PacketUtils.getWorldServer(player.getWorld()))
            );
        else if (Version.atLeast(20.02))
            return PLAYER.newInstance(
                    PacketUtils.getMinecraftServer(),
                    PacketUtils.getWorldServer(player.getWorld()),
                    profile,
                    DEFAULT_CLIENT_INFORMATION.invoke(null)
            );
        else if (Version.between(19, 19.02))
            return PLAYER.newInstance(
                    PacketUtils.getMinecraftServer(),
                    PacketUtils.getWorldServer(player.getWorld()),
                    profile,
                    null
            );
        else return PLAYER.newInstance(
                    PacketUtils.getMinecraftServer(),
                    PacketUtils.getWorldServer(player.getWorld()),
                    profile
            );
    }

    private static Object createConnectionDump(@NotNull Player player, @NotNull Object fakePlayer, @NotNull GameProfile profile) {
        if (Version.atLeast(20.02)) {
            Object networkManager = NETWORK_MANAGER.apply(player);
            Object playerCon = PLAYER_CONNECTION.newInstance(PacketUtils.getMinecraftServer(), networkManager, fakePlayer, COMMON_LISTENER_COOKIE.apply(profile));
            MUTE_NETWORK_MANAGER.accept(networkManager);
            return playerCon;
        } else {
            return PLAYER_CONNECTION.newInstance(PacketUtils.getMinecraftServer(), NETWORK_MANAGER.apply(player), fakePlayer);
        }
    }
}
