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
import org.bukkit.event.entity.EntityDamageEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.ProtocolManager;

import java.util.Set;

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
        Set<LivingEntity> affectedEntities = effectManager.getEntitiesWithEffect(EffectTypes.FREEZING);

        for (LivingEntity entity : affectedEntities) {
            // Apply 1 damage to each entity affected by the freezing effect
            entity.damage(1.0); // Deals 1 damage every 1.5 seconds (30 ticks)
        }
    }

    @EventHandler
    public void onReceiveFreezingEffect(EffectReceiveEvent event) {
        // Check if the effect is "FREEZING"
        if (event.isCancelled()) return;
        if (event.getEffect().getEffectType() == EffectTypes.FREEZING) {
            LivingEntity target = event.getTarget();

            // Send custom packets for the "FREEZING" effect (powder snow effect)
            if (target instanceof Player player) {
                // Send the effect packet that simulates the "FREEZING" effect (like being in powder snow)
                sendFreezingPackets(player);

                // Play the glass-breaking sound **only** when receiving the effect
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.0f);
            }
        }
    }

    // This method sends the necessary packets to the player for the visual freezing effect
    private void sendFreezingPackets(Player player) {
        try {
            // Create and send the packet to apply the powder snow overlay (freezing screen effect)
            PacketContainer overlayPacket = new PacketContainer(PacketType.Play.Server.SET_OVERLAY);
            
            // Setting the overlay to powder snow (value 1 corresponds to powder snow screen overlay)
            overlayPacket.getSpecificModifier(int.class).write(0, 1); // 1 corresponds to powder snow overlay
            
            // Send the packet to the player to apply the freezing effect (screen overlay)
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, overlayPacket);

        } catch (Exception e) {
            e.printStackTrace(); // Handle any errors that occur while sending the packet
        }
    }
}

