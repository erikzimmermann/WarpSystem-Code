package de.codingair.warpsystem.spigot.base.utils.teleport.process;

import de.codingair.codingapi.particles.animations.movables.PlayerMid;
import de.codingair.codingapi.player.MessageAPI;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.utils.Lang;
import de.codingair.warpsystem.spigot.base.utils.SoundUtil;
import de.codingair.warpsystem.spigot.features.animations.AnimationManager;
import de.codingair.warpsystem.spigot.features.animations.utils.AnimationPlayer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportDelay extends TeleportStage {
    private AnimationPlayer animation;
    private BukkitRunnable runnable;

    protected TeleportDelay() {
    }

    @Override
    public void destroy() {
        if (this.animation != null) {
            this.animation.setRunning(false);
            this.animation = null;
        }

        if (this.runnable != null) {
            this.runnable.cancel();
            this.runnable = null;
        }

        MessageAPI.sendActionBar(player, null);
    }

    @Override
    public void start() {
        teleport.motionRestricted = true;
        int delay = teleport.getOptions().getDelay(player);
        if (delay == 0) {
            end();
            return;
        }

        boolean hasAnimation = teleport.getOptions().isTeleportAnimation();
        if (hasAnimation) {
            this.animation = new AnimationPlayer(player, new PlayerMid(player), AnimationManager.getInstance().getActive(), delay, true, teleport.getOptions().isPublicAnimations());
            this.animation.setTeleportSound(false);
            this.animation.setRunning(true);
        }

        Display d = WarpSystem.opt().getDelayDisplay();

        String[] text = new String[] {"", ""};
        String msg = Lang.get("Teleporting_Info");

        if (d == Display.TITLE) {
            MessageAPI.sendTitle(player, " ", " ", 0, 1, 0);
            int i = msg.indexOf("\n");
            if (i != -1) {
                text[0] = msg.substring(0, i);
                text[1] = msg.substring(i + 2);
            } else text[0] = msg;
        }

        this.runnable = new BukkitRunnable() {
            private int left = delay;

            @Override
            public void run() {
                if (left == 0) {
                    Bukkit.getScheduler().runTask(WarpSystem.getInstance(), TeleportDelay.this::end);
                    return;
                }

                if (!hasAnimation && AnimationManager.getInstance().getActive().getTickSound() != null) SoundUtil.play(player, AnimationManager.getInstance().getActive().getTickSound());

                if (d == Display.ACTION_BAR) MessageAPI.sendActionBar(player, msg.replace("%seconds%", left + ""));
                else if (d == TeleportDelay.Display.TITLE) {
                    int fadeIn = left == delay ? 5 : 0;
                    int fadeOut = left == 1 ? 5 : 0;
                    MessageAPI.sendTitle(player, text[0].replace("%seconds%", left + ""), text[1].replace("%seconds%", left + ""), fadeIn, 25 - fadeOut, fadeOut, false, false, false);
                }
                left--;
            }
        };

        this.runnable.runTaskTimerAsynchronously(WarpSystem.getInstance(), 0L, 20L);
    }

    @Override
    public void end() {
        teleport.motionRestricted = false;
        super.end();
    }

    public static enum Display {
        ACTION_BAR,
        TITLE;
    }
}
