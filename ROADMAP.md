# MagicForce Development Roadmap

> **Current Version:** 1.0.0-SNAPSHOT
> **Target:** 5.0.0
> **Platform:** Minecraft Spigot Plugin (Java 21, Spigot API 1.21.10)
> **Last Updated:** 2026-06-29

---

## Table of Contents

- [Version 1.0.x - Stabilization & Polish](#version-10x---stabilization--polish)
- [Version 2.0.x - Progression & Persistence](#version-20x---progression--persistence)
- [Version 3.0.x - Content Expansion](#version-30x---content-expansion)
- [Version 4.0.x - World & Multiplayer](#version-40x---world--multiplayer)
- [Version 5.0.x - API & Ecosystem](#version-50x---api--ecosystem)
- [Release Cadence Summary](#release-cadence-summary)

---

## Version 1.0.x - Stabilization & Polish

The 1.0.x line focuses on hardening the existing v1.0.0 codebase: fixing bugs surfaced
in real-world testing, adding a configuration layer so server owners can tune values
without recompiling, and polishing the player experience. No new major systems are
introduced&mdash;the goal is a rock-solid foundation before the 2.0.0 progression overhaul.

### v1.0.1 - Critical Bug Fixes [COMPLETED]

**Focus:** Fix data-loss and edge-case bugs that affect gameplay integrity.

| Area | Change | Files Affected |
|------|--------|----------------|
| **Cooldown Persistence** | Cooldowns are currently stored in an in-memory `Map` in `MagicListener` and are lost on restart/reload. Persist cooldowns to a `cooldowns.yml` file keyed by `UUID:spellId` with expiry timestamps. Restore active cooldowns on plugin enable. | `MagicListener.java`, new `CooldownManager.java` |
| **Mana Persistence** | `ManaManager` resets mana to 100 on every join and wipes it on quit. Save mana values to `mana.yml` (or a `playerdata.yml`) on quit and restore on join so players don't get a free refill by relogging. | `ManaManager.java`, `PlayerListener.java` |
| **Scoreboard Slot Bounds** | `ScoreboardManager.getActiveSlot()` can return a slot index that exceeds the socketed scrolls list after a grimoire is downgraded or scrolls are removed. Add bounds clamping in `updateScoreboard()` and `cycleSlot()`. | `ScoreboardManager.java` |
| **Inscription Table Dupe** | The `BlockBreakEvent` handler drops a fresh inscription table item, but if the block is broken in creative or via explosion the event may not fire. Add a `BlockExplodeEvent` and `EntityExplodeEvent` listener to unregister and drop tables. | `MagicListener.java` |
| **Raw Scroll Cooldown Bypass** | `castRawScroll()` in `MagicListener` casts the spell directly without checking or setting cooldowns, allowing zero-cooldown spam. Route raw scroll casts through the same `checkCooldown`/`setCooldown` path. | `MagicListener.java` |
| **Grimoire Identity Check** | `ChantingManager.isSameGrimoire()` uses `ItemStack.isSimilar()`, which compares lore and meta. If a player socketed/removed a scroll mid-charge, the grimoire is "different" and the chant cancels. Compare by PDC grimoire identity instead. | `ChantingManager.java` |
| **ConcurrentModification** | `ChantingManager.cleanup()` iterates `states.keySet()` while the per-tick task may still be running. Use `ConcurrentHashMap` or snapshot the keyset (already partially done with `new HashSet<>`, but the running tasks can still mutate). | `ChantingManager.java` |

### v1.0.2 - Configuration System [COMPLETED]

**Focus:** Introduce a `config.yml` so every hardcoded constant can be tuned by server
owners without recompiling. This is a prerequisite for the 2.0.0 data-driven spell
system.

| Area | Change |
|------|--------|
| **config.yml skeleton** | Create `src/main/resources/config.yml` with sections: `mana` (max, grimoire-regen, normal-regen, hud-interval), `chanting` (hold-window-ticks, loop-sound-interval), `inscription` (tier costs in XP levels), `grimoire` (capacities per tier), `spells` (per-spell mana cost, cooldown, charge time, damage, etc.). |
| **ConfigManager** | New `ConfigManager` singleton that loads `config.yml` on enable, provides typed getters (`getInt`, `getDouble`, `getString`, `getBoolean`), and reloads on `/mf reload`. |
| **Refactor hardcoded values** | Replace all `private static final` constants in `ManaManager`, `ChantingManager`, `InscriptionManager`, `GrimoireManager`, and all spell classes with `ConfigManager` lookups. Spell tier values move to a nested `spells.<spell_id>.tier_<N>` structure. |
| **Default values** | Every config key ships with a default that matches the current hardcoded value, so existing behavior is unchanged. |
| **Config validation** | On load, validate that mana max > 0, regen >= 0, capacities >= 1, charge times >= 1 tick, etc. Log warnings for invalid values and fall back to defaults. |

### v1.0.3 - Quality of Life & Feedback

**Focus:** Improve player-facing feedback and usability without changing core mechanics.

| Area | Change |
|------|--------|
| **Cooldown HUD** | Show remaining cooldown time in the action bar alongside the mana bar (e.g., `&cIgnition Dart: 2s`). Merge into the existing `ManaManager.sendManaHUD()` or add a second action-bar line via packet. |
| **Spell info command** | Add `/mf info <spell>` that prints a spell's element, tier, mana cost, cooldown, and charge time. Useful for players who can't see scroll lore after socketing. |
| **Grimoire inspect** | Sneak + right-click a grimoire to open a read-only info GUI showing socketed spells, their stats, and the active slot highlighted. Currently the only way to see socketed spells is the scoreboard, which is limited. |
| **Insufficient mana feedback** | When chanting fails at completion due to insufficient mana, play a distinct "fizzle" sound and show `&c&lNot enough mana!` as a title for 1 second instead of just a chat message. |
| **Charging cancel indicator** | When a chant is cancelled (damage, item switch, shift release), briefly flash the boss bar red before removing it so the player understands why it stopped. |
| **Configurable messages** | Move all player-facing strings (`&cYou need Paper!`, `&aScroll socketed!`, etc.) into `config.yml` under a `messages` section so server owners can translate or reword them. |
| **Tab completion improvements** | Add tab completion for `/mf reload` and `/mf info`. |

### v1.0.4 - Performance & Robustness

**Focus:** Reduce per-tick overhead and harden against edge cases on busy servers.

| Area | Change |
|------|--------|
| **Particle optimization** | Spells like `AquaTorrent` and `CombustionBlast` spawn many particles per tick. Add a configurable `particles.scale` (0.0&ndash;1.0) that multiplies particle counts. At 0.0, all particles are suppressed for low-end servers. |
| **Nearby entity query caching** | `TremorSmash`, `GaleForce`, `AquaTorrent`, and `CombustionBlast` all call `getNearbyEntities()` independently. For concurrent spell casts in the same area, this is redundant. Consider a shared per-tick entity cache scoped by chunk. |
| **Task cleanup** | Audit all `BukkitRunnable` instances across spells to ensure they call `cancel()` on every exit path. A leaked task that never cancels will run forever. Add a debug command `/mf tasks` that reports active task counts. |
| **Thread safety** | `ManaManager.manaMap`, `ChantingManager.states`, `ScoreboardManager.activeSlots`, and `MagicListener.cooldowns` are plain `HashMap`s accessed from async tasks (e.g., the mana regen runnable). Switch to `ConcurrentHashMap` to prevent corruption. |
| **World unloading** | `TableManager` stores `Location` objects that reference `World` objects. If a world is unloaded, `Bukkit.getWorld()` returns null and locations become stale. Add a `WorldUnloadEvent` listener that filters out tables in the unloaded world and restores them on `WorldLoadEvent`. |
| **Grimoire item stacking** | Grimoires and scrolls are stackable by default (same material). Add `ItemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)` and set max stack size to 1 via `ItemMeta` to prevent stacking exploits. |

### v1.0.5 - Final Pre-2.0 Polish

**Focus:** Last polish pass and preparation for the 2.0.0 data model migration.

| Area | Change |
|------|--------|
| **JUnit test scaffolding** | Add `src/test/java` with initial unit tests for `Utils.colorize()`, `Utils.toRoman()`, `SpellRegistry.getById()`, and `GrimoireManager.getCapacity()`. Set up the Maven Surefire plugin in `pom.xml`. |
| **Logging** | Replace `e.printStackTrace()` calls in `TableManager` with `getLogger().severe()`. Add debug logging behind a `config.debug` flag for chant start/cancel, mana changes, and inscription events. |
| **Metrics** | Integrate bStats (or equivalent) for anonymous usage statistics: server count, spell cast frequency, element distribution. Fully opt-out via config. |
| **Update checker** | On enable, check the Spigot/Hangar resource page for a newer version and log + notify admins. Configurable via `config.check-updates`. |
| **Documentation** | Write a `README.md` with build instructions, feature overview, and a user-facing command/item reference. This also serves as onboarding for the 2.0.0 contributor team. |
| **Code cleanup** | Remove the duplicate inscription-table item creation logic between `GrimoireManager.createScrollInscriptionTable()` and the inline code in `PluginManager.registerRecipes()`. Consolidate into one factory method. |

---

## Version 2.0.x - Progression & Persistence

The 2.0.0 release introduces the **Mage Progression System**&mdash;a persistent
player data layer that tracks spell unlocks, mana capacity upgrades, casting
stats, and achievements. This is the foundation that makes MagicForce a long-term
RPG experience rather than a sandbox spell toy. All player data is stored per-UUID
and survives restarts, world changes, and plugin reloads.

### v2.0.0 - Mage Progression System (Major Release)

**Focus:** Persistent player data, leveling, and spell unlocking.

#### New Systems

| System | Description |
|--------|-------------|
| **MageLevel** | Players earn **Mage XP** by casting spells, inscribing scrolls, and completing chant sequences. Mage XP fills a level bar (1&ndash;50). Each level grants 1 **Skill Point**. |
| **SkillTree** | A GUI-based skill tree (`/mf skills`) where players spend skill points on: Mana Capacity (+10 per node, max +200), Mana Regen (+0.5/sec per node), Cast Speed (-2% charge time per node), Spell Power (+5% damage/healing per node), Cooldown Reduction (-3% per node). Each element has its own subtree. |
| **Spell Unlocking** | Spells are no longer all available at inscription tables. Tier 1 spells unlock at Mage Level 1, Tier 2 at Level 15, Tier 3 at Level 30. Players must also spend skill points in the corresponding element subtree to unlock advanced spells. |
| **PlayerDataStore** | New `PlayerDataManager` that persists to `playerdata/<UUID>.yml`. Stores: mage level, mage XP, skill point allocations, unlocked spells, total casts per spell, mana (replaces the v1.x `mana.yml`), cooldowns (replaces the v1.x `cooldowns.yml`). Consolidates all per-player persistence into one file. |
| **Casting Stats** | Track per-spell statistics: total casts, total damage dealt, total healing done. Displayed in `/mf stats`. |

#### Changes to Existing Systems

| Area | Change |
|------|--------|
| **ManaManager** | `MAX_MANA` becomes `100 + skillBonus`. Regen rate becomes `base + skillBonus`. All values now read from `PlayerDataManager` instead of a flat `HashMap`. |
| **InscriptionManager** | Tier selection UI only shows unlocked spells. Locked spells appear as grayed-out items with a "Requires Mage Level X" tooltip. |
| **ChantingManager** | Charge time is multiplied by `(1 - castSpeedBonus)`. Mana cost check reads from `PlayerDataManager`. |
| **AdminCommand** | Add `/mf level set <player> <level>`, `/mf level add <player> <amount>`, `/mf skill reset <player>`, `/mf unlock <spell> <player>`. |
| **ScoreboardManager** | Sidebar now shows Mage Level and XP bar at the top, followed by socketed spells. |
| **plugin.yml** | Add `magicforce.player` permission (default: true) for basic spell usage. Add `magicforce.admin` sub-permissions: `magicforce.admin.give`, `magicforce.admin.level`, `magicforce.admin.reload`. |

#### Migration

- On first run with v2.0.0, the plugin detects v1.x `mana.yml` and `cooldowns.yml` files, imports them into `playerdata/<UUID>.yml`, and renames the old files to `.bak`.
- All existing spells remain unlocked for players who had them before migration (tracked via a `legacy_unlocked` flag).
- Default config grants all spells at level 1 for servers that want the v1.0 experience (config: `progression.unlock-all-at-start: true`).

### v2.0.1 - Progression Bug Fixes

| Area | Change |
|------|--------|
| **XP dupe fix** | Prevent earning Mage XP from raw scroll casts in creative mode. |
| **Skill point refund** | Add `/mf skill refund <player> <node>` to undo a skill point allocation without resetting the entire tree. |
| **Async save** | `PlayerDataManager` saves synchronously on quit, causing lag spikes on busy servers. Move file I/O to an async task with a bounded write queue. |
| **Level cap edge case** | At max level (50), XP bar should display "MAX" instead of overflowing. |
| **Concurrent data access** | `PlayerDataManager` is accessed from the main thread (event handlers) and async tasks (save queue). Add a `ReadWriteLock` per player to prevent read-during-write corruption. |

### v2.0.2 - Achievement System

**Focus:** Add an achievement/milestone system that rewards players for diverse spell usage.

| Achievement | Condition | Reward |
|-------------|-----------|--------|
| **First Spark** | Cast your first spell | 5 Mage XP |
| **Elementalist** | Cast at least one spell from all 4 elements | 50 Mage XP + 2 Skill Points |
| **Master of Fire** | Cast 500 Fire spells | 1 Skill Point + Fire damage +5% |
| **Healer's Hand** | Heal 1,000 total HP with Cleansing Tide | 1 Skill Point + "Healer" title |
| **Wall Builder** | Cast EarthenRampart 100 times | 1 Skill Point |
| **Combo Caster** | Cast 3 different spells within 10 seconds | 20 Mage XP |
| **Scholar** | Inscribe 50 scrolls | 1 Skill Point |
| **Survivor** | Cancel a chant by taking damage 25 times | 10 Mage XP + "Resilient" title |
| **Maxed Out** | Reach Mage Level 50 | 5 Skill Points + "Archmage" title |

- Achievements are stored in `playerdata/<UUID>.yml` under `achievements:`.
- `/mf achievements` opens a GUI showing all achievements, their progress, and rewards.
- Titles are displayable in chat via a configurable prefix.

### v2.0.3 - Grimoire Enhancements

**Focus:** Deepen the grimoire system with upgrade paths and visual customization.

| Area | Change |
|------|--------|
| **Grimoire upgrading** | Add a "Grimoire Ascension" ritual: place a Tier 1 grimoire in a crafting grid with specific materials (netherite, diamond, etc.) to upgrade it to Tier 2, preserving socketed scrolls. Tier 2 → Tier 3 similarly. |
| **Grimoire capacity runes** | Rare drop items ("Capacity Runes") that can be applied to a grimoire via sneak + right-click to add +1 slot (max +2 over base capacity). |
| **Grimoire visual tiers** | Each grimoire tier gets a distinct item model/color: Tier 1 = plain book (gold trim), Tier 2 = enchanted book appearance (cyan trim), Tier 3 = glowing book (purple trim, animated enchantment glint). |
| **Active spell indicator** | The selected scroll in the grimoire's socketing GUI gets a glowing border (enchanted book appearance) so players can see which spell is active without checking the scoreboard. |
| **Grimoire binding** | Optional config: `grimoire.soulbound: true` makes grimoires un-droppable on death and un-tradeable between players. Prevents high-tier grimoire economy flooding. |

### v2.0.4 - Balancing & Tuning Pass

**Focus:** Data-driven balancing based on 2.0.x playtest feedback.

| Area | Change |
|------|--------|
| **Spell damage spreadsheet** | Create a `balance.md` document tracking every spell's DPS (damage / (charge time + cooldown)). Adjust outliers so no spell is strictly dominant. |
| **Mana economy** | Audit total mana expenditure vs. regen across a 5-minute combat scenario. Adjust regen rates or spell costs if players can spam indefinitely or are always starved. |
| **Tier scaling curves** | Currently tier scaling is linear. Consider exponential or logarithmic curves for damage/healing so Tier 3 feels meaningfully stronger than Tier 2. Make the curve configurable: `balance.tier-curve: linear|exponential|logarithmic`. |
| **PvP damage scaling** | Add `balance.pvp-damage-multiplier` (default 0.5) so spells don't one-shot players in full armor. PvE damage is unaffected. |
| **Cooldown floor** | Add `balance.min-cooldown` (default 0.5s) to prevent cooldown reduction skills from making spells spammable. |

---

## Version 3.0.x - Content Expansion

The 3.0.0 release doubles the spell roster and introduces **two new elements**,
new spell archetypes (buffs, debuffs, mobility, summons), and a **spell
augmentation** system. This is the largest content drop in the roadmap, targeting
players who have progressed through the 2.0.x skill tree and want more build
variety.

### v3.0.0 - New Elements & Spell Archetypes (Major Release)

#### New Elements

| Element | Color | Theme | New Spells |
|---------|-------|-------|------------|
| **LIGHTNING** | `ChatColor.YELLOW` | Fast, precise, chain damage | Storm Lance (piercing projectile), Thunderclap (AoE stun), Galvanic Surge (chain lightning between entities), Static Field (persistent damage zone) |
| **VOID** | `ChatColor.DARK_PURPLE` | Risk/reward, reality distortion | Null Bolt (projectile, ignores armor but costs HP), Rift Walk (short-range teleport), Gravity Well (pulls entities inward), Entropy (DoT that amplifies other damage) |

- `SpellElement` enum gains `LIGHTNING` and `VOID` with corresponding colors, display names, chanting sounds, and boss bar colors.
- `ChantingManager.elementToBarColor()` adds `LIGHTNING -> BarColor.YELLOW`, `VOID -> BarColor.PURPLE`.
- `Spell.getChantingStartSound()`, `getChantingLoopSound()`, `getChantingReadySound()`, `getChantingCancelSound()` add cases for both new elements.
- New chanting particle patterns: Lightning = electric sparks (`ELECTRIC_SPARK`, `FLASH`), Void = swirling dark particles (`WITCH`, `REVERSE_PORTAL`, `SQUID_INK`).

#### New Spell Archetypes

| Archetype | Description | Example Spells |
|-----------|-------------|----------------|
| **Buffs** | Self or ally enhancements with durations. | Stone Skin (Earth, +armor), Swiftness (Wind, +speed), Mana Shield (Water, absorbs damage using mana) |
| **Debuffs** | Applied to enemies via projectile or proximity. | Weakness Curse (Void, -25% damage), Frigid Touch (Water, freeze), Cripple (Earth, slowness+jump prevention) |
| **Mobility** | Movement spells for traversal and repositioning. | Wind Walk (Wind, dash forward), Phase Step (Void, short blink), Updraft (Wind, launch upward) |
| **Summons** | Temporary allied entities. | Guardian Golem (Earth, 30s summon that taunts mobs), Spirit Wolf (Lightning, 20s summon that attacks), Phoenix (Fire, 15s summon that heals allies in range) |
| **Persistent Zones** | Ground-placed area effects. | Lava Pool (Fire, 10s damage zone), Healing Spring (Water, 10s regen zone), Storm Cloud (Lightning, 10s random strikes) |
| **Channeled** | Continuous-cast spells that require holding shift and drain mana per tick. | Flamethrower (Fire, cone of fire), Arcane Beam (Void, sustained laser) |

- The `Spell` abstract class gains an optional `onChannelTick(Player, int tick)` method (default no-op) for channeled spells. `ChantingManager` is extended to support a "channeling" state distinct from "charging" and "ready."
- Summon spells use a new `SummonManager` that tracks active summons per player, limits to 1 active summon, and cleans up on player quit/death/zone-change.
- Buff/debuff spells use a new `EffectManager` that applies timed custom effects (beyond vanilla `PotionEffect`) and tracks their expiry per player.

#### Spell Augmentation System

| Feature | Description |
|---------|-------------|
| **Augment Gems** | New rare items dropped by mobs (configurable drop table). Each gem has an element and a modifier type: **Potency** (+damage/healing), **Efficiency** (-mana cost), **Celerity** (-charge time), **Endurance** (-cooldown), **Reach** (+range/radius). |
| **Socketing augments** | Scrolls gain a second PDC key `scroll_augment`. Sneak + right-click a scroll with an augment gem in the off-hand to apply it. Each scroll holds 1 augment (2 at Tier 3). |
| **Grimoire augmentation** | Grimoires gain an augment slot per tier (1/2/3 slots). Grimoire augments apply to ALL socketed spells: e.g., a "Potency" gem in a grimoire boosts every spell's damage by 10%. |
| **Augment extraction** | Augments can be removed via the inscription table at an XP cost, returning the gem. Prevents permanently "wasting" a gem on a low-tier scroll. |
| **Config-driven** | All augment values, drop rates, and stack limits are in `config.yml` under `augments:`. |

#### Changes to Existing Systems

| Area | Change |
|------|--------|
| **SpellRegistry** | Registration loop now iterates a config-defined list of spell class names, allowing servers to add/remove spells without code changes. |
| **SkillTree** | Two new element subtrees (Lightning, Void). Existing subtree nodes rebalanced for 6 elements. Max skill points increased to 75 (from 50). |
| **ScoreboardManager** | Sidebar now paginates if socketed spells exceed capacity (handles grimoires with +2 capacity runes). |
| **InscriptionManager** | Tier selection GUI expands to 36-slot (or paginated) layout to accommodate 14+ spells per tier. |

### v3.0.1 - New Element Bug Fixes

| Area | Change |
|------|--------|
| **Void HP cost** | `Null Bolt` HP cost can kill the caster if they have exactly the HP cost. Add a floor: spell cannot be cast below 2 HP. |
| **Lightning chain** | `Galvanic Surge` chain lightning can hit the caster if they're within range of the second bounce. Exclude the caster from all bounces. |
| **Summon cleanup** | Summons that persist across chunk unloads cause entity leaks. Register summons as `PersistentDataType` on the entity and clean up via `ChunkUnloadEvent`. |
| **Channeling cancel** | Channeled spells don't cancel when the player switches hotbar slots. Add `PlayerItemHeldEvent` cancellation for channeling state. |
| **Augment gem stacking** | Augment gems stack in inventory, but applying one from a stack consumes the entire stack. Fix to consume exactly 1. |

### v3.0.2 - Rituals & Crafting Expansion

**Focus:** Add depth to the crafting and inscription systems with multi-step rituals.

| Area | Change |
|------|--------|
| **Ritual Altar** | A new multi-block structure (3x3 obsidian base + enchanting table center) that enables advanced crafting: combining scrolls, transferring augments, and creating grimoires with guaranteed augment slots. |
| **Scroll fusion** | Two scrolls of the same spell and tier can be fused at a ritual altar to create a +1 "enhanced" variant (e.g., Ignition Dart I+ → Ignition Dart I Enhanced, +10% damage). Max enhancement level: +5. |
| **Augment transmutation** | Convert 3 low-tier augment gems into 1 higher-tier gem at the ritual altar. Tiers: Crude → Polished → Flawless → Perfect. Each tier doubles the augment's effect. |
| **Custom recipes** | Move all shaped recipes from `PluginManager.registerRecipes()` into `config.yml` so server owners can customize crafting grids and ingredients. |
| **Recipe discovery** | Players can find "recipe scrolls" in loot chests that teach them new crafting recipes (e.g., the ritual altar blueprint). Displayed in a `/mf recipes` GUI. |

### v3.0.3 - Spell Combos & Synergies

**Focus:** Reward players for using multiple elements strategically.

| Mechanic | Description |
|----------|-------------|
| **Elemental reactions** | Casting certain element pairs within 3 seconds triggers a bonus effect: Fire + Water = **Steam Burst** (AoE blindness), Fire + Earth = **Magma** (ground leaves fire), Water + Lightning = **Electrified** (chain stun), Wind + Fire = **Wildfire** (spread fire), Earth + Lightning = **Magnetize** (pull metallic mobs). |
| **Combo counter** | A UI element (boss bar or action bar) tracks consecutive unique-element casts. At 3x combo: +10% spell power. At 4x: +20%. Resets if the same element is cast twice in a row or 5 seconds pass. |
| **Spell weaving** | High-level skill tree node: casting a buff spell then immediately an attack spell within 2 seconds applies the buff's element as additional damage (e.g., Stone Skin + Ignition Dart = fire damage bonus). |
| **Reaction config** | All reactions are defined in `config.yml` under `reactions:` with element pairs, effects, durations, and multipliers. Server owners can add custom reactions. |

### v3.0.4 - Content Polish & Balancing

| Area | Change |
|------|--------|
| **Spell roster audit** | With 6 elements × 2+ spells × 3 tiers, there are now 36+ spell variants. Audit each for uniqueness—no two spells should fill the same niche. |
| **Augment rebalancing** | Playtest data from 3.0.0–3.0.3 may show certain augment combinations are overpowered. Add per-augment-type stack limits (e.g., max 2 Potency augments per grimoire). |
| **Channeled spell mana drain** | Channeled spells drain too slowly or too quickly depending on gear. Add a `channel_mana_per_tick` config that scales with spell tier. |
| **Summon AI improvements** | Spirit Wolf and Guardian Golem pathfinding is rudimentary. Add target-priority logic (aggro nearest hostile, protect caster, return to caster if >15 blocks away). |
| **Void risk tuning** | Void spells' HP costs may be too punishing or too lenient. Add `balance.void-hp-cost-multiplier` (default 1.0) for server-wide tuning. |
| **Particle count review** | With 6 elements and new archetypes, particle counts per tick can spike. Re-tune `particles.scale` defaults and add per-archetype particle multipliers. |

---

## Version 4.0.x - World & Multiplayer

The 4.0.0 release takes MagicForce out of the player's hands and into the **world**:
custom boss encounters, dungeon structures, multiplayer party mechanics, and
PvP arena support. This transforms MagicForce from a single-player spell mod
into a server-wide magical RPG.

### v4.0.0 - Bosses, Dungeons & Parties (Major Release)

#### Boss System

| Boss | Element | HP | Mechanics | Drops |
|------|---------|----|-----------| -------|
| **Inferno Warden** | Fire | 500 | Phases: 100-66% melee + fireballs, 66-33% summons flame adds + lava pools, 33-0% enrage (2x cast speed, full arena fire). Immune to Fire spells. | Flawless Fire Augment, Tier 3 Fire grimoire, "Warden Slayer" title |
| **Tideweaver Leviathan** | Water | 750 | Underwater arena. Phases: trident throws, whirlpool pull, tidal wave AoE. Heals in water. Immune to Water spells. | Flawless Water Augment, Capacity Rune, "Drowned Champion" title |
| **Titan of Stone** | Earth | 1000 | Slow but devastating ground slams. Summons stone golems. EarthenRampart walls block exits. Immune to Earth spells. | Flawless Earth Augment, Grimoire Ascension catalyst, "Titan Breaker" title |
| **Storm Sovereign** | Lightning | 600 | Fast, erratic movement. Chain lightning, thunderclap stun, summons lightning elementals. Immune to Lightning spells. | Flawless Lightning Augment, Perfect Augment gem (random), "Storm Tamer" title |
| **Void Harbinger** | Void | 888 | Teleports, creates gravity wells, reverses gravity (players float), applies Entropy DoT. Immune to Void spells. | Flawless Void Augment, unique "Harbinger's" cosmetic grimoire, "Reality's End" title |

- Bosses are spawned via a **Boss Altar** (similar to ritual altar but with a boss-specific catalyst item).
- Bosses have custom AI via a `BossManager` that extends entity goals, manages phase transitions, and syncs health bars via BossBar.
- Boss encounters are instanced: each player party gets their own arena (see Parties below) so no interference.
- Bosses scale HP and damage with party size: `baseHP * (1 + 0.25 * (partySize - 1))`.

#### Dungeon System

| Feature | Description |
|---------|-------------|
| **Dungeon generation** | Procedurally generated dungeon structures spawned via `/mf dungeon create <type>` or found naturally in specific biomes. Dungeon types: Fire (Nether fortresses), Water (ocean monuments), Earth (mineshafts), Lightning (sky islands), Void (end cities). |
| **Dungeon rooms** | Each dungeon has 5&ndash;10 rooms with increasing difficulty. Rooms contain mob spawners (element-themed), puzzle traps, and loot chests. Final room contains the boss. |
| **Loot tables** | Custom loot tables per dungeon type: augment gems, capacity runes, spell scrolls (including dungeon-exclusive spells), grimoire upgrade materials, and cosmetic items. |
| **Dungeon cooldown** | Each player can enter a dungeon once per `config.dungeon.cooldown` (default 2 hours). Party members share the cooldown. |
| **Dungeon persistence** | Active dungeons are saved to `dungeons.yml` so they survive restarts. Abandoned dungeons (no players for 10 min) are cleaned up. |
| **WorldGuard integration** | Dungeons automatically create WorldGuard regions to prevent block breaking/placing and mob griefing. Falls back to manual block protection if WorldGuard is absent. |

#### Party System

| Feature | Description |
|---------|-------------|
| **Party creation** | `/mf party create`, `/mf party invite <player>`, `/mf party accept/deny`, `/mf party leave`, `/mf party disband`. Max party size: 6 (configurable). |
| **Party HUD** | A sidebar (or custom scoreboard) showing party members' health, mana, and active buffs. Visible only to party members. |
| **Shared XP** | Mage XP from spell casts is split: 100% to caster, 25% to each party member within 30 blocks. Encourages grouping. |
| **Party buffs** | Certain buff spells (Stone Skin, Mana Shield) can be cast on party members by sneak + right-clicking them. Range: 8 blocks. |
| **Party dungeon lock** | Only party members can enter a dungeon instance. Non-party players are blocked at the entrance. |
| **Friendly fire config** | `config.pvp.party-friendly-fire: false` prevents spell damage between party members (except in designated PvP arenas). |
| **Party chat** | `/mf party chat` toggles a party-only chat channel. Messages prefixed with `[Party]` in a configurable color. |

#### PvP Arena System

| Feature | Description |
|---------|-------------|
| **Arena creation** | `/mf arena create <name>` defines a region (pos1/pos2 selection via blaze rod). `/mf arena setspawn <name> <team>` sets spawn points. Supports 2&ndash;4 teams. |
| **Arena modes** | Free-for-all, Team Deathmatch, Spell Duel (1v1), Capture the Flag. Configurable score limit and time limit. |
| **Spectator mode** | Eliminated players become spectators with flight and invisibility. Can't interact with the arena. |
| **Arena stats** | Track kills, deaths, spell usage per arena session. Display MVP at match end. |
| **Elo ranking** | Optional 1v1 Elo system for Spell Duel mode. `/mf arena leaderboard` shows top players. |

#### Changes to Existing Systems

| Area | Change |
|------|--------|
| **ChantingManager** | Channeling state (from 3.0.0) now supports movement: channeled spells slow the player by 50% instead of rooting. |
| **ManaManager** | Mana regen pauses during PvP arena matches (configurable). Instead, mana is set to full at match start. |
| **EffectManager** | Debuffs now check for PvP immunity (`config.pvp.debuff-immunity-duration` after spawning) to prevent spawn-camping. |
| **PlayerDataManager** | Stores dungeon cooldowns, arena Elo, party membership, and boss kill counts. |
| **plugin.yml** | New commands: `party`, `dungeon`, `arena`. New permissions: `magicforce.party`, `magicforce.dungeon`, `magicforce.arena`, `magicforce.arena.admin`. |

### v4.0.1 - Boss & Dungeon Bug Fixes

| Area | Change |
|------|--------|
| **Boss phase transition** | Inferno Warden can get stuck between phases if damage is dealt in a single tick (one-shot). Add a minimum phase duration of 3 seconds. |
| **Dungeon cleanup** | Dungeons abandoned by party disconnect don't clean up if the party leader's `PlayerQuitEvent` is missed. Add a periodic cleanup task (every 5 min) that checks for empty dungeons. |
| **Party invite expiry** | Party invites never expire. Add a 60-second timeout; after which the invite is auto-denied. |
| **Arena region overlap** | Two arenas can overlap if their regions intersect. Add overlap detection on `arena create` and reject. |
| **Boss bar leak** | Boss health bars are not removed if the boss is killed by environmental damage (fall, void). Add a `EntityDeathEvent` listener that cleans up boss bars regardless of damage source. |
| **Instance isolation** | Players in different dungeon instances can see each other's particles and hear sounds. Add per-instance particle/sound filtering by checking if the viewer is in the same instance. |

### v4.0.2 - World Events

**Focus:** Server-wide events that bring the community together.

| Event | Trigger | Mechanics | Rewards |
|-------|---------|-----------|---------|
| **Elemental Surge** | Random (every 2&ndash;6 hours, configurable) | A random element "surges" for 10 minutes. All spells of that element get +50% power but +50% mana cost. Element-themed mobs spawn in the overworld. | Double Mage XP during surge, surge-themed cosmetic |
| **Rift Invasion** | Admin-triggered or scheduled | Void rifts open at random locations. Waves of void-themed mobs pour out. Players must close rifts by casting Void spells near them. | Flawless Void Augment, unique rift-themed title |
| **Boss Rush** | Weekly scheduled event | All 5 bosses spawn simultaneously in a mega-arena. Parties fight them in sequence. 30-minute time limit. | Perfect Augment gem (guaranteed), "Boss Rush Champion" title |
| **Spell Tournament** | Admin-triggered | Bracket-style 1v1 Spell Duel tournament. Single elimination. | Tournament winner gets a unique cosmetic grimoire + "Grand Champion" title |
| **Mana Storm** | Random (rare, 1% chance per hour) | For 5 minutes, all mana costs are 0 and regen is 10x. Chaos ensues. | "Storm Rider" title for casting 10+ spells during the storm |

- Events are announced via configurable broadcast messages and boss bar countdowns.
- Event schedule is configurable in `config.yml` under `events:`.
- `/mf event start <type>` and `/mf event stop` for admin control.

### v4.0.3 - Economy & Trading

**Focus:** Integrate MagicForce items into server economies.

| Area | Change |
|---------|--------|
| **Vault integration** | Hook into Vault (optional dependency) for economy support. Grimoires, scrolls, and augment gems can be bought/sold via server shops. |
| **Magic Market** | A dedicated GUI shop (`/mf market`) where players list MagicForce items for sale. Other players browse and buy with Vault currency. 5% server tax on transactions (configurable). |
| **Scroll trading** | Scrolls are tradeable between players (unless `grimoire.soulbound` is enabled). Trade via the market or direct `/mf trade <player>`. |
| **Augment gem appraisal** | Unidentified augment gems (new drop type) must be appraised at an inscription table (XP cost) to reveal their type and tier. Adds an economy sink for XP. |
| **Quest rewards** | NPC quest givers (or command-based quests) reward Vault currency + MagicForce items. Simple quest framework: kill X mobs, cast Y spells, inscribe Z scrolls. |
| **Bounty system** | Players can place bounties (Vault currency) on other players. Bounty hunter must defeat the target in a Spell Duel to claim. |

### v4.0.4 - Social & Cosmetics

**Focus:** Non-gameplay content that enhances player expression and community.

| Area | Change |
|---------|--------|
| **Grimoire skins** | Cosmetic-only grimoire textures applied via `/mf skin apply <skin> <grimoire>`. Skins earned from events, bosses, or achievements. Do not affect stats. |
| **Spell trail effects** | Cosmetic particle replacements for spell trails. E.g., Ignition Dart's default flame trail can be swapped for a "Frostfire" blue-flame skin. Earned or purchased. |
| **Cast sounds** | Custom casting sound packs. E.g., "Arcane" (mystical chimes), "Primal" (drums), "Tech" (electronic). |
| **Titles & prefixes** | Expand the title system from 2.0.2. Titles earned from achievements, bosses, events, and tournaments. Displayed as chat prefixes: `[Archmage] PlayerName:`. |
| **Leaderboards** | `/mf leaderboard <stat>` for: total spell casts, boss kills, arena wins, Mage level, achievement count. Top 10 displayed in chat; full list in a GUI. |
| **Discord webhook** | Optional: send boss kill announcements, event starts, and tournament results to a Discord channel via webhook. Configured in `config.yml` under `discord:`. |

---

## Version 5.0.x - API & Ecosystem

The 5.0.0 release opens MagicForce to **third-party developers**. A stable public
API allows other plugins to register custom spells, elements, augment types,
bosses, and dungeons. This positions MagicForce as a **platform** rather than a
standalone plugin, enabling a community addon ecosystem.

### v5.0.0 - Public Developer API (Major Release)

#### API Architecture

| Package | Purpose |
|---------|---------|
| `com.perseusj.magicforce.api` | Public API interfaces and classes. Guaranteed stable across minor versions. |
| `com.perseusj.magicforce.api.events` | Custom Bukkit events: `SpellCastEvent`, `SpellCastPreEvent` (cancellable), `ChargingStartEvent`, `ChargingCompleteEvent`, `ManaChangeEvent`, `MageLevelUpEvent`, `BossDeathEvent`, `DungeonCompleteEvent`. |
| `com.perseusj.magicforce.api.spells` | `CustomSpell` interface (extends `Spell`) for third-party spell registration. `SpellBuilder` fluent API for easy spell creation without subclassing. |
| `com.perseusj.magicforce.api.elements` | `CustomElement` interface for registering new elements beyond the built-in 6. Includes color, sounds, particles, and boss bar color. |
| `com.perseusj.magicforce.api.augments` | `AugmentType` registry for custom augment modifiers. |
| `com.perseusj.magicforce.api.bosses` | `CustomBoss` interface for registering custom boss encounters. |
| `com.perseusj.magicforce.api.dungeons` | `CustomDungeon` interface for registering custom dungeon types and room generators. |
| `com.perseusj.magicforce.api.player` | `Mage` interface wrapping a player's MagicForce data (level, XP, mana, skills, unlocks). Read and write access with permission checks. |

#### Key API Methods

```java
// Spell registration
MagicForceAPI.registerSpell(CustomSpell spell);
MagicForceAPI.unregisterSpell(String spellId);
MagicForceAPI.getSpellRegistry();

// Element registration
MagicForceAPI.registerElement(CustomElement element);

// Player data access
Mage mage = MagicForceAPI.getMage(player);
mage.getMana();
mage.setMana(double amount);
mage.getLevel();
mage.addXP(double amount);
mage.unlockSpell(String spellId);
mage.hasUnlocked(String spellId);

// Event listening (standard Bukkit)
@EventHandler
public void onSpellCast(SpellCastEvent event) {
    Player caster = event.getCaster();
    Spell spell = event.getSpell();
    double damage = event.getDamage();
    event.setDamage(damage * 1.1); // 10% bonus
    event.setCancelled(true); // block the cast
}

// Augment registration
MagicForceAPI.registerAugmentType(AugmentType type);

// Boss registration
MagicForceAPI.registerBoss(CustomBoss boss);
```

#### Addon System

| Feature | Description |
|---------|-------------|
| **Addon detection** | MagicForce scans `plugins/MagicForce/addons/` for `.jar` files on enable. Each addon's `plugin.yml` declares a `magicforce-addon: true` flag. |
| **Addon lifecycle** | Addons are loaded AFTER MagicForce's core systems initialize. Addons receive an `AddonInitializeEvent` where they register their custom content. Addons are unloaded on MagicForce disable. |
| **Addon dependency** | Addons can depend on other addons via `depend: [OtherAddon]` in their `plugin.yml`. |
| **Addon config namespace** | Each addon gets its own config section: `addons.<addon_name>:` in `config.yml`. MagicForce auto-generates default configs from addon-provided defaults. |
| **Addon sandboxing** | Addons run in the same classloader as MagicForce (no true sandboxing), but the API is designed to discourage direct access to internal classes. Internal packages are marked `@ApiStatus.Internal`. |

#### PlaceholderAPI Integration

| Placeholder | Description |
|-------------|-------------|
| `%magicforce_level%` | Player's Mage level |
| `%magicforce_xp%` | Current Mage XP (toward next level) |
| `%magicforce_xp_total%` | Total Mage XP earned |
| `%magicforce_mana%` | Current mana |
| `%magicforce_mana_max%` | Max mana |
| `%magicforce_mana_percent%` | Mana as percentage |
| `%magicforce_active_spell%` | Currently selected spell name |
| `%magicforce_active_element%` | Currently selected spell's element |
| `%magicforce_casts_total%` | Total spell casts |
| `%magicforce_casts_<spellId>%` | Casts of a specific spell |
| `%magicforce_boss_kills%` | Total boss kills |
| `%magicforce_arena_elo%` | Arena Elo rating |
| `%magicforce_title%` | Active title |
| `%magicforce_party_leader%` | Party leader name (or "None") |
| `%magicforce_party_size%` | Current party size |

#### Integration Hooks

| Integration | Description |
|-------------|-------------|
| **PlaceholderAPI** | All placeholders listed above. Soft-dependency. |
| **Vault** | Economy (from 4.0.3) + permission hooks. Soft-dependency. |
| **WorldGuard** | Region protection for dungeons and arenas (from 4.0.0). Soft-dependency. |
| **MythicMobs** | Allow MythicMobs entities to be used as boss bases and dungeon mobs. Soft-dependency. |
| **Citizens/NPCs** | NPC quest givers (from 4.0.3) can use Citizens NPCs for better compatibility. Soft-dependency. |
| **Towny** | Respect Towny town claims when placing inscription tables and ritual altars. Soft-dependency. |
| **mcMMO** | Cross-plugin XP bonuses (mcMMO level boosts Mage XP gain). Soft-dependency. |
| **DiscordSRV** | Replace the v4.0.4 webhook with native DiscordSRV integration for richer Discord messages. Soft-dependency. |

#### Refactoring for API Stability

| Area | Change |
|------|--------|
| **Package restructure** | Move all internal implementation to `com.perseusj.magicforce.internal`. Only `api` packages are public. Existing `spells`, `managers`, `listeners`, `commands`, `utils` packages move under `internal`. |
| **Singleton access** | Replace direct `ManaManager.getInstance()` calls in API consumers with `MagicForceAPI.getMage(player).getMana()`. Internal singletons remain but are not part of the public API. |
| **Event-driven architecture** | Internal systems that currently call each other directly (e.g., `ChantingManager` calls `ManaManager.removeMana()`) now fire events that are handled internally. This allows addons to intercept and modify behavior. |
| **Semantic versioning** | Adopt strict SemVer. API-breaking changes only in major versions (6.0.0+). Addon compatibility is checked on load: addon declares `api-version: 5.0` and MagicForce warns if the addon targets an incompatible API version. |

### v5.0.1 - API Bug Fixes & Documentation

| Area | Change |
|------|--------|
| **Event threading** | `SpellCastEvent` fires on the main thread, but addons doing heavy computation cause TPS drops. Document that event handlers must be non-blocking; add a `SpellCastAsyncEvent` for post-cast analytics. |
| **Addon classloader isolation** | Two addons bundling different versions of the same library conflict. Add a `AddonClassLoader` that isolates addon dependencies (similar to Bukkit's plugin classloader). |
| **API Javadoc** | Generate comprehensive Javadoc for all `api` package classes. Publish to a GitHub Pages site. |
| **Developer guide** | Write `DEVELOPER.md` with: API overview, addon creation tutorial, spell creation examples, event reference, and migration guide from v4.x direct-access patterns. |
| **Example addon** | Publish a `MagicForce-ExampleAddon` repository with 3 example spells, 1 custom element, 1 custom boss, and 1 event listener. Serves as a reference template. |
| **API version mismatch handling** | If an addon targets `api-version: 5.0` but MagicForce is 5.1, it should load fine (backward compatible). If it targets 5.2 but MagicForce is 5.1, warn and skip. Implement and test this logic. |

### v5.0.2 - Addon Marketplace & Discovery

**Focus:** Make it easy for server owners to find and install community addons.

| Area | Change |
|---------|--------|
| **Addon registry** | A community-maintained JSON registry (hosted on GitHub) listing available addons with metadata: name, description, author, download URL, API version, dependencies. |
| **In-plugin browser** | `/mf addons browse` opens a GUI listing addons from the registry. `/mf addons install <name>` downloads and installs the addon. `/mf addons update` checks for updates. |
| **Addon search** | Filter by element, spell type, or keyword. Show download counts and ratings (pulled from the registry). |
| **Addon verification** | Addons in the registry are optionally signed with a GPG key. MagicForce verifies signatures on install and warns for unsigned addons. |
| **Automatic updates** | Configurable `addons.auto-update: true` checks for updates on server start and installs them silently (signed addons only). |
| **Addon conflict detection** | If two addons register a spell with the same ID, MagicForce logs a conflict and disables the second addon rather than crashing. |

### v5.0.3 - Performance Profiling & Debug Tools

**Focus:** Give server admins and addon developers tools to diagnose performance issues.

| Tool | Description |
|------|-------------|
| **`/mf profile`** | Starts a 30-second profiling session that records: per-spell cast counts, per-tick particle counts, total active BukkitRunnable tasks, mana manager overhead, event handler timings (including addon handlers). Outputs a report to chat and a file. |
| **`/mf profile spells`** | Shows a breakdown of CPU time per spell class. Identifies which spells are most expensive. |
| **`/mf profile addons`** | Shows CPU time consumed by each addon's event handlers. Identifies poorly optimized addons. |
| **`/mf debug chant <player>`** | Live-updating view of a player's chanting state: current spell, charge progress, elapsed ticks, ready state, remaining hold window. |
| **`/mf debug mana <player>`** | Shows mana value, regen rate, and recent mana changes (last 10 events) with timestamps. |
| **Particle counter** | A debug overlay (via `/mf debug particles`) that displays total particles spawned per tick in the console. Helps identify particle-heavy spells or addons. |
| **Memory monitor** | `/mf debug memory` shows: total player data entries, total active boss bars, total active summons, total dungeon instances, total cooldown entries. Helps identify memory leaks. |
| **Benchmark mode** | `/mf benchmark <spell> <count>` casts a spell N times in rapid succession (ignoring cooldowns/mana) and reports average cast time. Useful for addon developers testing spell performance. |

### v5.0.4 - Long-Term Sustainability & Final Polish

**Focus:** Ensure MagicForce 5.x is maintainable, well-documented, and ready for
long-term community stewardship.

| Area | Change |
|---------|--------|
| **Code documentation** | 100% Javadoc coverage on all public API classes. Internal classes get package-level Javadoc explaining their role. |
| **CI/CD pipeline** | Set up GitHub Actions: compile on push, run unit tests, build artifact on tag, auto-publish to Hangar/SpigotMC on release. |
| **Test suite expansion** | Expand unit tests to cover: all spell registry operations, config loading/validation, player data save/load, augment application/removal, party operations, event firing/cancellation. Target: 70% line coverage on `api` and `internal.managers` packages. |
| **Localization framework** | Full i18n support. All messages move to `lang/en.yml` with locale variants (`lang/es.yml`, `lang/de.yml`, `lang/zh.yml`, etc.). Locale selected per-player via `/mf lang <locale>` or auto-detected from client locale. |
| **Config migration system** | Automatic config migration between versions. A `config_version` field in `config.yml` triggers migration scripts that transform old config formats to new ones without losing user settings. |
| **Deprecation cleanup** | Remove all `@Deprecated` methods from the 4.x era that have 5.x API replacements. Log a one-time warning for addons still using removed methods. |
| **Performance regression tests** | Automated benchmarks that run in CI: cast every spell 1000 times and compare timing against a baseline. Fails CI if any spell is >20% slower than baseline. |
| **Comprehensive wiki** | A player-facing wiki covering: getting started, every spell (with stats and GIFs), all items, crafting recipes, dungeon guides, boss strategies, skill tree planning, addon list, and FAQ. |
| **Modrinth/Hangar publication** | Publish MagicForce to Modrinth and Hangar with automated release pipelines. Include source jars and Javadoc jars. |

---

## Release Cadence Summary

| Version | Theme | Estimated Timeline | Key Deliverables |
|---------|-------|-------------------|------------------|
| **1.0.1** | Critical bug fixes | +1 week | Cooldown/mana persistence, dupe fixes, thread safety |
| **1.0.2** | Configuration system | +2 weeks | `config.yml`, `ConfigManager`, all values configurable |
| **1.0.3** | Quality of life | +2 weeks | Cooldown HUD, spell info, grimoire inspect, message config |
| **1.0.4** | Performance & robustness | +2 weeks | Particle scaling, thread safety, world unload handling |
| **1.0.5** | Pre-2.0 polish | +1 week | Tests, logging, metrics, update checker, README |
| **2.0.0** | Mage progression | +6 weeks | Leveling, skill tree, spell unlocking, PlayerDataStore |
| **2.0.1** | Progression fixes | +1 week | XP dupe, async save, skill refund, concurrency |
| **2.0.2** | Achievements | +2 weeks | 9 achievements, achievement GUI, titles |
| **2.0.3** | Grimoire enhancements | +3 weeks | Grimoire upgrading, capacity runes, visual tiers, soulbinding |
| **2.0.4** | Balancing pass | +2 weeks | DPS audit, mana economy, tier curves, PvP scaling |
| **3.0.0** | New elements & archetypes | +10 weeks | Lightning + Void, buffs/debuffs/mobility/summons/zones/channels, augment system |
| **3.0.1** | New element fixes | +1 week | Void HP floor, chain exclusion, summon cleanup, channel cancel |
| **3.0.2** | Rituals & crafting | +3 weeks | Ritual altar, scroll fusion, augment transmutation, config recipes |
| **3.0.3** | Combos & synergies | +3 weeks | Elemental reactions, combo counter, spell weaving |
| **3.0.4** | Content balancing | +2 weeks | Roster audit, augment limits, channel drain, summon AI, particle review |
| **4.0.0** | Bosses, dungeons, parties | +12 weeks | 5 bosses, 5 dungeon types, party system, PvP arenas |
| **4.0.1** | Boss & dungeon fixes | +2 weeks | Phase transitions, cleanup, invite expiry, region overlap, bar leak |
| **4.0.2** | World events | +3 weeks | Elemental Surge, Rift Invasion, Boss Rush, Spell Tournament, Mana Storm |
| **4.0.3** | Economy & trading | +3 weeks | Vault hook, Magic Market, scroll trading, gem appraisal, quests, bounties |
| **4.0.4** | Social & cosmetics | +3 weeks | Grimoire skins, spell trails, cast sounds, titles, leaderboards, Discord |
| **5.0.0** | Public developer API | +10 weeks | API packages, events, custom spell/element/boss/dungeon registration, addon system, PlaceholderAPI, integration hooks |
| **5.0.1** | API fixes & docs | +2 weeks | Async events, classloader isolation, Javadoc, dev guide, example addon |
| **5.0.2** | Addon marketplace | +3 weeks | Addon registry, in-plugin browser, auto-update, conflict detection |
| **5.0.3** | Profiling & debug tools | +2 weeks | `/mf profile`, spell/addon profiling, debug overlays, benchmark mode |
| **5.0.4** | Sustainability & polish | +4 weeks | Javadoc, CI/CD, test suite, i18n, config migration, wiki, publication |

**Total estimated timeline:** ~80 weeks (~18&ndash;20 months) from v1.0.0 to v5.0.4.

---

## Guiding Principles

1. **Config-driven first.** Every gameplay value should be in `config.yml` before it's hardcoded. No server owner should need to recompile to balance their server.
2. **Backward compatibility.** Minor versions (x.y.z where z changes) never break configs or player data. Major versions (x.0.0) provide migration scripts.
3. **Soft-dependency integration.** Every third-party plugin integration (Vault, WorldGuard, PlaceholderAPI, etc.) is optional. MagicForce runs standalone with graceful degradation.
4. **Performance is a feature.** No spell should cause measurable TPS drop on a 50-player server. Particle counts are always configurable.
5. **API stability from 5.0.0.** Once the public API ships, it's frozen until the next major version. Addons are first-class citizens.
6. **Community-driven content.** From 5.0.2 onward, the addon ecosystem is the primary content pipeline. The core team focuses on the API and balance, not new spells.
