package ro.ecoregistru.exception;

import lombok.Getter;

/**
 * Business error catalogue. Codes are stable machine keys; messages are Romanian
 * (shown to non-technical users). Keep messages user-facing and blame-free.
 */
@Getter
public enum ErrorMessageEnum {

    // --- Authentication ---
    PASSWORDS_NOT_MATCH("passwords.not.match", "Parolele nu coincid."),
    WEAK_PASSWORD("weak.password", "Parola trebuie să aibă minim 8 caractere, cu literă mare, literă mică și cifră."),
    ACCOUNT_ALREADY_EXISTS("account.already.exists", "Există deja un cont cu acest email."),
    ACCOUNT_ALREADY_VERIFIED("account.already.verified", "Acest cont este deja verificat."),
    // Codul rămâne, textul spune ce se face: aplicația n-are verificare de email, iar un cont se activează
    // alegând parola (din invitație sau din „Am uitat parola”). Textul vechi trimitea după un mail care nu există.
    EMAIL_NOT_VERIFIED("email.not.verified", "Contul nu e activ încă. Alege-ți parola din linkul primit pe email sau din „Am uitat parola”."),
    ACCOUNT_DEACTIVATED("account.deactivated", "Contul a fost dezactivat. Cere administratorului să îl reactiveze."),
    INVALID_CREDENTIALS("invalid.credentials", "Email sau parolă incorecte."),
    INVALID_VERIFICATION_CODE("verification.code.invalid", "Codul de verificare este invalid."),
    VERIFICATION_CODE_EXPIRED("verification.code.expired", "Codul de verificare a expirat. Solicită unul nou."),
    VERIFICATION_RECORD_NOT_FOUND("verification.record.not.found", "Cod de verificare inexistent."),
    USER_NOT_FOUND("user.not.found", "Utilizator inexistent."),

    // --- Email ---
    EMAIL_SEND_FAILED("email.send.failed", "Trimiterea emailului a eșuat. Încearcă din nou mai târziu."),
    INVALID_EMAIL_EVENT("invalid.email.event", "Eveniment de email invalid."),

    // --- Tenancy / access ---
    TENANT_NOT_FOUND("tenant.not.found", "Firma (tenant) nu a fost găsită."),
    TENANT_REQUIRED("tenant.required", "Operațiunea necesită o firmă selectată."),
    ACCESS_DENIED("access.denied", "Nu ai acces la această resursă."),

    // --- Account requests (intake form) ---
    ACCOUNT_REQUEST_NOT_FOUND("account.request.not.found", "Cererea de cont nu a fost găsită."),
    ACCOUNT_REQUEST_ALREADY_HANDLED("account.request.already.handled", "Cererea a fost deja rezolvată."),

    // --- Company ---
    COMPANY_NOT_FOUND("company.not.found", "Firma nu a fost găsită."),
    INVALID_CUI("company.cui.invalid", "CUI invalid. Introdu un cod fiscal valid (ex. RO12345678 sau 12345678)."),
    COMPANY_CUI_ALREADY_EXISTS("company.cui.exists", "Există deja o firmă cu acest CUI."),
    // P2.13 — what a consultant is told instead of COMPANY_CUI_ALREADY_EXISTS. The firm may be a
    // client of another consultancy; the message must not say whose, and moving it is ours to do.
    COMPANY_CUI_UNAVAILABLE("company.cui.unavailable", "Firma cu acest CUI nu poate fi adăugată din contul tău. Scrie-ne la contact@wastehouse.ro și o rezolvăm."),
    INVALID_INVITE_ROLE("invite.role.invalid", "Rol invalid pentru invitație. Alege Administrator, Operator sau Vizualizare."),

    // --- Cabinete de consultanță (P2.13) ---
    CONSULTANCY_NOT_FOUND("consultancy.not.found", "Cabinetul nu a fost găsit."),
    CONSULTANCY_CUI_ALREADY_EXISTS("consultancy.cui.exists", "Există deja un cabinet cu acest CUI."),

