package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class CombustionBlast extends Spell {
    private static final String SPELL_ID_BASE = "combustion_blast";

    private final double damage;
    private final int fireTicks;
    private final double knockbackMultiplier;
    private final double radius;

    public CombustionBlast(int tier) {
        super(
            tier == 1 ? SPELL_ID_BASE : SPELL_ID_BASE + "_" + tier,
            "Combustion Blast" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.FIRE,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.damage = getDamageForTier(tier);
        this.fireTicks = getFireTicksForTier(tier);
        this.knockbackMultiplier = getKnockbackMultiplierForTier(tier);
        this.radius = getRadiusForTier(tier);
    }

    private static int getChargeTimeForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "charge-time",
            tier == 2 ? 50 : tier == 3 ? 40 : 60);
    }

    private static int getManaCostForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "mana-cost",
            tier == 2 ? 45 : tier == 3 ? 60 : 30);
    }

    private static double getCooldownForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "cooldown",
            tier == 2 ? 9.0 : tier == 3 ? 6.0 : 12.0);
    }

    private static double getDamageForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "damage",
            tier == 2 ? 6.0 : tier == 3 ? 7.0 : 4.0);
    }

    private static int getFireTicksForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "fire-ticks",
            tier == 2 ? 120 : tier == 3 ? 160 : 80);
    }

    private static double getKnockbackMultiplierForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "knockback-multiplier",
            tier == 2 ? 1.6 : tier == 3 ? 2.2 : 1.0);
    }

    private static double getRadiusForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "radius", 4.0);
    }

    @Override
    public void cast(Player player) {
        Location origin = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2));
        World world = player.getWorld();
        world.createExplosion(origin, 0, false, false);
        world.playSound(origin, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
        for (Entity entity : nearby) {
            if (entity.equals(player)) continue;
            Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector());
            if (toEntity.lengthSquared() == 0) toEntity = new Vector(1, 0, 0);

            Vector knockback = toEntity.normalize().multiply(1.5 * knockbackMultiplier);
            knockback.setY(0.5);
            entity.setVelocity(knockback);
            entity.setFireTicks(fireTicks);

            if (entity instanceof LivingEntity target) {
                target.damage(damage, player);
            }
        }

        final double radiusFinal = radius;
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 5) {
                    cancel();
                    return;
                }
                double r = 1.0 + ticks * (radiusFinal / 4.0);
                int particles = (int) (r * 12);
                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / particles;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location ringLoc = origin.clone().add(x, 0, z);
                    world.spawnParticle(Particle.FLAME, ringLoc, 1, 0.1, 0.1, 0.1, 0);
                    if (ticks < 3) {
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, ringLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                    if (ticks > 1) {
                        world.spawnParticle(Particle.SMOKE, ringLoc.clone().add(0, 0.5, 0), 1, 0.2, 0.1, 0.2, 0);
                    }
                }
                if (ticks < 3) {
                    for (int i = 0; i < 3; i++) {
                        Location sparkLoc = origin.clone().add(
                            (Math.random() - 0.5) * r,
                            Math.random() * 0.5,
                            (Math.random() - 0.5) * r
                        );
                        world.spawnParticle(Particle.LAVA, sparkLoc, 1, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 2L);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // Chaotic dual-flame with smoke — volatile, explosive energy building up
        double jitter = 0.1 + progress * 0.3;
        int flameCount = 1 + (int) (progress * 5);

        for (int i = 0; i < flameCount; i++) {
            Location jitterLoc = handLoc.clone().add(
                (Math.random() - 0.5) * jitter,
                (Math.random() - 0.5) * jitter,
                (Math.random() - 0.5) * jitter
            );
            player.getWorld().spawnParticle(Particle.FLAME, jitterLoc, 1, 0, 0, 0, 0);
            if (i % 2 == 0) {
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, jitterLoc, 1, 0.03, 0.03, 0.03, 0);
            }
        }
        // Rising smoke
        if (progress > 0.2) {
            player.getWorld().spawnParticle(Particle.SMOKE, handLoc.clone().add(0, 0.2, 0), 1, 0.1, 0.1, 0.1, 0.02);
        }
        // Lava sparks at high charge
        if (progress > 0.6 && elapsed % 4 == 0) {
            player.getWorld().spawnParticle(Particle.LAVA, handLoc, 1, 0.1, 0.1, 0.1, 0);
        }
    }
}
