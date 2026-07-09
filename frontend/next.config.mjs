/** @type {import('next').NextConfig} */

// D4Y_STATIC_EXPORT=1 aktiviert den statischen Export (out/), der vom Spring-Boot-
// Backend als statische Ressourcen ausgeliefert wird — kein Node zur Laufzeit
// (ADR-0006 Single-Image, ADR-0014). Der Gradle-Build setzt diese Variable.
const staticExport = process.env.D4Y_STATIC_EXPORT === "1";

const base = {
  reactStrictMode: true,
  // Standard-MUI-Design; Lint blockiert den Build nicht.
  eslint: { ignoreDuringBuilds: true },
};

const nextConfig = staticExport
  ? {
      ...base,
      // Statischer Export: HTML/JS/CSS nach out/. UI und /api kommen im Betrieb
      // vom selben Spring-Boot-Port → same-origin, kein Proxy, kein CORS.
      output: "export",
    }
  : {
      ...base,
      // Dev (next dev): der Browser ruft /api/* same-origin, Next leitet server-seitig
      // an das Backend weiter (kein CORS). Ziel via D4Y_BACKEND_URL überschreibbar.
      // rewrites() ist mit output:"export" nicht erlaubt und daher hier ausgeklammert.
      async rewrites() {
        const backend = process.env.D4Y_BACKEND_URL || "http://localhost:8080";
        return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
      },
    };

export default nextConfig;
