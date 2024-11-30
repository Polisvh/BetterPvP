package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class FreezingListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public FreezingListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onReceiveFreezingEffect(EffectReceiveEvent event) {
        // Check if the effect is "FREEZING" and not cancelled
        if (event.isCancelled() || event.getEffect().getEffectType() != EffectTypes.FREEZING) {
            return;
        }

        LivingEntity target = event.getTarget();
        if (target instanceof Player player) {
            // Get the duration of the effect in seconds
            long effectDurationMillis = event.getEffect().getRemainingDuration();
            long effectDurationSeconds = effectDurationMillis / 1000;

            // Convert duration to ticks (1 second = 20 ticks)
            int freezeTicks = (int) effectDurationSeconds * 20;

            // Apply freezeTicks to the player
            player.setFreezeTicks(freezeTicks);
        }
    }
}

