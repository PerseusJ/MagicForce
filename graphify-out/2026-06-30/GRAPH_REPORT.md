# Graph Report - MagicForce  (2026-06-28)

## Corpus Check
- 24 files · ~4,033 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 191 nodes · 389 edges · 17 communities (13 shown, 4 thin omitted)
- Extraction: 79% EXTRACTED · 21% INFERRED · 0% AMBIGUOUS · INFERRED: 82 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d6b81fb3`
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

## God Nodes (most connected - your core abstractions)
1. `MagicListener` - 13 edges
2. `InscriptionManager` - 13 edges
3. `GrimoireManager` - 10 edges
4. `ManaManager` - 10 edges
5. `Spell` - 9 edges
6. `ItemStack` - 8 edges
7. `Player` - 7 edges
8. `ScoreboardManager` - 7 edges
9. `MagicForce` - 6 edges
10. `EventHandler` - 6 edges

## Surprising Connections (you probably didn't know these)
- `plugin.yml Configuration` --references--> `MagicForce`  [EXTRACTED]
  src/main/resources/plugin.yml → src/main/java/com/perseusj/magicforce/MagicForce.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Plugin Lifecycle and Management** — magicforce_magicforce_magicforce, listeners_playerlistener_playerlistener, managers_pluginmanager_pluginmanager [INFERRED 0.85]

## Communities (17 total, 4 thin omitted)

### Community 0 - "Player Event Handling"
Cohesion: 0.31
Nodes (6): Listener, PlayerListener, PlayerInteractEvent, PlayerJoinEvent, PlayerQuitEvent, EventHandler

### Community 1 - "Plugin Initialization and Lifecycle"
Cohesion: 0.24
Nodes (5): JavaPlugin, MagicForce, PluginManager, plugin.yml Configuration, Override

### Community 2 - "Plugin Manager Startup"
Cohesion: 0.16
Nodes (11): InventoryCloseEvent, InventoryDragEvent, MagicListener, GrimoireManager, PlayerItemHeldEvent, EventHandler, InventoryClickEvent, ItemStack (+3 more)

### Community 3 - "Text Color Utilities"
Cohesion: 0.36
Nodes (6): ChatColor, getColor(), getDisplayName(), SpellElement(), String, Utils

### Community 6 - "Community 6"
Cohesion: 0.15
Nodes (10): SpellElement, Spell, ItemStack, Player, Spell, String, Spell, Player (+2 more)

### Community 7 - "Community 7"
Cohesion: 0.09
Nodes (13): Spell, CleansingTide, CombustionBlast, EarthenRampart, IgnitionDart, Override, Player, Override (+5 more)

### Community 8 - "Community 8"
Cohesion: 0.30
Nodes (5): InscriptionManager, InventoryClickEvent, ItemStack, Player, UUID

### Community 10 - "Community 10"
Cohesion: 0.29
Nodes (5): Collection, SpellRegistry, List, Spell, String

### Community 12 - "Community 12"
Cohesion: 0.40
Nodes (3): AquaTorrent, Override, Player

### Community 13 - "Community 13"
Cohesion: 0.40
Nodes (3): GaleForce, Override, Player

### Community 14 - "Community 14"
Cohesion: 0.40
Nodes (3): TremorSmash, Override, Player

### Community 15 - "Community 15"
Cohesion: 0.40
Nodes (3): ZephyrBlade, Override, Player

### Community 16 - "Community 16"
Cohesion: 0.50
Nodes (3): 1. Enchanting Table Inscription GUI, 2. Dynamic Scroll Selection Sub-UIs, MagicForce - Scroll Inscription Selection System

## Knowledge Gaps
- **17 isolated node(s):** `java.compile.nullAnalysis.mode`, `String`, `Override`, `Override`, `Override` (+12 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PlayerListener` connect `Player Event Handling` to `Plugin Initialization and Lifecycle`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **Why does `InscriptionManager` connect `Community 8` to `Plugin Manager Startup`?**
  _High betweenness centrality (0.081) - this node is a cross-community bridge._
- **Why does `MagicListener` connect `Plugin Manager Startup` to `Player Event Handling`, `Community 6`?**
  _High betweenness centrality (0.077) - this node is a cross-community bridge._
- **What connects `java.compile.nullAnalysis.mode`, `String`, `Override` to the rest of the system?**
  _17 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 6` be split into smaller, more focused modules?**
  _Cohesion score 0.14666666666666667 - nodes in this community are weakly interconnected._
- **Should `Community 7` be split into smaller, more focused modules?**
  _Cohesion score 0.09333333333333334 - nodes in this community are weakly interconnected._