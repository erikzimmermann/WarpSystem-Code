package de.codingair.warpsystem.spigot.base.utils.teleport.process;

import de.codingair.codingapi.tools.Callback;
import de.codingair.codingapi.utils.Value;
import de.codingair.warpsystem.api.destinations.utils.Result;
import de.codingair.warpsystem.api.events.AsyncPlayerTeleportEvent;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.money.Bank;
import de.codingair.warpsystem.spigot.base.utils.teleport.TeleportOptions;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.versionfactory.VFac;
import de.codingair.warpsystem.spigot.versionfactory.VKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Teleport {
    private final Player player;
    private final TeleportOptions options;
    protected boolean motionRestricted = false;
    private TeleportStage stage;
    private long started = 0;

    public Teleport(Player player, TeleportOptions options) {
        this.player = player;
        this.options = options;
    }

    public Teleport start() {
        Bukkit.getScheduler().runTaskAsynchronously(WarpSystem.getInstance(), () -> {
            //run event async
            AsyncPlayerTeleportEvent event = new AsyncPlayerTeleportEvent(player, options);
            Bukkit.getPluginManager().callEvent(event);

            //go sync again
            Bukkit.getScheduler().runTask(WarpSystem.getInstance(), () -> {
                if (event.isCancelled()) {
                    options.fireCallbacks(Result.CANCELLED_BY_EXTERNAL);
                    return;
                }

                started = System.currentTimeMillis();
                Value<Location> afterEffectPosition = new Value<>(player.getLocation());

                options.addCallback(new Callback<Result>() {
                    @Override
                    public void accept(Result result) {
                        if (stage != null && result != Result.SUCCESS && stage.active().isBefore(ConfirmPayment.class) && Bank.adapter() != null) {
                            //payback
                            double costs = options.getCosts(player);
                            if (costs > 0) Bank.adapter().deposit(player, costs);
                        }
                    }
                });

                stage = new SimulateStage(Teleport.this)
                        .then(new WaitWhileMoving())
                        .then(new ConfirmPayment())
                        .then(new TeleportDelay())
                        .then(new PlayerTeleport(afterEffectPosition));

                TeleportStage post = VFac.buildOr(VKey.TeleportPostProcessing, null);
                if (post != null) stage = stage.then(post);

                stage = stage.then(new AfterEffects(afterEffectPosition))
                        .begin();
            });
        });

        return this;
    }

    public void cancel(Result result) {
        if (this.stage != null) this.stage.active().cancel(result);
        cancelByStage(result);
    }

    public void cancelByStage(Result result) {
        if (options.getCancelSound() != null) {
            if (stage != null) {
                TeleportStage active = stage.active();
                if (active instanceof TeleportDelay && getOptions().getDelay(player) > 0) options.getCancelSound().play(player);
                else if (active instanceof WaitWhileMoving) options.getCancelSound().play(player);
            }
        }
        options.fireCallbacks(result);

        if (result == Result.NOT_ENOUGH_MONEY) {
            player.sendMessage(Lang.getPrefix() + Lang.get("Not_Enough_Money").replace("%AMOUNT%", options.getFinalCosts(player).toString()));
        }

        if (result == Result.TARGET_SERVER_IS_FULL) {
            player.sendMessage(Lang.getPrefix() + Lang.get("Target_Server_Is_Full"));
        }

        if (result == Result.DENIED_PAYMENT) {
            String message = options.getPaymentDeniedMessage(player);
            if (message != null) player.sendMessage(message);
        }
    }

    public boolean expired() {
        return options.expired();
    }

    public Player getPlayer() {
        return player;
    }

    public TeleportOptions getOptions() {
        return options;
    }

    public long getStartTime() {
        return started;
    }

    public boolean isCanMove() {
        return options.isCanMove() || !motionRestricted;
    }

    public Destination getDestination() {
        return options.getOriginalDestination();
    }
}
