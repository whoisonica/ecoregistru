import { LegalDocument } from "@/components/LegalDocument";
import { TERMS } from "@/lib/legal";

/** `/termeni` — publică. Textul e în `lib/legal.ts`, forma în `LegalDocument`. */
export function TermsPage() {
  return <LegalDocument doc={TERMS} />;
}
