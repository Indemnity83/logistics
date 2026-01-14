#!/usr/bin/env python3
import argparse
import json
from pathlib import Path
from urllib.error import URLError
from urllib.request import Request, urlopen

from PIL import Image, ImageDraw, ImageFont

SLOT_FILL = (139, 139, 139, 255)
TEXT_COLOR = (0, 0, 0, 255)

ICON_SIZE = 32
TITLE_PADDING_Y = 30
TITLE_FONT_SIZE_BASE = 10
TITLE_FONT_SCALE = 3
TITLE_TOP_PADDING = 10
INFO_PADDING_Y = 100
INFO_TOP_PADDING = 8
INFO_LINE_SPACING = 2
INFO_FONT_SCALE = TITLE_FONT_SCALE
FONT_URL = "https://www.minecraft.net/etc.clientlibs/minecraftnet/clientlibs/clientlib-site/resources/fonts/Minecraft-Seven_v2.woff2"
FONT_FILENAME = "Minecraft-Seven_v2.woff2"
TAG_FALLBACKS = {
    "minecraft:planks": "minecraft:oak_planks",
}
TAG_VARIANTS = {
    "minecraft:planks": [
        "minecraft:oak_planks",
        "minecraft:spruce_planks",
        "minecraft:birch_planks",
        "minecraft:jungle_planks",
        "minecraft:acacia_planks",
        "minecraft:dark_oak_planks",
        "minecraft:mangrove_planks",
        "minecraft:cherry_planks",
        "minecraft:bamboo_planks",
        "minecraft:crimson_planks",
        "minecraft:warped_planks",
    ],
}
ITEM_INFO = {
    "logistics:copper_transport_pipe": "Reliable, all-purpose transport. Moves items at standard speed.",
    "logistics:stone_transport_pipe": "A simple starter pipe. Slower, but easy to craft.",
    "logistics:gold_transport_pipe": "Gives items a speed boost when powered.",
    "logistics:item_merger_pipe": "Funnels everything into one chosen output.",
    "logistics:item_extractor_pipe": "Pulls items from the selected side.",
    "logistics:item_void_pipe": "Safely deletes anything that enters.",
    "logistics:item_filter_pipe": "Sorts items using per-side filters.",
    "logistics:item_insertion_pipe": "Prefers inserting into inventories with space.",
    "logistics:wrench": "Adjusts pipes and cycles settings.",
}


def parse_ingredient(value):
    if isinstance(value, str):
        if value.startswith("#"):
            return {"tag": value[1:]}
        return {"item": value}
    if isinstance(value, dict):
        if "item" in value:
            return {"item": value["item"]}
        if "tag" in value:
            tag = value["tag"]
            if isinstance(tag, str) and tag.startswith("#"):
                tag = tag[1:]
            return {"tag": tag}
    return {}


