import Constants from "expo-constants";
import * as Device from "expo-device";
import * as Notifications from "expo-notifications";
import { useRouter, type Href } from "expo-router";
import * as SecureStore from "expo-secure-store";
import { useEffect } from "react";
import { Platform } from "react-native";

import * as api from "./api";
import { useSession } from "./session";

// Tokenul de push trimis ultima oară, pe sesiune: nu se rescrie pe server la fiecare deschidere.
const SENT_KEY = "wh_push_sent";

/** Cu aplicația deschisă, notificarea se arată tot ca banner — altfel un termen de azi ar trece neobservat. */
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
  }),
});

/**
 * Ecranele pe care le poate deschide o notificare. Lista e închisă: `screen` vine de pe server, dar
 * o valoare necunoscută nu devine rută — o notificare veche, după o versiune nouă, deschide Acasă.
 */
const SCREENS: Record<string, Href> = { termene: "/termene", control: "/control", acasa: "/" };

/**
 * G2 — telefonul își spune tokenul de push serverului, legat de **sesiunea lui de dispozitiv**
 * (`PUT /auth/devices/{id}/push-token`). Ieșirea din cont, parola schimbată sau contul dezactivat
 * sting sesiunea, deci și notificările — nicio listă separată de ținut la zi.
 *
 * <p>Tokenul Expo cere `extra.eas.projectId` în `app.json`, adică proiectul EAS al proprietarului.
 * Fără el (azi) nu se cere nimic și nu se trimite nimic: aplicația merge ca înainte.
 *
 * <p>Permisiunea se cere după login, nu la pornire: întâi omul vede la ce folosește aplicația.
 */
export function usePushRegistration() {
  const { session, auth } = useSession();
  const deviceSessionId = session?.deviceSessionId;
  const token = auth?.token;

  useEffect(() => {
    if (!token || !deviceSessionId) return;
    let cancelled = false;
    (async () => {
      const projectId = Constants.expoConfig?.extra?.eas?.projectId as string | undefined;
      if (!projectId || !Device.isDevice) return;
      if (Platform.OS === "android") {
        await Notifications.setNotificationChannelAsync("default", {
          name: "Termene și alerte",
          importance: Notifications.AndroidImportance.DEFAULT,
        });
      }
      const current = await Notifications.getPermissionsAsync();
      const granted = current.granted || (current.canAskAgain && (await Notifications.requestPermissionsAsync()).granted);
      if (!granted || cancelled) return;
      const { data: pushToken } = await Notifications.getExpoPushTokenAsync({ projectId });
      const sentKey = `${deviceSessionId}:${pushToken}`;
      if ((await SecureStore.getItemAsync(SENT_KEY)) === sentKey || cancelled) return;
      await api.registerPushToken({ token, tenantId: null }, deviceSessionId, pushToken);
      await SecureStore.setItemAsync(SENT_KEY, sentKey);
    })().catch(() => {
      // Push-ul e un plus: fără semnal sau fără permisiune, restul aplicației merge. Se reîncearcă la următoarea pornire.
    });
    return () => {
      cancelled = true;
    };
  }, [token, deviceSessionId]);
}

let handled: string | null = null;

/** Apăsarea pe o notificare deschide ecranul ei (`data.screen`), inclusiv când aplicația era închisă. */
export function useNotificationRoutes() {
  const router = useRouter();
  const last = Notifications.useLastNotificationResponse();
  useEffect(() => {
    const screen = last?.notification.request.content.data?.screen;
    // O dată pe notificare: bara de jos se montează din nou după un login, iar răspunsul rămâne „ultimul”.
    const id = last?.notification.request.identifier ?? null;
    if (typeof screen !== "string" || id === handled) return;
    handled = id;
    router.push(SCREENS[screen] ?? "/");
  }, [last, router]);
}
