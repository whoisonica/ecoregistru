import { ImageManipulator, SaveFormat } from "expo-image-manipulator";
import * as ImagePicker from "expo-image-picker";

/**
 * Poza avizului, de la cameră sau din galerie, micșorată pe telefon înainte de orice (todo-mobil §6):
 * latura lungă la 2000 px, JPEG 0,7 — în jur de un megaoctet, destul ca textul să se citească și
 * departe de plafonul de 10 MB. Folosită de „Adaugă” și de tasta „Pozează avizul” de pe Acasă.
 *
 * <p>`"denied"` = omul n-a dat acces la cameră; `null` = a renunțat.
 */
export async function pickAvizPhoto(source: "camera" | "gallery"): Promise<string | "denied" | null> {
  let result: ImagePicker.ImagePickerResult;
  if (source === "camera") {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) return "denied";
    result = await ImagePicker.launchCameraAsync({ mediaTypes: ["images"], quality: 1 });
  } else {
    result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ["images"], quality: 1 });
  }
  if (result.canceled || !result.assets[0]) return null;
  return shrink(result.assets[0]);
}

async function shrink(asset: ImagePicker.ImagePickerAsset): Promise<string> {
  const wide = asset.width >= asset.height;
  const context = ImageManipulator.manipulate(asset.uri);
  if (Math.max(asset.width, asset.height) > 2000) context.resize(wide ? { width: 2000 } : { height: 2000 });
  const image = await context.renderAsync();
  const saved = await image.saveAsync({ compress: 0.7, format: SaveFormat.JPEG });
  return saved.uri;
}
