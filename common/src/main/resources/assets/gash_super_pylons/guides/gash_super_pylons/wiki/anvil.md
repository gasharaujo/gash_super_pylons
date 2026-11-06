---
navigation:
  title: Anvil
  icon: gash_super_pylons:anvil_upgrade
  position: 1
  parent: workshops.md
item_ids:
  - gash_super_pylons:anvil_upgrade
---

# Anvil

## Repair or combine items

1. Install **Anvil** on the main pylon. Use the [binding walkthrough](workshops.md) to link one XP Tank and one item auxiliary. In **Bind** mode, sneak and right-click the main pylon, then release Sneak and right-click both auxiliaries.
2. Put the **second input** in the item auxiliary. It can be a compatible enchanted book, another item of the same type or the matching repair material.
3. Put the item to keep and improve in the **main pylon**. Anvil runs automatically when it finds a valid operation and the linked tank has enough XP. It consumes one second input per successful operation.
4. Collect the result from the working-side chest. Hit the main pylon to take the result if it is still in the core.

## Enchantment levels and XP

Two copies of the same enchantment at the same level combine into the next level. When levels differ, the result keeps the higher one. The item must support the enchantment and it must be compatible with the others on the item.

For each enchantment on the result, multiply its level by its rarity multiplier (1 common, 2 uncommon, 3 rare, 4 very rare). A level above the usual maximum multiplies that term again by rarity, with a minimum second multiplier of 2. The sum gives the level cost, which the Anvil withdraws as the equivalent XP points from the linked tank. A repair with no enchantments costs one level.

## Reuse a book with Super

Install **Super** on the main pylon. An enchanted book in the item auxiliary stays in its core after use. The Anvil still spends XP for each operation and consumes other donor items or repair materials normally.

<RecipeFor id="gash_super_pylons:anvil_upgrade" />
