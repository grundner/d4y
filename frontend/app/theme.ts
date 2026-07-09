"use client";

import { createTheme } from "@mui/material/styles";
import { plexSans, plexMono, spectral } from "./fonts";

// Dark-Theme mit bewusster Blau-/Lila-Palette gemäß ADR-0015 (Referenz-Design „D4Y m9r").
// Anpassung auf Theme-Ebene: die MUI-Komponentenstruktur bleibt erhalten.
const theme = createTheme({
  palette: {
    mode: "dark",
    primary: { main: "#4db8ff", light: "#6ac4ff", contrastText: "#06202e" },
    secondary: { main: "#b28dff", light: "#c3a9ff", contrastText: "#160a2e" },
    success: { main: "#5fd0a8" },
    warning: { main: "#e0a94a" },
    error: { main: "#f4776b" },
    info: { main: "#6ac4ff" },
    background: { default: "#0e1013", paper: "#161b22" },
    text: { primary: "#e6eaf0", secondary: "#8b93a1" },
    divider: "#262c35",
  },
  typography: {
    fontFamily: plexSans.style.fontFamily,
    h1: { fontFamily: spectral.style.fontFamily },
    h2: { fontFamily: spectral.style.fontFamily },
    h3: { fontFamily: spectral.style.fontFamily },
    h4: { fontFamily: spectral.style.fontFamily },
    h5: { fontFamily: spectral.style.fontFamily },
    h6: { fontFamily: spectral.style.fontFamily },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        code: { fontFamily: plexMono.style.fontFamily },
      },
    },
    MuiButton: { styleOverrides: { root: { textTransform: "none" } } },
    MuiTab: { styleOverrides: { root: { textTransform: "none" } } },
  },
});

export default theme;
