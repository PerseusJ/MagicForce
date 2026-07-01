package com.perseusj.magicforce;

import org.bukkit.plugin.java.JavaPlugin;
import com.perseusj.magicforce.commands.AdminCommand;
import com.perseusj.magicforce.managers.ChantingManager;
import com.perseusj.magicforce.managers.ConfigManager;
import com.perseusj.magicforce.managers.CooldownManager;
import com.perseusj.magicforce.managers.ManaManager;
import com.perseusj.magicforce.managers.PluginManager;
import com.perseusj.magicforce.managers.TableManager;
import com.perseusj.magicforce.listeners.PlayerListener;
import com.perseusj.magicforce.listeners.MagicListener;

public class MagicForce extends JavaPlugin {
    private static MagicForce instance;

    public static MagicForce getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // v1.0.2: configuration system must load first so other managers
        // can read tunables during their own initialization.
        ConfigManager.getInstance().load();

        PluginManager.getInstance().initialize();
        ChantingManager.getInstance(); // initialize singleton

        // v1.0.1: load persisted cooldowns and mana before players can interact
        CooldownManager.getInstance().load();

        if (getCommand("magicforce") != null) {
            getCommand("magicforce").setExecutor(new AdminCommand());
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new MagicListener(), this);

        getLogger().info("MagicForce has been enabled!");
    }

    @Override
    public void onDisable() {
        ChantingManager.getInstance().cleanup();
        TableManager.getInstance().saveTables();

        // v1.0.1: persist cooldowns and mana so data survives restarts/reloads
        CooldownManager.getInstance().save();
        ManaManager.getInstance().saveAll();

        getLogger().info("MagicForce has been disabled!");
    }
}
