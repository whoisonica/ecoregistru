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
    packaging: "Ambalaje",
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
    volumeM3Hint: "Singura măsură pe care o ai fără cântar. Nu ține loc de kilograme în evidență.",
    operation: "Operațiune",
    physicalState: "Stare fizică",
    physicalStatePlaceholder: "— fără —",
    storageAndTreatment: "Stocare, tratare și transport (cap. 2)",
    transportMeans: "Transport — mijlocul",
    wasteDestination: "Transport — destinația",
    storageType: "Stocare — tipul",
    treatmentMethod: "Tratare — ce se face",
    nomenclatorPlaceholder: "— fără —",
    operationGeneratorHint:
      "Mișcarea pornește de la generare. Ce se întâmplă cu deșeul după — pleacă spre valorificare sau spre eliminare — se alege mai jos, după transport.",
    // --- Ce se întâmplă cu deşeul: sub transport, nu în capul formularului ---
    fateTitle: "Ce se întâmplă cu deșeul",
    fateHint:
      "Se alege după transport, fiindcă de el atârnă: transportul spre valorificare cere un cod R, cel spre eliminare un cod D.",
    fateStock: "Rămâne în stoc",
    fateStockEffect:
      "Cantitatea intră la „Generate” și rămâne pe amplasament. Nu se predă nimic, deci nu există Anexa 3.",
    fateRecovery: "Transport spre valorificare",
    fateRecoveryEffect:
      "Cantitatea se raportează la „Valorificată”, cu un cod R și cu operatorul care o face (cap. 3).",
    fateDisposal: "Transport spre eliminare",
    fateDisposalEffect:
      "Cantitatea se raportează la „Eliminată final”, cu un cod D și cu operatorul care o face (cap. 4).",
    operationCode: "Cod operațiune (R/D)",
    operationCodeHint:
      "Evidența gestiunii deșeurilor nu are coloană de „predare”: cantitatea se raportează la „valorificată” (cod R) sau la „eliminată final” (cod D). Predarea se înregistrează alegând operațiunea și partenerul care o face.",
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

    // --- Provenienţa deşeului la ieşire ---
    // Se arată doar la conturile care pot prelua de la terţi. Fiecare opţiune îşi spune efectul,
    // fiindcă alegerea nu schimbă un câmp, ci pe ce formular oficial ajunge cantitatea.
    originTitle: "Proveniența deșeului",
    originHint:
      "De răspunsul ăsta atârnă pe ce formular ajunge cantitatea. Nu se poate deduce din operațiune: și deșeul tău, și marfa preluată se valorifică cu același cod R.",
    originOwn: "Generat în activitatea proprie",
    originOwnEffect:
      "Intră în Evidența gestiunii deșeurilor generate și, dacă e cod 15 01 xx, în Anexa 1 Ambalaje — tabelul 1 ca ambalaj pus de tine pe piață, tabelul 2 dacă l-ai predat cuiva.",
    originTakeover: "Preluat de la terți",
    originTakeoverEffect:
      "Intră în registrul cronologic art. 48 și în raportarea colectorilor (Anexa 3 la Ordinul 794/2012, încă neconstruită). NU intră în Anexa 1 și nici în evidența gestiunii — nu e deșeul tău.",
    originRequired:
      "Alege proveniența: fără ea, cantitatea ar intra automat în Anexa 1 ca deșeu propriu.",
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
    recordWeightTitle: "Cantitatea cântărită la descărcare",
    recordWeightHint:
      "Cifra pe care ți-a trimis-o destinatarul după cântărire. Până o completezi, luna rămâne provizorie în evidență și rubrica de pe Anexa 3 iese goală.",
    recordWeightSaved: "Cantitatea a fost înregistrată.",
    recordWeightError: "Cantitatea nu a putut fi înregistrată.",
    loadDate: "Data încărcării",
    loadDateHint: "Este data mișcării, de mai sus — pe formular sunt același lucru.",
    anexa3Unit: "Unitatea tipărită",
    anexa3UnitCompany: "Ca la firmă",
    anexa3UnitHint:
      "Actul are „tone”, dar evidența se ține în kilograme. Cifra se convertește la tipărire, deci cifra și unitatea de pe hârtie sunt mereu de acord.",
    // Cele trei exemplare nu se mai numesc *pe hârtie* (02.09.2026): modelul n-are așa ceva, iar
    // pe hârtie sunt un carnet cu indigo — aceeași filă de trei ori, sortată după semnare. Cine ce
    // exemplar ia rămâne scris aici, unde nu costă nimic.
    anexa3Copies:
      "Formularul iese în 3 exemplare identice, ca pe carnet: unul rămâne la tine (expeditor), unul la destinatar, unul la transportator.",
    anexa3Download: "Anexa 3",
    anexa3Downloading: "Se generează...",
    anexa3Error: "Formularul nu a putut fi generat.",
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
    driverIdentificationPlaceholder: "ex. serie și nr. CI",
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
      identificationHint:
        "Ce se scrie pe formular la „Date de identificare delegat”: serie și număr de CI, sau CNP. Rămâne editabil pe fiecare mișcare.",
      vehicle: "Nr. înmatriculare uzual",
      vehiclePlaceholder: "ex. CJ 01 ABC",
      vehicleHint: "Mașina cu care vine de obicei. Pe mișcare se poate schimba.",
      empty: "Niciun șofer încă.",
      active: "Activ",
      inactive: "Inactiv",
      deactivate: "Dezactivează",
      confirmDeactivate: "Sigur dezactivezi acest șofer? Mișcările deja înregistrate nu se schimbă.",
      loadError: "Nu am putut încărca șoferii.",
      created: "Șofer adăugat.",
      updated: "Șofer actualizat.",
      deactivated: "Șofer dezactivat.",
      saveError: "Salvarea a eșuat. Verifică datele și încearcă din nou.",
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
    typeNoneHint:
      "Pentru o firmă care doar transportă: nu face nimic cu deșeul, deci n-are tip. Se poate alege numai cu bifa de mai sus.",
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
    // Numele scurt "Anexa 1" a fost cedat declarației de ambalaje (Ordinul 794/2012) pe
    // 24.08.2026, la cererea specialistei: așa îi zice clientul. Documentul de aici își
    // poartă de-acum numele întreg, care e și titlul tipărit pe el.
    anexa1: "Evidența gestiunii deșeurilor generate",
    anexa1Hint:
      "Formularul oficial (HG 856/2002, anexa 1): antet + cele 4 capitole, o pagină per cod de deșeu.",
    anexa1Error: "Evidența gestiunii deșeurilor nu a putut fi generată.",
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
    // exact ce cere evidența gestiunii deșeurilor.
    viewHandovers: "Predări",
    viewMonthly: "Evidența lunară",
    handoversSubtitle:
      "Ce a plecat de pe amplasament: cantitatea, data predării, cine a primit și sub ce cod. Din rândul de aici tipărești Anexa 3.",
    colHandoverDate: "Data predării",
    colOperationCode: "Operațiune",
    colPartnerName: "Partener",
    emptyHandovers: "Nicio predare pentru filtrele alese.",
    handoversLoadError: "Nu am putut încărca predările.",
    ownSite: "pe amplasament propriu",
    // Roșu, nu galben: o ieșire fără cod R/D nu e o rubrică de completat cândva, e o cantitate
    // care lipsește din evidență. Se cere la orice ieșire nouă, deci rândurile astea sunt vechi.
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
    yearsFour: "Ultimii 4 ani",
    yearsFive: "Ultimii 5 ani (cu marjă peste termenul legal)",
    yearsHint:
      "Evidența se păstrează cel puțin 3 ani (OUG 92/2021, art. 48). La control se poate cere toată perioada, nu doar anul curent.",
    download: "Descarcă dosarul (.zip)",
    downloading: "Se pregătește arhiva...",
    downloadError: "Descărcarea dosarului a eșuat. Încearcă din nou.",
    contents: "Arhiva conține:",
    contentAnexa1:
      "Evidența gestiunii deșeurilor generate (HG 856/2002, anexa 1): cele 4 capitole, o pagină per cod de deșeu. Termen de depunere: 15 martie.",
    contentAnnualDeclaration:
      "Declarația anuală (centralizatorul): un rând per cod de deșeu — stoc inițial, generat, valorificat, eliminat, stoc final și prin cine. O pagină per punct de lucru.",
    contentEvidence: "Același an ca tabel de lucru (Excel + PDF)",
    contentPartners: "Rezumat PDF cu autorizațiile partenerilor și statusul lor",
    contentAttachments: "Documentele justificative atașate mișcărilor (+ index)",
    note: "Notă: în afară de evidența gestiunii deșeurilor, dosarul NU înlocuiește formularele oficiale (SIM / AFM); e un pachet de lucru pentru pregătirea controlului.",
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
      ANEXA_1: "Evidența gestiunii — deșeu propriu",
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
      AFM_ANNUAL: "AFM (anual, 25 ianuarie) — contribuția pentru ambalaje",
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
    body: "{count} linii din documentul pe care îl generezi așteaptă cântarul de la destinatar („se cântărește la descărcare”). Pe hârtie, cantitatea lor va lipsi, iar stocul nu se închide pe rândurile astea.",
    andMore: "și încă {count}",
    hint: "Poți completa cifra din Mișcări → „Adaugă cantitatea”, pe rândurile marcate „De cântărit”. Sau generează acum, dacă documentul e o ciornă de lucru.",
    generateAnyway: "Generează oricum",
    cancel: "Renunț, completez întâi",
  },

  packaging: {
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
      "Marfă preluată de la terți: apare aici fiindcă e ambalaj, dar NU intră în Anexa 1 — nu e deșeul tău. Raportul ei e Anexa 3 la Ordinul 794/2012, încă neconstruită.",
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
      "{n} mișcări fără materialul ambalajului. Codul nu îl decide singur: 15 01 04 acoperă și aluminiul, și oțelul; 15 01 02 și PET-ul, și navetele. Alege-l pe mișcare.",
    blockedMissingCategory:
      "{n} mișcări fără felul ambalajului (desfacere / primar / secundar și de transport), deci n-au coloană în tabelul 1.",
    awaitingWeighing: "{n} mișcări încă de cântărit — cantitatea lor lipsește din ambele tabele.",
    missingOperation: "{n} mișcări fără cod R/D — operatorul apare, operațiunea rămâne goală.",
    fix: "Completează",

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