def build_grid(recipe):
    grid = [[None for _ in range(3)] for _ in range(3)]
    if recipe.get("type") == "minecraft:crafting_shaped":
        pattern = recipe.get("pattern", [])
        if not pattern:
            return grid
        height = len(pattern)
        width = max(len(row) for row in pattern)
        offset_x = max(0, (3 - width) // 2)
        offset_y = max(0, (3 - height) // 2)
        key = recipe.get("key", {})
        for row_idx, row in enumerate(pattern):
            for col_idx, symbol in enumerate(row):
                if symbol == " ":
                    continue
                ingredient = parse_ingredient(key.get(symbol, {}))
                x = col_idx + offset_x
                y = row_idx + offset_y
                if 0 <= x < 3 and 0 <= y < 3:
                    grid[y][x] = ingredient
        return grid

    if recipe.get("type") == "minecraft:crafting_shapeless":
        ingredients = recipe.get("ingredients", [])
        idx = 0
        for ingredient in ingredients:
            x = idx % 3
            y = idx // 3
            if y >= 3:
                break
            grid[y][x] = parse_ingredient(ingredient)
            idx += 1
    return grid


def icon_path(icons_dir, item_id):
    namespace, path = item_id.split(":", 1)
    return icons_dir / f"{namespace}__{path}.png"


def resolve_tag(tag):
    if tag in TAG_VARIANTS:
        return TAG_VARIANTS[tag]
    fallback = TAG_FALLBACKS.get(tag)
    if fallback:
        return [fallback]
    return []


def expand_grid_variants(grid):
    variant_tag = None
    for row in grid:
        for ingredient in row:
            if ingredient and ingredient.get("tag") in TAG_VARIANTS:
                variant_tag = ingredient["tag"]
                break
        if variant_tag:
            break

    if not variant_tag:
        return [(grid, None)]

    variants = TAG_VARIANTS[variant_tag]
    expanded = []
    for item_id in variants:
        new_grid = []
        for row in grid:
            new_row = []
            for ingredient in row:
                if ingredient and ingredient.get("tag") == variant_tag:
                    new_row.append({"item": item_id})
                else:
                    new_row.append(ingredient)
            new_grid.append(new_row)
        expanded.append((new_grid, human_name(item_id)))
    return expanded


def draw_missing_icon(draw, box, label):
    x0, y0, x1, y1 = box
    draw.rectangle(box, outline=(160, 50, 50, 255), fill=(230, 200, 200, 255))
    draw.line((x0, y0, x1, y1), fill=(160, 50, 50, 255), width=2)
    draw.line((x0, y1, x1, y0), fill=(160, 50, 50, 255), width=2)
    draw.text((x0 + 3, y0 + 3), label, fill=(120, 40, 40, 255))


def paste_icon(canvas, icon, box):
    x0, y0, x1, y1 = box
    target_size = (x1 - x0, y1 - y0)
    if icon.size != target_size:
        icon = icon.resize(target_size, Image.NEAREST)
    canvas.paste(icon, (x0, y0), icon)


def paste_icon_centered(canvas, icon, box):
    x0, y0, x1, y1 = box
    box_w = x1 - x0
    box_h = y1 - y0
    icon_w, icon_h = icon.size
    if icon_w != box_w or icon_h != box_h:
        raise SystemExit(
            f"Icon size {icon_w}x{icon_h} does not match slot {box_w}x{box_h}. "
            "Re-export icons at the correct size."
        )
    left = x0 + (box_w - icon_w) // 2
    top = y0 + (box_h - icon_h) // 2
    canvas.paste(icon, (left, top), icon)


def text_size(font, text, scale=1):
    bbox = font.getbbox(text)
    width = bbox[2] - bbox[0]
    height = bbox[3] - bbox[1]
    return width * scale, height * scale


def draw_text_bitmap(image, position, text, font, fill, scale=1):
    bbox = font.getbbox(text)
    width = bbox[2] - bbox[0]
    height = bbox[3] - bbox[1]
    mask = Image.new("1", (width, height), 0)
    draw = ImageDraw.Draw(mask)
    draw.text((-bbox[0], -bbox[1]), text, font=font, fill=1)
    mask_l = mask.convert("L")
    color = Image.new("RGBA", (width, height), fill)
    if scale != 1:
        color = color.resize((width * scale, height * scale), Image.NEAREST)
        mask_l = mask_l.resize((width * scale, height * scale), Image.NEAREST)
    image.paste(color, position, mask_l)


def wrap_text(font, text, max_width, scale=1):
    words = text.split()
    if not words:
        return []
    lines = []
    current = words[0]
    for word in words[1:]:
        candidate = f"{current} {word}"
        width, _ = text_size(font, candidate, scale)
        if width <= max_width:
            current = candidate
        else:
            lines.append(current)
            current = word
    lines.append(current)
    return lines


def ensure_font(font_path):
    if font_path.exists():
        return font_path
    font_path.parent.mkdir(parents=True, exist_ok=True)
    try:
        request = Request(FONT_URL, headers={"User-Agent": "Mozilla/5.0"})
        with urlopen(request, timeout=20) as response:
            data = response.read()
    except URLError as exc:
        raise SystemExit(f"Failed to download font: {exc}") from exc
    font_path.write_bytes(data)
    return font_path


def find_boxes(image, target, min_size=20):
    width, height = image.size
    pix = image.load()
    visited = [[False] * width for _ in range(height)]
    boxes = []

    for y in range(height):
        for x in range(width):
            if visited[y][x] or pix[x, y] != target:
                continue
            min_x = max_x = x
            min_y = max_y = y
            stack = [(x, y)]
            visited[y][x] = True
            while stack:
                cx, cy = stack.pop()
                if cx < min_x:
                    min_x = cx
                if cx > max_x:
                    max_x = cx
                if cy < min_y:
                    min_y = cy
                if cy > max_y:
                    max_y = cy
                for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                    if 0 <= nx < width and 0 <= ny < height and not visited[ny][nx]:
                        if pix[nx, ny] == target:
                            visited[ny][nx] = True
                            stack.append((nx, ny))
            if (max_x - min_x) >= min_size and (max_y - min_y) >= min_size:
                boxes.append((min_x, min_y, max_x + 1, max_y + 1))
    return boxes


def template_boxes(template):
    boxes = find_boxes(template, SLOT_FILL)
    slot_boxes = []
    result_boxes = []
    for x0, y0, x1, y1 in boxes:
        w = x1 - x0
        h = y1 - y0
        if 60 <= w <= 70 and 60 <= h <= 70:
            slot_boxes.append((x0, y0, x1, y1))
        elif w >= 80 and h >= 80:
            result_boxes.append((x0, y0, x1, y1))
    slot_boxes.sort(key=lambda b: (b[1], b[0]))
    result_box = result_boxes[0] if result_boxes else None
    return slot_boxes, result_box


def offset_box(box, dx, dy):
    x0, y0, x1, y1 = box
    return (x0 + dx, y0 + dy, x1 + dx, y1 + dy)


def human_name(item_id):
    namespace, path = item_id.split(":", 1)
    return path.replace("_", " ").title()


def render_card(
    recipe_path,
    icons_dir,
    result_icons_dir,
    out_dir,
    template_path,
    font_path,
):
    recipe = json.loads(recipe_path.read_text())
    result = recipe.get("result", {})
    result_id = result.get("id")
    if not result_id:
        return None

    count = int(result.get("count", 1))
    grid = build_grid(recipe)

    font = ImageFont.truetype(str(font_path), TITLE_FONT_SIZE_BASE)
    template = Image.open(template_path).convert("RGBA")
    bg_color = template.getpixel((0, 0))
    image = Image.new(
        "RGBA",
        (template.width, template.height + TITLE_PADDING_Y + INFO_PADDING_Y),
        bg_color,
    )
    image.paste(template, (0, TITLE_PADDING_Y))
    draw = ImageDraw.Draw(image)

    slot_boxes, result_box = template_boxes(template)
    if len(slot_boxes) < 9 or not result_box:
        raise SystemExit(f"Template missing slots/result: {template_path}")
    slot_boxes = [offset_box(box, 0, TITLE_PADDING_Y) for box in slot_boxes]
    result_box = offset_box(result_box, 0, TITLE_PADDING_Y)

    title = human_name(result_id)
    _, title_height = text_size(font, title, TITLE_FONT_SCALE)
    title_x = min(box[0] for box in slot_boxes)
    title_y = (TITLE_PADDING_Y - title_height) // 2 + TITLE_TOP_PADDING
    draw_text_bitmap(
        image,
        (title_x, max(2, title_y)),
        title,
        font,
        TEXT_COLOR,
        TITLE_FONT_SCALE,
    )

    info_text = ITEM_INFO.get(result_id, "")
    info_left = title_x
    info_top = TITLE_PADDING_Y + template.height + INFO_TOP_PADDING
    info_width = image.width - info_left - 12
    line_height = text_size(font, "A", INFO_FONT_SCALE)[1] + INFO_LINE_SPACING
    if info_text:
        lines = wrap_text(font, info_text, info_width, INFO_FONT_SCALE)
        max_lines = max(1, (INFO_PADDING_Y - INFO_TOP_PADDING * 2) // line_height)
        for idx, line in enumerate(lines[:max_lines]):
            draw_text_bitmap(
                image,
                (info_left, info_top + idx * line_height),
                line,
                font,
                TEXT_COLOR,
                INFO_FONT_SCALE,
            )

    outputs = []
    frames = []
    for grid_variant, suffix in expand_grid_variants(grid):
        card = image.copy()
        card_draw = ImageDraw.Draw(card)

        for row in range(3):
            for col in range(3):
                slot_box = slot_boxes[row * 3 + col]

                ingredient = grid_variant[row][col]
                if not ingredient:
                    continue

                icon_box = slot_box

                if "item" in ingredient:
                    item_icon_path = icon_path(icons_dir, ingredient["item"])
                    if item_icon_path.exists():
                        icon = Image.open(item_icon_path).convert("RGBA")
                        paste_icon_centered(card, icon, icon_box)
                    else:
                        draw_missing_icon(card_draw, icon_box, "?")
                elif "tag" in ingredient:
                    resolved = resolve_tag(ingredient["tag"])
                    if resolved:
                        item_icon_path = icon_path(icons_dir, resolved[0])
                        if item_icon_path.exists():
                            icon = Image.open(item_icon_path).convert("RGBA")
                            paste_icon_centered(card, icon, icon_box)
                        else:
                            draw_missing_icon(card_draw, icon_box, "?")
                    else:
                        draw_missing_icon(card_draw, icon_box, "tag")

        result_icon_path = icon_path(result_icons_dir, result_id)
        if not result_icon_path.exists():
            result_icon_path = icon_path(icons_dir, result_id)
        if result_icon_path.exists():
            icon = Image.open(result_icon_path).convert("RGBA")
            paste_icon_centered(card, icon, result_box)
        else:
            draw_missing_icon(card_draw, result_box, "?")

        if count > 1:
            count_text = f"{count}"
            count_w, count_h = text_size(font, count_text, TITLE_FONT_SCALE)
            count_x = result_box[2] - count_w - 6
            count_y = result_box[3] - count_h - 5
            draw_text_bitmap(
                card,
                (count_x, count_y),
                count_text,
                font,
                (52, 48, 48, 255),
                TITLE_FONT_SCALE,
            )
            draw_text_bitmap(
                card,
                (count_x - 3, count_y - 3),
                count_text,
                font,
                (255, 255, 255, 255),
                TITLE_FONT_SCALE,
            )

        if suffix is None:
            out_dir.mkdir(parents=True, exist_ok=True)
            out_name = f"{human_name(result_id)}.png"
            out_path = out_dir / out_name
            card.save(out_path)
            outputs.append(out_path)
        frames.append(card)
    if len(frames) > 1:
        gif_name = f"{human_name(result_id)}.gif"
        gif_path = out_dir / gif_name
        gif_frames = [frame.convert("P", palette=Image.ADAPTIVE) for frame in frames]
        gif_frames[0].save(
            gif_path,
            save_all=True,
            append_images=gif_frames[1:],
            duration=2000,
            loop=0,
            disposal=2,
        )
        outputs.append(gif_path)
    return outputs


def main():
    parser = argparse.ArgumentParser(description="Generate crafting recipe cards.")
    parser.add_argument(
        "--recipes-dir",
        type=Path,
        default=Path("src/main/resources/data/logistics/recipe"),
        help="Directory containing recipe JSON files.",
    )
    parser.add_argument(
        "--icons-dir",
        type=Path,
        default=Path("run/icon-exports-x64"),
        help="Directory containing exported 64px icon PNGs.",
    )
    parser.add_argument(
        "--result-icons-dir",
        type=Path,
        default=Path("run/icon-exports-x88"),
        help="Directory containing exported 88px result icon PNGs.",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=Path("assets/recipie-cards"),
        help="Output directory for recipe cards.",
    )
    parser.add_argument(
        "--template",
        type=Path,
        default=Path("assets/art/crafting-grid.png"),
        help="Background template PNG for the recipe card.",
    )
    parser.add_argument(
        "--font",
        type=Path,
        default=Path("scripts/fonts") / FONT_FILENAME,
        help="Path to the pixel font (WOFF2) for titles.",
    )

    args = parser.parse_args()
    if not args.icons_dir.exists():
        raise SystemExit(f"Missing icons dir: {args.icons_dir}")
    if not args.template.exists():
        raise SystemExit(f"Missing template: {args.template}")
    if not args.result_icons_dir.exists():
        raise SystemExit(f"Missing result icons dir: {args.result_icons_dir}")
    font_path = ensure_font(args.font)
    recipe_paths = sorted(args.recipes_dir.glob("*.json"))

    if not recipe_paths:
        raise SystemExit(f"No recipes found in {args.recipes_dir}")

    for recipe_path in recipe_paths:
        out_paths = render_card(
            recipe_path,
            args.icons_dir,
            args.result_icons_dir,
            args.out_dir,
            args.template,
            font_path,
        )
        for out_path in out_paths:
            print(f"[recipe] Wrote {out_path}")


if __name__ == "__main__":
    main()
