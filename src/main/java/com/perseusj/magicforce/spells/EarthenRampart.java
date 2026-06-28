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
            getCooldownForTier(tier)
        );
        this.wallDurationTicks = getDurationForTier(tier);
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
}
