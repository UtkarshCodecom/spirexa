import admin from 'firebase-admin';
import { config } from './index';
import { getProxyUrl } from './proxy';

let adminApp: admin.app.App;
let db: admin.firestore.Firestore;
let auth: admin.auth.Auth;

// preferRest: on this project's intermittently-proxied network, plain
// Firestore *point reads* (.doc().get(), used by e.g. shares) work fine over
// gRPC once the proxy env vars are mirrored to lowercase (see config/proxy.ts),
// but Firestore *queries* (.where()/.orderBy(), used by media/album listing)
// use gRPC's streaming RunQuery RPC under the hood — and this proxy hangs
// indefinitely on that streamed response even though it handles plain
// request/response gRPC calls fine. REST-transport queries are a normal
// chunked HTTP response instead of an HTTP/2 stream, which the proxy handles
// correctly. Confirmed by isolating exactly this: `.doc().get()` succeeded
// consistently (with or without this setting) while `.where().orderBy()`
// hung indefinitely, on a fresh server process, every time, until this was
// added.
//
// Only forced on when a proxy is actually configured, though — off-proxy
// (e.g. plain home/hotspot networks), REST-via-node-fetch turned out to be
// the less reliable transport for the larger paginated media-list query,
// failing with ERR_STREAM_PREMATURE_CLOSE where plain gRPC does not.
const FIRESTORE_SETTINGS = { ignoreUndefinedProperties: true, preferRest: !!getProxyUrl() };

export function initializeFirebase(): admin.app.App {
  if (admin.apps.length > 0) {
    adminApp = admin.apps[0]!;
    db = adminApp.firestore();
    db.settings(FIRESTORE_SETTINGS);
    auth = adminApp.auth();
    return adminApp;
  }

  const hasCredentials =
    config.firebase.projectId &&
    config.firebase.clientEmail &&
    config.firebase.privateKey;

  if (hasCredentials) {
    adminApp = admin.initializeApp({
      credential: admin.credential.cert({
        projectId: config.firebase.projectId,
        clientEmail: config.firebase.clientEmail,
        privateKey: config.firebase.privateKey.replace(/\\n/g, '\n'),
      }),
    });
  } else {
    // For local development with Firebase emulator or default credentials
    adminApp = admin.initializeApp({
      projectId: config.firebase.projectId,
    });
  }

  db = adminApp.firestore();
  db.settings(FIRESTORE_SETTINGS);
  auth = adminApp.auth();

  return adminApp;
}

export { admin, db, auth };
