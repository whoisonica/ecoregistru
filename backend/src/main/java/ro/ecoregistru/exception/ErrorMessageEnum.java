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
    EMAIL_NOT_VERIFIED("email.not.verified", "Emailul nu a fost verificat. Verifică-ți căsuța de email."),
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
    INVALID_INVITE_ROLE("invite.role.invalid", "Rol invalid pentru invitație. Alege Administrator, Operator sau Vizualizare."),

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

    // --- Partner ---
    PARTNER_NOT_FOUND("partner.not.found", "Partenerul nu a fost găsit."),
    DRIVER_NOT_FOUND("driver.not.found", "Șoferul nu a fost găsit."),
    DRIVER_NAME_REQUIRED("driver.name.required", "Scrie numele șoferului."),
    DRIVER_BELONGS_TO_PARTNER("driver.belongs.to.partner", "Șoferul ăsta e al unui transportator: editează-l în fișa partenerului."),
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
    ANEXA3_HAZARDOUS_NOT_ALLOWED("anexa3.hazardous", "Anexa 3 e formularul pentru deșeuri NEpericuloase. Pentru un cod periculos se folosește formularul de expediție/transport din anexa 2 la HG 1061/2008 — butonul „Anexa 2” de pe aceeași mișcare."),

    // --- Anexa 2 la HG 1061/2008 (transportul deşeurilor periculoase) ---
    ANEXA2_REQUIRES_HANDOVER("anexa2.requires.handover", "Formularul de expediție/transport se generează pentru un deșeu predat unui partener: alege valorificare sau eliminare și partenerul care o face."),
    ANEXA2_NOT_HAZARDOUS("anexa2.not.hazardous", "Anexa 2 e formularul pentru deșeuri PERICULOASE. Pentru un cod nepericulos se folosește formularul de încărcare-descărcare din anexa 3 la HG 1061/2008."),
    // Art. 24: la deşeurile periculoase din activitatea medicală formularul îl întocmeşte
    // transportatorul — „chiar dacă acesta este şi destinatar" — pe cantitatea cumulată a unei rute, cu
    // o anexă a expeditorilor. E alt flux, nu o variantă a acestuia, iar clinicile sunt clienţi-ţintă:
    // refuzul le spune pe ecran ce ar afla altfel la control.
    ANEXA2_MEDICAL_WASTE("anexa2.medical", "Deșeurile periculoase din activitatea medicală au alt flux: art. 24 din HG 1061/2008 cere ca formularul să-l întocmească TRANSPORTATORUL, pe cantitatea cumulată a unui transport dintr-o zonă, cu o anexă a expeditorilor. Nu e documentul pe care îl tipărește generatorul, deci nu-l generăm în locul lui."),
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
