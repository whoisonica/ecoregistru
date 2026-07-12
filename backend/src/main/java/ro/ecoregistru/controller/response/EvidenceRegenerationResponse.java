package ro.ecoregistru.controller.response;

/** Result of regenerating a tenant's monthly evidence for a year. */
public record EvidenceRegenerationResponse(int year, int linesGenerated) {}
