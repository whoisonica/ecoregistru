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
    REGISTRATION_DISABLED("registration.disabled", "Înregistrarea liberă este dezactivată. Conturile sunt create de administrator."),
    USER_NOT_FOUND("user.not.found", "Utilizator inexistent."),

    // --- Email ---
    EMAIL_SEND_FAILED("email.send.failed", "Trimiterea emailului a eșuat. Încearcă din nou mai târziu."),
    INVALID_EMAIL_EVENT("invalid.email.event", "Eveniment de email invalid."),

    // --- Tenancy / access ---
    TENANT_NOT_FOUND("tenant.not.found", "Firma (tenant) nu a fost găsită."),
    TENANT_REQUIRED("tenant.required", "Operațiunea necesită o firmă selectată."),
    ACCESS_DENIED("access.denied", "Nu ai acces la această resursă."),

    // --- Company ---
    COMPANY_NOT_FOUND("company.not.found", "Firma nu a fost găsită."),
    INVALID_CUI("company.cui.invalid", "CUI invalid. Introdu un cod fiscal valid (ex. RO12345678 sau 12345678)."),

    // --- WorkPoint ---
    WORK_POINT_NOT_FOUND("work.point.not.found", "Punctul de lucru nu a fost găsit."),

    // --- Partner ---
    PARTNER_NOT_FOUND("partner.not.found", "Partenerul nu a fost găsit."),

    // --- WasteCode ---
    WASTE_CODE_NOT_FOUND("waste.code.not.found", "Codul de deșeu nu a fost găsit."),

    // --- WasteMovement ---
    MOVEMENT_NOT_FOUND("movement.not.found", "Înregistrarea de deșeu nu a fost găsită."),
    PARTNER_REQUIRED_FOR_HANDOVER("movement.partner.required", "Predarea deșeului necesită selectarea unui partener (colector/transportator)."),
    INVALID_QUANTITY("movement.quantity.invalid", "Cantitatea trebuie să fie mai mare decât zero."),
    OPERATION_CODE_REQUIRED_RECOVERY("movement.operation.code.recovery", "Valorificarea deșeului necesită un cod de operație R (R1–R13)."),
    OPERATION_CODE_REQUIRED_DISPOSAL("movement.operation.code.disposal", "Eliminarea deșeului necesită un cod de operație D (D1–D15)."),
    OPERATION_CODE_NOT_ALLOWED("movement.operation.code.not.allowed", "Codul de operație R/D se aplică doar la valorificare sau eliminare."),

    // --- Evidence / deadlines ---
    EVIDENCE_NOT_FOUND("evidence.not.found", "Evidența nu a fost găsită."),
    DEADLINE_NOT_FOUND("deadline.not.found", "Termenul nu a fost găsit.");

    private final String code;
    private final String message;

    ErrorMessageEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
