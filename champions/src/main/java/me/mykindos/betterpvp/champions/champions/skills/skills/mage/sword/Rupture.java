package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.CustomArmourStand;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Rupture extends Skill implements Listener, InteractSkill, CooldownSkill, AreaOfEffectSkill, DebuffSkill, DamageSkill {

    private final WeakHashMap<Player, ArrayList<LivingEntity>> cooldownJump = new WeakHashMap<>();
    private final WeakHashMap<ArmorStand, Long> stands = new WeakHashMap<>();

    private double baseDamage;

    private double damageIncreasePerLevel;

    private double baseSlowDuration;

    private double slowDurationIncreasePerLevel;

    private int slowStrength;

    @Inject
    public Rupture(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Rupture";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Rupture the earth in the direction",
                "you are facing, dealing " + getValueString(this::getDamage, level) + " damage,",
                "knocking up and giving <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> to enemies",
                "hit for " + getValueString(this::getSlowDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @UpdateEvent
    public void onUpdate() {
        stands.entrySet().removeIf(entry -> {
            if (entry.getValue() - System.currentTimeMillis() <= 0) {
                entry.getKey().remove();
                return true;
            }
            return false;
        });
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        final Vector[] vector = {player.getLocation().getDirection().normalize().multiply(0.3D)};
        vector[0].setY(0);
        final Location loc = player.getLocation().subtract(0.0D, 1.0D, 0.0D).add(vector[0]);
        loc.setY(Math.floor(loc.getY()));
        cooldownJump.put(player, new ArrayList<>());

        final BukkitTask runnable = new BukkitRunnable() {

    @Override
    public void run() {
        // If the player is holding right-click, update the path's direction
        if (gamer.isHoldingRightClick()) {
            Vector lookDirection = player.getLocation().getDirection().normalize();
            lookDirection.setY(0);  // Make sure to keep the movement horizontal
            vector[0] = lookDirection.multiply(0.3D);
        }

        // Continue the path even if the player is not holding right-click
        for (int i = 0; i < 3; i++) {
            if ((!UtilBlock.airFoliage(loc.getBlock())) && UtilBlock.solid(loc.getBlock())) {
                loc.add(0.0D, 1.0D, 0.0D);
            }
        }

        if ((!UtilBlock.airFoliage(loc.getBlock())) && UtilBlock.solid(loc.getBlock())) {
            cancel();
            return;
        }

        if (loc.getBlock().getType().name().contains("DOOR")) {
            cancel();
            return;
        }

        if ((loc.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType() == Material.AIR)) {
            Block halfBlock = loc.clone().add(0, -0.5, 0).getBlock();
            if (!halfBlock.getType().name().contains("SLAB") && !halfBlock.getType().name().contains("STAIR")) {
                loc.add(0.0D, -1.0D, 0.0D);
            }
        }

        for (int i = 0; i < 3; i++) {
            loc.add(vector[0]);
            Location tempLoc = new Location(player.getWorld(), loc.getX() + UtilMath.randDouble(-1.5D, 1.5D), loc.getY() + UtilMath.randDouble(0.3D, 0.8D) - 0.75,
                    loc.getZ() + UtilMath.randDouble(-1.5D, 1.5D));

            Block nearestSolidBlock = getNearestSolidBlock(loc);
            if (nearestSolidBlock == null) {
                cancel();
                return;
            }

            CustomArmourStand as = new CustomArmourStand(((CraftWorld) loc.getWorld()).getHandle());
            ArmorStand armourStand = (ArmorStand) as.spawn(tempLoc);
            armourStand.getEquipment().setHelmet(new ItemStack(nearestSolidBlock.getType()));
            armourStand.setGravity(false);
            armourStand.setVisible(false);
            armourStand.setSmall(true);
            armourStand.setPersistent(false);
            armourStand.setHeadPose(new EulerAngle(UtilMath.randomInt(360), UtilMath.randomInt(360), UtilMath.randomInt(360)));

            player.getWorld().playEffect(loc, Effect.STEP_SOUND, nearestSolidBlock.getType());

            stands.put(armourStand, System.currentTimeMillis() + 4000);

            // Schedule the individual removal of this ArmorStand after 1 second (20 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    armourStand.remove();
                }
            }.runTaskLater(champions, 14);

            for (LivingEntity ent : UtilEntity.getNearbyEnemies(player, armourStand.getLocation(), 1)) {
                if (!cooldownJump.get(player).contains(ent)) {

                    Vector knockbackDirection = player.getLocation().getDirection().multiply(-1).normalize();
                    VelocityData velocityData = new VelocityData(knockbackDirection, -1.25, false, 0.0, 1.0, 2.0, false);
                    UtilVelocity.velocity(ent, player, velocityData, VelocityType.CUSTOM);

                    championsManager.getEffects().addEffect(ent, player, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration(level) * 1000L));
                    UtilDamage.doCustomDamage(new CustomDamageEvent(ent, player, null, DamageCause.CUSTOM, getDamage(level), false, getName()));

                    // Trigger explosion effect
                    createExplosionEffect(ent.getLocation(), nearestSolidBlock.getType());

                    cooldownJump.get(player).add(ent);
                }
            }
        }
    }
}.runTaskTimer(champions, 0, 2);


        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.cancel();
                cooldownJump.get(player).clear();
            }
        }.runTaskLater(champions, 40);
    }

    private Block getNearestSolidBlock(Location location) {
        for (int y = 0; y < location.getY(); y++) {
            Block block = location.clone().subtract(0, y, 0).getBlock();
            if (!UtilBlock.airFoliage(block) && UtilBlock.solid(block)) {
                return block;
            }
        }
        return null;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        baseSlowDuration = getConfig("baseSlowDuration", 1.5, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowStrength = getConfig("slowStrength", 3, Integer.class);
    }

    // New method for explosion effect
    private void createExplosionEffect(Location location, Material blockType) {
        for (int i = 0; i < 15; i++) { // Number of debris particles
            // Generate a random offset for debris location
            Location debrisLoc = location.clone().add(
                    UtilMath.randDouble(-0.5, 0.5), // Random X offset
                    UtilMath.randDouble(0.5, 1.0), // Random Y offset
                    UtilMath.randDouble(-0.5, 0.5)  // Random Z offset
            );

            // Create the ThrowableItem representing debris
            ItemStack debrisItem = new ItemStack(blockType);
            Item debrisEntity = location.getWorld().dropItem(debrisLoc, debrisItem);

            // Apply random velocity to simulate debris being thrown out
            Vector velocity = new Vector(
                    UtilMath.randDouble(-0.5, 0.5), // X velocity
                    UtilMath.randDouble(0.5, 1.0), // Y velocity
                    UtilMath.randDouble(-0.5, 0.5)  // Z velocity
            );
            debrisEntity.setVelocity(velocity);

            // Create a ThrowableItem instance
            ThrowableItem throwableItem = new ThrowableItem(
                    (ThrowableListener) this, debrisEntity, null, "RuptureDebris", 1000L // Lifetime in milliseconds
            );
            throwableItem.setRemoveInWater(true); // Optional: Remove debris if it lands in water
            championsManager.getThrowables().addThrowable(throwableItem);

            // Schedule removal of the debris after a short time
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!debrisEntity.isDead()) {
                        debrisEntity.remove();
                    }
                }
            }.runTaskLater(champions, 28); // 28 ticks (~1.4 seconds)
        }
    }
}
