import type { PackagingCategory, PackagingMaterial, PackagingOrigin } from "@/lib/types";
import { strings } from "@/lib/strings";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { PACKAGING_MATERIALS } from "@/components/movements/movementRules";

const t = strings.movements;
const e = strings.enums;

/**
 * Rubricile de ambalaj ale mișcării (tabelul 1 al Anexei 1 Ambalaje și proveniența de pe Anexa 3
 * Ambalaje), arătate numai pe coduri 15 01 xx. Starea rămâne în formularul de mișcare.
 */
export function PackagingFields({
  collected,
  suggestedMaterial,
  packagingOnMarket,
  setPackagingOnMarket,
  packagingOrigin,
  setPackagingOrigin,
  packagingMaterial,
  setPackagingMaterial,
  packagingCategory,
  setPackagingCategory,
  packagingReusable,
  setPackagingReusable,
  packagingHazardousContent,
  setPackagingHazardousContent,
}: {
  /** Preluare: numai acolo are înțeles proveniența (nota 2 a Anexei 3 Ambalaje). */
  collected: boolean;
  suggestedMaterial: PackagingMaterial | null;
  /** `null` pe o mișcare veche, până când cineva atinge bifa. */
  packagingOnMarket: boolean | null;
  setPackagingOnMarket: (value: boolean) => void;
  packagingOrigin: PackagingOrigin | "";
  setPackagingOrigin: (value: PackagingOrigin | "") => void;
  packagingMaterial: PackagingMaterial | "";
  setPackagingMaterial: (value: PackagingMaterial | "") => void;
  packagingCategory: PackagingCategory | "";
  setPackagingCategory: (value: PackagingCategory | "") => void;
  packagingReusable: boolean;
  setPackagingReusable: (value: boolean) => void;
  packagingHazardousContent: boolean;
  setPackagingHazardousContent: (value: boolean) => void;
}) {
  return (
    <div className="space-y-3 rounded-md border border-emerald-200 bg-emerald-50/50 p-3">
      <div>
        <span className="text-sm font-semibold text-content-strong">{t.packagingSection}</span>
        <p className="text-xs text-content-muted">{t.packagingSectionHint}</p>
      </div>
      <label className="flex cursor-pointer items-start gap-2">
        <input
          type="checkbox"
          className="mt-0.5 h-4 w-4 shrink-0"
          checked={packagingOnMarket !== false}
          onChange={(ev) => setPackagingOnMarket(ev.target.checked)}
        />
        <span>
          <span className="text-sm font-medium text-content-strong">{t.packagingOnMarket}</span>
          <span className="block text-xs text-content-muted">{t.packagingOnMarketHint}</span>
        </span>
      </label>

      {collected && (
        <div>
          <Label htmlFor="mv-pk-origin">{strings.packagingOrigin.label}</Label>
          <Select
            id="mv-pk-origin"
            value={packagingOrigin}
            onChange={(ev) => setPackagingOrigin(ev.target.value as PackagingOrigin | "")}
          >
            <option value="">{strings.packagingOrigin.fromPartner}</option>
            <option value="POPULATIE">{strings.packagingOrigin.POPULATIE}</option>
            <option value="GENERATOR_PJ">{strings.packagingOrigin.GENERATOR_PJ}</option>
            <option value="COLECTOR">{strings.packagingOrigin.COLECTOR}</option>
            <option value="COMERCIANT">{strings.packagingOrigin.COMERCIANT}</option>
          </Select>
          <p className="mt-1 text-xs text-content-muted">{strings.packagingOrigin.hintMovement}</p>
        </div>
      )}

      {packagingOnMarket === null && (
        <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          {t.packagingLegacy}
        </p>
      )}

      {/* Rubricile de mai jos dau rândul şi coloana din tabelul 1, deci n-au sens dacă
          mişcarea nu ajunge în tabel. */}
      {packagingOnMarket !== false && (
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div>
          <Label htmlFor="mv-pk-material">{t.packagingMaterial}</Label>
          <Select
            id="mv-pk-material"
            value={packagingMaterial}
            onChange={(ev) =>
              setPackagingMaterial(ev.target.value as PackagingMaterial | "")
            }
          >
            <option value="">
              {suggestedMaterial
                ? `${e.packagingMaterial[suggestedMaterial]} ${t.packagingFromCode}`
                : t.packagingMaterialPlaceholder}
            </option>
            {PACKAGING_MATERIALS.map((m) => (
              <option key={m} value={m}>
                {e.packagingMaterial[m]}
              </option>
            ))}
          </Select>
          {!suggestedMaterial && !packagingMaterial && (
            <p className="mt-1 text-xs text-amber-700">{t.packagingMaterialNeeded}</p>
          )}
        </div>
        <div>
          <Label htmlFor="mv-pk-category">{t.packagingCategory}</Label>
          <Select
            id="mv-pk-category"
            value={packagingCategory}
            onChange={(ev) => setPackagingCategory(ev.target.value as PackagingCategory | "")}
          >
            <option value="">{t.packagingCategoryPlaceholder}</option>
            <option value="SALES">{e.packagingCategory.SALES}</option>
            <option value="PRIMARY">{e.packagingCategory.PRIMARY}</option>
            <option value="SECONDARY">{e.packagingCategory.SECONDARY}</option>
          </Select>
          <p className="mt-1 text-xs text-content-muted">{t.packagingCategoryHint}</p>
        </div>
      </div>
      )}
      {packagingOnMarket !== false && (
      <div className="flex flex-wrap gap-4">
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            className="h-4 w-4"
            checked={packagingReusable}
            onChange={(ev) => setPackagingReusable(ev.target.checked)}
          />
          {t.packagingReusable}
        </label>
        {/* Nota 3: ambalajele cu conţinut periculos sunt tot ambalaje primare. Bifa apare
            numai acolo, ca să nu se poată răspunde ceva ce formularul n-ar putea tipări. */}
        {packagingCategory === "PRIMARY" && (
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="h-4 w-4"
              checked={packagingHazardousContent}
              onChange={(ev) => setPackagingHazardousContent(ev.target.checked)}
            />
            {t.packagingHazardous}
          </label>
        )}
      </div>
      )}
    </div>
  );
}
