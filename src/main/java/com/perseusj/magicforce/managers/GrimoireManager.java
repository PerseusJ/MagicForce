package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.spells.SpellRegistry;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import com.perseusj.magicforce.MagicForce;

import java.util.ArrayList;
import java.util.List;

public class GrimoireManager {
    private static GrimoireManager instance;

    public static final NamespacedKey GRIMOIRE_KEY = new NamespacedKey(MagicForce.getInstance(), "grimoire");
    public static final NamespacedKey GRIMOIRE_TIER_KEY = new NamespacedKey(MagicForce.getInstance(), "grimoire_tier");
    public static final NamespacedKey GRIMOIRE_SCROLLS_KEY = new NamespacedKey(MagicForce.getInstance(), "grimoire_scrolls");
    public static final NamespacedKey SCROLL_SPELL_KEY = new NamespacedKey(MagicForce.getInstance(), "scroll_spell");
    public static final NamespacedKey SCROLL_TIER_KEY = new NamespacedKey(MagicForce.getInstance(), "scroll_tier");
    public static final NamespacedKey SCROLL_INSCRIPTION_TABLE_KEY = new NamespacedKey(MagicForce.getInstance(), "scroll_inscription_table");

    public static GrimoireManager getInstance() {
        if (instance == null) instance = new GrimoireManager();
        return instance;
    }

    public ItemStack createGrimoire(int tier) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.colorize("&6Grimoire (Tier " + tier + ")"));
        meta.setUnbreakable(true);

        List<String> lore = new ArrayList<>();
        lore.add(Utils.colorize("&7A mystical tome for channeling spells"));
        lore.add(Utils.colorize("&7Tier: &e" + tier));
        int capacity = ConfigManager.getInstance().getGrimoireCapacity(tier);
        lore.add(Utils.colorize("&7Capacity: &e" + capacity + " slots"));
        lore.add("");
        lore.add(Utils.colorize("&7Sneak + Left-Click to socket scrolls"));
        lore.add(Utils.colorize("&7Sneak + Scroll to change active spell"));
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(GRIMOIRE_KEY, PersistentDataType.INTEGER, 1);
        pdc.set(GRIMOIRE_TIER_KEY, PersistentDataType.INTEGER, tier);
        pdc.set(GRIMOIRE_SCROLLS_KEY, PersistentDataType.STRING, "");

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createScroll(Spell spell) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.colorize(spell.getElement().getColor() + "✦ " + spell.getName()));
        List<String> lore = new ArrayList<>();
        lore.add(Utils.colorize("&7Element: " + spell.getElement().getColor() + spell.getElement().getDisplayName()));
        lore.add(Utils.colorize("&7Tier: &e" + spell.getTier()));
        lore.add(Utils.colorize("&7Mana: &b" + spell.getManaCost()));
        lore.add(Utils.colorize("&7Cooldown: &f" + spell.getCooldown() + "s"));
        lore.add("");
        lore.add(Utils.colorize("&7Right-click to cast directly"));
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(SCROLL_SPELL_KEY, PersistentDataType.STRING, spell.getId());
        pdc.set(SCROLL_TIER_KEY, PersistentDataType.INTEGER, spell.getTier());

        item.setItemMeta(meta);
        return item;
    }

    public int getCapacity(int tier) {
        return ConfigManager.getInstance().getGrimoireCapacity(tier);
    }

    public int getGrimoireTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(GRIMOIRE_TIER_KEY, PersistentDataType.INTEGER, 0);
    }

    public List<String> getSocketedScrolls(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new ArrayList<>();
        String data = item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(GRIMOIRE_SCROLLS_KEY, PersistentDataType.STRING, "");
        if (data.isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String s : data.split(",")) {
            if (!s.isEmpty()) result.add(s);
        }
        return result;
    }

    public void setSocketedScrolls(ItemStack item, List<String> scrollIds) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(GRIMOIRE_SCROLLS_KEY, PersistentDataType.STRING,
                String.join(",", scrollIds));
        item.setItemMeta(meta);
    }

    public boolean canSocketScroll(ItemStack grimoire, ItemStack scroll) {
        int grimoireTier = getGrimoireTier(grimoire);
        if (grimoireTier == 0) return false;

        int scrollTier = scroll.getItemMeta().getPersistentDataContainer()
                .getOrDefault(SCROLL_TIER_KEY, PersistentDataType.INTEGER, 0);
        if (scrollTier == 0 || scrollTier > grimoireTier) return false;

        List<String> currentScrolls = getSocketedScrolls(grimoire);
        return currentScrolls.size() < getCapacity(grimoireTier);
    }

    public ItemStack createScrollInscriptionTable() {
        ItemStack item = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.colorize("&5Scroll Inscription Table"));
        List<String> lore = new ArrayList<>();
        lore.add(Utils.colorize("&7A specialized enchanting table"));
        lore.add(Utils.colorize("&7used to inscribe spell scrolls."));
        lore.add(Utils.colorize("&ePlace it down to use!"));
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(SCROLL_INSCRIPTION_TABLE_KEY, PersistentDataType.INTEGER, 1);

        item.setItemMeta(meta);
        return item;
    }

    public String getScrollSpellId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(SCROLL_SPELL_KEY, PersistentDataType.STRING);
    }
}
