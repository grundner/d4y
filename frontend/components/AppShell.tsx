"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  AppBar,
  Box,
  Container,
  Divider,
  Drawer,
  IconButton,
  LinearProgress,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Switch,
  Toolbar,
  Tooltip,
  Typography,
} from "@mui/material";
import DashboardIcon from "@mui/icons-material/Dashboard";
import AppsIcon from "@mui/icons-material/Apps";
import AccountTreeIcon from "@mui/icons-material/AccountTree";
import HistoryIcon from "@mui/icons-material/History";
import SourceIcon from "@mui/icons-material/Source";
import RefreshIcon from "@mui/icons-material/Refresh";
import AccountCircleIcon from "@mui/icons-material/AccountCircle";
import { useD4y } from "@/lib/store";
import { CONFIG_REPO } from "@/lib/mockData";

const DRAWER_WIDTH = 250;

const NAV = [
  { href: "/", label: "Dashboard", Icon: DashboardIcon, match: (p: string) => p === "/" },
  { href: "/applications", label: "Applications", Icon: AppsIcon, match: (p: string) => p.startsWith("/applications") },
  { href: "/infrastructure", label: "Infrastruktur", Icon: AccountTreeIcon, match: (p: string) => p.startsWith("/infrastructure") },
  { href: "/activity", label: "Aktivität / Audit", Icon: HistoryIcon, match: (p: string) => p.startsWith("/activity") },
];

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() || "/";
  const { autoRefresh, toggleAutoRefresh, refreshing, manualRefresh } = useD4y();

  return (
    <Box sx={{ display: "flex", height: "100vh" }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: 2,
              bgcolor: "rgba(255,255,255,0.18)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontWeight: 700,
              fontSize: 15,
              letterSpacing: "0.5px",
              mr: 1.5,
            }}
          >
            D4
          </Box>
          <Box sx={{ lineHeight: 1.15 }}>
            <Typography variant="h6" sx={{ fontWeight: 500, lineHeight: 1.15 }}>
              D4Y
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.82 }}>
              Git-native Runtime Platform
            </Typography>
          </Box>
          <Box sx={{ flexGrow: 1 }} />
          <Tooltip title="Jetzt aktualisieren">
            <IconButton color="inherit" onClick={manualRefresh}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Box sx={{ display: "flex", alignItems: "center", mr: 1 }}>
            <Typography variant="body2" sx={{ opacity: 0.92 }}>
              Auto-Refresh
            </Typography>
            <Switch
              color="default"
              checked={autoRefresh}
              onChange={toggleAutoRefresh}
              inputProps={{ "aria-label": "Auto-Refresh umschalten" }}
            />
          </Box>
          <Divider orientation="vertical" flexItem sx={{ borderColor: "rgba(255,255,255,0.28)", mx: 1 }} />
          <Tooltip title="Konto (Platzhalter)">
            <IconButton color="inherit">
              <AccountCircleIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          "& .MuiDrawer-paper": { width: DRAWER_WIDTH, boxSizing: "border-box" },
        }}
      >
        <Toolbar />
        <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
          <List sx={{ flexGrow: 1 }}>
            {NAV.slice(0, 4).map((item) => (
              <ListItemButton
                key={item.href}
                component={Link}
                href={item.href}
                selected={item.match(pathname)}
              >
                <ListItemIcon sx={{ minWidth: 40 }}>
                  <item.Icon />
                </ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ))}
            <Divider sx={{ my: 1 }} />
            <ListItemButton component={Link} href="/config" selected={pathname.startsWith("/config")}>
              <ListItemIcon sx={{ minWidth: 40 }}>
                <SourceIcon />
              </ListItemIcon>
              <ListItemText primary="Config-Repository" />
            </ListItemButton>
          </List>
          <Divider />
          <Box sx={{ p: 2 }}>
            <Typography variant="overline" color="text.secondary" sx={{ letterSpacing: "0.07em" }}>
              Config-Version
            </Typography>
            <Typography sx={{ fontFamily: "monospace", fontSize: 13 }}>{CONFIG_REPO.version}</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {CONFIG_REPO.reconcile}
            </Typography>
          </Box>
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, height: "100vh", overflow: "auto", bgcolor: "grey.50" }}>
        <Toolbar />
        <Box sx={{ height: 4 }}>{refreshing && <LinearProgress />}</Box>
        <Container maxWidth="lg" sx={{ py: 3, pb: 8 }}>
          {children}
        </Container>
      </Box>
    </Box>
  );
}
