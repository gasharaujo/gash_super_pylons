---
navigation:
  title: Utility
  icon: gash_super_pylons:harvester_upgrade
  position: 2
  parent: upgrades.md
item_ids:
  - gash_super_pylons:fluid_storage_upgrade
  - gash_super_pylons:xp_tank_upgrade
  - gash_super_pylons:anvil_upgrade
  - gash_super_pylons:enchanting_upgrade
  - gash_super_pylons:clicker_upgrade
  - gash_super_pylons:fertilizer_upgrade
  - gash_super_pylons:harvester_upgrade
  - gash_super_pylons:break_upgrade
  - gash_super_pylons:filter_upgrade
  - gash_super_pylons:magnet_upgrade
  - gash_super_pylons:planter_upgrade
  - gash_super_pylons:placer_upgrade
  - gash_super_pylons:dropper_upgrade
  - gash_super_pylons:void_upgrade
  - gash_super_pylons:chunk_loader_upgrade
  - gash_super_pylons:super_upgrade
  - gash_super_pylons:smelt_upgrade
  - gash_super_pylons:pulverize_upgrade
---

# Utility

## Farming, collection and building

- **Fertilizer** gives plants random ticks. Speed controls the cycle and the number of attempts per plant. Quantity sets the number of plants.
- **Harvester** harvests mature crops and leaves them planted. It also fells whole trees with natural leaves. Drops that cannot be stored fall in the world. Craft it with an Upgrade Base and a diamond axe.
- **Planter** plants stored seeds or saplings in valid spaces within range.
- **Magnet** gathers dropped items. It starts with one item per 60 ticks in a 3×3×3 area and follows the pylon's filter.
- **Placer** places a stored block on the working side.
- **Break** mines the block on the working side with a virtual diamond pickaxe. Sneak and right-click with a mining tool to equip it. Matching drops of the first kind go into storage, and the rest fall.
- **Clicker** right-clicks the working side with one stored item or with an empty hand.
- **Dropper** releases items centered on the working side. Quantity and Speed control the cycle, and each Range tier adds one block of reach.

## Storage and control

- **Fluid Storage** holds one fluid type and starts at 32 buckets. It accepts buckets and fluid automation. Empty it before removing the upgrade. Breaking the pylon discards the fluid.
- **XP Tank** gathers orbs within two blocks, draws XP from players above and gives XP to players below. Starts at 32,000 XP points and uses Speed for the player transfer rate. An experience bottle adds 7 points and returns a glass bottle. A glass bottle does the reverse. It works with supported liquid XP from Just Dire Things, Ender IO and Mob Grinding Utils at 20 mB per point.
- **Filter** opens with right-click and empty hands. The 27 slots support samples from the cursor or JEI, plus item tags selected with middle-click. Choose an allow or block list.
- **Void** discards incoming items, fluids, energy and XP.
- **Chunk Loader** keeps a square of chunks active around the pylon. The starting radius is one chunk. Range can extend it.
- **Creative Storage** installs with a vanilla Command Block. Seed the appropriate storage and withdrawal supplies copies. An empty store supplies nothing.
- **Super** adds four orbiting Nether Stars and changes the upgrades listed below.

## Processing

**Smelt** uses furnace recipes. Install a blast furnace or smoker on the pylon to specialize its recipes. **Pulverize** turns cobblestone into gravel, gravel into sand with a chance of flint, terracotta into red sand and supported stone into cobblestone. It also processes ore blocks and makes equivalent dusts if they exist in the modpack. Quantity changes some yields and chances. Datapacks can add pulverizing recipes.

Both send outputs to the inventory on the working side and cost **1,000 FE per input item**. They try the pylon's FE first, then adjacent pylons, then furnace fuel from a side inventory. One coal supplies eight uses. Valid generator products can feed either processor.

## Enchanter and Anvil

With the [Wrench](tools.md) in Bind mode, sneak and right-click the main pylon, then right-click an XP Tank and an item auxiliary. The auxiliaries can be 1–5 blocks away in any direction. The main pylon takes one input at a time. It sends the finished item to the working-side inventory, or keeps it in the core until space opens.

**Enchanter** takes the item in the main pylon and lapis in the auxiliary. It spends one lapis and 27 XP points per item. Capacity sets enchanting power to 1, 10, 20, 30, 40, 50 or 100. Quantity sets a minimum of 1–7 compatible enchantments when available. Right-click to install up to 64 supported Apotheosis or Apothic Enchanting shelves.

**Anvil** takes the first item in the main pylon and the second item or repair material in the auxiliary. Equal levels combine into the next level. Each improved enchantment costs its resulting level times its rarity. Going above the normal maximum multiplies that price again by rarity, with a minimum multiplier of two.

## Super combinations

- **Smelt and Pulverize** work through every eligible stack in the inventory opposite the output as space and fuel allow.
- **Fertilizer** advances plant stages directly. **Harvester** attacks nearby mobs, up to 1 + Quantity tier per operation. Sneak and right-click with a weapon to equip it. The default weapon is a diamond axe.
- **Break and Fluid Storage** scan the pylon's chunk from three blocks below it. Each Range tier adds one chunk to the radius. Break sends drops to the output inventory, and Fluid Storage collects sources and can send them to the adjacent tank.
- **Magnet** puts items directly in the output inventory. **Placer** fills gaps in a plane three blocks below. **Dropper** uses dispenser behaviors and aims projectiles along the working direction.
- **Enchanter** can read a blacklist from an optional third auxiliary's enchanted item. **Anvil** keeps an enchanted book used as its second item.
