package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@BPvPListener
@Singleton
public class FreezingListener implements Listener {

    private final EffectManager effectManager;

    // Track players with the Freezing effect
    private final Set<UUID> playersWithFreezingEffect = new HashSet<>();

    // Track when the Freezing effect started
    private final Map<UUID, Long> freezingStartTimes = new HashMap<>();

    // Track the last time players took damage from freezing
    private final Map<UUID, Long> lastDamageTimes = new HashMap<>();

    private static final long DAMAGE_INTERVAL = 1500; // Damage every 1.5 seconds
    private final BetterPvPPlugin plugin; // Add plugin reference

    @Inject
    public FreezingListener(EffectManager effectManager, BetterPvPPlugin plugin) {
        this.effectManager = effectManager;
        this.plugin = plugin; // Store the plugin instance

        // Start periodic tasks for Slowness and Damage
        startFreezingEffectTasks();
    }

    @EventHandler
    public void onReceiveFreezingEffect(EffectReceiveEvent event) {
        if (event.isCancelled()) return;

        // Check if the effect is "FREEZING"
        if (event.getEffect().getEffectType() == EffectTypes.FREEZING) {
            LivingEntity target = event.getTarget();

            if (target instanceof Player player) {
                UUID playerUUID = player.getUniqueId();

                // Register the start time of the Freezing effect
                freezingStartTimes.put(playerUUID, System.currentTimeMillis());

                // Play sound if not already played
                if (!playersWithFreezingEffect.contains(playerUUID)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.0f);
                    playersWithFreezingEffect.add(playerUUID);
                }
            }
        }
    }

    private void startFreezingEffectTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                // Iterate over players with the freezing effect
                Iterator<UUID> iterator = playersWithFreezingEffect.iterator();
                while (iterator.hasNext()) {
                    UUID playerUUID = iterator.next();
                    Player player = getPlayerByUUID(playerUUID);

                    if (player == null || !effectManager.hasEffect(player, EffectTypes.FREEZING)) {
                        // Remove players no longer affected by Freezing
                        freezingStartTimes.remove(playerUUID);
                        lastDamageTimes.remove(playerUUID);
                        iterator.remove();
                        continue;
                    }

                    long startTime = freezingStartTimes.getOrDefault(playerUUID, currentTime);

                    // Check if the player has had the Freezing effect for 140 ticks (7 seconds)
                    if (currentTime - startTime >= 7000) {
                        // Apply Slowness effect
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 140, 1, true, true));
                    }

                    // Apply periodic damage if enough time has passed
                    long lastDamageTime = lastDamageTimes.getOrDefault(playerUUID, 0L);
                    if (currentTime - lastDamageTime >= DAMAGE_INTERVAL) {
                        player.damage(1.0); // Apply 1 damage
                        lastDamageTimes.put(playerUUID, currentTime); // Update last damage time
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Use the plugin instance here
    }

    private Player getPlayerByUUID(UUID uuid) {
        return Bukkit.getPlayer(uuid); // Fetch player by UUID
    }

    @EventHandler
    public void onEffectEnd(EffectReceiveEvent event) {
        if (event.isCancelled()) return;

        // Check if the effect is "FREEZING"
        if (event.getEffect().getEffectType() == EffectTypes.FREEZING) {
            LivingEntity target = event.getTarget();
            if (target instanceof Player player) {
                UUID playerUUID = player.getUniqueId();

                // Clean up after the effect ends
                freezingStartTimes.remove(playerUUID);
                playersWithFreezingEffect.remove(playerUUID);
                lastDamageTimes.remove(playerUUID);
            }
        }
    }
}
