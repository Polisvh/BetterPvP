package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;

@BPvPListener
@Singleton
public class FreezingListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public FreezingListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFreezeEffect(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        // Check if the player has the "frozen" effect
        Optional<Effect> effectOptional = effectManager.getEffect(player, EffectTypes.FROZEN);
        effectOptional.ifPresent(effect -> {
            // Apply Slowness I effect to the player
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 0, false, false, false));

            // Send freeze ticks metadata (blue hearts)
            sendFreezeTicks(player, 100); // Freeze progress set to 100 (adjust as needed)

            // Send the frost overlay to the player
            sendFrostOverlay(player);

            // Optional: Apply damage or custom logic based on the frozen effect
            // Example: Slow freezing damage
            if (player.getFreezeTicks() >= 140) { // Fully frozen state
                event.setDamage(event.getDamage() + 2.0); // Deal 2 extra damage
            }
        });
    }

    private void sendFreezeTicks(Player player, int freezeTicks) {
        try {
            // Create metadata packet to set freeze ticks
            PacketContainer metadataPacket = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.ENTITY_METADATA);

            metadataPacket.getIntegers().write(0, player.getEntityId()); // Entity ID of the player
            
            // Use WrappedDataWatcher to modify metadata
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            WrappedDataWatcher.WrappedDataWatcherObject freezeTicksIndex = 
                new WrappedDataWatcher.WrappedDataWatcherObject(17, WrappedDataWatcher.Registry.get(Integer.class)); // Index 17 is typically freeze ticks
            watcher.setObject(freezeTicksIndex, freezeTicks);

            metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, metadataPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFrostOverlay(Player player) {
        try {
            // Create overlay packet to send frost screen effect
            PacketContainer overlayPacket = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.SET_OVERLAY);
            // Set the overlay to powder snow
            overlayPacket.getSpecificModifier(int.class).write(0, 1); // 1 corresponds to powder snow overlay
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, overlayPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
