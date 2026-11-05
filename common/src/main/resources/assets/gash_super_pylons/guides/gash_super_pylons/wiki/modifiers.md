---
navigation:
  title: Modifiers
  icon: gash_super_pylons:speed_upgrade_1
  position: 1
  parent: upgrades.md
item_ids:
  - gash_super_pylons:speed_upgrade_1
  - gash_super_pylons:speed_upgrade_2
  - gash_super_pylons:speed_upgrade_3
  - gash_super_pylons:speed_upgrade_4
  - gash_super_pylons:speed_upgrade_5
  - gash_super_pylons:speed_upgrade_6
  - gash_super_pylons:quantity_upgrade_1
  - gash_super_pylons:quantity_upgrade_2
  - gash_super_pylons:quantity_upgrade_3
  - gash_super_pylons:quantity_upgrade_4
  - gash_super_pylons:quantity_upgrade_5
  - gash_super_pylons:quantity_upgrade_6
  - gash_super_pylons:range_upgrade_1
  - gash_super_pylons:range_upgrade_2
  - gash_super_pylons:range_upgrade_3
  - gash_super_pylons:range_upgrade_4
  - gash_super_pylons:range_upgrade_5
  - gash_super_pylons:range_upgrade_6
  - gash_super_pylons:storage_upgrade_1
  - gash_super_pylons:storage_upgrade_2
  - gash_super_pylons:storage_upgrade_3
  - gash_super_pylons:storage_upgrade_4
  - gash_super_pylons:storage_upgrade_5
  - gash_super_pylons:storage_upgrade_6
  - gash_super_pylons:energy_upgrade_1
  - gash_super_pylons:energy_upgrade_2
  - gash_super_pylons:energy_upgrade_3
  - gash_super_pylons:energy_upgrade_4
  - gash_super_pylons:energy_upgrade_5
  - gash_super_pylons:energy_upgrade_6
---

# Modifiers

- **Speed** sets the interval between operations. Fertilizer also gets more growth attempts and XP Tank transfers more levels per tick.
- **Quantity** sets the number of units per operation. Some machines use the tier directly, such as the minimum enchantments on Enchanter and targets on Super Harvester.
- **Range** expands the working area horizontally and vertically. On Dropper, each tier adds one block of reach. On Chunk Loader, it sets the radius in chunks.
- **Capacity** increases item, fluid and XP storage. Tier VI enables [bulk transfer](automation.md). Enchanter uses Capacity for enchanting power instead.
- **Energy** adds Forge Energy storage to any pylon. It can pull from the side opposite the working side, charge a compatible item on the pedestal and send FE to the working side.

| Tier | Speed (ticks) | Quantity | Range | Capacity |
| --- | --- | --- | --- | --- |
| 0 | 60 | 1 | 3×3×3 | 1× |
| I | 40 | 2 | 7×7×7 | 2× |
| II | 30 | 4 | 9×9×9 | 4× |
| III | 20 | 8 | 11×11×11 | 8× |
| IV | 10 | 16 | 13×13×13 | 16× |
| V | 5 | 32 | 15×15×15 | 32× |
| VI | 1 | 64 | 17×17×17 | Unlimited |

| Energy tier | Storage | FE per tick, per direction |
| --- | --- | --- |
| I | 1,000,000 FE | 1,000 |
| II | 10,000,000 FE | 10,000 |
| III | 40,000,000 FE | 40,000 |
| IV | 100,000,000 FE | 100,000 |
| V | 400,000,000 FE | 400,000 |
| VI | 2,000,000,000 FE | Unlimited |
