import type { Dispatch, SetStateAction } from "react";
import type { Driver, Partner, TransportDestination } from "@/lib/types";
import { strings } from "@/lib/strings";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { PillGroup } from "@/components/ui/pill-group";

const t = strings.movements;
const e = strings.enums;

/**
 * Cine duce deșeul: transportatorul, șoferul, vehiculul și destinațiile drumului — rubricile pe care
 * le cer și Anexa 3, și Anexa 2 (HG 1061/2008). Scoase din `MovementFormDialog` pe 18.09.2026, unde
 * formularul trecuse de 1.700 de linii; stau împreună fiindcă se leagă între ele — firma aleasă
 * decide ce șoferi se propun —, iar alăturarea asta era deja motivul pentru care erau scrise alături.
 *
 * <p>Starea rămâne în formularul de mișcare, ca la {@link Anexa2Fields}: rubricile astea intră în
 * aceeași salvare cu restul, iar o stare proprie aici ar trebui oricum ridicată înapoi la trimitere.
 */
export function TransportFields({
  transportPartnerId,
  setTransportPartnerId,
  carrierPartners,
  otherPartners,
  driverId,
  setDriverId,
  availableDrivers,
  driverName,
  setDriverName,
  driverIdentification,
  setDriverIdentification,
  driverCnp,
  setDriverCnp,
  vehicleRegistration,
  setVehicleRegistration,
  transportDestinations,
  setTransportDestinations,
  destinationsPrefilled,
  setDestinationsPrefilled,
}: {
  transportPartnerId: string;
  setTransportPartnerId: (value: string) => void;
  /** Partenerii care pot transporta — grupul propus întâi în listă. */
  carrierPartners: Partner[];
  /** Restul, sub ei: o firmă poate transporta fără să fie bifată așa. */
  otherPartners: Partner[];
  driverId: string;
  setDriverId: (value: string) => void;
  /** Șoferii transportatorului ales; fără transportator, cei proprii. */
  availableDrivers: Driver[];
  driverName: string;
  setDriverName: (value: string) => void;
  driverIdentification: string;
  setDriverIdentification: (value: string) => void;
  driverCnp: string;
  setDriverCnp: (value: string) => void;
  vehicleRegistration: string;
  setVehicleRegistration: (value: string) => void;
  transportDestinations: TransportDestination[];
  /** Forma cu funcție se folosește în bloc (bifă adăugată/scoasă), deci tipul e al unui `setState`. */
  setTransportDestinations: Dispatch<SetStateAction<TransportDestination[]>>;
  /** Dacă destinațiile au fost puse de aplicație: la prima atingere a omului, nu se mai rescriu. */
  destinationsPrefilled: boolean;
  setDestinationsPrefilled: (value: boolean) => void;
}) {
  return (
    <>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {/* Transportatorul și șoferul stau alături: alegerea firmei decide ce șoferi se
                  propun, iar alăturarea face legătura vizibilă fără s-o explice nimeni. */}
              <div>
                <Label htmlFor="mv-carrier">{t.transportPartner}</Label>
                <Select
                  id="mv-carrier"
                  value={transportPartnerId}
                  onChange={(ev) => {
                    setTransportPartnerId(ev.target.value);
                    // Șoferii sunt ai transportatorului: schimbi firma, alegerea nu mai e a ei.
                    // Textul deja scris rămâne — poate a fost scris de mână, și nu se șterge munca.
                    setDriverId("");
                  }}
                >
                  <option value="">{t.transportPartnerPlaceholder}</option>
                  {carrierPartners.length > 0 && (
                    <optgroup label={t.carrierGroup}>
                      {carrierPartners.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name}
                        </option>
                      ))}
                    </optgroup>
                  )}
                  {otherPartners.length > 0 && (
                    <optgroup label={carrierPartners.length > 0 ? t.otherPartnersGroup : t.allPartnersGroup}>
                      {otherPartners.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name}
                        </option>
                      ))}
                    </optgroup>
                  )}
                </Select>
                <p className="mt-1 text-xs text-content-muted">
                  {carrierPartners.length > 0 ? t.transportPartnerHint : t.transportPartnerNoneHint}
                </p>
              </div>
              <div>
                <Label htmlFor="mv-driver-pick">{t.driverPick}</Label>
                <Select
                  id="mv-driver-pick"
                  value={driverId}
                  onChange={(ev) => {
                    const picked = availableDrivers.find((d) => d.id === ev.target.value);
                    setDriverId(ev.target.value);
                    if (picked) {
                      setDriverName(picked.name);
                      setDriverIdentification(picked.identification ?? "");
                      setDriverCnp(picked.cnp ?? "");
                      setVehicleRegistration(picked.vehicleRegistration ?? "");
                    }
                  }}
                  disabled={availableDrivers.length === 0}
                >
                  <option value="">{t.driverPickFreeText}</option>
                  {availableDrivers.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                      {d.vehicleRegistration ? ` — ${d.vehicleRegistration}` : ""}
                    </option>
                  ))}
                </Select>
                <p className="mt-1 text-xs text-content-muted">
                  {availableDrivers.length > 0
                    ? t.driverPickHint
                    : transportPartnerId
                      ? t.driverPickNoneCarrier
                      : t.driverPickNoneOwn}
                </p>
              </div>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <Label htmlFor="mv-driver">{t.driverName}</Label>
                <Input
                  id="mv-driver" maxLength={255}
                  value={driverName}
                  onChange={(ev) => setDriverName(ev.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="mv-driver-id">{t.driverIdentification}</Label>
                <Input
                  id="mv-driver-id" maxLength={100}
                  value={driverIdentification}
                  onChange={(ev) => setDriverIdentification(ev.target.value)}
                  placeholder={t.driverIdentificationPlaceholder}
                />
              </div>
              <div>
                <Label htmlFor="mv-driver-cnp">{strings.common.cnp}</Label>
                <Input
                  id="mv-driver-cnp"
                  inputMode="numeric"
                  maxLength={13}
                  value={driverCnp}
                  onChange={(ev) => setDriverCnp(ev.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="mv-plate">{t.vehicleRegistration}</Label>
                <Input
                  id="mv-plate" maxLength={50}
                  value={vehicleRegistration}
                  onChange={(ev) => setVehicleRegistration(ev.target.value)}
                />
              </div>
            </div>
            <div>
              <span id="mv-destinat-label" className="block text-sm font-medium text-content-strong">
                {t.askTransportDestinations}
              </span>
              <p className="text-xs text-content-muted">
                {destinationsPrefilled ? t.destinationsPrefilled : t.transportDestinationsHint}
              </p>
              <PillGroup
                multiple
                name="mv-destinat"
                aria-labelledby="mv-destinat-label"
                className="mt-2"
                selected={transportDestinations}
                onToggle={(d) => {
                  setDestinationsPrefilled(false);
                  setTransportDestinations((prev) =>
                    prev.includes(d) ? prev.filter((x) => x !== d) : [...prev, d]
                  );
                }}
                options={(Object.keys(e.transportDestination) as TransportDestination[]).map((d) => ({
                  value: d,
                  label: e.transportDestination[d],
                }))}
              />
            </div>
    </>
  );
}
