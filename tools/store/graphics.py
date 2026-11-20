#!/usr/bin/env python3
"""Generates the Google Play graphics (icon 512x512, feature graphic 1024x500) for every language.

The artwork mirrors app/src/main/res/drawable/ic_launcher_foreground.xml (white card with a
barcode on the launcher teal). Output follows the fastlane "supply" layout:

    store/<lang>/images/icon.png
    store/<lang>/images/featureGraphic.png

Run:  python3 tools/store/graphics.py
"""
import os
import re
import runpy

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FONT = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
SS = 4  # supersampling factor for smooth edges

TEAL = (0x00, 0x69, 0x5C)
DARK = (0x00, 0x4D, 0x40)
WHITE = (255, 255, 255)

# Same numbers as the launcher vector (108x108 viewport): card 27..81 x 36..72, bars y 44..64.
CARD = (27, 36, 81, 72)
CARD_RADIUS = 6
BARS = [(34, 3), (39, 1.5), (43, 4), (49, 1.5), (52.5, 3), (58, 1.5), (61.5, 4), (67.5, 1.5), (71, 3)]
BARS_Y = (44, 64)


def draw_card(draw, cx, cy, card_width):
    """Draws the card centered at (cx, cy) so that it is `card_width` px wide."""
    scale = card_width / (CARD[2] - CARD[0])
    ox = cx - (CARD[0] + CARD[2]) / 2 * scale
    oy = cy - (CARD[1] + CARD[3]) / 2 * scale

    def pt(x, y):
        return ox + x * scale, oy + y * scale

    draw.rounded_rectangle([pt(CARD[0], CARD[1]), pt(CARD[2], CARD[3])],
                           radius=CARD_RADIUS * scale, fill=WHITE)
    for x, width in BARS:
        draw.rectangle([pt(x, BARS_Y[0]), pt(x + width, BARS_Y[1])], fill=DARK)


def make_icon(path):
    size = 512 * SS
    image = Image.new("RGB", (size, size), TEAL)
    draw_card(ImageDraw.Draw(image), size / 2, size / 2, size * 0.62)
    image.resize((512, 512), Image.LANCZOS).save(path)


def make_feature(path, title):
    width, height = 1024 * SS, 500 * SS
    image = Image.new("RGB", (width, height), TEAL)
    draw = ImageDraw.Draw(image)
    # Subtle darker band on the right to give the banner some depth.
    draw.polygon([(width * 0.62, 0), (width, 0), (width, height), (width * 0.50, height)], fill=(0x00, 0x5A, 0x4E))
    draw_card(draw, width * 0.24, height / 2, width * 0.34)

    size = 78 * SS
    font = ImageFont.truetype(FONT, size)
    max_width = width * 0.44
    while draw.textlength(title, font=font) > max_width and size > 30 * SS:
        size -= 2 * SS
        font = ImageFont.truetype(FONT, size)
    # Wrap on a space if still too wide on one line (long names).
    words = title.split(" ")
    lines = [title]
    if len(words) > 1 and draw.textlength(title, font=font) > max_width * 0.85:
        lines = [" ".join(words[:-1]), words[-1]]
    line_height = size * 1.2
    top = height / 2 - line_height * len(lines) / 2
    for i, line in enumerate(lines):
        draw.text((width * 0.46, top + i * line_height), line, font=font, fill=WHITE)
    image.resize((1024, 500), Image.LANCZOS).save(path)


def main():
    ns = runpy.run_path(os.path.join(ROOT, "tools", "i18n", "strings.py"))
    langs, names = ns["LANGS"], ns["STRINGS"]["app_name"]
    for lang, name in zip(langs, names):
        folder = os.path.join(ROOT, "store", lang, "images")
        os.makedirs(folder, exist_ok=True)
        make_icon(os.path.join(folder, "icon.png"))
        make_feature(os.path.join(folder, "featureGraphic.png"), name)
        print(folder, name)


if __name__ == "__main__":
    main()
