package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class IgnitionDart extends Spell {
    private static final String SPELL_ID_BASE = "ignition_dart";
    private static final double ENTITY_CHECK_RADIUS = 1.0;
    private static final int SUB_STEPS = 3;

    private final double damage;
    private final int fireTicks;
    private final double speed;
    private final int maxTicks;

    public IgnitionDart(int tier) {
        super(
            tier == 1 ? SPELL_ID_BASE : SPELL_ID_BASE + "_" + tier,
            "Ignition Dart" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.FIRE,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.damage = getDamageForTier(tier);
        this.fireTicks = getFireTicksForTier(tier);
        this.speed = getSpeedForTier(tier);
        this.maxTicks = getMaxTicksForTier(tier);
    }

    private static int getChargeTimeForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "charge-time",
            tier == 2 ? 26 : tier == 3 ? 22 : 30);
    }

    private static int getManaCostForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "mana-cost",
            tier == 2 ? 25 : tier == 3 ? 40 : 15);
    }

    private static double getCooldownForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "cooldown",
            tier == 2 ? 2.0 : tier == 3 ? 1.0 : 3.0);
    }

    private static double getDamageForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "damage",
            tier == 2 ? 4.5 : tier == 3 ? 5.25 : 3.0);
    }

    private static int getFireTicksForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "fire-ticks",
            tier == 2 ? 120 : tier == 3 ? 140 : 80);
    }

    private static double getSpeedForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "speed", 1.5);
    }

    private static int getMaxTicksForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "max-ticks", 40);
    }

    @Override
    public void cast(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        // Spawn from player's hand (approximate right hand relative to view)
        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(0.4);
        Location origin = eyeLoc.clone().add(0, -0.5, 0).add(right);

        final double subStepSize = speed / SUB_STEPS;

        new BukkitRunnable() {
            int ticks = 0;
            Location current = origin.clone();

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                for (int step = 0; step < SUB_STEPS; step++) {
                    current.add(direction.clone().multiply(subStepSize));

                    spawnTrail(player, current, ticks * SUB_STEPS + step);

                    if (current.getBlock().getType().isSolid()) {
                        spawnImpact(player, current);
                        player.getWorld().playSound(current, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
                        cancel();
                        return;
                    }

                    List<Entity> nearby = (List<Entity>) current.getWorld().getNearbyEntities(current, ENTITY_CHECK_RADIUS, ENTITY_CHECK_RADIUS, ENTITY_CHECK_RADIUS);
                    for (Entity entity : nearby) {
                        if (entity.equals(player)) continue;
                        if (entity instanceof LivingEntity) {
                            ((LivingEntity) entity).damage(damage, player);
                            entity.setFireTicks(fireTicks);
                            spawnImpact(player, current);
                            player.getWorld().playSound(current, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
                            cancel();
                            return;
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 1L);
    }

    private void spawnTrail(Player player, Location loc, int totalSteps) {
        double angle = totalSteps * 0.5;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            double radius = 0.3;
            Location helixLoc = loc.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            player.getWorld().spawnParticle(Particle.FLAME, helixLoc, 1, 0, 0, 0, 0);
            player.getWorld().spawnParticle(Particle.SMALL_FLAME, helixLoc, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    private void spawnImpact(Player player, Location loc) {
        player.getWorld().spawnParticle(Particle.LAVA, loc, 15, 0.5, 0.5, 0.5, 0);
        player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // Flame particles spiral inward, forming a tight fireball in the hand
        int orbitCount = 2 + (int) (progress * 4); // 2 → 6 particles as charge fills
        double radius = 0.5 - progress * 0.35;      // shrinks from 0.5 to 0.15 (condensing)
        double speed = 0.3 + progress * 0.7;         // spiral speeds up with progress

        for (int i = 0; i < orbitCount; i++) {
            double angle = elapsed * speed + (2 * Math.PI * i / orbitCount);
            Location flameLoc = handLoc.clone().add(
                Math.cos(angle) * radius,
                Math.sin(elapsed * 0.15) * 0.15,
                Math.sin(angle) * radius
            );
            player.getWorld().spawnParticle(Particle.FLAME, flameLoc, 1, 0, 0, 0, 0);
            if (progress > 0.3) {
                player.getWorld().spawnParticle(Particle.SMALL_FLAME, flameLoc, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        // Dense core glow when nearly charged
        if (progress > 0.7) {
            player.getWorld().spawnParticle(Particle.FLAME, handLoc, 2, 0.05, 0.05, 0.05, 0);
        }
    }
}

