package com.perseusj.magicforce.spells;

import org.bukkit.entity.Player;

public abstract class Spell {
    private final String id;
    private final String name;
    private final SpellElement element;
    private final int tier;
    private final int manaCost;
    private final double cooldown;

    public Spell(String id, String name, SpellElement element, int tier, int manaCost, double cooldown) {
        this.id = id;
        this.name = name;
        this.element = element;
        this.tier = tier;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public SpellElement getElement() { return element; }
    public int getTier() { return tier; }
    public int getManaCost() { return manaCost; }
    public double getCooldown() { return cooldown; }

    public abstract void cast(Player player);
}
