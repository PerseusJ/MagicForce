package com.perseusj.magicforce.spells;

import com.perseusj.magicforce.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public abstract class Spell {
    private final String id;
    private final String name;
    private final SpellElement element;
    private final int tier;
    private final int manaCost;
    private final double cooldown;
    private final int chargeTimeTicks;

    public Spell(String id, String name, SpellElement element, int tier, int manaCost, double cooldown, int chargeTimeTicks) {
        this.id = id;
        this.name = name;
        this.element = element;
        this.tier = tier;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.chargeTimeTicks = chargeTimeTicks;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public SpellElement getElement() { return element; }
    public int getTier() { return tier; }
    public int getManaCost() { return manaCost; }
    public double getCooldown() { return cooldown; }
    public int getChargeTimeTicks() { return chargeTimeTicks; }

    // ── v1.0.2 config-aware helpers ─────────────────────────────────────────
    // Subclasses use these inside their static getXForTier(tier) methods to
    // resolve values from config.yml with the original hardcoded fallback.

    protected static int configInt(String spellId, int tier, String key, int defaultValue) {
        ConfigManager cfg = ConfigManager.getInstance();
        return cfg.getSpellInt(spellId, tier, key, defaultValue);
    }

    protected static double configDouble(String spellId, int tier, String key, double defaultValue) {
        ConfigManager cfg = ConfigManager.getInstance();
        return cfg.getSpellDouble(spellId, tier, key, defaultValue);
    }

    /** Cast the spell. Called after chanting completes and the player left-clicks. */
    public abstract void cast(Player player);

    /**
     * Spawn chanting particles at the player's hand position.
     * @param player   The caster
     * @param handLoc  Location of the player's right hand
     * @param progress Charge progress from 0.0 (just started) to 1.0 (fully charged)
     * @param elapsed  Elapsed ticks since chanting started (for animation phase)
     */
    public abstract void spawnChantingParticles(Player player, Location handLoc, double progress, int elapsed);

    // ── Default element-based sounds ─────────────────────────────────────────

    public Sound getChantingStartSound() {
        return switch (element) {
            case FIRE  -> Sound.ITEM_FLINTANDSTEEL_USE;
            case WATER -> Sound.BLOCK_WATER_AMBIENT;
            case EARTH -> Sound.BLOCK_STONE_PLACE;
            case WIND  -> Sound.ENTITY_BREEZE_CHARGE;
        };
    }

    public Sound getChantingLoopSound() {
        return switch (element) {
            case FIRE  -> Sound.BLOCK_FIRE_AMBIENT;
            case WATER -> Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT;
            case EARTH -> Sound.BLOCK_GRAVEL_STEP;
            case WIND  -> Sound.ENTITY_BREEZE_IDLE_AIR;
        };
    }

    public Sound getChantingReadySound() {
        return switch (element) {
            case FIRE  -> Sound.ENTITY_BLAZE_SHOOT;
            case WATER -> Sound.ENTITY_DOLPHIN_PLAY;
            case EARTH -> Sound.BLOCK_ANVIL_LAND;
            case WIND  -> Sound.ENTITY_BREEZE_SHOOT;
        };
    }

    public Sound getChantingCancelSound() {
        return switch (element) {
            case FIRE  -> Sound.BLOCK_FIRE_EXTINGUISH;
            case WATER -> Sound.BLOCK_WATER_AMBIENT;
            case EARTH -> Sound.BLOCK_GRAVEL_BREAK;
            case WIND  -> Sound.ENTITY_BREEZE_DEATH;
        };
    }
}
