/**
 * Mesajul de sub o rubrică greșită.
 *
 * <p>Perechea lui e `aria-describedby` pe control: fără legătura asta mesajul e roșu pentru cine
 * vede și inexistent pentru cine ascultă. De asta cere un `id` — nu e decorativ.
 */
export function FieldError({ id, message }: { id: string; message?: string }) {
  if (!message) return null;
  return (
    <p id={id} className="mt-1 text-xs font-medium text-red-600">
      {message}
    </p>
  );
}

/**
 * Ce se pune pe controlul însuși. `data-invalid` e cârligul după care formularul găsește **prima**
 * rubrică greșită ca să deruleze la ea — un selector, nu o listă de referințe ținută de mână.
 */
export function invalidProps(id: string, message?: string) {
  return {
    "aria-invalid": message ? true : undefined,
    "aria-describedby": message ? id : undefined,
    "data-invalid": message ? "true" : undefined,
  } as const;
}
