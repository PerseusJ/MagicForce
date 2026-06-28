package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaManager {
    private static ManaManager instance;
    private final Map<UUID, Double> manaMap = new HashMap<>();
    private static final double MAX_MANA = 100.0;
    private static final double GRIMOIRE_REGEN = 5.0;
    private static final double NORMAL_REGEN = 2.0;

    public static ManaManager getInstance() {
        if (instance == null) instance = new ManaManager();
        return instance;
    }

    public void initialize() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    double regen = isHoldingGrimoire(player) ? GRIMOIRE_REGEN : NORMAL_REGEN;
                    addMana(player, regen);
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
        }.runTaskTimer(MagicForce.getInstance(), 0L, 10L);
    }

    public double getMana(Player player) {
        return manaMap.getOrDefault(player.getUniqueId(), MAX_MANA);
    }

    public void setMana(Player player, double amount) {
        manaMap.put(player.getUniqueId(), Math.max(0, Math.min(MAX_MANA, amount)));
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
        int fullBlocks = (int) (mana / 10);
        int emptyBlocks = 10 - fullBlocks;

        StringBuilder bar = new StringBuilder();
        bar.append("&3Mana: &b");
        bar.append("▰".repeat(Math.max(0, fullBlocks)));
        bar.append("&7▱".repeat(Math.max(0, emptyBlocks)));
        bar.append(" &3[").append((int) mana).append("/100]");

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(Utils.colorize(bar.toString())));
    }

    public void resetMana(UUID uuid) {
        manaMap.remove(uuid);
    }
}
