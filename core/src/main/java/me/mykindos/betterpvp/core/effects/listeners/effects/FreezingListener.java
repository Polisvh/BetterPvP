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

import java.util.Map;
import java.util.HashMap;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.stream.Collectors;
import java.util.*;
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

    
public class FrostDamageHandler {

    private final Map<LivingEntity, Long> lastDamageTimes = new HashMap<>(); // To store the last damage time for each player
    private final long DAMAGE_INTERVAL = 30L; // 30 ticks = 1.5 seconds

    @UpdateEvent(delay = 30) // This is the delay between event ticks (30 ticks = 1.5 seconds)
    public void applyFrostDamage() {
        long currentTime = System.currentTimeMillis(); // Get the current time in milliseconds
        
        // Iterate over all entities affected by the "FREEZING" effect
        Set<LivingEntity> affectedEntities = effectManager.getAllEntitiesWithEffects().stream()
            .filter(entity -> effectManager.hasEffect(entity, EffectTypes.FREEZING)) // Filter to those with the FREEZING effect
            .collect(Collectors.toSet());

        for (LivingEntity entity : affectedEntities) {
            // Check the last time the entity was damaged
            Long lastDamageTime = lastDamageTimes.get(entity);

            // If it's been more than 1.5 seconds (30 ticks), apply damage
            if (lastDamageTime == null || (currentTime - lastDamageTime) >= DAMAGE_INTERVAL * 50) {
                entity.damage(1.0); // Apply 1 damage
                lastDamageTimes.put(entity, currentTime); // Update the last damage time
            }
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
