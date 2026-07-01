package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.MagicForce;
import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.spells.SpellElement;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChantingManager {

    private static ChantingManager instance;

    public static ChantingManager getInstance() {
        if (instance == null) {
            instance = new ChantingManager();
        }
        return instance;
    }

    // ── Per-player state ──────────────────────────────────────────────────────

    // Bug fix: ConcurrentHashMap prevents ConcurrentModificationException when
    // the per-tick BukkitRunnable and cleanup() access states simultaneously.
    private final Map<UUID, ChantState> states = new ConcurrentHashMap<>();

    private static class ChantState {
        final Spell spell;
        final ItemStack grimoire;
        BossBar bar;
        BukkitTask task;
        boolean ready = false;
        int elapsed = 0;       // ticks since chanting started
        int readyTick = 0;     // elapsed value when spell became ready

        ChantState(Spell spell, ItemStack grimoire) {
            this.spell = spell;
            this.grimoire = grimoire;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Start charging a spell. Cancels any existing charge silently first. */
    public void startChanting(Player player, Spell spell, ItemStack grimoire) {
        cancelChanting(player, false);

        ChantState state = new ChantState(spell, grimoire);

        // Boss bar with element colour
        BarColor barColor = elementToBarColor(spell.getElement());
        BossBar bar = org.bukkit.Bukkit.createBossBar(
            Utils.colorize("&f\u2746 Charging: &e" + spell.getName()),
            barColor,
            BarStyle.SOLID
        );
        bar.setProgress(0.0);
        bar.addPlayer(player);
        state.bar = bar;

        states.put(player.getUniqueId(), state);

        // Start sound
        player.playSound(player.getLocation(), spell.getChantingStartSound(), 0.8f, 1.2f);

        // Per-tick task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                ChantState s = states.get(player.getUniqueId());
                if (s == null) { cancel(); return; }

                if (!player.isOnline() || player.isDead()) {
                    cancelChanting(player, false);
                    cancel();
                    return;
                }

                // Bug fix: compare by PDC grimoire identity (tier) rather than
                // ItemStack.isSimilar(), which fails when lore changes due to socketing.
                ItemStack held = player.getInventory().getItemInMainHand();
                if (!isSameGrimoire(held, s.grimoire)) {
                    cancelChanting(player, true);
                    cancel();
                    return;
                }

                s.elapsed++;

                if (!s.ready) {
                    // ── Charging phase ─────────────────────────────────────
                    double progress = Math.min(1.0, (double) s.elapsed / s.spell.getChargeTimeTicks());
                    s.bar.setProgress(progress);

                    // Spawn chanting particles at hand position
                    Location handLoc = getHandLocation(player);
                    s.spell.spawnChantingParticles(player, handLoc, progress, s.elapsed);

                    // Apply slowness (short duration, refreshed each tick)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5, 1, true, false, false));

                    // Looping charge sound (pitch rises with progress)
                    if (s.elapsed % ConfigManager.getInstance().getLoopSoundInterval() == 0) {
                        player.playSound(player.getLocation(), s.spell.getChantingLoopSound(),
                            0.5f, 1.0f + (float) progress * 0.4f);
                    }

                    // Check completion
                    if (s.elapsed >= s.spell.getChargeTimeTicks()) {
                        if (!ManaManager.getInstance().removeMana(player, s.spell.getManaCost())) {
                            player.sendMessage(Utils.colorize("&cNot enough mana!"));
                            cancelChanting(player, false);
                            cancel();
                            return;
                        }
                        s.ready = true;
                        s.readyTick = s.elapsed;
                        s.bar.setProgress(1.0);
                        s.bar.setTitle(Utils.colorize("&a\u2746 Ready: &f" + s.spell.getName()
                            + " &7(left-click to cast)"));
                        player.playSound(player.getLocation(), s.spell.getChantingReadySound(), 1.0f, 1.0f);
                    }

                } else {
                    // ── Ready phase — holding the charged spell ─────────────
                    int heldTicks = s.elapsed - s.readyTick;

                    // Keep particles at full intensity
                    Location handLoc = getHandLocation(player);
                    s.spell.spawnChantingParticles(player, handLoc, 1.0, s.elapsed);

                    // Pulse boss bar to signal "waiting for click"
                    double pulse = 0.85 + Math.sin(s.elapsed * 0.4) * 0.15;
                    s.bar.setProgress(Math.max(0.0, Math.min(1.0, pulse)));

                    // Timeout fizzle
                    if (heldTicks >= ConfigManager.getInstance().getHoldWindowTicks()) {
                        player.sendMessage(Utils.colorize("&7The spell dissipated..."));
                        player.playSound(player.getLocation(), s.spell.getChantingCancelSound(), 0.6f, 0.8f);
                        cleanupState(player);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(MagicForce.getInstance(), 1L, 1L);

        state.task = task;
    }

    /**
     * Cancel an in-progress chant.
     * @param playSound whether to play the cancel/fizzle sound
     */
    public void cancelChanting(Player player, boolean playSound) {
        ChantState s = states.remove(player.getUniqueId());
        if (s == null) return;
        if (s.task != null) s.task.cancel();
        if (s.bar != null) { s.bar.removePlayer(player); s.bar.removeAll(); }
        if (playSound) {
            player.playSound(player.getLocation(), s.spell.getChantingCancelSound(), 0.7f, 1.0f);
        }
    }

    /**
     * Fire the charged spell on left-click.
     * @return true if the spell was fired, false if player wasn't in READY state
     */
    public boolean fireSpell(Player player) {
        ChantState s = states.get(player.getUniqueId());
        if (s == null || !s.ready) return false;

        Spell spell = s.spell;
        cleanupState(player);

        spell.cast(player);
        player.sendMessage(Utils.colorize("&6\u2746 Cast &f" + spell.getName()));
        return true;
    }

    // ── State queries ─────────────────────────────────────────────────────────

    public boolean isChanting(Player player) {
        ChantState s = states.get(player.getUniqueId());
        return s != null && !s.ready;
    }

    public boolean isReady(Player player) {
        ChantState s = states.get(player.getUniqueId());
        return s != null && s.ready;
    }

    public boolean isChantingOrReady(Player player) {
        return states.containsKey(player.getUniqueId());
    }

    public Spell getCurrentSpell(Player player) {
        ChantState s = states.get(player.getUniqueId());
        return s == null ? null : s.spell;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Cleanup without sound (spell was fired or state already handled). */
    private void cleanupState(Player player) {
        ChantState s = states.remove(player.getUniqueId());
        if (s == null) return;
        if (s.task != null) s.task.cancel();
        if (s.bar != null) { s.bar.removePlayer(player); s.bar.removeAll(); }
    }

    /**
     * Approximate right-hand position of the player.
     * Matches the origin logic used in projectile spells.
     */
    public static Location getHandLocation(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(0.5);
        return eyeLoc.clone().add(0, -0.6, 0).add(right);
    }

    private static BarColor elementToBarColor(SpellElement element) {
        return switch (element) {
            case FIRE  -> BarColor.RED;
            case WATER -> BarColor.BLUE;
            case EARTH -> BarColor.GREEN;
            case WIND  -> BarColor.WHITE;
        };
    }

    /**
     * Bug fix: compare grimoires by PDC identity (GRIMOIRE_TIER_KEY) instead of
     * ItemStack.isSimilar(). isSimilar() compares lore, so socketing/removing a
     * scroll mid-charge would make the grimoire appear "different" and cancel the
     * chant. Comparing by tier PDC key survives lore changes.
     */
    private static boolean isSameGrimoire(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (!a.hasItemMeta() || !b.hasItemMeta()) return false;
        PersistentDataContainer pdcA = a.getItemMeta().getPersistentDataContainer();
        PersistentDataContainer pdcB = b.getItemMeta().getPersistentDataContainer();
        Integer tierA = pdcA.get(GrimoireManager.GRIMOIRE_TIER_KEY, PersistentDataType.INTEGER);
        Integer tierB = pdcB.get(GrimoireManager.GRIMOIRE_TIER_KEY, PersistentDataType.INTEGER);
        // Both must be grimoires (tierA/tierB not null) and the same tier
        return tierA != null && tierA.equals(tierB);
    }

    /** Remove all active states (call on plugin disable). */
    public void cleanup() {
        for (UUID id : new java.util.HashSet<>(states.keySet())) {
            ChantState s = states.get(id);
            if (s != null) {
                if (s.task != null) s.task.cancel();
                if (s.bar != null) s.bar.removeAll();
            }
        }
        states.clear();
    }
}
