package com.perseusj.magicforce.spells;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpellRegistry {
    private static final Map<String, Spell> spells = new HashMap<>();

    static {
        for (int tier = 1; tier <= 3; tier++) {
            register(new IgnitionDart(tier));
            register(new CombustionBlast(tier));
            register(new AquaTorrent(tier));
            register(new CleansingTide(tier));
            register(new EarthenRampart(tier));
            register(new TremorSmash(tier));
            register(new GaleForce(tier));
            register(new ZephyrBlade(tier));
        }
    }

    public static void register(Spell spell) {
        spells.put(spell.getId(), spell);
    }

    public static Spell getById(String id) {
        return spells.get(id);
    }

    public static Collection<Spell> getAll() {
        return spells.values();
    }

    public static List<Spell> getByTier(int tier) {
        return spells.values().stream()
                .filter(s -> s.getTier() == tier)
                .collect(Collectors.toList());
    }
}
