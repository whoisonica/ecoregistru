import { useQuery, useQueryClient } from "@tanstack/react-query";
import { strings } from "@web/strings";
import type {
  PackagingCategory,
  PackagingMaterial,
  Partner,
  PartnerType,
  PhysicalState,
  StorageType,
  TransportDestination,
  TransportMeans,
  Unit,
  WasteCode,
  WasteDestination,
  WasteOperationCode,
} from "@web/types";
import {
  destinationsFor,
  PACKAGING_MATERIALS,
  suggestedDestinations,
  suggestedPackagingMaterial,
} from "@/components/movements/movementRules";
import { isValidCnp } from "@/lib/cnp";
import * as Crypto from "expo-crypto";
import { Stack, useLocalSearchParams, useRouter } from "expo-router";
import { extractTextFromImage, isSupported } from "expo-text-extractor";
import { useEffect, useMemo, useRef, useState } from "react";
import { ActivityIndicator, Image, ScrollView, StyleSheet, Switch, Text, View } from "react-native";

import * as api from "../src/api";
import { parseAviz, cuiDigits, type AvizReading } from "../src/aviz/parse";
import { canWrite } from "../src/auth";
import { Field, Input, MultiPills, Pills, PrimaryButton } from "../src/components/Form";
import { Group, Note, rowStyles, SectionHead } from "../src/components/Rows";
import { formatDate, formatKg } from "../src/format";
import { useHandoverData } from "../src/handover";
import { drain, enqueue } from "../src/outbox";
import { useSession } from "../src/session";
import { colors, fonts, radius } from "../src/theme";

const t = strings.movements;
const m = strings.mobile;
const e = strings.enums;

type Fate = "RECOVERED" | "DISPOSED";
/** Rubricile care pot veni din poză și trebuie confirmate. */
type ReadField = "date" | "wasteCode" | "quantity" | "partner" | "documentReference" | "vehicle";

const ALL_CODES = Object.keys(e.wasteOperationCode) as WasteOperationCode[];

/**
 * M1b — predarea deșeului propriu: generare + transport spre valorificare sau eliminare, pe Anexa 1.
 * Exact ce salvează „Generare” pe web (`MovementsPage.buildInput`): `operation` e soarta aleasă,
 * `register` e `ANEXA_1`, codul R/D e obligatoriu (G3).
 *
 * <p>Rubricile stau în ordinea formularului web: punct de lucru, dată, cod · cantitate · starea ·
 * depozitarea · transportul și destinația · soarta și codul R/D · destinatarul · ambalajul ·
 * documentul și mașina.
 *
 * <p>Din 19.09.2026 serverul refuză o predare pe Anexa 1 fără ce tipăresc fișa și anexele de
 * ambalaje (`validateOwnWasteHandover`, BUG-023): starea, depozitarea, mijlocul de transport,
 * destinația, materialul și felul ambalajului, partenerul autorizat. Le cere și telefonul, înainte
 * ca predarea să intre în coadă — altfel ar fi ieșit „refuzată” abia la trimitere.
 *
 * <p>Ce vine din poză e o propunere cu rândul ei de pe aviz; „Salvează” nu pleacă până nu e confirmată
 * fiecare. Schimbarea unei valori o confirmă și ea: omul a pus-o, nu camera.
 */
