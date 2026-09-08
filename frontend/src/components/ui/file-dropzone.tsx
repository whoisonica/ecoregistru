import { useRef, useState, type DragEvent } from "react";
import { Paperclip, UploadCloud, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";

/**
 * Ce se poate atașa la o mișcare: dovada predării, avizul, buletinul de analiză, poza de la cântar.
 * Lista nu e o restricție de securitate — aia e a backendului — ci selectorul de fișiere deschis
 * pe ce caută omul, în loc de tot discul.
 */
const ACCEPTED_TYPES = "image/*,application/pdf,.doc,.docx,.xls,.xlsx,.csv,.txt";

/**
 * Cât de mare are voie un fișier. Se verifica nicăieri: se putea pune la coadă o filmare de 500 MB,
 * care se descoperea abia după ce urcarea o pornea, fișier cu fișier, pe conexiunea din depozit.
 */
const MAX_FILE_MB = 15;

interface FileDropzoneProps {
  /** Currently staged files (controlled by the parent). */
  files: File[];
  onChange: (files: File[]) => void;
  hint?: string;
  disabled?: boolean;
  /** Ce se spune despre un fișier respins. Lipsă = se aruncă tăcut, ceea ce nu se face. */
  onReject?: (message: string) => void;
}

/**
 * Drag-and-drop (or click-to-pick) file staging area. Holds the selected files
 * in parent state; the parent uploads them after the movement is created.
 */
export function FileDropzone({
  files,
  onChange,
  hint,
  disabled = false,
  onReject,
}: FileDropzoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  function addFiles(list: FileList | null) {
    if (!list || list.length === 0) return;
    const accepted: File[] = [];
    const tooBig: string[] = [];
    for (const file of Array.from(list)) {
      if (file.size > MAX_FILE_MB * 1024 * 1024) tooBig.push(file.name);
      else accepted.push(file);
    }
    if (tooBig.length > 0) {
      onReject?.(
        strings.fileDropzone.tooBig
          .replace("{files}", tooBig.join(", "))
          .replace("{mb}", String(MAX_FILE_MB))
      );
    }
    if (accepted.length > 0) onChange([...files, ...accepted]);
  }

  function removeAt(index: number) {
    onChange(files.filter((_, i) => i !== index));
  }

  function onDrop(e: DragEvent<HTMLElement>) {
    e.preventDefault();
    setDragging(false);
    if (disabled) return;
    addFiles(e.dataTransfer.files);
  }

  return (
    <div>
      {/*
        Un `<button>` adevărat, nu un `<div onClick>`.
        Zona era o casetă pe care se putea doar da clic: fără `role`, fără `tabIndex`, iar
        `<input type="file">` era `className="hidden"`, adică `display: none` — deci nefocusabil el
        însuși. Cu tastatura nu exista **nicio** cale către încărcarea unui fișier, singura zonă din
        aplicație rămasă așa după runda de accesibilitate. Butonul aduce focusul, Enter și Space de
        la browser, fără să le scriem noi.
      */}
      <button
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={cn(
          "flex w-full cursor-pointer flex-col items-center justify-center gap-1 rounded-md border border-dashed px-4 py-5 text-center text-sm transition-colors",
          dragging ? "border-brand bg-brand-muted" : "border-line-strong bg-surface-muted hover:bg-surface-sunken",
          disabled && "cursor-not-allowed opacity-50"
        )}
      >
        <UploadCloud className="h-5 w-5 text-content-subtle" aria-hidden />
        <span className="text-content-muted">{hint ?? strings.fileDropzone.hint}</span>
        <span className="text-xs text-content-subtle">
          {strings.fileDropzone.limit.replace("{mb}", String(MAX_FILE_MB))}
        </span>
      </button>
      {/* `sr-only`, nu `hidden`: rămâne în afara ecranului, dar accesibil programatic. */}
      <input
        ref={inputRef}
        type="file"
        multiple
        accept={ACCEPTED_TYPES}
        className="sr-only"
        tabIndex={-1}
        disabled={disabled}
        aria-hidden
        onChange={(e) => {
          addFiles(e.target.files);
          e.target.value = ""; // allow re-picking the same file
        }}
      />

      {files.length > 0 && (
        <ul className="mt-2 space-y-1">
          {files.map((file, index) => (
            <li
              key={`${file.name}-${index}`}
              className="flex items-center justify-between gap-2 rounded border border-line px-2 py-1 text-sm"
            >
              <span className="flex min-w-0 items-center gap-2">
                <Paperclip className="h-3.5 w-3.5 shrink-0 text-content-subtle" />
                <span className="truncate text-content-strong">{file.name}</span>
              </span>
              <button
                type="button"
                onClick={() => removeAt(index)}
                className="shrink-0 text-content-subtle hover:text-red-600"
                aria-label={strings.fileDropzone.remove}
              >
                <X className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
