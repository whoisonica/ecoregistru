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
      "Dacă ai fost invitat în EcoRegistru, parola pe care o pui acum îți activează contul.",
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
    volumeM3Hint: "Singura măsură pe care o ai fără cântar. Nu ține loc de kilograme în Anexa 1.",
    operation: "Operațiune",
    physicalState: "Stare fizică",
    physicalStatePlaceholder: "— fără —",
    storageAndTreatment: "Stocare, tratare și transport (cap. 2)",
    transportMeans: "Transport — mijlocul",
    wasteDestination: "Transport — destinația",
    storageType: "Stocare — tipul",
    treatmentMethod: "Tratare — ce se face",
    nomenclatorPlaceholder: "— fără —",
    operationCode: "Cod operațiune (R/D)",
    operationCodeHint:
      "Fișa Anexa 1 nu are coloană de „predare”: cantitatea se raportează la „valorificată” (cod R) sau la „eliminată final” (cod D). Predarea se înregistrează alegând operațiunea și partenerul care o face.",
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
    anexa3Section: "Anexa 3 — dovada predării",
    anexa3SectionHint:
      "Formularul de încărcare-descărcare deșeuri nepericuloase (HG 1061/2008), tipărit din această mișcare. Completează ce apare pe hârtie.",
    anexa3Download: "Anexa 3",
    anexa3Downloading: "Se generează...",
    anexa3Error: "Formularul nu a putut fi generat.",
    unloadDate: "Data descărcării",
    transportPartner: "Transportator",
    transportPartnerPlaceholder: "— transportăm noi —",
    driverName: "Delegat (șofer)",
    driverIdentification: "Act de identitate",
    driverIdentificationPlaceholder: "ex. serie și nr. CI",
    vehicleRegistration: "Nr. înmatriculare",
    transportDestinations: "Destinat:",
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
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivate: "Sigur dezactivezi acest generator intern?",
      created: "Generator intern adăugat.",
      updated: "Generator intern actualizat.",
      deactivated: "Generator intern dezactivat.",
      saveError: "Salvarea a eșuat. Încearcă din nou.",
      loadError: "Nu am putut încărca generatorii interni.",
      noWorkPoints: "Adaugă întâi un punct de lucru.",
    },
  },

  partners: {
    title: "Parteneri",
    subtitle:
      "Firmele cu care lucrezi: clienții (le predai deșeu și le facturezi) și furnizorii (îți prestează serviciul și îți facturează).",
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
    role: "Rol comercial",
    filterRole: "Rol",
    filterRoleAll: "Toate rolurile",
    roleRequired: "Alege cel puțin un rol: client, furnizor sau ambele.",
    address: "Adresa sediului social",
    workPointAddress: "Adresa punctului de lucru",
    workPointAddressHint:
      "Unde se descarcă efectiv deșeul, dacă e altundeva decât sediul. Asta se tipărește pe Anexa 3.",
    tradeRegisterNumber: "Nr. Registrul Comerțului",
    tradeRegisterNumberPlaceholder: "ex. J12/1351/2011",
    transportLicenseNumber: "Licență de transport mărfuri",
    transportLicenseExpiry: "Expiră licența",
    anexa3Hint:
      "Datele astea se tipăresc pe Anexa 3 când îi predai deșeu sau când el face transportul.",
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
    anexa1: "Fișa Anexa 1",
    anexa1Hint:
      "Formularul oficial (HG 856/2002): antet + cele 4 capitole, o pagină per cod de deșeu.",
    anexa1Error: "Fișa nu a putut fi generată.",
    annualDeclaration: "Declarația anuală",
    annualDeclarationHint:
      "Centralizatorul anual: un rând per cod de deșeu — stoc inițial, generat, valorificat, eliminat, stoc final și prin cine. O pagină per punct de lucru.",
    annualDeclarationError: "Declarația anuală nu a putut fi generată.",
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
    // Cele două vederi ale tabului. Implicit e registrul de predări, cerut la meeting; vederea
    // lunară rămâne fiindcă poartă stocul cumulativ, singura cifră pe care ochiul n-o reface și
    // exact ce cere fișa Anexa 1.
    viewHandovers: "Predări",
    viewMonthly: "Anexa 1 — lunar",
    handoversSubtitle:
      "Ce a plecat de pe amplasament: cantitatea, data predării, cine a primit și sub ce cod. Din rândul de aici tipărești Anexa 3.",
    colHandoverDate: "Data predării",
    colOperationCode: "Operațiune",
    colPartnerName: "Partener",
    emptyHandovers: "Nicio predare pentru filtrele alese.",
    handoversLoadError: "Nu am putut încărca predările.",
    ownSite: "pe amplasament propriu",
    // Roșu, nu galben: o ieșire fără cod R/D nu e o rubrică de completat cândva, e o cantitate
    // care lipsește din Anexa 1. Se cere la orice ieșire nouă, deci rândurile astea sunt vechi.
    missingCode: "Fără cod R/D",
    missingCodeHint:
      "Cantitate ieșită fără cod de operațiune (R/D). Se scade din stoc, dar nu poate fi raportată în „Valorificat” sau „Eliminat” până nu completezi codul pe mișcare.",
    awaitingWeighing: "De cântărit",
    awaitingWeighingHint:
      "O ieșire din luna asta așteaptă cântarul destinatarului, deci totalurile sunt provizorii.",
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
    // Perioada acoperită. Trei ani e termenul de păstrare din OUG 92/2021 art. 48 alin. (5) —
    // exact cât poate cere un control —, dar implicit rămâne un an: cel mai des se descarcă
    // pentru anul care se depune.
    filterYears: "Perioada",
    yearsOne: "Doar anul ales",
    yearsTwo: "Ultimii 2 ani",
    yearsThree: "Ultimii 3 ani (cât cere un control)",
    yearsHint:
      "Evidența se păstrează cel puțin 3 ani (OUG 92/2021, art. 48). La control se poate cere toată perioada, nu doar anul curent.",
    download: "Descarcă dosarul (.zip)",
    downloading: "Se pregătește arhiva...",
    downloadError: "Descărcarea dosarului a eșuat. Încearcă din nou.",
    contents: "Arhiva conține:",
    contentAnexa1:
      "Evidența gestiunii deșeurilor generate — fișa Anexa 1 (HG 856/2002): cele 4 capitole, o pagină per cod de deșeu. Termen de depunere: 15 martie.",
    contentAnnualDeclaration:
      "Declarația anuală (centralizatorul): un rând per cod de deșeu — stoc inițial, generat, valorificat, eliminat, stoc final și prin cine. O pagină per punct de lucru.",
    contentEvidence: "Același an ca tabel de lucru (Excel + PDF)",
    contentPartners: "Rezumat PDF cu autorizațiile partenerilor și statusul lor",
    contentAttachments: "Documentele justificative atașate mișcărilor (+ index)",
    note: "Notă: în afară de fișa Anexa 1, dosarul NU înlocuiește formularele oficiale (SIM / AFM); e un pachet de lucru pentru pregătirea controlului.",
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
    contactRoleHint: "Se tipărește la „Întocmit / Funcția” pe declarația anuală.",
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
      ANEXA_1: "Anexa 1 — deșeu propriu",
      ART_48: "Registru cronologic — preluat de la terți",
    },
    // HG 856/2002, anexa nr. 1, cap. 2, nota 3. Nota definește și „E — în vederea eliminării”,
    // dar niciun formular completat pe care îl avem nu îl scrie: pe fișele de eliminare coloana
    // rămâne goală, iar ce identifică eliminarea e codul D din cap. 4.
    treatmentPurpose: {
      V: "V — pentru valorificare",
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
    partnerType: {
      GENERATOR: "Generator",
      COLLECTOR: "Colector",
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
      // Ce se depune pe 15 martie e chiar evidența — fișa Anexa 1 din HG 856/2002 —, încărcată
      // în sistemul pus la dispoziție de APM (OUG 92/2021 art. 48 alin. (1)). „Raportarea SIM"
      // numea canalul și lăsa clientul să ghicească ce are de pregătit.
      SIM_ANNUAL: "Anexa 1 — evidența gestiunii deșeurilor generate (anual, 15 martie)",
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

  accountRequest: {
    // Formularul are șase secțiuni, deci se salvează singur în browser cât îl completezi. Restaurarea
    // se anunță, nu se face pe furiș: altfel omul nu știe de ce vede date pe care nu le-a tastat acum.
    draftRestored:
      "Am păstrat ce completaseși și am pus la loc în formular. Verifică datele înainte de trimitere.",
    draftDiscard: "Șterge și începe de la zero",
    // Public page
    title: "Cerere de cont EcoRegistru",
    subtitle:
      "Conturile se creează de echipa EcoRegistru, pe baza acestui formular. Completează-l o dată — din răspunsuri configurăm aplicația pentru tipul tău de activitate, ca să vezi doar ce îți trebuie.",
    sectionCompany: "Firma",
    sectionWorkPoint: "Punctul de lucru",
    sectionContact: "Persoana de contact",
    sectionAuthorization: "Autorizația de mediu",
    sectionTransport: "Transport",
    sectionWaste: "Deșeurile",
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
      "Apare în antetul declarației anuale de deșeuri. Dacă nu ești sigur care e, lasă gol — rubrica rămâne necompletată și o stabilim împreună.",
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
    recovery: "Valorificare (R)",
    disposal: "Eliminare (D)",
    wasteCodesText: "Ce deșeuri generați / preluați",
    wasteCodesTextPlaceholder: "ex. carton, folie de plastic, deșeu menajer, moloz",
    wasteCodesTextHint:
      "Scrie-le în cuvintele tale. Le transformăm noi în codurile din Lista Europeană a Deșeurilor.",
    notes: "Alte observații",
    submit: "Trimite cererea",
    submitting: "Se trimite...",
    successTitle: "Cererea a fost trimisă",
    successBody:
      "Echipa EcoRegistru o verifică și îți creează contul. Primești datele de acces pe emailul completat.",
    submitError: "Trimiterea a eșuat. Verifică datele și încearcă din nou.",
    backToLogin: "Înapoi la autentificare",
    linkFromLogin: "Nu ai cont? Trimite o cerere",
    // Admin list
    adminTitle: "Cereri de cont",
    adminSubtitle: "Formularele trimise de clienți. Din ele se creează firmele.",
    adminEmpty: "Nicio cerere.",
    adminLoadError: "Nu am putut încărca cererile.",
    colCompany: "Firma",
    colType: "Tip",
    colMarketRole: "Tip generator",
    colContact: "Contact",
    colWaste: "Deșeuri",
    colDate: "Trimisă",
    colStatus: "Stare",
    approve: "Creează contul",
    reject: "Respinge",
    rejectPrompt: "De ce respingi cererea?",
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
      "Ce e firma pe piață pentru marfa pe care o vinde. Decide dacă depune declarația de ambalaje (Ordinul 794/2012). Fișa Anexa 1 se ține oricum, de oricine generează deșeu.",
    marketRolesTraderOnly:
      "Comerciant: nu introduce el ambalaj pe piață, deci nu depune declarația de ambalaje. Fișa Anexa 1 rămâne obligatorie.",
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
