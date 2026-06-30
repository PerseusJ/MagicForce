# Graph Report - MagicForce  (2026-06-30)

## Corpus Check
- 28 files · ~18,644 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 387 nodes · 808 edges · 26 communities (24 shown, 2 thin omitted)
- Extraction: 81% EXTRACTED · 19% INFERRED · 0% AMBIGUOUS · INFERRED: 153 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `06140205`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Player Event Handling|Player Event Handling]]
- [[_COMMUNITY_Plugin Initialization and Lifecycle|Plugin Initialization and Lifecycle]]
- [[_COMMUNITY_Plugin Manager Startup|Plugin Manager Startup]]
- [[_COMMUNITY_Text Color Utilities|Text Color Utilities]]
- [[_COMMUNITY_Graphify Agent Rules|Graphify Agent Rules]]
- [[_COMMUNITY_VS Code Environment Settings|VS Code Environment Settings]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]

## God Nodes (most connected - your core abstractions)
1. `MagicListener` - 18 edges
2. `Spell` - 15 edges
3. `ChantingManager` - 14 edges
4. `ManaManager` - 14 edges
5. `InscriptionManager` - 13 edges
6. `EventHandler` - 12 edges
7. `IgnitionDart` - 12 edges
8. `GrimoireManager` - 11 edges
9. `AquaTorrent` - 11 edges
10. `CleansingTide` - 11 edges

## Surprising Connections (you probably didn't know these)
- `plugin.yml Configuration` --references--> `MagicForce`  [EXTRACTED]
  src/main/resources/plugin.yml → src/main/java/com/perseusj/magicforce/MagicForce.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Plugin Lifecycle and Management** — magicforce_magicforce_magicforce, listeners_playerlistener_playerlistener, managers_pluginmanager_pluginmanager [INFERRED 0.85]

## Communities (26 total, 2 thin omitted)

### Community 0 - "Player Event Handling"
Cohesion: 0.10
Nodes (17): BlockBreakEvent, BlockExplodeEvent, BlockPlaceEvent, EntityDamageByEntityEvent, EntityExplodeEvent, InventoryCloseEvent, InventoryDragEvent, MagicListener (+9 more)

### Community 1 - "Plugin Initialization and Lifecycle"
Cohesion: 0.15
Nodes (8): JavaPlugin, MagicForce, CooldownManager, PluginManager, plugin.yml Configuration, Override, File, UUID

### Community 2 - "Plugin Manager Startup"
Cohesion: 0.17
Nodes (9): GrimoireManager, ScoreboardManager, ItemStack, Player, ItemStack, List, Spell, String (+1 more)

### Community 3 - "Text Color Utilities"
Cohesion: 0.14
Nodes (11): BarColor, ChantingManager, ChantState, ItemStack, Location, Player, Spell, SpellElement (+3 more)

### Community 6 - "Community 6"
Cohesion: 0.18
Nodes (5): Sound, Spell, Location, Player, SpellElement

### Community 7 - "Community 7"
Cohesion: 0.26
Nodes (5): Spell, EarthenRampart, Location, Override, Player

### Community 8 - "Community 8"
Cohesion: 0.15
Nodes (11): ChatColor, InscriptionManager, getColor(), getDisplayName(), SpellElement(), InventoryClickEvent, ItemStack, Player (+3 more)

### Community 9 - "Community 9"
Cohesion: 0.16
Nodes (10): Listener, PlayerListener, ManaManager, PlayerInteractEvent, PlayerJoinEvent, PlayerQuitEvent, EventHandler, File (+2 more)

### Community 10 - "Community 10"
Cohesion: 0.18
Nodes (13): Collection, Command, AdminCommand, CommandSender, SpellRegistry, List, Override, Player (+5 more)

### Community 11 - "Community 11"
Cohesion: 0.26
Nodes (4): IgnitionDart, Location, Override, Player

### Community 12 - "Community 12"
Cohesion: 0.25
Nodes (4): AquaTorrent, Location, Override, Player

