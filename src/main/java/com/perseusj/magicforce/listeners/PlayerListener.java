package com.perseusj.magicforce.listeners;

import com.perseusj.magicforce.managers.ManaManager;
import com.perseusj.magicforce.managers.ScoreboardManager;
import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.spells.SpellRegistry;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ManaManager.getInstance().setMana(player, 100.0);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ManaManager.getInstance().resetMana(player.getUniqueId());
        ScoreboardManager.getInstance().removePlayer(player);
    }
}
