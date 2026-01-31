package de.codingair.warpsystem.spigot.base.utils.teleport;

import de.codingair.codingapi.server.sounds.Sound;
import de.codingair.codingapi.server.sounds.SoundData;
import de.codingair.codingapi.tools.Callback;
import de.codingair.codingapi.utils.ChatColor;
import de.codingair.codingapi.utils.ImprovedDouble;
import de.codingair.warpsystem.api.Options;
import de.codingair.warpsystem.api.destinations.utils.IDestination;
import de.codingair.warpsystem.api.destinations.utils.Result;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.guis.editor.pages.SoundPage;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.Permissions;
import de.codingair.warpsystem.spigot.base.utils.money.Bank;
import de.codingair.warpsystem.spigot.base.utils.options.specific.GeneralOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.adapters.LocationAdapter;
import de.codingair.warpsystem.spigot.features.animations.AnimationManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class TeleportOptions implements Options {
    private final Set<Callback<Result>> callback = new HashSet<>();
    private Origin origin;
    private IDestination destination;

    private String displayName;
    private String permission;
    private double costs;
    private int delay;

    private Boolean skip;
    private boolean canMove;
    private boolean waitForTeleport; //Waiting for walking teleports
    private boolean confirmPayment = true;
    private boolean silent;
    private boolean notifyPlayer = true;

    private String payMessage;
    private String paymentDeniedMessage;
    private String message;
    private String serverNotOnline;

    private SoundData teleportSound;
    private SoundData cancelSound;

    private Boolean afterEffects;
    private boolean publicAnimations;
    private boolean teleportAnimation = true;

    private boolean invulnerability;
    private float invulnerabilityTime;

    public TeleportOptions() {
        this((Destination) null, null);
    }

    public TeleportOptions(@NotNull Location location, @Nullable String displayName) {
        this(location, displayName, Origin.Custom);
    }

    public TeleportOptions(@NotNull Location location, @Nullable String displayName, Origin origin) {
        this(new Destination(new LocationAdapter(location)), displayName, origin);
    }

    public TeleportOptions(@Nullable Destination destination, @Nullable String displayName) {
        this(destination, displayName, Origin.Custom);
    }

    public TeleportOptions(@Nullable Destination destination, @Nullable String displayName, @NotNull Origin origin) {
        this.origin = origin;
        this.destination = destination;
        setDisplayName(displayName);
        this.permission = null;
        this.costs = 0;
        this.delay = WarpSystem.opt().getTeleportDelay();
        this.canMove = WarpSystem.opt().isAllowMove();
        this.waitForTeleport = false;
        this.payMessage = Lang.getPrefix() + Lang.get("Money_Paid");
        this.paymentDeniedMessage = Lang.getPrefix() + Lang.get("Payment_denied");
        this.message = Lang.getPrefix() + Lang.get("Teleported_To");
        this.serverNotOnline = Lang.getPrefix() + Lang.get("Server_Is_Not_Online");
        this.silent = false;
        this.teleportSound = null;
        this.cancelSound = new SoundData(Sound.ENTITY_ITEM_BREAK, 0.7F, 1F);
        if (destination != null) this.afterEffects = destination.getCustomOptions().getParticles();
        this.publicAnimations = WarpSystem.opt().isPublicAnimations();

        invulnerability = WarpSystem.opt().invulnerability();
        invulnerabilityTime = (float) WarpSystem.opt().invulnerabilityTime();
    }

    public Location buildLocation() {
        return destination == null ? null : destination.buildLocation();
    }

    public @NotNull Options destroy() {
        callback.clear();
        return this;
    }

    public Origin getOriginalOrigin() {
        return origin;
    }

    @NotNull
    public de.codingair.warpsystem.api.destinations.utils.Origin getOrigin() {
        return origin.getApiOrigin();
    }

    public TeleportOptions setOrigin(@NotNull Origin origin) {
        this.origin = origin;
        return this;
    }

    public Destination getOriginalDestination() {
        if (destination instanceof Destination) return (Destination) destination;
        return null;
    }

    @Override
    public @NotNull IDestination getDestination() {
        return this.destination;
    }

    @Override
    public @NotNull Options setDestination(@NotNull IDestination iDestination) {
        this.destination = iDestination;
        return this;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public @Nullable String getDisplayName() {
        return displayName;
    }

    public @NotNull Options setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getPermission() {
        return permission;
    }

    public @NotNull Options setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public double getCosts(Player player) {
        if (player.hasPermission(Permissions.PERMISSION_ByPass_Teleport_Costs)) return 0;
        return costs;
    }

    public @NotNull Options setCosts(double costs) {
        this.costs = costs;
        return this;
    }

    public @NotNull Number getFinalCosts(@NotNull Player player) {
        return new ImprovedDouble(costs > 0 && Bank.adapter() != null && !player.hasPermission(Permissions.PERMISSION_ByPass_Teleport_Costs) ? costs : 0).get();
    }

    public boolean isSkip() {
        if (skip == null) return false;
        return skip;
    }

    public Boolean getSkip() {
        return skip;
    }

    public @NotNull Options setSkip(boolean skip) {
        this.skip = skip;
        return this;
    }

    public boolean isCanMove() {
        return canMove;
    }

    public @NotNull Options setCanMove(boolean canMove) {
        this.canMove = canMove;
        return this;
    }

    public boolean isWaitForTeleport() {
        return waitForTeleport;
    }

    public @NotNull Options setWaitForTeleport(boolean waitForTeleport) {
        this.waitForTeleport = waitForTeleport;
        return this;
    }

    public String getMessage() {
        String displayName = this.displayName;

        if (destination instanceof Destination) {
            if (destination.getCustomOptions().getDisplayName() != null) displayName = destination.getCustomOptions().getColoredDisplayName();
        }

        return message == null ? null : displayName == null ? message : message.replace("%warp%", ChatColor.translateAll('&', displayName));
    }

    public @NotNull Options setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isNotifyPlayer() {
        return notifyPlayer;
    }

    public void setNotifyPlayer(boolean notifyPlayer) {
        this.notifyPlayer = notifyPlayer;
    }

    public SoundData getTeleportSound() {
        if (teleportSound == null) teleportSound = AnimationManager.getInstance().getActive().getTeleportSound();
        if (this.teleportSound == null) this.teleportSound = SoundPage.createStandard();
        return teleportSound;
    }

    public @NotNull Options setTeleportSound(SoundData teleportSound) {
        this.teleportSound = teleportSound;
        return this;
    }

    public boolean isAfterEffects() {
        return afterEffects != null ? afterEffects : WarpSystem.opt().isAfterEffects();
    }

    public @NotNull Options setAfterEffects(boolean afterEffects, boolean force) {
        if (this.afterEffects != null && !force) return this;
        this.afterEffects = afterEffects;
        return this;
    }

    public void fireCallbacks(Result result) {
        for (Callback<Result> teleportResultCallback : this.callback) {
            teleportResultCallback.accept(result);
        }

        this.callback.clear();
    }

    public boolean expired() {
        return this.callback.isEmpty();
    }

    public @NotNull TeleportOptions addCallback(@NotNull Callback<Result> callback) {
        this.callback.add(callback);
        return this;
    }

    public String getPayMessage() {
        return payMessage == null ? null : payMessage.replace("%warp%", displayName);
    }

    public @NotNull Options setPayMessage(String payMessage) {
        this.payMessage = payMessage;
        return this;
    }

    public String getFinalMessage(@NotNull Player player) {
        return getFinalCosts(player).doubleValue() > 0 ? getPayMessage() : getMessage();
    }

    public boolean isConfirmPayment() {
        return confirmPayment;
    }

    public @NotNull Options setConfirmPayment(boolean confirmPayment) {
        this.confirmPayment = confirmPayment;
        return this;
    }

    public String getPaymentDeniedMessage(@NotNull Player player) {
        if (this.paymentDeniedMessage == null) return null;
        return this.paymentDeniedMessage.replace("%AMOUNT%", getFinalCosts(player) + "");
    }

    public @NotNull Options setPaymentDeniedMessage(String paymentDeniedMessage) {
        this.paymentDeniedMessage = paymentDeniedMessage;
        return this;
    }

    public boolean isTeleportAnimation() {
        return teleportAnimation;
    }

    public @NotNull Options setTeleportAnimation(boolean teleportAnimation) {
        this.teleportAnimation = teleportAnimation;
        return this;
    }

    public String getServerNotOnline() {
        return serverNotOnline;
    }

    public @NotNull Options setServerNotOnline(String serverNotOnline) {
        this.serverNotOnline = serverNotOnline;
        return this;
    }

    public boolean isPublicAnimations() {
        return publicAnimations;
    }

    public @NotNull Options setPublicAnimations(boolean publicAnimations) {
        this.publicAnimations = publicAnimations;
        return this;
    }

    public SoundData getCancelSound() {
        return cancelSound;
    }

    public @NotNull Options setCancelSound(SoundData cancelSound) {
        this.cancelSound = cancelSound;
        return this;
    }

    public int getDelay(Player player) {
        if (player.hasPermission(Permissions.PERMISSION_ByPass_Teleport_Delay) || (skip != null && skip)) return 0;
        if (destination instanceof Destination) return destination.getCustomOptions().getDelay(delay);
        return delay;
    }

    public @NotNull Options setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    @Override
    public boolean withPostInvulnerability() {
        return invulnerability;
    }

    @Override
    public float postInvulnerabilityDuration() {
        return invulnerabilityTime;
    }

    @Override
    public @NotNull Options setWithPostInvulnerability(boolean invulnerability) {
        this.invulnerability = invulnerability;
        return this;
    }

    @Override
    public @NotNull Options setPostInvulnerabilityDuration(float invulnerabilityTime) {
        this.invulnerabilityTime = invulnerabilityTime;
        return this;
    }
}
