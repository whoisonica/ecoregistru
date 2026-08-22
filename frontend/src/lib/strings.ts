/**
 * Central Romanian UI strings. Keep all user-facing text here so it can be
 * extracted into an i18n framework later without hunting through components.
 */
export const strings = {
  appName: "EcoRegistru",
  tagline: "Evidența și raportarea gestiunii deșeurilor",

  header: {
    // Shown above the nav: the current company. For PLATFORM_ADMIN it's a tenant switcher.
    currentCompany: "Firma curentă",
    selectCompany: "Alege firma",
    noCompanySelected: "Nicio firmă selectată",
    platformAdmin: "Administrator platformă",
    loadCompaniesError: "Nu am putut încărca firmele.",
  },

  nav: {
    dashboard: "Panou",
    movements: "Mișcări",
    evidences: "Evidențe",
    partners: "Parteneri",
    deadlines: "Termene",
    auditFile: "Dosar de control",
    clients: "Clienți",
    settings: "Setări",
    logout: "Deconectare",
  },

  login: {
    title: "Autentificare",
    email: "Email",
    password: "Parolă",
    submit: "Intră în cont",
    loading: "Se autentifică...",
    forgotPassword: "Ai uitat parola?",
    genericError: "Autentificare eșuată. Verifică datele și încearcă din nou.",
  },

  dashboard: {
    title: "Panou de control",
    welcome: "Bine ai venit",
    addMovement: "Adaugă mișcare",
    // stat tiles
    statMovements: "Mișcări luna aceasta",
    statMovementsSub: "înregistrări în {month}",
    statDeadlines: "Termene de făcut",
    statDeadlinesSub: "din care {overdue} depășite",
    statExpiring: "Autorizații care expiră",
    statExpiringSub: "parteneri cu autorizația aproape expirată",
    // sections
    upcomingTitle: "Termene următoare",
    upcomingEmpty: "Niciun termen deschis pentru anul curent.",
    expiringTitle: "Autorizații care expiră curând",
    expiringEmpty: "Nicio autorizație de partener aproape de expirare.",
    viewAll: "Vezi toate",
    loadError: "Nu am putut încărca datele panoului.",
  },

  movements: {
    title: "Mișcări de deșeuri",
    subtitle: "Intrările și ieșirile de deșeuri, pe puncte de lucru și luni.",
    add: "Adaugă mișcare",
    addTitle: "Adaugă mișcare",
    editTitle: "Editează mișcarea",
    empty: "Nicio mișcare pentru filtrele alese.",
    loadError: "Nu am putut încărca mișcările.",
    // filters
    filterMonth: "Luna",
    filterWorkPoint: "Punct de lucru",
    filterWasteCode: "Cod deșeu",
    filterAll: "Toate",
    allMonths: "Toate lunile",
    clearFilters: "Șterge filtrele",
    // columns
    colDate: "Data",
    colWasteCode: "Cod deșeu",
    colOperation: "Operațiune",
    colQuantity: "Cantitate",
    colPartner: "Partener",
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
    operation: "Operațiune",
    physicalState: "Stare fizică",
    physicalStatePlaceholder: "— fără —",
    operationCode: "Cod operațiune (R/D)",
    handoverCodeHint:
      "Operațiunea pe care o face partenerul. Fișa Anexa 1 nu are coloană de „predare”: cantitatea se raportează la „valorificată” (cod R) sau la „eliminată final” (cod D), iar capitolele 3 și 4 cer operațiunea și operatorul.",
    partner: "Partener",
    partnerPlaceholder: "Alege un partener",
    documentReference: "Referință document",
    documentReferencePlaceholder: "ex. aviz / factură",
    notes: "Note",
    attachments: "Atașamente",
    hazardous: "Periculos",
    // validation / feedback
    partnerRequired: "Partenerul este obligatoriu la predare.",
    recoveryCodeRequired: "Alege un cod de valorificare (R) pentru valorificare.",
    disposalCodeRequired: "Alege un cod de eliminare (D) pentru eliminare.",
    handoverCodeRequired:
      "Alege operațiunea pe care o face partenerul: un cod R (valorificare) sau D (eliminare).",
    created: "Mișcare adăugată.",
    updated: "Mișcare actualizată.",
    deleted: "Mișcare ștearsă.",
    saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
    confirmDelete: "Sigur ștergi această mișcare?",
    attachmentError: "Fișierul nu a putut fi încărcat.",
    attachmentDeleted: "Atașament șters.",
    noWorkPointHint: "Adaugă întâi un punct de lucru din Setări.",
  },

  settings: {
    title: "Setări",
    workPoints: {
      title: "Puncte de lucru",
      subtitle: "Locațiile pentru care ții evidența deșeurilor.",
      add: "Adaugă punct de lucru",
      edit: "Editează punctul de lucru",
      name: "Denumire",
      address: "Adresă",
      empty: "Niciun punct de lucru încă.",
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivate: "Sigur dezactivezi acest punct de lucru?",
      created: "Punct de lucru adăugat.",
      updated: "Punct de lucru actualizat.",
      deactivated: "Punct de lucru dezactivat.",
      saveError: "Salvarea a eșuat. Încearcă din nou.",
      loadError: "Nu am putut încărca punctele de lucru.",
    },
  },

  partners: {
    title: "Parteneri",
    subtitle: "Colectorii și transportatorii cărora le predai deșeuri.",
    add: "Adaugă partener",
    addTitle: "Adaugă partener",
    editTitle: "Editează partenerul",
    empty: "Niciun partener încă.",
    loadError: "Nu am putut încărca partenerii.",
    // columns
    name: "Denumire",
    cui: "CUI",
    type: "Tip",
    authorizationNumber: "Nr. autorizație",
    authorizationExpiry: "Expirare autorizație",
    // form placeholders
    cuiPlaceholder: "ex. RO12345678",
    authorizationNumberPlaceholder: "ex. 123/2024",
    typePlaceholder: "Alege tipul",
    // status / badges
    active: "Activ",
    inactive: "Inactiv",
    noAuthorization: "—",
    expired: "Expirată",
    expiringSoon: "Expiră curând",
    deactivate: "Dezactivează",
    confirmDeactivate: "Sigur dezactivezi acest partener? Acțiunea nu poate fi anulată.",
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
    regenerated: "Evidență regenerată: {count} linii pentru {year}.",
    // export
    export: "Export",
    exportExcel: "Export Excel",
    exportPdf: "Export PDF",
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
    incomplete: "Incomplet",
    incompleteHint:
      "Cantitate ieșită fără cod de operațiune (R/D). Se scade din stoc, dar nu poate fi raportată în „Valorificat” sau „Eliminat” până nu completezi codul pe mișcare.",
    resaleSuspected: "De verificat",
    resaleSuspectedHint:
      "La acest punct de lucru și cod există și deșeu preluat de la terți. Dacă predarea dă mai departe marfă preluată, ea nu aparține Anexei 1, ci registrului cronologic (art. 48).",
    regeneratedCascade: "Evidență regenerată: {count} linii pentru {year} (și anii {years}).",
    // empty state
    empty: "Nu există linii de evidență pentru {year}.",
    emptyHint: "Apasă „Regenerează” pentru a calcula evidența anului {year} din mișcări.",
    // note about the cache being manually regenerated
    staleNote:
      "Evidența nu se actualizează singură. După ce adaugi sau ștergi mișcări, apasă „Regenerează”.",
  },

  deadlines: {
    title: "Termene de raportare",
    subtitle:
      "Calendarul obligațiilor de raportare pe firmă: SIM anual și, pentru firmele cu obligație, AFM lunar.",
    generate: "Generează termenele",
    generating: "Se generează...",
    generated: "Termene generate: {count} noi pentru {year}.",
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
    notePlaceholder: "ex. depus la ANPM pe 12.03, nr. înregistrare 1234",
    // empty state
    empty: "Niciun termen pentru {year}.",
    emptyHint: "Apasă „Generează termenele” pentru a crea calendarul anului {year}.",
  },

  auditFile: {
    title: "Dosar de control",
    subtitle:
      "Descarcă într-o singură arhivă tot ce ai nevoie la un control: evidența, autorizațiile partenerilor și documentele atașate.",
    filterYear: "An",
    download: "Descarcă dosarul (.zip)",
    downloading: "Se pregătește arhiva...",
    downloadError: "Descărcarea dosarului a eșuat. Încearcă din nou.",
    contents: "Arhiva conține:",
    contentEvidence: "Evidența gestiunii deșeurilor (Excel + PDF)",
    contentPartners: "Rezumat PDF cu autorizațiile partenerilor și statusul lor",
    contentAttachments: "Documentele justificative atașate mișcărilor (+ index)",
    note: "Notă: dosarul NU înlocuiește formularele oficiale (Anexa 1 / SIM / AFM); e un pachet de lucru pentru pregătirea controlului.",
  },

  clients: {
    title: "Clienți",
    subtitle:
      "Firmele pentru care ții evidența. Creezi firme și inviți utilizatori care primesc pe email un link de setare a parolei.",
    add: "Adaugă firmă",
    addTitle: "Adaugă firmă",
    editTitle: "Editează firma",
    empty: "Nicio firmă încă.",
    loadError: "Nu am putut încărca firmele.",
    onlyPlatformAdmin: "Această secțiune este disponibilă doar administratorului platformei.",
    // columns
    name: "Denumire",
    cui: "CUI",
    type: "Tip",
    afm: "Obligație AFM",
    // form fields
    namePlaceholder: "ex. Demo Reciclare SRL",
    cuiPlaceholder: "ex. RO12345678",
    afmLabel: "Firma are obligație de declarație AFM lunară",
    environmentalAuthNumber: "Nr. autorizație de mediu",
    environmentalAuthExpiry: "Expirare autorizație de mediu",
    address: "Adresă",
    contactName: "Persoană de contact",
    contactEmail: "Email de contact",
    contactPhone: "Telefon de contact",
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
    wasteOperation: {
      GENERATED: "Generare",
      COLLECTED: "Colectare",
      HANDED_OVER: "Predare",
      RECOVERED: "Valorificare",
      DISPOSED: "Eliminare",
    },
    wasteRegister: {
      ANEXA_1: "Anexa 1 — deșeu propriu",
      ART_48: "Registru cronologic — preluat de la terți",
    },
    // HG 856/2002, anexa nr. 1, cap. 2, nota 3, verbatim.
    treatmentPurpose: {
      V: "V — pentru valorificare",
      E: "E — în vederea eliminării",
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
    partnerType: {
      COLLECTOR: "Colector",
      CARRIER: "Transportator",
      BOTH: "Colector și transportator",
    },
    companyType: {
      GENERATOR: "Generator",
      COLLECTOR: "Colector",
      BOTH: "Generator și colector",
    },
    inviteRole: {
      ADMIN: "Administrator",
      OPERATOR: "Operator",
      CLIENT_VIEWER: "Vizualizare (read-only)",
    },
    reportType: {
      SIM_ANNUAL: "SIM (anual) — ANPM",
      AFM_MONTHLY: "AFM (lunar) — Fondul pentru Mediu",
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

  common: {
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
  },
} as const;
