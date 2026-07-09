import { IBM_Plex_Sans, IBM_Plex_Mono, Spectral } from "next/font/google";

// Kein "use client": die Font-Instanzen werden sowohl im Server-Layout (className) als auch
// im Client-Theme (fontFamily) verwendet und dürfen die RSC-Grenze nicht überschreiten.

// UI-/Body-Schrift.
export const plexSans = IBM_Plex_Sans({
  weight: ["400", "500", "600", "700"],
  subsets: ["latin"],
  display: "swap",
});

// Monospace für Code, Images/IDs und die Log-/exec-Viewer.
export const plexMono = IBM_Plex_Mono({
  weight: ["400", "500", "600"],
  subsets: ["latin"],
  display: "swap",
});

// Optionale Serifen-Schrift für Headings.
export const spectral = Spectral({
  weight: ["400", "500", "600"],
  subsets: ["latin"],
  display: "swap",
});
