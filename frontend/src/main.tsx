import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";
import { ToastProvider } from "@/components/ui/toast";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { initMonitoring } from "@/lib/monitoring";
import "./index.css";

// 15.10.2026 — aplicația s-a mutat de pe app.wastehouse.ro pe www.eco360.ro. Redirectul e aici, nu pe
// server: `serve -s` nu știe de host, iar fișierele statice (`/brand/wastehouse-mark.png` din mailurile
// vechi) trebuie să rămână servite și pe vechiul domeniu. Calea și parametrii se păstrează, deci și
// linkurile de resetare a parolei trimise înainte de mutare. Sesiunea nu trece: tokenul e în `localStorage`.
if (window.location.hostname === "app.wastehouse.ro") {
  const { pathname, search, hash } = window.location;
  window.location.replace(`https://www.eco360.ro${pathname}${search}${hash}`);
}

// P0.6 — înaintea oricărei randări, ca o excepție din primul render să aibă unde ajunge. Fără
// `VITE_SENTRY_DSN` nu pornește nimic. Vezi `lib/monitoring.ts`.
initMonitoring();

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        {/* A doua plasă, pentru ce cade în afara paginilor: `Layout`, `AuthProvider`, routerul.
            Acolo nu mai e niciun meniu de păstrat, deci mesajul ia ecranul întreg. */}
        <ErrorBoundary fullPage>
          <App />
        </ErrorBoundary>
      </ToastProvider>
    </QueryClientProvider>
  </React.StrictMode>
);
