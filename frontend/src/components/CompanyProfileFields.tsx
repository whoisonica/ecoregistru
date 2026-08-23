import { useState } from "react";
import { X } from "lucide-react";
import type { CompanyType, MarketRole, WasteCode, WasteOperationCode } from "@/lib/types";
import { useWasteCodeSearch } from "@/hooks/useWasteCodes";
import { strings } from "@/lib/strings";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { DateInput } from "@/components/ui/date-input";
import { Combobox, type ComboboxItem } from "@/components/ui/combobox";

const t = strings.companyProfile;
const codeLabels = strings.enums.wasteOperationCode;

const marketRoleLabels = strings.enums.marketRole;
const marketRoleHints = strings.enums.marketRoleHint;
export const MARKET_ROLES: MarketRole[] = ["PRODUCER", "IMPORTER", "TRADER"];

const ALL_CODES = Object.keys(codeLabels) as WasteOperationCode[];
const R_CODES = ALL_CODES.filter((c) => c.startsWith("R"));
const D_CODES = ALL_CODES.filter((c) => c.startsWith("D"));


/**
 * „Ce tip de generator": producător / importator / comerciant, bifabile împreună — o firmă poate
 * fi și una, și alta. Nimic bifat înseamnă „nu s-a răspuns", nu „niciuna".
 */
export function MarketRolePicker({
  value,
  onChange,
  label,
  hint,
}: {
  value: MarketRole[];
  onChange: (next: MarketRole[]) => void;
  label: string;
  hint: string;
}) {
  const toggle = (role: MarketRole) =>
    onChange(value.includes(role) ? value.filter((r) => r !== role) : [...value, role]);

  // Bifat doar „Comerciant" e singurul răspuns din care decurge ceva vizibil: nu depune
  // declarația de ambalaje. Se spune pe loc, ca omul să vadă că răspunsul a fost înțeles.
  const traderOnly = value.length > 0 && value.every((r) => r === "TRADER");

  return (
    <div>
      <span className="block text-sm font-medium text-gray-700">{label}</span>
      <p className="text-xs text-gray-500">{hint}</p>
      <div className="mt-2 space-y-2 rounded-md border border-gray-200 p-3">
        {MARKET_ROLES.map((role) => (
          <label key={role} className="flex items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-gray-300"
              checked={value.includes(role)}
              onChange={() => toggle(role)}
            />
            <span>
              <span className="font-medium text-gray-800">{marketRoleLabels[role]}</span>
              <span className="block text-xs text-gray-500">{marketRoleHints[role]}</span>
            </span>
          </label>
        ))}
      </div>
      {traderOnly && (
        <p className="mt-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          {strings.companyProfile.marketRolesTraderOnly}
        </p>
      )}
    </div>
  );
}

export interface CompanyProfileValue {
  authorizedOperationCodes: WasteOperationCode[];
  marketRoles: MarketRole[];
  authorizedWasteCodes: WasteCode[];
  transportMeans: string;
  transportLicenseNumber: string;
  transportLicenseExpiry: string;
}

export const emptyCompanyProfile: CompanyProfileValue = {
  authorizedOperationCodes: [],
  marketRoles: [],
  authorizedWasteCodes: [],
  transportMeans: "",
  transportLicenseNumber: "",
  transportLicenseExpiry: "",
};

function CodeGroup({
  title,
  codes,
  selected,
  onToggle,
  onSet,
}: {
  title: string;
  codes: WasteOperationCode[];
  selected: WasteOperationCode[];
  onToggle: (code: WasteOperationCode) => void;
  onSet: (codes: WasteOperationCode[]) => void;
}) {
  const allSelected = codes.every((c) => selected.includes(c));
  return (
    <div>
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wide text-gray-500">{title}</span>
        <button
          type="button"
          className="text-xs text-blue-600 hover:underline"
          onClick={() =>
            onSet(
              allSelected
                ? selected.filter((c) => !codes.includes(c))
                : [...selected, ...codes.filter((c) => !selected.includes(c))]
            )
          }
        >
          {allSelected ? t.clearAll : t.selectAll}
        </button>
      </div>
      <div className="mt-1 max-h-44 space-y-1 overflow-y-auto rounded-md border border-gray-200 p-2">
        {codes.map((c) => (
          <label key={c} className="flex items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-gray-300"
              checked={selected.includes(c)}
              onChange={() => onToggle(c)}
            />
            <span className="text-gray-700">{codeLabels[c]}</span>
          </label>
        ))}
      </div>
    </div>
  );
}

/**
 * The account profile — the questions from the client's intake form, in the shape support fills
 * them in. It is deliberately the same block wherever it appears, so the form the client answers
 * and the screen support types it into ask the same things in the same order.
 *
 * <p>Leaving everything empty is a valid answer and means "no restriction": an account whose form
 * has not come back yet must keep seeing the whole nomenclator, not none of it.
 */
