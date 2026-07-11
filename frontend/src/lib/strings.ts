/**
 * Central Romanian UI strings. Keep all user-facing text here so it can be
 * extracted into an i18n framework later without hunting through components.
 */
export const strings = {
  appName: "EcoRegistru",
  tagline: "Evidența și raportarea gestiunii deșeurilor",

  nav: {
    dashboard: "Panou",
    movements: "Mișcări",
    evidences: "Evidențe",
    partners: "Parteneri",
    deadlines: "Termene",
    auditFile: "Dosar de control",
    settings: "Setări",
    logout: "Deconectare",
  },

  login: {
    title: "Autentificare",
    email: "Email",
    password: "Parolă",
    submit: "Intră în cont",
    loading: "Se autentifică...",
    forgotPassword: "Ai uitat parola?",
    genericError: "Autentificare eșuată. Verifică datele și încearcă din nou.",
  },

  dashboard: {
    title: "Panou de control",
    welcome: "Bine ai venit",
    placeholder:
      "Aici vor apărea termenele următoare, mișcările lunii curente și alertele de expirare a autorizațiilor.",
  },

  common: {
    loading: "Se încarcă...",
    save: "Salvează",
    cancel: "Anulează",
    delete: "Șterge",
    edit: "Editează",
    add: "Adaugă",
  },
} as const;
