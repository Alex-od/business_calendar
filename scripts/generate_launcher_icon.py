#!/usr/bin/env python3
"""Generate Android launcher icon assets from ic_launcher_source.png."""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
SOURCE = RES / "drawable-nodpi" / "ic_launcher_source.png"

# Adaptive icon foreground canvas (xxxhdpi: 108 dp * 4)
FOREGROUND_SIZE = 432
SAFE_ZONE_RATIO = 0.88

LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def crop_icon_only(img: Image.Image) -> Image.Image:
    """Keep the notebook graphic; drop the caption rendered below it."""
    width, height = img.size
    center_left = int(width * 0.2)
    center_right = int(width * 0.8)
    center_span = center_right - center_left
    last_content_row = 0
    crop_height = height

    for y in range(height):
        row_opaque = sum(
            1
            for x in range(center_left, center_right)
            if img.getpixel((x, y))[3] > 20
        )
        if row_opaque > center_span * 0.15:
            last_content_row = y
        elif last_content_row > height * 0.25 and row_opaque == 0:
            crop_height = last_content_row + 8
            break

    return img.crop((0, 0, width, crop_height))


def trim_transparent(img: Image.Image) -> Image.Image:
    bbox = img.getbbox()
    return img.crop(bbox) if bbox else img


def fit_on_canvas(icon: Image.Image, canvas_size: int) -> Image.Image:
    safe = int(canvas_size * SAFE_ZONE_RATIO)
    icon = icon.copy()
    icon.thumbnail((safe, safe), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    offset = ((canvas_size - icon.width) // 2, (canvas_size - icon.height) // 2)
    canvas.paste(icon, offset, icon)
    return canvas


def main() -> None:
    if not SOURCE.exists():
        raise SystemExit(f"Source not found: {SOURCE}")

    source = Image.open(SOURCE).convert("RGBA")
    cropped = trim_transparent(crop_icon_only(source))

    foreground = fit_on_canvas(cropped, FOREGROUND_SIZE)
    drawable_dir = RES / "drawable"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    foreground.save(drawable_dir / "ic_launcher_foreground.webp", "WEBP", quality=95)

    for folder, size in LEGACY_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        legacy = fit_on_canvas(cropped, size)
        legacy.save(out_dir / "ic_launcher.png", "PNG")
        legacy.save(out_dir / "ic_launcher_round.png", "PNG")

    print("Generated launcher icons:")
    print(f"  {drawable_dir / 'ic_launcher_foreground.webp'}")
    for folder in LEGACY_SIZES:
        print(f"  {RES / folder / 'ic_launcher.png'}")
        print(f"  {RES / folder / 'ic_launcher_round.png'}")


if __name__ == "__main__":
    main()