export default function PredareScreen() {
  const { photo } = useLocalSearchParams<{ photo?: string }>();
  const { auth, session } = useSession();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { company, workPoints, partners, recent, drivers } = useHandoverData();

  // Cheia de idempotență a predării, dată o dată, la deschiderea formularului (todo-mobil §10).
  const id = useRef(Crypto.randomUUID()).current;

  const activeWorkPoints = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  const [workPointId, setWorkPointId] = useState("");
  useEffect(() => {
    if (!workPointId && activeWorkPoints.length === 1) setWorkPointId(activeWorkPoints[0].id);
  }, [activeWorkPoints, workPointId]);

  const [date, setDate] = useState(formatDate(todayIso()));
  const [wasteCode, setWasteCode] = useState<WasteCode | null>(null);
  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState<Unit>("KG");
  const [weighed, setWeighed] = useState(false);
  const [fate, setFate] = useState<Fate | "">("");
  const [operationCode, setOperationCode] = useState<WasteOperationCode | "">("");
  const [partnerId, setPartnerId] = useState("");
  const [packagingOnMarket, setPackagingOnMarket] = useState(false);
  const [packagingMaterial, setPackagingMaterial] = useState<PackagingMaterial | "">("");
  const [packagingCategory, setPackagingCategory] = useState<PackagingCategory | "">("");
  const [physicalState, setPhysicalState] = useState<PhysicalState | "">("");
  const [storageType, setStorageType] = useState<StorageType | "">("");
  const [transportMeans, setTransportMeans] = useState<TransportMeans | "">("");
  const [wasteDestination, setWasteDestination] = useState<WasteDestination | "">("");
  const [documentReference, setDocumentReference] = useState("");
  const [vehicle, setVehicle] = useState("");
  // ── Anexa 3: transportul (ca `TransportFields` pe web) ──
  const [partnerWorkPointId, setPartnerWorkPointId] = useState("");
  const [loadDate, setLoadDate] = useState("");
  const [unloadDate, setUnloadDate] = useState("");
  const [anexa3Unit, setAnexa3Unit] = useState<Unit | "">("");
  const [transportPartnerId, setTransportPartnerId] = useState("");
  const [driverId, setDriverId] = useState("");
  const [driverName, setDriverName] = useState("");
  const [driverIdentification, setDriverIdentification] = useState("");
  const [driverCnp, setDriverCnp] = useState("");
  const [transportDestinations, setTransportDestinations] = useState<TransportDestination[]>([]);
  const [destinationsPrefilled, setDestinationsPrefilled] = useState(false);
  const [showErrors, setShowErrors] = useState(false);

  // ── citirea avizului ───────────────────────────────────────────────────────
  const [lines, setLines] = useState<string[] | null>(null);
  const [ocrFailed, setOcrFailed] = useState(false);
  useEffect(() => {
    if (!photo) return;
    if (!isSupported) return setOcrFailed(true);
    extractTextFromImage(photo)
      .then(setLines)
      .catch(() => setOcrFailed(true));
  }, [photo]);

  const [unknownCui, setUnknownCui] = useState<string | null>(null);
  const [pending, setPending] = useState<Partial<Record<ReadField, string>>>({});
  const confirm = (f: ReadField) => setPending(({ [f]: _, ...rest }) => rest);
  const applied = useRef(false);

  // Se aplică o singură dată, după ce și textul, și listele firmei sunt aici — altfel un CUI citit
  // înainte să vină partenerii ar fi ieșit „necunoscut”. Fără semnal și fără cache, listele cad pe
  // eroare, și atunci se citește cu ce este.
  const listsSettled = !company.isLoading && !partners.isLoading;
  useEffect(() => {
    if (!lines || !listsSettled || applied.current) return;
    applied.current = true;
    const profileCodes = company.data?.authorizedWasteCodes ?? [];
    const reading: AvizReading = parseAviz(lines, {
      ownCui: company.data?.cui,
      wasteCodes: profileCodes.map((c) => c.code),
      partners: (partners.data ?? []).filter((p) => p.active).map((p) => ({ id: p.id, cui: p.cui })),
    });
    const next: Partial<Record<ReadField, string>> = {};
    if (reading.date) {
      setDate(formatDate(reading.date.value));
      next.date = reading.date.source;
    }
    const code = reading.wasteCode && profileCodes.find((c) => c.code === reading.wasteCode!.value);
    if (code) {
      setWasteCode(code);
      next.wasteCode = reading.wasteCode!.source;
    }
    if (reading.quantity) {
      setQuantity(String(reading.quantity.value.amount).replace(".", ","));
      setUnit(reading.quantity.value.unit);
      next.quantity = reading.quantity.source;
    }
    if (reading.partnerId) {
      setPartnerId(reading.partnerId.value);
      next.partner = reading.partnerId.source;
    } else if (reading.unknownCui) {
      setUnknownCui(reading.unknownCui.value);
    }
    if (reading.documentNumber) {
      setDocumentReference(reading.documentNumber.value);
      next.documentReference = reading.documentNumber.source;
    }
    if (reading.vehicle) {
      setVehicle(reading.vehicle.value);
      next.vehicle = reading.vehicle.source;
    }
    setPending(next);
  }, [lines, listsSettled, company.data, partners.data]);

  const reading = !!photo && !ocrFailed && (!lines || !applied.current);
  const readNothing =
    ocrFailed || (lines != null && applied.current && Object.keys(pending).length === 0 && !unknownCui);

  // ── G3: codul R/D ──────────────────────────────────────────────────────────
  const profileOps = company.data?.authorizedOperationCodes ?? [];
  const familyCodes = ALL_CODES.filter((c) =>
    fate === "RECOVERED" ? c.startsWith("R") : fate === "DISPOSED" ? c.startsWith("D") : false,
  );
  const codeOptions = profileOps.length === 0 ? familyCodes : familyCodes.filter((c) => profileOps.includes(c));

  /**
   * „La fel ca data trecută”: codul ultimei predări a aceluiași deșeu către același partener. Numai o
   * propunere — nu se pune singur, se aplică la apăsare, ca pe web.
   */
  const last = useMemo(() => {
    if (!partnerId || !wasteCode) return null;
    return (
      (recent.data?.content ?? []).find(
        (mv) => mv.partnerId === partnerId && mv.wasteCodeId === wasteCode.id && mv.operationCode,
      ) ?? null
    );
  }, [recent.data, partnerId, wasteCode]);

  /** „La fel ca data trecută” pune ce ar pune și pe web (`applyLast`), fără cantitate, dată și document. */
  const applyLast = () => {
    if (!last?.operationCode) return;
    setFate(last.operationCode.startsWith("R") ? "RECOVERED" : "DISPOSED");
    setOperationCode(last.operationCode);
    setPhysicalState(last.physicalState ?? "");
    setStorageType(last.storageType ?? "");
    setTransportMeans(last.transportMeans ?? "");
    setWasteDestination(last.wasteDestination ?? "");
    setPackagingMaterial(last.packagingMaterial ?? "");
    setPackagingCategory(last.packagingCategory ?? "");
    if (last.packagingOnMarket != null) setPackagingOnMarket(last.packagingOnMarket);
    // Transportul, ca pe web: aceeași firmă, același șofer, aceeași mașină. Datele nu — sunt ale drumului de azi.
    setPartnerWorkPointId(last.partnerWorkPointId ?? "");
    setAnexa3Unit(last.anexa3Unit ?? "");
    setTransportPartnerId(last.transportPartnerId ?? "");
    setDriverId("");
    setDriverName(last.driverName ?? "");
    setDriverIdentification(last.driverIdentification ?? "");
    setDriverCnp(last.driverCnp ?? "");
    if (last.vehicleRegistration) setVehicle(last.vehicleRegistration);
    setTransportDestinations(last.transportDestinations ?? []);
    setDestinationsPrefilled(false);
  };

  // Destinația din tabăra soartei alese (nota 5); schimbarea soartei golește una din tabăra cealaltă.
  const offeredDestinations = useMemo(() => destinationsFor(fate), [fate]);
  useEffect(() => {
    if (wasteDestination && !offeredDestinations.includes(wasteDestination)) setWasteDestination("");
  }, [offeredDestinations, wasteDestination]);

  // ── destinatarul ───────────────────────────────────────────────────────────
  const partner = (partners.data ?? []).find((p) => p.id === partnerId) ?? null;

  /**
   * Alegerea destinatarului, ca pe web: punctul lui de lucru se golește (era al celuilalt), iar
   * „Destinat:” se propune din felul partenerului numai peste o rubrică neatinsă.
   */
  const pickPartner = (id: string) => {
    setPartnerId(id);
    setPartnerWorkPointId("");
    if (transportDestinations.length === 0 && fate) {
      const chosen = (partners.data ?? []).find((p) => p.id === id);
      const suggested = suggestedDestinations(chosen?.type, fate);
      if (suggested.length > 0) {
        setTransportDestinations(suggested);
        setDestinationsPrefilled(true);
      }
    }
  };

  /**
   * Cine poate transporta: cei bifați „Transportator” în Parteneri, plus destinatarul — de cele mai
   * multe ori chiar el vine cu mașina. Pe web lista are toți partenerii (grupați); pe telefon ar fi o
   * listă derulantă lungă, deci restul rămân pe web.
   */
  const carrierOptions = useMemo(() => {
    const active = (partners.data ?? []).filter((p) => p.active);
    const list = active.filter((p) => p.carrier);
    if (partner && !list.some((p) => p.id === partner.id)) list.push(partner);
    const chosen = active.find((p) => p.id === transportPartnerId);
    if (chosen && !list.some((p) => p.id === chosen.id)) list.push(chosen);
    return list;
  }, [partners.data, partner, transportPartnerId]);
  /** Șoferii transportatorului ales; fără transportator, ai noștri (`partnerId` null). */
  const availableDrivers = useMemo(
    () =>
      (drivers.data ?? []).filter(
        (d) => d.active && (transportPartnerId ? d.partnerId === transportPartnerId : d.partnerId === null),
      ),
    [drivers.data, transportPartnerId],
  );
  const pickDriver = (id: string) => {
    setDriverId(id);
    const d = availableDrivers.find((x) => x.id === id);
    if (!d) return;
    // Alegerea precompletează cele trei rubrici (rămân editabile) și mașina, dacă e goală.
    setDriverName(d.name);
    setDriverIdentification(d.identification ?? "");
    setDriverCnp(d.cnp ?? "");
    if (d.vehicleRegistration && !vehicle.trim()) setVehicle(d.vehicleRegistration);
  };
  const [partnerQuery, setPartnerQuery] = useState("");
  const partnerMatches = useMemo(() => {
    const q = partnerQuery.trim().toLowerCase();
    const digits = cuiDigits(q);
    return (partners.data ?? [])
      .filter((p) => p.active)
      .filter((p) => !q || p.name.toLowerCase().includes(q) || (digits.length > 1 && cuiDigits(p.cui).includes(digits)))
      .slice(0, 6);
  }, [partners.data, partnerQuery]);

  // ── codul de deșeu ─────────────────────────────────────────────────────────
  const profileCodes = company.data?.authorizedWasteCodes ?? [];
  const [codeQuery, setCodeQuery] = useState("");
  const codeSearch = useQuery({
    queryKey: ["waste-codes", codeQuery.trim()],
    queryFn: () => api.wasteCodes(auth!, codeQuery.trim()),
    enabled: !!auth && codeQuery.trim().length >= 2,
    retry: 0,
  });
  const shownCodes = codeQuery.trim().length >= 2 ? (codeSearch.data ?? []).slice(0, 8) : profileCodes;
  const isPackaging = wasteCode?.code.startsWith("15 01") ?? false;
  // Codul care își spune singur materialul nu-l mai cere (ca pe web și ca `PackagingMaterial.resolve`).
  const suggestedMaterial = wasteCode ? suggestedPackagingMaterial(wasteCode.code) : null;

  // ── validarea ──────────────────────────────────────────────────────────────
  const isoDate = parseDate(date);
  // Rubricile Anexei 3 se văd (și pleacă) numai cu destinatar ales; fără el nu le cerem și nu le trimitem.
  const isoLoad = partner && loadDate.trim() ? parseDate(loadDate) : null;
  const isoUnload = partner && unloadDate.trim() ? parseDate(unloadDate) : null;
  const amount = Number(quantity.replace(/\./g, "").replace(",", "."));
  const errors = {
    workPoint: !workPointId ? strings.common.requiredField : undefined,
    date: !isoDate ? m.dateFormat : undefined,
    wasteCode: !wasteCode ? t.wasteCodePlaceholder : undefined,
    quantity: !weighed && !(amount > 0) ? strings.common.requiredField : undefined,
    fate: !fate ? strings.common.requiredField : undefined,
    operationCode:
      fate === "RECOVERED" && !operationCode.startsWith("R")
        ? t.recoveryCodeRequired
        : fate === "DISPOSED" && !operationCode.startsWith("D")
          ? t.disposalCodeRequired
          : undefined,
    partner:
      weighed && !partnerId
        ? t.weighingNeedsPartner
        : partner && !partner.authorizationNumber?.trim()
          ? t.partnerNeedsAuthorization
          : undefined,
    physicalState: !physicalState ? t.physicalStateRequired : undefined,
    storageType: !storageType ? t.storageTypeRequired : undefined,
    transportMeans: !transportMeans ? t.transportMeansRequired : undefined,
    wasteDestination: !wasteDestination ? t.wasteDestinationRequired : undefined,
    packagingMaterial:
      isPackaging && !packagingMaterial && !suggestedMaterial ? t.packagingMaterialRequired : undefined,
    packagingCategory:
      isPackaging && packagingOnMarket && !packagingCategory ? t.packagingCategoryRequired : undefined,
    loadDate: partner && loadDate.trim() && !isoLoad ? m.dateFormat : undefined,
    driverCnp: partner && driverCnp.trim() && !isValidCnp(driverCnp.trim()) ? strings.naturalPersons.cnpInvalid : undefined,
    unloadDate:
      partner && unloadDate.trim() && !isoUnload
        ? m.dateFormat
        : isoUnload && isoUnload < (isoLoad ?? isoDate ?? "")
          ? m.unloadBeforeLoad
          : undefined,
  };
  const unconfirmed = Object.keys(pending).length;
  const valid = Object.values(errors).every((v) => !v);

  const save = async () => {
    setShowErrors(true);
    if (!valid || unconfirmed > 0 || !session || !wasteCode || !isoDate) return;
    await enqueue({
      id,
      owner: session.email,
      tenantId: session.tenantId,
      photoUri: photo ?? null,
      payload: {
        workPointId,
        date: isoDate,
        wasteCodeId: wasteCode.id,
        quantity: weighed ? null : amount,
        weighedAtUnloading: weighed,
        unit,
        operation: fate,
        register: "ANEXA_1",
        operationCode,
        partnerId: partnerId || null,
        documentReference: documentReference.trim() || null,
        vehicleRegistration: vehicle.trim() || null,
        ...(partner
          ? {
              partnerWorkPointId: partnerWorkPointId || null,
              loadDate: isoLoad,
              unloadDate: isoUnload,
              anexa3Unit: anexa3Unit || null,
              transportPartnerId: transportPartnerId || null,
              driverName: driverName.trim() || null,
              driverIdentification: driverIdentification.trim() || null,
              driverCnp: driverCnp.trim() || null,
              transportDestinations,
            }
          : {}),
        physicalState,
        storageType,
        transportMeans,
        wasteDestination,
        packagingOnMarket: isPackaging ? packagingOnMarket : null,
        packagingMaterial: isPackaging ? packagingMaterial || null : null,
        packagingCategory: isPackaging && packagingOnMarket ? packagingCategory || null : null,
      },
      summary: {
        wasteCode: wasteCode.code,
        quantity: weighed ? t.awaitingWeighing : `${formatKg(amount)} ${e.unit[unit]}`,
        partnerName: partner?.name ?? null,
        date: isoDate,
      },
    });
    // Cu semnal pleacă pe loc; fără, rămâne în „De trimis”.
    if (auth) {
      drain(auth, session.email)
        .then((sent) => {
          if (sent) queryClient.invalidateQueries({ queryKey: ["movements"] });
        })
        .catch(() => {});
    }
    router.back();
  };

  const err = (k: keyof typeof errors) => (showErrors ? errors[k] : undefined);

  const vehicleField = (
    <Field label={t.vehicleRegistration} read={pending.vehicle} onConfirm={() => confirm("vehicle")} testID="f-vehicle">
      <Input
        value={vehicle}
        onChangeText={(v) => {
          setVehicle(v);
          confirm("vehicle");
        }}
        autoCapitalize="characters"
      />
    </Field>
  );

  return (
    <>
      <Stack.Screen options={{ headerShown: true, title: m.handoverNew, headerBackTitle: m.tabAdd }} />
      <ScrollView
        style={styles.fill}
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
      >
        {photo ? (
          <View style={styles.photoBox}>
            <Image source={{ uri: photo }} style={styles.photo} resizeMode="cover" />
            {reading ? (
              <View style={styles.readingOverlay} testID="aviz-reading">
                <ActivityIndicator color={colors.lcdDigit} />
                <Text style={styles.readingText}>{m.reading}</Text>
              </View>
            ) : null}
          </View>
        ) : null}
        {photo && readNothing ? <Note tone="alert">{m.readNothing}</Note> : null}

        <SectionHead>{t.sectionWaste}</SectionHead>
        <Group>
          <Field label={t.filterWorkPoint} error={err("workPoint")}>
            {workPoints.isError && !workPoints.data ? (
              <Text style={styles.hint}>{m.listUnavailable}</Text>
            ) : (
              <Pills
                testID="wp"
                options={activeWorkPoints.map((w) => ({ value: w.id, label: w.name }))}
                value={workPointId}
                onChange={setWorkPointId}
              />
            )}
          </Field>
          <Sep />
          <Field
            label={t.date}
            read={pending.date}
            onConfirm={() => confirm("date")}
            error={err("date")}
            testID="f-date"
          >
            <Input
              value={date}
              onChangeText={(v) => {
                setDate(v);
                confirm("date");
              }}
              keyboardType="numbers-and-punctuation"
              placeholder="zz.ll.aaaa"
            />
          </Field>
          <Sep />
          <Field
            label={t.wasteCode}
            read={pending.wasteCode}
            onConfirm={() => confirm("wasteCode")}
            error={err("wasteCode")}
            testID="f-code"
          >
            {wasteCode ? (
              <ChosenRow
                title={wasteCode.code}
                sub={wasteCode.name}
                onChange={() => {
                  setWasteCode(null);
                  confirm("wasteCode");
                }}
              />
            ) : (
              <>
                <Input
                  value={codeQuery}
                  onChangeText={setCodeQuery}
                  placeholder={t.wasteCodeSearch}
                  testID="code-search"
                />
                {shownCodes.length === 0 && codeQuery.trim().length < 2 ? (
                  <Text style={styles.hint}>{m.wasteCodeNoProfile}</Text>
                ) : null}
                {shownCodes.map((c) => (
                  <OptionRow
                    key={c.id}
                    title={c.code}
                    sub={c.name}
                    testID="code-option"
                    onPress={() => setWasteCode(c)}
                  />
                ))}
              </>
            )}
          </Field>
        </Group>

        <SectionHead>{t.sectionQuantity}</SectionHead>
        <Group>
          <Field
            label={t.quantity}
            read={pending.quantity}
            onConfirm={() => confirm("quantity")}
            error={err("quantity")}
            testID="f-qty"
          >
            <View style={styles.qtyRow}>
              <Input
                style={styles.qtyInput}
                value={weighed ? "" : quantity}
                editable={!weighed}
                onChangeText={(v) => {
                  setQuantity(v);
                  confirm("quantity");
                }}
                keyboardType="decimal-pad"
                testID="qty"
              />
              <Pills
                options={(["KG", "TONS"] as Unit[]).map((u) => ({ value: u, label: e.unit[u] }))}
                value={unit}
                onChange={(u) => {
                  setUnit(u);
                  confirm("quantity");
                }}
              />
            </View>
          </Field>
          <Sep />
          <View style={styles.switchRow}>
            <View style={styles.switchText}>
              <Text style={rowStyles.title}>{t.weighedAtUnloading}</Text>
            </View>
            <Switch value={weighed} onValueChange={setWeighed} />
          </View>
        </Group>

        <SectionHead>{t.askState}</SectionHead>
        <Group>
          <Field label={t.physicalState} error={err("physicalState")}>
            <Pills testID="state" options={nomenclator(e.physicalState)} value={physicalState} onChange={setPhysicalState} />
          </Field>
        </Group>

        <SectionHead>{t.askHandling}</SectionHead>
        <Group>
          <Field label={t.askStorage} error={err("storageType")}>
            <Pills testID="storage" options={codes(e.storageType)} value={storageType} onChange={setStorageType} />
            {storageType ? <Text style={styles.hint}>{e.storageType[storageType]}</Text> : null}
          </Field>
        </Group>

        <SectionHead>{t.askTransport}</SectionHead>
        <Group>
          <Field label={t.askTransportMeans} error={err("transportMeans")}>
            <Pills
              testID="means"
              options={codes(e.transportMeans)}
              value={transportMeans}
              onChange={setTransportMeans}
            />
            {transportMeans ? <Text style={styles.hint}>{e.transportMeans[transportMeans]}</Text> : null}
          </Field>
          <Sep />
          <Field label={t.askDestination} error={err("wasteDestination")}>
            <Pills
              testID="dest"
              options={offeredDestinations.map((d) => ({ value: d, label: d }))}
              value={wasteDestination}
              onChange={setWasteDestination}
            />
            {wasteDestination ? <Text style={styles.hint}>{e.wasteDestination[wasteDestination]}</Text> : null}
          </Field>
        </Group>

        <SectionHead>{t.fateTitle}</SectionHead>
        <Group>
          <Field label={t.operation} error={err("fate")}>
            <Pills
              testID="fate"
              options={[
                { value: "RECOVERED" as Fate, label: t.fateRecovery },
                { value: "DISPOSED" as Fate, label: t.fateDisposal },
              ]}
              value={fate}
              onChange={(f) => {
                setFate(f);
                if (operationCode && !operationCode.startsWith(f === "RECOVERED" ? "R" : "D")) setOperationCode("");
              }}
            />
          </Field>
          <Sep />
          <Field label={t.operationCode} error={err("operationCode")}>
            {last?.operationCode ? (
              <PrimaryButton
                tone="quiet"
                testID="same-as-last"
                label={m.sameAsLast(last.operationCode, formatDate(last.date))}
                onPress={applyLast}
              />
            ) : null}
            {fate ? (
              <Pills
                testID="op"
                options={codeOptions.map((c) => ({ value: c, label: c }))}
                value={operationCode}
                onChange={setOperationCode}
              />
            ) : null}
            {operationCode ? <Text style={styles.hint}>{e.wasteOperationCode[operationCode]}</Text> : null}
          </Field>
        </Group>

        <SectionHead>{t.sectionRecipient}</SectionHead>
        <Group>
          <Field
            label={t.partner}
            read={pending.partner}
            onConfirm={() => confirm("partner")}
            error={err("partner")}
            testID="f-partner"
          >
            {partner ? (
              <ChosenRow
                title={partner.name}
                sub={partner.cui ?? ""}
                onChange={() => {
                  pickPartner("");
                  confirm("partner");
                }}
              />
            ) : (
              <>
                {unknownCui ? (
                  <UnknownPartner
                    cui={unknownCui}
                    canAdd={canWrite(session?.role)}
                    onAdded={(p) => {
                      setUnknownCui(null);
                      pickPartner(p.id);
                    }}
                  />
                ) : null}
                {partners.isError && !partners.data ? (
                  <Text style={styles.hint}>{m.listUnavailable}</Text>
                ) : (
                  <>
                    <Input
                      value={partnerQuery}
                      onChangeText={setPartnerQuery}
                      placeholder={m.partnerSearch}
                      testID="partner-search"
                    />
                    {partnerMatches.length === 0 ? <Text style={styles.hint}>{m.partnerNone}</Text> : null}
                    {partnerMatches.map((p) => (
                      <OptionRow
                        key={p.id}
                        title={p.name}
                        sub={p.cui ?? ""}
                        testID="partner-option"
                        onPress={() => pickPartner(p.id)}
                      />
                    ))}
                  </>
                )}
              </>
            )}
          </Field>
          {/* Punctul de lucru al destinatarului — numai când are mai multe; cu unul, Anexa 3 îl scrie pe acela. */}
          {partner && partner.workPoints.length > 1 ? (
            <>
              <Sep />
              <Field label={t.partnerWorkPoint}>
                <Pills
                  testID="partner-wp"
                  options={partner.workPoints.map((wp) => ({
                    value: wp.id,
                    label: wp.name ? `${wp.name}, ${wp.address}` : wp.address,
                  }))}
                  value={partnerWorkPointId}
                  onChange={setPartnerWorkPointId}
                />
                <Text style={styles.hint}>{t.partnerWorkPointHint}</Text>
              </Field>
            </>
          ) : null}
        </Group>

        {isPackaging ? (
          <Group>
            <View style={styles.switchRow}>
              <View style={styles.switchText}>
                <Text style={rowStyles.title}>{t.packagingOnMarket}</Text>
                <Text style={rowStyles.sub}>{m.packagingOnWeb}</Text>
              </View>
              <Switch value={packagingOnMarket} onValueChange={setPackagingOnMarket} />
            </View>
            <Sep />
            <Field label={t.packagingMaterial} error={err("packagingMaterial")}>
              <Pills
                testID="material"
                options={PACKAGING_MATERIALS.map((v) => ({ value: v, label: e.packagingMaterial[v] }))}
                value={packagingMaterial || suggestedMaterial}
                onChange={setPackagingMaterial}
              />
              {!packagingMaterial && suggestedMaterial ? (
                <Text style={styles.hint}>{`${e.packagingMaterial[suggestedMaterial]} ${t.packagingFromCode}`}</Text>
              ) : null}
            </Field>
            {packagingOnMarket ? (
              <>
                <Sep />
                <Field label={t.packagingCategory} error={err("packagingCategory")}>
                  <Pills
                    testID="category"
                    options={nomenclator(e.packagingCategory)}
                    value={packagingCategory}
                    onChange={setPackagingCategory}
                  />
                </Field>
              </>
            ) : null}
          </Group>
        ) : null}

        {/* Anexa 3 (la periculoase, doar transportul): aceleași rubrici și aceeași ordine ca pe web
            (`TransportFields`), numai când există destinatar — fără el nu pleacă nimic nicăieri. */}
        {partner ? (
          <>
            <SectionHead>{wasteCode?.hazardous ? t.sectionTransport : t.anexa3Section}</SectionHead>
            <Group>
              <Field label={t.loadDate} error={err("loadDate")} testID="f-load">
                <Input
                  value={loadDate}
                  onChangeText={setLoadDate}
                  keyboardType="numbers-and-punctuation"
                  placeholder="zz.ll.aaaa"
                />
                <Text style={styles.hint}>{t.loadDateHint}</Text>
              </Field>
              <Sep />
              <Field label={t.unloadDate} error={err("unloadDate")} testID="f-unload">
                <Input
                  value={unloadDate}
                  onChangeText={setUnloadDate}
                  keyboardType="numbers-and-punctuation"
                  placeholder="zz.ll.aaaa"
                />
              </Field>
              {!wasteCode?.hazardous ? (
                <>
                  <Sep />
                  <Field label={t.anexa3Unit}>
                    <Pills
                      testID="a3unit"
                      options={[
                        { value: "COMPANY", label: t.anexa3UnitCompany },
                        { value: "KG", label: e.unit.KG },
                        { value: "TONS", label: e.unit.TONS },
                      ]}
                      value={anexa3Unit || "COMPANY"}
                      onChange={(v) => setAnexa3Unit(v === "COMPANY" ? "" : (v as Unit))}
                    />
                  </Field>
                </>
              ) : null}
              <Sep />
              <Field label={t.transportPartner}>
                <Pills
                  testID="carrier"
                  options={[
                    { value: "OWN", label: m.carrierOwn },
                    ...carrierOptions.map((p) => ({ value: p.id, label: p.name })),
                  ]}
                  value={transportPartnerId || "OWN"}
                  onChange={(v) => {
                    // Șoferii sunt ai transportatorului: schimbi firma, alegerea nu mai e a ei. Textul rămâne.
                    setTransportPartnerId(v === "OWN" ? "" : v);
                    setDriverId("");
                  }}
                />
              </Field>
              {availableDrivers.length > 0 ? (
                <>
                  <Sep />
                  <Field label={t.driverPick}>
                    <Pills
                      testID="driver"
                      options={[
                        ...availableDrivers.map((d) => ({ value: d.id, label: d.name })),
                        { value: "OTHER", label: m.driverOther },
                      ]}
                      value={driverId || "OTHER"}
                      onChange={(v) => (v === "OTHER" ? setDriverId("") : pickDriver(v))}
                    />
                  </Field>
                </>
              ) : null}
              <Sep />
              <Field label={t.driverName} testID="f-driver">
                <Input value={driverName} onChangeText={setDriverName} autoCapitalize="words" />
              </Field>
              <Sep />
              <Field label={t.driverIdentification} testID="f-driver-id">
                <Input
                  value={driverIdentification}
                  onChangeText={setDriverIdentification}
                  placeholder={t.driverIdentificationPlaceholder}
                  autoCapitalize="characters"
                />
              </Field>
              <Sep />
              <Field label={strings.common.cnp} error={err("driverCnp")} testID="f-driver-cnp">
                <Input value={driverCnp} onChangeText={setDriverCnp} keyboardType="number-pad" maxLength={13} />
              </Field>
              <Sep />
              {vehicleField}
              <Sep />
              <Field label={t.askTransportDestinations}>
                <MultiPills
                  testID="tdest"
                  options={(Object.keys(e.transportDestination) as TransportDestination[]).map((d) => ({
                    value: d,
                    label: e.transportDestination[d],
                  }))}
                  value={transportDestinations}
                  onChange={(v) => {
                    setTransportDestinations(v);
                    setDestinationsPrefilled(false);
                  }}
                />
                <Text style={styles.hint}>
                  {destinationsPrefilled ? t.destinationsPrefilled : t.transportDestinationsHint}
                </Text>
              </Field>
            </Group>
          </>
        ) : null}

        <SectionHead>{t.askDocument}</SectionHead>
        <Group>
          <Field
            label={t.documentReference}
            read={pending.documentReference}
            onConfirm={() => confirm("documentReference")}
            testID="f-doc"
          >
            <Input
              value={documentReference}
              onChangeText={(v) => {
                setDocumentReference(v);
                confirm("documentReference");
              }}
              placeholder={t.documentReferencePlaceholder}
              autoCapitalize="characters"
            />
          </Field>
          {/* Fără destinatar mașina stă aici: avizul citit din poză o poate aduce oricum. */}
          {!partner ? (
            <>
              <Sep />
              {vehicleField}
            </>
          ) : null}
        </Group>

        {photo ? <Text style={styles.hint}>{m.photoAttached}</Text> : null}
        {unconfirmed > 0 ? <Note tone="alert">{m.confirmPending(unconfirmed)}</Note> : null}
        <PrimaryButton label={m.save} onPress={save} disabled={reading} testID="handover-save" />
      </ScrollView>
    </>
  );
}

