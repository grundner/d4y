import { Roboto } from "next/font/google";

// Kein "use client": die Font-Instanz wird sowohl im Server-Layout (className) als auch
// im Client-Theme (fontFamily) verwendet und darf die RSC-Grenze nicht überschreiten.
export const roboto = Roboto({
  weight: ["300", "400", "500", "700"],
  subsets: ["latin"],
  display: "swap",
});
