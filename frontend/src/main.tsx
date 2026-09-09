import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";
import { ToastProvider } from "@/components/ui/toast";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { initMonitoring } from "@/lib/monitoring";
import "./index.css";

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
