package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class GaleForce extends Spell {
    private static final String SPELL_ID_BASE = "gale_force";

    private final double maxRadius;
    private final double knockbackMultiplier;

    public GaleForce(int tier) {
        super(
            tier == 1 ? SPELL_ID_BASE : SPELL_ID_BASE + "_" + tier,
            "Gale Force" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.WIND,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.maxRadius = getRadiusForTier(tier);
        this.knockbackMultiplier = getKnockbackMultiplierForTier(tier);
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
            tier == 2 ? 4.5 : tier == 3 ? 3.5 : 6.0);
    }

    private static double getRadiusForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "radius",
            tier == 2 ? 6.5 : tier == 3 ? 8.0 : 5.0);
    }

    private static double getKnockbackMultiplierForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "knockback-multiplier",
            tier == 2 ? 2.5 : tier == 3 ? 3.2 : 2.0);
    }

    @Override
    public void cast(Player player) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.0f);

        List<Entity> nearby = player.getNearbyEntities(maxRadius, maxRadius, maxRadius);
        for (Entity entity : nearby) {
            if (entity.equals(player)) continue;
            Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector());
            if (toEntity.length() > maxRadius) continue;
            if (direction.dot(toEntity.normalize()) < 0.3) continue;
            entity.setVelocity(direction.clone().multiply(knockbackMultiplier).setY(0.5));
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = 10;

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    cancel();
                    return;
                }

                Location origin = player.getEyeLocation();
                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();

                double spread = 0.5 + ticks * 0.3;
                int particles = 6 + ticks * 2;

                for (int i = 0; i < particles; i++) {
                    double hAngle = Math.toRadians(yaw + 90) + (Math.random() - 0.5) * spread * 2;
                    double vAngle = Math.toRadians(pitch) + (Math.random() - 0.5) * spread * 0.8;

                    double dist = 1.0 + ticks * 0.8 * (maxRadius / 5.0);
                    double x = -Math.sin(hAngle) * Math.cos(vAngle) * dist;
                    double z = Math.cos(hAngle) * Math.cos(vAngle) * dist;
                    double y = -Math.sin(vAngle) * dist;

                    Location particleLoc = origin.clone().add(x, y, z);
                    player.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                    player.getWorld().spawnParticle(Particle.POOF, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                    if (ticks % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 2L);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // Swirling wind vortex — rotation accelerates dramatically toward full charge
        double spinSpeed = 0.2 + progress * 0.8; // gets much faster
        int streamCount = 3 + (int) (progress * 4); // 3 → 7 streams
        double radius = 0.4 + progress * 0.15;  // expands slightly

        for (int i = 0; i < streamCount; i++) {
            double angle = elapsed * spinSpeed + (2 * Math.PI * i / streamCount);
            double yOff = Math.sin(angle * 0.5) * 0.2;
            Location cloudLoc = handLoc.clone().add(
                Math.cos(angle) * radius,
                yOff,
                Math.sin(angle) * radius
            );
            player.getWorld().spawnParticle(Particle.CLOUD, cloudLoc, 1, 0.01, 0.01, 0.01, 0);
            if (i % 2 == 0) {
                player.getWorld().spawnParticle(Particle.POOF, cloudLoc, 1, 0.01, 0.01, 0.01, 0);
            }
        }
    }
}
