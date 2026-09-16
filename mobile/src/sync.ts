import { useQueryClient } from "@tanstack/react-query";
import * as Network from "expo-network";
import { useEffect } from "react";
import { AppState } from "react-native";

import { drain } from "./outbox";
import { useSession } from "./session";

/** Cât de des se uită aplicația în coadă cât timp e deschisă, pentru pauzele care au expirat. */
const TICK_MS = 30_000;

/**
 * Când pleacă predările din coadă: la deschidere, când aplicația revine în față, când se întoarce
 * rețeaua și, cât e deschisă, la fiecare jumătate de minut. Nu în fundal — pe iOS o sarcină de
 * fundal nu are oră garantată, iar magazionerul oricum deschide aplicația a doua zi.
 */
export function useOutboxSync() {
  const { auth, session } = useSession();
  const queryClient = useQueryClient();
  const owner = session?.email;

  useEffect(() => {
    if (!auth || !owner) return;
    const kick = () =>
      drain(auth, owner)
        .then((sent) => {
          if (sent > 0) queryClient.invalidateQueries({ queryKey: ["movements"] });
        })
        .catch(() => {});

    kick();
    const app = AppState.addEventListener("change", (s) => s === "active" && kick());
    const net = Network.addNetworkStateListener((s) => s.isConnected && kick());
    const timer = setInterval(kick, TICK_MS);
    return () => {
      app.remove();
      net.remove();
      clearInterval(timer);
    };
  }, [auth, owner, queryClient]);
}