### Community 13 - "Community 13"
Cohesion: 0.27
Nodes (4): GaleForce, Location, Override, Player

### Community 14 - "Community 14"
Cohesion: 0.24
Nodes (5): Block, TremorSmash, Location, Override, Player

### Community 15 - "Community 15"
Cohesion: 0.27
Nodes (4): ZephyrBlade, Location, Override, Player

### Community 16 - "Community 16"
Cohesion: 0.25
Nodes (4): CleansingTide, Location, Override, Player

### Community 17 - "Community 17"
Cohesion: 0.25
Nodes (4): CombustionBlast, Location, Override, Player

### Community 18 - "Community 18"
Cohesion: 0.20
Nodes (10): Changes to Existing Systems, New Elements, New Spell Archetypes, Spell Augmentation System, v3.0.0 - New Elements & Spell Archetypes (Major Release), v3.0.1 - New Element Bug Fixes, v3.0.2 - Rituals & Crafting Expansion, v3.0.3 - Spell Combos & Synergies (+2 more)

### Community 19 - "Community 19"
Cohesion: 0.22
Nodes (9): Changes to Existing Systems, Migration, New Systems, v2.0.0 - Mage Progression System (Major Release), v2.0.1 - Progression Bug Fixes, v2.0.2 - Achievement System, v2.0.3 - Grimoire Enhancements, v2.0.4 - Balancing & Tuning Pass (+1 more)

### Community 20 - "Community 20"
Cohesion: 0.29
Nodes (7): Addon System, API Architecture, Integration Hooks, Key API Methods, PlaceholderAPI Integration, Refactoring for API Stability, v5.0.0 - Public Developer API (Major Release)

### Community 21 - "Community 21"
Cohesion: 0.33
Nodes (6): Boss System, Changes to Existing Systems, Dungeon System, Party System, PvP Arena System, v4.0.0 - Bosses, Dungeons & Parties (Major Release)

### Community 22 - "Community 22"
Cohesion: 0.33
Nodes (6): v1.0.1 - Critical Bug Fixes, v1.0.2 - Configuration System, v1.0.3 - Quality of Life & Feedback, v1.0.4 - Performance & Robustness, v1.0.5 - Final Pre-2.0 Polish, Version 1.0.x - Stabilization & Polish

### Community 23 - "Community 23"
Cohesion: 0.40
Nodes (4): Guiding Principles, MagicForce Development Roadmap, Release Cadence Summary, Table of Contents

### Community 24 - "Community 24"
Cohesion: 0.40
Nodes (5): v4.0.1 - Boss & Dungeon Bug Fixes, v4.0.2 - World Events, v4.0.3 - Economy & Trading, v4.0.4 - Social & Cosmetics, Version 4.0.x - World & Multiplayer

### Community 25 - "Community 25"
Cohesion: 0.40
Nodes (5): v5.0.1 - API Bug Fixes & Documentation, v5.0.2 - Addon Marketplace & Discovery, v5.0.3 - Performance Profiling & Debug Tools, v5.0.4 - Long-Term Sustainability & Final Polish, Version 5.0.x - API & Ecosystem

## Knowledge Gaps
- **47 isolated node(s):** `java.compile.nullAnalysis.mode`, `String`, `graphify`, `Workflow: graphify`, `Table of Contents` (+42 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MagicListener` connect `Player Event Handling` to `Community 9`, `Plugin Manager Startup`, `Text Color Utilities`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._
- **Why does `IgnitionDart` connect `Community 11` to `Community 7`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **Why does `ZephyrBlade` connect `Community 15` to `Community 7`?**
  _High betweenness centrality (0.046) - this node is a cross-community bridge._
- **What connects `java.compile.nullAnalysis.mode`, `String`, `graphify` to the rest of the system?**
  _47 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Player Event Handling` be split into smaller, more focused modules?**
  _Cohesion score 0.10241820768136557 - nodes in this community are weakly interconnected._
- **Should `Text Color Utilities` be split into smaller, more focused modules?**
  _Cohesion score 0.13911290322580644 - nodes in this community are weakly interconnected._