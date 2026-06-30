package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player spell cooldowns with YAML persistence.
 * Cooldowns survive plugin reloads and server restarts.
 * Data is stored in plugins/MagicForce/cooldowns.yml, keyed as UUID:spellId,
 * with the value being the expiry timestamp (System.currentTimeMillis()).
 */
public class CooldownManager {

    private static CooldownManager instance;

    // UUID -> (spellId -> expiry timestamp ms)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public static CooldownManager getInstance() {
        if (instance == null) instance = new CooldownManager();
        return instance;
    }

    private File getFile() {
        return new File(MagicForce.getInstance().getDataFolder(), "cooldowns.yml");
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Load cooldowns from cooldowns.yml on plugin enable.
     * Entries whose expiry is already in the past are silently discarded.
     */
    public void load() {
        File file = getFile();
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        long now = System.currentTimeMillis();

        for (String key : config.getKeys(false)) {
            // key format: "<UUID>:<spellId>"
            int sep = key.indexOf(':');
            if (sep < 0) continue;
            try {
                UUID uuid = UUID.fromString(key.substring(0, sep));
                String spellId = key.substring(sep + 1);
                long expiry = config.getLong(key, 0L);
                if (expiry > now) {
                    cooldowns
                        .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                        .put(spellId, expiry);
                }
            } catch (IllegalArgumentException ignored) {
                // malformed key — skip
            }
        }

        MagicForce.getInstance().getLogger().info(
            "[CooldownManager] Loaded cooldowns for " + cooldowns.size() + " player(s).");
    }

    /**
     * Save all active (non-expired) cooldowns to cooldowns.yml on plugin disable.
     */
    public void save() {
        File file = getFile();
        YamlConfiguration config = new YamlConfiguration();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldowns.entrySet()) {
            UUID uuid = playerEntry.getKey();
            for (Map.Entry<String, Long> spellEntry : playerEntry.getValue().entrySet()) {
                if (spellEntry.getValue() > now) {
                    config.set(uuid + ":" + spellEntry.getKey(), spellEntry.getValue());
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            MagicForce.getInstance().getLogger().severe(
                "[CooldownManager] Failed to save cooldowns.yml: " + e.getMessage());
        }
    }

    // ── Runtime API ───────────────────────────────────────────────────────────

    /**
     * Check whether a player is on cooldown for a spell.
     * Sends a chat message if they are.
     *
     * @return true if the player may cast (not on cooldown), false otherwise
     */
    public boolean checkCooldown(Player player, Spell spell) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return true;
        Long expiry = playerCooldowns.get(spell.getId());
        if (expiry == null) return true;
        long now = System.currentTimeMillis();
        if (now < expiry) {
            long remaining = (expiry - now + 999) / 1000; // ceiling seconds
            player.sendMessage(Utils.colorize("&cCooldown: " + remaining + "s remaining."));
            return false;
        }
        // Expired entry — clean up lazily
        playerCooldowns.remove(spell.getId());
        return true;
    }

    /**
     * Record a cooldown for the player/spell pair after a successful cast.
     */
    public void setCooldown(Player player, Spell spell) {
        if (spell.getCooldown() <= 0) return;
        long expiry = System.currentTimeMillis() + (long) (spell.getCooldown() * 1000);
        cooldowns
            .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
            .put(spell.getId(), expiry);
    }

    /**
     * Remove all cooldown entries for a player.
     * Call on player quit if you want to avoid accumulating stale data.
     */
    public void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
