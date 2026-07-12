import { useRef, useState, type DragEvent } from "react";
import { Paperclip, UploadCloud, X } from "lucide-react";
import { cn } from "@/lib/utils";

interface FileDropzoneProps {
  /** Currently staged files (controlled by the parent). */
  files: File[];
  onChange: (files: File[]) => void;
  hint?: string;
  disabled?: boolean;
}

/**
 * Drag-and-drop (or click-to-pick) file staging area. Holds the selected files
 * in parent state; the parent uploads them after the movement is created.
 */
export function FileDropzone({ files, onChange, hint, disabled = false }: FileDropzoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  function addFiles(list: FileList | null) {
    if (!list || list.length === 0) return;
    onChange([...files, ...Array.from(list)]);
  }

  function removeAt(index: number) {
    onChange(files.filter((_, i) => i !== index));
  }

  function onDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragging(false);
    if (disabled) return;
    addFiles(e.dataTransfer.files);
  }

  return (
    <div>
      <div
        onClick={() => !disabled && inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={cn(
          "flex cursor-pointer flex-col items-center justify-center gap-1 rounded-md border border-dashed px-4 py-5 text-center text-sm transition-colors",
          dragging ? "border-brand bg-brand-muted" : "border-gray-300 bg-gray-50 hover:bg-gray-100",
          disabled && "cursor-not-allowed opacity-50"
        )}
      >
        <UploadCloud className="h-5 w-5 text-gray-400" />
        <span className="text-gray-500">{hint ?? "Trage fișiere aici sau apasă pentru a alege"}</span>
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          disabled={disabled}
          onChange={(e) => {
            addFiles(e.target.files);
            e.target.value = ""; // allow re-picking the same file
          }}
        />
      </div>

      {files.length > 0 && (
        <ul className="mt-2 space-y-1">
          {files.map((file, index) => (
            <li
              key={`${file.name}-${index}`}
              className="flex items-center justify-between gap-2 rounded border border-gray-200 px-2 py-1 text-sm"
            >
              <span className="flex min-w-0 items-center gap-2">
                <Paperclip className="h-3.5 w-3.5 shrink-0 text-gray-400" />
                <span className="truncate text-gray-700">{file.name}</span>
              </span>
              <button
                type="button"
                onClick={() => removeAt(index)}
                className="shrink-0 text-gray-400 hover:text-red-600"
                aria-label="Elimină fișierul"
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
