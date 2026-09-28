import { IBMPlexMono_400Regular, IBMPlexMono_500Medium } from "@expo-google-fonts/ibm-plex-mono";
import {
  IBMPlexSans_400Regular,
  IBMPlexSans_500Medium,
  IBMPlexSans_600SemiBold,
  useFonts,
} from "@expo-google-fonts/ibm-plex-sans";
import { focusManager, onlineManager, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as Network from "expo-network";
import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { StatusBar } from "expo-status-bar";
import { useEffect } from "react";
import { AppState, Platform } from "react-native";

import { initMonitoring, wrap } from "../src/monitoring";
import { SessionProvider, useSession } from "../src/session";

initMonitoring();
SplashScreen.preventAutoHideAsync();

const queryClient = new QueryClient({
  // `always`: fără semnal cererile pleacă oricum, fiindcă listele formularului cad pe copia din telefon
  // (`cached`). Modul implicit le-ar fi pus pe pauză odată ce `onlineManager` de mai jos știe că nu e rețea.
  defaultOptions: { queries: { retry: 1, staleTime: 30_000, networkMode: "always" }, mutations: { networkMode: "always" } },
});

// React Query știe singur când revine o fereastră de browser și când revine rețeaua, dar nu pe telefon:
// fără legăturile astea, ecranele arătau cifrele de dinainte de fundal sau de zona fără semnal până la
// o ieșire din ecran. Cu ele, la revenirea în față și la întoarcerea semnalului se reîncarcă ce e vechi.
focusManager.setEventListener((onFocus) => {
  if (Platform.OS === "web") return;
  const sub = AppState.addEventListener("change", (state) => onFocus(state === "active"));
  return () => sub.remove();
});
onlineManager.setEventListener((setOnline) => {
  const sub = Network.addNetworkStateListener((state) => setOnline(!!state.isConnected));
  return () => sub.remove();
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
        {/* F5: „Bifează” de pe Termene — foaia nativă de jos (iOS și Android), nu un modal desenat de mână. */}
        <Stack.Screen
          name="bifeaza"
          options={{ presentation: "formSheet", sheetAllowedDetents: [0.6, 1.0], sheetGrabberVisible: true, sheetCornerRadius: 24 }}
        />
        {/* F10: modul inspector pe tot ecranul, fără bara de jos. */}
        <Stack.Screen name="inspector" options={{ presentation: "fullScreenModal" }} />
        {/* Ecranele deschise peste taburi stau și ele după cont: nescrise aici, rămâneau pe ecran după „Deconectare”
            (Profil fără firmă, 28.09.2026) în loc să ducă la login. */}
        <Stack.Screen name="profil" />
        <Stack.Screen name="dispozitive" />
        <Stack.Screen name="firme" />
        <Stack.Screen name="predare" />
        <Stack.Screen name="gata" />
        <Stack.Screen name="miscare/[id]" />
      </Stack.Protected>
      <Stack.Protected guard={!session}>
        <Stack.Screen name="login" />
      </Stack.Protected>
    </Stack>
  );
}
