import { db } from '../../config/firebase';
import { DocumentSnapshot } from 'firebase-admin/firestore';

/**
 * Shares live in a top-level collection (not nested under users/{uid} like
 * everything else) because an anonymous visitor has no uid to scope a lookup
 * by — they only have the share id itself. The Admin SDK bypasses Firestore
 * security rules entirely, so this is safe: the only door into this
 * collection is our own server code, which enforces ownership on every
 * write and only ever exposes a deliberately-limited read for GET-by-id.
 */

export interface ShareData {
  id: string;
  ownerId: string;
  title: string;
  mediaIds: string[];
  createdAt: Date;
}

export interface CreateShareData {
  id: string;
  ownerId: string;
  title: string;
  mediaIds: string[];
}

// A function, not a top-level const: `db` isn't assigned until
// initializeFirebase() runs in server.ts, which happens after every module's
// imports (including this one) have already resolved.
function sharesCollection() {
  return db.collection('shares');
}

export async function create(data: CreateShareData): Promise<ShareData> {
  const shareData = {
    ownerId: data.ownerId,
    title: data.title,
    mediaIds: data.mediaIds,
    createdAt: new Date(),
  };

  await sharesCollection().doc(data.id).set(shareData);

  return { id: data.id, ...shareData };
}

export async function findById(shareId: string): Promise<ShareData | null> {
  const doc = await sharesCollection().doc(shareId).get();
  if (!doc.exists) {
    return null;
  }
  return docToShare(doc);
}

export async function findByOwner(uid: string): Promise<ShareData[]> {
  // Deliberately just an equality filter (no orderBy) so this never needs a
  // manually-provisioned Firestore composite index — sort in memory instead.
  // A single user's share count is always small, so this is cheap.
  const snapshot = await sharesCollection().where('ownerId', '==', uid).get();

  const shares: ShareData[] = [];
  snapshot.forEach((doc) => shares.push(docToShare(doc)));
  shares.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
  return shares;
}

export async function remove(shareId: string): Promise<boolean> {
  const docRef = sharesCollection().doc(shareId);
  const doc = await docRef.get();
  if (!doc.exists) {
    return false;
  }
  await docRef.delete();
  return true;
}

function docToShare(doc: DocumentSnapshot): ShareData {
  const data = doc.data()!;
  return {
    id: doc.id,
    ownerId: data.ownerId,
    title: data.title,
    mediaIds: data.mediaIds || [],
    createdAt: data.createdAt?.toDate?.() || new Date(data.createdAt),
  };
}
