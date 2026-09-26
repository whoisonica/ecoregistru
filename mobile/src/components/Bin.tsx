import { binFor } from "@/lib/binColor";
import { StyleSheet, View } from "react-native";

import { binColors } from "../theme";

/**
 * Pubela pe codul de deșeu, ca pe web (`lib/binColor.ts`) — aceeași listă explicită. Nimic pentru un
 * cod din afara ei: un pătrat gol ar fi o afirmație, deci locul rămâne liber.
 */
export function Bin({ code, hazardous }: { code: string; hazardous: boolean }) {
  const bin = binFor(code, hazardous);
  return bin ? <View style={[styles.bin, { backgroundColor: binColors[bin] }]} /> : <View style={styles.gap} />;
}

const styles = StyleSheet.create({
  bin: { width: 10, height: 13, borderRadius: 2 },
  gap: { width: 10 },
});
