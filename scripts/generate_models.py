#!/usr/bin/env python3
import json
import re
from pathlib import Path

BASE_DIR = Path("assets/models")
OUT_DIR = Path("src/main/resources/assets/logistics/models/block")
VALID_DIRECTIONS = {"north", "south", "east", "west", "up", "down"}


def texture_name_from_bbmodel(data, fallback):
    textures = data.get("textures") or []
    if textures:
        name = textures[0].get("name", "")
        if name.endswith(".png"):
            return name[:-4]
        if name:
            return name
    return fallback


def convert_faces(faces):
    out = {}
    for name, face in faces.items():
        entry = {"texture": "#texture"}
        if "uv" in face:
            entry["uv"] = face["uv"]
        if "rotation" in face:
            entry["rotation"] = face["rotation"]
        out[name] = entry
    return out


def suffix_for_element(name):
    if name == "center":
        return "core"
    if name == "center_powered":
        return "core_powered"
    if name in VALID_DIRECTIONS:
        return name
    if name.endswith("_extension") or name.endswith("_feature") or name.endswith("_feature_extension"):
        return name
    if name.endswith("_powered") or name.endswith("_powered_extension"):
        return name
    return None


def format_json(payload):
    text = json.dumps(payload, indent=2)
    pattern = re.compile(r"\[\n((?:\s*-?\d+(?:\.\d+)?(?:e[+-]?\d+)?\s*,?\n)+)\s*\]")

    def repl(match):
        inner = match.group(1)
        lines = [line.strip().rstrip(",") for line in inner.strip().splitlines() if line.strip()]
        return "[" + ", ".join(lines) + "]"

    text = pattern.sub(repl, text)

    face_pattern = re.compile(r'"(north|south|east|west|up|down)": \{\n([^{}]*?)\n\s*\}', re.DOTALL)

    def face_repl(match):
        face = match.group(1)
        inner = match.group(2)
        entries = []
        for line in inner.splitlines():
            stripped = line.strip().rstrip(",")
            if stripped:
                entries.append(stripped)
        return f"\"{face}\": {{ " + ", ".join(entries) + " }"

    return face_pattern.sub(face_repl, text)


def main():
    if not BASE_DIR.exists():
        raise SystemExit(f"Missing {BASE_DIR}")
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    for bb_path in sorted(BASE_DIR.glob("*.bbmodel")):
        data = json.loads(bb_path.read_text())
        prefix = bb_path.stem.replace("-", "_")
        texture_name = texture_name_from_bbmodel(data, prefix)
        textures = {
            "texture": f"logistics:block/{texture_name}",
            "particle": f"logistics:block/{texture_name}",
        }

        outputs = {}
        for element in data.get("elements", []):
            name = element.get("name", "")
            suffix = suffix_for_element(name)
            if suffix is None:
                print(f"[bbmodel] Skip unknown element '{name}' in {bb_path.name}")
                continue
            converted = {
                "from": element["from"],
                "to": element["to"],
                "faces": convert_faces(element.get("faces", {})),
            }
            outputs.setdefault(suffix, []).append(converted)

        for suffix, elements in outputs.items():
            out_path = OUT_DIR / f"{prefix}_{suffix}.json"
            model = {
                "generated_by": "scripts/generate_bbmodel_models.py",
                "source": str(bb_path),
                "parent": "block/block",
                "textures": textures,
                "elements": elements,
            }
            out_path.write_text(format_json(model) + "\n")
            print(f"[bbmodel] Wrote {out_path}")


if __name__ == "__main__":
    main()