    // --- Antetul cabinetului pe rapoarte (P2.14) ---
    BRANDING_LOGO_INVALID("branding.logo.invalid", "Logoul trebuie să fie o imagine PNG sau JPG."),
    BRANDING_LOGO_TOO_LARGE("branding.logo.too.large", "Logoul e prea mare. Cel mult 500 KB."),
    BRANDING_LOGO_NOT_FOUND("branding.logo.not.found", "Cabinetul nu are logo încărcat."),
    BRANDING_HEADER_TOO_LONG("branding.header.too.long", "Rândul de antet are cel mult 200 de caractere."),

    // --- Abonamente (plata-abonamente.md, F1) ---
    SUBSCRIPTION_COMPANY_IN_CONSULTANCY("subscription.company.in.consultancy", "Firma e în portofoliul unui cabinet, iar abonamentul îl plătește cabinetul. Scoate-o întâi din cabinet."),
    SUBSCRIPTION_PLAN_MISMATCH("subscription.plan.mismatch", "Abonamentul de cabinet se pune numai pe un cabinet, iar celelalte pachete numai pe o firmă."),
    COMPANY_HAS_OWN_SUBSCRIPTION("company.has.own.subscription", "Firma are abonament propriu. Șterge-l întâi, altfel ar plăti de două ori: o dată ea, o dată cabinetul."),
    // F2 — a subscription with invoices is the only link between them and FGO.
    SUBSCRIPTION_HAS_INVOICES("subscription.has.invoices", "Abonamentul are deja facturi și nu se mai poate șterge, altfel se pierde legătura cu ele în FGO."),
    // F3 — cardul.
    SUBSCRIPTION_NOT_FOUND("subscription.not.found", "Contul ăsta n-are un abonament de plătit."),
    INVOICE_NOT_FOUND("invoice.not.found", "Factura nu a fost găsită."),
    INVOICE_NOT_PAYABLE("invoice.not.payable", "Factura e deja plătită sau nu e încă emisă."),
    CARD_PAYMENT_UNAVAILABLE("card.payment.unavailable", "Plata cu cardul nu e disponibilă acum. Poți plăti prin transfer, în contul de pe factură, sau încearcă mai târziu."),
    // F4, §9.3 — oprirea.
    SUBSCRIPTION_ALREADY_CANCELLED("subscription.already.cancelled", "Abonamentul e deja oprit."),

    // --- Utilizatorii firmei (P1.12) ---
    CANNOT_MANAGE_SELF("user.cannot.manage.self", "Nu îți poți schimba sau dezactiva propriul cont de aici."),
    LAST_ADMIN("user.last.admin", "E singurul administrator activ al firmei. Fă întâi pe altcineva administrator, apoi revino."),
    USER_NOT_PENDING("user.not.pending", "Invitația se retrimite doar unui cont care n-a intrat încă. Contul ăsta are deja parolă."),
    USER_NOT_DEACTIVATED("user.not.deactivated", "Contul nu e dezactivat, deci n-are ce reactiva."),
    USER_ALREADY_DEACTIVATED("user.already.deactivated", "Contul e deja dezactivat."),
    USER_STILL_PENDING("user.still.pending", "Contul n-a intrat încă în aplicație. Anulează invitația, nu-l dezactiva."),
    USER_NOT_INVITATION("user.not.invitation", "Contul a fost folosit, deci nu mai e o invitație de anulat. Dezactivează-l."),

    // --- WorkPoint ---
    WORK_POINT_NOT_FOUND("work.point.not.found", "Punctul de lucru nu a fost găsit."),

