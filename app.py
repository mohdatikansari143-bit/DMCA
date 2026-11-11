import os
import uuid
from pathlib import Path
from fastapi import FastAPI, UploadFile, File, Header, HTTPException
from fastapi.staticfiles import StaticFiles

app = FastAPI(title="Trex Wallet Media Service")
STORAGE = Path(os.getenv("MEDIA_STORAGE_DIR", "./uploads")).resolve()
STORAGE.mkdir(parents=True, exist_ok=True)
API_KEY = os.getenv("MEDIA_API_KEY", "")
PUBLIC_BASE = os.getenv("PUBLIC_MEDIA_BASE_URL", "http://localhost:8001").rstrip("/")
MAX_BYTES = 50 * 1024 * 1024
ALLOWED = {
    "image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp", "image/gif": ".gif",
    "video/mp4": ".mp4", "video/webm": ".webm", "video/quicktime": ".mov"
}
app.mount("/media", StaticFiles(directory=str(STORAGE)), name="media")

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/internal/media")
async def upload_media(file: UploadFile = File(...), x_media_api_key: str | None = Header(default=None)):
    if not API_KEY or x_media_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized media service request")
    content_type = (file.content_type or "").lower()
    extension = ALLOWED.get(content_type)
    if not extension:
        raise HTTPException(status_code=415, detail="Unsupported media type")
    data = await file.read(MAX_BYTES + 1)
    if not data:
        raise HTTPException(status_code=400, detail="Empty file")
    if len(data) > MAX_BYTES:
        raise HTTPException(status_code=413, detail="File exceeds 50 MB")
    # Generate our own filename; never trust a user-supplied path or filename.
    filename = f"{uuid.uuid4().hex}{extension}"
    destination = STORAGE / filename
    destination.write_bytes(data)
    media_type = "video" if content_type.startswith("video/") else "image"
    return {"url": f"{PUBLIC_BASE}/media/{filename}", "mediaType": media_type, "size": len(data)}
