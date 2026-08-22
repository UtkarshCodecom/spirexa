import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  transpilePackages: ['@photos/types'],
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: '**.bunnycdn.com',
      },
      {
        protocol: 'https',
        hostname: 'storage.googleapis.com',
      },
      {
        protocol: 'https',
        hostname: 'firebasestorage.googleapis.com',
      },
    ],
  },
};

export default nextConfig;
