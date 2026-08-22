import { db } from '../../config/firebase';
import { DocumentSnapshot } from 'firebase-admin/firestore';

export interface AlbumData {
  id: string;
  ownerId: string;
  title: string;
  description?: string;
  coverMediaId?: string;
  mediaCount: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateAlbumData {
  id: string;
  ownerId: string;
  title: string;
  description?: string;
  coverMediaId?: string;
}

export interface UpdateAlbumData {
  title?: string;
  description?: string;
  coverMediaId?: string;
}

export interface AlbumMediaData {
  mediaId: string;
  addedAt: Date;
}

function getAlbumsCollection(uid: string) {
  return db.collection('users').doc(uid).collection('albums');
}

function getAlbumMediaCollection(uid: string, albumId: string) {
  return getAlbumsCollection(uid).doc(albumId).collection('media');
}

export async function findByOwner(
  uid: string
): Promise<AlbumData[]> {
  const snapshot = await getAlbumsCollection(uid)
    .orderBy('createdAt', 'desc')
    .get();

  const albums: AlbumData[] = [];
  snapshot.forEach((doc) => {
    albums.push(docToAlbum(doc));
  });

  return albums;
}

export async function findById(
  uid: string,
  albumId: string
): Promise<AlbumData | null> {
  const doc = await getAlbumsCollection(uid).doc(albumId).get();
  if (!doc.exists) {
    return null;
  }
  return docToAlbum(doc);
}

export async function create(data: CreateAlbumData): Promise<AlbumData> {
  const now = new Date();
  const albumData = {
    ...data,
    mediaCount: 0,
    createdAt: now,
    updatedAt: now,
  };

  await getAlbumsCollection(data.ownerId).doc(data.id).set(albumData);

  return albumData;
}

export async function update(
  uid: string,
  albumId: string,
  data: UpdateAlbumData
): Promise<AlbumData | null> {
  const docRef = getAlbumsCollection(uid).doc(albumId);
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
  return docToAlbum(updatedDoc);
}

export async function remove(uid: string, albumId: string): Promise<boolean> {
  const docRef = getAlbumsCollection(uid).doc(albumId);
  const doc = await docRef.get();

  if (!doc.exists) {
    return false;
  }

  // Delete all media in the album
  const mediaSnapshot = await getAlbumMediaCollection(uid, albumId).get();
  const batch = db.batch();

  mediaSnapshot.forEach((doc) => {
    batch.delete(doc.ref);
  });

  batch.delete(docRef);
  await batch.commit();

  return true;
}

export async function addMedia(
  uid: string,
  albumId: string,
  mediaId: string
): Promise<boolean> {
  const albumDoc = await getAlbumsCollection(uid).doc(albumId).get();

  if (!albumDoc.exists) {
    return false;
  }

  const mediaRef = getAlbumMediaCollection(uid, albumId).doc(mediaId);
  const mediaDoc = await mediaRef.get();

  if (mediaDoc.exists) {
    return true; // Already in album
  }

  const batch = db.batch();

  batch.set(mediaRef, {
    mediaId,
    addedAt: new Date(),
  });

  batch.update(getAlbumsCollection(uid).doc(albumId), {
    mediaCount: (albumDoc.data()?.mediaCount || 0) + 1,
    updatedAt: new Date(),
  });

  await batch.commit();
  return true;
}

export async function removeMedia(
  uid: string,
  albumId: string,
  mediaId: string
): Promise<boolean> {
  const albumDoc = await getAlbumsCollection(uid).doc(albumId).get();

  if (!albumDoc.exists) {
    return false;
  }

  const mediaRef = getAlbumMediaCollection(uid, albumId).doc(mediaId);
  const mediaDoc = await mediaRef.get();

  if (!mediaDoc.exists) {
    return false;
  }

  const batch = db.batch();

  batch.delete(mediaRef);

  batch.update(getAlbumsCollection(uid).doc(albumId), {
    mediaCount: Math.max(0, (albumDoc.data()?.mediaCount || 1) - 1),
    updatedAt: new Date(),
  });

  await batch.commit();
  return true;
}

export async function getAlbumMedia(
  uid: string,
  albumId: string
): Promise<AlbumMediaData[]> {
  const snapshot = await getAlbumMediaCollection(uid, albumId)
    .orderBy('addedAt', 'desc')
    .get();

  const media: AlbumMediaData[] = [];
  snapshot.forEach((doc) => {
    const data = doc.data();
    media.push({
      mediaId: data.mediaId,
      addedAt: data.addedAt?.toDate?.() || new Date(data.addedAt),
    });
  });

  return media;
}

function docToAlbum(doc: DocumentSnapshot): AlbumData {
  const data = doc.data()!;
  return {
    id: doc.id,
    ownerId: data.ownerId,
    title: data.title,
    description: data.description,
    coverMediaId: data.coverMediaId,
    mediaCount: data.mediaCount || 0,
    createdAt: data.createdAt?.toDate?.() || new Date(data.createdAt),
    updatedAt: data.updatedAt?.toDate?.() || new Date(data.updatedAt),
  };
}
