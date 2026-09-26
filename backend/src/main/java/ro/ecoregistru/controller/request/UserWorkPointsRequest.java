package ro.ecoregistru.controller.request;

import java.util.List;
import java.util.UUID;

/**
 * D2.4 — pe ce depozite lucrează un operator sau un cont de vizualizare. {@code allWorkPoints = true}:
 * toate, și cele deschise mai târziu (lista se ignoră). Altfel doar cele din listă, cel puțin unul.
 */
public record UserWorkPointsRequest(boolean allWorkPoints, List<UUID> workPointIds) {}
