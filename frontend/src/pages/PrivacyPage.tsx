import { LegalDocument } from "@/components/LegalDocument";
import { PRIVACY } from "@/lib/legal";

/** `/confidentialitate` — publică. Textul e în `lib/legal.ts`, forma în `LegalDocument`. */
export function PrivacyPage() {
  return <LegalDocument doc={PRIVACY} />;
}
