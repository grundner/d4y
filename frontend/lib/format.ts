export function formatCountdown(secs: number | null): string {
  if (secs == null) return "—";
  const s = Math.max(0, secs);
  const m = Math.floor(s / 60);
  const ss = s % 60;
  return `${m < 10 ? "0" : ""}${m}:${ss < 10 ? "0" : ""}${ss}`;
}

export function holdTypeLabel(type: string): string {
  if (type === "temp-param") return "Temporäre Parameter";
  if (type === "stop") return "Stop (mit Hold)";
  return "Hold";
}

/** Backend-Enum (STOP | TEMP_PARAM | MANUAL) auf ein lesbares Label abbilden. */
export function holdEnumLabel(enumName: string): string {
  return holdTypeLabel(enumName.toLowerCase().replace("_", "-"));
}

/** ISO-8601-Zeitpunkt → "HH:MM:SS" (lokale Zeit). Leer bei ungültiger Eingabe. */
export function formatClock(iso: string): string {
  const d = new Date(iso);
  return isNaN(d.getTime()) ? "" : d.toLocaleTimeString("de-DE", { hour12: false });
}

/** ISO-8601-Zeitpunkt → "YYYY-MM-DD HH:MM:SS" (lokale Zeit). Rohwert bei ungültiger Eingabe. */
export function formatTimestamp(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  const p = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
    `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  );
}
