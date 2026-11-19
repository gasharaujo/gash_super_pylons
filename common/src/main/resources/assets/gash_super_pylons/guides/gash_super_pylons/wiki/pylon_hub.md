---
navigation:
  title: Pylon HUB
  icon: gash_super_pylons:pylon_hub
  position: 0
  parent: pylon.md
item_ids:
  - gash_super_pylons:pylon_hub
---

# Pylon HUB

The HUB exposes items, fluids, XP and energy from its linked pylons. Right-click with an empty hand to see totals and the number of active links.

1. Put the Wrench in **Bind** mode. Sneak and right-click the HUB.
2. Release Sneak and right-click each target pylon. Click it again to unlink it.
3. Attach a cable or external storage reader to the HUB. Item, fluid, FE and XP operations go directly to the target pylon.

Base range is **16 blocks**. Each [Range](range.md) tier adds 16. With [Super](super.md), links can span any distance or dimension. The HUB and the target must be in loaded chunks. The HUB accepts Range and Super, and saves the link configuration. Integrations are optional.

On NeoForge, the AE2 Storage Bus accesses the HUB's items and fluids through native ME storage. Refined Storage External Storage and other readers can use the compatible loader ports. SCC can use the fluid port for liquid XP.

<RecipeFor id="gash_super_pylons:pylon_hub" />
