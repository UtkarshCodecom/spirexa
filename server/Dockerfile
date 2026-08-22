FROM node:20-alpine AS base

# Install dependencies only when needed
FROM base AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app
COPY package.json package-lock.json* ./
COPY packages/types/package.json ./packages/types/
RUN npm ci --workspace=@photos/types

# Build the types package
FROM deps AS build-types
COPY packages/types/ ./packages/types/
RUN npm run build --workspace=@photos/types

# Build the server
FROM base AS server-deps
WORKDIR /app
COPY package.json package-lock.json* ./
COPY packages/types/package.json ./packages/types/
COPY server/package.json ./server/
RUN npm ci --workspace=server --workspace=@photos/types

FROM server-deps AS server-build
COPY --from=build-types /app/packages/types/dist ./packages/types/dist
COPY server/ ./server/
RUN npm run build --workspace=server

# Production server image
FROM base AS server
WORKDIR /app
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nodejs

COPY --from=server-build /app/server/dist ./dist
COPY --from=server-build /app/server/package.json ./
COPY --from=server-build /app/node_modules ./node_modules

USER nodejs

EXPOSE 3001
ENV NODE_ENV=production

CMD ["node", "dist/server.js"]
