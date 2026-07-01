package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ManaManager {
    private static ManaManager instance;
    private final Map<UUID, Double> manaMap = new ConcurrentHashMap<>();

    public static ManaManager getInstance() {
        if (instance == null) instance = new ManaManager();
        return instance;
    }

    public void initialize() {
        ConfigManager cfg = ConfigManager.getInstance();

        new BukkitRunnable() {
            @Override
            public void run() {
                double grimoireRegen = cfg.getGrimoireRegen();
                double normalRegen = cfg.getNormalRegen();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    double regen = isHoldingGrimoire(player) ? grimoireRegen : normalRegen;
                    if (regen > 0) addMana(player, regen);
                }
            }
        }.runTaskTimer(MagicForce.getInstance(), 20L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendManaHUD(player);
                }
            }
        }.runTaskTimer(MagicForce.getInstance(), 0L, Math.max(1L, (long) cfg.getManaHudInterval()));
    }

    public double getMana(Player player) {
        return manaMap.getOrDefault(player.getUniqueId(), ConfigManager.getInstance().getManaMax());
    }

    public void setMana(Player player, double amount) {
        double max = ConfigManager.getInstance().getManaMax();
        manaMap.put(player.getUniqueId(), Math.max(0, Math.min(max, amount)));
    }

    public void addMana(Player player, double amount) {
        setMana(player, getMana(player) + amount);
    }

    public boolean removeMana(Player player, double amount) {
        double current = getMana(player);
        if (current < amount) return false;
        setMana(player, current - amount);
        return true;
    }

    public boolean isHoldingGrimoire(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item != null && item.getType() == Material.BOOK && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                        .has(GrimoireManager.GRIMOIRE_KEY, org.bukkit.persistence.PersistentDataType.INTEGER);
    }

    private void sendManaHUD(Player player) {
        double mana = getMana(player);
        double max = ConfigManager.getInstance().getManaMax();
        int slots = 10;
        int fullBlocks = (int) (mana / max * slots);
        if (fullBlocks < 0) fullBlocks = 0;
        if (fullBlocks > slots) fullBlocks = slots;
        int emptyBlocks = slots - fullBlocks;

        StringBuilder bar = new StringBuilder();
        bar.append("&3Mana: &b");
        bar.append("▰".repeat(Math.max(0, fullBlocks)));
        bar.append("&7▱".repeat(Math.max(0, emptyBlocks)));
        bar.append(" &3[").append((int) mana).append("/").append((int) max).append("]");

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(Utils.colorize(bar.toString())));
    }

    /** Remove the player's mana from the in-memory map (call after saving on quit). */
    public void resetMana(UUID uuid) {
        manaMap.remove(uuid);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private File getManaFile() {
        return new File(MagicForce.getInstance().getDataFolder(), "mana.yml");
    }

    /**
     * Load saved mana for a player from mana.yml.
     * Returns current mana.max if no saved value exists (e.g. first join).
     */
    public double loadMana(UUID uuid) {
        File file = getManaFile();
        if (!file.exists()) return ConfigManager.getInstance().getManaMax();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getDouble(uuid.toString(), ConfigManager.getInstance().getManaMax());
    }

    /**
     * Persist a single player's current mana to mana.yml.
     * Called on player quit so the value is not lost between sessions.
     */
    public void saveMana(UUID uuid, double amount) {
        File file = getManaFile();
        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        config.set(uuid.toString(), amount);
        try {
            config.save(file);
        } catch (IOException e) {
            MagicForce.getInstance().getLogger().severe(
                "[ManaManager] Failed to save mana.yml: " + e.getMessage());
        }
    }

    /**
     * Persist all currently online players' mana at once.
     * Called on plugin disable to cover crash/reload scenarios.
     */
    public void saveAll() {
        File file = getManaFile();
        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : manaMap.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            MagicForce.getInstance().getLogger().severe(
                "[ManaManager] Failed to save mana.yml on shutdown: " + e.getMessage());
        }
    }
}
