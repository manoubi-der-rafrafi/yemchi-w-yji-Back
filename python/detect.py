import re
import sys
import warnings
from io import BytesIO
from pathlib import Path

from PIL import Image
from transformers import pipeline
from transformers.utils import logging as hf_logging

# =========================
# CONFIG
# =========================
IMAGE_PATH = Path(__file__).resolve().parent / "download.jpg"
MODEL_NAME = "Salesforce/blip-image-captioning-base"
MAX_NEW_TOKENS = 30


# =========================
# EXTRACTION OBJET
# =========================
def extract_first_object(caption: str) -> str:
    """
    Retourne uniquement l'objet principal détecté dans la phrase BLIP.
    """
    if not caption or not caption.strip():
        return "objet inconnu"

    c = caption.lower().strip()

    # enlever article au début
    c = re.sub(r"^(a|an|the)\s+", "", c)

    # cas fréquent: "person using / holding ..."
    m = re.match(r"^(person|man|woman)\s+(using|holding|carrying)\s+(a|an|the)?\s*(.+)$", c)
    if m:
        c = m.group(4).strip()

    # mots qui introduisent contexte ou autre objet
    split_words = [
        " and ", " with ", " next to ", " beside ", ",",
        " on ", " in ", " at ", " near ", " inside ", " outside ",
        " sitting ", " lying ", " standing ", " placed ", " using "
    ]

    for w in split_words:
        if w in c:
            c = c.split(w)[0].strip()
            break

    # nettoyage final
    c = re.sub(r"[^a-z0-9\s\-]", "", c)
    c = re.sub(r"\s+", " ", c).strip()

    return c if c else "objet inconnu"


# =========================
# MAIN
# =========================
def _load_image():
    try:
        data = sys.stdin.buffer.read()
        if data:
            return Image.open(BytesIO(data)).convert("RGB")
    except Exception:
        pass

    if not IMAGE_PATH.exists():
        return None

    return Image.open(IMAGE_PATH).convert("RGB")


def main():
    warnings.filterwarnings("ignore", category=FutureWarning)
    hf_logging.set_verbosity_error()

    img = _load_image()
    if img is None:
        print("objet inconnu")
        return

    try:
        captioner = pipeline(
            task="image-to-text",
            model=MODEL_NAME,
            device=-1  # CPU
        )

        out = captioner(img, max_new_tokens=MAX_NEW_TOKENS)
        caption = out[0]["generated_text"] if out else ""

        objet = extract_first_object(caption)

        # 🔥 SORTIE FINALE (SEULEMENT L'OBJET)
        print(objet)

    except KeyboardInterrupt:
        pass
    except Exception:
        print("objet inconnu")


if __name__ == "__main__":
    main()
