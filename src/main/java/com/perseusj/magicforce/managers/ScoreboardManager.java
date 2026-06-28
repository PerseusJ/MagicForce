package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.spells.SpellRegistry;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    private static ScoreboardManager instance;
    private final Map<UUID, Integer> activeSlots = new HashMap<>();

    public static ScoreboardManager getInstance() {
        if (instance == null) instance = new ScoreboardManager();
        return instance;
    }

    public int getActiveSlot(Player player) {
        return activeSlots.getOrDefault(player.getUniqueId(), 0);
    }

    public void setActiveSlot(Player player, int slot) {
        activeSlots.put(player.getUniqueId(), slot);
        updateScoreboard(player);
    }

    public void cycleSlot(Player player, int direction) {
        ItemStack item = player.getInventory().getItemInMainHand();
        int tier = GrimoireManager.getInstance().getGrimoireTier(item);
        if (tier == 0) return;
        int capacity = GrimoireManager.getInstance().getCapacity(tier);
        int current = getActiveSlot(player);
        int next = (current + direction + capacity) % capacity;
        setActiveSlot(player, next);
    }

    public void updateScoreboard(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        int tier = GrimoireManager.getInstance().getGrimoireTier(item);
        if (tier == 0) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("spells", "dummy",
                Utils.colorize("&6&l✦ Spells"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> scrolls = GrimoireManager.getInstance().getSocketedScrolls(item);
        int capacity = GrimoireManager.getInstance().getCapacity(tier);
        int active = getActiveSlot(player);

        for (int i = 0; i < capacity; i++) {
            String prefix = (i == active) ? "&a▸ &f" : "  &7";
            String line;
            if (i < scrolls.size()) {
                Spell spell = SpellRegistry.getById(scrolls.get(i));
                if (spell != null) {
                    line = prefix + spell.getElement().getColor() + "✦ " + spell.getName();
                } else {
                    line = prefix + "&7Empty Slot";
                }
            } else {
                line = prefix + "&7Empty Slot";
            }
            Score score = objective.getScore(Utils.colorize(line));
            score.setScore(capacity - i);
        }

        player.setScoreboard(board);
    }

    public void removePlayer(Player player) {
        activeSlots.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}
