package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class AquaTorrent extends Spell {
    private final double knockbackMultiplier;
    private final int slownessDurationTicks;
    private final int slownessAmplifier;

    public AquaTorrent(int tier) {
        super(
            tier == 1 ? "aqua_torrent" : "aqua_torrent_" + tier,
            "Aqua Torrent" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.WATER,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.knockbackMultiplier = getKnockbackMultiplierForTier(tier);
        this.slownessDurationTicks = getSlownessDurationForTier(tier);
        this.slownessAmplifier = getSlownessAmplifierForTier(tier);
    }

    private static int getChargeTimeForTier(int tier) {
        return switch (tier) {
            case 2 -> 34;
            case 3 -> 28;
            default -> 40; // 2s
        };
    }

    private static int getManaCostForTier(int tier) {
        return switch (tier) {
            case 2 -> 35;
            case 3 -> 50;
            default -> 20;
        };
    }

    private static double getCooldownForTier(int tier) {
        return switch (tier) {
            case 2 -> 4.0;
            case 3 -> 3.0;
            default -> 5.0;
        };
    }

    private static double getKnockbackMultiplierForTier(int tier) {
        return switch (tier) {
            case 2 -> 1.5;
            case 3 -> 2.0;
            default -> 1.0;
        };
    }

    private static int getSlownessDurationForTier(int tier) {
        return switch (tier) {
            case 2 -> 100; // 5s
            case 3 -> 140; // 7s
            default -> 60; // 3s
        };
    }

    private static int getSlownessAmplifierForTier(int tier) {
        return switch (tier) {
            case 2 -> 1;
            case 3 -> 2;
            default -> 1;
        };
    }

    @Override
    public void cast(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 40;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }
                Location origin = player.getEyeLocation();
                Vector direction = origin.getDirection().normalize();
                for (int i = 0; i < 8; i++) {
                    double coneRadius = 0.5 + i * 0.15;
                    Vector forward = direction.clone().multiply(i);
                    for (int j = 0; j < 4; j++) {
                        double angle = 2 * Math.PI * j / 4 + ticks * 0.1;
                        double x = Math.cos(angle) * coneRadius;
                        double z = Math.sin(angle) * coneRadius;
                        Location particleLoc = origin.clone().add(forward).add(x, -0.2 + Math.random() * 0.4, z);
                        player.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                        if (j % 2 == 0) {
                            player.getWorld().spawnParticle(Particle.BUBBLE, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                        }
                    }
                    Location wakeLoc = origin.clone().add(forward).add(0, -0.5, 0);
                    player.getWorld().spawnParticle(Particle.FALLING_WATER, wakeLoc, 2, 0.3, 0.1, 0.3, 0);
                }
                List<Entity> nearby = player.getNearbyEntities(8, 2, 8);
                for (Entity entity : nearby) {
                    Location eLoc = entity.getLocation();
                    Vector toEntity = eLoc.toVector().subtract(origin.toVector());
                    if (toEntity.length() > 8 || toEntity.length() < 0.5) continue;
                    if (origin.getDirection().normalize().dot(toEntity.normalize()) < 0.7) continue;
                    entity.setVelocity(direction.clone().multiply(0.5 * knockbackMultiplier));
                    if (entity instanceof Player target) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDurationTicks, slownessAmplifier));
                    }
                }
                player.getWorld().playSound(origin, org.bukkit.Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.0f);
                ticks += 2;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 2L);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // Water droplets spiral in a vortex, spinning faster as charge builds
        double spinSpeed = 0.15 + progress * 0.45;
        int streams = 2 + (int) (progress * 3); // 2 → 5 water streams
        double radius = 0.45 - progress * 0.15; // slightly tightens

        for (int i = 0; i < streams; i++) {
            double angle = elapsed * spinSpeed + (2 * Math.PI * i / streams);
            Location dropLoc = handLoc.clone().add(
                Math.cos(angle) * radius,
                Math.sin(elapsed * 0.1) * 0.12,
                Math.sin(angle) * radius
            );
            player.getWorld().spawnParticle(Particle.SPLASH, dropLoc, 1, 0.02, 0.02, 0.02, 0);
            if (i % 2 == 0 && progress > 0.3) {
                player.getWorld().spawnParticle(Particle.BUBBLE, dropLoc, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        // Dripping water at high charge
        if (progress > 0.6) {
            player.getWorld().spawnParticle(Particle.DRIPPING_WATER, handLoc.clone().add(0, 0.1, 0), 1, 0.1, 0, 0.1, 0);
        }
    }
}
