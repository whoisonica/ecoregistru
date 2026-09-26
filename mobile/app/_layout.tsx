import { IBMPlexMono_400Regular, IBMPlexMono_500Medium } from "@expo-google-fonts/ibm-plex-mono";
import {
  IBMPlexSans_400Regular,
  IBMPlexSans_500Medium,
  IBMPlexSans_600SemiBold,
  useFonts,
} from "@expo-google-fonts/ibm-plex-sans";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { StatusBar } from "expo-status-bar";
import { useEffect } from "react";

import { initMonitoring, wrap } from "../src/monitoring";
import { SessionProvider, useSession } from "../src/session";

initMonitoring();
SplashScreen.preventAutoHideAsync();

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

export default wrap(RootLayout);

function RootLayout() {
  // Fonturile vin din pachetele @expo-google-fonts (fișiere TTF, OFL) legate în aplicație, nu descărcate de pe Google.
  const [fontsLoaded] = useFonts({
    IBMPlexSans_400Regular,
    IBMPlexSans_500Medium,
    IBMPlexSans_600SemiBold,
    IBMPlexMono_400Regular,
    IBMPlexMono_500Medium,
  });

  return (
    <QueryClientProvider client={queryClient}>
      <SessionProvider>
        <StatusBar style="light" />
        {fontsLoaded ? <Routes /> : null}
      </SessionProvider>
    </QueryClientProvider>
  );
}

function Routes() {
  const { session, ready } = useSession();

  useEffect(() => {
    if (ready) SplashScreen.hideAsync();
  }, [ready]);

  if (!ready) return null;
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Protected guard={!!session}>
        <Stack.Screen name="(tabs)" />
      </Stack.Protected>
      <Stack.Protected guard={!session}>
        <Stack.Screen name="login" />
      </Stack.Protected>
    </Stack>
  );
}
