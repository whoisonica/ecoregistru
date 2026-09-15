import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { ConsultancyBranding } from "@/lib/types";

/**
 * P2.14 — antetul cabinetului pe rapoartele neoficiale. Ca echipa, cabinetul nu se numește: e al sesiunii.
 */
export const brandingKey = ["consultancy", "branding"] as const;

export function useConsultancyBranding(enabled: boolean) {
  return useQuery({
    queryKey: brandingKey,
    queryFn: async () => (await api.get<ConsultancyBranding>("/api/v1/consultancy/branding")).data,
    enabled,
  });
}

function useBrandingMutation<T>(fn: (input: T) => Promise<ConsultancyBranding>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: (data) => qc.setQueryData(brandingKey, data),
  });
}

export function useSaveHeaderLine() {
  return useBrandingMutation(async (headerLine: string) =>
    (await api.put<ConsultancyBranding>("/api/v1/consultancy/branding", { headerLine })).data,
  );
}

export function useUploadLogo() {
  return useBrandingMutation(async (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return (await api.post<ConsultancyBranding>("/api/v1/consultancy/branding/logo", form)).data;
  });
}

export function useDeleteLogo() {
  return useBrandingMutation(async (_: void) =>
    (await api.delete<ConsultancyBranding>("/api/v1/consultancy/branding/logo")).data,
  );
}

/**
 * Logoul ca URL local. Se cere cu tokenul (un `<img src>` spre API n-ar trimite antetul), din nou la
 * fiecare `updatedAt`, și se eliberează când nu mai e afișat.
 */
export function useLogoUrl(branding: ConsultancyBranding | undefined) {
  const [url, setUrl] = useState<string | null>(null);
  const version = branding?.hasLogo ? branding.updatedAt : null;

  useEffect(() => {
    if (!version) {
      setUrl(null);
      return;
    }
    let objectUrl: string | null = null;
    let cancelled = false;
    api
      .get("/api/v1/consultancy/branding/logo", { responseType: "blob" })
      .then((res) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(res.data as Blob);
        setUrl(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setUrl(null);
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [version]);

  return url;
}
