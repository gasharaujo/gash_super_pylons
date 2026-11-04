"""Convert the original Blockbench export into portable frame/item resources.

The core is rendered separately because 1.20.1 and 1.21.1 cannot bake its
multi-axis rotation. Run this explicitly after editing model-source/Pylon.json.
Generated resources are committed, so building does not require Python.
"""
import copy
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
MOD = "gash_super_pylons"


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    source = json.loads((ROOT / "model-source/Pylon.json").read_text(encoding="utf-8"))
    cores = [element for element in source["elements"] if element.get("name") == "core"]
    if len(cores) != 1:
        raise ValueError("Expected exactly one element named core")
    core = cores[0]
    # Keep the Java renderer aligned with the source; fail visibly if it changes.
    if core["from"] != [3, 3, 8] or core["to"] != [8, 8, 13]:
        raise ValueError("Update PylonCoreModel.java for the new core dimensions")
    if core["rotation"] != {"x": 180, "y": 45, "z": -125, "origin": [8, 3, 8]}:
        raise ValueError("Update PylonAnimation.java for the new core orientation")
    bars = []
    for element in source["elements"]:
        if element is core:
            continue
        bar = copy.deepcopy(element)
        bar.pop("color", None)
        rotation = bar.get("rotation")
        if rotation and rotation.get("angle") == 0:
            bar.pop("rotation")
        for face in bar["faces"].values():
            face["texture"] = "#frame"
        bars.append(bar)

    assets = ROOT / f"common/src/main/resources/assets/{MOD}"
    write_json(assets / "models/block/super_pylon.json", {
        "parent": "minecraft:block/block",
        "credit": source.get("credit", "Made with Blockbench"),
        "textures": {"frame": "minecraft:block/iron_block", "particle": "minecraft:block/iron_block"},
        "elements": bars,
    })
    write_json(assets / "blockstates/super_pylon.json", {
        "variants": {
            "painted=false": {"model": f"{MOD}:block/super_pylon"},
            "painted=true": {"model": f"{MOD}:block/super_pylon_painted"}
        }
    })
    write_json(assets / "models/block/super_pylon_painted.json", {
        "textures": {"particle": "minecraft:block/iron_block"},
        "elements": []
    })
    for language in ["en_us", "pt_br"]:
        lang_path = assets / f"lang/{language}.json"
        translations = json.loads(lang_path.read_text(encoding="utf-8")) if lang_path.exists() else {}
        translations[f"block.{MOD}.super_pylon"] = "Super Pylon"
        write_json(lang_path, translations)

    # Opaque pure-white RGBA, with enough space for the cube's unfolded UV map.
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress((b"\x00" + b"\xff" * (32 * 4)) * 32))
    png += chunk(b"IEND", b"")
    texture = assets / "textures/block/core.png"
    texture.parent.mkdir(parents=True, exist_ok=True)
    texture.write_bytes(png)

    display = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375] * 3},
        "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4] * 3},
    }
    for module in ["forge-1.20.1", "neoforge-1.21.1", "neoforge-26.1.2"]:
        resources = ROOT / module / "src/main/resources"
        modern = module.endswith("26.1.2")
        plural = module.startswith("forge-")
        write_json(resources / f"assets/{MOD}/models/item/super_pylon.json", {
            "parent": "minecraft:block/block" if modern else "builtin/entity",
            "gui_light": "side",
            "textures": {"particle": "minecraft:block/iron_block"},
            "display": display,
        })
        if modern:
            write_json(resources / f"assets/{MOD}/items/super_pylon.json", {
                "model": {"type": "minecraft:special", "base": f"{MOD}:item/super_pylon",
                          "model": {"type": f"{MOD}:super_pylon"}}
            })
        write_json(resources / f"data/{MOD}/{'loot_tables' if plural else 'loot_table'}/blocks/super_pylon.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:super_pylon"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        })
        for tag in ["mineable/pickaxe", "needs_stone_tool"]:
            write_json(resources / f"data/minecraft/tags/{'blocks' if plural else 'block'}/{tag}.json",
                       {"replace": False, "values": [f"{MOD}:super_pylon"]})
    print("Generated Super Pylon frame, white core texture, item models, loot and tags.")


if __name__ == "__main__":
    main()
