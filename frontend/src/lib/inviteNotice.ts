import { strings } from "@/lib/strings";
import type { CompanyUser } from "@/lib/types";

/**
 * BUG-038 — după o invitație: „a plecat”, sau, când mailul n-a putut fi trimis, o eroare care
 * rămâne pe ecran până e închisă și trimite la „Retrimite invitația”. Contul e creat în ambele cazuri.
 */
export function notifyInvited(
  notify: (message: string, variant?: "success" | "error" | "info") => void,
  user: CompanyUser,
  sent: string,
) {
  if (user.inviteEmailSent === false) notify(strings.common.inviteMailFailed, "error");
  else notify(sent, "success");
}
