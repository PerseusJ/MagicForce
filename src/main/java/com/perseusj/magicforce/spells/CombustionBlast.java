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
    private static final double RADIUS = 4.0;

    private final double damage;
    private final int fireTicks;
    private final double knockbackMultiplier;

    public CombustionBlast(int tier) {
        super(
            tier == 1 ? "combustion_blast" : "combustion_blast_" + tier,
            "Combustion Blast" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.FIRE,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier)
        );
        this.damage = getDamageForTier(tier);
        this.fireTicks = getFireTicksForTier(tier);
        this.knockbackMultiplier = getKnockbackMultiplierForTier(tier);
    }

    private static int getManaCostForTier(int tier) {
        return switch (tier) {
            case 2 -> 45;
            case 3 -> 60;
            default -> 30;
        };
    }

    private static double getCooldownForTier(int tier) {
        return switch (tier) {
            case 2 -> 9.0;
            case 3 -> 6.0;
            default -> 12.0;
        };
    }

    private static double getDamageForTier(int tier) {
        return switch (tier) {
            case 2 -> 6.0;
            case 3 -> 7.0;
            default -> 4.0;
        };
    }

    private static int getFireTicksForTier(int tier) {
        return switch (tier) {
            case 2 -> 120; // 6s
            case 3 -> 160; // 8s
            default -> 80;  // 4s
        };
    }

    private static double getKnockbackMultiplierForTier(int tier) {
        return switch (tier) {
            case 2 -> 1.6;
            case 3 -> 2.2;
            default -> 1.0;
        };
    }

    @Override
    public void cast(Player player) {
        Location origin = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2));
        World world = player.getWorld();
        world.createExplosion(origin, 0, false, false);
        world.playSound(origin, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        List<Entity> nearby = player.getNearbyEntities(RADIUS, RADIUS, RADIUS);
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

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 5) {
                    cancel();
                    return;
                }
                double radius = 1.0 + ticks * (RADIUS / 4.0);
                int particles = (int) (radius * 12);
                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / particles;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
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
                            (Math.random() - 0.5) * radius,
                            Math.random() * 0.5,
                            (Math.random() - 0.5) * radius
                        );
                        world.spawnParticle(Particle.LAVA, sparkLoc, 1, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 2L);
    }
}
