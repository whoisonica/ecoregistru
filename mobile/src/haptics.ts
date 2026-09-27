import * as Haptics from "expo-haptics";
import { Platform } from "react-native";

/**
 * Confirmarea se simte, nu doar se vede (propunerea de refresh, §3.8): ușor la „Corect” și la o
 * apăsare care schimbă ceva, mediu la „Salvează”, notificare la succes sau refuz. Pe web și pe un
 * telefon fără motor, apelul cade în tăcere — vibrația e un plus, nu o funcție.
 */
const quiet = (p: Promise<void>) => p.catch(() => {});

export const haptic = {
  tap: () => (Platform.OS === "web" ? undefined : quiet(Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light))),
  press: () => (Platform.OS === "web" ? undefined : quiet(Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium))),
  success: () => (Platform.OS === "web" ? undefined : quiet(Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success))),
  warning: () => (Platform.OS === "web" ? undefined : quiet(Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning))),
  error: () => (Platform.OS === "web" ? undefined : quiet(Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error))),
};
