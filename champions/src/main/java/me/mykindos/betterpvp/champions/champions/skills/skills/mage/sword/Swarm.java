package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Map.Entry;

@Singleton
@BPvPListener
public class Swarm extends ChannelSkill implements InteractSkill, EnergyChannelSkill, Listener {

    private final WeakHashMap<Player, Long> batCD = new WeakHashMap<>();
    private final WeakHashMap<Player, ArrayList<BatData>> batData = new WeakHashMap<>();

    private double batLifespan;
    private double batDamage;
    private double cooldownReductionPerLevel;

    @Inject
    public Swarm(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Swarm";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Release a swarm of bats which",
                "damage and knock back any enemies",
                "they come in contact with",
                "",
                "Energy: " + getValueString(this::getEnergy, level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }
    public double getCooldowns(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
    }



    


    @UpdateEvent
    public void channeling() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()) {
                removeVelocityAndLeash(player);
                iterator.remove();
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
            } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
                iterator.remove();
                removeLeash(player);
            } else if (!isHolding(player)) {
                iterator.remove();
                removeLeash(player);
            } else {
                Bat closestBat = findClosestBat(player);
                if (closestBat != null) {
                    pullPlayerTowardsBat(player, closestBat);
                    leashPlayerToBat(player, closestBat);
                }
            }
        }
    }
    private Bat findClosestBat(Player player) {
        double closestDistance = Double.MAX_VALUE;
        Bat closestBat = null;

        // Iterate over all bats spawned by the player
        for (BatData batData : batData.get(player)) {
            Bat bat = batData.getBat();
            if (bat != null && !bat.isDead()) {
                double distance = player.getLocation().distance(bat.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestBat = bat;
                }
            }
        }
        return closestBat;
    }
    private void pullPlayerTowardsBat(Player player, Bat bat) {
        // Calculate the direction towards the bat
        Vector directionToBat = bat.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

        // Apply the pulling effect to the player (you can modify the speed here)
        player.setVelocity(directionToBat.multiply(0.5)); // Adjust the pulling strength
    }

    private void leashPlayerToBat(Player player, Bat bat) {
        // Set the bat as the leash holder for the player
        // Bats can hold a leash, so we can set the player as the holder of the leash
        bat.setLeashHolder(player);

        // Optionally, you can play a sound or visual effect here to indicate the leash effect
        bat.getWorld().playSound(bat.getLocation(), Sound.ENTITY_BAT_AMBIENT, 0.5F, 1.0F);
    }

    private void removeLeash(Player player) {
        
        for (BatData batData : batData.get(player)) {
            Bat bat = batData.getBat();
            if (bat != null && !bat.isDead()) {
                bat.setLeashHolder(null);
         
            }
        }
    }




    public boolean hitPlayer(Location loc, LivingEntity player) {
        if (loc.add(0, -loc.getY(), 0).toVector().subtract(player.getLocation()
                .add(0, -player.getLocation().getY(), 0).toVector()).length() < 0.8D) {
            return true;
        }
        if (loc.add(0, -loc.getY(), 0).toVector().subtract(player.getLocation()
                .add(0, -player.getLocation().getY(), 0).toVector()).length() < 1.2) {
            return (loc.getY() > player.getLocation().getY()) && (loc.getY() < player.getEyeLocation().getY());
        }
        return false;
    }
    @UpdateEvent(delay = 100)
    public void batHit() {
        for (Player player : batData.keySet()) {
            for (BatData batData : batData.get(player)) {
                Bat bat = batData.getBat();
                Vector rand = new Vector((Math.random() - 0.5D) / 3.0D, (Math.random() - 0.5D) / 3.0D, (Math.random() - 0.5D) / 3.0D);
                bat.setVelocity(batData.getLoc().getDirection().clone().multiply(0.5D).add(rand));

                for (var data : UtilEntity.getNearbyEntities(player, bat.getLocation(), 3, EntityProperty.ENEMY)) {
                    LivingEntity other = data.get();

                    if (other instanceof Bat) continue;
                    if (!hitPlayer(bat.getLocation(), other)) continue;

                    if (other instanceof Player) {
                        if (batCD.containsKey(other)) {
                            if (!UtilTime.elapsed(batCD.get(other), 500)) continue;
                        }
                        batCD.put((Player) other, System.currentTimeMillis());
                        championsManager.getEffects().addEffect(other, EffectTypes.SHOCK, 800L);
                    }

                    final CustomDamageEvent event = new CustomDamageEvent(other,
                            player,
                            null,
                            DamageCause.CUSTOM,
                            batDamage,
                            false,
                            getName());
                    UtilDamage.doCustomDamage(event);

                    if (!event.isCancelled()) {
                        Vector vector = bat.getLocation().getDirection();
                        final VelocityData velocityData = new VelocityData(vector, 0.4d, 0.2d, 7.5d, true);
                        UtilVelocity.velocity(other, player, velocityData);

                        bat.getWorld().playSound(bat.getLocation(), Sound.ENTITY_BAT_HURT, 0.1F, 0.7F);
                    }

                    bat.remove();
                }
            }
        }
    }
    @UpdateEvent(delay = 500)
    public void destroyBats() {

        Iterator<Entry<Player, ArrayList<BatData>>> iterator = batData.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Player, ArrayList<BatData>> data = iterator.next();
            ListIterator<BatData> batIt = data.getValue().listIterator();
            while (batIt.hasNext()) {
                BatData bat = batIt.next();

                if (bat.getBat() == null || bat.getBat().isDead()) {
                    batIt.remove();
                    continue;
                }

                if (UtilTime.elapsed(bat.getTimer(), (long) batLifespan * 1000)) {
                    bat.getBat().remove();
                    batIt.remove();

                }
            }

            if (data.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
    active.add(player.getUniqueId());
    spawnBats(player);
    }



private void spawnBats(Player player) {
    final Vector direction = player.getLocation().getDirection().normalize().multiply(0.3D);
    final Location spawnLocation = player.getEyeLocation().add(direction); // Start near player's eye level

    final int batCount = 32; // Total number of bats to spawn
    for (int i = 0; i < batCount; i++) {
        // Spawn the bat at the player's current eye location
        Bat bat = player.getWorld().spawn(spawnLocation, Bat.class);
        bat.setHealth(1); // Set health to 1
        bat.setMetadata("PlayerSpawned", new FixedMetadataValue(champions, true));
        bat.setVelocity(direction.clone().multiply(0.5)); // Set initial velocity

        // Add to tracking data
        if (!batData.containsKey(player)) {
            batData.put(player, new ArrayList<>());
        }
        batData.get(player).add(new BatData(bat, System.currentTimeMillis(), player.getEyeLocation()));
    }
}

@Override
public void loadSkillConfig() {
    batLifespan = getConfig("batLifespan", 4.0, Double.class);
    batDamage = getConfig("batDamage", 1.0, Double.class);
    cooldown = getConfig("cooldown", 16.0, Double.class);
    cooldownReductionPerLevel = getConfig("cooldownReductionPerLevel", 1.0, Double.class);
}

@Override
public Action[] getActions() {
    return SkillActions.RIGHT_CLICK;
}

@Data
private static class BatData {

    private final Bat bat;
    private final long timer;
    private final Location loc;

}

}

