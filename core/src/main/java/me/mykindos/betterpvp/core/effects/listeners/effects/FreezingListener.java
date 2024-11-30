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

import java.util.Set;
import java.util.stream.Collectors;

@BPvPListener
@Singleton
public class FreezingListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public FreezingListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    // Apply frost damage every 1.5 seconds (30 ticks) to players affected by the "FREEZING" effect
    @UpdateEvent(delay = 30)
    public void applyFrostDamage() {
        // Get all entities affected by the "FREEZING" effect
        Set<LivingEntity> affectedEntities = effectManager.getAllEntitiesWithEffects().stream()
                .filter(entity -> effectManager.hasEffect(entity, EffectTypes.FREEZING)) // Filter to only those with the FREEZING effect
                .collect(Collectors.toSet());

        for (LivingEntity entity : affectedEntities) {
            // Apply 1 damage to each entity affected by the freezing effect
            entity.damage(1.0); // Deals 1 damage every 1.5 seconds (30 ticks)
            
            // Check if the entity is a player, and freeze if it is
            if (entity instanceof Player player) {
                player.setFreezeTicks(140);  // Apply freezing effect to the player
            }
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
                // Freeze the player for a specific duration when receiving the effect
                player.setFreezeTicks(140);

                // Play the glass-breaking sound only when receiving the effect
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.0f);
            }
        }
    }
}
