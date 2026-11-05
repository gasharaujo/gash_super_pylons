---
navigation:
  title: Upgrades
  icon: planter_upgrade
  parent: index.md
  position: 2
item_ids:
  - gash_super_pylons:planter_upgrade
  - gash_super_pylons:harvester_upgrade
  - gash_super_pylons:placer_upgrade
  - gash_super_pylons:dropper_upgrade
  - gash_super_pylons:super_upgrade
  - gash_super_pylons:void_upgrade
  - gash_super_pylons:chunk_loader_upgrade
  - gash_super_pylons:xp_tank_upgrade
  - gash_super_pylons:energy_upgrade_1
---

# Upgrades

Craft the Upgrade Base from five cobblestone in a cross. Tier I uses the base in the center. Each higher tier uses the previous upgrade. JEI lists the crafting recipes when installed.

**Speed** changes the operation interval. **Quantity** changes the amount per operation. **Range** expands the working area horizontally and vertically. **Capacity** increases storage. **Energy** adds FE storage independently of the pylon's specialty and accepts power from every side.

- **Planter** plants stored seeds and saplings. **Fertilizer** speeds up growth.
- **Harvester** is crafted from an upgrade base and a diamond axe. It replants crops and fells trees. Tree drops that fit stay in the pylon. Excess drops remain in the world.
- **Placer** places a stored block at the working side. **Break** mines that side. **Clicker** interacts with it, including with an empty hand.
- **Dropper** ejects stored items at the center of the adjacent block on the working side. Speed and Quantity control the cycle, and each Range tier extends the ejection by one block.
- **Super** adds four Nether Stars that orbit the core and is removed first.
- **Void** destroys incoming items, fluids, FE, and XP. **Chunk Loader** keeps a square of chunks active with a starting radius of one chunk.
- **XP Tank** exchanges a Bottle o' Enchanting for seven XP and a glass bottle. Use a glass bottle on the tank to reverse the exchange.
- **Fluid Storage** starts at 32 buckets. Water and Lava Generators also act as tanks.

Item generators can feed Smelt or Pulverize. Each processed item costs 1,000 FE. The pylon tries its own energy first, then checks adjacent pylons. It uses furnace fuel when FE is unavailable. Use a blast furnace or smoker on a Smelt pylon to select that recipe family.

Generators have their own slot: only one generator is allowed, independently of utility/type and processing upgrades. Generation runs alongside the other installed upgrades. A different stored item pauses item generation instead of blocking installation. Water/lava generators can hold items as well as fluid; an installed type may still block item input. With XP Tank, fluid automation exposes two separate reservoirs.

## Creative Storage

Right-click a Super Pylon with a vanilla Command Block to install Creative Storage in its own slot. Seed the storage with an item, fluid, FE or XP. Extraction then creates copies while preserving the original contents. This applies to manual withdrawal, hoppers, automation, wires, transfer upgrades, other pylons and machine consumption. Empty stores produce nothing. The fluid, energy and XP upgrades are still needed for their respective stores; capacity and transfer limits still apply.

The Command Block is shown in the upgrade bar and overlay, with infinite amounts marked as ∞. Shift-right-click with an empty hand removes and refunds it in the normal removal order, after Super and an installed Break tool. Removal restores finite storage. Copy Paste includes it and requires a Command Block from the player's inventory. Breaking preserves the setup and real item/FE/XP contents in the dropped pylon; fluids are still discarded.

## Enchanter and Anvil

Shift-right-click the main pylon with the Wrench in Bind mode, then right-click an XP Tank and an item pylon. Either order works. Auxiliaries may be 1–5 blocks from the main pylon, measured between centers, vertically, horizontally or diagonally. Both machines work on one item at a time; the finished item replaces the input in the core and can be extracted by automation. Speed sets the operation interval. Nothing is consumed when an operation cannot finish.

### Enchanter

Put the item in the main pylon and lapis lazuli in the item auxiliary. Each operation consumes one lapis and **27 XP points**, equivalent to three levels starting from zero, at every power level. Capacity 0/I/II/III/IV/V/VI sets base enchanting power to **1/10/20/30/40/50/100**. Quantity on the main pylon sets a minimum of **1/2/3/4/5/6/7 enchantments**; only compatible available enchantments are added. Already enchanted items are not enchanted again.

With Super, bind an optional third pylon containing an enchanted item or book. Its enchantments are blacklisted, its contents are preserved, and its normal core remains visible. Only the first two auxiliaries display enchanting-table books. Removing Super releases the third link.

Right-click Apotheosis/Apothic Enchanting shelves onto the Enchanter to install up to 64 shelves. The integration uses that mod's actual shelf stats and enchantment selection, including Eterna, Quanta, Arcana, treasure access, stability/rectification and shelf blacklists where available. Interface clues do not affect automatic enchanting. Shelves are saved and displayed in the upgrade bar. They are returned before the Enchanter when it becomes next in the removal order.

### Anvil

Put the first anvil item in the main pylon and the second item or repair material in the item auxiliary. Equal enchantment levels combine into the next level, including beyond the normal maximum; different levels keep the greater value. Enchantments must remain compatible. The technical level limit is 255.

The price sums **resulting enchantment level × rarity**: common 1, uncommon 2, rare 3 and very rare 4. For an enchantment above its normal maximum, multiply its price by rarity again, with a minimum multiplier of two. Repairs cost at least one level. The total is converted to XP points using the vanilla level curve from zero and paid by the XP auxiliary.

Super preserves an enchanted book in the second slot. Other donor items and repair materials are still consumed. An operation must improve enchantments or repair damage to spend resources. Jade, WTHIT and The One Probe show machine status, XP cost and installed shelves.

## Super combinations

- **Smelt and Pulverize** process every eligible stack from the inventory opposite the working side in the same cycle, as output space and energy or fuel allow.
- **Fertilizer** advances plant stages directly. **Harvester** attacks nearby mobs, starting with the closest. Quantity allows 1 + tier targets. Sneak and use a weapon on the pylon to equip it. The default weapon is a diamond axe.
- **Break and Fluid Storage** work down through the pylon's chunk, starting three blocks beneath it. Each Range tier adds one chunk to the radius. Break sends drops to the inventory on the working side. Fluid Storage collects sources and can send them to a tank on the working side.
- **Magnet** inserts items into the inventory on its working side. **Placer** fills gaps in a plane three blocks below the pylon. Range expands the plane.
- **Dropper** uses Minecraft dispenser behaviors and aligns each projectile's initial trajectory with the working direction.
