package com.perseusj.magicforce.listeners;

import com.perseusj.magicforce.managers.GrimoireManager;
import com.perseusj.magicforce.managers.InscriptionManager;
import com.perseusj.magicforce.managers.ManaManager;
import com.perseusj.magicforce.managers.ScoreboardManager;
import com.perseusj.magicforce.managers.TableManager;
import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.spells.SpellRegistry;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MagicListener implements Listener {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ENCHANTING_TABLE
                && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            if (TableManager.getInstance().isRegisteredTable(event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
                InscriptionManager.getInstance().openInscriptionUI(player);
                return;
            }
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        boolean isGrimoire = GrimoireManager.getInstance().getGrimoireTier(item) > 0;
        String scrollSpellId = GrimoireManager.getInstance().getScrollSpellId(item);

        if (isGrimoire && player.isSneaking()
                && (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR
                    || event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true);
            openSocketingGUI(player, item);
            return;
        }

        if (isGrimoire && (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true);
            castFromGrimoire(player, item);
            return;
        }

        if (scrollSpellId != null && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            castRawScroll(player, item, scrollSpellId);
            return;
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (GrimoireManager.getInstance().getGrimoireTier(item) == 0) return;

        event.setCancelled(true);
        int direction = (event.getNewSlot() - event.getPreviousSlot() + 9) % 9;
        if (direction > 4) direction -= 9;
        ScoreboardManager.getInstance().cycleSlot(player, direction > 0 ? 1 : -1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (InscriptionManager.getInstance().isInscriptionGUI(title)
                || InscriptionManager.getInstance().isTierSelectionGUI(title)) {
            InscriptionManager.getInstance().handleClick(event);
            return;
        }
        if (title.contains("Socket Scrolls")) {
            event.setCancelled(true);
            handleSocketingClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("Socket Scrolls")
                || InscriptionManager.getInstance().isInscriptionGUI(title)
                || InscriptionManager.getInstance().isTierSelectionGUI(title)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (InscriptionManager.getInstance().isTierSelectionGUI(title)) {
            InscriptionManager.getInstance().clearPendingSelection((Player) event.getPlayer());
        }
    }

    private void openSocketingGUI(Player player, ItemStack grimoire) {
        int tier = GrimoireManager.getInstance().getGrimoireTier(grimoire);
        int capacity = GrimoireManager.getInstance().getCapacity(tier);
        List<String> scrolls = GrimoireManager.getInstance().getSocketedScrolls(grimoire);

        Inventory inv = org.bukkit.Bukkit.createInventory(null, 9,
                Utils.colorize("&5Socket Scrolls (Tier " + tier + ")"));

        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(Utils.colorize("&7Locked"));
        placeholder.setItemMeta(meta);

        for (int i = 0; i < capacity; i++) {
            if (i < scrolls.size()) {
                Spell spell = SpellRegistry.getById(scrolls.get(i));
                if (spell != null) {
                    inv.setItem(i, GrimoireManager.getInstance().createScroll(spell));
                }
            }
        }
        for (int i = capacity; i < 9; i++) {
            inv.setItem(i, placeholder);
        }

        player.openInventory(inv);
    }

    private void handleSocketingClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        int slot = event.getRawSlot();

        ItemStack grimoire = player.getInventory().getItemInMainHand();
        int grimoireTier = GrimoireManager.getInstance().getGrimoireTier(grimoire);
        if (grimoireTier == 0) return;
        int capacity = GrimoireManager.getInstance().getCapacity(grimoireTier);
        List<String> scrolls = GrimoireManager.getInstance().getSocketedScrolls(grimoire);

        if (event.isShiftClick() || (slot >= 9 && event.getClick() == ClickType.LEFT)) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            String spellId = GrimoireManager.getInstance().getScrollSpellId(clicked);
            if (spellId == null) return;

            if (!GrimoireManager.getInstance().canSocketScroll(grimoire, clicked)) {
                player.sendMessage(Utils.colorize("&cThis grimoire's tier is too low for this scroll (or the grimoire is full)!"));
                return;
            }

            for (int i = 0; i < capacity; i++) {
                if (i < scrolls.size() && scrolls.get(i).equals(spellId)) continue;
                int emptySlot = findEmptySocket(scrolls, capacity);
                if (emptySlot == -1) {
                    player.sendMessage(Utils.colorize("&cAll sockets are full!"));
                    return;
                }
                while (scrolls.size() <= emptySlot) {
                    scrolls.add("");
                }
                scrolls.set(emptySlot, spellId);
                GrimoireManager.getInstance().setSocketedScrolls(grimoire, scrolls);
                clicked.setAmount(clicked.getAmount() - 1);
                openSocketingGUI(player, grimoire);
                ScoreboardManager.getInstance().updateScoreboard(player);
                player.sendMessage(Utils.colorize("&aScroll socketed!"));
                return;
            }
            return;
        }

        if (slot < 0 || slot >= 9) return;

        if (cursor != null && cursor.getType() != Material.AIR) {
            String spellId = GrimoireManager.getInstance().getScrollSpellId(cursor);
            if (spellId == null) return;

            if (slot >= capacity) {
                player.sendMessage(Utils.colorize("&cThis slot is locked for your grimoire tier!"));
                return;
            }
            int scrollTier = cursor.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(GrimoireManager.SCROLL_TIER_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
            if (scrollTier > grimoireTier) {
                player.sendMessage(Utils.colorize("&cThis grimoire's tier is too low for this scroll!"));
                return;
            }

            while (scrolls.size() < slot) {
                scrolls.add("");
            }
            if (slot < scrolls.size()) {
                scrolls.set(slot, spellId);
                event.getView().setCursor(null);
                cursor.setAmount(cursor.getAmount() - 1);
            } else if (slot < capacity) {
                scrolls.add(spellId);
                event.getView().setCursor(null);
                cursor.setAmount(cursor.getAmount() - 1);
            } else {
                return;
            }

            GrimoireManager.getInstance().setSocketedScrolls(grimoire, scrolls);
            openSocketingGUI(player, grimoire);
            ScoreboardManager.getInstance().updateScoreboard(player);
            player.sendMessage(Utils.colorize("&aScroll socketed!"));
        } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR
                && event.getCurrentItem().hasItemMeta()
                && event.getCurrentItem().getItemMeta().getPersistentDataContainer()
                        .has(GrimoireManager.SCROLL_SPELL_KEY, PersistentDataType.STRING)) {
            String spellId = GrimoireManager.getInstance().getScrollSpellId(event.getCurrentItem());
            if (spellId == null) return;

            if (slot < scrolls.size() && scrolls.get(slot).equals(spellId)) {
                scrolls.remove(slot);
                scrolls.remove("");
                GrimoireManager.getInstance().setSocketedScrolls(grimoire, scrolls);
                openSocketingGUI(player, grimoire);
                ScoreboardManager.getInstance().updateScoreboard(player);
                player.sendMessage(Utils.colorize("&eScroll removed!"));
            }
        }
    }

    private int findEmptySocket(List<String> scrolls, int capacity) {
        for (int i = 0; i < capacity; i++) {
            if (i >= scrolls.size() || scrolls.get(i).isEmpty()) return i;
        }
        return -1;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.ENCHANTING_TABLE) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                    .has(GrimoireManager.SCROLL_INSCRIPTION_TABLE_KEY, PersistentDataType.INTEGER)) {
                TableManager.getInstance().registerTable(event.getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.ENCHANTING_TABLE) {
            if (TableManager.getInstance().isRegisteredTable(event.getBlock().getLocation())) {
                event.setDropItems(false);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                        GrimoireManager.getInstance().createScrollInscriptionTable());
                TableManager.getInstance().unregisterTable(event.getBlock().getLocation());
            }
        }
    }

    private void castFromGrimoire(Player player, ItemStack grimoire) {
        List<String> scrolls = GrimoireManager.getInstance().getSocketedScrolls(grimoire);
        int activeSlot = ScoreboardManager.getInstance().getActiveSlot(player);

        if (scrolls.isEmpty() || activeSlot >= scrolls.size()) {
            player.sendMessage(Utils.colorize("&cNo spell selected!"));
            return;
        }

        String spellId = scrolls.get(activeSlot);
        Spell spell = SpellRegistry.getById(spellId);
        if (spell == null) return;

        if (!checkCooldown(player, spell)) return;

        if (!ManaManager.getInstance().removeMana(player, spell.getManaCost())) {
            player.sendMessage(Utils.colorize("&cNot enough mana!"));
            return;
        }

        spell.cast(player);
        setCooldown(player, spell);
        player.sendMessage(Utils.colorize("&6✦ Cast &f" + spell.getName()));
    }

    private void castRawScroll(Player player, ItemStack scroll, String spellId) {
        Spell spell = SpellRegistry.getById(spellId);
        if (spell == null) return;

        spell.cast(player);
        scroll.setAmount(scroll.getAmount() - 1);
        player.sendMessage(Utils.colorize("&6✦ Consumed &f" + spell.getName()));
    }

    private boolean checkCooldown(Player player, Spell spell) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return true;
        Long expires = playerCooldowns.get(spell.getId());
        if (expires == null) return true;
        if (System.currentTimeMillis() < expires) {
            long remaining = (expires - System.currentTimeMillis()) / 1000;
            player.sendMessage(Utils.colorize("&cCooldown: " + remaining + "s"));
            return false;
        }
        return true;
    }

    private void setCooldown(Player player, Spell spell) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(spell.getId(), System.currentTimeMillis() + (long) (spell.getCooldown() * 1000));
    }
}
