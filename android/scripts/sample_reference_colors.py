from PIL import Image
from pathlib import Path

src = Path(r"C:\Users\day night\Downloads\KX_VISUAL_REFERENCE_MASTER_20\references")
for name in ("14-main-home-vpn.png", "18-tools-catalog-nine.png", "03-repair-lab-hub.png"):
    im = Image.open(src / name).convert("RGB")
    print(name, im.size)
    for label, box in {
        "topleft": (0, 0, 200, 200),
        "topright": (741, 0, 941, 200),
        "mid": (350, 600, 590, 800),
        "bottom": (200, 1500, 740, 1672),
        "card": (520, 540, 880, 640),
        "nav": (200, 1470, 740, 1580),
    }.items():
        crop = im.crop(box).resize((1, 1), Image.LANCZOS)
        print(" ", label, "#%02X%02X%02X" % crop.getpixel((0, 0)))
