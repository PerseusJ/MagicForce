package com.perseusj.magicforce.commands;

import com.perseusj.magicforce.managers.ConfigManager;
import com.perseusj.magicforce.managers.GrimoireManager;
import com.perseusj.magicforce.spells.Spell;
import com.perseusj.magicforce.spells.SpellRegistry;
import com.perseusj.magicforce.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("magicforce.admin")) {
            sender.sendMessage(Utils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Utils.colorize("&cUsage: /mf <give|reload> ..."));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            // v1.0.2: re-read config.yml from disk and re-validate.
            ConfigManager.getInstance().reload();
            sender.sendMessage(Utils.colorize("&aConfiguration reloaded."));
            return true;
        }

        if (sub.equals("give")) {
            return handleGive(sender, args);
        }

        sender.sendMessage(Utils.colorize("&cUsage: /mf <give|reload> ..."));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.colorize("&cUsage: /mf give <inscriptiontable|grimoire|scroll> [player] [args]"));
            return true;
        }

        String itemType = args[1].toLowerCase();
        Player target;

        switch (itemType) {
            case "inscriptiontable" -> {
                target = resolveTarget(sender, args.length > 2 ? args[2] : null);
                if (target == null) {
                    sender.sendMessage(Utils.colorize("&cPlayer not found."));
                    return true;
                }
                ItemStack table = GrimoireManager.getInstance().createScrollInscriptionTable();
                target.getInventory().addItem(table);
                sender.sendMessage(Utils.colorize("&aGave Scroll Inscription Table to " + target.getName()));
                if (!sender.equals(target)) {
                    target.sendMessage(Utils.colorize("&aYou received a Scroll Inscription Table."));
                }
            }
            case "grimoire" -> {
                if (args.length < 3 || (!args[2].equals("1") && !args[2].equals("2") && !args[2].equals("3"))) {
                    sender.sendMessage(Utils.colorize("&cUsage: /mf give grimoire <1|2|3> [player]"));
                    return true;
                }
                int tier = Integer.parseInt(args[2]);
                target = resolveTarget(sender, args.length > 3 ? args[3] : null);
                if (target == null) {
                    sender.sendMessage(Utils.colorize("&cPlayer not found."));
                    return true;
                }
                ItemStack grimoire = GrimoireManager.getInstance().createGrimoire(tier);
                target.getInventory().addItem(grimoire);
                sender.sendMessage(Utils.colorize("&aGave Tier " + tier + " Grimoire to " + target.getName()));
                if (!sender.equals(target)) {
                    target.sendMessage(Utils.colorize("&aYou received a Tier " + tier + " Grimoire."));
                }
            }
            case "scroll" -> {
                if (args.length < 3) {
                    sender.sendMessage(Utils.colorize("&cUsage: /mf give scroll <spell> [player]"));
                    sender.sendMessage(Utils.colorize("&7Available spells: " + getSpellNames()));
                    return true;
                }
                String spellArg = args[2].toLowerCase();
                Spell spell = SpellRegistry.getById(spellArg);
                if (spell == null) {
                    for (Spell s : SpellRegistry.getAll()) {
                        if (s.getName().toLowerCase().contains(spellArg)) {
                            spell = s;
                            break;
                        }
                    }
                }
                if (spell == null) {
                    sender.sendMessage(Utils.colorize("&cSpell not found. Available: " + getSpellNames()));
                    return true;
                }
                target = resolveTarget(sender, args.length > 3 ? args[3] : null);
                if (target == null) {
                    sender.sendMessage(Utils.colorize("&cPlayer not found."));
                    return true;
                }
                ItemStack scroll = GrimoireManager.getInstance().createScroll(spell);
                target.getInventory().addItem(scroll);
                sender.sendMessage(Utils.colorize("&aGave " + spell.getName() + " scroll to " + target.getName()));
                if (!sender.equals(target)) {
                    target.sendMessage(Utils.colorize("&aYou received a " + spell.getName() + " scroll."));
                }
            }
            default -> {
                sender.sendMessage(Utils.colorize("&cUnknown item: " + itemType));
                sender.sendMessage(Utils.colorize("&7Valid items: inscriptiontable, grimoire, scroll"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("magicforce.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return List.of("give", "reload");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return List.of("inscriptiontable", "grimoire", "scroll");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            return new ArrayList<>();
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            if (args.length == 3) {
                return switch (args[1].toLowerCase()) {
                    case "grimoire" -> List.of("1", "2", "3");
                    case "scroll" -> getSpellNamesList();
                    case "inscriptiontable" -> null; // returns online player names
                    default -> new ArrayList<>();
                };
            }

            if (args.length == 4 && (args[1].equalsIgnoreCase("grimoire") || args[1].equalsIgnoreCase("scroll"))) {
                return null; // returns online player names
            }
        }

        return new ArrayList<>();
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (name == null) {
            return sender instanceof Player ? (Player) sender : null;
        }
        return Bukkit.getPlayerExact(name);
    }

    private String getSpellNames() {
        return SpellRegistry.getAll().stream()
                .map(s -> s.getId() + " (" + s.getName() + ")")
                .collect(Collectors.joining(", "));
    }

    private List<String> getSpellNamesList() {
        List<String> result = new ArrayList<>();
        for (Spell s : SpellRegistry.getAll()) {
            result.add(s.getId());
        }
        return result;
    }
}
