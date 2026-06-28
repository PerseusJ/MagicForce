package com.perseusj.magicforce;

import org.bukkit.plugin.java.JavaPlugin;
import com.perseusj.magicforce.managers.PluginManager;
import com.perseusj.magicforce.listeners.PlayerListener;

public class MagicForce extends JavaPlugin {
    
    @Override
    public void onEnable() {
        
        // Initialize managers
        PluginManager.getInstance().initialize();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        
        getLogger().info("MagicForce has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MagicForce has been disabled!");
    }
    
}