from PIL import Image
from pathlib import Path

path = Path(r"K:\KX-Private-VPN\android\app\src\main\res\drawable-nodpi\kx_brand.png")
image = Image.open(path).convert("RGBA")
alpha = image.getchannel("A")
print("size", image.size)
print("alpha bbox", alpha.getbbox())
print("alpha extrema", alpha.getextrema())
w, h = image.size
cx, cy = w / 2 - 0.5, h / 2 - 0.5
for radius in (525, 526, 527, 528, 529, 530, 531, 531.5, 532, 533, 535, 539, 540):
    samples = []
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (0.707, 0.707), (-0.707, 0.707)):
        x = int(round(cx + dx * radius))
        y = int(round(cy + dy * radius))
        if 0 <= x < w and 0 <= y < h:
            samples.append(alpha.getpixel((x, y)))
    print(radius, samples)
