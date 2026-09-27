import { onlineManager } from "@tanstack/react-query";
import { useEffect, useState } from "react";

/**
 * Are telefonul semnal? Aceeași sursă ca React Query (`onlineManager`, legat la `expo-network` în
 * `app/_layout.tsx`), ca banda „Fără semnal” și listele să nu spună lucruri diferite în aceeași clipă.
 */
export function useOnline() {
  const [online, setOnline] = useState(onlineManager.isOnline());
  useEffect(() => onlineManager.subscribe(setOnline), []);
  return online;
}
