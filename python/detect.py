import os
from pathlib import Path
from ultralytics import YOLO

IMAGE_PATH = str(Path(__file__).resolve().parent / "detect.png")
THRESHOLD = 0.70  # 75%

def main():
    if not os.path.exists(IMAGE_PATH):
        print("image n'est pas claire0")
        return

    try:
        model = YOLO("yolov8n.pt")
        results = model.predict(source=IMAGE_PATH, conf=0.25, verbose=False)

        detections = []
        for r in results:
            names = r.names
            if r.boxes:
                for b in r.boxes:
                    cls_id = int(b.cls[0])
                    confidence = float(b.conf[0])
                    label = names[cls_id]
                    detections.append((label, confidence))

        # 0 objet
        if len(detections) == 0:
            print("image n'est pas claire1")
            return

        # 1 objet
        if len(detections) == 1:
            label, conf = detections[0]
            if conf >= THRESHOLD:
                print(label)
            else:
                print("image n'est pas claire2")
            return

        # > 1 objet : prendre le plus confiant
        best_label, best_conf = max(detections, key=lambda x: x[1])
        if best_conf >= THRESHOLD:
            print(best_label)
        else:
            print("image n'est pas claire3")

    except Exception:
        print("image n'est pas claire4")

    finally:
        # supprimer l'image après traitement
        try:
            os.remove(IMAGE_PATH)
        except Exception:
            pass

if __name__ == "__main__":
    main()
