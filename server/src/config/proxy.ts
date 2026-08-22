import http from 'http';
import https from 'https';
import { setGlobalDispatcher, ProxyAgent } from 'undici';
import { HttpsProxyAgent } from 'https-proxy-agent';
import logger from '../utils/logger';

/** Whatever proxy URL was found in the environment, in whichever casing. */
export function getProxyUrl(): string | undefined {
  return process.env.HTTPS_PROXY || process.env.https_proxy ||
    process.env.HTTP_PROXY || process.env.http_proxy;
}

/**
 * Node's global `fetch` (undici) does NOT automatically honor the standard
 * HTTP_PROXY/HTTPS_PROXY environment variables the way curl and most other
 * HTTP clients do. On networks that require an outbound proxy, this leaves
 * every fetch-based call (notably firebase-admin's token verification,
 * which fetches Google's public certs) hanging until it times out — while
 * curl to the exact same URL succeeds instantly.
 *
 * This is a no-op when no proxy is configured, so it's always safe to call
 * on startup regardless of network (office proxy, home network, Docker, CI).
 */
export function configureOutboundProxy(): void {
  const proxyUrl = getProxyUrl();

  if (!proxyUrl) return;

  setGlobalDispatcher(new ProxyAgent(proxyUrl));

  // Firestore's default transport is gRPC, which has its own proxy-detection
  // logic entirely separate from undici (@grpc/grpc-js reads process.env
  // directly — see its src/http_proxy.ts) and — critically — only checks the
  // lowercase grpc_proxy/https_proxy/http_proxy names, never the uppercase
  // ones. Mirror whatever we found into the lowercase vars so it's picked up
  // regardless of which casing the shell/OS actually set.
  if (!process.env.grpc_proxy) process.env.grpc_proxy = proxyUrl;
  if (!process.env.https_proxy) process.env.https_proxy = proxyUrl;
  if (!process.env.http_proxy) process.env.http_proxy = proxyUrl;

  // Firestore's REST fallback transport (used when settings.preferRest is
  // true — needed because gRPC's streaming RunQuery call hangs indefinitely
  // through this proxy even though simple gRPC calls work fine once
  // grpc_proxy above is set) goes through google-gax's fallback client,
  // which uses the `node-fetch` package — a THIRD HTTP client, distinct from
  // both undici and grpc-js, with its own separate proxy blind spot.
  // node-fetch v2 delegates to Node's core http/https modules and honors
  // their default global agent when no per-request agent is specified
  // (which google-gax doesn't), so pointing the global agents at the proxy
  // fixes it for node-fetch and any other library with the same gap.
  const proxyAgent = new HttpsProxyAgent(proxyUrl);
  http.globalAgent = proxyAgent as unknown as http.Agent;
  https.globalAgent = proxyAgent as unknown as https.Agent;

  logger.info({ proxyUrl }, 'Outbound fetch, gRPC, and REST-fallback requests routed through proxy');
}
