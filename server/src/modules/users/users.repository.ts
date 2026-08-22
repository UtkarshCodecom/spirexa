import { db } from '../../config/firebase';
import { DocumentSnapshot, FieldValue } from 'firebase-admin/firestore';

const USERS_COLLECTION = 'users';

export interface UserData {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  storageUsedBytes: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface UpdateUserData {
  displayName?: string;
  photoURL?: string;
}

// A lazy accessor, not a module-level const: db is only assigned once
// initializeFirebase() runs in server.ts, which is after this module (and
// every route/controller that imports it) has already been evaluated.
function usersCollection() {
  return db.collection(USERS_COLLECTION);
}

export async function findByUid(uid: string): Promise<UserData | null> {
  const doc = await usersCollection().doc(uid).get();
  if (!doc.exists) {
    return null;
  }
  return docToUser(doc);
}

export async function create(data: UserData): Promise<UserData> {
  const now = new Date();
  const userData = {
    ...data,
    createdAt: now,
    updatedAt: now,
  };
  await usersCollection().doc(data.uid).set(userData);
  return userData;
}

export async function update(
  uid: string,
  data: UpdateUserData
): Promise<UserData | null> {
  const docRef = usersCollection().doc(uid);
  const doc = await docRef.get();

  if (!doc.exists) {
    return null;
  }

  const updateData = {
    ...data,
    updatedAt: new Date(),
  };

  await docRef.update(updateData);
  const updatedDoc = await docRef.get();
  return docToUser(updatedDoc);
}

export async function remove(uid: string): Promise<boolean> {
  const docRef = usersCollection().doc(uid);
  const doc = await docRef.get();

  if (!doc.exists) {
    return false;
  }

  await docRef.delete();
  return true;
}

export async function getStorageUsedBytes(uid: string): Promise<number> {
  const doc = await usersCollection().doc(uid).get();
  return doc.data()?.storageUsedBytes || 0;
}

/** Atomically adjusts the user's tracked storage usage by [deltaBytes] (can be negative). */
export async function adjustStorageUsed(uid: string, deltaBytes: number): Promise<void> {
  await usersCollection().doc(uid).set(
    {
      storageUsedBytes: FieldValue.increment(deltaBytes),
      updatedAt: new Date(),
    },
    { merge: true }
  );
}

function docToUser(doc: DocumentSnapshot): UserData {
  const data = doc.data()!;
  return {
    uid: doc.id,
    email: data.email || null,
    displayName: data.displayName || null,
    photoURL: data.photoURL || null,
    storageUsedBytes: data.storageUsedBytes || 0,
    createdAt: data.createdAt?.toDate?.() || new Date(data.createdAt),
    updatedAt: data.updatedAt?.toDate?.() || new Date(data.updatedAt),
  };
}
