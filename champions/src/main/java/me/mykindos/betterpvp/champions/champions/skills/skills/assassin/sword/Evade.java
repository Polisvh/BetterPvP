package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.EvadeData;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Color;


import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Evade extends Skill implements InteractSkill, Listener, MovementSkill {

    private final WeakHashMap<Player, EvadeData> charges = new WeakHashMap<>();
    private final WeakHashMap<Player, Vector> movementDirections = new WeakHashMap<>();

    // Action bar
    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        // Only display charges in hotbar if holding the weapon
        if (player == null || !charges.containsKey(player) || !isHolding(player)) {
            return null; // Skip if not online or not charging
        }

        final int maxCharges = getMaxCharges(getLevel(player));
        final int newCharges = charges.get(player).getCharges();

        return Component.text(getName() + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(newCharges)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(Math.max(0, maxCharges - newCharges))).color(NamedTextColor.RED));
    });

    private int baseMaxCharges;

    private int chargeIncreasePerLevel;

    private double baseRechargeSeconds;

    private double rechargeReductionPerLevel;
    private double teleportDistance;

    @Inject
    public Evade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Evade";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Dodge your attacks by teleporting " + getValueString(this::getTeleportDistance, level) + " blocks",
                "in the direction you are moving",
                "",
                "You can perform up to" + getValueString(this::getMaxCharges, level) + " evades",
                "",
                "Gain an evade charge every: " + getValueString(this::getRechargeSeconds, level) + " seconds"
        };
    }

    private int getMaxCharges(int level) {
        return baseMaxCharges + ((level - 1) * chargeIncreasePerLevel);
    }

    private double getRechargeSeconds(int level) {
        return baseRechargeSeconds - ((level - 1) * rechargeReductionPerLevel);
    }

    private double getTeleportDistance(int level) {
        return teleportDistance;
    }

    @Override
    public void loadSkillConfig() {
        baseMaxCharges = getConfig("baseMaxCharges", 2, Integer.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 0, Integer.class);
        baseRechargeSeconds = getConfig("baseRechargeSeconds", 8.0, Double.class);
        rechargeReductionPerLevel = getConfig("rechargeReductionPerLevel", 1.0, Double.class);
        teleportDistance = getConfig("teleportDistance", 3.0, Double.class);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean displayWhenUsed() {
        return false;
    }

    private void notifyCharges(Player player, int charges) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "Evade Charges: <alt2>" + charges);
    }

    public boolean canUse(Player player) {
        EvadeData evadeData = charges.get(player);
        if (evadeData != null && evadeData.getCharges() > 0) {
            return true;
        }

        UtilMessage.simpleMessage(player, getClassType().getName(), "You have no <alt>" + getName() + "</alt> charges.");
        return false;
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        charges.remove(player);
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        charges.computeIfAbsent(player, k -> new EvadeData());
        gamer.getActionBar().add(900, actionBarComponent);
    }

    
@EventHandler
public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Location from = event.getFrom();
    Location to = event.getTo();

    if (from == null || to == null) {
        return;
    }

    // Calculate the current velocity vector
    Vector velocity = to.toVector().subtract(from.toVector());

    // Ignore small movements
    if (velocity.lengthSquared() < 0.01) {
        return;
    }

    // Store the player's current velocity
    movementDirections.put(player, velocity.normalize());
}

    
@Override
public void activate(Player player, int level) {
    final Location origin = player.getLocation();
    Vector movementVector = movementDirections.getOrDefault(player, new Vector(0, 0, 0)); // Get current movement vector

    // Check if the player is moving
    if (movementVector.lengthSquared() <= 0.01) {
        UtilMessage.simpleMessage(player, "Assassin", "You aren't moving in any direction");
        return;
    }

    // Normalize the movement vector and scale it by teleport distance
    Vector teleportVector = movementVector.clone().normalize().multiply(teleportDistance);

    try {
        if (teleportVector.isZero()) {
            UtilMessage.simpleMessage(player, "Assassin", "You aren't moving in any direction!.");
            return;
        }
    } catch (IllegalArgumentException e) {
        UtilMessage.simpleMessage(player, "Assassin", "You aren't moving in any direction!.");
        return;
    }
    // Calculate the destination
    Location teleportLocation = origin.clone().add(teleportVector);

    // Perform the teleport
    UtilLocation.teleportToward(player, teleportVector, teleportDistance, false, success -> {
        if (!Boolean.TRUE.equals(success)) {
            return;
        }

        // Handle cooldown and reduce charges
        EvadeData evadeData = charges.get(player);
        if (evadeData == null) {
            return;
        }

        final int curCharges = evadeData.getCharges();
        final int maxCharges = getMaxCharges(getLevel(player));
        if (curCharges >= maxCharges) {
            championsManager.getCooldowns().use(player, getName(), getRechargeSeconds(getLevel(player)), false, true, true);
        }

        final int newCharges = curCharges - 1;
        evadeData.setCharges(newCharges);

        // Notify charges and play effects
        notifyCharges(player, newCharges);
        final Location lineStart = origin.add(0.0, player.getHeight() / 2, 0.0);
        final Location lineEnd = player.getLocation().clone().add(0.0, player.getHeight() / 2, 0.0);
        final VectorLine line = VectorLine.withStepSize(lineStart, lineEnd, 0.25f);


                // Particle effect
        double playerHeight = player.getHeight(); // Assuming 1.8 for most players, could be different for some
        double particleSpacing = 0.1;
        Location baseLocation = player.getLocation().add(0, 0, 0); // This will be the starting location for particles

        // Loop over the line path and spawn particles at each point
        for (Location point : line.toLocations()) {
            // For each point along the teleport path, spawn particles from the feet to the top of the player
            for (double yOffset = 0; yOffset <= playerHeight; yOffset += particleSpacing) {
                Location particleLocation = point.clone().add(0.0, yOffset, 0.0); // Adjust for height

                // Spawn black particles using FIREWORKS (or another particle type, like SMOKE_NORMAL)
                player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLocation, 1, 0, 0, 0, 0.1); // Adjust the effect as needed
            }
        }

        player.getWorld().playSound(origin, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5F, 2.0F);
    });
}
    
    private Vector rotateVector(Vector vector, double angle) {
    double radians = Math.toRadians(angle);
    double cos = Math.cos(radians);
    double sin = Math.sin(radians);

    double x = vector.getX() * cos - vector.getZ() * sin;
    double z = vector.getX() * sin + vector.getZ() * cos;

    return new Vector(x, 0, z);
}

    @UpdateEvent(delay = 100)
    public void recharge() {
        final Iterator<Map.Entry<Player, EvadeData>> iterator = charges.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, EvadeData> entry = iterator.next();
            final Player player = entry.getKey();
            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            final EvadeData data = entry.getValue();
            final int maxCharges = getMaxCharges(level);

            if (data.getCharges() >= maxCharges) {
                continue; // skip if already at max charges
            }

            if (!championsManager.getCooldowns().use(player, getName(), getRechargeSeconds(level), false, true, false)) {
                continue; // skip if not enough time has passed
            }

            // add a charge
            data.addCharge();
            notifyCharges(player, data.getCharges());
        }
    }

}
