package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.stream.Collectors;
import java.util.UUID;

@BPvPListener
@Singleton
public class FreezingListener implements Listener {

    private final EffectManager effectManager;

    private final Set<UUID> playersWithFreezingEffect = new HashSet<>();


    @Inject
    public FreezingListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    // Apply frost damage every 1.5 seconds (30 ticks) to players affected by the "FREEZING" effect
    @UpdateEvent(delay = 30)
    public void applyFrostDamage() {
        Set<LivingEntity> affectedEntities = effectManager.getAllEntitiesWithEffects().stream()
                .filter(entity -> effectManager.hasEffect(entity, EffectTypes.FREEZING)) // Filter to only those with the FREEZING effect
                .collect(Collectors.toSet());

        for (LivingEntity entity : affectedEntities) {
            entity.damage(1.0);
            }
        }

   @EventHandler
public void onReceiveFreezingEffect(EffectReceiveEvent event) {
    // Check if the effect is "FREEZING"
    if (event.isCancelled()) return;
    if (event.getEffect().getEffectType() == EffectTypes.FREEZING) {
        LivingEntity target = event.getTarget();

        // Apply the freezing effect
        if (target instanceof Player player) {
            // Get the duration of the effect in seconds
            long effectDurationMillis = event.getEffect().getRemainingDuration();
            long effectDurationSeconds = effectDurationMillis / 1000;

            // Convert duration to ticks (1 second = 20 ticks)
            int freezeTicks = (int) effectDurationSeconds * 20;

            // Apply freezeTicks to the player
            player.setFreezeTicks(freezeTicks);

            // Play the glass-breaking sound only when receiving the effect
         UUID playerUUID = player.getUniqueId();
            if (!playersWithFreezingEffect.contains(playerUUID)) {
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.0f);
                playersWithFreezingEffect.add(playerUUID);
                }
            }
        }
    }
}

