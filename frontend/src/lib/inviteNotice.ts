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
  /** BUG-060: unde nu e listă cu „Retrimite invitația” (platforma → consultant), alt drum. */
  failed: string = strings.common.inviteMailFailed,
) {
  if (user.inviteEmailSent === false) notify(failed, "error");
  else notify(sent, "success");
}