/**
 * G4 — un CUI citit care nu e partener. Căutarea la ANAF se face la apăsare; partenerul se creează
 * numai cu ce a arătat ANAF și cu cele două întrebări fără de care serverul nu-l primește (ce face
 * cu deșeul, cine facturează). Nimic nu se creează tăcut.
 */
function UnknownPartner({ cui, canAdd, onAdded }: { cui: string; canAdd: boolean; onAdded: (p: Partner) => void }) {
  const { auth, session } = useSession();
  const queryClient = useQueryClient();
  const [asked, setAsked] = useState(false);
  const [type, setType] = useState<PartnerType | "">("");
  const [role, setRole] = useState<"supplier" | "client">("supplier");
  // Serverul nu primește un colector sau un valorificator fără numărul autorizației de mediu.
  const [authorizationNumber, setAuthorizationNumber] = useState("");
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);

  const lookup = useQuery({
    queryKey: ["company-lookup", cui],
    queryFn: () => api.companyLookup(auth!, cui),
    enabled: asked && !!auth,
    retry: 0,
  });

  const add = async () => {
    if (!lookup.data || !type || !authorizationNumber.trim() || !auth) return;
    setAdding(true);
    setAddError(null);
    try {
      const created = await api.createPartner(auth, {
        name: lookup.data.name,
        cui: lookup.data.cui,
        address: lookup.data.address,
        tradeRegisterNumber: lookup.data.tradeRegisterNumber,
        authorizationNumber: authorizationNumber.trim(),
        type,
        client: role === "client",
        supplier: role === "supplier",
      });
      await queryClient.invalidateQueries({ queryKey: ["partners", session?.tenantId] });
      onAdded(created);
    } catch (error) {
      setAddError(error instanceof api.ApiError && error.serverMessage ? error.serverMessage : m.addPartnerFailed);
    } finally {
      setAdding(false);
    }
  };

  const notFound = lookup.error instanceof api.ApiError && lookup.error.status === 404;
  const offline = lookup.isError && !(lookup.error instanceof api.ApiError);

  return (
    <View style={styles.unknown} testID="unknown-cui">
      <Text style={styles.unknownText}>{m.unknownCui(cui)}</Text>
      {!asked ? (
        <PrimaryButton tone="quiet" label={m.anafLookup} onPress={() => setAsked(true)} testID="anaf-lookup" />
      ) : lookup.isLoading ? (
        <ActivityIndicator />
      ) : notFound ? (
        <Text style={styles.hint}>{m.anafNotFound}</Text>
      ) : offline ? (
        <Text style={styles.hint}>{m.anafOffline}</Text>
      ) : lookup.isError ? (
        <Text style={styles.hint}>{m.anafNotFound}</Text>
      ) : lookup.data ? (
        <View style={{ gap: 8 }}>
          <Text style={rowStyles.title}>{lookup.data.name}</Text>
          {lookup.data.address ? <Text style={rowStyles.sub}>{lookup.data.address}</Text> : null}
          {lookup.data.inactive ? <Text style={styles.warn}>{m.anafInactive}</Text> : null}
          {canAdd ? (
            <>
              <Text style={styles.subLabel}>{m.partnerWhatDoes}</Text>
              <Pills
                testID="ptype"
                options={(["COLLECTOR", "RECOVERER"] as PartnerType[]).map((v) => ({
                  value: v,
                  label: e.partnerType[v],
                }))}
                value={type}
                onChange={setType}
              />
              <Text style={styles.subLabel}>{m.partnerWhoInvoices}</Text>
              <Pills
                testID="prole"
                options={[
                  { value: "supplier" as const, label: e.partnerRole.supplier },
                  { value: "client" as const, label: e.partnerRole.client },
                ]}
                value={role}
                onChange={setRole}
              />
              <Text style={styles.subLabel}>{strings.partners.authorizationNumber}</Text>
              <Input
                value={authorizationNumber}
                onChangeText={setAuthorizationNumber}
                placeholder={strings.partners.authorizationNumberPlaceholder}
                testID="pauth"
              />
              {addError ? <Text style={styles.warn}>{addError}</Text> : null}
              <PrimaryButton
                label={m.addPartner}
                onPress={add}
                disabled={!type || !authorizationNumber.trim() || adding}
                testID="add-partner"
              />
            </>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

function ChosenRow({ title, sub, onChange }: { title: string; sub: string; onChange: () => void }) {
  return (
    <View style={styles.chosen}>
      <View style={{ flex: 1 }}>
        <Text style={rowStyles.mono}>{title}</Text>
        {sub ? (
          <Text style={rowStyles.sub} numberOfLines={2}>
            {sub}
          </Text>
        ) : null}
      </View>
      <PrimaryButton tone="quiet" label={m.change} onPress={onChange} />
    </View>
  );
}

function OptionRow({
  title,
  sub,
  onPress,
  testID,
}: {
  title: string;
  sub: string;
  onPress: () => void;
  testID?: string;
}) {
  return (
    <Text onPress={onPress} testID={testID} style={styles.option} suppressHighlighting={false}>
      <Text style={rowStyles.mono}>{title}</Text>
      {sub ? <Text style={rowStyles.sub}>{`  ${sub}`}</Text> : null}
    </Text>
  );
}

/** Pastilele unui nomenclator scurt, cu eticheta întreagă. */
function nomenclator<T extends string>(labels: Record<T, string>) {
  return (Object.keys(labels) as T[]).map((v) => ({ value: v, label: labels[v] }));
}

/** Pastilele unui nomenclator cu etichete lungi: numai codul; denumirea stă sub ele, după alegere. */
function codes<T extends string>(labels: Record<T, string>) {
  return (Object.keys(labels) as T[]).map((v) => ({ value: v, label: v }));
}

function Sep() {
  return <View style={rowStyles.sep} />;
}

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/** `zz.ll.aaaa` → `yyyy-MM-dd`, sau null dacă nu e o zi adevărată. */
function parseDate(text: string): string | null {
  const match = text.trim().match(/^(\d{1,2})[./-](\d{1,2})[./-](\d{4})$/);
  if (!match) return null;
  const [day, month, year] = [Number(match[1]), Number(match[2]), Number(match[3])];
  const d = new Date(Date.UTC(year, month - 1, day));
  if (d.getUTCMonth() !== month - 1 || d.getUTCDate() !== day) return null;
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { padding: 16, gap: 8, paddingBottom: 60 },
  photoBox: { borderRadius: radius.group, overflow: "hidden", height: 180, backgroundColor: colors.lcd },
  photo: { width: "100%", height: "100%" },
  readingOverlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: "rgba(8,12,10,0.6)",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  },
  readingText: { fontFamily: fonts.monoMedium, fontSize: 14, color: colors.lcdDigit, letterSpacing: 0.6 },
  hint: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.ink2 },
  warn: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.redText },
  subLabel: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.ink2, marginTop: 4 },
  qtyRow: { flexDirection: "row", alignItems: "center", gap: 10 },
  qtyInput: { flex: 1, fontFamily: fonts.monoMedium },
  switchRow: { flexDirection: "row", alignItems: "center", gap: 12, paddingHorizontal: 16, paddingVertical: 12 },
  switchText: { flex: 1 },
  chosen: { flexDirection: "row", alignItems: "center", gap: 12 },
  option: { paddingVertical: 10, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  unknown: { backgroundColor: colors.ground, borderRadius: 12, padding: 12, gap: 8 },
  unknownText: { fontFamily: fonts.sansMedium, fontSize: 14.5, color: colors.ink },
});
