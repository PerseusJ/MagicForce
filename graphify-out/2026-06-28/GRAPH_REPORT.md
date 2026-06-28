# Graph Report - .  (2026-06-28)

## Corpus Check
- Corpus is ~341 words - fits in a single context window. You may not need a graph.

## Summary
- 25 nodes · 27 edges · 6 communities (2 shown, 4 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 2 edges (avg confidence: 0.8)
- Token cost: 1,500 input · 300 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Player Event Handling|Player Event Handling]]
- [[_COMMUNITY_Plugin Initialization and Lifecycle|Plugin Initialization and Lifecycle]]
- [[_COMMUNITY_Plugin Manager Startup|Plugin Manager Startup]]
- [[_COMMUNITY_Text Color Utilities|Text Color Utilities]]
- [[_COMMUNITY_Graphify Agent Rules|Graphify Agent Rules]]
- [[_COMMUNITY_VS Code Environment Settings|VS Code Environment Settings]]

## God Nodes (most connected - your core abstractions)
1. `MagicForce` - 5 edges
2. `PlayerListener` - 4 edges
3. `PluginManager` - 3 edges
4. `Override` - 2 edges
5. `PlayerJoinEvent` - 2 edges
6. `EventHandler` - 2 edges
7. `Utils` - 2 edges
8. `java.compile.nullAnalysis.mode` - 1 edges
9. `String` - 1 edges
10. `plugin.yml Configuration` - 1 edges

## Surprising Connections (you probably didn't know these)
- `plugin.yml Configuration` --references--> `MagicForce`  [EXTRACTED]
  src/main/resources/plugin.yml → src/main/java/com/perseusj/magicforce/MagicForce.java
- `Graphify Workflow` --references--> `Graphify Rules`  [EXTRACTED]
  .agents/workflows/graphify.md → .agents/rules/graphify.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Plugin Lifecycle and Management** — magicforce_magicforce_magicforce, listeners_playerlistener_playerlistener, managers_pluginmanager_pluginmanager [INFERRED 0.85]

## Communities (6 total, 4 thin omitted)

### Community 0 - "Player Event Handling"
Cohesion: 0.53
Nodes (4): EventHandler, Listener, PlayerListener, PlayerJoinEvent

### Community 1 - "Plugin Initialization and Lifecycle"
Cohesion: 0.40
Nodes (4): JavaPlugin, MagicForce, Override, plugin.yml Configuration

## Knowledge Gaps
- **5 isolated node(s):** `java.compile.nullAnalysis.mode`, `String`, `plugin.yml Configuration`, `Graphify Rules`, `Graphify Workflow`
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PlayerListener` connect `Player Event Handling` to `Plugin Manager Startup`?**
  _High betweenness centrality (0.204) - this node is a cross-community bridge._
- **Why does `MagicForce` connect `Plugin Initialization and Lifecycle` to `Plugin Manager Startup`?**
  _High betweenness centrality (0.168) - this node is a cross-community bridge._
- **What connects `java.compile.nullAnalysis.mode`, `String`, `plugin.yml Configuration` to the rest of the system?**
  _5 weakly-connected nodes found - possible documentation gaps or missing edges._