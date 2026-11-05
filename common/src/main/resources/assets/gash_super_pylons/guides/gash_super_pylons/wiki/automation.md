---
navigation:
  title: Automation
  icon: gash_super_pylons:insert_item_upgrade
  position: 3
  parent: upgrades.md
item_ids:
  - gash_super_pylons:insert_item_upgrade
  - gash_super_pylons:insert_fluid_upgrade
  - gash_super_pylons:insert_energy_upgrade
  - gash_super_pylons:insert_xp_upgrade
  - gash_super_pylons:extract_item_upgrade
  - gash_super_pylons:extract_fluid_upgrade
  - gash_super_pylons:extract_energy_upgrade
  - gash_super_pylons:extract_xp_upgrade
---

# Automation

**Insert** pulls from the block on the working side. **Extract** pushes into that block. Put a compatible inventory, tank, battery or XP port against that side. The default is below. The [Wrench](tools.md) can select any of the six sides.

| Variant | Resource moved |
| --- | --- |
| Item | Item stacks |
| Fluid | Liquid in mB |
| Energy | Forge Energy (FE) |
| XP | XP points or supported liquid XP |

Install one direction per resource. Insert and Extract for the same resource can be converted into each other in crafting. Pylons also expose their storage to compatible external automation.

The normal budget is **10% of the pylon's capacity for that resource per tick**, shared with [Wires](wires.md) for that resource. **Capacity VI** enables bulk, so either direction can move the available stock until the destination stops accepting it. Either endpoint having Capacity VI enables bulk on a direct connection between pylons. The destination's own limits and item filter still apply.
