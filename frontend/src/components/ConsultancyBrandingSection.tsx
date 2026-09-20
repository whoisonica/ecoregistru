import { useEffect, useRef, useState, type FormEvent } from "react";
import { ImageUp, Trash2 } from "lucide-react";
import {
  useConsultancyBranding,
  useDeleteLogo,
  useLogoUrl,
  useSaveHeaderLine,
  useUploadLogo,
} from "@/hooks/useConsultancyBranding";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/components/ui/toast";
import { LoadError } from "@/components/ui/load-error";

const t = strings.consultancyBranding;
/** Aceeași limită ca serverul (`ReportBrandingService.MAX_LOGO_BYTES`), spusă înainte de urcare. */
const MAX_LOGO_BYTES = 500 * 1024;
const MAX_HEADER_LINE = 200;

/**
 * P2.14 — logoul și rândul de contact pe care le tipăresc rapoartele neoficiale ale firmelor cabinetului.
 *
 * <p>Previzualizarea desenează banda de sus a paginii cum o face `ReportBranding.addPdfHeader`: logo în
 * stânga, „Pregătit de …" și rândul în dreapta, o linie sub ele.
 */
export function ConsultancyBrandingSection() {
  const { data: branding, isLoading, isError, refetch } = useConsultancyBranding(true);
  const logoUrl = useLogoUrl(branding);
  const saveMut = useSaveHeaderLine();
  const uploadMut = useUploadLogo();
  const deleteMut = useDeleteLogo();
  const { notify } = useToast();
  const fileInput = useRef<HTMLInputElement>(null);

  const [headerLine, setHeaderLine] = useState("");
  useEffect(() => {
    setHeaderLine(branding?.headerLine ?? "");
  }, [branding?.headerLine]);

  async function handleFile(file: File | undefined) {
    if (!file) return;
    if (file.size > MAX_LOGO_BYTES) {
      notify(t.logoTooLarge, "error");
      return;
    }
    try {
      await uploadMut.mutateAsync(file);
      notify(t.logoUploaded, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    } finally {
      if (fileInput.current) fileInput.current.value = "";
    }
  }

  async function handleRemove() {
    try {
      await deleteMut.mutateAsync();
      notify(t.logoRemoved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    try {
      await saveMut.mutateAsync(headerLine);
      notify(t.saved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  const line = headerLine.trim();
  const hasHeader = Boolean(logoUrl) || line.length > 0;
  const busy = uploadMut.isPending || deleteMut.isPending;

  return (
    <section id="antet-rapoarte" className="mt-10 scroll-mt-20">
      <div className="mb-3">
        <h2 className="text-lg font-semibold text-content">{t.title}</h2>
        <p className="mt-1 max-w-2xl text-sm text-content-muted">{t.subtitle}</p>
        <p className="mt-1 max-w-2xl text-xs text-content-subtle">{t.officialNote}</p>
      </div>

      {isError && <LoadError message={t.loadError} onRetry={refetch} />}

      {!isError && !isLoading && branding && (
        <div className="grid gap-6 rounded-lg border border-line bg-surface p-4 lg:grid-cols-2">
          <div className="space-y-5">
            <div>
              <Label>{t.logo}</Label>
              <div className="mt-1 flex flex-wrap items-center gap-2">
                <input
                  ref={fileInput}
                  type="file"
                  accept="image/png,image/jpeg"
                  className="hidden"
                  onChange={(e) => handleFile(e.target.files?.[0])}
                />
                <Button
                  type="button"
                  variant="outline"
                  disabled={busy}
                  onClick={() => fileInput.current?.click()}
                >
                  <ImageUp className="mr-2 h-4 w-4" />
                  {branding.hasLogo ? t.replaceLogo : t.chooseLogo}
                </Button>
                {branding.hasLogo && (
                  <Button
                    type="button"
                    variant="ghost"
                    className="text-red-600 hover:bg-red-50"
                    disabled={busy}
                    onClick={handleRemove}
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    {t.removeLogo}
                  </Button>
                )}
              </div>
              <p className="mt-1 text-xs text-content-subtle">{t.logoHint}</p>
            </div>

            <form onSubmit={handleSave}>
              <Label htmlFor="branding-line">{t.headerLine}</Label>
              <Input
                id="branding-line"
                value={headerLine}
                maxLength={MAX_HEADER_LINE}
                placeholder={t.headerLinePlaceholder}
                onChange={(e) => setHeaderLine(e.target.value)}
              />
              <p className="mt-1 text-xs text-content-subtle">{t.headerLineHint}</p>
              <Button
                type="submit"
                className="mt-3"
                disabled={saveMut.isPending || line === (branding.headerLine ?? "")}
              >
                {saveMut.isPending ? strings.common.saving : t.save}
              </Button>
            </form>
          </div>

          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-content-muted">
              {t.preview}
            </p>
            <div className="rounded border border-line bg-white p-4 shadow-sm">
              {hasHeader ? (
                <div className="flex items-center justify-between gap-4 border-b border-gray-300 pb-2">
                  <div className="flex h-10 min-w-0 items-center">
                    {logoUrl ? (
                      <img src={logoUrl} alt={t.logo} className="max-h-10 max-w-[9rem] object-contain" />
                    ) : (
                      <span className="text-xs text-gray-400">{t.noLogo}</span>
                    )}
                  </div>
                  <div className="min-w-0 text-right">
                    <p className="truncate text-xs font-bold text-gray-900">
                      {t.preparedBy.replace("{name}", branding.consultancyName)}
                    </p>
                    {line && <p className="truncate text-[11px] text-gray-600">{line}</p>}
                  </div>
                </div>
              ) : (
                <p className="text-xs text-gray-500">{t.emptyPreview}</p>
              )}
              <div className="mt-3 space-y-1.5" aria-hidden>
                <div className="h-2.5 w-1/3 rounded bg-gray-300" />
                <div className="h-2 w-2/3 rounded bg-gray-200" />
                <div className="h-2 w-1/2 rounded bg-gray-200" />
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
