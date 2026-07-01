package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.MagicForce;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * v1.0.2 — Centralized configuration access.
 *
 * <p>Loads {@code plugins/MagicForce/config.yml} on enable and on {@code /mf reload}.
 * Provides typed getters ({@code getInt}, {@code getDouble}, {@code getString}, ...)
 * with safe fallbacks to compile-time defaults, plus a small set of
 * domain-specific helpers ({@code getManaMax()}, {@code getGrimoireCapacity(tier)},
 * {@code getSpellTierSection(spellId, tier)}, ...) for readability at call sites.</p>
 *
 * <p>Invalid values (negative mana, capacity &lt; 1, charge time &lt; 1 tick, ...)
 * are rejected at load time: a warning is logged and the corresponding default
 * value is used. The config file is overwritten from the bundled resource only
 * if it doesn't exist on disk yet, so server owners' edits are preserved.</p>
 */
public class ConfigManager {

    private static ConfigManager instance;

    private YamlConfiguration config;
    private final File file;

    // ── Compile-time defaults (mirror original hardcoded values) ───────────

    public static final double DEFAULT_MANA_MAX = 100.0;
    public static final double DEFAULT_MANA_GRIMOIRE_REGEN = 5.0;
    public static final double DEFAULT_MANA_NORMAL_REGEN = 2.0;
    public static final int    DEFAULT_MANA_HUD_INTERVAL = 10;

    public static final int DEFAULT_HOLD_WINDOW_TICKS = 100;
    public static final int DEFAULT_LOOP_SOUND_INTERVAL = 10;

    public static final int DEFAULT_TIER_COST_1 = 5;
    public static final int DEFAULT_TIER_COST_2 = 15;
    public static final int DEFAULT_TIER_COST_3 = 30;

    public static final int DEFAULT_GRIMOIRE_CAPACITY_1 = 2;
    public static final int DEFAULT_GRIMOIRE_CAPACITY_2 = 4;
    public static final int DEFAULT_GRIMOIRE_CAPACITY_3 = 6;

