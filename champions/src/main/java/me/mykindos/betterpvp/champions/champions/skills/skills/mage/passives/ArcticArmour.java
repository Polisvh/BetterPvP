package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.WorldSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

@Singleton
@BPvPListener
public class ArcticArmour extends ActiveToggleSkill implements EnergySkill, DefensiveSkill, TeamSkill, DebuffSkill, BuffSkill, WorldSkill {

    private final WorldBlockHandler blockHandler;

    private int baseRadius;
    private int radiusIncreasePerLevel;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int resistanceStrength;
    
    private double freezeDuration;
    private double freezeTimeRequired;
    private double freezeDurationIncreasePerLevel;
    private double freezeTimeRequiredDecreasePerLevel;

    private final Map<UUID, Long> playersInRangeTimer = new HashMap<>();

    @Inject
    public ArcticArmour(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Arctic Armour";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Create a freezing area around",
                "you in a " + getValueString(this::getRadius, level) + " Block radius",
                "",
                "Allies inside this area receive <effect>Resistance " + UtilFormat.getRomanNumeral(resistanceStrength) + "</effect>, and",
                "enemies that remain in the area for " + getValueString(this::getFreezeTimeRequired, level) + " seconds <effect>freeze</effect> for",
                getValueString(this::getFreezeDuration, level) + " seconds",
                "",
                "Uses " + getValueString(this::getEnergyStartCost, level) + " energy on activation",
                "Energy / Second: " + getValueString(this::getEnergy, level),
                EffectTypes.RESISTANCE.getDescription(resistanceStrength),
                EffectTypes.FREEZING.getGenericDescription()
        };
    }

    public int getRadius(int level) {
        return baseRadius + ((level - 1) * radiusIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }
    public double getFreezeDuration(int level) {
        return freezeDuration + ((level - 1) * freezeDurationIncreasePerLevel);
    }
    public double getFreezeTimeRequired(int level) {
        return freezeTimeRequired - ((level - 1) * freezeTimeRequiredDecreasePerLevel);
    }
    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public boolean process(Player player) {

        HashMap<String, Long> updateCooldowns = updaterCooldowns.get(player.getUniqueId());

        if (updateCooldowns.getOrDefault("audio", 0L) < System.currentTimeMillis()) {
            audio(player);
            updateCooldowns.put("audio", System.currentTimeMillis() + 1000);
        }

        if (updateCooldowns.getOrDefault("snowAura", 0L) < System.currentTimeMillis()) {
            snowAura(player);
            updateCooldowns.put("snowAura", System.currentTimeMillis() + 100);
        }

        return doArcticArmour(player);
    }

    private void audio(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.3F, 0.0F);
    }

    private boolean doArcticArmour(Player player) {
        int level = getLevel(player);
        final int distance = getRadius(level);
        if (level <= 0) {
            return false;
        }

        if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
            return false;
        }

        // Apply resistance and slow effects
        final List<KeyValue<Player, EntityProperty>> nearby = UtilPlayer.getNearbyPlayers(player, distance);
        nearby.add(new KeyValue<>(player, EntityProperty.FRIENDLY));
        for (KeyValue<Player, EntityProperty> nearbyEnt : nearby) {
            final Player target = nearbyEnt.getKey();
            final boolean friendly = nearbyEnt.getValue() == EntityProperty.FRIENDLY;

            if (friendly) {
                championsManager.getEffects().addEffect(target, EffectTypes.RESISTANCE, resistanceStrength, 1000);
            } else {
                manageFreezeEffect(player, target);
            }
        }
        return true;
    }

    
    private void manageFreezeEffect(Player player, Player target) {
        UUID targetId = target.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (!playersInRangeTimer.containsKey(targetId)) {
            playersInRangeTimer.put(targetId, currentTime);
        } else {
            long timeInRange = currentTime - playersInRangeTimer.get(targetId);
            if (timeInRange >= getFreezeTimeRequired(getLevel(player)) * 1000) {
                championsManager.getEffects().addEffect(target, EffectTypes.FREEZING, 1, getFreezeDuration(getLevel(player)) * 1000L));

                playersInRangeTimer.remove(targetId);
            }
        }

        if (target.getLocation().distance(player.getLocation()) > getRadius(getLevel(player))) {
            playersInRangeTimer.remove(targetId);
        }
    }

    
    private void snowAura(Player player) {

        int level = getLevel(player);
        final int distance = getRadius(level);
        // Apply cue effects
        // Spin particles around the player in the radius
        final int angle = (int) ((System.currentTimeMillis() / 10) % 360);
        playEffects(player, distance, -angle);
        playEffects(player, distance, angle);
        playEffects(player, distance, -angle + 180f);
        playEffects(player, distance, angle + 180f);

        convertWaterToIce(player, getDuration(level), distance);
    }

    private void playEffects(final Player player, float radius, float angle) {
        final Location reference = player.getLocation();
        reference.setPitch(0);

        final Location relative = UtilLocation.fromAngleDistance(reference, radius, angle);
        final Optional<Location> closestSurface = UtilLocation.getClosestSurfaceBlock(relative, 3, true);
        closestSurface.ifPresent(loc -> loc.add(0, 2.2, 0));
        final Location result = closestSurface.orElse(relative.add(0, 1.2, 0));
        Particle.CLOUD.builder().extra(0).location(result).receivers(60).spawn();
    }

    private void convertWaterToIce(Player player, double duration, int radius) {
        // Sort by height descending
        final HashMap<Block, Double> inRadius = UtilBlock.getInRadius(player.getLocation(), radius);
        Collection<Block> blocks = inRadius.keySet().stream()
                .sorted((b1, b2) -> b2.getLocation().getBlockY() - b1.getLocation().getBlockY())
                .toList();

        for (Block block : blocks) {
            if (block.getLocation().getY() > player.getLocation().getY()) {
                continue;
            }

            final boolean water = UtilBlock.isWater(block);
            if (!water && block.getType() != Material.ICE) {
                continue;
            }

            final Block top = block.getRelative(0, 1, 0);
            if (UtilBlock.isWater(top)) {
                continue;
            }

            if(blockHandler.isRestoreBlock(block, "Ice Prison")) {
                continue;
            }

            final long expiryOffset = (long) (100 * (inRadius.get(block) * radius));
            final long delay = (long) Math.pow((1 - inRadius.get(block)) * radius, 2);
            blockHandler.scheduleRestoreBlock(player, block, Material.ICE, delay, ((long) duration * 1000) + expiryOffset, false);

            final double chance = Math.random();
            if (chance < 0.025) {
                Particle.SNOWFLAKE.builder().extra(0).location(block.getLocation()).receivers(60).spawn();
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public void toggleActive(Player player) {
        if (championsManager.getEnergy().use(player, getName(), getEnergyStartCost(getLevel(player)), false)) {
            UtilMessage.message(player, getClassType().getName(), "Arctic Armour: <green>On");
        }
        else
        {
            cancel(player);
        }

    }
    
    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 4, Integer.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1, Integer.class);
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);

        resistanceStrength = getConfig("resistanceStrength", 1, Integer.class);
        
        freezeDuration = getConfig("freezeDuration", 4.0, Double.class);
        freezeTimeRequired = getConfig("freezeTimeRequired", 5.0, Double.class);
        freezeDurationIncreasePerLevel = getConfig("freezeDurationIncreasePerLevel", 0.0, Double.class);
        freezeTimeRequiredDecreasePerLevel = getConfig("freezeTimeRequiredDecreasePerLevel", 1.0, Double.class);
    }


}
