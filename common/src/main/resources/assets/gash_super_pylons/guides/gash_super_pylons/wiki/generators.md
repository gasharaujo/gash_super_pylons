---
navigation:
  title: Generators
  icon: gash_super_pylons:cobblestone_generator_upgrade
  position: 0
  parent: upgrades.md
item_ids:
  - gash_super_pylons:cobblestone_generator_upgrade
  - gash_super_pylons:deepslate_generator_upgrade
  - gash_super_pylons:blackstone_generator_upgrade
  - gash_super_pylons:netherrack_generator_upgrade
  - gash_super_pylons:sand_generator_upgrade
  - gash_super_pylons:glass_generator_upgrade
  - gash_super_pylons:water_generator_upgrade
  - gash_super_pylons:lava_generator_upgrade
---

# Generators

A generator produces **one unit every 60 ticks** by default. [Speed](modifiers.md) shortens the interval and Quantity increases production per cycle. Each pylon has one generator slot, separate from utility and processing.

| Generator | Output |
| --- | --- |
| Cobblestone | Cobblestone |
| Deepslate | Deepslate |
| Blackstone | Blackstone |
| Netherrack | Netherrack |
| Sand | Sand |
| Glass | Glass |
| Water | 100 mB of water per unit |
| Lava | 100 mB of lava per unit |

An item generator pauses when the pylon is full or contains a different item. It can feed Smelt or Pulverize when a matching recipe exists.

Water and Lava come with a **32-bucket tank**, increased by Capacity. The tank can fill buckets and feed fluid automation. The fluid's texture shows in the core. Removing a generator with fluid asks for confirmation before discarding it. Breaking the pylon discards the fluid.
