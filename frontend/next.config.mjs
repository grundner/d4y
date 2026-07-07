/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Standard-MUI-Design; Lint blockiert den Build nicht.
  eslint: { ignoreDuringBuilds: true },
  // Proxy zum D4Y-Backend: der Browser ruft /api/* same-origin, Next leitet weiter
  // (server-seitig, daher kein CORS). Ziel via D4Y_BACKEND_URL überschreibbar.
  async rewrites() {
    const backend = process.env.D4Y_BACKEND_URL || "http://localhost:8080";
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;
