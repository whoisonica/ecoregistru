/**
 * Central Romanian UI strings. Keep all user-facing text here so it can be
 * extracted into an i18n framework later without hunting through components.
 */
export const strings = {
  appName: "WasteHouse",
  tagline: "Evidența și raportarea gestiunii deșeurilor",
  // Rândul mic verde cu linie de deasupra titlului, pe paginile publice — același ca pe landing.
  publicKicker: "Evidența gestiunii deșeurilor",

  header: {
    // Shown above the nav: the current company. For PLATFORM_ADMIN it's a tenant switcher.
    currentCompany: "Firma curentă",
    selectCompany: "Alege firma",
    noCompanySelected: "Nicio firmă selectată",
    platformAdmin: "Administrator platformă",
    loadCompaniesError: "Nu am putut încărca firmele.",
    // Ecranul pe care îl vede administratorul de platformă înainte să aleagă o firmă. Până acum
    // ecranele se randau oricum, cereau date fără `X-Tenant-Id` şi primeau 400 — patru cereri
    // roşii în consolă la fiecare autentificare —, iar Panoul scria „Eşti la zi" peste ele.
    // Un ecran de firmă fără firmă n-are ce arăta: se spune asta, şi se arată de unde se alege.
    pickCompanyTitle: "Alege o firmă ca să vezi ecranul",
    pickCompanyHint:
      "Ecranele de evidență sunt ale unei firme anume. Alege una din „Firma curentă”, sus în bara laterală — sau deschide Clienți, de unde se administrează toate.",
    pickCompanyAction: "Deschide Clienți",
    pickCompanyActionConsultant: "Deschide firmele mele",
  },

  nav: {
    packaging: "Ambalaje",
    // „Acasă", nu „Panou": panoul e bara din stânga de când există direcția „Cântar” (15.09.2026),
    // iar ecranul de pornire e locul unde se vede ce e de făcut azi.
    dashboard: "Acasă",
    movements: "Mișcări",
    // Trei ecrane, după registru și direcție (proprietarul, 15.09.2026): generatorul vede
    // „Generare"; colectorul „Intrări" și „Ieșiri", **separate** („să poți să vezi tot frumos");
    // „Generator și colector" le vede pe toate trei. Vezi `lib/movementScreens.ts`.
    movementsGenerator: "Generare",
    movementsIn: "Intrări",
    movementsOut: "Ieșiri",
    kwMovementsIn: "intrare primit preluat de la firme persoane fizice cântar depozit art. 48",
    kwMovementsOut: "ieșire plecat predat valorificat eliminat cod R/D anexa 3 aviz anexa 2 art. 48",
    // Ecranul depozitului: operațiunea de cântar, cu liniile ei. Numai la firmele cu registrul art. 48.
    weighing: "Cântar",
    kwWeighing:
      "cântar operațiune bon brut tara neto acceptat sortiment borderou de achiziție intrare ieșire persoană fizică preț reținere AFM impozit depozit",
    evidences: "Evidențe",
    partners: "Parteneri",
    deadlines: "Termene",
    auditFile: "Dosar de control",
    clients: "Clienți",
    // P2.13, felia 2 — panoul cabinetului, numai la consultant.
    consultancyOverview: "Firmele mele",
    kwConsultancyOverview:
      "cabinet toate firmele portofoliu termene depășite blocaje autorizații parteneri ce am de făcut consultant",
    settings: "Setări",
    logout: "Deconectare",
    // Grupurile din bara laterală. Nouă intrări plate nu spun nimic despre ce ține de ce; patru
    // grupuri de două-trei spun.
    groupRecords: "Evidență",
    groupReporting: "Raportare",
    groupSetup: "Configurare",
    groupAdmin: "Administrare",

    /**
     * Cuvintele după care se mai găsește un ecran în paletă (Ctrl+K), pe lângă numele lui.
     *
     * <p>Sunt numele **documentelor** și vorbele clientului, nu sinonime de dicționar: nimeni nu
     * caută „evidențe", toată lumea caută „fișa" sau „anexa 1". Nu se compară cu diacritice — și
     * textul căutat, și astea trec prin `fold()`.
     *
     * <p>„Anexa 1" apare dinadins la două ecrane. Numele scurt înseamnă chiar două documente
     * (decizia 12): declarația de ambalaje la Ambalaje, fișa din HG 856/2002 la Evidențe. A alege
     * unul singur ca „adevăratul" ar ascunde celălalt document exact de cine îl caută pe nume;
     * amândouă apar, cu eticheta lor, iar omul alege.
     */
    kwDashboard: "acasă start situație stoc alerte ce am de făcut",
    kwMovements:
      "adaugă mișcare predare generare intrare ieșire transport aviz cântar anexa 3 dovada predării cod R/D",
    kwEvidences:
      "anexa 1 fișa de evidență evidența gestiunii deșeurilor generate centralizată declarația anuală centralizator HG 856/2002 regenerează tone",
    kwPackaging:
      "anexa 1 ambalaje anexa 3 ambalaje Ordinul 794/2012 tabelul 1 tabelul 2 xls pus pe piață",
    kwDeadlines: "scadențe termene 15 martie 25 februarie 25 ianuarie AFM SIM alerte",
    kwAuditFile: "arhivă inspector Garda de Mediu control dosar zip",
    kwPartners: "clienți furnizori colector valorificator transportator șoferi autorizație CUI",
    kwSettings:
      "puncte de lucru secții generatori interni șoferii noștri datele firmei CAEN persoana desemnată",
    kwClients: "firme companii tenant cereri de cont profil de piață cabinet consultanți echipa",
    billing: "Abonament",
    kwBilling: "facturi factură plată plătește transfer pachet preț FGO scadență restanță",
    importExcel: "Import din Excel",
    kwImport: "import excel xlsx șablon istoric încarcă parteneri mișcări implementare",
    groupCabinet: "Cabinet",
  },

  /**
   * Panoul — bara din stânga, direcția „Cântar” (docs/stil-interfata.md, `todo-ui-cantar.md`).
   *
   * <p>Pornește de la cântarul de la colector: firma ca etichetă, afișajul lunii cu cel mai urgent
   * lucru, tastele de adăugare, apoi meniul cu o tastă pe fiecare intrare și un indicator la
   * dreapta. Indicatorii doar citesc cifre calculate deja pe Acasă — nicio regulă nouă de business.
   */
  panel: {
    collapse: "Strânge panoul",
    expand: "Desfă panoul",
    // Tasta e scrisă în etichetă, ca reper; scurtătura o leagă `Layout`.
    collapseKey: "[",
    searchEverywhere: "Caută oriunde",
    searchKey: "Ctrl K",
    companyPick: "Alege firma",
    companySearch: "Caută firma…",
    companyNeedsAttention: "Cer atenție",
    companyFine: "În regulă",
    companyNone: "Nicio firmă aleasă",
    // Afișajul lunii: eticheta e luna cu majuscule, cifra e în kg, rândul de jos e cel mai urgent
    // lucru — același verdict ca banda „Următoarea acțiune" de pe Acasă.
    monthGenerated: "generat",
    monthReceived: "intrat",
    kg: "kg",
    // Când o sursă n-a răspuns, afișajul scrie „?", nu „0" (decizia 68).
    unknown: "?",
    // Tastele de adăugare, după tipul firmei (§5 din todo-ui-cantar.md).
    addWaste: "Adaugă deșeuri",
    addOwnWaste: "Deșeuri proprii",
    addInbound: "Intrare",
    addOutbound: "Ieșire",
    // Indicatorii din meniu: cifre deja calculate pe Acasă, scrise scurt.
    indAwaitingWeighing: "{n} de cântărit",
    indMissingCode: "{n} fără cod R/D",
    indOverdue: "{n} depășite",
    indOverdueOne: "1 depășit",
    indExpiring: "{n} expiră",
    indUnknown: "?",
    // Jos în panou.
    billing: "Abonament",
    billingDays: "{n} zile",
    billingDayOne: "1 zi",
    billingReadOnly: "doar citire",
    billingPastDue: "restanță",
    billingCancelled: "oprit",
    accountMenu: "Meniul contului",
    shortcuts: "Scurtături",
    shortcutsTitle: "Scurtături de tastatură",
    shortcutsHint:
      "Tastele lucrează oriunde pe ecran, în afara rubricilor de text. Cifrele urmează ordinea meniului din panou.",
    shortcutNav: "Deschide ecranul cu numărul respectiv din meniu",
    shortcutSearch: "Caută oriunde: un ecran sau o acțiune",
    shortcutFind: "Sari în caseta de căutare a listei",
    shortcutNew: "Acțiunea principală a ecranului: adaugă (deșeuri, partener, punct de lucru)",
    shortcutIn: "Intrare de deșeuri (la colector)",
    shortcutOut: "Ieșire de deșeuri (la colector)",
    shortcutCollapse: "Strânge sau desface panoul",
    shortcutEscape: "Închide dialogul sau meniul deschis",
    // Telefon: bara de jos.
    barHome: "Acasă",
    barAdd: "Adaugă",
    barDeadlines: "Termene",
    barMore: "Mai mult",
    barAddTitle: "Ce adaugi?",
  },

  // Aplicația de telefon (`mobile/`, todo-mobil.md). Tot ce e deja aici pe web se ia de acolo;
  // aici stă doar ce spune numai telefonul.
  mobile: {
    tabHome: "Acasă",
    tabAdd: "Adaugă",
    tabControl: "Control",
    // Afișajul lunii. `/movements/summary` numără toate mișcările lunii, în ambele sensuri, deci
    // eticheta spune „înregistrat", nu „predat".
    lcdLabel: (month: string) => `${month.toUpperCase()} · ÎNREGISTRAT`,
    lcdState: "KG",
    lcdMovements: (n: number) => (n === 1 ? "1 mișcare în lună" : `${n} mișcări în lună`),
    lcdEmpty: "Nicio mișcare în luna asta",
    lcdError: "Nu am putut încărca luna",
    previousMonth: "Luna anterioară",
    nextMonth: "Luna următoare",
    serverUnreachable: "Nu ajung la server. Verifică legătura la internet și încearcă din nou.",
    comingSoon: "Ecranul ăsta vine în versiunea următoare a aplicației.",
    account: "Contul",

    // --- M1a ---
    // Consultantul și administratorul de platformă aleg firma dintr-o listă; ceilalți n-au ce alege.
    pickCompany: "Alege firma",
    pickCompanyEmpty: "Nicio firmă pe contul ăsta.",
    noCompanyYet: "Alege firma ca să vezi luna.",
    company: "Firma",
    // Afișajul generatorului spune „predat”, fiindcă cifra e pe direcția de ieșire (`direction=OUT`);
    // la colector luna are și intrări, deci rămâne „înregistrat”.
    lcdLabelOut: (month: string) => `${month.toUpperCase()} · PREDAT`,
    lcdLabelIn: (month: string) => `${month.toUpperCase()} · PRIMIT`,
    monthMovements: "Mișcările lunii",
    movementsEmpty: "Nicio mișcare în luna asta.",
    movementsError: "Nu am putut încărca mișcările.",
    awaitingWeighing: "cântărire la destinatar",
    missingOperationCode: "fără cod R/D",
    deadlinesEmpty: "Niciun termen deschis.",
    deadlinesError: "Nu am putut încărca termenele.",
    deadlineDue: (date: string) => `Termen ${date}`,
    deadlineDone: (date: string) => `Bifat ${date}`,
    devices: "Dispozitive conectate",
    devicesEmpty: "Niciun telefon conectat.",
    devicesError: "Nu am putut încărca dispozitivele.",
    deviceThis: "telefonul ăsta",
    deviceLastUsed: (date: string) => `Folosit ${date}`,
    // Bara de jos a colectorului: „Intrare” albastru în locul listei de generare (todo-mobil §6).
    tabIn: "Intrări",
    tabOut: "Ieșiri",

    // --- M1b: „Pozează avizul” și coada de predări ---
    handoverNew: "Predare nouă",
    snapAviz: "Pozează avizul",
    pickFromGallery: "Alege poza din galerie",
    withoutPhoto: "Completează fără poză",
    addHint: "Camera citește avizul, tu confirmi fiecare câmp. Nimic nu intră în registru până nu apeși „Salvează”.",
    // Colectorul pur n-are „Generare”: intrările lui trec prin cântar, care vine la M2.
    collectorLater: "Intrările la cântar vin într-o versiune următoare a aplicației.",
    cameraDenied: "Fără acces la cameră nu pot poza avizul. Îl poți da din Setările telefonului, sau alegi poza din galerie.",
    reading: "Citesc avizul…",
    readNothing: "N-am putut citi nimic din poză. Completează de mână — poza rămâne atașată predării.",
    readFrom: (line: string) => `Din poză: „${line}”`,
    confirmRead: "Corect",
    confirmPending: (n: number) =>
      n === 1 ? "Confirmă câmpul citit din poză." : `Confirmă cele ${n} câmpuri citite din poză.`,
    photoAttached: "Poza avizului se atașează predării.",
    // G3 — codul R/D e obligatoriu la predare. Propunerea se aplică numai la apăsare.
    sameAsLast: (code: string, date: string) => `La fel ca data trecută: ${code} (${date})`,
    partnerSearch: "Caută după nume sau CUI",
    partnerNone: "Niciun partener nu se potrivește.",
    change: "Schimbă",
    // G4 — partenerul după CUI.
    unknownCui: (cui: string) => `CUI-ul ${cui} de pe aviz nu e printre partenerii firmei.`,
    anafLookup: "Caută la ANAF",
    anafNotFound: "ANAF nu găsește CUI-ul ăsta. Verifică poza sau alege partenerul din listă.",
    anafInactive: "ANAF îl arată inactiv fiscal.",
    anafOffline: "Fără semnal nu pot căuta la ANAF. Alege partenerul din listă sau adaugă-l când ai semnal.",
    partnerWhatDoes: "Ce face cu deșeul",
    partnerWhoInvoices: "Cine facturează",
    addPartner: "Adaugă partenerul",
    addPartnerFailed: "Nu am putut adăuga partenerul.",
    packagingOnWeb: "Materialul și categoria ambalajului se completează pe web.",
    wasteCodeNoProfile: "Scrie codul sau denumirea ca să cauți în nomenclator.",
    listUnavailable: "Lista nu e pe telefon încă. Deschide ecranul o dată cu semnal.",
    dateFormat: "Scrie data ca zz.ll.aaaa.",
    save: "Salvează predarea",
    // Coada: ce n-a ajuns încă pe server.
    outboxTitle: "De trimis",
    outboxHint: "Pleacă singure când telefonul prinde semnal.",
    outboxPending: "de trimis",
    outboxRejected: "refuzată",
    outboxPhotoFailed: "Predarea e în registru; poza n-a urcat.",
    outboxRemove: "Scoate",
    outboxSendNow: "Trimite acum",

    // --- M1c: „A venit controlul” ---
    // Rândurile sunt exact ce socotește Panoul web (`lib/readiness.ts`), nu lista din prototip: ce nu
    // verifică aplicația nicăieri nu se afirmă nici pe telefon.
    controlTitle: "A venit controlul",
    controlLcdLabel: "PREGĂTIT PENTRU CONTROL",
    controlLcdState: "ACUM",
    controlOf: (n: number) => `din ${n}`,
    controlAllOk: "Nimic de văzut acum",
    controlUnknown: "Un rând n-a putut încărca",
    controlChecks: "Ce găsește inspectorul",
    controlOk: "în regulă",
    controlWarn: "de văzut",
    controlBad: "de rezolvat",
    controlUnknownChip: "?",
    controlUnknownRow: "Nu am putut încărca. Deschide ecranul din nou când ai semnal.",
    checkDeadlines: "Termene",
    checkDeadlinesOk: "Niciunul depășit",
    checkMissingCode: (year: number) => `Codul R/D pe predări, ${year}`,
    checkMissingCodeOk: "Completat pe toate ieșirile",
    checkWeighing: "Cântăriri la destinatar",
    checkWeighingOk: "Nicio ieșire nu așteaptă cântarul",
    checkPartners: "Autorizațiile partenerilor",
    checkPartnersOk: "Niciuna aproape de expirare",
    checkPartnerOne: (name: string, date: string) => `${name}: valabilă până pe ${date}`,
    // Dosarul (D5): ZIP-ul de pe web, dat foii de partajare a telefonului — Mail, WhatsApp, Drive.
    sendDossier: "Trimite dosarul",
    sendDossierBusy: "Se pregătește arhiva...",
    sendDossierHint: "Aceeași arhivă ca „Dosar de control” pe web. Alegi tu cui o trimiți.",
    dossierYear: (year: number) => `Anul ${year}`,
    dossierThreeYears: "Ultimii 3 ani",
    sendDossierOffline: "Dosarul se face pe server. Fără semnal nu îl pot pregăti.",
    shareUnavailable: "Telefonul ăsta nu poate trimite fișiere din aplicație.",
  },

  login: {
    title: "Intră în cont",
    subtitle: "Cu emailul și parola contului tău.",
    // Verdele din stânga (direcția „Poster”, 16.09.2026): promisiunea produsului, în limbajul
    // patronului, și trei tile-uri cu ecrane care chiar există în aplicație.
    posterHeadline: "Evidența deșeurilor,",
    posterAccent: "fără dosarul de hârtie.",
    posterLede: "Registrul, anexele și termenele legale, ținute pe punct de lucru și gata de depus.",
    tile1Kicker: "Anexa 1",
    tile1Title: "Generare",
    tile1Note: "gata de depus",
    tile2Kicker: "Termen",
    tile2Title: "15 martie",
    tile2Note: "declarația anuală",
    tile3Kicker: "Dosar de control",
    tile3Title: "Un singur PDF",
    tile3Note: "tot ce cere Garda de Mediu",
    noAccount: "Nu ai cont?",
    requestAccount: "Cere un cont",
    email: "Email",
    password: "Parolă",
    submit: "Intră în cont",
    loading: "Se autentifică...",
    forgotPassword: "Ai uitat parola?",
    genericError: "Autentificare eșuată. Verifică datele și încearcă din nou.",
    // Arătat o singură dată, după ce un 401 pe o cerere autentificată a închis sesiunea. Până pe
    // 24.08 utilizatorul ateriza aici fără niciun cuvânt și credea că s-a stricat aplicația.
    sessionExpired:
      "Sesiunea a expirat, din motive de siguranță. Autentifică-te din nou ca să continui.",
  },

  // Unde aterizează linkul din mail — și din invitație, fiindcă o invitație e tot o resetare:
  // contul se creează dezactivat, iar alegerea parolei e ce îl activează.
  resetPassword: {
    title: "Alege-ți parola",
    subtitle:
      "Dacă ai fost invitat în WasteHouse, parola pe care o pui acum îți activează contul.",
    password: "Parolă nouă",
    confirmPassword: "Confirmă parola",
    rules: "Minim 8 caractere, cu literă mare, literă mică și cifră.",
    submit: "Salvează parola",
    saving: "Se salvează...",
    mismatch: "Parolele nu coincid.",
    done: "Parola a fost salvată și contul e activ. Te poți autentifica.",
    toLogin: "Mergi la autentificare",
    missingCode:
      "Linkul e incomplet — îi lipsește codul. Copiază-l din email întreg, sau cere unul nou.",
    requestNew: "Cere un link nou",
    genericError: "Nu am putut salva parola. Linkul poate fi expirat — cere unul nou.",
    // Stânga paginii, în direcția „Poster” (ca loginul și cererea de cont).
    posterHeadline: "Încă un pas",
    posterAccent: "și intri în evidență.",
    posterLede: "Dacă ai fost invitat, parola îți activează contul. Dacă ai uitat-o, o înlocuiește pe cea veche.",
    posterFacts: [
      "Linkul din email e valabil 30 de minute",
      "Contul invitat devine activ odată cu parola",
      "Evidența și documentele firmei rămân neatinse",
    ],
    haveAccount: "Ai deja parolă?",
    goToLogin: "Autentifică-te",
  },

  forgotPassword: {
    title: "Ai uitat parola?",
    subtitle:
      "Scrie adresa de email a contului și îți trimitem un link de resetare. Linkul e valabil 30 de minute.",
    email: "Email",
    submit: "Trimite linkul",
    sending: "Se trimite...",
    // Același mesaj și când adresa n-are cont: backend-ul nu spune cine e înregistrat, iar
    // ecranul n-are voie să spună în locul lui.
    sent: "Dacă adresa are un cont, linkul de resetare e pe drum.",
    sentHint: "Verifică și în Spam. Linkul expiră în 30 de minute.",
    backToLogin: "Înapoi la autentificare",
    genericError: "Nu am putut trimite linkul. Încearcă din nou.",
    posterHeadline: "Parola se schimbă,",
    posterAccent: "evidența rămâne.",
    posterLede: "Îți trimitem pe email un link cu care îți alegi o parolă nouă.",
    posterFacts: [
      "Invitația expirată se reînnoiește tot de aici",
      "Verifică și în Spam, dacă nu vine în câteva minute",
      "Evidența și documentele firmei rămân neatinse",
    ],
    remembered: "Ți-ai amintit parola?",
    goToLogin: "Autentifică-te",
  },

  dashboard: {
    title: "Acasă",
    subtitle: "Ce e de făcut azi și cum stă evidența pe anul în curs.",
    welcome: "Bine ai venit",
    addMovement: "Adaugă mișcare",
    // Cuvântul de pe LED-ul benzii „Următoarea acțiune": cât de urgent e.
    toneNow: "Acum",
    toneSoon: "Curând",
    toneOk: "La zi",
    toneStart: "De început",
    toneUnknown: "Necunoscut",
    // stat tiles
    statMovements: "Mișcări luna aceasta",
    statMovementsSub: "înregistrări în {month}",
    statDeadlines: "Termene de făcut",
    statDeadlinesSub: "din care {overdue} depășite",
    statExpiring: "Autorizații care expiră",
    // sections
    upcomingTitle: "Termene următoare",
    upcomingEmpty: "Niciun termen deschis pentru anul curent.",
    expiringTitle: "Autorizații care expiră curând",
    expiringEmpty: "Nicio autorizație de partener aproape de expirare.",
    viewAll: "Vezi toate",
    loadError: "Nu am putut încărca datele panoului.",

    // --- Starea de conformitate: întrebarea la care panoul trebuie să răspundă ---
    statusTitle: "Starea evidenței pe {year}",
    statusOk: "Nimic nu blochează documentele",
    // Aceeaşi grijă ca la bandă, o casetă mai jos: pe un an fără nicio linie, „Fişa de evidenţă şi
    // declaraţia se pot tipări aşa cum sunt" e adevărat şi nefolositor — se pot tipări **goale**.
    // Zero blocaje şi zero de raportat sunt două lucruri diferite.
    statusEmpty: "Nu e nimic de verificat pe {year}",
    statusEmptyHint:
      "Evidența anului e goală: nu s-a înregistrat nicio mișcare. Nu e nimic care să blocheze depunerea, dar nici nimic de depus.",
    statusOkHint:
      "Toate ieșirile au cod R/D și cantitate. Fișa de evidență și declarația se pot tipări așa cum sunt.",
    // Cele două feluri de a fi „nu gata", cu urmarea fiecăruia.
    // Complementul se acordă și el: „1 linie cu ieșiri" era corect la plural și fals la singular.
    // Rescris fără număr în el, ca să nu mai depindă de cifră.
    blockerMissingCode: "{count} fără cod R/D la ieșire",
    blockerMissingCodeHint:
      "Cantitatea a plecat de pe amplasament, dar nu intră în nicio coloană oficială. Fișa nu se poate depune așa.",
    blockerAwaitingWeighing: "{count} care așteaptă cântarul",
    blockerAwaitingWeighingHint:
      "Ieșiri cântărite la destinatar. Așteptare legitimă, dar cifrele sunt provizorii până vine bonul.",
    blockerFix: "Vezi liniile",

    // --- Cifrele care contează ---
    // „Generat" ar fi o afirmație falsă: suma e pe toate mișcările lunii, iar o ieșire nu e o
    // generare. Iar generarea o deduce motorul din ieșiri (decizia 17), deci n-ar fi nici măcar
    // suma rândurilor cu operațiunea „Generare".
    statGenerated: "Cantitate înregistrată în {month}",
    // Numărul de mișcări stă lângă kilograme fiindcă răspunde la altceva: cifra spune *cât*, iar
    // numărul spune *dacă se ține evidența la zi*. A fost pe dală ca cifră principală până pe
    // 08.09, când dala a trecut pe kilograme; se întoarce aici, unde nu concurează cu ea.
    statGeneratedSub: "kilograme, pe {count} din luna aceasta",
    statGeneratedSubNone: "kilograme — nicio mișcare înregistrată luna aceasta",
    // Zilele nu se mai socotesc aici: `daysLabel` le scrie o singură dată pentru toată aplicația,
    // cu „azi" și „mâine" în cuvinte și cu acordul numeralului. Șirul ăsta spunea „în 1 zile" — și,
    // mai rău, „în 0 zile" chiar în ziua termenului.
    statDeadlinesNext: "Următorul termen: {label}, {days}",
    // Slot gol dinadins: aici trebuie să se acorde și **adjectivul**, nu doar substantivul, deci
    // toată sintagma intră în perechea dată lui `countOf` („1 termen depășit" · „2 termene
    // depășite" · „20 de termene depășite").
    statDeadlinesOverdue: "{count}",
    statDeadlinesNone: "Niciun termen deschis",
    statExpiringDays: "expiră în {count}",
    // Ziua expirării şi cea dinaintea ei se scriu în cuvinte, ca la termene (`daysLabel`):
    // `countOf` chemat cu 0 dădea „expiră în 0 de zile" — corect gramatical, citit ca o eroare —
    // iar 1 dădea „expiră în 1 zi" acolo unde restul aplicaţiei scrie „mâine". E chiar greşeala
    // pe care felia din 09.09 a reparat-o pentru `statDeadlinesNext`, rămasă cu o casetă mai jos.
    statExpiringToday: "expiră azi",
    statExpiringTomorrow: "expiră mâine",
    statExpiringPast: "expirată",

    // --- Următoarea acțiune ---
    // Panoul spunea *starea*, în cinci locuri deodată — trei dale, o casetă de blocaje și două
    // liste — și lăsa clientul să tragă singur concluzia. Banda de sus numește **un singur** lucru,
    // cel mai scump dintre cele deschise, și drumul către el. Nu aduce nicio cifră nouă pe ecran:
    // ce lipsea era propoziția.
    //
    // Ordinea e după ce costă mai mult dacă rămâne nefăcut: un termen depășit curge deja, o ieșire
    // fără cod R/D blochează depunerea următoare, un termen apropiat se poate încă prinde, o
    // autorizație aproape expirată se poate reînnoi, iar cântarul e o așteptare legitimă (decizia
    // 13) — deci ultimul.
    nextTitle: "Următoarea acțiune",
    nextOverdue: "{count} depășite",
    nextOverdueHint:
      "Depunerea se face oricum; marchează termenul finalizat după ce ai depus, cu numărul de înregistrare în notă.",
    nextOverdueCta: "Vezi termenele",
    nextMissingCode: "Completează codul R/D pe {count}",
    nextMissingCodeHint:
      "Cantitatea a plecat de pe amplasament și nu intră în nicio coloană oficială — fișa nu se poate depune așa.",
    nextDeadline: "{label} — {days}",
    nextDeadlineHint: "Documentul se scoate din aplicație; termenul se marchează finalizat după depunere.",
    nextDeadlineCta: "Vezi termenul",
    nextExpiring: "{count} cu autorizația aproape expirată",
    nextExpiringHint:
      "Predarea e legală doar către un operator autorizat (OUG 92/2021 art. 23), iar răspunderea rămâne a ta.",
    nextExpiringCta: "Vezi partenerii",
    nextWeighing: "{count} așteaptă cântarul",
    nextWeighingHint:
      "Cifra vine de la destinatar. Până atunci linia e provizorie — nu e o greșeală, dar documentele o poartă așa.",
    nextWeighingCta: "Vezi mișcările",
    nextNothing: "Ești la zi",
    nextNothingHint:
      "Niciun termen deschis apropiat, nicio linie de lămurit și nicio autorizație pe terminate.",
    // Contul pe care nu s-a înregistrat încă nimic. „Eşti la zi" e **adevărat** acolo — n-are
    // nimic de făcut din ce ştie aplicaţia —, dar e răspunsul la altă întrebare: cine tocmai a
    // primit contul întreabă „de unde încep?", iar un verde cu bifă îi spune că a terminat.
    //
    // Cele două trepte nu sunt ghicite: o mişcare se înregistrează **pe** un punct de lucru, deci
    // fără el nu se poate scrie nimic — chiar formularul de mişcare o spune, în `noWorkPointHint`.
    // Iar evidenţa, fişa şi declaraţiile se calculează **din** mişcări, deci fără prima mişcare
    // n-are ce raporta. Amândouă sunt dependenţe din cod, nu preferinţe de flux.
    nextStartWorkPoint: "Adaugă primul punct de lucru",
    nextStartWorkPointHint:
      "O mișcare se înregistrează pe un punct de lucru, deci ăsta e primul pas. Adresa lui e cea care ajunge pe fișa de evidență.",
    nextStartWorkPointCta: "Deschide Setări",
    nextStartMovement: "Înregistrează prima mișcare",
    nextStartMovementHint:
      "Evidența, fișa Anexa 1 și declarațiile se construiesc din mișcări — fiecare intrare și ieșire de deșeu. Până nu e înregistrată una, nu e nimic de raportat.",
    nextStartMovementCta: "Adaugă mișcare",
    // „Primii pași" (16.09.2026): drumul unui cont nou până la primul document. Pașii se bifează singuri.
    firstSteps: {
      title: "Primii pași",
      progress: "{done} din {total} gata",
      hide: "Ascunde",
      done: "Gata",
      todo: "De făcut",
      company: "Verifică datele firmei",
      companyHintDone: "Adresa, codul CAEN și persoana desemnată sunt completate. Ele se tipăresc pe fișa de evidență și pe declarația anuală.",
      companyHintMissing: "Lipsesc: {fields}. Se tipăresc pe fișa de evidență și pe declarația anuală; necompletate, rubricile ies goale.",
      companyHintAskUs: "Scrie-ne la contact@wastehouse.ro și le completăm.",
      companyHintClients: "Le completezi din Clienți.",
      companyCta: "Vezi datele",
      companyClientsCta: "Deschide Clienți",
      fieldAddress: "adresa",
      fieldCaen: "codul CAEN",
      fieldWasteManager: "persoana desemnată cu gestiunea deșeurilor",
      workPoint: "Adaugă punctul de lucru",
      workPointHint: "Locul unde apar deșeurile. Fiecare mișcare se înregistrează pe un punct de lucru, iar adresa lui ajunge pe fișă.",
      workPointCta: "Deschide Setări",
      partner: "Adaugă firma care îți preia deșeurile",
      partnerHint: "Colectorul sau operatorul căruia îi predai, cu numărul și data autorizației de mediu. Aplicația te anunță înainte să-i expire.",
      partnerCta: "Deschide Parteneri",
      movement: "Înregistrează prima mișcare",
      movementHint: "Evidența și anexele se construiesc din mișcări.",
      importHintSendUs: "Dacă ții deja evidența într-un tabel Excel, trimite-ne-o la contact@wastehouse.ro și o aducem noi în aplicație.",
      importHintPlatform: "Evidența din Excel a clientului se aduce din import.",
      movementCta: "Adaugă",
      importCta: "Importă din Excel",
    },
    // A treia stare a benzii, pe lângă „ai de făcut" şi „eşti la zi": **nu se ştie**.
    //
    // Garda de dinainte (`nextActionLoading`) acoperea numai cererile în zbor. O cerere **căzută**
    // iese din `isLoading` cu `data` nedefinit, deci `?? []` dădea liste goale şi banda scria
    // „Eşti la zi" — probat pe firma demo, cu 9 termene depăşite şi o linie fără cod R/D, cu cele
    // trei surse răspunzând 500. Un verde fals e cea mai scumpă propoziţie din aplicaţia asta:
    // clientul închide laptopul liniştit peste o depunere blocată. Tăcerea nu ajunge — ecranul
    // trebuie să spună că n-a putut citi.
    nextUnknown: "Nu am putut verifica starea",
    nextUnknownHint:
      "Cel puțin una dintre surse nu a răspuns, deci nu se poate spune nici că e ceva de făcut, nici că nu e. Reîncarcă pagina; dacă se repetă, ecranele de mai jos spun care nu răspunde.",
    nextUnknownCta: "Reîncarcă",
    // Aceeaşi grijă pentru caseta de conformitate: „Nimic nu blochează documentele" e o afirmaţie
    // despre linii care n-au fost citite.
    statusUnknown: "Nu am putut citi evidența",
    statusUnknownHint:
      "Lista liniilor de evidență nu a venit, deci nu se poate spune dacă ceva blochează depunerea. Reîncarcă pagina.",
    statLoadError: "nu s-a putut încărca",
    // Şi listele de jos afirmau: „Niciun termen deschis" peste o listă care n-a venit e acelaşi
    // fals ca „Eşti la zi", doar cu litere mai mici. S-a văzut abia pe captura reparaţiei.
    listLoadError: "Lista nu a putut fi încărcată. Reîncarcă pagina.",
  },

  movements: {
    // Registrul art. 48, pe două ecrane (proprietarul, 15.09.2026): ce a intrat și ce a ieșit.
    // `title`/`subtitle`/`add` rămân numele generice, pentru formular și pentru linkurile vechi.
    title: "Intrări și ieșiri de deșeuri",
    subtitle: "Deșeurile preluate de la terți și cele predate mai departe (registrul art. 48), pe puncte de lucru și luni.",
    add: "Adaugă mișcare",
    inTitle: "Intrări de deșeuri",
    inSubtitle: "Ce ai primit de la firme și de la persoane fizice. Intră în evidența art. 48, nu în fișa ta de gestiune.",
    inAdd: "Intrare",
    inAddTitle: "Intrare de deșeuri",
    inEditTitle: "Editează intrarea",
    outTitle: "Ieșiri de deșeuri",
    outSubtitle: "Ce a plecat din ce ai primit: la cine, cu ce cod R/D și cu ce document.",
    outAdd: "Ieșire",
    outAddTitle: "Ieșire de deșeuri",
    outEditTitle: "Editează ieșirea",
    // Totalurile de deasupra listei: cifrele lunii (sau ale anului) din filtru, socotite de server
    // peste toate rândurile, nu peste pagina adusă. Fără cântar = „de cântărit", nu zero.
    totGenerated: "Generat",
    totRecovered: "Valorificat",
    totDisposed: "Eliminat",
    totReceived: "Primit",
    totFromCompanies: "De la firme",
    totFromPersons: "De la persoane fizice",
    totLeft: "Plecat",
    totAwaiting: "De cântărit",
    totMissingCode: "Fără cod R/D",
    totRows: "rânduri",
    totRow: "rând",
    totalsError: "Totalurile nu s-au putut citi.",
    colFrom: "De la",
    colTo: "Către",
    colRdCode: "Cod R/D",
    colDocuments: "Documente",
    colState: "Stare",
    // Evidența art. 48 (AD închisă 15.09.2026): pe anul din filtru, xlsx pentru portal, PDF pentru control.
    art48Menu: "Evidența cronologică",
    art48Xlsx: "Descarcă .xlsx (pentru portal)",
    art48Pdf: "Descarcă PDF (pentru control)",
    art48Hint: "Anul {year}: tabelul cronologic și totalurile pentru chestionarul SIM „Colectare/Tratare”, în tone.",
    art48Error: "Evidența cronologică nu s-a putut genera.",
    addTitle: "Adaugă mișcare",
    editTitle: "Editează mișcarea",
    // Colectorul pur nu vede „Generare" (proprietarul, 14.09.2026), dar legea îi cere Anexa 1 pentru
    // deșeul propriu (HG 856/2002 art. 2 alin. (1)) — deci ecranul spune unde se trece.
    collectorOwnWasteHint:
      "Deșeul generat în activitatea proprie a firmei (de exemplu, refuzul de la sortare) intră pe fișa Anexa 1 și se înregistrează la „Generare”. Ecranul apare când firma e de tipul „Generator și colector”.",
    // „Generare" — Anexa 1: deșeul firmei și ce se întâmplă cu el. Pe 14.09.2026 specialista a
    // cerut „Ieșiri"; proprietarul l-a numit în aceeași seară după ce se înregistrează: generarea.
    generatorTitle: "Generare de deșeuri",
    generatorSubtitle: "Deșeurile generate de firmă și ce se întâmplă cu ele, pentru fișa Anexa 1 și rapoartele de generator.",
    // „Adaugă deșeuri" pe generator, „Deșeuri proprii" acolo unde firma e și colector (proprietarul,
    // 15.09.2026): numele spune ce face omul, nu ce coloană se umple.
    generatorAdd: "Adaugă deșeuri",
    generatorAddOwn: "Deșeuri proprii",
    generatorAddTitle: "Adaugă deșeuri",
    generatorEditTitle: "Editează generarea",
    // Linkul din rapoarte a adus un id care nu mai e printre mișcările lunii: ștearsă între timp,
    // sau o adresă veche. Se spune, în loc să se deschidă un formular gol.
    movementNotFound: "Mișcarea cerută nu mai există sau a fost ștearsă.",
    // Reperele formularului lung. Sunt titluri, nu uși: secțiunile nu se pliază, fiindcă jumătate
    // din ele conțin rubrici obligatorii.
    sectionWaste: "Deșeul",
    sectionQuantity: "Cantitatea",
    sectionOperation: "Operațiunea",
    sectionHandling: "Depozitare și tratare",
    sectionTransport: "Transport",
    sectionRecipient: "Destinatarul",
    sectionDocument: "Document și observații",
    sectionAttachments: "Atașamente",
    // Banda de sus: unde ajunge cantitatea, spusă înainte de salvare.
    effectTitle: "Ce face mișcarea asta",
    effectAnexa1: "Intră pe fișa de evidență (Anexa 1)",
    effectArt48: "Intră în registrul art. 48 — marfă preluată, nu deșeul firmei",
    effectRecovered: "Iese ca valorificare, cu codul {code}",
    effectDisposed: "Iese ca eliminare, cu codul {code}",
    effectPackaging: "Intră și în declarația de ambalaje (Anexa 1 Ambalaje)",
    effectAnexa3: "Se poate tipări Anexa 3 pentru predarea asta",
    effectAnexa2: "Se poate tipări Anexa 2 (transport periculos) pentru predarea asta",
    effectIncomplete: "Alege codul de deșeu ca să vezi unde ajunge cantitatea.",
    empty: "Nicio mișcare pentru filtrele alese.",
    emptyHint: "Schimbă luna sau punctul de lucru, ori adaugă prima mișcare a perioadei.",
    searchPlaceholder: "Caută după cod, partener, punct de lucru, document...",
    duplicate: "Duplică mișcarea",
    duplicateTitle: "Mișcare nouă, pornită de la alta",
    // Urcarea atașamentelor e secvențială și poate dura: fără semn, arată a aplicație blocată.
    uploadingFile: "Se încarcă fișierul {n} din {total}: {name}",
    uploadingWait: "Nu închide fereastra până nu se termină.",
    // Coloana arăta numărul și atât, iar singurul drum către fișier trecea prin formularul de
    // treizeci de rubrici — pe care un VIEWER nici nu-l poate deschide, fiindcă butonul „Editează"
    // stă sub `canWrite`. Deci pentru jumătate din roluri atașamentul era o cifră, nu un document.
    attachmentsView: "Vezi atașamentele",
    attachmentsDialogTitle: "Atașamentele mișcării",
    attachmentsDialogHint:
      "Fișierele se deschid într-un tab nou. Se adaugă și se șterg din editarea mișcării.",
    attachmentOpening: "Se deschide...",
    loadError: "Nu am putut încărca mișcările.",
    // filters
    filterMonth: "Luna",
    filterWorkPoint: "Punct de lucru",
    filterWasteCode: "Cod deșeu",
    filterAll: "Toate",
    allMonths: "Toate lunile",
    clearFilters: "Șterge filtrele",
    // Golul dintr-o lună anume nu se spune ca golul din tot: unul e o listă goală, celălalt e o
    // firmă fără nicio mișcare. Al doilea îngrijorează pe drept, primul n-are de ce.
    emptyMonth: "Nicio mișcare în {month}",
    emptyMonthHint:
      "Ecranul pornește pe luna curentă. Alege altă lună sau vezi anul întreg — mișcările vechi sunt acolo.",
    showWholeYear: "Vezi tot anul {year}",
    // columns
    colDate: "Data",
    colWasteCode: "Cod deșeu",
    colOperation: "Operațiune",
    colQuantity: "Cantitate",
    colPartner: "Operator",
    colInternalGenerator: "Secția",
    colWorkPoint: "Punct de lucru",
    colDocument: "Document",
    colAttachments: "Atașamente",
    // form fields
    date: "Data",
    wasteCode: "Cod deșeu",
    wasteCodePlaceholder: "Alege un cod de deșeu",
    wasteCodeSearch: "Caută după cod sau denumire…",
    quantity: "Cantitate",
    unit: "Unitate",
    weighedAtUnloading: "Se cântărește la descărcare",
    weighedAtUnloadingHint:
      "Bifează dacă nu ai cântar și cantitatea o stabilește destinatarul la descărcare. Mișcarea se salvează fără cantitate, iar formularul de transport se tipărește cu rubrica goală — se completează pe loc, după cântărire.",
    awaitingWeighing: "De cântărit",
    awaitingWeighingHint:
      "Deșeul a plecat, dar cantitatea nu e încă știută. Completeaz-o când primești cântarul de la destinatar.",
    missingCode: "Fără cod R/D",
    missingCodeHint:
      "Ieșire înregistrată înainte ca aplicația să ceară codul de operațiune. Cantitatea a plecat din stoc, dar nu intră nici la „Valorificat”, nici la „Eliminat”. Deschide mișcarea și alege codul.",
    volumeM3: "Volum (mc)",
    volumeM3Hint: "Singura măsură pe care o ai fără cântar. Nu ține loc de kilograme în evidență.",
    operation: "Operațiune",
    physicalState: "Stare fizică",
    physicalStatePlaceholder: "— fără —",
    storageAndTreatment: "Stocare, tratare și transport (cap. 2)",
    transportMeans: "Transport — mijlocul",
    wasteDestination: "Transport — destinația",
    storageType: "Depozitare până la predare — tipul",
    treatmentMethod: "Tratare — ce se face",
    treatmentMethodHint: "Ce alegi aici apare pe fișă la „Modul”. La „— fără —” apare „-”.",
    treatmentPurpose: "Tratare — scopul",
    treatmentPurposeHint:
      "Nu se alege: îl dă unde pleacă deșeul. V la valorificare, E la eliminare — așa apare în coloana „Scopul” de pe fișă.",
    wasteDestinationRequired: "Alege destinația: DO, I, Vr sau A.",
    partnerNeedsAuthorization:
      "Partenerul n-are trecut numărul autorizației de mediu. Cine preia deșeul trebuie să fie autorizat: completează-l în fișa lui, la Parteneri.",
    nomenclatorPlaceholder: "— fără —",
    operationGeneratorHint:
      "Mișcarea pornește de la generare. Ce se întâmplă cu deșeul după — pleacă spre valorificare sau spre eliminare — se alege mai jos, după transport.",
    // --- Ce se întâmplă cu deşeul: sub transport, nu în capul formularului ---
    fateTitle: "Ce se întâmplă cu deșeul",
    fateHint:
      "Se alege după transport, fiindcă de el atârnă: transportul spre valorificare cere un cod R, cel spre eliminare un cod D.",
    fateRequired: "Alege unde pleacă deșeul: spre valorificare sau spre eliminare.",
    fateRecovery: "Transport spre valorificare",
    fateRecoveryEffect:
      "Cantitatea se raportează la „Valorificată”, cu un cod R și cu operatorul care o face (cap. 3).",
    fateDisposal: "Transport spre eliminare",
    fateDisposalEffect:
      "Cantitatea se raportează la „Eliminată final”, cu un cod D și cu operatorul care o face (cap. 4).",
    operationCode: "Cod operațiune (R/D)",
    // Cine e „agentul economic” și ce cod poartă: notele 5 și 6 la anexa 3 și nota 3 la anexa 7 din
    // OUG 92/2021, plus corpusul (docs/surse-oficiale.md §1.2, închis 16.09.2026). Se scrie operațiunea
    // destinatarului direct, nu a celui de după el.
    operationCodeHintRecovery:
      "Alege ce face cu deșeul firma căreia i-l predai, după autorizația ei de mediu: R13 dacă doar îl depozitează, R12 dacă îl sortează sau îl balotează, R3, R4 ori R5 dacă îl reciclează chiar ea. Nu scrie ce se întâmplă mai departe, la altă firmă.",
    operationCodeHintDisposal:
      "Alege ce face cu deșeul firma căreia i-l predai, după autorizația ei de mediu: D15 dacă doar îl depozitează până la eliminare, D13 dacă îl sortează sau îl amestecă, D1 dacă e depozitul de deșeuri. Nu scrie ce se întâmplă mai departe, la altă firmă.",
    partner: "Operatorul care efectuează operațiunea",
    partnerPlaceholder: "— noi, pe amplasamentul propriu —",
    partnerHint:
      "Lasă gol dacă operațiunea o faci tu. Dacă predai deșeul, alege partenerul: el e „agentul economic care efectuează operația” din cap. 3 / cap. 4.",
    internalGenerator: "Generator intern (Secția)",
    internalGeneratorPlaceholder: "— fără —",
    internalGeneratorHint: "Sursa din punctul de lucru. Se tipărește în coloana „Secția” din cap. 2.",
    documentReference: "Referință document",
    documentReferencePlaceholder: "ex. aviz / factură",
    notes: "Note",
    attachments: "Atașamente",
    hazardous: "Periculos",
    // Anexa 3 — dovada predării
    anexa3NeedsPartner:
      "Anexa 3 (dovada predării) apare după ce alegi partenerul care preia deșeul — fără destinatar, formularul n-are ce tipări.",

    // --- Autorizația destinatarului la data predării (OUG 92/2021 art. 23 alin. (1)) ---
    // Avertisment, niciodată refuz: predarea a avut loc, iar un refuz ar bloca reconstituirea
    // unui dosar vechi. Și nu se tipărește pe Anexa 3 — hârtia ajunge la inspector, iar o notă
    // a noastră pe ea ar fi propria noastră acuzație pusă în dosarul clientului.
    authExpiredAtHandover: "Autorizație expirată",
    // Primește data deja formatată: strings.ts nu importă nimic, ca să rămână un modul de text pur.
    authExpiredAtHandoverHint: (expiryFormatted: string | null) =>
      `Autorizația de mediu a destinatarului${
        expiryFormatted ? ` expirase la ${expiryFormatted}` : " expirase"
      }, adică înainte de data acestei predări. Legea cere predarea către un operator autorizat (OUG 92/2021, art. 23 alin. (1)), iar predarea nu te descarcă de răspundere (art. 24 alin. (1)). Formularul se tipărește oricum — documentul consemnează ce s-a întâmplat. Verifică dacă partenerul are o autorizație reînnoită și actualizeaz-o în fișa lui.`,

    // --- Codul-oglindă declarat nepericulos (OUG 92/2021 art. 8 alin. (2)) ---
    // Singurul avertisment din modul care se uită la o ÎNCADRARE, nu la o rubrică goală: restul
    // spun „lipsește un câmp", ăsta spune „ai afirmat ceva ce actul cere să fie dovedit". De aceea
    // nu blochează și nu se tipărește nicăieri — e o întrebare pusă clientului, nu o constatare
    // pusă în mâna inspectorului.
    mirrorCode: "Cod-oglindă",
    // Primește codurile periculoase perechi, așa cum le-a scris Decizia 2014/955/UE în chiar numele
    // codului nepericulos — deci nu e o listă de-a noastră, e textul actului citit înapoi.
    mirrorCodeHint: (pair: string | null) =>
      `Codul ăsta are pereche periculoasă${pair ? ` (${pair})` : ""}: același deșeu se încadrează sub două coduri, după cum are sau nu caracteristici periculoase. Încadrarea ca nepericulos se face „numai în baza unei analize a originii, testelor, buletinelor de analiză și a altor documente relevante” (OUG 92/2021, art. 8 alin. (2)), iar la mișcarea asta nu e atașat niciun document. Mișcarea rămâne înregistrată și formularele se tipăresc — atașează buletinul sau analiza care susține încadrarea, ca la un control să iasă din dosar.`,

    // --- Provenienţa deşeului la ieşire ---
    // Se arată doar la conturile care pot prelua de la terţi. Fiecare opţiune îşi spune efectul,
    // fiindcă alegerea nu schimbă un câmp, ci pe ce formular oficial ajunge cantitatea.
    originCollected:
      "Preluare de la terți: intră automat în registrul cronologic art. 48, niciodată în Anexa 1.",

    // --- Anexa 1 Ambalaje: se cer doar pe coduri 15 01 xx ---
    packagingSection: "Ambalaje — pentru Anexa 1",
    packagingSectionHint:
      "Codul e de ambalaje. Dar Anexa 1 Ambalaje raportează ambalajul pe care l-ai pus TU pe piața națională, nu orice deșeu de ambalaj — cutiile în care ți-a venit marfa le-a pus pe piață furnizorul tău. Deci se bifează, nu se deduce din cod.",
    packagingOnMarket: "Ambalaj pus de noi pe piața națională",
    packagingOnMarketHint:
      "Bifează dacă firma ta a introdus ambalajul ăsta pe piață, odată cu marfa vândută. Nebifat, cantitatea rămâne în evidența gestiunii deșeurilor ca oricare alta — doar că nu intră în Anexa 1 Ambalaje.",
    packagingLegacy:
      "Mișcare înregistrată înainte de a exista întrebarea: intră în Anexa 1 Ambalaje ca până acum. Bifează sau debifează ca să confirmi.",
    packagingMaterial: "Materialul ambalajului",
    packagingMaterialPlaceholder: "Alege materialul",
    packagingFromCode: "(propus din cod)",
    packagingMaterialNeeded:
      "Codul nu spune din ce material e: 15 01 04 acoperă și aluminiul, și oțelul. Alege tu, altfel cantitatea nu intră în tabel.",
    packagingCategory: "Felul ambalajului",
    packagingCategoryPlaceholder: "Alege felul",
    packagingCategoryHint:
      "Coloana din tabelul 1: desfacere (col. 1), primar (col. 3) sau secundar și de transport (col. 5).",
    packagingReusable: "Ambalaj reutilizabil",
    packagingHazardous: "A conținut substanțe periculoase",
    anexa3Section: "Anexa 3 — dovada predării",
    anexa3SectionHint:
      "Formularul de încărcare-descărcare deșeuri nepericuloase (HG 1061/2008), tipărit din această mișcare. Completează ce apare pe hârtie.",
    recordWeight: "Adaugă cantitatea",
    // Pe rând, scurt: butonul stă pe fiecare linie de cântărit, iar numele întreg e în `aria-label`.
    recordWeightShort: "Cântar",
    // Rândurile venite de la cântar se schimbă numai prin operațiunea lor (BUG-018).
    openWeighing: "La cântar",
    /** P1.11 — drumul de la rând la jurnalul lui. */
    history: "Istoric",
    recordWeightTitle: "Cantitatea cântărită la descărcare",
    recordWeightHint:
      "Cifra pe care ți-a trimis-o destinatarul după cântărire. Până o completezi, luna rămâne provizorie în evidență și rubrica de pe Anexa 3 iese goală.",
    recordWeightSaved: "Cantitatea a fost înregistrată.",
    recordWeightError: "Cantitatea nu a putut fi înregistrată.",
    loadDate: "Data încărcării",
    loadDateHint: "Goală, se tipărește data mișcării.",
    anexa3Unit: "Unitatea tipărită",
    anexa3UnitCompany: "Ca la firmă",
    anexa3UnitHint:
      "Actul are „tone”, dar evidența se ține în kilograme. Cifra se convertește la tipărire, deci cifra și unitatea de pe hârtie sunt mereu de acord.",
    // Un singur exemplar în PDF de pe 15.09.2026, la cererea specialistei; cine ce exemplar ia
    // rămâne scris aici, unde nu costă nimic.
    anexa3Copies:
      "Formularul se deschide într-un exemplar; tipărește-l de câte ori ai nevoie: pentru tine (expeditor), destinatar și transportator.",
    anexa3Download: "Anexa 3",
    anexa3Downloading: "Se generează...",
    anexa3Error: "Formularul nu a putut fi generat.",
    avizDownload: "Aviz de însoțire",
    avizDownloading: "Se generează...",
    avizError: "Avizul nu a putut fi generat.",

    // --- Anexa 2 — transportul deșeurilor periculoase (HG 1061/2008) ---
    // Art. 8: „Expeditorul completează, semnează și ștampilează formularul". Prima citire, făcută
    // pe un exemplar completat, dedusese contrariul — că îl dă colectorul. De aceea propoziția din
    // hint spune cine îl completează: e singurul lucru pe care un client nu-l ghicește corect.
    anexa2Section: "Anexa 2 — transport deșeuri periculoase",
    anexa2SectionHint:
      "Formularul de expediție/transport (HG 1061/2008). Art. 8 spune că îl completează, semnează și ștampilează expeditorul — adică tu.",
    anexa2NeedsPartner:
      "Anexa 2 (formularul de transport periculos) apare după ce alegi partenerul care preia deșeul — fără destinatar, formularul n-are ce tipări.",
    anexa2Number: "Nr. formularului",
    anexa2NumberHint:
      "Îl dă agenția județeană pentru protecția mediului, nu aplicația — nota *1) a modelului. Lasă-l gol dacă n-ai primit unul; nu inventăm un număr pe un formular oficial.",
    anexa2ApprovalNumber: "Nr. formularului de aprobare",
    anexa2ApprovalNumberHint:
      "Aprobarea transportului (anexa 1 la același act), cerută de art. 7 peste 1 t/an. Sub prag rămâne gol: art. 6 alin. (1) scoate tocmai aprobarea.",
    anexa2Packaging: "Număr și tip de ambalaje",
    anexa2PackagingPlaceholder: "ex. 2 butoaie metalice de 200 l",
    anexa2Threshold: "Cantitatea generată într-un an",
    anexa2ThresholdFollows: "Cum reiese din evidență ({proposal})",
    anexa2ThresholdFollowsUnknown: "Cum reiese din evidență",
    anexa2ThresholdAfterSave:
      "Cifra apare după ce salvezi mișcarea: pragul se citește din evidența anului, pe codul ăsta.",

    anexa2Below: "Sub 1 t/an (fără aprobare)",
    anexa2Above: "Peste 1 t/an (cu aprobare)",
    // Cifra stă lângă bifă fiindcă bifa singură ar fi o decizie luată în tăcere pe un cuvânt pe
    // care actul nu-l definește: art. 2 trimite „categoria" la un act abrogat.
    anexa2ThresholdProposed:
      "{tons} t generate în {year} pe {code}. Bifa e propusă din cifra asta, pe cod — actul nu definește „aceeași categorie de deșeuri periculoase”, așa că rămâne alegerea ta.",
    anexa2GroupWarning:
      "⚠️ Pe grupa {group} cumulul anului e {tons} t, adică peste prag. Dacă „categoria” din act înseamnă grupa, nu codul, atunci transportul are nevoie de aprobare (art. 7) — iar lipsa ei e contravenție de 10.000–20.000 lei.",
    anexa2Copies3:
      "Sub prag formularul iese în 3 exemplare (art. 15 alin. (2)): unul la tine, unul la destinatar, unul la transportator. Destinatarul depune și el unul la agenție — art. 6 alin. (2).",
    anexa2Copies6:
      "Peste prag formularul iese în 6 exemplare (art. 12): cele trei părți, agenția care a aprobat transportul, ISU-ul județului tău și agenția județului tău. Un exemplar îl duci tu la ISU, pentru autorizarea rutei (art. 4 alin. (8) lit. b)) — notificarea de 48 de ore o face ISU-ul mai departe, nu tu (art. 14).",
    anexa2EmptyColumns:
      "Cantitatea primită, cea recepționată, cea respinsă și data primirii ies goale, intenționat: sunt declarațiile transportatorului și destinatarului, semnate la primirea deșeurilor (art. 9 și 10). Se completează pe hârtie, de ei.",
    // --- Drumul aprobării, spus pe ecran (HG 1061/2008, art. 4, 5, 7 și 14) ---
    // Peste prag e singurul loc din aplicație unde clientul poate crede că a terminat când n-a
    // terminat: îi tipărim rubrica „nr. formularului de aprobare" și nu-i spunem nimic despre cum
    // se obține. Pașii se scriu întregi, cu **cine** face fiecare, fiindcă asta e partea pe care
    // un client n-o ghicește corect — hârtia trece prin patru mâini înainte să se întoarcă la el.
    anexa2ApprovalMissing:
      "Peste 1 t/an transportul are nevoie și de formularul de aprobare din anexa 1 (art. 7). Aplicația nu-l generează încă — iar drumul lui e mai lung decât pare, și nu e tot al tău:",
    anexa2ApprovalRoadTitle: "Drumul formularului de aprobare, pas cu pas",
    anexa2ApprovalRoad: [
      { who: "Tu", what: "completezi formularul din anexa 1", basis: "art. 4 alin. (2)" },
      {
        who: "Tu",
        what: "îl trimiți destinatarului, semnat și ștampilat",
        basis: "art. 4 alin. (3)",
      },
      {
        who: "Destinatarul",
        what: "acceptă sau cere lămuriri; dacă acceptă, semnează și ștampilează",
        basis: "art. 4 alin. (4)",
      },
      {
        who: "Destinatarul",
        what: "îl duce la agenția de mediu din raza instalației LUI — nu a ta",
        basis: "art. 4 alin. (5)",
      },
      {
        who: "Agenția",
        what: "are 7 zile lucrătoare de la primirea tuturor informațiilor",
        basis: "art. 4 alin. (5)",
      },
      {
        who: "Agenția",
        what: "semnează, după ce verifică operațiile și regimul ariilor protejate / Natura 2000",
        basis: "art. 4 alin. (7)",
      },
      {
        who: "Agenția instalației",
        what: "îl trimite înapoi ție și agenției din județul tău",
        basis: "art. 4 alin. (8) lit. a)",
      },
      {
        who: "Tu",
        what: "îl duci la ISU-ul județului tău, pentru autorizarea rutei",
        basis: "art. 4 alin. (8) lit. b)",
      },
      {
        who: "ISU-ul",
        what: "anunță, cu 48 de ore înainte, celelalte ISU-uri și polițiile de pe traseu",
        basis: "art. 14 alin. (1)",
      },
    ],
    // Pasul 9 e capcana: prima variantă a documentației noastre l-a scris ca obligație a
    // clientului, și greșeala ajunsese pe ecran. Dacă noi am confundat-o citind actul, clientul o
    // confundă sigur — deci se spune pe față, nu se lasă dedus din tabel.
    anexa2ApprovalRoadMine:
      "Din cei nouă pași, ai tăi sunt trei: 1, 2 și 8. Notificarea de 48 de ore (pasul 9) o face ISU-ul după ce primește formularele, nu tu — e partea cel mai des confundată.",
    anexa2ApprovalValidity:
      "Nu reiei drumul la fiecare cursă: art. 5 — aprobarea poate fi dată pentru un singur transport sau pentru mai multe, iar în al doilea caz e valabilă 2 ani de la acordare.",
    anexa2Medical:
      "Deșeurile periculoase din activitatea medicală au alt flux: art. 24 cere ca formularul să-l întocmească TRANSPORTATORUL, pe cantitatea cumulată a unui transport dintr-o zonă, cu o anexă a expeditorilor. Nu e documentul pe care îl tipărește generatorul.",
    // Specialista, 14.09.2026: „anexa 2 o păstrăm doar pentru colectori".
    anexa2ByCollector:
      "Deșeu periculos: formularul de transport (Anexa 2) îl întocmește colectorul care preia deșeul.",
    anexa2Download: "Anexa 2",
    anexa2Downloading: "Se generează...",
    anexa2Error: "Formularul nu a putut fi generat.",
    unloadDate: "Data descărcării",
    transportPartner: "Transportator",
    partnerWorkPoint: "Punctul de lucru al destinatarului",
    partnerWorkPointHint:
      "Unde s-a descărcat marfa. Se tipărește pe Anexa 3, la destinatar.",
    partnerWorkPointOnly: "Singurul punct de lucru al partenerului",
    transportPartnerPlaceholder: "— transportăm noi —",
    carrierGroup: "Transportatori",
    otherPartnersGroup: "Alți parteneri",
    allPartnersGroup: "Parteneri",
    transportPartnerHint:
      "Cei bifați „Transportator” în Parteneri stau primii. Poate fi ales oricare partener.",
    transportPartnerNoneHint:
      "Niciun partener nu e bifat „Transportator” încă. Bifa se pune în Parteneri și aduce cu ea licența de transport și șoferii.",
    driverPick: "Alege delegatul",
    driverPickFreeText: "— altcineva (scriu mai jos) —",
    driverPickHint: "Completează singur cele trei rubrici de mai jos. Rămân editabile.",
    driverPickNoneCarrier:
      "Transportatorul ăsta n-are șoferi configurați. Se adaugă în fișa lui, din Parteneri.",
    driverPickNoneOwn:
      "N-ai șoferi proprii configurați. Se adaugă în Setări, sub punctele de lucru.",
    driverName: "Delegat (șofer)",
    driverIdentification: "Act de identitate",
    // Seria actului pe Anexa 3; CNP-ul are rubrica lui din 15.09.2026, pentru avizul de însoțire.
    driverIdentificationPlaceholder: "serie și nr. CI",
    vehicleRegistration: "Nr. înmatriculare",
    transportDestinations: "Destinat:",
    destinationsPrefilled:
      "Bifat automat după ce este destinatarul. Schimbă dacă transportul ăsta e altfel.",
    transportDestinationsHint: "Se pot bifa mai multe, ca pe formular.",
    // validation / feedback
    weighingNeedsPartner: "Cântărirea la descărcare o face destinatarul: alege partenerul care preia deșeul.",
    recoveryCodeRequired: "Alege un cod de valorificare (R) pentru valorificare.",
    disposalCodeRequired: "Alege un cod de eliminare (D) pentru eliminare.",
    legacyExitHint:
      "Linie veche, fără cod R/D: cantitatea a ieșit de pe amplasament, dar nu intră în nicio coloană oficială. Alege valorificare sau eliminare și codul operațiunii ca să o completezi.",
    created: "Mișcare adăugată.",
    updated: "Mișcare actualizată.",
    deleted: "Mișcare ștearsă.",
    saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
    confirmDeleteTitle: "Ștergi mișcarea?",
    // Corpul dialogului poartă identitatea rândului; aici rămâne doar urmarea.
    confirmDelete:
      "Cantitatea dispare din evidența lunară și din documentele care se tipăresc din ea. Ștergerea nu poate fi anulată.",
    attachmentError: "Fișierul nu a putut fi încărcat.",
    // Mișcarea s-a salvat, atașamentele nu — două fapte diferite, care înainte se spuneau amândouă
    // ca „Salvarea a eșuat". Cine citea asta apăsa din nou și înregistra cantitatea a doua oară.
    attachmentsFailedSaved:
      // Verbul stă în perechea dată lui `countOf` („1 fișier n-a urcat" · „2 fișiere n-au urcat"),
      // fiindcă e singurul fel în care se acordă și el. Restul propoziției nu mai numără nimic.
      "Mișcarea s-a salvat, dar {count}. Ce a rămas e tot în listă — apasă Salvează încă o dată doar pentru asta.",
    attachmentDeleted: "Atașament șters.",
    // Deschiderea trece de la 11-bis prin backend, deci poate eșua din alt motiv decât urcarea:
    // sesiune expirată, fișierul șters de la furnizor, rețea. Mesaj separat, ca omul să nu creadă
    // că a pierdut atașamentul.
    attachmentOpenError: "Fișierul nu a putut fi deschis.",
    noWorkPointHint: "Adaugă întâi un punct de lucru din Setări.",
    workPointPlaceholder: "Alege punctul de lucru...",
    // Punctul unei mișcări vechi, dezactivat între timp: rămâne în listă, dar se vede că nu mai e
    // în uz. Alternativa — să dispară — ar face mișcarea nesalvabilă fără să spună de ce.
    workPointInactiveSuffix: "(dezactivat)",
  },

  settings: {
    title: "Setări",
    // Cuprinsul lipicios din capul paginii. Patru secțiuni una sub alta, dintre care trei tabele,
    // fac cea mai lungă pagină de configurare din aplicație.
    sections: "Pe pagină",
    // D1.8. Opțiunile numesc și suportul WasteHouse: la „toată firma” îl include, iar omul trebuie
    // să știe asta când alege (proprietarul, 15.09.2026: platforma e tratată ca un consultant).
    prices: {
      title: "Prețuri",
      subtitle:
        "Prețurile de la cântar, pe intrări și pe ieșiri, fără TVA. Alegi cine le vede. Cine nu le vede nu le poate nici schimba.",
      label: "Cine vede prețurile",
      options: {
        COMPANY: "Toată firma, inclusiv consultantul și suportul WasteHouse",
        NO_CONSULTANT: "Doar utilizatorii firmei, fără consultant și fără suportul WasteHouse",
        ADMIN_ONLY: "Doar administratorul firmei",
      },
      // Aceleași trei variante, pe carduri: numele scurt sus, cine rămâne afară dedesubt.
      choice: {
        COMPANY: { label: "Toată firma", description: "Inclusiv consultantul și suportul WasteHouse." },
        NO_CONSULTANT: {
          label: "Doar oamenii firmei",
          description: "Fără consultant și fără suportul WasteHouse.",
        },
        ADMIN_ONLY: {
          label: "Doar administratorul",
          description: "Nici operatorul, nici consultantul, nici suportul WasteHouse.",
        },
      },
      youSee: "Tu vezi prețurile.",
      youDontSee: "Tu nu vezi prețurile.",
      onlyAdmin: "Setarea o schimbă administratorul firmei.",
      saved: "Am salvat cine vede prețurile.",
      saveError: "Salvarea a eșuat. Încearcă din nou.",
    },
    company: {
      title: "Datele firmei",
      subtitle:
        "Ce tipăresc documentele oficiale în capul lor: identificarea, autorizația, persoana desemnată.",
      // Un titlu propriu: aici, sub autorizație, se citește și profilul (coduri R/D, coduri de
      // deșeu, transport), care în formularul de editare stă în blocul lui.
      groupAuthorization: "Autorizația de mediu și profilul",
      // Rubrica există pe firmă (o tipărește Anexa 3, lângă CUI), dar nu se editează din „Clienți",
      // deci eticheta ei nu e nici acolo. Cea din „Parteneri" e a partenerului, nu a firmei.
      tradeRegisterNumber: "Nr. Registrul Comerțului",
      unset: "Necompletat",
      // Două rubrici își iau aici alt nume decât în formularul de editare. Acolo sunt etichete de
      // bifă și de select, scrise ca să se citească împreună cu ce urmează („Datorează ceva la
      // AFM, dar…"); aici stau singure, deasupra unui răspuns, și trebuie să se ție pe picioarele
      // lor.
      afm: "Obligație la Fondul pentru mediu",
      wasteManagerExternal: "Angajat propriu sau terț",
      until: "până la",
      expired: "Expirată",
      wasteCodesCount: "{count} din autorizație",
      editInClients: "Editează în Clienți",
      // Nu trimite la o adresă de e-mail: aplicația n-are nicăieri una, iar o adresă inventată aici
      // ar fi prima care se dovedește falsă. Trimite la om — cel care a deschis contul.
      readOnlyNote:
        "Rubricile astea se citesc aici, dar se schimbă din contul care ți-a deschis firma. Pentru o corectură — autorizație reînnoită, altă persoană desemnată — spune-i consultantului tău.",
      loadError: "Nu am putut încărca datele firmei.",
    },
    workPoints: {
      title: "Puncte de lucru",
      subtitle: "Locațiile pentru care ții evidența deșeurilor.",
      add: "Adaugă punct de lucru",
      edit: "Editează punctul de lucru",
      name: "Denumire",
      address: "Adresă",
      empty: "Niciun punct de lucru încă.",
      searchPlaceholder: "Caută după nume sau adresă...",
      emptyHint: "Punctul de lucru e locul de unde pleacă deșeul. Fără cel puțin unul, nu se poate înregistra nicio mișcare.",
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivateTitle: "Dezactivezi punctul de lucru?",
      confirmDeactivate:
        "Nu mai apare la înregistrarea mișcărilor. Cele deja înregistrate rămân neatinse, dar dezactivarea nu poate fi anulată.",
      created: "Punct de lucru adăugat.",
      updated: "Punct de lucru actualizat.",
      deactivated: "Punct de lucru dezactivat.",
      saveError: "Salvarea a eșuat. Încearcă din nou.",
      loadError: "Nu am putut încărca punctele de lucru.",
    },
    articles: {
      title: "Sortimente",
      subtitle:
        "Denumirile cu care lucrezi la cântar („Cupru”, „Carton balotat”), fiecare legată de codul ei de deșeu. Pe registre se tipărește codul, iar pe ecran și pe borderou, sortimentul.",
      add: "Adaugă sortiment",
      addTitle: "Adaugă sortiment",
      editTitle: "Editează sortimentul",
      name: "Denumire",
      namePlaceholder: "ex. Cupru",
      code: "Cod de deșeu",
      codePlaceholder: "Caută după cod sau denumire...",
      metal: "Metal feros sau neferos",
      // OUG 31/2011 art. 1 alin. (1^2); Codul fiscal art. 114 alin. (2) lit. m²), art. 115 alin. (1).
      metalHint:
        "La o persoană fizică, borderoul cere CNP-ul și actul de identitate, iar din plată se reține impozitul de 10%. Bifa se propune din cod; o poți schimba.",
      forbidden: "Nu se cumpără de la persoane fizice",
      // OUG 31/2011 art. 1 alin. (1): șine, capace de canal, cabluri de semnalizare și altele asemenea.
      forbiddenHint:
        "Pentru ce interzice OUG 31/2011 art. 1 alin. (1): șine și piese de cale ferată, capace de cămin, cabluri de semnalizare și telecomunicații, componente de rețele de utilități. Aplicația refuză sortimentul la o intrare de la persoană fizică.",
      badgeMetal: "Metal",
      badgeForbidden: "Interzis PF",
      empty: "Niciun sortiment încă.",
      emptyHint: "La cântar alegi sortimentul, nu codul. Adaugă-le pe cele cu care lucrezi.",
      searchPlaceholder: "Caută după denumire sau cod...",
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivateTitle: "Dezactivezi sortimentul?",
      confirmDeactivate:
        "Nu mai apare la cântar. Operațiunile deja înregistrate îl păstrează. Îl poți reactiva oricând.",
      created: "Sortiment adăugat.",
      updated: "Sortiment actualizat.",
      deactivated: "Sortiment dezactivat.",
      saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
      loadError: "Nu am putut încărca sortimentele.",
    },
    /** D2.1 — flota depozitului. */
    vehicles: {
      title: "Flota",
      subtitle:
        "Mașinile cu care încarci și descarci. La cântar le alegi din listă și numărul se trece singur pe operațiune și pe documente. Primești un mail cu 30 de zile înainte să expire ITP-ul sau licența.",
      add: "Adaugă vehicul",
      addTitle: "Adaugă vehicul",
      editTitle: "Editează vehiculul",
      registration: "Nr. înmatriculare",
      registrationPlaceholder: "ex. CJ 12 ABC",
      registrationHint: "Se salvează cu majuscule și fără spații, ca același camion să nu apară de două ori.",
      kind: "Tip",
      kindPlaceholder: "ex. Camion cu macara",
      standardTare: "Tara standard (kg)",
      standardTareHint: "Cât cântărește goală. La cântar o vezi alături, ca să prinzi o tară greșită.",
      heavy: "Masa maximă autorizată peste 3,5 t",
      heavyHint: "Peste prag, transportul de mărfuri cere licență de transport.",
      itpExpiry: "ITP valabil până la",
      licenseNumber: "Licența de transport",
      licenseExpiry: "Licență valabilă până la",
      homeWorkPoint: "Depozitul de bază",
      noHomeWorkPoint: "— niciunul —",
      carrier: "Al cui e",
      ownFleet: "Al firmei",
      carrierHint: "Doar partenerii bifați ca transportator.",
      documents: "ITP / licență",
      expired: "Expirat",
      expiresSoon: "Expiră",
      noDocuments: "—",
      empty: "Niciun vehicul încă.",
      emptyHint: "Vehiculele salvate aici se aleg la cântar.",
      searchPlaceholder: "Caută după număr, tip sau depozit...",
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivateTitle: "Dezactivezi vehiculul?",
      confirmDeactivate:
        "Nu mai apare în lista de la cântar. Operațiunile deja înregistrate își păstrează numărul. Îl poți reactiva oricând.",
      delete: "Șterge definitiv",
      confirmDeleteTitle: "Ștergi definitiv vehiculul?",
      confirmDelete:
        "Fișa lui dispare. Operațiunile și documentele deja făcute își păstrează numărul de înmatriculare. Ștergerea nu se poate anula.",
      deleted: "Vehiculul a fost șters.",
      loadError: "Nu am putut încărca flota.",
      created: "Vehicul adăugat.",
      updated: "Vehicul actualizat.",
      deactivated: "Vehicul dezactivat.",
      saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
    },
    drivers: {
      title: "Șoferii noștri",
      subtitle:
        "Delegații firmei, pentru transporturile pe care le faci singur. La înregistrarea mișcării îi alegi din listă și rubricile de pe Anexa 3 se completează singure. Șoferii unui transportator se adaugă în fișa lui, din Parteneri.",
      add: "Adaugă șofer",
      addTitle: "Adaugă șofer",
      editTitle: "Editează șoferul",
      name: "Nume",
      namePlaceholder: "ex. Ion Popescu",
      identification: "Act de identitate",
      identificationPlaceholder: "ex. CJ 123456",
      // Seria actului rămâne în rubrica ei, tipărită pe Anexa 3; CNP-ul are rubrica lui de pe
      // 15.09.2026, fiindcă avizul de însoțire îl tipărește.
      identificationHint:
        "Ce se scrie pe Anexa 3 la „Date de identificare delegat”: seria și numărul actului de identitate. CNP-ul are rubrica lui, mai jos. Rămâne editabil pe fiecare mișcare.",
      vehicle: "Nr. înmatriculare uzual",
      vehiclePlaceholder: "ex. CJ 01 ABC",
      vehicleHint: "Mașina cu care vine de obicei. Pe mișcare se poate schimba.",
      // D2.2 — depozitul implicit și atestatul.
      homeWorkPoint: "Depozitul implicit",
      noHomeWorkPoint: "— niciunul —",
      attestation: "Atestat",
      attestationNumber: "Atestat (CPC / ADR)",
      attestationNumberPlaceholder: "ex. seria și numărul",
      attestationExpiry: "Atestat valabil până la",
      attestationHint: "Cu 30 de zile înainte de expirare primești un e-mail.",
      noAttestation: "—",
      attestationExpired: "Expirat",
      attestationExpiresSoon: "Expiră curând",
      empty: "Niciun șofer încă.",
      searchPlaceholder: "Caută după nume, act sau număr auto...",
      emptyHint: "Șoferii salvați aici precompletează rubrica de delegat de pe Anexa 3.",
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivateTitle: "Dezactivezi șoferul?",
      confirmDeactivate:
        "Nu mai apare în lista de delegați. Mișcările deja înregistrate nu se schimbă — Anexa 3 tipărește instantaneul de atunci. Îl poți reactiva oricând, sau îi poți șterge fișa după dezactivare.",
      // AO, 14.09.2026: specialista a lăsat decizia la noi, „cu atenție la GDPR".
      delete: "Șterge definitiv",
      confirmDeleteTitle: "Ștergi definitiv fișa șoferului?",
      confirmDelete:
        "Fișa lui dispare din aplicație. Mișcările deja înregistrate își păstrează numele și actul tipărite pe Anexa 3 până la termenul de păstrare a evidenței (cel puțin 3 ani, OUG 92/2021, art. 48 alin. (5)); după el, numele și actul de identitate se șterg singure și de pe ele. Ștergerea nu se poate anula.",
      deleted: "Fișa șoferului a fost ștearsă.",
      loadError: "Nu am putut încărca șoferii.",
      created: "Șofer adăugat.",
      updated: "Șofer actualizat.",
      deactivated: "Șofer dezactivat.",
      saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
    },
    /**
     * P1.11 — jurnalul de audit.
     *
     * <p>Subtitlul spune ce **nu** e, fiindcă ăsta e felul în care oamenii înțeleg greșit un jurnal:
     * nu e un log al bazei de date, e lista faptelor care ating ce se depune la autoritate sau ce
     * privește o persoană.
     */
    audit: {
      title: "Jurnal de audit",
      subtitle:
        "Cine a modificat ce și când — peste datele care ajung pe documentele depuse și peste cele care privesc o persoană. Se scrie singur, nu se poate șterge și nu se poate rescrie. Recalcularea evidenței apare ca o singură faptă, nu ca mii de rânduri.",
      colWhen: "Când",
      colWho: "Cine",
      colWhat: "Ce",
      colAction: "Faptă",
      colChanges: "Modificări",
      allTypes: "Toate tipurile",
      filterType: "Tipul înregistrării",
      searchPlaceholder: "Caută după cine, ce sau ce s-a schimbat...",
      empty: "Nicio faptă înregistrată încă.",
      emptyHint:
        "Jurnalul pornește de la prima modificare făcută după ce a fost pus în funcțiune. Ce s-a întâmplat înainte nu poate fi reconstituit.",
      loadError: "Nu am putut încărca jurnalul.",
      noChanges: "—",
      /** Un rând al cărui autor nu mai are cont. Adresa rămâne scrisă; contul, nu. */
      deletedActor: "cont șters",
      /** Valoarea goală, în lista de modificări: „(gol) → 7,5" se citește mai bine decât „ → 7,5". */
      emptyValue: "(gol)",
      changeArrow: "→",
      /** Câte fapte se văd sub un rând înainte să se strângă. */
      moreChanges: "+{count} încă",
      /** Jurnalul deschis din „Istoric” de pe un rând de mișcare: numai faptele acelui rând. */
      oneRowTitle: "Istoricul unei singure mișcări",
      oneRowHint: "Se văd doar faptele rândului din care ai venit.",
      showAll: "Arată tot jurnalul",
      retention:
        "Jurnalul se păstrează cât evidența pe care o descrie — cel puțin 3 ani (OUG 92/2021, art. 48 alin. (5)).",
    },
    internalGenerators: {
      title: "Generatori interni",
      subtitle:
        "Sursele din interiorul punctelor de lucru — birouri, producție, cantină. Se tipăresc în coloana „Secția” din cap. 2 al Anexei 1.",
      add: "Adaugă generator intern",
      addTitle: "Adaugă generator intern",
      editTitle: "Editează generatorul intern",
      workPoint: "Punct de lucru",
      workPointLocked: "Punctul de lucru nu se poate schimba. Dezactivează-l aici și adaugă-l acolo.",
      name: "Denumire (Secția)",
      namePlaceholder: "ex. birouri, producție, cantină",
      description: "Descriere",
      empty: "Niciun generator intern încă.",
      searchPlaceholder: "Caută după nume sau punct de lucru...",
      emptyHint: "Secțiile — birouri, producție — se tipăresc în capitolul 2 al fișei de evidență.",
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivateTitle: "Dezactivezi generatorul intern?",
      confirmDeactivate:
        "Nu mai apare ca secție la înregistrarea mișcărilor. Cele deja înregistrate rămân neatinse, dar dezactivarea nu poate fi anulată.",
      created: "Generator intern adăugat.",
      updated: "Generator intern actualizat.",
      deactivated: "Generator intern dezactivat.",
      saveError: "Salvarea a eșuat. Încearcă din nou.",
      loadError: "Nu am putut încărca generatorii interni.",
      noWorkPoints: "Adaugă întâi un punct de lucru.",
    },
    users: {
      title: "Utilizatorii firmei",
      subtitle:
        "Cine are acces în contul firmei și ce poate face. Invitatul primește pe email un link cu care își pune singur parola — nimeni de aici nu-i vede și nu-i alege parola.",
      invite: "Invită utilizator",
      inviteTitle: "Invită un utilizator",
      inviteHint:
        "Primește un email cu un link valabil 30 de minute, din care își alege parola. Până atunci contul apare „În așteptare” și nu se poate autentifica.",
      email: "Email",
      emailPlaceholder: "ex. maria@firma.ro",
      firstName: "Prenume",
      lastName: "Nume",
      role: "Rol",
      name: "Nume",
      status: "Stare",
      you: "tu",
      statusActive: "Activ",
      statusPending: "În așteptare",
      statusDeactivated: "Dezactivat",
      // Rândul de sub tabel: ce înseamnă fiecare rol, o dată, în loc de trei tooltipuri.
      roleLegend:
        "Administrator: tot, inclusiv utilizatorii și datele firmei. Operator: înregistrează mișcări și tipărește documente. Vizualizare: doar citește.",
      resend: "Retrimite invitația",
      // Etichetele de pe rând sunt scurte fiindcă stau amândouă în coloana lipită la dreapta, iar
      // pe telefon perechea lungă măsura 352px într-un container de 341 — adică acoperea complet
      // emailul, numele, rolul și starea. Textul întreg rămâne în `aria-label` și în confirmare.
      resendShort: "Retrimite",
      cancelInviteShort: "Anulează",
      resent: "Invitația a plecat din nou.",
      resendError: "Nu am putut retrimite invitația.",
      changeRole: "Schimbă rolul",
      changeRoleTitle: "Schimbă rolul",
      roleChanged: "Rol actualizat.",
      deactivate: "Dezactivează",
      confirmDeactivateTitle: "Dezactivezi contul?",
      // Spune exact cele trei lucruri pe care le întreabă cineva înainte să apese: ce se
      // întâmplă acum, ce se întâmplă cu ce a scris omul, și dacă se poate lua înapoi.
      confirmDeactivate:
        "Sesiunile deschise se închid imediat și nu se mai poate autentifica. Mișcările și documentele pe care le-a înregistrat rămân neatinse, cu numele lui. Se poate reactiva oricând.",
      cancelInvite: "Anulează invitația",
      confirmCancelTitle: "Anulezi invitația?",
      // Spune și că se poate relua, fiindcă asta e frica reală: „am greșit adresa, acum ce fac".
      confirmCancel:
        "Linkul trimis se stinge și contul dispare de tot — n-a fost folosit, deci nu se pierde nimic din ce e înregistrat. Adresa rămâne liberă, deci poți invita din nou.",
      cancelled: "Invitație anulată.",
      deactivated: "Cont dezactivat.",
      reactivated: "Cont reactivat.",
      invited: "Invitația a plecat.",
      inviteError: "Nu am putut trimite invitația.",
      empty: "Niciun utilizator încă.",
      emptyHint: "Invită-ți colegii ca să înregistreze mișcări fără să folosiți același cont.",
      searchPlaceholder: "Caută după nume, email sau rol...",
      loadError: "Nu am putut încărca utilizatorii.",
      saveError: "Operațiunea a eșuat. Încearcă din nou.",
    },
  },

  // D1.7b — tabul de lângă Parteneri, doar la firmele cu „Intrări și ieșiri” (art. 48).
  /** Ecranul „Cântar” — operațiunile depozitului (D1.15), peste D1.4–D1.10. */
  weighing: {
    title: "Cântar",
    subtitle:
      "Ce a intrat și ce a ieșit din depozit, cu cântarul pe fiecare sortiment. Cât e „în lucru”, o operațiune nu contează nicăieri; o finalizezi când bonul e gata, și abia atunci intră în stoc și în registre.",
    tabIn: "Intrări",
    tabOut: "Ieșiri",
    newIn: "Intrare nouă",
    newOut: "Ieșire nouă",
    exportRegister: "Registrul lunii",
    exportRegisterError: "Registrul nu a putut fi descărcat. Încearcă din nou.",
    number: "Nr.",
    date: "Data",
    workPoint: "Depozit",
    counterpartyIn: "De la",
    counterpartyOut: "Către",
    articles: "Sortimente",
    quantity: "Cantitate (kg)",
    value: "Valoare",
    status: "Stare",
    searchPlaceholder: "Caută după număr, firmă sau persoană...",
    empty: "Nicio operațiune în luna asta.",
    emptyHint: "Prima cântărire o pornești cu butonul de sus.",
    loadError: "Nu am putut încărca operațiunile.",
    noDepot: "Firma nu ține registrul de intrări și ieșiri.",
    noDepotHint:
      "Cântarul e al firmelor care preiau deșeu de la alții (art. 48). Dacă firma face și asta, schimbă tipul ei în Setări → Datele firmei.",
    statusInProgress: "În lucru",
    statusFinalized: "Finalizată",
    statusCancelled: "Anulată",
    // --- formularul ---
    sectionWho: "Cine și când",
    sectionTransport: "Transport",
    sectionScale: "Cântarul",
    sectionPayment: "Plata",
    fromCompany: "Firmă",
    fromPerson: "Persoană fizică",
    partner: "Firma",
    partnerPlaceholder: "Caută firma după nume sau CUI...",
    person: "Persoana",
    personPlaceholder: "Caută persoana după nume...",
    personMissing: "Persoana nu e în listă? Adaug-o la Parteneri → Persoane fizice.",
    driver: "Șofer",
    vehicle: "Mașina",
    /** D2.1 — numărul potrivit cu un vehicul din flotă. */
    vehicleFromFleet: "Din flotă",
    vehicleStandardTare: "tara standard",
    vehicleDocumentsExpired: "ITP-ul sau licența au expirat",
    // D2.2 — șoferul ales din listă; un nume care nu e în listă rămâne text (șoferul ocazional).
    driverFromList: "Din lista de șoferi",
    driverOccasional: "Șofer ocazional, fără fișă",
    driverAttestationExpired: "Atestatul șoferului a expirat",
    orderNumber: "Nr. comandă / aviz",
    notes: "Observații",
    truckGross: "Brut mașină (kg)",
    truckTare: "Tara mașină (kg)",
    truckHint: "Mașina plină și mașina goală, dacă s-a cântărit așa. Liniile de mai jos au cântăririle lor.",
    lineArticle: "Sortiment",
    lineGross: "Brut",
    lineTare: "Tara",
    lineNet: "Neto",
    lineFinal: "Acceptat",
    linePrice: "Lei/kg",
    lineTotal: "Valoare",
    lineCode: "Cod R/D",
    lineAdd: "Adaugă sortiment",
    lineRemove: "Șterge linia",
    lineTareHint: "Tara unei linii e brutul liniei dinainte: se cântărește o dată, la fiecare sortiment descărcat.",
    linesRequired: "Adaugă cel puțin un sortiment.",
    totalKg: "Total acceptat",
    totalValue: "Total valoare",
    paymentMethod: "Cum se plătește",
    paymentVirement: "Virament",
    paymentNumerar: "Numerar",
    receiptNumber: "Nr. chitanță",
    cashHint:
      "Numerarul către o persoană fizică are plafon zilnic (Legea 70/2015). Viramentul se face în cel mult 3 zile lucrătoare de la borderou.",
    ownHousehold: "Declar că deșeurile provin din gospodăria proprie",
    ownHouseholdHint:
      "Obligatorie la metal de la persoane fizice: OUG 31/2011 art. 1 alin. (1^1) interzice cumpărarea metalului care nu provine din gospodăria proprie.",
    // Cota vine de la server și se lipește de etichetă: „Fondul pentru mediu · 2%”.
    withheldAfm: "Fondul pentru mediu",
    withheldTax: "Impozit pe venit",
    withheldNet: "Rămâne de plătit",
    withheldHint:
      "Cifrele se recalculează la finalizare și rămân scrise pe operațiune. Cei 2% se rețin la orice intrare cu preț; cei 10%, doar la metalul cumpărat de la o persoană fizică.",
    save: "Salvează",
    saved: "Operațiune salvată.",
    saveError: "Salvarea a eșuat. Verifică rubricile marcate.",
    finalize: "Finalizează",
    finalized: "Operațiune finalizată. Liniile intră acum în registre.",
    finalizeHint: "O finalizează administratorul firmei sau consultantul.",
    confirmFinalizeTitle: "Finalizezi operațiunea?",
    confirmFinalize:
      "După finalizare nu se mai modifică: liniile intră în stoc, în registrul art. 48 și în evidențe. Dacă e greșită, o poți anula, cu motiv.",
    cancelOperation: "Anulează operațiunea",
    printAnexa3: "Anexa 3",
    printAviz: "Aviz",
    printBorderou: "Borderou",
    cashAboveLimit:
      "În ziua operațiunii, acestei persoane i s-au plătit în numerar peste 10.000 lei. Legea 70/2015 art. 4 nu permite mai mult pe zi către aceeași persoană: diferența se plătește prin virament.",
    cashPaidToday: "Numerar în ziua respectivă:",
    documentError: "Documentul nu a putut fi generat.",
    cancelled: "Operațiune anulată.",
    cancelReason: "Motivul anulării",
    cancelReasonPlaceholder: "ex. cântărire dublă",
    confirmCancelTitle: "Anulezi operațiunea?",
    confirmCancelBody:
      "Nu se șterge nimic: rămâne în listă, cu motivul, cine și când. Liniile ies din stoc și din registre.",
    cancelledBecause: "Anulată:",
    open: "Deschide",
    retentions: {
      title: "Reținut la sursă în luna asta",
      afm: "Fondul pentru mediu",
      tax: "Impozit pe venit",
      base: "din",
      due: "De declarat și plătit până pe",
      empty: "Nimic de reținut încă în luna asta.",
      hint:
        "Cifrele sunt cele reținute la finalizare, pe cote fixe prin lege: 2% la Fondul pentru mediu din orice intrare cu preț (OUG 196/2005) și 10% impozit pe metalul cumpărat de la persoane fizice (Codul fiscal art. 115). Aplicația le calculează; declarațiile le depune contabilul.",
    },
  },
  naturalPersons: {
    tabFirms: "Firme",
    tab: "Persoane fizice",
    subtitle:
      "Oamenii care vând depozitului. La hârtie sau plastic ajunge numele; la metal borderoul cere și CNP-ul, actul de identitate și domiciliul (OUG 31/2011).",
    add: "Adaugă persoană",
    addTitle: "Adaugă persoană fizică",
    editTitle: "Editează persoana fizică",
    name: "Nume și prenume",
    namePlaceholder: "ex. Ion Popescu",
    cnp: "CNP",
    cnpHint: "Obligatoriu doar la metal. În listă se văd numai ultimele 4 cifre.",
    cnpInvalid: "Verifică cifrele: un CNP are 13 cifre, iar ultima trebuie să se potrivească cu celelalte. Probabil una e greșit tastată.",
    identification: "Seria și numărul actului de identitate",
    identificationPlaceholder: "ex. CJ 123456",
    address: "Domiciliu",
    addressPlaceholder: "ex. Cluj-Napoca, str. Horea 1",
    metalColumn: "Metal",
    metalReady: "Poate vinde metal",
    nameOnly: "Lipsesc datele pentru metal",
    searchPlaceholder: "Caută după nume sau ultimele cifre din CNP...",
    empty: "Nicio persoană fizică încă.",
    emptyHint: "Adaugă oamenii care îți aduc deșeuri. La cântar îi alegi din listă.",
    active: "Activă",
    inactive: "Inactivă",
    deactivate: "Dezactivează",
    confirmDeactivateTitle: "Dezactivezi persoana?",
    confirmDeactivate:
      "Nu mai apare la cântar. Operațiunile deja înregistrate o păstrează. O poți reactiva oricând.",
    delete: "Șterge definitiv",
    confirmDeleteTitle: "Ștergi definitiv fișa persoanei?",
    confirmDelete: "Persoana n-a vândut nimic, deci fișa se poate șterge. Nu se mai poate recupera.",
    hasOperationsLabel: "De ce nu se poate șterge",
    hasOperationsHint:
      "Apare pe operațiuni: fișa nu se șterge, fiindcă borderoul se păstrează 10 ani. Datele pleacă singure la termen.",
    created: "Persoană adăugată.",
    updated: "Persoană actualizată.",
    deactivated: "Persoană dezactivată.",
    deleted: "Fișa persoanei a fost ștearsă.",
    saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
    loadError: "Nu am putut încărca persoanele fizice.",
    loadOneError: "Nu am putut deschide fișa persoanei.",
  },
  partners: {
    title: "Parteneri",
    subtitle:
      "Firmele cu care lucrezi: clienții (le predai deșeu și le facturezi) și furnizorii (îți prestează serviciul și îți facturează).",
    add: "Adaugă partener",
    addTitle: "Adaugă partener",
    editTitle: "Editează partenerul",
    empty: "Niciun partener încă.",
    searchPlaceholder: "Caută după nume, CUI, autorizație, adresă...",
    emptyHint: "Partenerii sunt firmele către care predai sau de la care preiei deșeu, plus transportatorii.",
    loadError: "Nu am putut încărca partenerii.",
    // columns
    name: "Denumire",
    cui: "CUI",
    type: "Tip",
    authorizationNumber: "Nr. autorizație",
    authorizationExpiry: "Expirare autorizație",
    authorizationValidUntil: "Valabilă până la",
    // V41 — viza anuală. Răspunsul specialistei la AH (14.09.2026): „am viza din locul datei".
    authorizationIssueDate: "Data emiterii autorizației inițiale",
    authorizationIssueDateHint:
      "Ziua și luna ei deschid anul de viză; la o autorizație revizuită contează tot cea inițială (Ordinul 1150/2020, art. 5 alin. (4)).",
    visaDecisionNumber: "Decizia de viză anuală nr.",
    visaDecisionDate: "Data deciziei",
    visaValidUntil: "Viza aplicată până la",
    visaValidUntilHint:
      "Cum scrie pe decizie: „se aplică viza pentru perioada …”. Perioada o dă agenția, deci se trece de pe hârtie.",
    visaValidUntilSuggest: "Propune {date}",
    authorizationExpiryOptional: "Data expirării (numai la autorizațiile cu termen)",
    authorizationExpiryOptionalHint:
      "Autorizațiile de azi nu mai expiră: rămân valabile cât se obține viza anuală (OUG 195/2005, art. 16 alin. (2^1)). Termen mai au doar cele de dinainte de 18.11.2019, nemodificate (Legea 219/2019, art. II).",
    // Reperele formularului lung, ca la mișcare și la firmă. Nu se pliază: jumătate din secțiuni
    // conțin rubrici obligatorii, iar un câmp obligatoriu ascuns sub un titlu închis e un formular
    // care se refuză fără să spună de ce.
    sectionIdentity: "Identificare",
    sectionRole: "Ce face partenerul",
    sectionCarrier: "Transport",
    sectionAuthorization: "Autorizația de mediu",
    sectionAuthorizationHint:
      "Predarea se face către un operator autorizat (OUG 92/2021, art. 23 alin. (1)), iar autorizația rămâne valabilă cât se obține viza anuală. Cu perioada vizei sau data expirării completată, aplicația anunță cu 60 de zile înainte și marchează predările făcute după. Dacă le ai pe amândouă, contează cea care vine prima.",
    sectionAnexa3: "Date pentru Anexa 3",
    // Sugestia de duplicat ducea la fişa existentă pe tăcute: acelaşi dialog devenea „Editează",
    // iar tot ce completasei dispărea. Comutarea rămâne fapta bună — sugestia există tocmai ca să
    // nu se creeze un partener de două ori —, dar se **spune** că s-a întâmplat, cu drum înapoi.
    // Linkul din badge-ul „Autorizație expirată" de pe o predare. Partenerul poate fi dezactivat
    // între timp, sau linkul poate fi vechi — se spune, nu se deschide un formular gol.
    partnerNotFound: "Partenerul nu mai e în listă. Poate a fost dezactivat între timp.",
    suggestionSwitchTitle: "Deschizi fișa partenerului existent?",
    suggestionSwitchConfirm:
      "Ce ai completat aici nu se salvează — deschizi fișa lui, nu adaugi un partener nou.",
    suggestionSwitchGo: "Deschide fișa",
    suggestionSwitchedTitle: "Editezi un partener care există deja.",
    suggestionSwitchedHint:
      "Ai ajuns aici din sugestia de duplicat, de la „{name}”. Modificările se salvează pe fișa lui.",
    suggestionSwitchedBack: "Înapoi la adăugare",
    nameSuggestions: "Există deja la tine:",
    nameSuggestionsHint:
      "Ca să nu apară același partener de două ori, cu două grafii. Apasă unul ca să-l deschizi în loc să creezi altul.",
    role: "Rol comercial",
    filterRole: "Rol",
    filterRoleAll: "Toate rolurile",
    roleRequired: "Alege cel puțin un rol: client, furnizor sau ambele.",
    address: "Adresa sediului social",
    workPoints: "Puncte de lucru",
    workPointsHint:
      "Unde se descarcă efectiv deșeul, dacă e altundeva decât sediul. Adaugă câte are — la înregistrarea mișcării alegi la care a ajuns marfa, și adresa aia se tipărește pe Anexa 3.",
    workPointName: "Nume (opțional)",
    workPointNamePlaceholder: "ex. Depozit Florești",
    workPointAddress: "Adresa",
    addWorkPoint: "Adaugă punct de lucru",
    removeWorkPoint: "Șterge",
    tradeRegisterNumber: "Nr. Registrul Comerțului",
    tradeRegisterNumberPlaceholder: "ex. J12/1351/2011",
    transportLicenseNumber: "Licență de transport mărfuri",
    transportLicenseExpiry: "Expiră licența",
    heavyVehicles: "Transportă cu vehicule peste 3,5 tone",
    heavyVehiclesHint: "Licența de transport mărfuri se cere numai peste 3,5 tone.",
    anexa3Hint:
      "Datele astea se tipăresc pe Anexa 3 când îi predai deșeu sau când el face transportul.",
    // --- Transportator (V28) ---
    carrier: "Transportator",
    carrierHint:
      "Poate face transportul. Bifă, nu tip: același partener e des și colector, și transportator. La înregistrarea mișcării apare în grupa „Transportatori”.",
    carrierColumn: "Transport",
    carrierYes: "Transportator",
    carrierNo: "—",
    filterCarrier: "Transportatori",
    typeNone: "— doar transportator —",
    typeNoneShort: "Doar transport",
    // Fără „de mai sus" / „de mai jos": bifa s-a mutat o dată deja (era sub select, iar textul
    // spunea „mai sus"), și textul a rămas în urmă. Numește rubrica, nu direcția.
    typeNoneHint:
      "Pentru o firmă care doar transportă: nu face nimic cu deșeul, deci n-are tip. Se poate alege numai bifând „Transportator”.",
    typeRequired:
      "Alege ce face partenerul cu deșeul, sau bifează „Transportator” dacă e o firmă care doar transportă.",
    drivers: "Șoferi",
    driversHint:
      "Delegații care vin de obicei de la el. La înregistrarea mișcării îl alegi din listă și cele trei rubrici de pe Anexa 3 se completează singure — sau scrii altul de mână.",
    driverName: "Nume",
    driverNamePlaceholder: "ex. Ion Popescu",
    driverIdentification: "Act de identitate",
    driverIdentificationPlaceholder: "ex. CJ 123456",
    driverVehicle: "Nr. înmatriculare uzual",
    driverVehiclePlaceholder: "ex. CJ 01 ABC",
    addDriver: "Adaugă șofer",
    removeDriver: "Șterge",
    // form placeholders
    cuiPlaceholder: "ex. RO12345678",
    // Căutarea după CUI, la ANAF. Completează numai rubricile goale: ce a scris omul nu se rescrie.
    anafLookup: "Completează din ANAF",
    anafLookupHint:
      "Scrie CUI-ul și apasă butonul: datele firmei vin din registrul public ANAF. Ce ai completat deja rămâne cum e.",
    anafFilled: "Completat din ANAF: {fields}. Verifică înainte să salvezi.",
    anafNothingToFill: "ANAF o știe ca „{name}”. Rubricile erau deja completate, deci n-am schimbat nimic.",
    anafFieldName: "denumirea",
    anafFieldAddress: "adresa",
    anafFieldRegistry: "numărul de la Registrul Comerțului",
    anafFieldCaen: "codul CAEN",
    anafInactive: "Atenție: ANAF o arată ca contribuabil inactiv.",
    anafError: "Nu am putut întreba ANAF. Completează datele de mână.",
    anafCuiFirst: "Scrie întâi CUI-ul, apoi apasă butonul.",
    authorizationNumberPlaceholder: "ex. 123/2024",
    authorizationRequired:
      "Completează numărul autorizației de mediu: colectorul sau valorificatorul căruia îi predai deșeul trebuie să fie autorizat.",
    typePlaceholder: "Alege tipul",
    // status / badges
    active: "Activ",
    inactive: "Inactiv",
    noAuthorization: "—",
    expired: "Expirată",
    expiringSoon: "Expiră curând",
    deactivate: "Dezactivează",
    confirmDeactivateTitle: "Dezactivezi partenerul?",
    confirmDeactivate:
      "Nu mai apare la înregistrarea mișcărilor. Cele deja înregistrate rămân neatinse, dar dezactivarea nu poate fi anulată — nu există reactivare.",
    // feedback
    created: "Partener adăugat.",
    updated: "Partener actualizat.",
    deactivated: "Partener dezactivat.",
    saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
  },

  evidences: {
    title: "Evidența gestiunii deșeurilor",
    subtitle:
      "Situația lunară pe puncte de lucru și coduri de deșeu, cu stoc cumulativ. Regenerată din mișcări.",
    regenerate: "Regenerează",
    regenerating: "Se regenerează...",
    loadError: "Nu am putut încărca evidența.",
    regenerateError: "Regenerarea a eșuat. Încearcă din nou.",
    regenerated: "Evidență regenerată: {count} pentru {year}.",
    // Zero linii nu e o eroare — anul chiar n-are mişcări —, dar „Evidenţă regenerată: 0 de linii"
    // se citeşte ca una. Aceeaşi gardă pe care Termenele au primit-o pe 09.09 (`generatedNone`),
    // sărită aici: se vede pe orice firmă nouă, adică la primul contact al oricărui client.
    regeneratedNone: "Nimic de regenerat pentru {year}: nu există mișcări înregistrate în anul ăsta.",
    // export
    export: "Export",
    // Numele scurt "Anexa 1" a fost cedat declarației de ambalaje (Ordinul 794/2012) pe
    // 24.08.2026, la cererea specialistei: așa îi zice clientul. Documentul de aici își
    // poartă de-acum numele întreg, care e și titlul tipărit pe el.
    anexa1: "Evidența gestiunii deșeurilor generate",
    anexa1Hint:
      "Formularul oficial (HG 856/2002, anexa 1): antet + cele 4 capitole, o pagină per cod de deșeu.",
    anexa1Error: "Evidența gestiunii deșeurilor nu a putut fi generată.",
    // Fosta „Declarația anuală", redenumită la cererea specialistei pe 15.09.2026.
    annualDeclaration: "Evidența gestiunii deșeurilor centralizată",
    annualDeclarationHint:
      "Centralizatorul anului: un rând per cod de deșeu — stoc inițial, generat, valorificat, eliminat, stoc final și prin cine. O pagină per punct de lucru.",
    annualDeclarationError: "Evidența centralizată nu a putut fi generată.",

    // --- Cifra pentru depunere, în tone (OUG 92/2021 art. 48 alin. (1)) ---
    // Evidența rămâne în kg peste tot, inclusiv pe hârtie; asta e doar ajutorul de la încărcare.
    tonnesTitle: "Pentru depunerea din 15 martie — totalul anului {year}, în tone",
    tonnesHint:
      "Evidența se ține în kilograme, și e corect așa: fișa din HG 856/2002 lasă unitatea la alegere, iar formularele tipărite rămân în kg. Depunerea e altceva — OUG 92/2021, art. 48 alin. (1) cere cantitatea în tone. Aici o ai gata calculată pe an și pe cod, ca să n-o împarți de mână la 1000 în ziua depunerii.",
    colGeneratedTonnes: "Generat [t]",
    colRecoveredTonnes: "Valorificat [t]",
    colDisposedTonnes: "Eliminat [t]",
    // Cele două exporturi generice au stat până pe 08.09 în antet, la fel de vizibile ca cele două
    // documente oficiale — cinci butoane pe un rând, care strângeau titlul paginii pe trei rânduri.
    // Sunt lucruri de alt fel: unul se depune la agenție, celălalt scrie pe el „rezumat neoficial".
    // Deci ies din antet, într-un meniu care le spune pe față ce sunt.
    exportsMenu: "Alte descărcări",
    exportExcel: "Rezumat Excel",
    exportPdf: "Rezumat PDF",
    exportsHint: "Rezumat neoficial al evidenței — pentru lucru, nu pentru depunere.",
    exportError: "Exportul a eșuat. Încearcă din nou.",
    // filters
    filterYear: "An",
    filterMonth: "Luna",
    filterWorkPoint: "Punct de lucru",
    allMonths: "Toate lunile",
    allWorkPoints: "Toate",
    // columns — Anexa 1, cap. 1: "Generate | din care: valorificată | eliminată final |
    // rămasă în stoc". Fișa nu are coloană de predare, deci predarea apare în „Valorificat" sau
    // „Eliminat", după codul R/D al operațiunii făcute de destinatar.
    colWorkPoint: "Punct de lucru",
    colMonth: "Luna",
    colWasteCode: "Cod deșeu",
    colGenerated: "Generat",
    colRecovered: "Valorificat",
    colDisposed: "Eliminat",
    colHandedOver: "din care predat",
    colUnclassified: "Neclasificat",
    colStock: "Stoc",
    hazardous: "Periculos",
    // line warnings
    // Cele două vederi ale tabului. Implicit e registrul de predări, cerut la meeting; vederea
    // lunară rămâne fiindcă poartă stocul cumulativ, singura cifră pe care ochiul n-o reface și
    // exact ce cere evidența gestiunii deșeurilor.
    viewHandovers: "Predări",
    viewMonthly: "Evidența lunară",
    handoversSubtitle:
      "Ce a plecat de pe amplasament: cantitatea, data predării, cine a primit și sub ce cod. Din rândul de aici tipărești Anexa 3.",
    colHandoverDate: "Data predării",
    colOperationCode: "Operațiune",
    colPartnerName: "Partener",
    emptyHandovers: "Nicio predare pentru filtrele alese.",
    handoversSearchPlaceholder: "Caută după cod, partener, punct de lucru...",
    handoversLoadError: "Nu am putut încărca predările.",
    ownSite: "pe amplasament propriu",
    // Roșu, nu galben: o ieșire fără cod R/D nu e o rubrică de completat cândva, e o cantitate
    // care lipsește din evidență. Se cere la orice ieșire nouă, deci rândurile astea sunt vechi.
    missingCode: "Fără cod R/D",
    missingCodeHint:
      "Cantitate ieșită fără cod de operațiune (R/D). Se scade din stoc, dar nu poate fi raportată în „Valorificat” sau „Eliminat” până nu completezi codul pe mișcare.",
    // Badge-ul spunea ce e stricat; abia asta duce unde se repară.
    fixMissingCode: "Completează codul",
    // Filtrul care leagă panoul de rândurile vinovate. Stă în adresă, deci linkul se poate trimite.
    onlyMissingCode: "Doar ieșirile fără cod R/D",
    onlyMissingCodeOff: "Arată toate predările",
    onlyMissingCodeEmpty: "Nicio ieșire fără cod R/D pentru filtrele alese. Depunerea nu e blocată aici.",
    awaitingWeighing: "De cântărit",
    awaitingWeighingHint:
      "O ieșire din luna asta așteaptă cântarul destinatarului, deci totalurile sunt provizorii.",
    regeneratedCascade: "Evidență regenerată: {count} pentru {year} (și anii {years}).",
    // empty state
    empty: "Nu există linii de evidență pentru {year}.",
    searchPlaceholder: "Caută după cod, denumire sau punct de lucru...",
    emptyHint: "Apasă „Regenerează” pentru a calcula evidența anului {year} din mișcări.",
    // Evidența e un cache derivat din mișcări, iar citirea îl reconstruiește când a rămas în
    // urmă. Butonul rămâne pentru reconstrucția cerută explicit — după o migrare, sau când vrei
    // să vezi cifra recalculată sub ochii tăi.
    staleNote:
      "Evidența se recalculează din mișcări ori de câte ori s-a schimbat ceva. „Regenerează” o reconstruiește pe loc și rescrie și anii următori, fiindcă stocul se reportează.",
  },

  deadlines: {
    title: "Termene de raportare",
    subtitle:
      "Următorul termen al fiecărei raportări pe care o datorează firma. Când unul e bifat sau trece, apare următorul.",
    generate: "Verifică termenele",
    generating: "Se verifică...",
    // „Termene generate: 1 noi" — substantivul și adjectivul intră amândouă în `countOf`, iar
    // prefixul dispare ca să nu se spună „termene" de două ori. Zero are șir propriu: „0 de
    // termene noi" e corect gramatical și se citește ca o eroare.
    generated: "Calendarul: {count}.",
    generatedNone: "Termenele următoare existau deja.",
    generateError: "Generarea termenelor a eșuat. Încearcă din nou.",
    loadError: "Nu am putut încărca termenele.",
    // filters
    filterYear: "An",
    // columns
    colReportType: "Raportare",
    colDueDate: "Termen",
    colStatus: "Status",
    colNote: "Notă",
    // actions
    markDone: "Marchează finalizat",
    reopen: "Redeschide",
    completed: "Termen marcat ca finalizat.",
    reopened: "Termen redeschis.",
    actionError: "Acțiunea a eșuat. Încearcă din nou.",
    // complete dialog
    completeTitle: "Marchează termenul ca finalizat",
    noteLabel: "Notă (opțional)",
    notePlaceholder: "ex. depus la ANMAP pe 12.03, nr. înregistrare 1234",
    // empty state
    todoTitle: "De făcut",
    todoEmpty: "Niciun termen de făcut.",
    doneTitle: "Bifate",
    doneEmpty: "Niciun termen bifat în {year}.",
    searchPlaceholder: "Caută după tip de raportare sau notă...",
    emptyHint: "Termenele apar singure, câte unul pe fiecare raportare. Dacă lipsește unul, apasă „Verifică termenele”.",
    // Data singură cere o socoteală în cap — Panoul o făcea de mult, tabelul nu. „Azi" și „mâine"
    // se scriu în cuvinte: „în 0 zile" e adevărat și nu se citește ca nimic.
    daysLeft: "în {count}",
    daysToday: "azi",
    daysTomorrow: "mâine",
    daysOverdue: "depășit de {count}",
    // Drumul de la termen la documentul care îl stinge. Există doar unde chiar tipărim ceva:
    // contribuțiile AFM sunt bani declarați în aplicația AFM, nu un formular al nostru, iar un
    // link către un document inexistent ar promite mai mult decât ținem.
    documentEvidence: "Deschide evidența pe {year}",
    documentPackaging: "Deschide ambalajele pe {year}",
    // Starea documentului pe termenul lui: din ce e deja înregistrat, fără socoteli noi.
    readiness: {
      loading: "Se verifică documentul…",
      error: "Nu am putut verifica documentul.",
      yearOpen: "Anul {year} nu s-a încheiat — cifrele cresc până la 31 decembrie.",
      evidenceEmpty: "Nicio generare înregistrată în {year}.",
      evidenceFigures: "{year}: {codes}, {tonnes} t",
      missingCode: "{count} fără cod R/D",
      awaitingWeighing: "{count} de cântărit",
      unclassified: "{count} neîncadrate (material sau proveniență)",
      packagingEmpty: "Nicio mișcare cu ambalaje în {year}.",
      roleMissing: "Alege la Ambalaje ce ești: colector, comerciant, reciclator sau valorificator.",
      perWorkPoint: "Se depune pe fiecare punct de lucru — descarcă-le din Ambalaje.",
      ready: "Gata de depus",
      download: "Descarcă",
      downloadError: "Documentul nu a putut fi descărcat.",
    },
    colDocument: "Documentul",
  },

  auditFile: {
    title: "Dosar de control",
    subtitle:
      "Descarcă într-o singură arhivă tot ce ai nevoie la un control: evidența, autorizațiile partenerilor și documentele atașate.",
    filterYear: "An",
    // Perioada acoperită. Trei ani e termenul de păstrare din OUG 92/2021 art. 48 alin. (5) —
    // exact cât poate cere un control —, dar implicit rămâne un an: cel mai des se descarcă
    // pentru anul care se depune.
    filterYears: "Perioada",
    yearsOne: "Doar anul ales",
    yearsTwo: "Ultimii 2 ani",
    yearsThree: "Ultimii 3 ani (cât cere un control)",
    yearsFour: "Ultimii 4 ani",
    yearsFive: "Ultimii 5 ani (cu marjă peste termenul legal)",
    yearsHint:
      "Evidența se păstrează cel puțin 3 ani (OUG 92/2021, art. 48). La control se poate cere toată perioada, nu doar anul curent.",
    download: "Descarcă dosarul (.zip)",
    downloading: "Se pregătește arhiva...",
    downloadError: "Descărcarea dosarului a eșuat. Încearcă din nou.",
    /** Estimarea de dinainte de descărcare. {count} atașamente, {size} deja formatat. */
    sizeNone: "Fără atașamente în perioada aleasă — arhiva are doar foile generate.",
    sizeKnown: "{count} atașamente, cam {size} de descărcat.",
    sizeAllUnknown: "{count} atașamente, încărcate înainte să le ținem mărimea — nu știm cât cântăresc.",
    sizeUnknown: "{count} atașamente, cam {size} de descărcat, plus {unknown} încărcate înainte să le ținem mărimea.",
    contents: "Arhiva conține:",
    contentAnexa1:
      "Evidența gestiunii deșeurilor generate (HG 856/2002, anexa 1): cele 4 capitole, o pagină per cod de deșeu. Termen de depunere: 15 martie.",
    contentAnnualDeclaration:
      "Evidența gestiunii deșeurilor centralizată: un rând per cod de deșeu — stoc inițial, generat, valorificat, eliminat, stoc final și prin cine. O pagină per punct de lucru.",
    contentPackaging:
      "Anexa 1 Ambalaje (Ordinul 794/2012), .xls și PDF — numai dacă firma pune ambalaje pe piață (producător sau importator).",
    contentPackagingAnexa3:
      "Anexa 3 Ambalaje (Ordinul 794/2012), .xls și PDF — câte una pe fiecare punct de lucru care a avut ambalaje în an.",
    contentEvidence: "Același an ca tabel de lucru (Excel + PDF)",
    contentPartners: "Rezumat PDF cu autorizațiile partenerilor și statusul lor",
    contentAttachments: "Documentele justificative atașate mișcărilor (+ index)",
    note: "Notă: în afară de evidența gestiunii deșeurilor, dosarul NU înlocuiește formularele oficiale (SIM / AFM); e un pachet de lucru pentru pregătirea controlului.",
  },

  clients: {
    title: "Clienți",
    // Titlurile secțiunilor formularului de firmă. Le citește și „Setări", care arată exact
    // aceleași rubrici în citire — două nume pentru aceleași grupuri ar fi două hărți ale
    // aceluiași lucru.
    groupIdentity: "Identificare",
    groupAuthorization: "Autorizația de mediu",
    groupWasteManager: "Persoana desemnată cu gestiunea deșeurilor",
    groupReporting: "Raportare",
    // Nu „Raportare": acolo sunt rubricile care schimbă ce se **tipărește** (seria, unitatea).
    // Asta schimbă ce se **anunță** — e o obligație a firmei, nu o preferință de formular.
    groupObligations: "Obligații de raportare",
    groupContact: "Persoana de contact",
    subtitle:
      "Firmele pentru care ții evidența. Creezi firme și inviți utilizatori care primesc pe email un link de setare a parolei.",
    add: "Adaugă firmă",
    addTitle: "Adaugă firmă",
    editTitle: "Editează firma",
    empty: "Nicio firmă încă.",
    searchPlaceholder: "Caută după nume sau CUI...",
    emptyHint: "Firmele client se creează de aici, sau dintr-o cerere de cont primită mai jos.",
    loadError: "Nu am putut încărca firmele.",
    onlyPlatformAdmin: "Această secțiune este disponibilă administratorului platformei și consultanților.",
    // P2.13 — ce vede consultantul. Fără „cererile de cont de mai jos": inboxul e al platformei.
    subtitleConsultant:
      "Firmele cabinetului tău. Adaugi o firmă și îi inviți oamenii; o firmă adăugată de aici intră direct în cabinet și o poți alege din „Firma curentă”.",
    emptyHintConsultant: "Adaugă prima firmă pe care o gestionezi.",
    consultancy: "Cabinet",
    directClient: "Client direct",
    assignConsultancy: "Cabinet",
    assignTitle: "Cabinetul firmei {company}",
    // Spune urmarea, nu doar acțiunea: cine câștigă acces și cine îl pierde, și de când.
    assignHint:
      "Consultanții cabinetului ales văd firma și lucrează pe ea; cei ai cabinetului de dinainte nu o mai văd de la cererea următoare. „Client direct” o scoate din orice cabinet.",
    assignNone: "— client direct —",
    assigned: "Cabinetul firmei a fost schimbat.",
    assignError: "Nu am putut schimba cabinetul.",
    // columns
    name: "Denumire",
    cui: "CUI",
    type: "Tip",
    afm: "Obligație AFM",
    // form fields
    namePlaceholder: "ex. Demo Reciclare SRL",
    cuiPlaceholder: "ex. RO12345678",
    // Bifa veche rămâne, dar numai ca stare moștenită: cât timp nu s-a bifat nicio contribuție,
    // ea e cea care produce termenul lunar de dinainte. Se stinge singură pe măsură ce conturile
    // sunt completate — a opri o alertă pe o presupunere e mai rău decât a lăsa una gălăgioasă.
    afmLabel: "Datorează ceva la AFM, dar nu s-a stabilit ce (termen lunar, ca înainte)",
    afmContributions: "Contribuții la Fondul pentru mediu",
    afmContributionsHint:
      "Fiecare are cadența ei (OUG 196/2005, art. 11). Bifează-le pe cele datorate și clientul primește exact termenele lui — nu douăsprezece pe an pentru o contribuție anuală. Nebifat nu înseamnă „niciuna”, ci „nu s-a răspuns”.",
    environmentalAuthNumber: "Nr. autorizație de mediu",
    environmentalAuthExpiry: "Expirare autorizație de mediu",
    address: "Adresă",
    contactName: "Persoană de contact",
    contactEmail: "Email de contact",
    contactPhone: "Telefon de contact",
    anexa3Series: "Serie formulare Anexa 3",
    anexa3SeriesPlaceholder: "ex. HMB",
    anexa3SeriesHint: "Apare pe formularele de transport; numărul îl alocăm noi, crescător.",
    caenCode: "Cod CAEN",
    caenCodePlaceholder: "ex. 4677",
    caenCodeHint: "Apare în antetul declarației anuale. Necompletat, rubrica rămâne goală.",
    // Actul (HG 1061/2008) scrie tone, un model primit scrie KG. Nu alegem noi: alege firma,
    // o dată, iar necompletat înseamnă „ca în mișcare" — adică exact ce făcea aplicația înainte.
    anexa3Unit: "Unitatea de pe Anexa 3",
    anexa3UnitAsRecorded: "Ca în mișcare (implicit)",
    anexa3UnitKg: "Kilograme",
    anexa3UnitTons: "Tone",
    anexa3UnitHint:
      "Actul scrie „tone”, dar unele firme lucrează în kg. Cantitatea se convertește la tipărire, deci cifra și unitatea de pe formular sunt mereu de acord.",
    contactRole: "Funcția persoanei de contact",
    contactRolePlaceholder: "ex. Manager Mediu",
    contactRoleHint: "Se tipărește la „Întocmit / Funcția” pe evidența centralizată.",

    // --- Persoana desemnată cu gestiunea deșeurilor (OUG 92/2021 art. 23 alin. (4)-(5)) ---
    // Bloc separat dinadins: se confundă ușor cu persoana de contact de mai sus, dar aceea e
    // blocul de semnătură al declarației anuale, iar asta e o desemnare cerută de lege, care
    // poartă certificat de instruire. Inspectorul o cere printre primele lucruri.
    wasteManagerTitle: "Persoana desemnată cu gestiunea deșeurilor",
    wasteManagerHint:
      "Cerută de OUG 92/2021, art. 23 alin. (4): titularul unei autorizații de mediu desemnează o persoană dintre angajați sau deleagă obligația unei terțe persoane — de exemplu consultantul de mediu. Nu e persoana de contact de mai sus: aceea semnează evidența centralizată. Necompletată, dosarul de control o va marca lipsă.",
    wasteManagerName: "Nume",
    wasteManagerNamePlaceholder: "ex. Popescu Andrei",
    wasteManagerRole: "Calitatea",
    wasteManagerRolePlaceholder: "ex. consultant de mediu / responsabil mediu",
    wasteManagerExternal: "Cine e",
    wasteManagerExternalUnset: "— nu s-a răspuns —",
    wasteManagerExternalNo: "Angajat propriu",
    wasteManagerExternalYes: "Terță persoană delegată",
    wasteManagerTraining: "Certificat de instruire",
    wasteManagerTrainingPlaceholder: "program, număr și dată",
    wasteManagerTrainingHint:
      "Art. 23 alin. (5) cere ca persoana să fie instruită prin programe recunoscute la nivel național. Actul nu impune forma dovezii, deci se scrie liber.",

    // Rubrica de care atârnă jumătate din termenul de 30 aprilie. Se **întreabă**, nu se deduce:
    // capitolul 17 al nomenclatorului apare la oricine mișcă moloz, iar obligația e a titularului
    // autorizației — un transportator ar primi o alertă pentru o raportare care nu e a lui.
    // Cealaltă jumătate, uleiurile uzate, se citește din mișcări și nu cere nicio rubrică.
    constructionPermitHolder: "Titulară de autorizație de construire/desființare",
    constructionPermitHolderUnset: "— nu s-a răspuns —",
    constructionPermitHolderNo: "Nu",
    constructionPermitHolderYes: "Da",
    constructionPermitHolderHint:
      "OUG 92/2021, art. 49 alin. (9): titularii de autorizații de construire și/sau desființare raportează anual la APM, până la 30 aprilie, conformarea cu ținta de 70% la deșeurile din construcții (art. 17 alin. (7)). Sancțiune: 5.000–10.000 lei. Fără răspuns nu se generează nicio alertă — o alertă falsă e mai rea decât una lipsă.",

    // status / badges
    active: "Activ",
    inactive: "Inactiv",
    afmYes: "Da",
    afmNo: "Nu",
    // feedback
    created: "Firmă adăugată.",
    updated: "Firmă actualizată.",
    saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
    // invite user
    invite: "Invită utilizator",
    inviteTitle: "Invită utilizator în firma {company}",
    inviteEmail: "Email",
    inviteEmailPlaceholder: "utilizator@firma.ro",
    inviteRole: "Rol",
    inviteFirstName: "Prenume",
    inviteLastName: "Nume",
    inviteSubmit: "Trimite invitația",
    invited:
      "Utilizator invitat. I-am trimis (dacă emailul e configurat) un link de setare a parolei.",
    inviteError: "Invitația a eșuat. Verifică datele și încearcă din nou.",
    inviteHint:
      "Utilizatorul primește un email cu link de setare a parolei. Contul devine activ după ce își setează parola.",
  },

  /** P2.13 — cabinetele de consultanță, pe ecranul Clienți al platformei. */
  consultancies: {
    title: "Cabinete de consultanță",
    subtitle:
      "Firmele de consultanță care țin evidența pentru clienții lor. Creezi cabinetul și îi inviți primul consultant; de acolo își administrează singuri echipa și firmele. O firmă existentă se mută într-un cabinet din rândul ei, mai sus.",
    add: "Adaugă cabinet",
    addTitle: "Adaugă cabinet",
    name: "Denumire",
    namePlaceholder: "ex. EcoConsult SRL",
    cui: "CUI",
    cuiPlaceholder: "ex. RO12345678",
    companies: "Firme",
    consultants: "Consultanți",
    searchPlaceholder: "Caută după nume sau CUI...",
    empty: "Niciun cabinet încă.",
    emptyHint: "Un cabinet se creează după ce contractul cu el e semnat.",
    loadError: "Nu am putut încărca cabinetele.",
    created: "Cabinet adăugat.",
    saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
    invite: "Invită consultant",
    inviteTitle: "Invită un consultant în {consultancy}",
    invited: "Consultant invitat. Primește pe email linkul de setare a parolei.",
    inviteError: "Invitația a eșuat. Verifică datele și încearcă din nou.",
  },

  /** Plata abonamentelor, F2 — abonamentul și facturile, pentru cine le plătește (`/abonament`). */
  billing: {
    title: "Abonament",
    subtitle: "Pachetul tău, facturile emise și cum se plătesc.",
    none: "Contul ăsta n-are un abonament de plătit. Dacă firma e gestionată de un cabinet de consultanță, abonamentul e al cabinetului.",
    loadError: "Nu am putut încărca abonamentul.",
    plan: "Pachet",
    founder: "client fondator",
    startedAt: "Început pe",
    payment:
      "Factura se emite în prima zi a fiecărei perioade și vine pe email. Plata se face prin transfer bancar în 10 zile, în contul scris pe factură; la detalii plată treci seria și numărul facturii.",
    nextInvoice: "Următoarea factură ({from} – {to})",
    nextInvoiceHint:
      "Calculată pe ce e activ azi. Punctele de lucru sau firmele se numără din nou în prima zi a perioadei.",
    billingData: "Date de facturare",
    billingEmail: "Facturile vin pe",
    billingAddress: "Adresă",
    billingDataHint: "Ca să schimbi datele de facturare, scrie-ne la contact@wastehouse.ro.",
    invoices: "Facturi",
    noInvoices: "Nicio factură emisă încă.",
    number: "Factura",
    period: "Perioada",
    due: "Scadență",
    paidOn: "plătită {date}",
    document: "Document",
    pay: "Plătește online",
    // F3 — cardul
    method: "Cum plătești",
    methodCard: "Card",
    methodTransfer: "Transfer bancar",
    // Nu e doar o explicație: e autorizarea de debitare (contract art. 5.8, termeni 11.5). Schemele
    // de card cer ca, înainte de a salva cardul, omul să vadă ce se debitează, când, și cum oprește.
    methodCardHint:
      "Plătești factura cu cardul, pe pagina securizată Netopia. Dacă accepți acolo salvarea cardului, ne autorizezi să plătim de pe el fiecare factură emisă, la valoarea ei, în ziua emiterii (cu reîncercări în zilele 3, 6 și 10 dacă e refuzată). Poți revoca oricând, trecând pe transfer: cardul salvat se șterge.",
    methodTransferHint:
      "Plătești prin transfer, în contul de pe factură, în 10 zile. La detalii plată treci seria și numărul facturii.",
    methodSaved: "Salvat.",
    methodError: "Alegerea nu a putut fi salvată.",
    savedCard: "Card salvat: {pan}, expiră {expiry}",
    payByCard: "Plătește cu cardul",
    payByCardError: "Plata cu cardul nu a putut porni.",
    paidByCard: "cu cardul",
    cardRefused: "Ultima plată cu cardul nu a trecut: {reason}",
    returnPending: "Așteptăm confirmarea plății de la Netopia…",
    returnPaid: "Plata a trecut. Mulțumim!",
    returnFailed: "Plata nu a trecut: {reason}. Poți încerca din nou sau plăti prin transfer.",
    returnSlow:
      "Netopia n-a confirmat încă plata. Dacă ți s-a luat suma, factura apare plătită în câteva minute; nu plăti a doua oară.",
    // F4
    endsOn: "Abonamentul e oprit: ultima zi facturată e {date}. După ea contul rămâne doar pentru citire.",
    readOnlyOn: "Dacă factura restantă nu e plătită până pe {date}, contul trece în doar-citire.",
  },

  /** F4 — bannerul de sus, pe orice ecran și pentru orice rol. */
  billingBanner: {
    pastDue: "Abonamentul are o factură restantă.",
    pastDueReadOnlyOn: "Abonamentul are o factură restantă. Pe {date} contul trece în doar-citire.",
    readOnly:
      "Contul e doar pentru citire: abonamentul are o factură neplătită de peste 15 zile. Vezi și descarci tot; plata redeschide contul imediat.",
    cancelled: "Abonamentul e oprit: contul e doar pentru citire. Vezi și descarci tot.",
    action: "Vezi abonamentul",
    askAdmin: "Plata o face administratorul firmei.",
  },

  /** Plata abonamentelor, F1 — pachetul și prețul unui client, pe ecranul Clienți al platformei. */
  subscriptions: {
    action: "Abonament",
    title: "Abonament — {name}",
    hint: "Prețul se copiază din grilă când creezi abonamentul și nu se mai schimbă singur, doar dacă muți clientul pe alt pachet. Factura se emite prin FGO în prima zi a fiecărei perioade, cu plata prin transfer în 10 zile.",
    billing: "Date de facturare",
    billingHint: "Numele și CUI-ul vin din firmă sau din cabinet. Fără județ, localitate și adresă, factura nu pleacă la FGO.",
    billingEmail: "Email pentru facturi",
    billingEmailHint: "Gol: emailul de contact al firmei.",
    billingCounty: "Județ",
    billingCountyPlaceholder: "Alege județul",
    billingCity: "Localitate",
    billingAddress: "Adresă",
    invoices: "Facturi",
    noInvoices: "Nicio factură încă. Prima se emite în data de start.",
    invoiceStatus: {
      DRAFT: "Neemisă",
      ISSUED: "Emisă",
      PAID: "Plătită",
    },
    invoiceDue: "scadentă {date}",
    invoicePdf: "PDF",
    runBilling: "Emite facturile scadente acum",
    runBillingDone: "Facturare rulată: {issued} emise, {failed} căzute, {paid} plătite.",
    runBillingFailure: "Căzută — {client}: {reason}",
    runBillingNotStarted: "Nefacturat, începe pe {date} — {client}",
    runBillingOff: "Facturarea e oprită: lipsesc cheile FGO pe server.",
    runBillingError: "Facturarea nu a putut fi rulată.",
    none: "Clientul n-are abonament: nu e facturat și nu e restricționat.",
    plan: "Pachet",
    plans: {
      GENERATOR: "Generator",
      GENERATOR_PACKAGING: "Generator + Ambalaje",
      FULL_SERVICE: "Serviciu complet",
      CONSULTANCY: "Abonament de cabinet",
    },
    planHint: "Colectorii intră pe Generator: modulul de depozit nu se facturează încă.",
    startedAt: "Data de start",
    startedAtHint:
      "Ziua de la care se facturează. Fiecare lună se plătește întreagă, din ziua asta până în ziua dinaintea ei din luna următoare.",
    founder: "Client fondator: implementare gratuită, preț blocat 2 ani",
    founderCount: "{n} din 30 folosite",
    status: {
      PENDING: "Așteaptă prima plată",
      ACTIVE: "Activ",
      PAST_DUE: "Restant",
      READ_ONLY: "Doar citire",
      CANCELLED: "Oprit",
    },
    firstInvoice: "Prima factură ({period})",
    monthlyInvoice: "Apoi lunar ({period})",
    total: "Total",
    previewHint:
      "Calculat pe punctele de lucru sau firmele active de azi. La facturare se numără la începutul fiecărei perioade, deci o firmă adăugată în cabinet se plătește din perioada următoare.",
    create: "Creează abonamentul",
    saved: "Abonament salvat.",
    saveError: "Abonamentul nu a putut fi salvat.",
    remove: "Șterge abonamentul",
    removed: "Abonament șters. Clientul nu mai e facturat.",
    removeError: "Abonamentul nu a putut fi șters.",
    loadError: "Nu am putut încărca abonamentul.",
    // F3/F4
    method: "Plată",
    methodNone: "neales (transfer)",
    savedCard: "card {pan}, expiră {expiry}",
    paidByCard: "cu cardul",
    notInFgo: "încasarea nu e încă în FGO",
    cancel: "Oprește abonamentul",
    cancelConfirm:
      "Se facturează încă perioada de preaviz (o lună), apoi abonamentul se oprește și contul rămâne doar pentru citire. Continui?",
    cancelled: "Abonament oprit. Ultima zi facturată: {date}.",
    cancelError: "Abonamentul nu a putut fi oprit.",
    resume: "Anulează oprirea",
    resumed: "Oprirea a fost anulată.",
    endsOn: "Oprit: ultima zi facturată {date}",
  },

  /** P2.13 — echipa cabinetului, pe ecranul Clienți al consultantului. */
  consultancyTeam: {
    title: "Echipa cabinetului",
    subtitle:
      "Colegii care lucrează pe firmele cabinetului. Toți au aceleași drepturi: văd toate firmele cabinetului, adaugă firme și invită colegi.",
    invite: "Invită coleg",
    inviteTitle: "Invită un coleg",
    empty: "Niciun coleg încă.",
    emptyHint: "Colegii invitați primesc pe email un link din care își aleg parola.",
    loadError: "Nu am putut încărca echipa.",
    invited: "Coleg invitat. Primește pe email linkul de setare a parolei.",
    // Ca la utilizatorii firmei, plus singurul lucru care diferă: pierde toate firmele deodată.
    confirmDeactivate:
      "Sesiunile deschise se închid imediat și nu mai vede nicio firmă a cabinetului. Ce a înregistrat rămâne neatins, cu numele lui. Se poate reactiva oricând.",
  },

  /** P2.14 — antetul cabinetului pe rapoartele neoficiale. Formularele oficiale nu îl primesc, și ecranul o spune. */
  consultancyBranding: {
    title: "Antetul cabinetului pe rapoarte",
    subtitle:
      "Logoul și un rând de contact apar sus pe rezumatul evidenței (PDF și Excel), pe lista autorizațiilor partenerilor și în README-ul dosarului de control, la toate firmele cabinetului.",
    officialNote:
      "Formularele oficiale (fișa de evidență, evidența centralizată, anexele, avizul) rămân exact ca modelul din lege, fără antet.",
    logo: "Logo",
    logoHint: "PNG sau JPG, cel mult 500 KB. Se potrivește pe o înălțime de circa 1,5 cm.",
    chooseLogo: "Alege logoul",
    replaceLogo: "Schimbă logoul",
    removeLogo: "Șterge logoul",
    noLogo: "Fără logo",
    logoTooLarge: "Logoul e prea mare. Cel mult 500 KB.",
    logoUploaded: "Logoul a fost salvat.",
    logoRemoved: "Logoul a fost șters.",
    headerLine: "Rândul de contact",
    headerLinePlaceholder: "Ex.: Tel. 0722 000 000 · contact@cabinet.ro · Oradea",
    headerLineHint: "Opțional, cel mult 200 de caractere. Gol = doar numele cabinetului.",
    save: "Salvează rândul",
    saved: "Rândul de antet a fost salvat.",
    preview: "Așa arată sus pe pagină",
    preparedBy: "Pregătit de {name}",
    emptyPreview: "Fără logo și fără rând de contact, rapoartele rămân fără antet.",
    loadError: "Nu am putut încărca antetul cabinetului.",
    saveError: "Nu am putut salva antetul.",
  },

  /**
   * P2.13, felia 2 — „Firmele mele": ce e de făcut pe fiecare firmă a cabinetului, pe un singur ecran.
   * Cuvintele celulelor sunt ale Panoului firmei, ca un consultant să recunoască același lucru după ce comută.
   */
  consultancyOverview: {
    title: "Firmele mele",
    subtitle:
      "Ce cere atenție pe fiecare firmă a cabinetului: termene, ce blochează depunerea și autorizațiile partenerilor. Firmele cu termene depășite sunt primele.",
    loadError: "Nu am putut încărca firmele cabinetului.",
    // Pentru orice alt rol decât consultantul. Textul de la Clienți promitea secțiunea și administratorului.
    consultantsOnly:
      "Panoul e al consultanților: fiecare vede aici firmele cabinetului lui. Administratorul platformei le găsește în Clienți.",
    empty: "Cabinetul nu are încă nicio firmă activă.",
    emptyHint: "Adaugă prima firmă din Clienți; apare aici imediat.",
    emptyAction: "Deschide Clienți",
    summaryAllClear: "Toate firmele sunt la zi.",
    summary: "{attention} din {total} cer atenție",
    company: "Firmă",
    deadlines: "Termene",
    blockers: "Blocaje la depunere",
    partners: "Parteneri",
    open: "Deschide",
    overdue: "{count} depășite",
    overdueOne: "1 depășit",
    next: "Următorul: {date}",
    noNext: "Niciun termen în perioada următoare",
    notGenerated: "Termenele anului nu sunt generate",
    generate: "Generează",
    withoutCode: "{count} fără cod R/D",
    awaitingWeighing: "{count} în așteptarea cântăririi",
    mirror: "{count} cod-oglindă fără document",
    none: "Nimic",
    partnersExpiring: "{count} cu autorizația expirând",
    clear: "La zi",
    // Pe telefon tabelul se citește rând cu rând; eticheta spune ce e fiecare cifră.
    searchPlaceholder: "Caută după nume sau CUI…",
  },

  // Romanian month names (index 0 = January), for display of the `month` field.
  months: [
    "Ianuarie",
    "Februarie",
    "Martie",
    "Aprilie",
    "Mai",
    "Iunie",
    "Iulie",
    "August",
    "Septembrie",
    "Octombrie",
    "Noiembrie",
    "Decembrie",
  ],

  // Romanian labels for backend enums (constants are English by convention).
  enums: {
    /**
     * Rolurile, în românește. Până acum bara laterală tipărea chiar constanta din backend —
     * `PLATFORM_ADMIN` — sub adresa de email.
     */
    role: {
      PLATFORM_ADMIN: "Administrator platformă",
      CONSULTANT: "Consultant",
      ADMIN: "Administrator",
      OPERATOR: "Operator",
      CLIENT_VIEWER: "Vizualizare",
    },

    /** P1.11 — faptele din jurnalul de audit, ca verbe. */
    auditAction: {
      CREATE: "Creare",
      UPDATE: "Modificare",
      DELETE: "Ștergere",
      DEACTIVATE: "Dezactivare",
      REACTIVATE: "Reactivare",
      REGENERATE: "Recalculare",
    },

    /**
     * Ce fel de înregistrare a fost atinsă. Cheile sunt numele claselor din backend, fiindcă exact
     * așa le scrie jurnalul — traducerea se face aici, o singură dată.
     */
    auditEntity: {
      WasteMovement: "Mișcare",
      Company: "Datele firmei",
      Partner: "Partener",
      PartnerWorkPoint: "Punct de lucru al partenerului",
      WorkPoint: "Punct de lucru",
      InternalGenerator: "Generator intern",
      Driver: "Șofer",
      AnalysisBulletin: "Buletin de analiză",
      Attachment: "Atașament",
      AppUser: "Utilizator",
      MonthlyEvidence: "Evidența lunară",
    } as Record<string, string>,

    /**
     * Numele rubricilor, pentru lista de modificări.
     *
     * <p>⚠️ Dicționarul e **parțial, dinadins**, și ce lipsește se afișează așa cum e scris în
     * model. Alternativa — o traducere pentru fiecare dintre cele ~90 de coloane ale aplicației —
     * ar fi o listă pe care nimeni n-o ține la zi, iar prima rubrică adăugată mâine ar apărea aici
     * ca un gol. Un nume englezesc e mai puțin plăcut decât unul românesc și infinit mai bun decât
     * niciunul: aici sunt cele pe care cineva chiar le caută după un control.
     */
    auditField: {
      quantity: "Cantitate",
      unit: "Unitate",
      date: "Data",
      unloadDate: "Data descărcării",
      wasteCode: "Cod de deșeu",
      operation: "Operațiune",
      operationCode: "Cod R/D",
      partner: "Partener",
      transportPartner: "Transportator",
      workPoint: "Punct de lucru",
      internalGenerator: "Generator intern",
      partnerWorkPoint: "Punctul de lucru al partenerului",
      documentReference: "Document",
      register: "Registru",
      notes: "Observații",
      deleted: "Șters",
      active: "Activ",
      enabled: "Cont activ",
      role: "Rol",
      email: "E-mail",
      name: "Denumire",
      cui: "CUI",
      authorizationNumber: "Autorizație",
      authorizationExpiry: "Expirarea autorizației",
      authorizationIssueDate: "Emiterea autorizației",
      visaDecisionNumber: "Decizia de viză",
      visaDecisionDate: "Data deciziei de viză",
      visaValidUntil: "Viza până la",
      environmentalAuthNumber: "Autorizație de mediu",
      caenCode: "Cod CAEN",
      identification: "Act de identitate",
      vehicleRegistration: "Număr de înmatriculare",
      driverName: "Șofer",
      laboratory: "Laborator",
      issueDate: "Data buletinului",
      fileName: "Fișier",
    } as Record<string, string>,

    wasteOperation: {
      GENERATED: "Generare",
      COLLECTED: "Preluare de la terți",
      RECOVERED: "Valorificare",
      DISPOSED: "Eliminare",
      UNCLASSIFIED_OUT: "Ieșire neclasificată",
    },
    // The commercial role of a partner, as agreed on 23.08.2026.
    partnerRole: {
      client: "Client",
      clientHint: "Îi predai deșeu și îi facturezi tu.",
      supplier: "Furnizor",
      supplierHint: "Îți prestează serviciul și îți facturează el.",
      both: "Client + furnizor",
      none: "Rol nestabilit",
      noneHint: "Alege rolul la editare — nu îl putem deduce din datele existente.",
    },
    wasteRegister: {
      ANEXA_1: "Evidența gestiunii — deșeu propriu",
      ART_48: "Registru cronologic — preluat de la terți",
    },
    // HG 856/2002, anexa nr. 1, cap. 2, nota 3. Amândouă literele se tipăresc din 16.09.2026
    // (proprietarul): V la valorificare, E la eliminare.
    treatmentPurpose: {
      V: "V — pentru valorificare",
      E: "E — în vederea eliminării",
    },
    // HG 856/2002, anexa nr. 1, cap. 2, nota 1 — tipul de stocare.
    storageType: {
      RM: "RM — Recipient metalic",
      RP: "RP — Recipient de plastic",
      BZ: "BZ — Bazin decantor",
      CT: "CT — Container transportabil",
      CF: "CF — Container fix",
      S: "S — Saci",
      PD: "PD — Platformă de deshidratare",
      VN: "VN — În vrac, neacoperit",
      VA: "VA — În vrac, incintă acoperită",
      RL: "RL — Recipient din lemn",
      A: "A — Altele",
    },
    // HG 856/2002, anexa nr. 1, cap. 2, nota 2 — modul de tratare. „D” e deshidratare, nu un cod
    // de eliminare: coliziunea de abreviere e a formularului, iar cele două sunt coloane diferite.
    treatmentMethod: {
      TM: "TM — Tratare mecanică",
      TC: "TC — Tratare chimică",
      TMC: "TMC — Tratare mecano-chimică",
      TB: "TB — Tratare biochimică",
      TT: "TT — Tratare termică",
      D: "D — Deshidratare",
      A: "A — Altele",
    },
    // HG 856/2002, anexa nr. 1, cap. 2, nota 4 — mijlocul de transport.
    transportMeans: {
      AS: "AS — Autospeciale",
      AN: "AN — Auto nespecial",
      H: "H — Transport hidraulic",
      CF: "CF — Cale ferată",
      A: "A — Altele",
    },
    // HG 856/2002, anexa nr. 1, cap. 2, nota 5 — destinația deșeului. Alta decât caseta
    // „Destinat:” de pe Anexa 3: aici e o singură valoare.
    wasteDestination: {
      DO: "DO — Depozitul de gunoi al orașului/comunei",
      HP: "HP — Haldă proprie",
      HC: "HC — Haldă industrială comună",
      I: "I — Incinerare în scopul eliminării",
      Vr: "Vr — Valorificare prin agenți economici autorizați",
      P: "P — Utilizare în propria întreprindere",
      Ve: "Ve — Valorificare energetică prin agenți autorizați",
      A: "A — Altele",
    },
    // Anexa 3 la HG 1061/2008, caseta „Destinat:”. Se pot bifa mai multe.
    transportDestination: {
      COLECTARE: "Colectării",
      STOCARE_TEMPORARA: "Stocării temporare",
      TRATARE: "Tratării",
      VALORIFICARE: "Valorificării",
      ELIMINARE: "Eliminării",
    },
    physicalState: {
      SOLID: "Solid",
      LIQUID: "Lichid",
      SLUDGE: "Nămol",
      PASTY: "Păstos",
      POWDER: "Pulbere",
      GASEOUS: "Gazos",
    },
    unit: {
      KG: "kg",
      TONS: "tone",
    },
    // Ce este partenerul in raport cu deseul. Transportatorul nu mai e o categorie: e o rubrica a
    // transportului, pe miscare.
    packagingCategory: {
      SALES: "De desfacere (fabricate/importate)",
      PRIMARY: "Primar",
      SECONDARY: "Secundar și de transport",
    },
    packagingMaterial: {
      STICLA: "Sticlă",
      PET: "PET",
      ALTE_PLASTICE: "Alte plastice",
      HARTIE_CARTON: "Hârtie carton",
      ALUMINIU: "Aluminiu",
      OTEL: "Oțel",
      LEMN: "Lemn",
      ALTELE: "Altele",
    },
    afmContribution: {
      WITHHOLDING_2_PERCENT: "2% reținut la sursă (lunar, 25)",
      WITHHOLDING_2_PERCENT_HINT:
        "OUG 196/2005 art. 9 lit. a): 2% din vânzarea oricărui deșeu, reținuți de colector. E a centrelor de colectare, nu a firmei care doar vinde deșeul — aceleia i se reține din factură.",
      CIRCULAR_ECONOMY: "Economia circulară (trimestrial, 25)",
      CIRCULAR_ECONOMY_HINT:
        "OUG 196/2005 art. 9 lit. c): a depozitelor, pentru deșeurile duse la eliminare.",
      PACKAGING: "Ambalaje (anual, 25 ianuarie)",
      PACKAGING_HINT:
        "OUG 196/2005 art. 9 lit. d): a celor care pun produse ambalate pe piață — producători și importatori.",
    },
    partnerType: {
      GENERATOR: "Generator",
      COLLECTOR: "Colector",
      // Cerut pe 24.08.2026. Nu e cosmetic: de el atârnă ce se prebifează la „Destinat:” pe
      // Anexa 3 — la colector se bifează colectării + valorificării, la valorificator doar
      // valorificării. Codul R/D nu poate face diferența, fiindcă același R3 merge la amândoi.
      RECOVERER: "Valorificator",
    },
    companyType: {
      GENERATOR: "Generator",
      COLLECTOR: "Colector",
      BOTH: "Generator și colector",
    },
    // „Ce tip de generator", din meeting-ul cu specialista (23.08.2026). Trioul e clasificarea din
    // Legea 249/2015, anexa nr. 1: „producătorii de ambalaje şi produse ambalate, importatorii,
    // comercianţii, distribuitorii".
    marketRole: {
      PRODUCER: "Producător",
      IMPORTER: "Importator",
      TRADER: "Comerciant",
    },
    marketRoleHint: {
      PRODUCER: "Fabric sau ambalez eu produsele pe care le vând în România.",
      IMPORTER: "Aduc în țară produse ambalate și le vând în România.",
      TRADER: "Vând marfă ambalată de altcineva — nu eu am pus ambalajul pe piață.",
    },
    inviteRole: {
      ADMIN: "Administrator",
      OPERATOR: "Operator",
      CLIENT_VIEWER: "Vizualizare (read-only)",
    },
    reportType: {
      // Ce se depune pe 15 martie e chiar evidența din HG 856/2002, anexa 1, încărcată
      // în sistemul pus la dispoziție de APM (OUG 92/2021 art. 48 alin. (1)). „Raportarea SIM"
      // numea canalul și lăsa clientul să ghicească ce are de pregătit.
      SIM_ANNUAL: "Evidența gestiunii deșeurilor generate (anual, 15 martie)",
      // Trei termene, nu unul: OUG 196/2005 art. 11 are trei cadențe, iar până pe 24.08.2026
      // dădeam termenul lunar oricui avea bifa — 11 alerte greșite pe an la o firmă cu
      // contribuție doar anuală.
      AFM_MONTHLY: "AFM (lunar, 25) — contribuția de 2% reținută la sursă",
      AFM_QUARTERLY: "AFM (trimestrial, 25) — contribuția pentru economia circulară",
      // Două obligații cad pe 25 ianuarie, la același destinatar (AFM): contribuția pentru
      // ambalaje (OUG 196/2005 art. 11 alin. (2)) și notificarea din Ordinul 794/2012 art. 3.
      // Un al doilea rând ar pune două alerte în aceeași zi la aceeași adresă — exact zgomotul
      // pe care l-a stins `V21`. Un rând, o etichetă care le numește pe amândouă.
      AFM_ANNUAL:
        "AFM (anual, 25 ianuarie) — contribuția pentru ambalaje și notificarea că obiectivele se îndeplinesc individual",
      // 25 februarie e alt document, alt destinatar, alt act: raportarea de ambalaje din
      // Ordinul 794/2012 art. 6, la agenția județeană — nu la AFM. Construiam documentul de la
      // `V22`, dar nu pleca nicio alertă pentru el (punctul 3 al auditului).
      PACKAGING_ANNUAL: "Anexa 1 Ambalaje (anual, 25 februarie) — la agenția județeană de mediu",
      PACKAGING_ANNEX3:
        "Anexa 3 Ambalaje (anual, 25 februarie) — deșeurile de ambalaje preluate, la agenția de mediu a fiecărui punct de lucru",
      // 30 aprilie — al doilea termen anual la APM, găsit abia pe 10.09.2026 la recitirea legii-
      // cadru pe forma consolidată. Lipsea din calendar cu totul: aveam 15 martie, 25 ianuarie,
      // 25 februarie și cadențele AFM, iar un client cu un butoi de ulei uzat afla din tăcerea
      // noastră că aprilie n-are nimic. Două obligații pe un rând, ca la `AFM_ANNUAL`: aceeași zi,
      // același destinatar — iar eticheta le numește pe amândouă, fiindcă firma poate datora doar
      // una din ele și trebuie să vadă care.
      APM_ANNUAL_APRIL:
        "Raportare anuală la APM (30 aprilie) — uleiuri uzate (art. 31) și deșeuri din construcții (art. 17 alin. (7))",
      // 31 mai — al patrulea termen anual, găsit pe 11.09.2026. Eticheta numeşte documentul, nu
      // articolul: programul de prevenire nu seamănă cu nimic altceva din calendar, iar clientul
      // trebuie să vadă din rând ce anume pregăteşte. Semnalul e autorizaţia de mediu din profil.
      APM_ANNUAL_MAY:
        "Programul de prevenire și reducere a deșeurilor (anual, 31 mai) — la agenția județeană de mediu",
      OTHER: "Altă raportare",
    },
    deadlineStatus: {
      UPCOMING: "De făcut",
      DONE: "Finalizat",
      OVERDUE: "Depășit",
    },
    // R/D recovery & disposal operation codes (Waste Framework Directive annexes).
    wasteOperationCode: {
      R1: "R1 — Combustibil sau altă sursă de energie",
      R2: "R2 — Recuperarea/regenerarea solvenților",
      R3: "R3 — Reciclarea substanțelor organice (non-solvenți)",
      R4: "R4 — Reciclarea metalelor și compușilor metalici",
      R5: "R5 — Reciclarea altor materiale anorganice",
      R6: "R6 — Regenerarea acizilor sau a bazelor",
      R7: "R7 — Recuperarea componentelor pentru reducerea poluării",
      R8: "R8 — Recuperarea componentelor din catalizatori",
      R9: "R9 — Rerafinarea sau alte reutilizări ale petrolului",
      R10: "R10 — Împrăștiere pe sol (beneficii agricole/ecologice)",
      R11: "R11 — Utilizarea deșeurilor din operațiunile R1–R10",
      R12: "R12 — Schimb de deșeuri pentru operațiunile R1–R11",
      R13: "R13 — Stocare înaintea operațiunilor R1–R12",
      D1: "D1 — Depozitare în sau pe sol",
      D2: "D2 — Tratarea solului",
      D3: "D3 — Injectare în adâncime",
      D4: "D4 — Acumulare la suprafață (iazuri, lagune)",
      D5: "D5 — Depozite special construite",
      D6: "D6 — Evacuare în ape (cu excepția mărilor)",
      D7: "D7 — Evacuare în mări/oceane",
      D8: "D8 — Tratament biologic",
      D9: "D9 — Tratament fizico-chimic",
      D10: "D10 — Incinerare pe sol",
      D11: "D11 — Incinerare pe mare",
      D12: "D12 — Depozitare permanentă",
      D13: "D13 — Amestecare înaintea operațiunilor D1–D12",
      D14: "D14 — Reambalare înaintea operațiunilor D1–D13",
      D15: "D15 — Stocare înaintea operațiunilor D1–D14",
    },
  },

  accountRequest: {
    // Formularul are șase secțiuni, deci se salvează singur în browser cât îl completezi. Restaurarea
    // se anunță, nu se face pe furiș: altfel omul nu știe de ce vede date pe care nu le-a tastat acum.
    draftRestored:
      "Am păstrat ce completaseși și am pus la loc în formular. Verifică datele înainte de trimitere.",
    draftDiscard: "Șterge și începe de la zero",
    // Public page
    title: "Cerere de cont",
    subtitle:
      "Conturile se creează de echipa WasteHouse, pe baza acestui formular. Completează-l o dată — din răspunsuri configurăm aplicația pentru tipul tău de activitate, ca să vezi doar ce îți trebuie.",
    // Cele trei rânduri din capul paginii. Un prospect care intră pe link nu știe nici unde a
    // ajuns, nici cât durează, nici ce primește — iar formularul are șase secțiuni, deci
    // întrebarea „merită să încep?" se pune înainte de prima rubrică, nu după.
    stepsTitle: "Cum funcționează",
    step1: "Completezi formularul — câteva minute, doar trei rubrici sunt obligatorii.",
    step2: "Îl citim și configurăm aplicația pentru activitatea ta.",
    step3: "Primești datele de acces pe email, în 1–2 zile lucrătoare.",
    // Verdele din stânga (direcția „Poster”, 16.09.2026): titlul, apoi cuprinsul formularului —
    // cei trei pași ai lui, 01–03, cu pasul curent aprins și ce s-a completat scris sub cei trecuți.
    posterHeadline: "Cere un cont.",
    posterAccent: "Restul îl facem noi.",
    posterLede: "Conturile le creează echipa WasteHouse, din acest formular.",
    step1Name: "Compania",
    step1Lead: "Nume, CUI, sediu, ce face cu deșeurile.",
    step2Name: "Punctul de lucru",
    step2Lead: "Unde se produce deșeul, cu autorizația.",
    step3Name: "Contactul",
    step3Lead: "Unde trimitem datele de acces.",
    step4Name: "Deșeurile",
    step4Lead: "Ce ai de obicei și ce faci cu ele.",
    noteRequired: "Patru pași, câteva minute. Rubricile cu * sunt obligatorii.",
    noteAccess: "Accesul vine pe email, în 1–2 zile lucrătoare.",
    posterNote: "Nimic nu pleacă până nu apeși „Trimite cererea”.",
    // Formularul în pași (proprietarul, 16.09.2026, după ce a văzut 22 de rubrici pe o pagină: „nu e
    // prea lung?”, apoi „vreau mai prietenos” la „Cine sunteți?”, apoi, la blocul pliat cu opționale:
    // „hai să obligăm omul să își facă și punct de lucru și tot ce e sub «Detalii care ne scutesc…»”).
    // Fiecare pas e un titlu prietenos, nu o întrebare; aproape totul e obligatoriu, ca la aprobare
    // să nu mai fie nevoie de un telefon.
    stepOf: "Pasul {n} din 4 · {name}",
    step1Title: "Detalii despre compania ta",
    step1Subtitle: "Datele din certificatul de înregistrare, ca să putem crea firma exact cum e în acte.",
    step2Title: "Punctul de lucru",
    step2Subtitle:
      "Locul unde se produce efectiv deșeul. Evidența se ține pe punct de lucru, nu pe firmă, iar adresa e des alta decât sediul.",
    step3Title: "Datele tale de contact",
    step3Subtitle: "Pe acest email primești linkul cu care îți alegi parola, în 1–2 zile lucrătoare.",
    step4Title: "Deșeurile companiei tale",
    step4Subtitle:
      "Bifează ce ai de obicei. Dacă nu ești sigur de lista de deșeuri, lasă gol — o stabilim împreună la configurarea contului.",
    continueStep: "Continuă",
    backStep: "Înapoi",
    wasteOtherHint: "Ce nu e în listă, în cuvintele tale. Le transformăm noi în coduri.",
    // Rubricile obligatorii care n-au un mesaj al lor. Scurt, fiindcă stă sub o rubrică marcată.
    errRequired: "Rubrica e obligatorie.",
    errRequiredDate: "Scrie data, ca în act.",
    errMarketRoles: "Bifează cel puțin una — din răspuns știm dacă raportezi și ambalaje.",
    // Autorizația e obligatorie, dar nu toți generatorii au una: Anexa 1 a Ordinului 1798/2007 o cere
    // doar activităților cu impact (producție, HoReCa, ateliere…). Bifa ține loc de răspuns — echipa
    // vede că omul a spus „nu am”, nu că a sărit rubrica. La colectori nu apare: ei au întotdeauna.
    noEnvAuth: "Activitatea noastră nu are nevoie de autorizație de mediu",
    noEnvAuthHint:
      "Autorizația e cerută doar pentru activitățile din Anexa 1 a Ordinului 1798/2007 (producție, restaurante, ateliere, construcții). Dacă nu ești sigur, lasă nebifat și scrie ce ai.",
    noEnvAuthNote: "Fără autorizație de mediu — activitatea nu o cere (declarat în formular).",
    haveAccount: "Ai deja cont?",
    goToLogin: "Intră",
    // „Ce faceți cu deșeurile” ca trei carduri cu o propoziție (regula: sub șapte opțiuni, nu
    // `Select`). Valorile rămân `CompanyType`; pe listele interne firma se numește tot
    // „Generator” / „Colector” (`enums.companyType`).
    companyTypeChoice: {
      GENERATOR: { label: "Le generăm", hint: "Ies din activitatea firmei: brutărie, atelier, magazin, șantier." },
      COLLECTOR: { label: "Le colectăm", hint: "Primim deșeuri de la alții și le predăm mai departe." },
      BOTH: { label: "Amândouă", hint: "Generăm deșeuri proprii și colectăm de la alții." },
    },
    requiredLegend: "Rubricile marcate cu * sunt obligatorii.",
    sectionCompany: "Firma",
    sectionWorkPoint: "Punctul de lucru",
    sectionContact: "Persoana de contact",
    sectionAuthorization: "Autorizația de mediu",
    sectionTransport: "Transport",
    sectionWaste: "Deșeurile",
    // Rubricile respinse se marchează una câte una, ca pe formularul de mișcare. Bannerul din cap
    // spunea „verifică rubricile marcate mai jos" fără să marcheze nimic — exact defectul reparat
    // acolo pe 07.09, rămas aici.
    errCompanyName: "Scrie denumirea firmei, ca în certificatul de înregistrare.",
    errCui: "Scrie CUI-ul firmei.",
    // Aceeași formă pe care o cere `CompanyService` la crearea firmei: un CUI care nu trece pe aici
    // ar trece de formular și ar cădea abia la aprobare, în mâinile altcuiva.
    errCuiFormat: "CUI-ul se scrie din 2–10 cifre, cu sau fără „RO” în față. Ex.: RO12345678.",
    errContactEmail: "Scrie emailul pe care să-ți răspundem.",
    errContactEmailFormat: "Emailul nu pare complet. Ex.: nume@firma.ro",
    companyName: "Denumirea firmei",
    cui: "CUI",
    cuiPlaceholder: "ex. RO12345678",
    companyType: "Ce faceți cu deșeurile",
    companyAddress: "Adresa sediului social",
    workPointName: "Denumirea punctului de lucru",
    workPointNamePlaceholder: "ex. Punct de lucru Cluj",
    workPointAddress: "Adresa punctului de lucru",
    workPointHint:
      "Locul unde se produce efectiv deșeul. Evidența se ține pe punct de lucru, nu pe firmă, iar adresa e des alta decât sediul social.",
    contactName: "Nume și prenume",
    contactEmail: "Email",
    contactPhone: "Telefon",
    contactHint: "Pe acest email primești datele de acces, după ce creăm contul.",
    // Cele două rubrici pe care le cere antetul declarației anuale. Formularea e de validat cu
    // specialista (întrebarea S) — până atunci spun ce știm și nu presupun nimic în plus.
    contactRole: "Funcția",
    contactRolePlaceholder: "ex. Manager Mediu, administrator",
    contactRoleHint:
      "Persoana care semnează raportarea anuală. Se tipărește la „Întocmit / Funcția” pe declarație.",
    caenCode: "Cod CAEN",
    caenCodePlaceholder: "ex. 4677",
    caenCodeHint:
      "Codul activității principale, din certificatul de înregistrare. Apare în antetul declarației anuale de deșeuri.",
    environmentalAuthNumber: "Nr. autorizație de mediu",
    environmentalAuthExpiry: "Expiră la",
    transportMeans: "Cu ce transportați",
    transportMeansPlaceholder: "ex. autoutilitară 3,5 t, container 20 mc",
    transportLicenseNumber: "Licență de transport mărfuri",
    transportLicenseExpiry: "Expiră licența",
    transportHint: "Se completează doar dacă preluați deșeuri de la terți.",
    sectionMarketRole: "Tipul de generator",
    marketRoles: "Ce e firma dumneavoastră pentru marfa pe care o vinde?",
    marketRolesHint:
      "Bifează tot ce se potrivește; o firmă poate fi și producător, și importator. Din răspuns știm dacă aveți și obligația de raportare a ambalajelor. Evidența deșeurilor se ține oricum.",
    operationCodes: "Ce se întâmplă cu deșeul",
    operationCodesHint:
      "Bifează operațiunile pe care le folosiți. Doar acestea vor apărea în aplicație — poți lăsa necompletat dacă nu știi.",
    // Cele 28 de bife R/D stăteau deschise în fața cuiva care poate n-a auzit de R13. Necompletat
    // era deja un răspuns valid — „nu se restrânge nimic" — dar formularul nu spunea asta nicăieri,
    // deci arăta ca o listă la care ai rămas dator. Acum e o alegere cu două ieșiri, prima onorabilă.
    operationCodesUnknown: "Nu știu — le stabilim împreună",
    operationCodesUnknownHint:
      "Alegerea obișnuită dacă n-ai lucrat cu codurile R/D. Le completăm la configurarea contului, cu autorizația de mediu în față.",
    operationCodesChoose: "Le știu, le aleg acum",
    operationCodesChooseHint:
      "Codurile din autorizația de mediu. Doar cele bifate apar mai târziu la înregistrarea mișcărilor.",
    // Perechea „…SelectedOne / …Selected" a dispărut: `countOf` acoperă și forma de la 20 în sus,
    // pe care două șiruri fixe n-o puteau prinde („20 operațiuni alese").
    operationCodesSelected: "{count}",
    recovery: "Valorificare (R)",
    disposal: "Eliminare (D)",
    wasteCodesText: "Ce deșeuri generați / preluați",
    wasteListHint:
      "Bifează ce ai din lista de mai jos, dacă vrei. Poți lăsa totul gol: le stabilim împreună la configurarea contului.",
    wasteOtherText: "Alte deșeuri",
    wasteCodesTextPlaceholder: "ex. nămol de la separator, textile, toner",
    wasteCodesTextHint:
      "Ce nu e în listă, scris în cuvintele tale. Le transformăm noi în codurile din Lista Europeană a Deșeurilor.",
    notes: "Alte observații",
    submit: "Trimite cererea",
    submitting: "Se trimite...",
    successTitle: "Cererea a fost trimisă",
    successBody:
      "Echipa WasteHouse o verifică și îți creează contul. Primești datele de acces pe emailul completat.",
    // „Am primit cererea" răspunde la ce s-a întâmplat, nu la ce urmează — iar omul tocmai a dat
    // datele firmei lui unui site pe care nu-l cunoaște. Termenul e cel pe care îl ținem, nu unul
    // rotund: mai bine două zile promise și una ținută.
    successNextTitle: "Ce urmează",
    successNext1: "Îți citim răspunsurile și configurăm aplicația pentru activitatea ta.",
    successNext2: "Te sunăm sau îți scriem dacă ceva are nevoie de o lămurire.",
    successNext3: "Primești pe {email} un link pe care îți alegi parola. În 1–2 zile lucrătoare.",
    successNoEmailFallback: "emailul completat",
    successSpam: "Dacă nu vezi mesajul, uită-te și în „Spam” — vine de la o adresă nouă pentru tine.",
    submitError: "Trimiterea a eșuat. Verifică datele și încearcă din nou.",
    backToLogin: "Înapoi la autentificare",
    linkFromLogin: "Nu ai cont? Trimite o cerere",
    // Admin list
    adminTitle: "Cereri de cont",
    adminSubtitle: "Formularele trimise de clienți. Din ele se creează firmele.",
    adminEmpty: "Nicio cerere.",
    adminSearchPlaceholder: "Caută după firmă, CUI sau email...",
    adminLoadError: "Nu am putut încărca cererile.",
    colCompany: "Firma",
    colType: "Tip",
    colMarketRole: "Tip generator",
    colContact: "Contact",
    colWaste: "Deșeuri",
    colDate: "Trimisă",
    colStatus: "Stare",
    // Tabelul arată șapte coloane dintr-un formular cu douăzeci de rubrici. Restul — adresa
    // sediului, punctul de lucru, telefonul, autorizația, transportul și mai ales `notes`, rubrica
    // de text liber în care omul scrie ce nu încape în celelalte — nu se citeau de nicăieri, deși
    // exact cine creează firma din cerere are nevoie de ele.
    view: "Vezi cererea",
    viewTitle: "Cererea trimisă",
    viewEmptyValue: "—",
    viewSubmittedAt: "Trimisă la",
    viewHandledAt: "Rezolvată la",
    viewNoAnswers: "Nimic completat în această secțiune.",
    // Rândul devenea „Cont creat" și nu ducea nicăieri, deși firma stă în tabelul de deasupra.
    openCompany: "Vezi firma creată",
    openCompanyMissing: "Firma creată nu mai e în listă.",
    approve: "Creează contul",
    reject: "Respinge",
    rejectPrompt: "De ce respingi cererea?",
    rejectTitle: "Respinge cererea",
    rejectReasonLabel: "Motivul",
    rejectReasonPlaceholder: "Ex.: firma are deja cont · datele nu se verifică · duplicat",
    rejectReasonHint: "Rămâne în cerere, ca urmă de hârtie. Nu se trimite automat clientului.",
    // Aprobarea creează un tenant real, cu profil, punct de lucru și tot — iar aplicația n-are
    // ștergere de firmă. Ștergerea unei mișcări întreabă; crearea unei firme nu întreba nimic.
    confirmApproveTitle: "Creezi contul acestei firme?",
    confirmApprove:
      "Se creează firma cu profilul din cerere și punctul de lucru pe care l-a numit. Nu se invită încă niciun utilizator, iar firma nu se poate șterge după.",
    approved: "Firma a fost creată din cerere. Invită acum utilizatorul.",
    rejected: "Cerere respinsă.",
    actionError: "Acțiunea a eșuat. Încearcă din nou.",
    status: {
      NEW: "Nouă",
      APPROVED: "Cont creat",
      REJECTED: "Respinsă",
    },
  },

  companyProfile: {
    title: "Profilul firmei",
    subtitle:
      "Răspunsurile din formularul completat de client. Din ele se decide ce vede omul în aplicație — dacă nu completezi nimic, se oferă tot.",
    marketRoles: "Tip de generator",
    marketRolesHint:
      "Ce e firma pe piață pentru marfa pe care o vinde. Decide dacă depune Anexa 1 Ambalaje (Ordinul 794/2012). Evidența gestiunii deșeurilor se ține oricum, de oricine generează deșeu.",
    marketRolesTraderOnly:
      "Comerciant: nu introduce el ambalaj pe piață, deci nu depune Anexa 1 Ambalaje. Evidența gestiunii deșeurilor rămâne obligatorie.",
    operationCodes: "Operațiuni de valorificare / eliminare",
    operationCodesHint:
      "Ce se întâmplă cu deșeul. Doar acestea apar la mișcări, după operațiune.",
    recovery: "Valorificare (R)",
    disposal: "Eliminare (D)",
    selectAll: "Toate",
    clearAll: "Niciuna",
    wasteCodes: "Coduri de deșeu din autorizație",
    wasteCodesHint:
      "Ce generează sau ce transportă. Din 842 de coduri rămân la vedere doar acestea.",
    addWasteCode: "Adaugă cod",
    removeWasteCode: "Scoate codul",
    transport: "Transport (doar pentru colectori)",
    transportMeans: "Cu ce transportă",
    transportMeansPlaceholder: "ex. autoutilitară 3,5 t, container 20 mc",
    transportLicenseNumber: "Licență de transport mărfuri",
    transportLicenseExpiry: "Expiră licența de transport",
    empty: "Necompletat — se oferă tot.",
  },

  /**
   * Avertismentul de dinaintea generarii unui document, cand o cantitate inca nu a venit de la
   * destinatar. Cerut pe 24.08.2026, cu precizarea care conteaza: "doar unde impacteaza acea
   * miscare" — deci se numara doar liniile care chiar intra in documentul cerut.
   */
  awaitingWeighing: {
    title: "Ai cantități care încă nu au venit de la destinatar",
    body: "{count} din documentul pe care îl generezi așteaptă cântarul de la destinatar („se cântărește la descărcare”). Pe hârtie, cantitatea va lipsi, iar stocul nu se închide acolo.",
    andMore: "și încă {count}",
    hint: "Poți completa cifra din Mișcări → „Adaugă cantitatea”, pe rândurile marcate „De cântărit”. Sau generează acum, dacă documentul e o ciornă de lucru.",
    generateAnyway: "Generează oricum",
    cancel: "Renunț, completez întâi",
  },

  packaging: {
    searchPlaceholder: "Caută după cod, denumire, partener...",
    title: "Ambalaje",
    subtitle:
      "Tot ce ține de ambalaje, într-un loc. Mișcările pe coduri 15 01 xx sunt registrul; din ele se însumează cele două tabele ale Anexei 1 Ambalaje (Ordinul 794/2012), care se depune la agenția județeană de mediu până pe 25 februarie, pentru anul anterior. Totul în kilograme, cum cere art. 8 din ordin.",
    year: "Anul",
    download: "Descarcă Anexa 1 Ambalaje",
    downloadXls: "XLS — formatul de depunere",
    downloadPdf: "PDF — pentru dosar",
    downloadHint:
      "Art. 6 din Ordinul 794/2012 cere raportarea „în format electronic «.xls»”, cu cele două foi. PDF-ul e aceeași declarație, pentru dosarul de control.",
    downloadError: "Anexa 1 Ambalaje nu a putut fi generată.",
    saveError: "Cifra nu a putut fi salvată.",

    // --- cuprinsul paginii ---
    // Patru tabele mari unul sub altul plus grila de suprascriere: cea mai lungă pagină din
    // aplicație. Etichetele sunt scurte dinadins — bara stă pe un rând, și pe telefon. Titlurile
    // întregi, cu temeiul legal, rămân pe secțiuni.
    sections: "Pe pagină",
    navRegister: "Mișcări",
    navTable1: "Tabelul 1",
    navTable2: "Tabelul 2",
    navAnexa3: "Anexa 3",

    // --- registrul ---
    registerTitle: "Mișcări de ambalaje",
    registerHint:
      "Fiecare mișcare înregistrată pe un cod 15 01 xx. Astea sunt kilogramele din care se face declarația — cartonul înregistrat pe 20 01 01 nu intră aici, intră în evidența gestiunii deșeurilor.",
    inAnexa1: "În Anexa 1",
    inAnexa1Yes: "Da",
    inAnexa1No: "Nu — nu l-am pus noi pe piață",
    inAnexa1Legacy: "Din cod, neconfirmat",
    inAnexa1LegacyHint:
      "Mișcare de dinaintea bifei. Intră în declarație ca până acum; deschide-o și confirmă, ca să nu rămână o presupunere pe un formular depus.",
    inAnexa1NoHint:
      "Ambalajul l-a pus pe piață altcineva, deci nu intră în Anexa 1 Ambalaje. Rămâne în evidența gestiunii deșeurilor.",
    origin: "Proveniența",
    originOwnShort: "Deșeu propriu",
    originTakeoverShort: "Preluat de la terți",
    originTakeoverInTab:
      "Marfă preluată de la terți: apare aici fiindcă e ambalaj, dar NU intră în Anexa 1 — nu e deșeul tău. Raportul ei e Anexa 3 la Ordinul 794/2012, mai jos.",
    registerEmpty:
      "Nicio mișcare pe coduri 15 01 xx în anul ales. Dacă ai predat ambalaje, verifică sub ce cod le-ai înregistrat.",
    date: "Data",
    code: "Cod",
    kind: "Felul ambalajului",
    partner: "Partener",
    workPoint: "Punct de lucru",
    fromCode: "din cod",
    goToMovements: "Deschide în Mișcări",
    addMovement: "Adaugă mișcare",

    // --- semnale ---
    blockedTitle: "Nu intră în declarație",
    blockedMissingMaterial:
      "{count} fără materialul ambalajului. Codul nu îl decide singur: 15 01 04 acoperă și aluminiul, și oțelul; 15 01 02 și PET-ul, și navetele. Alege-l pe mișcare.",
    blockedMissingCategory:
      "{count} fără felul ambalajului (desfacere / primar / secundar și de transport) — fără el nu există coloană în tabelul 1.",
    awaitingWeighing: "{count} încă de cântărit — cantitatea lipsește din ambele tabele.",
    missingOperation: "{count} fără cod R/D — operatorul apare, operațiunea rămâne goală.",
    fix: "Completează",
    // Badge-ul „Completează" spune ce lipsește; acțiunea de pe rând duce chiar la mișcarea unde
    // se completează. Registrul de ambalaje era al treilea raport care numea vinovatul și se
    // oprea acolo.
    fixOnMovement: "Completează",

    // --- tabelul 1 ---
    table1Title: "Tabel 1. Ambalaje introduse pe piața națională",
    table1Hint:
      "Se însumează din mișcări: materialul dă rândul, felul ambalajului dă coloana. Coloana „Total (col. 3+5)” e o sumă. O celulă fără nicio mișcare rămâne goală — „gol” nu înseamnă „zero”, și pe un formular depus e altceva.",
    table1Override:
      "Dacă cifra reală de piață diferă de ce arată mișcările — tabelul e despre marfa vândută, nu despre deșeu — o poți scrie tu, pe materialul respectiv. Rândul scris de tine înlocuiește rândul calculat și e marcat ca atare.",
    overrideOpen: "Scrie cifre proprii",
    overrideClose: "Ascunde cifrele proprii",
    overriddenBadge: "scris de tine",
    computedBadge: "din mișcări",
    overrideClear: "Golește rândul ca să revii la cifra din mișcări.",
    // Grila are șaizeci și șase de celule și se salvează singură, un rând odată, la ieșirea din
    // celulă. Până pe 08.09.2026 se vedeau numai erorile: o salvare reușită nu spunea nimic, deci
    // cine completa tot tabelul n-avea de unde ști câte cifre au ajuns.
    overrideAutosaveHint:
      "Se salvează singur, un rând odată, când ieși din celulă. Coloana „Stare” spune ce s-a salvat și ce nu.",
    overrideStatus: "Stare",
    overrideDirty: "nesalvat",
    overrideSaving: "se salvează…",
    overrideSaved: "salvat",
    // Un singur șir pentru toate numerele: perechea de dinainte scria corect 1 și 2, dar „20
    // rânduri au cifre nesalvate", fără „de". Verbul stă la plural pentru orice n > 1 și e forma
    // în care `countOf` îl lasă oricum.
    overrideUnsavedRows: "{count} cu cifre nesalvate. Ieși din celulă ca să plece.",

    // --- tabelul 2 ---
    table2Title: "Tabelul 2. Deșeuri de ambalaje gestionate",
    table2Hint:
      "Se completează singur, din predările înregistrate pe coduri 15 01 xx — un rând pentru fiecare operator care a preluat, cum cere nota 1 a formularului.",

    material: "Material",
    colSales: "Ambalaje de desfacere fabricate/importate",
    colTotal: "Total (col. 3+5)",
    colPrimary: "Primare — total",
    colPrimaryReusable: "Primare — reutilizabile",
    colSecondary: "Secundare și de transport — total",
    colSecondaryReusable: "Secundare — reutilizabile",
    colHazardous: "Cu conținut periculos",
    totalPlastic: "Total plastic",
    totalMetal: "Total metal",
    total: "TOTAL",
    quantity: "Cantitatea (kg)",
    operator: "Operatorul care a preluat",
    operatorCui: "CUI",
    operation: "Operațiunea",
    noHandovers:
      "Nicio predare pe coduri 15 01 xx în anul ales. Dacă ai predat ambalaje, verifică sub ce cod le-ai înregistrat.",

    // --- Anexa 3: raportul anual al colectorilor, comercianților, reciclatorilor și valorificatorilor ---
    anexa3Title: "Anexa 3. Deșeuri de ambalaje preluate de la terți",
    anexa3Hint:
      "Celălalt capăt al lanțului față de Anexa 1: aceea raportează ce ai pus tu pe piață, asta raportează ce ai preluat de la alții și ce ai făcut cu marfa. Se însumează din mișcările de preluare pe coduri 15 01 xx. Termen: 25 februarie, ca și Anexa 1.",
    anexa3WorkPoint: "Punct de lucru",
    anexa3AllWorkPoints: "Toate punctele de lucru",
    anexa3WorkPointHint:
      "Art. 4 alin. (4) cere raportarea „pentru fiecare punct de lucru în parte”, iar alin. (3) o trimite la agenția din raza punctului de lucru — deci două puncte de lucru în două județe înseamnă două depuneri, la doi destinatari. Alege punctul de lucru înainte să descarci.",
    anexa3RoleMissing: "Nu știm care tabel ți se aplică",
    anexa3RoleMissingHint:
      "Ordinul 794/2012 art. 4 alin. (1) cere „tabelul 1 sau, după caz, tabelul 2” — tabelul 1 pentru colectori și comercianți, tabelul 2 pentru reciclatori și valorificatori. Care anume ține de calitatea firmei, pe care numai tu o știi. Până răspunzi, nu tipărim nimic: un formular depus ar afirma în locul tău o calitate juridică.",
    anexa3RoleMissingAction: "Completează calitatea în profilul firmei",
    anexa3Addressee: "Se depune la",
    // Cei doi destinatari din art. 4 alin. (3): toți depun la agenția din raza punctului de lucru,
    // comerciantul la agenția națională. Stăteau scriși în pagină, singurul text vizibil rămas
    // afară din fișierul ăsta după mutarea celor din `combobox.tsx`.
    //
    // ⚠️ Scrie ANMAP, deși Ordinul 794/2012 scrie „Agenţia Naţională pentru Protecţia Mediului" —
    // și e o decizie, nu o scăpare (G-5, verificat act cu act pe 11.09.2026). Redenumirea e făcută
    // de art. IV din Legea 26/2026 „în întreg cuprinsul OUG 92/2021", deci NU atinge Ordinul
    // 794/2012, care n-a fost modificat niciodată după 2015. Regula pe care am ales-o: unde citim
    // actul, numele actului; unde îi spunem clientului unde să se ducă, numele instituției de azi.
    // Aici e al doilea caz — rândul ăsta nu ajunge pe niciun formular, fiindcă modelul Anexei 3
    // n-are rubrică de destinatar; se citește doar pe ecran, de un om care va căuta „ANMAP".
    anexa3AddresseeAnmap: "ANMAP",
    anexa3AddresseeLocal: "agenția județeană pentru protecția mediului din raza punctului de lucru",
    anexa3ExitsTitle: "Anexa 3. Deșeuri de ambalaje predate — ieșiri",
    anexa3ExitsHint:
      "Ambalajele pe care le-ai predat în anul ales, pe material și pe firma care le-a preluat. Se însumează din predările de pe Generare, pe coduri 15 01 xx.",
    anexa3ExitsTableTitle: "Ieșiri — ce ai predat și cui",
    anexa3Table1Title: "Tabelul 1 — colectori și comercianți",
    anexa3Table2Title: "Tabelul 2 — reciclatori și valorificatori",
    anexa3IntakeTitle: "Cantitatea preluată",
    anexa3IntakeHint:
      "Un rând pentru fiecare material și fiecare proveniență, cum desenează formularul. Proveniența se răspunde o dată pe partener; „populație” se alege pe mișcare, fiindcă o persoană fizică nu e partener.",
    anexa3ColTotal: "Total (kg)",
    anexa3ColHazardous: "din care periculoase",
    anexa3ColOrigin: "Proveniența",
    anexa3OutTitle: "Ce a plecat mai departe",
    anexa3ColOut: "Cantitatea comercializată / trimisă la reciclare, valorificare sau export (kg)",
    anexa3ColOperator: "Operatorul economic",
    anexa3ColRecycled: "Cantitatea reciclată (kg)",
    anexa3ColOtherRecovery: "Cantitatea valorificată prin alte metode (kg)",
    anexa3ColMethods: "Metoda",
    anexa3RecyclingHint:
      "Împărțirea se citește din act, nu se ghicește: OUG 92/2021 anexa nr. 3 numește „Reciclarea/Recuperarea” exact trei operațiuni — R3, R4 și R5. Restul codurilor R sunt valorificare prin alte metode; R1 e arderea pentru energie, deci sigur nu reciclare. Codurile D nu intră în niciuna: eliminarea nu e valorificare.",
    anexa3Empty:
      "Nicio preluare de ambalaje în anul ales. Anexa 3 se completează din mișcări cu operațiunea „Preluare” pe coduri 15 01 xx.",
    anexa3UnclassifiedTitle: "Nu intră în tabel",
    anexa3MissingOrigin:
      "{count} fără proveniență. Răspunde o dată pe partener, în Parteneri — sau pe mișcare, dacă marfa vine de la populație.",
    anexa3MissingMaterialCount:
      "{count} fără materialul ambalajului. Codul nu îl decide singur.",
    anexa3MissingQuantity:
      "{count} încă de cântărit. Fără kilograme nu intră în niciun tabel — completează cantitatea când vine cifra de la cântar.",
    anexa3Download: "Descarcă Anexa 3",
    anexa3DownloadHint:
      "Același art. 6 ca la Anexa 1: „.xls” protejat pentru depunere, plus exemplarul pe hârtie. Se tipărește un singur tabel — cel care ți se aplică.",
    anexa3DownloadError: "Anexa 3 nu a putut fi generată.",
    anexa3PickWorkPoint:
      "Alege un punct de lucru ca să descarci. „Toate” e doar o privire de ansamblu pe ecran: art. 4 alin. (4) cere raportarea pentru fiecare punct de lucru în parte, iar un formular cu rubrica „Punct de lucru” goală nu se poate depune.",
  },

  /** Cele patru cuvinte pe care nota 2 a Anexei 3 le permite la „Proveniența". */
  packagingOrigin: {
    POPULATIE: "Populație",
    GENERATOR_PJ: "Generator persoană juridică",
    COLECTOR: "Colector",
    COMERCIANT: "Comerciant",
    label: "Proveniența deșeului de ambalaj",
    hintPartner:
      "Ce e partenerul ăsta față de ambalajele pe care ți le aduce. Nota 2 a Anexei 3 (Ordinul 794/2012) descrie sursa, nu transportul — deci se răspunde o dată aici, nu la fiecare transport.",
    hintMovement:
      "Doar dacă transportul ăsta vine de altundeva decât de obicei. Gol = se ia de pe partener. „Populație” se poate alege numai aici: o persoană fizică nu e partener.",
    none: "— nu s-a răspuns —",
    fromPartner: "de pe partener",
  },

  /** Calitatea din Ordinul 794/2012 art. 4 alin. (1), care decide care tabel al Anexei 3 se depune. */
  packagingOperatorRole: {
    label: "Calitatea pentru deșeuri de ambalaje",
    hint:
      "Decide care tabel al Anexei 3 se depune (Ordinul 794/2012, art. 4 alin. (1)): colectorii și comercianții completează tabelul 1, reciclatorii și valorificatorii tabelul 2. Se cere doar operatorilor care preiau deșeuri de ambalaje de la terți; un generator obișnuit o lasă goală.",
    none: "— nu se aplică / nu s-a răspuns —",
    COLECTOR: "Colector — tabelul 1",
    COMERCIANT: "Comerciant — tabelul 1, depus la ANMAP",
    RECICLATOR: "Reciclator — tabelul 2",
    VALORIFICATOR: "Valorificator — tabelul 2",
  },

  // Zona de atașamente. Erau scrise direct în componentă, singurele din aplicație în afara
  // fișierului ăstuia, alături de cele trei din combobox.
  notFound: {
    title: "Pagina asta nu există",
    body: "Adresa e greșită, sau pagina s-a mutat de când ai salvat linkul.",
    toDashboard: "Înapoi la Panou",
    toLogin: "Mergi la autentificare",
  },

  /**
   * Doar ce ține de **rama** celor două documente publice — cuprinsul, linkurile din subsol,
   * rândul de copyright. **Textul documentelor nu e aici**, ci în `lib/legal.ts`: aici stau
   * etichete care se schimbă odată cu ecranul, acolo stau documente care se schimbă odată cu
   * contractul și se anunță cu 30 de zile înainte.
   */
  legal: {
    contents: "Cuprins",
    termsLink: "Termeni și condiții",
    privacyLink: "Confidențialitate",
    contactEmail: "contact@wastehouse.ro",
    copyright: "© 2026 ONSIA S.R.L. · CUI 51779887",
    // Sub butonul de trimitere al cererii de cont. Nu e casetă de bifat: cererea nu deschide un
    // cont, iar acceptarea propriu-zisă se face la semnarea contractului (cap. 2 din termeni).
    // O bifă aici ar cere un consimțământ pentru ceva ce încă nu s-a întâmplat.
    accountRequestNotice: "Trimițând cererea, confirmi că ai citit {termeni} și {confidentialitate}.",
    // Sub butonul de salvare a parolei — adică la invitație, singurul moment în care un utilizator
    // invitat (nu cel care a cerut contul) trece printr-un ecran al nostru înainte să lucreze.
    // Termenii, cap. 6.4, spun exact asta; fără rândul ăsta ar spune-o degeaba.
    setPasswordNotice: "Salvând parola, confirmi că ai citit {termeni} și {confidentialitate}.",
    // Etichetele de **înăuntrul** propoziției. Nu se refolosesc cele din subsol („Termeni și
    // condiții"), fiindcă acolo sunt titluri de pagină, iar aici sunt complement direct.
    accountRequestTermsLabel: "termenii și condițiile",
    accountRequestPrivacyLabel: "politica de confidențialitate",
  },

  // P2.15 — importul de istoric. Mesajele de pe rânduri vin de la backend, cu numele coloanei în ele.
  importExcel: {
    title: "Import din Excel",
    subtitle:
      "Punctele de lucru, partenerii și mișcările de până acum, dintr-un singur fișier. Întâi se verifică, apoi se importă — totul sau nimic.",
    templateTitle: "1. Descarcă șablonul",
    templateHint:
      "Copiază datele în foile „Puncte de lucru”, „Parteneri” și „Mișcări”, fără să muți coloanele. Ambalajele și transportul (Anexa 3) au coloane opționale la capătul foii „Mișcări”. Foaia „Instrucțiuni” spune ce intră în fiecare.",
    templateButton: "Descarcă șablonul .xlsx",
    templateError: "Șablonul nu a putut fi descărcat.",
    fileTitle: "2. Verifică și importă fișierul completat",
    fileHint: "„Importă” se deblochează după o verificare fără nicio eroare, pe același fișier.",
    chooseFile: "Alege fișierul",
    noFile: "Niciun fișier ales",
    verify: "Verifică",
    import: "Importă",
    verifyError: "Fișierul nu a putut fi verificat.",
    importError: "Importul nu a reușit. Nu s-a salvat nimic.",
    resultTitle: "Ce s-a găsit în fișier",
    resultSaved: "Ce s-a salvat",
    workPointsNew: "Puncte de lucru noi",
    workPointsExisting: "Puncte de lucru deja în firmă",
    partnersNew: "Parteneri noi",
    partnersExisting: "Parteneri deja în firmă (nu se modifică)",
    movementsNew: "Mișcări noi",
    movementsExisting: "Mișcări deja importate din același fișier",
    noErrors: "Nicio eroare. Fișierul se poate importa.",
    errorsTitle: "{n} probleme. Nu se salvează nimic până nu sunt rezolvate toate.",
    sheet: "Foaia",
    row: "Rândul",
    problem: "Problema",
    saved: "Import salvat: {workPoints} puncte de lucru, {partners} parteneri și {movements} mișcări noi.",
    afterImport:
      "Ce n-a fost în fișier (ambalajele, transportul pentru Anexa 3, CNP-ul șoferului) se completează în aplicație, pe fiecare mișcare.",
    warningsTitle: "{n} rânduri sărite: sunt deja în firmă. Nu opresc importul.",
    historyTitle: "Importurile firmei",
    historyHint:
      "„Retrage” șterge mișcările importului pe care nu le-a modificat nimeni de atunci. Partenerii și punctele de lucru create rămân.",
    historyEmpty: "Niciun import salvat în firma asta.",
    historyDate: "Data",
    historyFile: "Fișierul",
    historyAdded: "Adăugate",
    historyAddedValue: "{movements} · {partners} · {workPoints}",
    historyNow: "Acum",
    historyNowValue: "{remaining}, {edited} modificate de atunci",
    historyUndone: "Retras {date}",
    historyError: "Istoricul nu s-a putut încărca.",
    undo: "Retrage",
    undoTitle: "Retragi importul?",
    undoMessage:
      "Importul „{file}” din {date}: se șterg {deletable}. Rămân {edited} modificate de atunci, partenerii și punctele de lucru.",
    undoConfirm: "Retrage importul",
    undone: "Import retras: {deleted} șterse, {kept} păstrate.",
    undoError: "Importul nu s-a putut retrage.",
    unnamedFile: "fără nume",
  },

  fileDropzone: {
    hint: "Trage fișiere aici sau apasă pentru a alege",
    limit: "Imagini, PDF, Word, Excel · cel mult {mb} MB per fișier",
    tooBig: "Prea mari, peste {mb} MB: {files}",
    remove: "Elimină fișierul",
  },

  errorBoundary: {
    title: "Ceva s-a stricat pe ecranul ăsta",
    body:
      "Nu s-a pierdut nimic din ce era salvat. Reîncarcă pagina; dacă se repetă, spune-ne pe ce ecran și ce ai apăsat.",
    details: "Detalii tehnice",
    reload: "Reîncarcă pagina",
    toDashboard: "Înapoi la Panou",
  },

  common: {
    /** Rubrica de parolă: „Arată” și bara de putere de la o parolă nouă. */
    password: {
      show: "Arată parola",
      hide: "Ascunde parola",
      strength0: "Încă nu respectă regula de mai jos",
      strength1: "Acceptată",
      strength2: "Bună",
      strength3: "Puternică",
    },
    /**
     * Nota de retenţie pentru actul de identitate al şoferilor — singurul dat personal pe care
     * aplicaţia îl ţine despre cineva care nu are cont în ea, şi singurul care se **tipăreşte**
     * (Anexa 3, rubrica „Date de identificare delegat"). Stă în ambele locuri unde se scrie: fişa
     * partenerului şi „Şoferii noştri" din Setări.
     *
     * Nu inventează o regulă de protecţie a datelor: spune scopul (rubrica de pe formular),
     * termenul (chiar cel de păstrare a evidenţei, OUG 92/2021 art. 48 alin. (5)) şi faptul —
     * deja adevărat în cod, decizia 30 — că mişcarea păstrează un instantaneu, deci dezactivarea
     * unui şofer nu-l scoate din documentele tipărite.
     */
    driversPrivacy:
      "Date personale: se țin doar pentru documentele de transport și se tipăresc pe ele — actul de identitate pe Anexa 3, CNP-ul pe avizul de însoțire. Rămân cât timp se păstrează evidența: cel puțin 3 ani (OUG 92/2021, art. 48 alin. (5)). După aceea, numele, actul de identitate și CNP-ul se șterg singure de pe mișcări, la începutul fiecărui an. Fișa unui șofer dezactivat se poate șterge definitiv oricând; mișcările își păstrează până atunci datele de pe documentele tipărite.",
    cnp: "CNP",
    cnpHint: "Se tipărește pe avizul de însoțire. Se verifică cifra de control, ca un CNP greșit să nu intre.",
    loading: "Se încarcă...",
    saving: "Se salvează...",
    save: "Salvează",
    cancel: "Anulează",
    delete: "Șterge",
    edit: "Editează",
    add: "Adaugă",
    close: "Închide",
    status: "Status",
    actions: "Acțiuni",
    requiredField: "Câmp obligatoriu.",
    // Citit doar de cititorul de ecran, în locul asteriscului. Vezi `Label`.
    requiredMarker: "obligatoriu",
    // `Stepper` — citit doar de cititorul de ecran, înaintea numelui pasului curent.
    stepOf: "Pasul {n} din {total}:",
    search: "Caută",
    // Citite doar de cititorul de ecran, pe cele două select-uri ale lui `MonthInput` — eticheta
    // vizibilă e una singură, a filtrului.
    month: "Luna",
    year: "Anul",
    wholeYear: "Tot anul",
    searchPlaceholder: "Caută în listă...",
    clearSearch: "Golește căutarea",
    noResults: "Niciun rezultat",

    /**
     * Textele lui `Combobox` — rubrica de la care pornește formularul de mișcare (codul de
     * deșeu) și cea care adaugă coduri pe profilul firmei.
     *
     * <p>Stăteau scrise în componentă, singurele rămase acolo după ce `file-dropzone.tsx` s-a
     * mutat pe 07.09.2026. Nu erau greșite — dar README-ul spune că toată interfața stă aici, iar
     * o excepție netrecută în listă e felul în care regula se pierde.
     *
     * <p>Nu se topesc peste `searchPlaceholder` / `noResults` de mai sus: alea sunt ale barei de
     * căutare dintr-un tabel („Caută în listă…", peste rândurile aduse), astea sunt ale unei liste
     * care se caută **la server**. Un singur șir pentru amândouă ar lega două ecrane care n-au de
     * ce să se miște împreună.
     */
    comboboxPlaceholder: "Selectează…",
    comboboxSearchPlaceholder: "Caută…",
    comboboxEmpty: "Niciun rezultat.",
    comboboxSearching: "Se caută…",
    comboboxClear: "Șterge selecția",
    noResultsHint: "Niciun rând nu se potrivește cu filtrele puse. Șterge-le ca să vezi tot.",
    moreActions: "Mai multe acțiuni",
    // Filtrul activ / inactiv. Numărul celor inactive stă chiar în opțiune: altfel „Inactive" e o
    // opțiune despre care nu știi dacă are ceva în ea până n-o alegi.
    stateFilter: "Starea rândurilor",
    stateActive: "Active",
    stateInactive: "Inactive ({n})",
    stateAll: "Toate",
    reactivate: "Reactivează",
    reactivated: "Reactivat.",
    // Paginare
    previous: "Înapoi",
    next: "Înainte",
    // „12–24 din 340"
    rangeOfTotal: "{from}–{to} din {total}",
    sortedAsc: "sortat crescător",
    sortedDesc: "sortat descrescător",
    // Navigație
    skipToContent: "Sari la conținut",
    mainNav: "Navigație principală",
    openNav: "Deschide meniul de navigație",
    closeNav: "Închide meniul de navigație",
    userMenu: "Meniul contului",
    // Erori de formular
    fixErrors: "Verifică rubricile marcate mai jos.",
    // Garda de la închiderea unui formular început. Titlul întreabă, mesajul spune ce se pierde,
    // iar butonul numește fapta — „Da/Nu" pe un dialog citit în grabă nu spune care e care.
    discardTitle: "Închizi fără să salvezi?",
    discardMessage: "Ce ai completat până acum se pierde. Nimic nu se înregistrează.",
    discardConfirm: "Închide, fără să salvez",
    // Scurtături
    commandPalette: "Caută sau sari la",
    commandPalettePlaceholder: "Sari la un ecran... (Ctrl+K)",
    goTo: "Navigare",
    // Al doilea grup al paletei: ce se **începe**, nu unde se ajunge. Cuvintele-cheie poartă
    // sinonimele pe care le tastează cineva grăbit — „predare" pentru o mișcare, „client" sau
    // „furnizor" pentru un partener.
    actionsGroup: "Acțiuni",
    actionNewMovement: "Adaugă deșeuri",
    actionNewMovementKeywords: "predare generare mișcare nouă înregistrează transport adaugă mișcare",
    actionNewInbound: "Intrare de deșeuri",
    actionNewInboundKeywords: "intrare primit preluat cântar depozit nouă",
    actionNewOutbound: "Ieșire de deșeuri",
    actionNewOutboundKeywords: "ieșire plecat predat valorificat eliminat nouă",
    actionNewPartner: "Adaugă partener",
    actionNewPartnerKeywords: "client furnizor colector transportator firmă nouă",
  },
} as const;
