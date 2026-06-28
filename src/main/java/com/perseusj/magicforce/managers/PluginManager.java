package com.perseusj.magicforce.managers;

import com.perseusj.magicforce.MagicForce;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public class PluginManager {
    private static PluginManager instance;

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void initialize() {
        ManaManager.getInstance().initialize();
        TableManager.getInstance().loadTables();
        registerRecipes();
    }

    private void registerRecipes() {
        GrimoireManager gm = GrimoireManager.getInstance();
        MagicForce plugin = MagicForce.getInstance();

        ShapedRecipe tier1 = new ShapedRecipe(
                new org.bukkit.NamespacedKey(plugin, "grimoire_tier_1"), gm.createGrimoire(1));
        tier1.shape(" G ", "L I", " P ");
        tier1.setIngredient('G', org.bukkit.Material.GOLD_INGOT);
        tier1.setIngredient('L', org.bukkit.Material.LEATHER);
        tier1.setIngredient('P', org.bukkit.Material.PAPER);
        tier1.setIngredient('I', org.bukkit.Material.INK_SAC);
        plugin.getServer().addRecipe(tier1);

        ShapedRecipe tier2 = new ShapedRecipe(
                new org.bukkit.NamespacedKey(plugin, "grimoire_tier_2"), gm.createGrimoire(2));
        tier2.shape(" D ", "L I", " P ");
        tier2.setIngredient('D', org.bukkit.Material.DIAMOND);
        tier2.setIngredient('L', org.bukkit.Material.LEATHER);
        tier2.setIngredient('P', org.bukkit.Material.PAPER);
        tier2.setIngredient('I', org.bukkit.Material.INK_SAC);
        plugin.getServer().addRecipe(tier2);

        ShapedRecipe tier3 = new ShapedRecipe(
                new org.bukkit.NamespacedKey(plugin, "grimoire_tier_3"), gm.createGrimoire(3));
        tier3.shape(" N ", "G I", " L ");
        tier3.setIngredient('N', org.bukkit.Material.NETHERITE_SCRAP);
        tier3.setIngredient('G', org.bukkit.Material.GOLD_INGOT);
        tier3.setIngredient('L', org.bukkit.Material.LEATHER);
        tier3.setIngredient('I', org.bukkit.Material.INK_SAC);
        plugin.getServer().addRecipe(tier3);

        org.bukkit.inventory.ItemStack inscriptionTable = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENCHANTING_TABLE);
        org.bukkit.inventory.meta.ItemMeta meta = inscriptionTable.getItemMeta();
        meta.setDisplayName(com.perseusj.magicforce.utils.Utils.colorize("&5Scroll Inscription Table"));
        meta.setLore(java.util.List.of(
                com.perseusj.magicforce.utils.Utils.colorize("&7A specialized enchanting table"),
                com.perseusj.magicforce.utils.Utils.colorize("&7used to inscribe spell scrolls."),
                com.perseusj.magicforce.utils.Utils.colorize("&ePlace it down to use!")
        ));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "scroll_inscription_table"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        inscriptionTable.setItemMeta(meta);

        ShapedRecipe tableRecipe = new ShapedRecipe(
                new org.bukkit.NamespacedKey(plugin, "scroll_inscription_table"), inscriptionTable);
        tableRecipe.shape("FBF", "IET", "LOL");
        tableRecipe.setIngredient('F', org.bukkit.Material.FEATHER);
        tableRecipe.setIngredient('B', org.bukkit.Material.BOOK);
        tableRecipe.setIngredient('I', org.bukkit.Material.INK_SAC);
        tableRecipe.setIngredient('E', org.bukkit.Material.ENCHANTING_TABLE);
        tableRecipe.setIngredient('T', org.bukkit.Material.INK_SAC);
        tableRecipe.setIngredient('L', org.bukkit.Material.LAPIS_LAZULI);
        tableRecipe.setIngredient('O', org.bukkit.Material.OBSIDIAN);
        plugin.getServer().addRecipe(tableRecipe);
    }
}