    // --- Modulul de depozit (V46) ---
    WEIGHING_OPERATION_NOT_FOUND("weighing.operation.not.found", "Operațiunea nu a fost găsită."),
    WEIGHING_OPERATION_TYPE_REQUIRED("weighing.operation.type.required", "Alege tipul operațiunii: intrare sau ieșire."),
    WEIGHING_OPERATION_TYPE_UNAVAILABLE("weighing.operation.type.unavailable", "Deocamdată se înregistrează doar intrări și ieșiri."),
    WEIGHING_OPERATION_DATE_REQUIRED("weighing.operation.date.required", "Alege data operațiunii."),
    WEIGHING_OPERATION_ONE_COUNTERPARTY("weighing.operation.one.counterparty", "Alege fie un partener, fie o persoană fizică, nu amândouă."),
    WEIGHING_OPERATION_PERSON_ONLY_IN("weighing.operation.person.only.in", "O persoană fizică doar vinde depozitului. O ieșire merge la un operator autorizat, deci alege un partener."),
    NATURAL_PERSON_NOT_FOUND("natural.person.not.found", "Persoana fizică nu a fost găsită."),
    WEIGHING_OPERATION_NOT_EDITABLE("weighing.operation.not.editable", "Operațiunea e finalizată sau anulată și nu se mai modifică."),
    WEIGHING_OPERATION_NO_LINES("weighing.operation.no.lines", "O operațiune fără linii nu se poate finaliza."),
    WEIGHING_OPERATION_CANCEL_REASON_REQUIRED("weighing.operation.cancel.reason.required", "Scrie motivul anulării."),
    WEIGHING_OPERATION_ALREADY_CANCELLED("weighing.operation.already.cancelled", "Operațiunea e deja anulată."),
    WEIGHING_OPERATION_TARE_ABOVE_GROSS("weighing.operation.tare.above.gross", "Tara mașinii trebuie să fie mai mică decât brutul."),
    WEIGHING_LINES_REQUIRED("weighing.lines.required", "Adaugă cel puțin o linie."),
    WEIGHING_LINE_ARTICLE_REQUIRED("weighing.line.article.required", "Alege sortimentul pe fiecare linie."),
    WASTE_ARTICLE_NOT_FOUND("waste.article.not.found", "Sortimentul nu a fost găsit."),
    WEIGHING_LINE_WEIGHT_NEGATIVE("weighing.line.weight.negative", "Greutățile nu pot fi negative."),
    WEIGHING_LINE_NET_REQUIRED("weighing.line.net.required", "Trece pe fiecare linie brutul și tara sau direct cantitatea netă."),
    WEIGHING_LINE_NET_NOT_POSITIVE("weighing.line.net.not.positive", "Neto trebuie să fie mai mare decât zero: brutul trebuie să fie mai mare decât tara."),
    WEIGHING_LINE_NET_MISMATCH("weighing.line.net.mismatch", "Neto nu se potrivește cu brut minus tara."),
    WEIGHING_LINE_FINAL_NOT_POSITIVE("weighing.line.final.not.positive", "Cantitatea finală trebuie să fie mai mare decât zero."),
    WEIGHING_LINE_FINAL_ABOVE_NET("weighing.line.final.above.net", "Cantitatea finală nu poate fi mai mare decât neto."),
    WEIGHING_LINE_PRICE_NEGATIVE("weighing.line.price.negative", "Prețul nu poate fi negativ."),
    WEIGHING_LINE_OPERATION_CODE_REQUIRED("weighing.line.operation.code.required", "La ieșire, alege pe fiecare linie operația: valorificare (R) sau eliminare (D)."),

