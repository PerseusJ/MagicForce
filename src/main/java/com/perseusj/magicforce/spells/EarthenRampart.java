package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class EarthenRampart extends Spell {
    private final int wallDurationTicks;

    public EarthenRampart(int tier) {
        super(
            tier == 1 ? "earthen_rampart" : "earthen_rampart_" + tier,
            "Earthen Rampart" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.EARTH,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.wallDurationTicks = getDurationForTier(tier);
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
            case 2 -> 40;
            case 3 -> 55;
            default -> 25;
        };
    }

    private static double getCooldownForTier(int tier) {
        return switch (tier) {
            case 2 -> 12.0;
            case 3 -> 10.0;
            default -> 15.0;
        };
    }

    private static int getDurationForTier(int tier) {
        return switch (tier) {
            case 2 -> 140; // 7s
            case 3 -> 200; // 10s
            default -> 100; // 5s
        };
    }

    @Override
    public void cast(Player player) {
        Location origin = player.getLocation();
        List<Location> wallBlocks = new ArrayList<>();
        BlockData cobbleData = Material.COBBLESTONE.createBlockData();

        for (int x = -2; x <= 2; x++) { // slightly wider to make up for lack of solid blocks
            for (int y = 0; y <= 2; y++) {
                Location blockLoc = origin.clone().add(
                    origin.getDirection().normalize().multiply(2).getX() == 0 ? x : 0,
                    y,
                    origin.getDirection().normalize().multiply(2).getZ() == 0 ? x : 0
                );
                wallBlocks.add(blockLoc);
            }
        }

        player.getWorld().playSound(origin, org.bukkit.Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);

        new BukkitRunnable() {
            int standingTicks = 0;

            @Override
            public void run() {
                if (standingTicks >= wallDurationTicks) {
                    player.getWorld().playSound(origin, org.bukkit.Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.7f);
                    cancel();
                    return;
                }

                if (standingTicks % 5 == 0) {
                    for (Location loc : wallBlocks) {
                        player.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5), 3, 0.4, 0.4, 0.4, 0, cobbleData);
                    }
                }
                
                // Keep entities out of the wall area since it's no longer physical
                for (Location loc : wallBlocks) {
                    for (Entity e : player.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 1, 1, 1)) {
                        if (!e.equals(player)) {
                            Vector push = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.2);
                            push.setY(0.1);
                            e.setVelocity(e.getVelocity().add(push));
                        }
                    }
                }

                standingTicks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 1L);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // Stone/cobblestone particles condensing into a heavy mass in the hand
        org.bukkit.block.data.BlockData cobbleData = org.bukkit.Material.COBBLESTONE.createBlockData();
        int particleCount = 1 + (int) (progress * 4);
        double radius = 0.5 - progress * 0.25; // condenses toward center

        for (int i = 0; i < particleCount; i++) {
            double angle = elapsed * 0.12 + (2 * Math.PI * i / particleCount);
            // Slight downward pull — heavy feel
            double yOff = -0.05 + Math.sin(elapsed * 0.08 + i) * 0.08;
            Location stoneLoc = handLoc.clone().add(
                Math.cos(angle) * radius,
                yOff,
                Math.sin(angle) * radius
            );
            player.getWorld().spawnParticle(Particle.BLOCK, stoneLoc, 1, 0.02, 0.02, 0.02, 0, cobbleData);
        }
        // Dense core at high charge
        if (progress > 0.7) {
            player.getWorld().spawnParticle(Particle.BLOCK, handLoc, 2, 0.05, 0.05, 0.05, 0, cobbleData);
        }
    }
}
