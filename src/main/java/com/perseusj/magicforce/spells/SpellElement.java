package com.perseusj.magicforce.spells;

import org.bukkit.ChatColor;

public enum SpellElement {
    FIRE(ChatColor.RED, "Fire"),
    WATER(ChatColor.AQUA, "Water"),
    EARTH(ChatColor.DARK_GREEN, "Earth"),
    WIND(ChatColor.WHITE, "Wind");

    private final ChatColor color;
    private final String displayName;

    SpellElement(ChatColor color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public ChatColor getColor() { return color; }
    public String getDisplayName() { return displayName; }
}