    // --- Partner ---
    PARTNER_NOT_FOUND("partner.not.found", "Partenerul nu a fost găsit."),
    DRIVER_NOT_FOUND("driver.not.found", "Șoferul nu a fost găsit."),
    DRIVER_NAME_REQUIRED("driver.name.required", "Scrie numele șoferului."),
    DRIVER_BELONGS_TO_PARTNER("driver.belongs.to.partner", "Șoferul ăsta e al unui transportator: editează-l în fișa partenerului."),
    DRIVER_DELETE_REQUIRES_DEACTIVATION("driver.delete.requires.deactivation", "Dezactivează întâi șoferul. Ștergerea fișei e definitivă, deci se face doar pentru un șofer scos deja din listă."),
    PARTNER_TYPE_REQUIRED("partner.type.required", "Alege ce face partenerul cu deșeul, sau bifează „Transportator” dacă e o firmă care doar transportă."),
    PARTNER_ROLE_REQUIRED("partner.role.required", "Alege rolul partenerului: client (îi predai deșeu și îi facturezi tu), furnizor (îți prestează serviciul și îți facturează el) sau ambele."),

    // --- Internal generator (Anexa 1 cap. 2 "Secţia") ---
    INTERNAL_GENERATOR_NOT_FOUND("internal.generator.not.found", "Generatorul intern nu a fost găsit."),
    INTERNAL_GENERATOR_NAME_TAKEN("internal.generator.name.taken", "Există deja un generator intern cu acest nume în punctul de lucru."),
    INTERNAL_GENERATOR_WORK_POINT_IMMUTABLE("internal.generator.work.point.immutable", "Generatorul intern nu poate fi mutat în alt punct de lucru. Dezactivează-l aici și adaugă-l acolo."),
    INTERNAL_GENERATOR_WRONG_WORK_POINT("internal.generator.wrong.work.point", "Generatorul intern ales aparține altui punct de lucru."),

    // --- WasteCode ---
    WASTE_CODE_NOT_FOUND("waste.code.not.found", "Codul de deșeu nu a fost găsit."),

    // --- WasteMovement ---
    MOVEMENT_NOT_FOUND("movement.not.found", "Înregistrarea de deșeu nu a fost găsită."),
    INVALID_QUANTITY("movement.quantity.invalid", "Cantitatea trebuie să fie mai mare decât zero."),
    UNLOAD_BEFORE_LOAD("movement.unload.before.load", "Data descărcării nu poate fi înaintea datei încărcării."),
    QUANTITY_REQUIRED("movement.quantity.required", "Cantitatea e obligatorie. Dacă o cântărește destinatarul la descărcare, bifează „Se cântărește la descărcare” și lasă câmpul gol."),
    WEIGHING_NEEDS_RECIPIENT("movement.weighing.recipient", "Cântărirea la descărcare o face destinatarul: alege partenerul care preia deșeul."),
    PARTNER_WORK_POINT_MISMATCH("movement.partner.work.point.mismatch", "Punctul de lucru ales nu e al destinatarului. Alege unul dintre punctele lui de lucru."),
    NOT_AWAITING_WEIGHING("movement.weight.not.awaited", "Mișcarea are deja cantitatea înregistrată. Ca s-o schimbi, editeaz-o."),

    // --- Attachments ---
    ATTACHMENT_NOT_FOUND("attachment.not.found", "Atașamentul nu a fost găsit."),
    ATTACHMENT_FETCH_FAILED("attachment.fetch.failed", "Fișierul nu a putut fi descărcat. Încearcă din nou peste câteva momente."),
    ATTACHMENT_TOO_LARGE("attachment.too.large", "Fișierul e prea mare. Cel mult 10 MB per fișier."),


    // --- Anexa 3 la HG 1061/2008 (formularul de transport) ---
    ANEXA3_REQUIRES_HANDOVER("anexa3.requires.handover", "Formularul de transport se generează pentru un deșeu predat unui partener: alege valorificare sau eliminare și partenerul care o face."),
    AVIZ_REQUIRES_HANDOVER("aviz.requires.handover", "Avizul de însoțire se generează pentru un deșeu predat unui partener: alege valorificare sau eliminare și partenerul care preia deșeul."),
    ANEXA3_HAZARDOUS_NOT_ALLOWED("anexa3.hazardous", "Anexa 3 e formularul pentru deșeuri NEpericuloase. Pentru un cod periculos se folosește formularul de expediție/transport din anexa 2 la HG 1061/2008 — butonul „Anexa 2” de pe aceeași mișcare."),

