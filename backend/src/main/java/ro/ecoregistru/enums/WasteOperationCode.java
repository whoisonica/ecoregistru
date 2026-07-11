package ro.ecoregistru.enums;

/**
 * Waste treatment operation codes from the EU Waste Framework Directive / OUG 92/2021 annexes.
 * R codes = recovery (valorificare), D codes = disposal (eliminare). These are stable.
 * Captured per movement for RECOVERED / DISPOSED so the official Anexa 1 chapters 3 and 4
 * can be generated later.
 */
public enum WasteOperationCode {
    R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13,
    D1, D2, D3, D4, D5, D6, D7, D8, D9, D10, D11, D12, D13, D14, D15;

    public boolean isRecovery() {
        return name().charAt(0) == 'R';
    }

    public boolean isDisposal() {
        return name().charAt(0) == 'D';
    }
}
