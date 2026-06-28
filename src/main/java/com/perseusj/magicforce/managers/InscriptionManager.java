package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.spells.SpellRegistry;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InscriptionManager {
    private static InscriptionManager instance;
    private static final String INSCRIPTION_TITLE = Utils.colorize("&5&lInscribe Scroll");
    private static final String TIER_SELECTION_PREFIX = "Choose Tier ";

    private final Map<UUID, int[]> pendingSelections = new HashMap<>();

    public static InscriptionManager getInstance() {
        if (instance == null) instance = new InscriptionManager();
        return instance;
    }

    public void openInscriptionUI(Player player) {
        clearPendingSelection(player);
        Inventory inv = Bukkit.createInventory(null, 27, INSCRIPTION_TITLE);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, createPlaceholder());
        }

        inv.setItem(10, new ItemStack(Material.PAPER));
        inv.setItem(12, new ItemStack(Material.INK_SAC));

        inv.setItem(20, createInscribeButton("Tier 1", "&aInscribe Tier 1 &7(5 Levels)", 1, 5));
        inv.setItem(22, createInscribeButton("Tier 2", "&bInscribe Tier 2 &7(15 Levels)", 2, 15));
        inv.setItem(24, createInscribeButton("Tier 3", "&dInscribe Tier 3 &7(30 Levels)", 3, 30));

        inv.setItem(26, createCloseButton());

        player.openInventory(inv);
    }

    private ItemStack createPlaceholder() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInscribeButton(String name, String display, int tier, int levels) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.colorize(display));
        meta.setLore(List.of(
                Utils.colorize("&7Cost: &e" + levels + " XP Levels"),
                Utils.colorize("&7Requires: &fPaper + Ink Sac"),
                Utils.colorize("&7Choose a Tier " + tier + " scroll to inscribe")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.colorize("&cClose"));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isInscriptionGUI(String title) {
        return title.equals(INSCRIPTION_TITLE);
    }

    public boolean isTierSelectionGUI(String title) {
        return title != null && title.startsWith(TIER_SELECTION_PREFIX);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        int slot = event.getRawSlot();

        if (title.equals(INSCRIPTION_TITLE)) {
            if (slot == 20) tryInscribe(player, 1, 5);
            else if (slot == 22) tryInscribe(player, 2, 15);
            else if (slot == 24) tryInscribe(player, 3, 30);
            else if (slot == 26) player.closeInventory();
        } else if (title.startsWith(TIER_SELECTION_PREFIX)) {
            handleTierSelectionClick(event, player, slot);
        }
    }

    public void clearPendingSelection(Player player) {
        pendingSelections.remove(player.getUniqueId());
    }

    private void tryInscribe(Player player, int tier, int levels) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        ItemStack paper = inv.getItem(10);
        ItemStack ink = inv.getItem(12);

        if (paper == null || paper.getType() != Material.PAPER || paper.getAmount() < 1) {
            player.sendMessage(Utils.colorize("&cYou need Paper!"));
            return;
        }
        if (ink == null || ink.getType() != Material.INK_SAC || ink.getAmount() < 1) {
            player.sendMessage(Utils.colorize("&cYou need an Ink Sac!"));
            return;
        }
        if (player.getLevel() < levels) {
            player.sendMessage(Utils.colorize("&cYou need " + levels + " XP levels!"));
            return;
        }

        List<Spell> tierSpells = SpellRegistry.getByTier(tier);
        if (tierSpells.isEmpty()) {
            player.sendMessage(Utils.colorize("&cNo spells available for that tier!"));
            return;
        }

        openTierSelectionUI(player, tier, levels);
    }

    private void openTierSelectionUI(Player player, int tier, int levels) {
        Inventory inv = Bukkit.createInventory(null, 27,
                TIER_SELECTION_PREFIX + tier + " Scroll");

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, createPlaceholder());
        }

        List<Spell> tierSpells = SpellRegistry.getByTier(tier);
        for (int i = 0; i < tierSpells.size(); i++) {
            inv.setItem(i, GrimoireManager.getInstance().createScroll(tierSpells.get(i)));
        }

        ItemStack goBack = new ItemStack(Material.ARROW);
        ItemMeta meta = goBack.getItemMeta();
        meta.setDisplayName(Utils.colorize("&7Go Back"));
        goBack.setItemMeta(meta);
        inv.setItem(22, goBack);

        pendingSelections.put(player.getUniqueId(), new int[]{tier, levels});
        player.openInventory(inv);
    }

    private void handleTierSelectionClick(InventoryClickEvent event, Player player, int slot) {
        if (slot == 22) {
            openInscriptionUI(player);
            return;
        }

        if (slot < 0 || slot > 21) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        String spellId = GrimoireManager.getInstance().getScrollSpellId(clicked);
        if (spellId == null) return;

        Spell spell = SpellRegistry.getById(spellId);
        if (spell == null) return;

        int[] selection = pendingSelections.get(player.getUniqueId());
        if (selection == null) return;

        int tier = selection[0];
        int levels = selection[1];

        if (player.getLevel() < levels) {
            player.sendMessage(Utils.colorize("&cYou need " + levels + " XP levels!"));
            return;
        }

        if (!player.getInventory().contains(Material.PAPER, 1)) {
            player.sendMessage(Utils.colorize("&cYou need Paper!"));
            return;
        }
        if (!player.getInventory().contains(Material.INK_SAC, 1)) {
            player.sendMessage(Utils.colorize("&cYou need an Ink Sac!"));
            return;
        }

        player.getInventory().removeItem(new ItemStack(Material.PAPER, 1));
        player.getInventory().removeItem(new ItemStack(Material.INK_SAC, 1));
        player.setLevel(player.getLevel() - levels);
        player.closeInventory();

        ItemStack scroll = GrimoireManager.getInstance().createScroll(spell);
        player.getInventory().addItem(scroll);
        player.sendMessage(Utils.colorize("&aYou inscribed a " + spell.getElement().getColor()
                + spell.getName() + " &ascroll!"));
        player.getWorld().playSound(player.getLocation(),
                org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        pendingSelections.remove(player.getUniqueId());
    }
}
