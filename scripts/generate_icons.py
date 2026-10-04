"""Generates the Car Motion Sickness Aid logo and launcher icons.

The glyph is Material Symbols Outlined "car_fan_recirculate_2" (FILL 0, wght 400, GRAD 0,
opsz 24) with its recirculation arrow removed, and Material Symbols "lens_blur" placed inside the
car body where the arrow was — the blurred dot field doubling as the app's on-screen motion cues.

Outputs follow the sibling apps' conventions: #2A7FFF glyph on white, a monochrome variant for
themed icons, a white status-bar icon, assets/logo.svg, and design/ PNGs for the Play Store.

Usage: python scripts/generate_icons.py   (needs resvg-py: pip install resvg-py)
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
BLUE = "#2A7FFF"

# car_fan_recirculate_2 minus its first subpath (the arrow), viewBox 0 -960 960 960.
CAR = (
    "M260-160q-41 0-75-21.5T134-240H0v-240q0-33 23.5-56.5T80-560h40l60-167q9-24 29.5-38.5"
    "T256-780h298q21 0 39.5 10.5T623-741l97 161 167 14q33 3 53 25t20 55v246H826q-17 37-51 58.5"
    "T700-160q-41 0-75-21.5T574-240H386q-17 37-51 58.5T260-160Zm0-80q25 0 42.5-17.5T320-300v-20"
    "h320v20q0 25 17.5 42.5T700-240q25 0 42.5-17.5T760-300v-20h120v-166l-208-18-117-196H256"
    "l-80 220H80v160h120v20q0 25 17.5 42.5T260-240Z"
)

# lens_blur as its dots: (cx, cy, r) in the same 960 viewBox.
LENS_BLUR = [(400, -840, 20), (560, -840, 20)]
LENS_BLUR += [(x, -720, 40) for x in (240, 400, 560, 720)]
for _y in (-560, -400):
    LENS_BLUR += [(120, _y, 20), (240, _y, 40), (400, _y, 60), (560, _y, 60), (720, _y, 40), (840, _y, 20)]
LENS_BLUR += [(x, -240, 40) for x in (240, 400, 560, 720)]
LENS_BLUR += [(400, -120, 20), (560, -120, 20)]

# Where lens_blur sits inside the car: centered on the cabin/body opening, scaled to clear the
# outline on every side.
BLUR_CENTER = (440.0, -500.0)
BLUR_SCALE = 0.40


def blur_path():
    cx, cy = BLUR_CENTER
    parts = []
    for x, y, r in LENS_BLUR:
        X = cx + (x - 480) * BLUR_SCALE
        Y = cy + (y + 480) * BLUR_SCALE
        R = r * BLUR_SCALE
        parts.append(f"M{X - R:.1f},{Y:.1f}a{R:.1f},{R:.1f} 0 1,0 {2 * R:.1f},0a{R:.1f},{R:.1f} 0 1,0 {-2 * R:.1f},0Z")
    return "".join(parts)


GLYPH = CAR + blur_path()


def vector(width_dp, viewport, body):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{width_dp}dp"\n'
        f'    android:height="{width_dp}dp"\n'
        f'    android:viewportWidth="{viewport}"\n'
        f'    android:viewportHeight="{viewport}">\n'
        f"{body}"
        "</vector>\n"
    )


def path(color, indent="    "):
    return (
        f"{indent}<path\n"
        f'{indent}    android:fillColor="{color}"\n'
        f'{indent}    android:pathData="{GLYPH}" />\n'
    )


def group(scale, tx, ty, color):
    return (
        "    <group\n"
        f'        android:scaleX="{scale}"\n'
        f'        android:scaleY="{scale}"\n'
        f'        android:translateX="{tx}"\n'
        f'        android:translateY="{ty}">\n'
        f"{path(color, '        ')}"
        "    </group>\n"
    )


def write(rel, text):
    target = RES / rel if not rel.startswith("/") else ROOT / rel[1:]
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8", newline="\n")


def main():
    # Adaptive icon: 108dp canvas with the glyph inside the 66dp safe zone (same placement as
    # the sibling apps).
    write("drawable/ic_launcher_foreground.xml", vector(108, 1200, group(0.5625, 330, 870, BLUE)))
    write("drawable/ic_launcher_monochrome.xml", vector(108, 1200, group(0.5625, 330, 870, "#FFFFFFFF")))
    write("drawable-nodpi/splash_icon.xml", vector(108, 1200, group(0.5625, 330, 870, BLUE)))
    write(
        "drawable/ic_launcher_background.xml",
        vector(108, 108, '    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="M0,0h108v108h-108z" />\n'),
    )
    write(
        "drawable-nodpi/ic_app_icon.xml",
        vector(
            48,
            1200,
            '    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="M0,0h1200v1200h-1200z" />\n'
            + group(0.8125, 210, 990, BLUE),
        ),
    )
    write("drawable-nodpi/ic_logo_mark.xml", vector(48, 960, '    <group android:translateY="960">\n' + path(BLUE, "        ") + "    </group>\n"))
    write("drawable/ic_stat_cues.xml", vector(24, 960, '    <group android:translateY="960">\n' + path("#FFFFFFFF", "        ") + "    </group>\n"))
    adaptive = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@drawable/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
        "</adaptive-icon>\n"
    )
    write("mipmap-anydpi-v26/ic_launcher.xml", adaptive)
    write("mipmap-anydpi-v26/ic_launcher_round.xml", adaptive)

    logo = f'<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="{BLUE}"><path d="{GLYPH}"/></svg>\n'
    write("/assets/logo.svg", logo)

    import resvg_py

    def png(svg, out):
        (ROOT / "design").mkdir(exist_ok=True)
        (ROOT / "design" / out).write_bytes(bytes(resvg_py.svg_to_bytes(svg_string=svg)))

    # Play Store icon: full-bleed white square, glyph at the same proportion as the launcher icon.
    png(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 1200 1200">'
        f'<rect width="1200" height="1200" fill="#fff"/>'
        f'<g transform="translate(210 990) scale(0.8125)"><path fill="{BLUE}" d="{GLYPH}"/></g></svg>',
        "logo-playstore-512.png",
    )
    png(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="960" height="960" viewBox="0 -960 960 960">'
        f'<path fill="{BLUE}" d="{GLYPH}"/></svg>',
        "logo-source.png",
    )


if __name__ == "__main__":
    main()
