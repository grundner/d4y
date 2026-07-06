"use client";

import { createTheme } from "@mui/material/styles";
import { roboto } from "./fonts";

// Helles Standard-MUI-Theme (Default-Palette). Nur die Button-Großschreibung wird
// abgeschaltet, damit die Beschriftungen dem Design entsprechen.
const theme = createTheme({
  palette: { mode: "light" },
  typography: { fontFamily: roboto.style.fontFamily },
  components: {
    MuiButton: { styleOverrides: { root: { textTransform: "none" } } },
    MuiTab: { styleOverrides: { root: { textTransform: "none" } } },
  },
});

export default theme;
