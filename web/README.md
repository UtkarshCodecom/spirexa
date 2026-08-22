# Photos — Web App

The browser companion to the Android app: sign in with the same Firebase account to browse, search, and manage the same library, plus the destination for public share links opened outside the app.

## What it does

- **Full library browsing** — photos, albums, favorites, trash, search — backed by the same API server and Firestore data as Android.
- **Public share links** (`/share/[id]`) — no login required. When someone opens a link shared from the Android app, this is what renders it: a read-only gallery of exactly the photos that were shared, nothing else in the library.
- **Auth** via Firebase (email/password), same project as Android — a user signed in on their phone can sign in here with the same credentials and see the same library.

## Tech stack

Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS, Firebase JS SDK.

## Setup

```bash
cp .env.local.example .env.local
```

| Variable | Notes |
|---|---|
| `NEXT_PUBLIC_FIREBASE_*` | From Firebase console → Project settings → General → Your apps → Web app config. Safe to be public — these identify the project, they don't authorize anything by themselves. |
| `NEXT_PUBLIC_API_URL` | The API server's base URL (e.g. `http://localhost:3002` locally) |

```bash
npm install
npm run dev     # http://localhost:3000
npm run build
npm start
npm run typecheck
```

## How it talks to the server

Every authenticated request attaches the Firebase ID token from the client SDK as `Authorization: Bearer <token>` — identical contract to the Android app, verified the same way server-side (see [server/README.md](../server/README.md#security)). The web app never talks to Backblaze B2 or Gemini directly; every media byte and every AI-derived caption/tag comes back through the API server, already unwrapped from its `{ success, data }` response envelope by `lib/api.ts`.

Media URLs returned by the API are relative paths carrying a short-lived signed token (`/api/media/:id/content?token=...`) — the web app prefixes them with `NEXT_PUBLIC_API_URL` before use in any `<img>`/`<video>` tag.

## Structure

```
web/
├── app/
│   ├── (protected)/     # requires auth: photos, albums, search, favorites, trash, settings, shared
│   ├── login/ signup/   # auth pages
│   └── share/[id]/      # public, no-login share-link view
├── components/
├── hooks/
├── lib/                 # api.ts (server client), firebase.ts
└── types/
```
