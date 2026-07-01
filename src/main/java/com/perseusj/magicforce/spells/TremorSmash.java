package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class TremorSmash extends Spell {
    private static final String SPELL_ID_BASE = "tremor_smash";

    private final double maxRadius;
    private final double damage;

    public TremorSmash(int tier) {
        super(
            tier == 1 ? SPELL_ID_BASE : SPELL_ID_BASE + "_" + tier,
            "Tremor Smash" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.EARTH,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.maxRadius = getRadiusForTier(tier);
        this.damage = getDamageForTier(tier);
    }

    private static int getChargeTimeForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "charge-time",
            tier == 2 ? 42 : tier == 3 ? 34 : 50);
    }

    private static int getManaCostForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "mana-cost",
            tier == 2 ? 50 : tier == 3 ? 70 : 35);
    }

    private static double getCooldownForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "cooldown",
            tier == 2 ? 8.0 : tier == 3 ? 6.0 : 10.0);
    }

    private static double getRadiusForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "radius",
            tier == 2 ? 6.5 : tier == 3 ? 8.0 : 5.0);
    }

    private static double getDamageForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "damage",
            tier == 2 ? 7.5 : tier == 3 ? 10.0 : 5.0);
    }

    @Override
    public void cast(Player player) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        world.playSound(origin, org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);

        List<Entity> nearby = player.getNearbyEntities(maxRadius, maxRadius, maxRadius);
        for (Entity entity : nearby) {
            if (entity.equals(player)) continue;
            Vector knockback = entity.getLocation().toVector().subtract(origin.toVector()).normalize();
            knockback.setY(0.4);
            entity.setVelocity(knockback);
            if (entity instanceof LivingEntity living) {
                living.damage(damage, player);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10) {
                    cancel();
                    return;
                }

                double radius = (double) ticks / 10 * maxRadius;
                int particles = (int) (radius * 10);

                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / particles;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location groundLoc = origin.clone().add(x, 0, z);
                    Block ground = groundLoc.getBlock();
                    int blockY = ground.getY();
                    while (ground.getType().isAir() && blockY > origin.getBlockY() - 3) {
                        blockY--;
                        ground = groundLoc.getBlock();
                    }
                    Location particleLoc = groundLoc.clone().add(0, 1, 0);

                    if (!ground.getType().isAir()) {
                        world.spawnParticle(Particle.BLOCK, particleLoc, 2, 0.3, 0.1, 0.3, 0,
                            ground.getType().createBlockData());
                    }
                    world.spawnParticle(Particle.GUST, particleLoc, 1, 0.2, 0.1, 0.2, 0);
                }

                ticks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 1L);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // Earth/dirt particles vibrating and rumbling — seismic energy
        org.bukkit.block.data.BlockData dirtData = org.bukkit.Material.DIRT.createBlockData();
        double vibration = 0.04 + progress * 0.12; // hand "shakes" more as charge fills
        int clumpCount = 2 + (int) (progress * 5);

        for (int i = 0; i < clumpCount; i++) {
            Location earthLoc = handLoc.clone().add(
                (Math.random() - 0.5) * vibration * 4,
                (Math.random() - 0.5) * vibration * 2,
                (Math.random() - 0.5) * vibration * 4
            );
            player.getWorld().spawnParticle(Particle.BLOCK, earthLoc, 1, 0.01, 0.01, 0.01, 0, dirtData);
        }
        // Gust particles at edges showing outward seismic pressure
        if (progress > 0.4 && elapsed % 3 == 0) {
            player.getWorld().spawnParticle(Particle.GUST, handLoc.clone().add(
                (Math.random() - 0.5) * 0.3, 0, (Math.random() - 0.5) * 0.3), 1, 0, 0, 0, 0);
        }
    }
}
