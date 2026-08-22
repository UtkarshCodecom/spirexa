FROM node:20-alpine AS base

# Install dependencies only when needed
FROM base AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app
COPY package.json package-lock.json* ./
COPY packages/types/package.json ./packages/types/
COPY web/package.json ./web/
RUN npm ci --workspace=web --workspace=@photos/types

# Build the types package
FROM deps AS build-types
COPY packages/types/ ./packages/types/
RUN npm run build --workspace=@photos/types

# Build the web app
FROM base AS web-build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY --from=build-types /app/packages/types/dist ./packages/types/dist
COPY web/ ./web/
COPY packages/types/package.json ./packages/types/
RUN npm run build --workspace=web

# Production web image
FROM base AS web
WORKDIR /app
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nodejs

COPY --from=web-build /app/web/.next/standalone ./
COPY --from=web-build /app/web/.next/static ./web/.next/static
COPY --from=web-build /app/web/public ./web/public

USER nodejs

EXPOSE 3000
ENV NODE_ENV=production

CMD ["node", "web/server.js"]
