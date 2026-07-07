"use client";

// Aufrufe der operativen Backend-Endpunkte (ADR-0013) über den Next-Proxy.
// Kein Auth vorhanden; der Akteur wird als Header mitgegeben (Audit).

const ACTOR = "web-ui";

async function call(path: string, method: string, body?: unknown): Promise<any> {
  const headers: Record<string, string> = { "X-Actor": ACTOR };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export interface ExecResult {
  output: string;
  exitCode: number;
}

export interface ContainerDetails {
  id: string;
  name: string;
  image: string;
  state: string;
  status: string;
  createdAt: string;
  env: string[];
}

export const restartApp = (name: string) => call(`/api/apps/${name}/restart`, "POST");
export const stopApp = (name: string, durationSeconds: number) =>
  call(`/api/apps/${name}/stop`, "POST", { durationSeconds });
export const setParams = (name: string, env: Record<string, string>, durationSeconds: number) =>
  call(`/api/apps/${name}/params`, "POST", { env, durationSeconds });
export const setHold = (name: string, type: string, durationSeconds: number) =>
  call(`/api/apps/${name}/hold`, "POST", { type, durationSeconds });
export const releaseHold = (name: string) => call(`/api/apps/${name}/hold`, "DELETE");
export const execCmd = (name: string, cmd: string[]): Promise<ExecResult> =>
  call(`/api/apps/${name}/exec`, "POST", { cmd });
export const fetchLogs = (name: string, tail: number): Promise<{ output: string }> =>
  call(`/api/apps/${name}/logs?tail=${tail}`, "GET");
export const inspectApp = (name: string): Promise<ContainerDetails> => call(`/api/apps/${name}`, "GET");
