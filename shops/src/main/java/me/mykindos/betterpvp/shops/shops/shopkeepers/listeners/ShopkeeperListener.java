package me.mykindos.betterpvp.shops.shops.shopkeepers.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownDisplayEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.ParrotShopkeeper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@BPvPListener
public class ShopkeeperListener implements Listener {

    private final ShopkeeperManager shopkeeperManager;

    @Inject
    public ShopkeeperListener(ShopkeeperManager shopkeeperManager) {
        this.shopkeeperManager = shopkeeperManager;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (shopkeeperManager.getObject(event.getDamagee().getUniqueId()).isPresent()) {
            event.cancel("Cannot damage shopkeepers");
        }
    }

    @EventHandler
    public void onFetchNearbyEntity(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(entity -> shopkeeperManager.getObject(entity.getKey().getUniqueId()).isPresent());
    }

    private static final Material[] MUSIC_DISC_MATERIALS = {
            Material.MUSIC_DISC_RELIC,
            Material.MUSIC_DISC_OTHERSIDE,
            Material.MUSIC_DISC_PIGSTEP
    };

    @UpdateEvent(delay = 1000)
    public void playParrotMusic() {

        Material song = MUSIC_DISC_MATERIALS[UtilMath.randomInt(MUSIC_DISC_MATERIALS.length)];
        for (var shopkeeper : shopkeeperManager.getObjects().values()) {
            if (shopkeeper instanceof ParrotShopkeeper) {
                Block block = shopkeeper.getEntity().getLocation().subtract(0, 2, 0).getBlock();
                if (block.getType() == Material.JUKEBOX) {
                    if (block.getState() instanceof Jukebox jukeboxState) {
                        if (!jukeboxState.isPlaying()) {
                            jukeboxState.setRecord(new ItemStack(song));
                            jukeboxState.update();
                        }
                    }
                }
            }
        }

    }


}
