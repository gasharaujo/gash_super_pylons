package net.gasharaujo.superpylons.client;

import java.util.List;

final class PylonPonderContent {
    enum Action { HOLD, RIGHT, LEFT, SNEAK_RIGHT, SNEAK_LEFT, SCROLL }
    enum Focus { PYLON, CHEST, OTHER }

    record Display(String upgrades, String item, int count) {}
    record Step(String icon, String text, Action action, Focus focus, Display display) {}
    record Chapter(String id, String title, List<String> items, boolean chest, boolean other, List<Step> steps) {}

    private PylonPonderContent() {}

    private static List<String> items(String ids) { return List.of(ids.split(" ")); }

    static final List<Chapter> CHAPTERS = List.of(
        new Chapter("pylon_basics", "Pylon: Item Storage",
            List.of(), false, false, List.of(
            new Step("minecraft:iron_ingot",
                "Right-click the Super Pylon to deposit items. It holds 2,048 items of one exact kind by default.",
                Action.RIGHT, Focus.PYLON, new Display("", "minecraft:iron_ingot", 32)),
            new Step("minecraft:iron_ingot",
                "Items must match exactly, including custom names and enchantments. The core shows what is stored.",
                Action.HOLD, Focus.PYLON, null),
            new Step("",
                "Hit the pylon to withdraw one item.",
                Action.LEFT, Focus.PYLON, new Display("", "minecraft:iron_ingot", 31)),
            new Step("",
                "Sneak and hit to withdraw a stack. In Creative, use a pickaxe to break the pylon.",
                Action.SNEAK_LEFT, Focus.PYLON, new Display("", "", 0))
        )),
        new Chapter("upgrade_setup", "Upgrades: Installation",
            items("upgrade_base"), false, false, List.of(
            new Step("upgrade_base",
                "Craft the Upgrade Base with five cobblestone in a cross. Tier I uses the base, and later tiers use the preceding tier.",
                Action.HOLD, Focus.PYLON, null),
            new Step("speed_upgrade_1",
                "Right-click with an upgrade to install it. Its icon appears on the bottom edge of the pylon.",
                Action.RIGHT, Focus.PYLON, new Display("speed_upgrade_1", "", 0)),
            new Step("speed_upgrade_2",
                "Installing a higher tier returns the older one. Sneak and right-click with an empty hand to remove the rightmost upgrade.",
                Action.RIGHT, Focus.PYLON, new Display("speed_upgrade_2", "", 0)),
            new Step("super_upgrade",
                "Super is removed first. When a generator and processor are installed together, the generator comes out before the processor.",
                Action.HOLD, Focus.PYLON, null)
        )),
        new Chapter("generators", "Upgrades: Generators",
            items("cobblestone_generator_upgrade deepslate_generator_upgrade " +
                "blackstone_generator_upgrade netherrack_generator_upgrade sand_generator_upgrade " +
                "glass_generator_upgrade water_generator_upgrade lava_generator_upgrade"), false, false, List.of(
            new Step("cobblestone_generator_upgrade",
                "A generator makes one unit every 60 ticks by default. Speed shortens the interval and Quantity increases the output.",
                Action.RIGHT, Focus.PYLON, new Display("cobblestone_generator_upgrade", "minecraft:cobblestone", 1)),
            new Step("deepslate_generator_upgrade",
                "Item generators make cobblestone, deepslate, blackstone, netherrack, sand or glass. Install one generator per pylon.",
                Action.HOLD, Focus.PYLON, null),
            new Step("water_generator_upgrade",
                "Water and Lava make 100 mB per unit. Their tanks start at 32 buckets. Capacity increases the tank size.",
                Action.HOLD, Focus.PYLON, null),
            new Step("smelt_upgrade",
                "The generator can feed Smelt or Pulverize if its output matches a recipe. It waits when the pylon has a different item or is full.",
                Action.HOLD, Focus.PYLON, null)
        )),
        new Chapter("modifiers", "Upgrades: Modifiers",
            items("speed_upgrade_1 speed_upgrade_2 speed_upgrade_3 speed_upgrade_4 speed_upgrade_5 " +
                "speed_upgrade_6 quantity_upgrade_1 quantity_upgrade_2 quantity_upgrade_3 " +
                "quantity_upgrade_4 quantity_upgrade_5 quantity_upgrade_6 range_upgrade_1 " +
                "range_upgrade_2 range_upgrade_3 range_upgrade_4 range_upgrade_5 range_upgrade_6 " +
                "storage_upgrade_1 storage_upgrade_2 storage_upgrade_3 storage_upgrade_4 " +
                "storage_upgrade_5 storage_upgrade_6 energy_upgrade_1 energy_upgrade_2 " +
                "energy_upgrade_3 energy_upgrade_4 energy_upgrade_5 energy_upgrade_6"), false, false, List.of(
            new Step("speed_upgrade_1",
                "Speed controls the cycle. At tiers 0 to VI the interval is 60, 40, 30, 20, 10, 5 or 1 tick.",
                Action.RIGHT, Focus.PYLON, new Display("speed_upgrade_1", "", 0)),
            new Step("quantity_upgrade_1",
                "Quantity controls output per cycle. At tiers 0 to VI the amounts are 1, 2, 4, 8, 16, 32 and 64.",
                Action.RIGHT, Focus.PYLON, new Display("speed_upgrade_1,quantity_upgrade_1", "", 0)),
            new Step("range_upgrade_1",
                "Range grows the work area in all axes. It starts at 3 cubed and reaches 17 cubed. Dropper gains one block per tier.",
                Action.HOLD, Focus.PYLON, null),
            new Step("storage_upgrade_6",
                "Capacity multiplies item, fluid and XP storage by 2, 4, 8, 16 or 32. Tier VI gives unlimited capacity and bulk transfer.",
                Action.HOLD, Focus.PYLON, null),
            new Step("energy_upgrade_1",
                "Energy adds FE storage to any pylon, from 1 million to 2 billion FE. It can also charge a compatible item in the core.",
                Action.HOLD, Focus.PYLON, null)
        )),
        new Chapter("utility_field", "Utility: Work in the World",
            items("fertilizer_upgrade harvester_upgrade planter_upgrade magnet_upgrade placer_upgrade " +
                "break_upgrade clicker_upgrade dropper_upgrade"), false, false, List.of(
            new Step("fertilizer_upgrade",
                "Fertilizer gives plants random ticks. Speed controls attempts and Quantity controls the number of plants.",
                Action.HOLD, Focus.PYLON, null),
            new Step("harvester_upgrade",
                "Harvester gathers mature crops and fells whole trees. Planter uses stored seeds or saplings to plant within Range.",
                Action.HOLD, Focus.PYLON, null),
            new Step("magnet_upgrade",
                "Magnet collects dropped items within range. Super sends them directly into the output inventory.",
                Action.HOLD, Focus.PYLON, null),
            new Step("break_upgrade",
                "Break mines the working side. Sneak and right-click with a mining tool to replace its default diamond pickaxe.",
                Action.HOLD, Focus.PYLON, null),
            new Step("placer_upgrade",
                "Placer places a stored block. Clicker right-clicks the working side with a stored item or an empty hand.",
                Action.HOLD, Focus.PYLON, null),
            new Step("dropper_upgrade",
                "Dropper releases items on the working side, one block farther per Range tier. With Super it works as a precise dispenser.",
                Action.HOLD, Focus.PYLON, null)
        )),
        new Chapter("utility_storage", "Utility: Storage and Control",
            items("fluid_storage_upgrade xp_tank_upgrade filter_upgrade void_upgrade " +
                "chunk_loader_upgrade super_upgrade"), false, false, List.of(
            new Step("fluid_storage_upgrade",
                "Fluid Storage holds one fluid and starts at 32 buckets. Empty the tank before removing it.",
                Action.HOLD, Focus.PYLON, null),
            new Step("xp_tank_upgrade",
                "XP Tank collects nearby orbs and XP from players above. Players below receive XP. One experience bottle gives seven points.",
                Action.HOLD, Focus.PYLON, null),
            new Step("filter_upgrade",
                "Right-click with empty hands to open Filter. Its 27 slots accept item samples or tags. Select an allow or block list.",
                Action.RIGHT, Focus.PYLON, null),
            new Step("void_upgrade",
                "Void discards incoming items, fluids, FE and XP. Chunk Loader keeps a square of chunks active around the pylon.",
                Action.HOLD, Focus.PYLON, null),
            new Step("minecraft:command_block",
                "Creative Storage is installed with a Command Block. Seed the stock first, then withdrawals make copies.",
                Action.HOLD, Focus.PYLON, null),
            new Step("super_upgrade",
                "Super changes other utilities. For example, Harvester attacks mobs and Break mines the chunk starting three blocks below.",
                Action.RIGHT, Focus.PYLON, new Display("super_upgrade", "", 0))
        )),
        new Chapter("utility_processing", "Utility: Processing and Workshops",
            items("smelt_upgrade pulverize_upgrade enchanting_upgrade anvil_upgrade"), true, true, List.of(
            new Step("smelt_upgrade",
                "Smelt uses furnace recipes. Install a blast furnace or smoker to select its recipe type.",
                Action.HOLD, Focus.PYLON, null),
            new Step("pulverize_upgrade",
                "Pulverize crushes blocks and ores. Both processors spend 1,000 FE per item, then use furnace fuel if FE runs out.",
                Action.HOLD, Focus.PYLON, null),
            new Step("enchanting_upgrade",
                "Enchanter needs lapis and 27 XP points from two auxiliaries. Put the item to enchant in the main pylon.",
                Action.HOLD, Focus.PYLON, null),
            new Step("anvil_upgrade",
                "Anvil needs the second item or repair material plus XP. Use the Wrench to bind both auxiliaries 1–5 blocks away.",
                Action.HOLD, Focus.PYLON, null),
            new Step("minecraft:chest",
                "The finished item goes to the inventory on the working side. If it is full, the result waits in the core.",
                Action.HOLD, Focus.CHEST, null)
        )),
        new Chapter("automation", "Upgrades: Automation",
            items("insert_item_upgrade insert_fluid_upgrade insert_energy_upgrade insert_xp_upgrade " +
                "extract_item_upgrade extract_fluid_upgrade extract_energy_upgrade extract_xp_upgrade"), true, false, List.of(
            new Step("insert_item_upgrade",
                "Insert pulls from the adjacent inventory into the pylon. The working side is down by default.",
                Action.RIGHT, Focus.PYLON, new Display("insert_item_upgrade", "minecraft:iron_ingot", 32)),
            new Step("extract_item_upgrade",
                "Extract sends from the pylon into the adjacent inventory. Use the Wrench to change the working side.",
                Action.RIGHT, Focus.PYLON, new Display("extract_item_upgrade", "", 0)),
            new Step("extract_fluid_upgrade",
                "There are Insert and Extract upgrades for items, fluids, energy and XP. The adjacent block needs a compatible port.",
                Action.HOLD, Focus.PYLON, null),
            new Step("storage_upgrade_6",
                "The normal budget is 10% of the pylon's capacity per tick. Capacity VI enables bulk up to what the destination accepts.",
                Action.HOLD, Focus.PYLON, null)
        )),
        new Chapter("wires", "Wires: Connect Two Pylons",
            items("item_wire fluid_wire energy_wire xp_wire"), false, true, List.of(
            new Step("item_wire",
                "Right-click the source first. Then right-click the destination up to 32 blocks away in the same dimension.",
                Action.RIGHT, Focus.PYLON, new Display("", "minecraft:iron_ingot", 32)),
            new Step("item_wire",
                "The connection consumes a wire and moves resources from the first pylon to the second.",
                Action.RIGHT, Focus.OTHER, new Display("", "minecraft:iron_ingot", 16)),
            new Step("fluid_wire",
                "Yellow carries items, blue carries fluids, red carries FE and green carries XP. Capacity VI enables bulk.",
                Action.HOLD, Focus.OTHER, null),
            new Step("item_wire",
                "Click the pair again in either order to recover the wire. Breaking either end also drops the wire once.",
                Action.RIGHT, Focus.OTHER, null)
        )),
        new Chapter("tools", "Tools: Wrench, Copy and Paint",
            items("wrench copy_paste_tool paint_tool"), false, true, List.of(
            new Step("wrench",
                "Sneak-scroll to switch Wrench modes. In Bind, sneak-right-click the main workshop, then right-click its auxiliaries.",
                Action.SCROLL, Focus.PYLON, null),
            new Step("wrench",
                "In Direction, sneak and right-click in the air to cycle sides. Right-click a pylon to set its working side.",
                Action.RIGHT, Focus.PYLON, null),
            new Step("copy_paste_tool",
                "Sneak-right-click to copy upgrades. Right-click the target to apply them using your inventory. Replaced upgrades return to you.",
                Action.SNEAK_RIGHT, Focus.PYLON, null),
            new Step("paint_tool",
                "With a block in your offhand, right-click to paint the frame. With no block, sneak-click to copy or click to apply a saved texture.",
                Action.RIGHT, Focus.OTHER, null),
            new Step("copy_paste_tool",
                "A glint and tooltip show saved data. Sneak and right-click in the air to clear Paint, Copy Paste or the Wrench in Bind mode.",
                Action.SNEAK_RIGHT, Focus.PYLON, null)
        ))
    );
}
