#!/usr/bin/env python3
"""PWA icons of VeriVoto from the shield of shared/brand-logo (viewBox 0 0 48 56). Needs rsvg-convert.
Usage: python3 scripts/brand/pwa-icons.py <output dir>"""
import subprocess, sys, pathlib

NAVY = '#0f2a47'      # --ion-color-tertiary, theme_color of the manifest
WHITE = '#ffffff'     # --ion-color-tertiary-contrast (outline of the logo on dark backgrounds)
ACCENT = '#5fd3a1'    # --vv-brand-accent (check)
SHIELD = ('<path d="M24 3 L43 10 V27 C43 40 34 49 24 53 C14 49 5 40 5 27 V10 Z" stroke="{w}"/>'
          '<path d="M16 15 H32" stroke="{w}"/><path d="M15 31 L22 38 L34 23" stroke="{a}"/>')

def svg(size, shield_height, rounded):
    s = shield_height * size / 56          # scale of the 48x56 drawing
    x = (size - 48 * s) / 2
    y = (size - 56 * s) / 2
    radius = size * 0.22 if rounded else 0
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 {size} {size}">'
            f'<rect width="{size}" height="{size}" rx="{radius:.2f}" fill="{NAVY}"/>'
            f'<g transform="translate({x:.3f} {y:.3f}) scale({s:.5f})" fill="none" stroke-width="3.5" '
            f'stroke-linecap="round" stroke-linejoin="round">'
            + SHIELD.format(w=WHITE, a=ACCENT) + '</g></svg>')

# purpose "any": rounded square, shield at 64 % of the height
# purpose "maskable": full-bleed square, shield at 52 % so it stays inside the 80 % safe zone
# apple-touch-icon: full square (iOS rounds it), shield at 60 %
ICONS = [(f'icon-{n}x{n}.png', n, 0.64, True) for n in (72, 96, 128, 144, 152, 192, 384, 512)]
ICONS += [(f'icon-maskable-{n}x{n}.png', n, 0.52, False) for n in (192, 512)]
ICONS += [('apple-touch-icon.png', 180, 0.60, False)]

out = pathlib.Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
for name, size, height, rounded in ICONS:
    source = out / (name[:-4] + '.svg')
    source.write_text(svg(size, height, rounded), encoding='utf-8')
    subprocess.run(['rsvg-convert', '-w', str(size), '-h', str(size), '-o', str(out / name), str(source)], check=True)
    source.unlink()
    print(name, size)
