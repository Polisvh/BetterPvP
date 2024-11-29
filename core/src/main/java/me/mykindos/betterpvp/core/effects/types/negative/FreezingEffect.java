package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class FreezingEffect extends VanillaEffectType {

    @Override
    public String getName() {
        return "Freezing";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        // For freezing, we don't actually use a vanilla potion effect, but this could be used for slowness if desired
        return PotionEffectType.SLOWNESS; // You could also use a custom effect, but this is for slowness
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        // No special behavior on expire for the freezing effect, so leave this empty or handle it if necessary
        super.onExpire(livingEntity, effect, notify);
    }

    @Override
    public String getGenericDescription() {
        // Description of the freezing effect
        return "<white>" + getName() + "<reset> slows and deals 1 frost damage every 1.5 seconds.";
    }
}
