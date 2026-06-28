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
    private static final double SPEED = 1.5;
    private static final int SUB_STEPS = 3;
    private static final double SUB_STEP_SIZE = SPEED / SUB_STEPS;
    private static final int MAX_TICKS = 40;
    private static final double ENTITY_CHECK_RADIUS = 1.0;

    private final double damage;
    private final int fireTicks;

    public IgnitionDart(int tier) {
        super(
            tier == 1 ? "ignition_dart" : "ignition_dart_" + tier,
            "Ignition Dart" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.FIRE,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier)
        );
        this.damage = getDamageForTier(tier);
        this.fireTicks = getFireTicksForTier(tier);
    }

    private static int getManaCostForTier(int tier) {
        return switch (tier) {
            case 2 -> 25;
            case 3 -> 40;
            default -> 15;
        };
    }

    private static double getCooldownForTier(int tier) {
        return switch (tier) {
            case 2 -> 2.0;
            case 3 -> 1.0;
            default -> 3.0;
        };
    }

    private static double getDamageForTier(int tier) {
        return switch (tier) {
            case 2 -> 4.5;
            case 3 -> 5.25;
            default -> 3.0;
        };
    }

    private static int getFireTicksForTier(int tier) {
        return switch (tier) {
            case 2 -> 120; // 6s
            case 3 -> 140; // 7s
            default -> 80;  // 4s
        };
    }

    @Override
    public void cast(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        
        // Spawn from player's hand (approximate right hand relative to view)
        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(0.4);
        Location origin = eyeLoc.clone().add(0, -0.5, 0).add(right);

        new BukkitRunnable() {
            int ticks = 0;
            Location current = origin.clone();

            @Override
            public void run() {
                if (ticks >= MAX_TICKS) {
                    cancel();
                    return;
                }

                for (int step = 0; step < SUB_STEPS; step++) {
                    current.add(direction.clone().multiply(SUB_STEP_SIZE));

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
}
