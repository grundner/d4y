import type { Metadata } from "next";
import * as React from "react";
import "./globals.css";
import { roboto } from "./fonts";
import Providers from "./providers";

export const metadata: Metadata = {
  title: "D4Y — Git-native Runtime Platform",
  description: "Statuskontrolle und operative Aktionen für die D4Y-Plattform",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="de" className={roboto.className}>
      <body style={{ margin: 0 }}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
