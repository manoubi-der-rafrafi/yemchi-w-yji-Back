from io import BytesIO

from fastapi import FastAPI, Request, Response
from PIL import Image
from transformers import pipeline
from transformers.utils import logging as hf_logging

import detect


app = FastAPI()
captioner = None


@app.on_event("startup")
def load_model():
    global captioner
    hf_logging.set_verbosity_error()
    try:
        captioner = pipeline(
            task="image-to-text",
            model=detect.MODEL_NAME,
            device=-1,
        )
        print("[serve] model loaded")
    except Exception as exc:
        captioner = None
        print(f"[serve] model load failed: {exc}")


@app.get("/health")
def health():
    return {"ok": captioner is not None}


@app.post("/detect")
async def detect_image(request: Request):
    if captioner is None:
        return Response("modele non disponible", status_code=503, media_type="text/plain")

    data = await request.body()
    if not data:
        return Response("image n'est pas claire", status_code=400, media_type="text/plain")

    try:
        img = Image.open(BytesIO(data)).convert("RGB")
    except Exception:
        return Response("image n'est pas claire", status_code=400, media_type="text/plain")

    try:
        out = captioner(img, max_new_tokens=detect.MAX_NEW_TOKENS)
        caption = out[0]["generated_text"] if out else ""
        objet = detect.extract_first_object(caption)
        if not objet:
            objet = "image n'est pas claire"
        return Response(objet, media_type="text/plain")
    except Exception as exc:
        print(f"[serve] detect failed: {exc}")
        return Response("image n'est pas claire", status_code=500, media_type="text/plain")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
