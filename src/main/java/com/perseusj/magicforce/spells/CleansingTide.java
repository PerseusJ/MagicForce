package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CleansingTide extends Spell {
    private static final String SPELL_ID_BASE = "cleansing_tide";

    private final double healingAmount;
    private final int regenDurationTicks;
    private final int regenAmplifier;

    public CleansingTide(int tier) {
        super(
            tier == 1 ? SPELL_ID_BASE : SPELL_ID_BASE + "_" + tier,
            "Cleansing Tide" + (tier == 1 ? "" : " " + Utils.toRoman(tier)),
            SpellElement.WATER,
            tier,
            getManaCostForTier(tier),
            getCooldownForTier(tier),
            getChargeTimeForTier(tier)
        );
        this.healingAmount = getHealingAmountForTier(tier);
        this.regenDurationTicks = getRegenDurationForTier(tier);
        this.regenAmplifier = getRegenAmplifierForTier(tier);
    }

    private static int getChargeTimeForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "charge-time",
            tier == 2 ? 42 : tier == 3 ? 34 : 50);
    }

    private static int getManaCostForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "mana-cost",
            tier == 2 ? 60 : tier == 3 ? 90 : 40);
    }

    private static double getCooldownForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "cooldown",
            tier == 2 ? 15.0 : tier == 3 ? 10.0 : 20.0);
    }

    private static double getHealingAmountForTier(int tier) {
        return configDouble(SPELL_ID_BASE, tier, "healing-amount",
            tier == 2 ? 12.0 : tier == 3 ? 20.0 : 8.0);
    }

    private static int getRegenDurationForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "regen-duration-ticks",
            tier == 2 ? 80 : tier == 3 ? 120 : 40);
    }

    private static int getRegenAmplifierForTier(int tier) {
        return configInt(SPELL_ID_BASE, tier, "regen-amplifier",
            tier == 2 ? 1 : tier == 3 ? 2 : 0);
    }

    @Override
    public void cast(Player player) {
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(health + healingAmount, maxHealth));
        player.setFireTicks(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenDurationTicks, regenAmplifier));
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.0f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = 20;

            @Override
            public void run() {
                if (ticks >= totalTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location center = player.getLocation().add(0, 1, 0);
                for (int i = 0; i < 4; i++) {
                    double angle = (2 * Math.PI * i / 4) + ticks * 0.4;
                    double radius = 0.8;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double yOffset = (double) ticks / totalTicks * 2.0;
                    Location vortexLoc = center.clone().add(x, yOffset, z);
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, vortexLoc, 1, 0.1, 0.1, 0.1, 0);
                    player.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, vortexLoc, 1, 0.05, 0.05, 0.05, 0);
                }

                if (ticks % 2 == 0) {
                    player.getWorld().spawnParticle(Particle.HEART, center.clone().add(0, 1, 0), 2, 0.5, 0.3, 0.5, 0);
                }

                double waveRadius = (double) ticks / totalTicks * 3.0;
                int waveParticles = (int) (waveRadius * 6);
                for (int i = 0; i < waveParticles; i++) {
                    double angle = 2 * Math.PI * i / waveParticles;
                    double x = Math.cos(angle) * waveRadius;
                    double z = Math.sin(angle) * waveRadius;
                    Location waveLoc = player.getLocation().clone().add(x, 0.1, z);
                    player.getWorld().spawnParticle(Particle.SPLASH, waveLoc, 1, 0.1, 0.05, 0.1, 0);
                }

                ticks++;
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, 1L);
    }

    @Override
    public void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed) {
        // Gentle healing particles rising from the hand — serene and warm
        int sparkCount = 1 + (int) (progress * 3);
        double driftRadius = 0.3 + progress * 0.2;

        for (int i = 0; i < sparkCount; i++) {
            double angle = elapsed * 0.2 + (2 * Math.PI * i / sparkCount);
            Location sparkLoc = handLoc.clone().add(
                Math.cos(angle) * driftRadius,
                0.1 + progress * 0.2,
                Math.sin(angle) * driftRadius
            );
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, sparkLoc, 1, 0.05, 0.1, 0.05, 0);
            if (progress > 0.4) {
                player.getWorld().spawnParticle(Particle.SPLASH, sparkLoc.clone().add(0, -0.1, 0), 1, 0.05, 0, 0.05, 0);
            }
        }
        // Heart particles appear near full charge
        if (progress > 0.8 && elapsed % 5 == 0) {
            player.getWorld().spawnParticle(Particle.HEART, handLoc.clone().add(0, 0.2, 0), 1, 0.1, 0.1, 0.1, 0);
        }
    }
}
