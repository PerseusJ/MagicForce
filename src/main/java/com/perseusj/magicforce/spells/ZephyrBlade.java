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

public class ZephyrBlade extends Spell {
    private static final String SPELL_ID_BASE = "zephyr_blade";
    private static final int SUB_STEPS = 3;
    private static final double ENTITY_CHECK_RADIUS = 1.0;

    private final double damage;
    private final double speed;
    private final int maxTicks;

    public ZephyrBlade(int tier) {
        super(
            tier == 1 ? SPELL_ID_BASE : SPELL_ID_BASE + "_" + tier,
            "Zephyr Blade" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.WIND,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.damage = getDamageForTier(tier);
        this.speed = getSpeedForTier(tier);
        this.maxTicks = getMaxTicksForTier(tier);
    }

    private static int getChargeTimeForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "charge-time",
            tier == 2 ? 26 : tier == 3 ? 22 : 30);
    }

    private static int getManaCostForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "mana-cost",
            tier == 2 ? 35 : tier == 3 ? 50 : 25);
    }

    private static double getCooldownForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "cooldown",
            tier == 2 ? 3.0 : tier == 3 ? 2.0 : 4.0);
    }

    private static double getDamageForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "damage",
            tier == 2 ? 7.0 : tier == 3 ? 9.5 : 5.0);
    }

    private static double getSpeedForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "speed", 3.0);
    }

    private static int getMaxTicksForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "max-ticks", 20);
    }

    @Override
    public void cast(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(0.4);
        Location origin = eyeLoc.clone().add(0, -0.5, 0).add(right);

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);

        final double subStepSize = speed / SUB_STEPS;

        new BukkitRunnable() {
            int ticks = 0;
            Location current = origin.clone();

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    spawnImpact(player, current);
                    cancel();
                    return;
                }

                for (int step = 0; step < SUB_STEPS; step++) {
                    current.add(direction.clone().multiply(subStepSize));

                    spawnTrail(player, current);

                    if (current.getBlock().getType().isSolid()) {
                        spawnImpact(player, current);
                        cancel();
                        return;
                    }

                    List<Entity> nearby = (List<Entity>) current.getWorld().getNearbyEntities(current, ENTITY_CHECK_RADIUS, ENTITY_CHECK_RADIUS, ENTITY_CHECK_RADIUS);
                    for (Entity entity : nearby) {
                        if (entity.equals(player)) continue;
                        if (entity instanceof LivingEntity) {
                            ((LivingEntity) entity).damage(damage, player);
                            spawnImpact(player, current);
                            cancel();
                            return;
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 1L);
    }

    private void spawnTrail(Player player, Location loc) {
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.05, 0.05, 0.05, 0);
    }

    private void spawnImpact(Player player, Location loc) {
        for (int i = 0; i < 5; i++) {
            double angle = 2 * Math.PI * i / 5;
            double offsetX = Math.cos(angle) * 0.8;
            double offsetZ = Math.sin(angle) * 0.8;
            Location slashLoc = loc.clone().add(offsetX, 0.5, offsetZ);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, slashLoc, 1, 0, 0, 0, 0);
        }
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // SWEEP_ATTACK particles forming a sharpening blade silhouette
        int bladePoints = 3 + (int) (progress * 6); // more defined blade at full charge
        double bladeLength = 0.2 + progress * 0.35;

        for (int i = 0; i < bladePoints; i++) {
            double t = (double) i / bladePoints - 0.5;
            // Blade aligned with player view direction
            org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().normalize();
            org.bukkit.util.Vector right = new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).normalize();
            Location bladeLoc = handLoc.clone().add(
                right.getX() * t * bladeLength,
                t * bladeLength * 0.3,
                right.getZ() * t * bladeLength
            );
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bladeLoc, 1, 0, 0, 0, 0);
            // Wispy cloud edges at higher progress
            if (progress > 0.4 && i % 2 == 0) {
                player.getWorld().spawnParticle(Particle.CLOUD, bladeLoc, 1, 0.02, 0.02, 0.02, 0);
            }
        }
    }
}
