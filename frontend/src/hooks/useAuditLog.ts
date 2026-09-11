import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { PageSlice, RemoteTableParams } from "@/hooks/useTableView";
import type { AuditLogEntry, AuditLogFilters } from "@/lib/types";

/**
 * Jurnalul de audit — P1.11.
 *
 * <p>Paginat la server din prima zi, fără varianta „aduce tot": e singura tabelă din aplicație
 * despre care se știe de la bun început că <b>numai crește</b>. La un client activ, un an de lucru
 * înseamnă zeci de mii de rânduri, iar aduse toate ar fi exact defectul pe care P3.1 tocmai l-a
 * închis în altă parte.
 *
 * <p>Nu există hook de scriere, fiindcă nu există endpoint de scriere. Rândurile le scrie
 * backendul singur, din interceptorul de Hibernate.
 */
const auditLogRoot = ["audit-log"] as const;

export function useAuditLog(filters: AuditLogFilters, table: RemoteTableParams, enabled = true) {
  return useQuery({
    enabled,
    queryKey: [...auditLogRoot, filters, table] as const,
    placeholderData: keepPreviousData,
    queryFn: async () => {
      const params: Record<string, string | number> = { page: table.page, size: table.size };
      if (filters.entityType) params.entityType = filters.entityType;
      if (filters.entityId) params.entityId = filters.entityId;
      if (table.search) params.search = table.search;
      return (await api.get<PageSlice<AuditLogEntry>>("/api/v1/audit-log", { params })).data;
    },
  });
}
