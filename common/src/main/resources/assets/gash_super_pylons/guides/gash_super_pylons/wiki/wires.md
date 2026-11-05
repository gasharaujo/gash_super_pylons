---
navigation:
  title: Wires
  icon: gash_super_pylons:item_wire
  position: 3
item_ids:
  - gash_super_pylons:item_wire
  - gash_super_pylons:fluid_wire
  - gash_super_pylons:energy_wire
  - gash_super_pylons:xp_wire
---

# Wires

Click the **source** pylon and then the **destination**. They must be within 32 blocks in the same dimension. A successful connection consumes one wire and moves resources from the first pylon to the second.

| Color | Resource |
| --- | --- |
| Yellow | Items |
| Blue | Fluids |
| Red | Energy |
| Green | XP |

One pair of pylons can have one connection. Click the same pair again in either order to disconnect and recover the cable. Sneak and right-click a pylon to cancel a selection. Breaking either endpoint also drops the wire once.

Wires share the [automation transfer rates](automation.md) of their source pylon. Capacity VI on either endpoint enables bulk.
