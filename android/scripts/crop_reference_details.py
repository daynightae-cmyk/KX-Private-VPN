from PIL import Image
from pathlib import Path

SRC = Path(r"C:\Users\day night\Downloads\KX_VISUAL_REFERENCE_MASTER_20\references")
OUT = Path(r"K:\KX-Private-VPN\docs\evidence\visual-reference-20261108\ref-detail-crops")
OUT.mkdir(parents=True, exist_ok=True)

CROPS = {
    "14-main-home-vpn.png": {
        "header": (0, 0, 941, 230),
        "orb-status": (200, 210, 740, 580),
        "cta": (20, 540, 921, 660),
        "session": (20, 680, 921, 840),
        "quick": (20, 845, 921, 1070),
        "activity": (20, 1075, 921, 1410),
        "nav": (20, 1430, 921, 1620),
    },
    "18-tools-catalog-nine.png": {
        "header": (0, 0, 941, 230),
        "search-chips": (20, 230, 921, 480),
        "card-topright": (455, 505, 921, 680),
        "card-topleft": (20, 505, 455, 680),
        "storage-health": (20, 1215, 921, 1380),
        "nav": (20, 1430, 921, 1620),
    },
    "03-repair-lab-hub.png": {
        "header": (0, 0, 941, 230),
        "orb-hint": (150, 230, 800, 760),
        "grid": (20, 760, 921, 1370),
        "cta": (20, 1370, 921, 1550),
    },
}

for name, regions in CROPS.items():
    image = Image.open(SRC / name).convert("RGB")
    for label, box in regions.items():
        crop = image.crop(box)
        crop = crop.resize((crop.width * 2, crop.height * 2), Image.LANCZOS)
        crop.save(OUT / f"{name.split('-')[0]}-{label}.png")

print("done")