export function CompanyProfileFields({
  value,
  onChange,
  companyType,
}: {
  value: CompanyProfileValue;
  onChange: (next: CompanyProfileValue) => void;
  companyType: CompanyType;
}) {
  const [codeQuery, setCodeQuery] = useState("");
  const codeSearch = useWasteCodeSearch(codeQuery);

  const patch = (part: Partial<CompanyProfileValue>) => onChange({ ...value, ...part });

  const codeItems: ComboboxItem[] = (codeSearch.data ?? [])
    .filter((w) => !value.authorizedWasteCodes.some((a) => a.id === w.id))
    .map((w) => ({ id: w.id, label: `${w.code} — ${w.name}` }));

  function addWasteCode(item: ComboboxItem | null) {
    if (!item) return;
    const found = (codeSearch.data ?? []).find((w) => w.id === item.id);
    if (!found) return;
    patch({ authorizedWasteCodes: [...value.authorizedWasteCodes, found] });
  }

  // The transport block is a collector's question: a generator has nothing to answer there.
  const asksTransport = companyType !== "GENERATOR";

  return (
    <div className="space-y-5 border-t border-gray-200 pt-5">
      <div>
        <h3 className="text-sm font-semibold text-gray-900">{t.title}</h3>
        <p className="mt-1 text-xs text-gray-500">{t.subtitle}</p>
      </div>

      <MarketRolePicker
        value={value.marketRoles}
        onChange={(marketRoles) => patch({ marketRoles })}
        label={t.marketRoles}
        hint={t.marketRolesHint}
      />

      <div>
        <span className="block text-sm font-medium text-gray-700">{t.operationCodes}</span>
        <p className="text-xs text-gray-500">{t.operationCodesHint}</p>
        <div className="mt-2 grid grid-cols-2 gap-3">
          <CodeGroup
            title={t.recovery}
            codes={R_CODES}
            selected={value.authorizedOperationCodes}
            onToggle={(c) =>
              patch({
                authorizedOperationCodes: value.authorizedOperationCodes.includes(c)
                  ? value.authorizedOperationCodes.filter((x) => x !== c)
                  : [...value.authorizedOperationCodes, c],
              })
            }
            onSet={(codes) => patch({ authorizedOperationCodes: codes })}
          />
          <CodeGroup
            title={t.disposal}
            codes={D_CODES}
            selected={value.authorizedOperationCodes}
            onToggle={(c) =>
              patch({
                authorizedOperationCodes: value.authorizedOperationCodes.includes(c)
                  ? value.authorizedOperationCodes.filter((x) => x !== c)
                  : [...value.authorizedOperationCodes, c],
              })
            }
            onSet={(codes) => patch({ authorizedOperationCodes: codes })}
          />
        </div>
      </div>

      <div>
        <Label htmlFor="cp-waste-codes">{t.wasteCodes}</Label>
        <p className="text-xs text-gray-500">{t.wasteCodesHint}</p>
        <div className="mt-2">
          <Combobox
            id="cp-waste-codes"
            value={null}
            onSelect={addWasteCode}
            onQueryChange={setCodeQuery}
            items={codeItems}
            loading={codeSearch.isFetching}
            placeholder={t.addWasteCode}
            searchPlaceholder={strings.movements.wasteCodeSearch}
          />
        </div>
        {value.authorizedWasteCodes.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {value.authorizedWasteCodes.map((w) => (
              <span
                key={w.id}
                className="inline-flex items-center gap-1 rounded-full bg-gray-100 py-0.5 pl-2.5 pr-1 text-xs text-gray-700"
              >
                {w.code}
                <button
                  type="button"
                  aria-label={t.removeWasteCode}
                  className="rounded-full p-0.5 text-gray-400 hover:bg-gray-200 hover:text-gray-700"
                  onClick={() =>
                    patch({
                      authorizedWasteCodes: value.authorizedWasteCodes.filter((x) => x.id !== w.id),
                    })
                  }
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            ))}
          </div>
        )}
      </div>

      {asksTransport && (
        <div className="space-y-3">
          <span className="block text-sm font-medium text-gray-700">{t.transport}</span>
          <div>
            <Label htmlFor="cp-transport-means">{t.transportMeans}</Label>
            <Textarea
              id="cp-transport-means"
              rows={2}
              value={value.transportMeans}
              onChange={(e) => patch({ transportMeans: e.target.value })}
              placeholder={t.transportMeansPlaceholder}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="cp-transport-licence">{t.transportLicenseNumber}</Label>
              <Input
                id="cp-transport-licence"
                value={value.transportLicenseNumber}
                onChange={(e) => patch({ transportLicenseNumber: e.target.value })}
              />
            </div>
            <div>
              <Label htmlFor="cp-transport-expiry">{t.transportLicenseExpiry}</Label>
              <DateInput
                id="cp-transport-expiry"
                value={value.transportLicenseExpiry}
                onChange={(e) => patch({ transportLicenseExpiry: e.target.value })}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
