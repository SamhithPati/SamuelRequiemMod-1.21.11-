# SamuelRequiemMod — Project Context

## Overview

**SamuelRequiemMod** is a Minecraft Fabric mod (version 1.21) inspired by the Requiem mod concept. Its core mechanic allows players to **possess and control Minecraft mobs** using a special item called the **Possession Relic**. When possessing a mob, the player temporarily becomes that entity — gaining its physical dimensions, stat modifiers, and unique abilities while retaining full player control.

---

## Goals and Purpose

- Implement a polished, Requiem-inspired possession system from scratch using the Fabric modding framework
- Support multiple mob types with individual behaviors, stat profiles, and controller logic
- Introduce custom world structures (Soul Trader Shrine) to integrate possession lore into world generation
- Keep the mod compatible with Minecraft 1.21 using Fabric Loader and Fabric API

---

## Technical Stack

| Layer | Tool/Version |
|---|---|
| Language | Java 21 |
| Framework | Fabric Loader 0.18.4 + Fabric API 0.102.0 |
| Minecraft Version | 1.21 |
| Mappings | Yarn (build.9) |
| Build System | Gradle with Fabric Loom |
| Bytecode Manipulation | Mixins (15+ mixin classes) |
| CI/CD | GitHub Actions (Ubuntu 24.04, Java 25) |
| License | CC0 1.0 Universal (public domain) |

---

## Core Systems

### Possession System
The heart of the mod. Manages state, transitions, attributes, and networking for mob possession.

| File | Role |
|---|---|
| `PossessionManager.java` | Central state tracker for all active possessions |
| `PossessionData.java` | Per-player possession state (mob type, saved health/hunger, etc.) |
| `PossessionProfile.java` | Physical stat profile (dimensions, health bonus, speed modifier) |
| `PossessionProfiles.java` | Registry of all mob profiles |
| `PossessionEffects.java` | Applies/clears attribute modifiers on possession start/end |
| `PossessionNetworking.java` | Server↔Client sync for possession state |
| `RelicPossessionHandler.java` | Triggers possession on relic right-click |
| `RelicUnpossessHandler.java` | Triggers unpossession on relic shift-click |

### Mob-Specific Controllers (9 total)
Each possessed mob type has a dedicated controller handling unique behaviors:

| Mob | Controller |
|---|---|
| Zombie | `zombie/ZombiePossessionController.java` |
| Baby Zombie | `zombie/BabyZombiePossessionController.java` |
| Husk | `husk/HuskPossessionController.java` |
| Baby Husk | `husk/BabyHuskPossessionController.java` |
| Drowned | `drowned/DrownedPossessionController.java` (water/trident mechanics) |
| Baby Drowned | `drowned/BabyDrownedPossessionController.java` |
| Pillager | `illager/PillagerPossessionController.java` |
| Vindicator | `illager/VindicatorPossessionController.java` |
| Evoker | `illager/EvokerPossessionController.java` (spell-casting) |

### Item System
- **Possession Relic** — right-click to possess a mob, shift-click to unpossess
- **Relic Shards** — crafting materials

### Entity System
- **CorruptedMerchantEntity** — custom Soul Trader NPC that spawns inside the shrine structure

### World Generation
- **Soul Trader Shrine** — procedurally placed structure containing the Soul Trader NPC
- Admin command `/shrine` for spawning it manually during development

### Mixin System (15+ mixins)
Bytecode patches to vanilla classes that enable:
- Player dimension/hunger overrides during possession
- Mob AI suppression for possessed entities
- Client-side rendering tweaks (possessed player visuals, animations)

---

## Gameplay Flow

1. Player obtains a Possession Relic (crafted from Relic Shards)
2. Player right-clicks a supported mob with the relic
3. Player's health/hunger state is saved; mob's attributes are applied
4. Player controls the mob — unique abilities activate based on mob type
5. Player shift-clicks the relic to unpossess and restore their original state
6. A 5-second immunity window prevents immediate mob re-aggro after unpossessing

---

## Project Structure

```
src/main/java/net/sam/samrequiemmod/
├── SamuelRequiemMod.java            # Server-side entry point
├── SamuelRequiemModClient.java      # Client-side entry point
├── item/                            # Items (Relic, Shards)
├── entity/                          # Custom entities + renderers
├── possession/                      # Core possession framework + controllers
│   ├── zombie/
│   ├── husk/
│   ├── drowned/
│   └── illager/
├── world/                           # World gen (shrine, structure commands)
├── mixin/                           # Bytecode patches
└── client/                          # Client state, HUD, animations

src/main/resources/
├── fabric.mod.json                  # Mod metadata
├── samrequiemmod.mixins.json        # Mixin config
├── assets/samrequiemmod/            # Models, textures, lang
└── data/samrequiemmod/              # Recipes, worldgen, structures
```

---

## Known Supported Mob Types (for reference)

- Zombie, Baby Zombie
- Husk, Baby Husk
- Drowned, Baby Drowned
- Pillager, Vindicator, Evoker

---

## Development Notes

- Always check both server (`SamuelRequiemMod.java`) and client (`SamuelRequiemModClient.java`) sides — networking is required for visual sync
- Mixins are in `samrequiemmod.mixins.json`; access widener is `samrequiemmod.accesswidener`
- Possession state lives in `PossessionManager` (server) and `ClientPossessionState` (client)
- Each new mob type needs: a `PossessionProfile`, a controller class, registration in `PossessionProfiles`, and potentially new mixins