    private ConfigManager() {
        this.file = new File(MagicForce.getInstance().getDataFolder(), "config.yml");
    }

    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    /** Load (or create) config.yml from disk and validate. */
    public void load() {
        if (!file.exists()) {
            MagicForce.getInstance().getDataFolder().mkdirs();
            try (InputStream in = MagicForce.getInstance().getResource("config.yml")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                MagicForce.getInstance().getLogger().severe(
                    "[ConfigManager] Could not create config.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        // Backfill any keys added in newer plugin versions by merging against
        // the bundled default — preserves server-owner overrides for existing keys.
        try (InputStream in = MagicForce.getInstance().getResource("config.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
                config.options().copyDefaults(true);
            }
        } catch (IOException e) {
            MagicForce.getInstance().getLogger().warning(
                "[ConfigManager] Could not read bundled defaults: " + e.getMessage());
        }

        validate();
        MagicForce.getInstance().getLogger().info(
            "[ConfigManager] Loaded config.yml from " + file.getAbsolutePath());
    }

    /** Reload from disk. Triggered by {@code /mf reload}. */
    public void reload() {
        load();
    }

    // ── Validation ─────────────────────────────────────────────────────────

    private void validate() {
        // Mana
        if (getDouble("mana.max", DEFAULT_MANA_MAX) <= 0) {
            warn("mana.max", DEFAULT_MANA_MAX);
            config.set("mana.max", DEFAULT_MANA_MAX);
        }
        if (getDouble("mana.grimoire-regen", DEFAULT_MANA_GRIMOIRE_REGEN) < 0) {
            warn("mana.grimoire-regen", DEFAULT_MANA_GRIMOIRE_REGEN);
            config.set("mana.grimoire-regen", DEFAULT_MANA_GRIMOIRE_REGEN);
        }
        if (getDouble("mana.normal-regen", DEFAULT_MANA_NORMAL_REGEN) < 0) {
            warn("mana.normal-regen", DEFAULT_MANA_NORMAL_REGEN);
            config.set("mana.normal-regen", DEFAULT_MANA_NORMAL_REGEN);
        }
        if (getInt("mana.hud-interval", DEFAULT_MANA_HUD_INTERVAL) < 1) {
            warn("mana.hud-interval", DEFAULT_MANA_HUD_INTERVAL);
            config.set("mana.hud-interval", DEFAULT_MANA_HUD_INTERVAL);
        }

        // Chanting
        if (getInt("chanting.hold-window-ticks", DEFAULT_HOLD_WINDOW_TICKS) < 1) {
            warn("chanting.hold-window-ticks", DEFAULT_HOLD_WINDOW_TICKS);
            config.set("chanting.hold-window-ticks", DEFAULT_HOLD_WINDOW_TICKS);
        }
        if (getInt("chanting.loop-sound-interval", DEFAULT_LOOP_SOUND_INTERVAL) < 1) {
            warn("chanting.loop-sound-interval", DEFAULT_LOOP_SOUND_INTERVAL);
            config.set("chanting.loop-sound-interval", DEFAULT_LOOP_SOUND_INTERVAL);
        }

        // Grimoire capacities
        for (int tier = 1; tier <= 3; tier++) {
            int def = switch (tier) {
                case 1 -> DEFAULT_GRIMOIRE_CAPACITY_1;
                case 2 -> DEFAULT_GRIMOIRE_CAPACITY_2;
                case 3 -> DEFAULT_GRIMOIRE_CAPACITY_3;
                default -> 2;
            };
            if (getInt("grimoire.capacities." + tier, def) < 1) {
                warn("grimoire.capacities." + tier, def);
                config.set("grimoire.capacities." + tier, def);
            }
        }

        // Inscription tier costs
        for (int tier = 1; tier <= 3; tier++) {
            int def = switch (tier) {
                case 1 -> DEFAULT_TIER_COST_1;
                case 2 -> DEFAULT_TIER_COST_2;
                case 3 -> DEFAULT_TIER_COST_3;
                default -> 1;
            };
            if (getInt("inscription.tier-costs." + tier, def) < 0) {
                warn("inscription.tier-costs." + tier, def);
                config.set("inscription.tier-costs." + tier, def);
            }
        }
    }

    private void warn(String key, Object defaultValue) {
        MagicForce.getInstance().getLogger().warning(
            "[ConfigManager] Invalid value for '" + key + "', falling back to default: " + defaultValue);
    }

    // ── Typed getters ───────────────────────────────────────────────────────

    public int getInt(String path, int defaultValue) {
        if (config == null) return defaultValue;
        return config.getInt(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        if (config == null) return defaultValue;
        return config.getDouble(path, defaultValue);
    }

    public String getString(String path, String defaultValue) {
        if (config == null) return defaultValue;
        String v = config.getString(path);
        return v == null ? defaultValue : v;
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        if (config == null) return defaultValue;
        return config.getBoolean(path, defaultValue);
    }

    public ConfigurationSection getSection(String path) {
        if (config == null) return null;
        return config.getConfigurationSection(path);
    }

    // ── Domain helpers ─────────────────────────────────────────────────────

    // Mana
    public double getManaMax() { return getDouble("mana.max", DEFAULT_MANA_MAX); }
    public double getGrimoireRegen() { return getDouble("mana.grimoire-regen", DEFAULT_MANA_GRIMOIRE_REGEN); }
    public double getNormalRegen() { return getDouble("mana.normal-regen", DEFAULT_MANA_NORMAL_REGEN); }
    public int getManaHudInterval() { return getInt("mana.hud-interval", DEFAULT_MANA_HUD_INTERVAL); }

    // Chanting
    public int getHoldWindowTicks() { return getInt("chanting.hold-window-ticks", DEFAULT_HOLD_WINDOW_TICKS); }
    public int getLoopSoundInterval() { return getInt("chanting.loop-sound-interval", DEFAULT_LOOP_SOUND_INTERVAL); }

    // Inscription
    public int getTierCost(int tier) {
        int def = switch (tier) {
            case 1 -> DEFAULT_TIER_COST_1;
            case 2 -> DEFAULT_TIER_COST_2;
            case 3 -> DEFAULT_TIER_COST_3;
            default -> 1;
        };
        return getInt("inscription.tier-costs." + tier, def);
    }

    // Grimoire
    public int getGrimoireCapacity(int tier) {
        int def = switch (tier) {
            case 1 -> DEFAULT_GRIMOIRE_CAPACITY_1;
            case 2 -> DEFAULT_GRIMOIRE_CAPACITY_2;
            case 3 -> DEFAULT_GRIMOIRE_CAPACITY_3;
            default -> DEFAULT_GRIMOIRE_CAPACITY_1;
        };
        return getInt("grimoire.capacities." + tier, def);
    }

    // Spells
    /**
     * Return the configuration section for {@code spells.<id>.tier_<N>}, or
     * {@code null} if either the spell or the tier is undefined.
     */
    public ConfigurationSection getSpellTierSection(String spellId, int tier) {
        ConfigurationSection spellSec = getSection("spells." + spellId);
        if (spellSec == null) return null;
        return spellSec.getConfigurationSection("tier_" + tier);
    }

    public int getSpellInt(String spellId, int tier, String key, int defaultValue) {
        ConfigurationSection sec = getSpellTierSection(spellId, tier);
        if (sec == null) return defaultValue;
        return sec.getInt(key, defaultValue);
    }

    public double getSpellDouble(String spellId, int tier, String key, double defaultValue) {
        ConfigurationSection sec = getSpellTierSection(spellId, tier);
        if (sec == null) return defaultValue;
        return sec.getDouble(key, defaultValue);
    }
}