    // --- Anexa 2 la HG 1061/2008 (transportul deşeurilor periculoase) ---
    ANEXA2_REQUIRES_HANDOVER("anexa2.requires.handover", "Formularul de expediție/transport se generează pentru un deșeu predat unui partener: alege valorificare sau eliminare și partenerul care o face."),
    ANEXA2_NOT_HAZARDOUS("anexa2.not.hazardous", "Anexa 2 e formularul pentru deșeuri PERICULOASE. Pentru un cod nepericulos se folosește formularul de încărcare-descărcare din anexa 3 la HG 1061/2008."),
    // Art. 24: la deşeurile periculoase din activitatea medicală formularul îl întocmeşte
    // transportatorul — „chiar dacă acesta este şi destinatar" — pe cantitatea cumulată a unei rute, cu
    // o anexă a expeditorilor. E alt flux, nu o variantă a acestuia, iar clinicile sunt clienţi-ţintă:
    // refuzul le spune pe ecran ce ar afla altfel la control.
    ANEXA2_MEDICAL_WASTE("anexa2.medical", "Deșeurile periculoase din activitatea medicală au alt flux: art. 24 din HG 1061/2008 cere ca formularul să-l întocmească TRANSPORTATORUL, pe cantitatea cumulată a unui transport dintr-o zonă, cu o anexă a expeditorilor. Nu e documentul pe care îl tipărește generatorul, deci nu-l generăm în locul lui."),
    // Specialista, 14.09.2026: „anexa 2 o păstrăm doar pentru colectori".
    ANEXA2_COLLECTORS_ONLY("anexa2.collectors.only", "Anexa 2 (formularul de transport pentru deșeuri periculoase) o întocmește colectorul care preia deșeul. Contul e de generator, deci formularul nu se tipărește de aici."),
    // Aceeași seară: „generatorii au doar ieșiri" — deci n-au ambalaje preluate de la terți de raportat.
    ANEXA3_PACKAGING_COLLECTORS_ONLY("anexa3.packaging.collectors.only", "Anexa 3 la Ordinul 794/2012 raportează ambalajele preluate de la terți, deci o depun colectorii, comercianții și reciclatorii. Contul e de generator, deci raportul nu se întocmește de aici."),
    // 15.09.2026, AD închisă: evidența art. 48 a mărfii preluate e a celor care preiau; generatorul o ține pe fișa Anexa 1.
    ART48_REGISTER_COLLECTORS_ONLY("art48.register.collectors.only", "Evidența cronologică a deșeurilor preluate de la terți o țin firmele care preiau deșeu. Contul e de generator: evidența lui e fișa Anexa 1, de pe ecranul „Generare”."),
    OPERATION_CODE_REQUIRED_RECOVERY("movement.operation.code.recovery", "Valorificarea deșeului necesită un cod de operație R (R1–R13)."),
    OPERATION_CODE_REQUIRED_DISPOSAL("movement.operation.code.disposal", "Eliminarea deșeului necesită un cod de operație D (D1–D15)."),
    OPERATION_CODE_NOT_ALLOWED("movement.operation.code.not.allowed", "Codul de operație R/D se completează doar la valorificare sau eliminare."),
    OPERATION_NOT_ALLOWED_FOR_COMPANY_TYPE("movement.operation.not.allowed", "Operațiunea nu este disponibilă pentru tipul firmei."),
    OPERATION_CODE_NOT_IN_PROFILE("movement.operation.code.not.in.profile", "Codul de operație nu e printre cele declarate de firmă. Completează profilul firmei dacă a apărut o operațiune nouă."),
    OPERATION_NOT_SELECTABLE("movement.operation.not.selectable", "„Ieșire neclasificată” nu se poate alege: e starea liniilor vechi, fără cod R/D. Alege valorificare sau eliminare și codul operației."),
    REGISTER_INVALID_FOR_OPERATION("movement.register.invalid", "Registrul nu se potrivește cu operațiunea: deșeul generat intră în Anexa 1, iar preluarea de la terți în registrul cronologic."),
    // Un cont care preia deşeu de la terţi nu poate lăsa întrebarea fără răspuns: aceeaşi ieşire
    // ajunge fie pe Anexa 1 (deşeu propriu), fie pe registrul art. 48 şi pe Anexa 3 Ambalaje
    // (marfă preluată). Implicitul de dinainte era Anexa 1, deci marfa altuia se declara ca pusă
    // pe piaţă de firmă. Vezi docs/status.md, „Provenienţa deşeului la ieşire" (25.08.2026).
    REGISTER_REQUIRED_ON_EXIT("movement.register.required",
            "Spune de unde vine deşeul: generat în activitatea proprie sau preluat de la terţi. "
                    + "De răspunsul ăsta atârnă pe ce formular ajunge cantitatea."),

