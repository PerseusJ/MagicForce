package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.MagicForce;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TableManager {
    private static TableManager instance;
    private final Set<Location> inscriptionTables = new HashSet<>();
    private File file;
    private YamlConfiguration config;

    public static TableManager getInstance() {
        if (instance == null) {
            instance = new TableManager();
        }
        return instance;
    }

    public void loadTables() {
        file = new File(MagicForce.getInstance().getDataFolder(), "tables.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        inscriptionTables.clear();
        List<String> tables = config.getStringList("inscription_tables");
        
        for (String s : tables) {
            String[] parts = s.split(",");
            if (parts.length == 4) {
                World w = Bukkit.getWorld(parts[0]);
                if (w != null) {
                    try {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        inscriptionTables.add(new Location(w, x, y, z));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public void saveTables() {
        if (file == null || config == null) return;
        List<String> tables = inscriptionTables.stream()
                .map(loc -> loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ())
                .toList();
        config.set("inscription_tables", tables);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRegisteredTable(Location location) {
        return inscriptionTables.stream().anyMatch(loc -> 
                loc.getWorld().equals(location.getWorld()) &&
                loc.getBlockX() == location.getBlockX() &&
                loc.getBlockY() == location.getBlockY() &&
                loc.getBlockZ() == location.getBlockZ());
    }

    public void registerTable(Location location) {
        inscriptionTables.add(location);
        saveTables(); // Auto-save for safety
    }

    public void unregisterTable(Location location) {
        inscriptionTables.removeIf(loc -> 
                loc.getWorld().equals(location.getWorld()) &&
                loc.getBlockX() == location.getBlockX() &&
                loc.getBlockY() == location.getBlockY() &&
                loc.getBlockZ() == location.getBlockZ());
        saveTables(); // Auto-save for safety
    }
}
