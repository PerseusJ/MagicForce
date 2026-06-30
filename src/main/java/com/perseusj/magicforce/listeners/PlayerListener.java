package com.perseusj.magicforce.listeners;

import com.perseusj.magicforce.managers.ManaManager;
import com.perseusj.magicforce.managers.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Bug fix: restore persisted mana instead of always resetting to 100
        double savedMana = ManaManager.getInstance().loadMana(player.getUniqueId());
        ManaManager.getInstance().setMana(player, savedMana);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Bug fix: save current mana so it survives disconnects
        ManaManager.getInstance().saveMana(player.getUniqueId(),
                ManaManager.getInstance().getMana(player));
        ManaManager.getInstance().resetMana(player.getUniqueId());
        ScoreboardManager.getInstance().removePlayer(player);
    }
}
