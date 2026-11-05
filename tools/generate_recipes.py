"""Generate the checked-in crafting recipes for the three supported loaders."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MOD = "gash_super_pylons"
BASE = f"{MOD}:upgrade_base"
MODULES = ("forge-1.20.1", "neoforge-1.21.1", "neoforge-26.1.2")


def vanilla(item):
    return f"minecraft:{item}"


def ingredient(value, modern):
    if modern:
        return value
    if value.startswith("#"):
        return {"tag": value[1:]}
    return {"item": value}


def shaped(pattern, key, result, count=1):
    return {"pattern": pattern, "key": key, "result": result, "count": count}


def shapeless(ingredients, result):
    return {"ingredients": ingredients, "result": result, "count": 1}


def tier_recipes():
    corners = ("copper_ingot", "iron_ingot", "gold_ingot", "diamond_block", "emerald_block", "crying_obsidian")
    materials = {
        "energy": ("redstone",) * 3 + ("redstone_block",) * 3,
        "range": ("ender_pearl",) * 3 + ("spyglass",) * 3,
        "speed": ("sugar",) * 3 + ("packed_ice",) * 2 + ("blue_ice",),
        "quantity": ("lapis_lazuli",) * 3 + ("lapis_block",) * 3,
        "storage": ("chest",) * 5 + ("ender_chest",),
    }
    for family, sides in materials.items():
        previous = BASE
        for tier, (corner, side) in enumerate(zip(corners, sides, strict=True), start=1):
            upgrade = f"{family}_upgrade_{tier}"
            yield upgrade, shaped(
                ["TMT", "MNM", "TMT"], {"T": vanilla(corner), "M": vanilla(side), "N": previous}, upgrade
            )
            previous = f"{MOD}:{upgrade}"


def recipes():
    result = {
        "upgrade_base": shaped([" C ", "CCC", " C "], {"C": vanilla("cobblestone")}, "upgrade_base"),
    }
    result.update(tier_recipes())

    generator_pattern = ["LMW", "MNM", "WML"]
    buckets = {"W": vanilla("water_bucket"), "L": vanilla("lava_bucket")}
    for material in ("cobblestone", "deepslate", "blackstone", "netherrack", "sand"):
        upgrade = f"{material}_generator_upgrade"
        result[upgrade] = shaped(generator_pattern, buckets | {"M": vanilla(material), "N": BASE}, upgrade)
    for fluid in ("water", "lava"):
        bucket = vanilla(f"{fluid}_bucket")
        upgrade = f"{fluid}_generator_upgrade"
        result[upgrade] = shaped(
            generator_pattern, {"W": bucket, "L": bucket, "M": vanilla("bucket"), "N": BASE}, upgrade
        )
    result["glass_generator_upgrade"] = shaped(generator_pattern, {
        "W": vanilla("lava_bucket"), "L": vanilla("lava_bucket"), "M": vanilla("glass"),
        "N": f"{MOD}:sand_generator_upgrade",
    }, "glass_generator_upgrade")

    generic = {
        "fluid_storage_upgrade": "bucket",
        "xp_tank_upgrade": "experience_bottle",
        "fertilizer_upgrade": "bone_block",
        "placer_upgrade": "stone",
        "break_upgrade": "diamond_pickaxe",
        "filter_upgrade": "paper",
        "void_upgrade": "cactus",
        "smelt_upgrade": "furnace",
        "pulverize_upgrade": "stonecutter",
        "clicker_upgrade": "dispenser",
        "dropper_upgrade": "dropper",
        "enchanting_upgrade": "enchanting_table",
    }
    for upgrade, material in generic.items():
        result[upgrade] = shapeless([BASE, vanilla(material), vanilla(material)], upgrade)
    result["harvester_upgrade"] = shapeless([BASE, vanilla("diamond_axe")], "harvester_upgrade")
    result["planter_upgrade"] = shapeless([BASE, f"#{MOD}:planter_seeds", "#minecraft:saplings"], "planter_upgrade")
    result["magnet_upgrade"] = shapeless([BASE, vanilla("iron_ingot"), vanilla("gold_ingot")], "magnet_upgrade")

    ports = {"item": "chest", "fluid": "bucket", "energy": "redstone_block", "xp": "emerald"}
    for resource, material in ports.items():
        insert = f"insert_{resource}_upgrade"
        extract = f"extract_{resource}_upgrade"
        key = {"M": vanilla(material), "H": vanilla("hopper")}
        result[insert] = shaped(["M", "H"], key, insert)
        result[extract] = shaped(["H", "M"], key, extract)
        result[f"{insert}_from_extract"] = shapeless([f"{MOD}:{extract}"], insert)
        result[f"{extract}_from_insert"] = shapeless([f"{MOD}:{insert}"], extract)

    result["chunk_loader_upgrade"] = shaped(
        ["LCL", "CNC", "LCL"], {"N": BASE, "L": vanilla("lodestone"), "C": vanilla("crying_obsidian")},
        "chunk_loader_upgrade",
    )
    result["anvil_upgrade"] = shaped(
        [" M ", "MNM", "DDD"], {"M": vanilla("anvil"), "N": BASE, "D": vanilla("ancient_debris")},
        "anvil_upgrade",
    )
    result["super_upgrade"] = shaped(
        ["SnS", "nNn", "SnS"], {"S": vanilla("nether_star"), "n": vanilla("netherite_ingot"), "N": BASE},
        "super_upgrade",
    )

    copper = vanilla("copper_ingot")
    # Vanilla shaped recipes also accept the horizontal mirror.
    result["wrench"] = shaped([" C ", " CC", "C  "], {"C": copper}, "wrench")
    result["copy_paste_tool"] = shapeless([vanilla("book"), BASE], "copy_paste_tool")
    result["paint_tool"] = shapeless([vanilla("brush"), BASE], "paint_tool")
    for resource, material in (ports | {"energy": "redstone"}).items():
        wire = f"{resource}_wire"
        result[wire] = shaped(["CMC"], {"C": copper, "M": vanilla(material)}, wire, 16)

    return result


def encode(recipe, module):
    modern = module == "neoforge-26.1.2"
    forge = module == "forge-1.20.1"
    shaped_recipe = "pattern" in recipe
    data = {
        "type": "minecraft:crafting_shaped" if shaped_recipe else "minecraft:crafting_shapeless",
        "category": "misc",
    }
    if shaped_recipe:
        data["pattern"] = recipe["pattern"]
        data["key"] = {key: ingredient(value, modern) for key, value in recipe["key"].items()}
    else:
        data["ingredients"] = [ingredient(value, modern) for value in recipe["ingredients"]]
    data["result"] = {"item" if forge else "id": f"{MOD}:{recipe['result']}", "count": recipe["count"]}
    return data


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    all_recipes = recipes()
    for module in MODULES:
        forge = module == "forge-1.20.1"
        root = ROOT / module / f"src/main/resources/data/{MOD}"
        for name, recipe in all_recipes.items():
            write_json(root / ("recipes" if forge else "recipe") / f"{name}.json", encode(recipe, module))
        loader_seeds = "forge:seeds" if forge else "c:seeds"
        write_json(root / ("tags/items" if forge else "tags/item") / "planter_seeds.json", {
            "replace": False,
            "values": [f"#{loader_seeds}", vanilla("torchflower_seeds"), vanilla("pitcher_pod")],
        })


if __name__ == "__main__":
    main()
