import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";
import { ToastProvider } from "@/components/ui/toast";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import "./index.css";

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