    ART48_REGISTER_NOT_ENABLED("movement.register.art48.disabled", "Firma e înregistrată doar ca generator. Preluarea de deșeuri de la terți cere tipul „Colector” sau „Ambele”."),

    // --- Import din Excel (P2.15) ---
    IMPORT_FILE_UNREADABLE("import.file.unreadable",
            "Fișierul nu se poate citi ca Excel. Folosește șablonul .xlsx descărcat din aplicație, fără parolă."),
    IMPORT_TEMPLATE_MISMATCH("import.template.mismatch",
            "Fișierul nu are foile și coloanele șablonului. Descarcă șablonul din aplicație și copiază datele în el, fără să muți coloanele."),
    IMPORT_TOO_MANY_ROWS("import.too.many.rows",
            "O foaie are mai mult de 2.000 de rânduri. Împarte fișierul pe ani și importă-i pe rând."),

    // --- Evidence / deadlines ---
    EVIDENCE_NOT_FOUND("evidence.not.found", "Evidența nu a fost găsită."),
    EXPORT_FORMAT_UNSUPPORTED("export.format.unsupported", "Format de export nesuportat. Alege „xlsx” sau „pdf”."),
    AUDIT_FILE_YEARS_UNSUPPORTED("audit.file.years.unsupported",
            // Cifra e MAX_YEARS din AuditFileService, nu termenul din lege: art. 48 alin. (5) spune
            // „cel puțin 3 ani", adică un prag, nu un plafon. Mesajul spunea „cel mult 3 ani" și
            // prezenta pragul ca limită — de două ori greșit, fiindcă plaja crescuse la 5 pe 25.08.
            "Dosarul se poate genera pentru cel mult 5 ani. Alege un număr între 1 și 5. (Evidența se păstrează cel puțin 3 ani — OUG 92/2021, art. 48 alin. (5).)"),
    DEADLINE_NOT_FOUND("deadline.not.found", "Termenul nu a fost găsit."),

    // Ordinul 794/2012 art. 4 alin. (1) cere „tabelul 1 sau, după caz, tabelul 2", iar care
    // anume ține de calitatea firmei — pe care numai ea o știe. Un ecran se oferă pe un profil
    // gol (decizia 6), un document nu: ar afirma calitatea juridică a clientului în locul lui.
    PACKAGING_OPERATOR_ROLE_REQUIRED("packaging.operator.role.required",
            "Alege întâi calitatea firmei pentru deșeurile de ambalaje — colector, comerciant, "
                    + "reciclator sau valorificator. Ea decide care tabel al Anexei 3 se depune "
                    + "(Ordinul 794/2012, art. 4 alin. (1)). Se completează în profilul firmei.");

    private final String code;
    private final String message;

    ErrorMessageEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
