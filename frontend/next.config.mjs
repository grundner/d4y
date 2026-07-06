/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Standard-MUI-Design; Lint blockiert den Build nicht.
  eslint: { ignoreDuringBuilds: true },
};

export default nextConfig;
