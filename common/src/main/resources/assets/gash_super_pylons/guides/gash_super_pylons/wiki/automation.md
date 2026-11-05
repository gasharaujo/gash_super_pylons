---
navigation:
  title: Automation
  icon: item_wire
  parent: index.md
  position: 1
item_ids:
  - gash_super_pylons:item_wire
  - gash_super_pylons:fluid_wire
  - gash_super_pylons:energy_wire
  - gash_super_pylons:xp_wire
  - gash_super_pylons:filter_upgrade
  - gash_super_pylons:wrench
---

# Automation

Use a Wire on the source first, then on the destination within 32 blocks. Yellow carries items, blue fluids, red energy, and green XP. Select the same pair again in either order to disconnect and recover the installed Wire.

Insert pulls from the adjacent block. Extract pushes into it. Wires and transfer upgrades share a budget of 10% of capacity per resource per tick. Capacity VI removes that budget, so the transfer can use all available supply and space within the connected equipment's limits.

Install Filter and right-click with empty hands to open its 27 slots. Place a sample from your cursor or drag an item from JEI. Middle-click a filled slot to choose an item tag or restore exact matching. Switch between allow and block lists at the top.

Hold the Wrench and sneak-scroll to change modes. **Bind** connects workshop auxiliaries. In **Direction**, sneak and right-click in the air to cycle through the six sides, then right-click a pylon to set its working side. The default is down.

## Bulk and wire refunds

Capacity VI enables bulk for items, fluids, FE and XP in both directions. Wires, Insert and Extract move as much as the destination accepts, continuing until a finite source is empty or acceptance stops. Either pylon having Capacity VI enables bulk on their connection. The pylon's FE rate cap is lifted in bulk mode; other mods' acceptance rules and item filters still apply. Large requests use the external API's supported batches. The AE2 Matter Condenser supports long quantities directly. Transfers between an inexhaustible source and destination use the largest API request so each cycle can finish.

Breaking either connected pylon drops its wire once and removes the connection. Incoming references are saved so the cable can also be refunded when the origin's chunk is inactive. Invalid links from older saves return their wire when detected.
