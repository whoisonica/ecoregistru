import { Platform } from "react-native";

import type { MovementSummary } from "@web/types";

import type { AuthResponse } from "./auth";

/**
 * Backendul local din profilul dev. Emulatorul Android vede Mac-ul la 10.0.2.2, simulatorul iOS la localhost.
 * `EXPO_PUBLIC_API_URL` bate amândouă (telefon real în aceeași rețea).
 */
const BASE_URL =
  process.env.EXPO_PUBLIC_API_URL ??
  (Platform.OS === "android" ? "http://10.0.2.2:8080" : "http://localhost:8080");

/** 401 pe o cerere cu token = sesiunea a expirat; ecranul scoate omul din cont, ca pe web. */
export class UnauthorizedError extends Error {}

export class ApiError extends Error {
  constructor(readonly status: number) {
    super(`HTTP ${status}`);
  }
}

async function request<T>(path: string, init: RequestInit & { token?: string | null } = {}): Promise<T> {
  const { token, headers, ...rest } = init;
  const res = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(headers as Record<string, string>),
    },
  });
  if (res.status === 401 && token) throw new UnauthorizedError();
  if (!res.ok) throw new ApiError(res.status);
  return (await res.json()) as T;
}

export function login(email: string, password: string) {
  return request<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function movementSummary(token: string, year: number, month: number) {
  return request<MovementSummary>(`/api/v1/movements/summary?year=${year}&month=${month}`, { token });
}
