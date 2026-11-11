# Trex Wallet Posts Backend (Java + Python)

This is a starter backend for the two frontend pages:
- `Posts.html`: `GET /api/posts`
- `CreatePost.html`: `POST /api/posts` (multipart form: `title`, `description`, `link`, optional `media`)

## Architecture
- Java 17 + Spring Boot: REST API, Telegram Mini App `initData` verification, PostgreSQL persistence.
- Python + FastAPI: media upload/storage service.
- PostgreSQL: durable post records.
- Socket.IO: Java emits `post:created` after a post is saved.

## 1. Requirements
- Java 17+
- Maven 3.9+
- Python 3.10+
- PostgreSQL 14+
- Telegram bot token from BotFather

## 2. Configure environment
Copy `.env.example` to `.env` and export its values in your shell, or configure them in your hosting provider.
Never commit the real bot token or database password.

Required:
- `TELEGRAM_BOT_TOKEN`: your Telegram bot token.
- `DATABASE_URL`: JDBC URL, e.g. `jdbc:postgresql://localhost:5432/trex_wallet`
- `DB_USERNAME`, `DB_PASSWORD`
- `MEDIA_SERVICE_URL`: normally `http://localhost:8001`
- `PUBLIC_MEDIA_BASE_URL`: public base URL of the Python media service, e.g. `http://localhost:8001`
- `MEDIA_API_KEY`: same random secret in Java and Python.
- `SOCKETIO_PORT`: default `9092`

Create the PostgreSQL database:
```sql
CREATE DATABASE trex_wallet;
```

## 3. Run Python media service
```bash
cd python-media
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
export MEDIA_API_KEY="replace-with-a-long-random-secret"
export MEDIA_STORAGE_DIR="./uploads"
export PUBLIC_MEDIA_BASE_URL="http://localhost:8001"
uvicorn app:app --host 0.0.0.0 --port 8001
```
Uploaded files are stored under `python-media/uploads/` and served at `/media/<filename>`.
For production, use persistent object storage (S3-compatible storage, Cloudinary, etc.) rather than an ephemeral app filesystem.

## 4. Run Java API
From `java-backend/`:
```bash
mvn spring-boot:run
```
The Java API runs on port 5000. On first run, JPA creates the `posts` table (`ddl-auto=update`).
Socket.IO runs on port 9092.

## 5. Connect frontend
The provided HTML files currently use `window.location.origin` as the API base.
If the pages are hosted separately from this backend, add this before their page scripts:
```html
<script>window.TREX_API_BASE = "https://YOUR-JAVA-API-DOMAIN";</script>
```
The frontend's Socket.IO connection currently uses the same API base and expects a Socket.IO-compatible server on that host. For this starter, Socket.IO listens on port `9092`; configure the frontend socket URL separately if needed:
```js
const SOCKET_URL = window.TREX_SOCKET_URL || API_BASE;
```
Then point `TREX_SOCKET_URL` to `https://YOUR-SOCKET-DOMAIN` (or your reverse-proxy path). The frontend currently uses `API_BASE` directly, so update that one constant to `window.TREX_SOCKET_URL || API_BASE` when deploying on separate ports/domains.

## 6. Telegram identity security
Open the Mini App from Telegram. The frontend sends `Telegram.WebApp.initData` in the `X-Telegram-Init-Data` header.
Java verifies Telegram's HMAC signature and freshness, then uses the verified `user.id`, `username`, `first_name`, `last_name`, and `photo_url`.
Do not accept Telegram user ID or username from ordinary form fields or trust `initDataUnsafe` for authorization.

Set the bot's Mini App URL to your deployed frontend URL in BotFather.
For local browser testing, the API will reject requests without valid Telegram initData; this is expected.

## API
- `GET /api/health`
- `GET /api/posts` returns `{ "posts": [...] }`
- `POST /api/posts` multipart form with `title`, `description`, `link`, optional `media`; requires valid Telegram initData.

## Important production work still required
- Put Java API, Python media service and Socket.IO behind HTTPS and a reverse proxy.
- Configure exact CORS origins instead of `*`.
- Add rate limiting, moderation/reporting, pagination, upload quotas, malware scanning, and backups.
- Use durable media storage and CDN.
- Do not expose the media service's upload endpoint publicly without its API key.
- Add Telegram bot token rotation and secret management.
