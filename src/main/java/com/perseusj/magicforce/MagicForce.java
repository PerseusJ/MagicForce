package com.perseusj.magicforce;

import org.bukkit.plugin.java.JavaPlugin;
import com.perseusj.magicforce.commands.AdminCommand;
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

        PluginManager.getInstance().initialize();

        if (getCommand("magicforce") != null) {
            getCommand("magicforce").setExecutor(new AdminCommand());
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new MagicListener(), this);

        getLogger().info("MagicForce has been enabled!");
    }

    @Override
    public void onDisable() {
        com.perseusj.magicforce.managers.TableManager.getInstance().saveTables();
        getLogger().info("MagicForce has been disabled!");
    }
}
