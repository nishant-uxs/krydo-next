"""Generate Google-friendly PNG/ICO favicons from Krydo brand mark."""
from __future__ import annotations

import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "client", "public")


def make_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    radius = max(2, int(size * 14 / 64))
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=(11, 18, 32, 255))

    s = size / 64.0
    shield = [
        (32 * s, 10 * s),
        (48 * s, 16.5 * s),
        (48 * s, 30 * s),
        (32 * s, 51.5 * s),
        (16 * s, 30 * s),
        (16 * s, 16.5 * s),
    ]
    d.polygon(shield, fill=(37, 99, 235, 255))
    top = [
        (32 * s, 10 * s),
        (48 * s, 16.5 * s),
        (48 * s, 24 * s),
        (16 * s, 24 * s),
        (16 * s, 16.5 * s),
    ]
    d.polygon(top, fill=(96, 165, 250, 180))

    w = max(2, int(3.2 * s))
    pts = [(24 * s, 31.5 * s), (30 * s, 37.5 * s), (41 * s, 26.5 * s)]
    d.line(pts, fill=(255, 255, 255, 255), width=w, joint="curve")
    rcap = max(1, w // 2)
    for p in pts:
        d.ellipse([p[0] - rcap, p[1] - rcap, p[0] + rcap, p[1] + rcap], fill=(255, 255, 255, 255))
    return img


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    named = {
        48: "favicon-48.png",
        96: "favicon-96.png",
        180: "apple-touch-icon.png",
        192: "icon-192.png",
        512: "icon-512.png",
    }
    for sz, name in named.items():
        path = os.path.join(OUT, name)
        make_icon(sz).save(path, "PNG")
        print("wrote", path)

    logo_path = os.path.join(OUT, "logo.png")
    make_icon(512).save(logo_path, "PNG")
    print("wrote", logo_path)

    ico_path = os.path.join(OUT, "favicon.ico")
    make_icon(48).save(
        ico_path,
        format="ICO",
        sizes=[(16, 16), (32, 32), (48, 48)],
    )
    print("wrote", ico_path)
    print("done")


if __name__ == "__main__":
    main()
