import { config } from '../config';
import logger from '../utils/logger';

export interface PhotoAnalysis {
  caption: string;
  tags: string[];
  isDocument: boolean;
  documentText?: string;
}

const PROMPT =
  'Analyze this photo for a personal photo library app. Respond with ONLY JSON, no markdown, ' +
  'matching exactly this shape: {"caption": string, "tags": string[], "isDocument": boolean, "documentText": string}. ' +
  '"caption" is one short plain sentence describing the photo, written for search (e.g. "Two friends at a beach at sunset"). ' +
  '"tags" is 5 to 8 short lowercase search keywords (subjects, setting, objects, mood). ' +
  '"isDocument" is true only if this is primarily a photographed or scanned document — an ID card, certificate, ' +
  'receipt, invoice, form, or page of text — and false for photos of people, places, food, or scenery. ' +
  '"documentText" is the visible text transcribed verbatim if isDocument is true, otherwise an empty string.';

/**
 * One Gemini Flash call per photo, in JSON mode, doing search-captioning and
 * document detection together — one cheap request instead of two. Images
 * only; the model name defaults to the cheapest current Flash tier, and a
 * blank API key makes this a no-op rather than an error, so AI features are
 * entirely optional to enable.
 */
export async function analyzePhoto(imageBytes: Buffer, mimeType: string): Promise<PhotoAnalysis | null> {
  if (!config.gemini.apiKey) return null;

  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${config.gemini.model}:generateContent?key=${config.gemini.apiKey}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [
          {
            parts: [
              { text: PROMPT },
              { inlineData: { mimeType, data: imageBytes.toString('base64') } },
            ],
          },
        ],
        generationConfig: {
          responseMimeType: 'application/json',
          maxOutputTokens: 300,
          temperature: 0.2,
        },
      }),
    });

    if (!response.ok) {
      logger.warn({ status: response.status, body: await response.text() }, 'Gemini photo analysis request failed');
      return null;
    }

    const json = (await response.json()) as {
      candidates?: { content?: { parts?: { text?: string }[] } }[];
    };
    const text = json.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) return null;

    const parsed = JSON.parse(text) as Partial<PhotoAnalysis>;
    const isDocument = Boolean(parsed.isDocument);
    return {
      caption: String(parsed.caption || '').slice(0, 300),
      tags: Array.isArray(parsed.tags)
        ? parsed.tags.slice(0, 8).map((t) => String(t).toLowerCase().trim()).filter(Boolean)
        : [],
      isDocument,
      documentText: isDocument && parsed.documentText ? String(parsed.documentText).slice(0, 4000) : undefined,
    };
  } catch (error) {
    logger.warn({ error }, 'Gemini photo analysis failed');
    return null;
  }
}
