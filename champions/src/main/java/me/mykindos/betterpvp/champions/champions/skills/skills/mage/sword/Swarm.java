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
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

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
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownReductionPerLevel);
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
        if (championsManager.getCooldowns().hasCooldown(cur, getName())) {
                UtilMessage.simpleMessage(cur, "Cooldown", "You cannot use <alt>%s</alt> for <alt>%s</alt> seconds.",
                        getName(),
                        Math.max(0, championsManager.getCooldowns().getAbilityRecharge(cur, getName()).getRemaining()));
                iterator.remove();
                continue;
            }

        // Set cooldown time
        long cooldownTime = (long) (getCooldown(level) * 1000);
        batCD.put(player, currentTime + cooldownTime);

        // Spawn bats immediately
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
        batLifespan = getConfig("batLifespan", 2.0, Double.class);
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
