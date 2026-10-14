/** O culoare a temei: variabila CSS din `src/index.css`, cu opacitatea lăsată la Tailwind (`bg-brand/40`). */
const v = (name) => `rgb(var(--${name}) / <alpha-value>)`;

/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    // Direcția „Cântar” (docs/stil-interfata.md): colțuri mici peste tot, ca la un aparat, nu ca la o
    // aplicație de telefon. Scara e **înlocuită**, nu extinsă, dinadins: ecranele scriu `rounded-xl`,
    // `rounded-2xl`, `rounded-3xl` în zeci de locuri, iar aici toate cad pe 6–8px fără să fie
    // rescrise de mână. `full` rămâne rotund — pentru avatar și rotița de încărcare, nu pentru pastile.
    borderRadius: {
      none: "0",
      sm: "3px",
      DEFAULT: "4px",
      md: "5px",
      lg: "6px",
      xl: "6px",
      "2xl": "8px",
      "3xl": "8px",
      full: "9999px",
    },
    extend: {
      fontFamily: {
        sans: ["IBM Plex Sans", "ui-sans-serif", "system-ui", "-apple-system", "Segoe UI", "Roboto", "sans-serif"],
        // Cifre, CUI, coduri de deșeu, date, etichete mici cu majuscule. Aceeași familie ca textul,
        // deci nu sare în ochi ca un font de terminal — doar aliniază.
        mono: ["IBM Plex Mono", "ui-monospace", "SFMono-Regular", "Menlo", "monospace"],
      },
      fontSize: {
        // 12,5 / 14,5 px: mărimile machetei aprobate. Tabelele stau la 14px (`table.tsx`).
        xs: ["0.78125rem", { lineHeight: "1.1rem" }],
        sm: ["0.90625rem", { lineHeight: "1.35rem" }],
      },
      fontWeight: {
        // IBM Plex e citeț la 500–600; 700 rămâne doar pentru titlurile mari. `font-bold` scris în
        // ecrane cade pe 600, ca textul să nu se îngroașe ca un afiș.
        medium: "500",
        semibold: "600",
        bold: "600",
        extrabold: "700",
      },
      // Toate culorile sunt variabile CSS (`src/index.css`), câte una pe temă: `:root` = Luminos. Numele claselor
      // rămân aceleași pe toate temele (`bg-brand`, `bg-panel`, `text-state-ok-text`), deci ecranele nu se
      // rescriu când omul alege altă temă din Setări. Nu se scriu culori de mână (`gray-*`, `bg-white`).
      colors: {
        // Verdele logoului eco360. Butonul principal e `bg-brand text-brand-fg`; `text-brand` e altă
        // variabilă (mai jos, `textColor`), pentru că pe Întunecat butonul și săgeata de pe pagină diferă.
        brand: {
          DEFAULT: v("brand"),
          hover: v("brand-hover"),
          edge: v("brand-edge"),
          fg: v("brand-fg"),
          muted: v("brand-muted"),
          line: v("brand-line"),
          ...Object.fromEntries([50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950].map((n) => [n, v(`brand-${n}`)])),
        },
        // Paginile de dinaintea contului (login, cererea de cont): până la 14.10.2026 verdele landingului
        // (`#047857`); acum verdele aplicației. Rămâne doar linia și punctele; textul e în `textColor`.
        mark: {
          DEFAULT: v("mark-line"),
          soft: v("brand-muted"),
        },
        focus: v("focus"),
        surface: {
          DEFAULT: v("surface"),
          muted: v("surface-muted"),
          sunken: v("surface-sunken"),
        },
        line: {
          DEFAULT: v("border-subtle"),
          strong: v("border-strong"),
        },
        content: {
          DEFAULT: v("content"),
          strong: v("content-strong"),
          muted: v("content-muted"),
          subtle: v("content-subtle"),
          title: v("content-title"),
        },
        // Panoul: bara din stânga și banda de sus de pe telefon. `strong` e textul rândului activ și numele
        // firmei (pe panoul grafit de dinainte era `text-white`); `mid` e tot ce e estompat.
        panel: {
          DEFAULT: v("panel"),
          edge: v("panel-edge"),
          line: v("panel-line"),
          hover: v("panel-hover"),
          active: v("panel-active"),
          ind: v("panel-ind"),
          key: v("panel-key"),
          "key-bg": v("panel-key-bg"),
          text: v("panel-text"),
          strong: v("panel-strong"),
          mid: v("panel-mid"),
        },
        // Plăcuța firmei din panou (fondul) și cifrele ei; tot ele colorează mesajele din colț.
        lcd: {
          DEFAULT: v("lcd"),
          digit: v("lcd-digit"),
          unit: v("lcd-unit"),
          warn: v("lcd-warn"),
          bad: v("lcd-bad"),
        },
        // Intrarea în depozit e albastră — cealaltă direcție față de verdele „predat/ieșit".
        inbound: {
          DEFAULT: v("inbound"),
          key: v("inbound-key"),
          border: v("inbound-border"),
          fg: v("inbound-fg"),
          text: v("inbound-text"),
        },
        // Stările, ca LED-uri: pătrățel + cuvânt. Sensul nu se schimbă: roșu = nu se poate depune
        // așa; galben = o așteptare legitimă. Culoarea pubelei (mai jos) e altceva.
        state: {
          ok: v("state-ok"),
          "ok-text": v("state-ok-text"),
          warn: v("state-warn"),
          "warn-text": v("state-warn-text"),
          bad: v("state-bad"),
          "bad-text": v("state-bad-text"),
          off: v("state-off"),
          // Mesajele de eroare și avertismentele de pe pagină (fondul și chenarul lor) și textul butonului „danger”.
          "bad-fg": v("state-bad-fg"),
          "bad-bg": v("state-bad-bg"),
          "bad-line": v("state-bad-line"),
          "warn-bg": v("state-warn-bg"),
          "warn-line": v("state-warn-line"),
        },
        // Pubelele, pe codul de deșeu (`lib/binColor.ts`): o listă explicită, nu un prefix ghicit.
        bin: {
          paper: v("bin-paper"),
          plastic: v("bin-plastic"),
          glass: v("bin-glass"),
          bio: v("bin-bio"),
          residual: v("bin-residual"),
          hazard: v("bin-hazard"),
          metal: v("bin-metal"),
        },
        // Graficul anului de pe Acasă.
        chart: {
          DEFAULT: v("chart-bar"),
          now: v("chart-now"),
        },
        scrim: "var(--scrim)",
      },
      // `text-brand` (linkurile, bifele) și `text-mark` (login) au variabila lor: textul verde nu e verdele
      // butonului (pe Luminos e mai închis, pe Întunecat mai deschis). Restul nuanțelor `text-brand-*` vin din `colors`.
      textColor: {
        brand: { DEFAULT: v("link") },
        mark: { DEFAULT: v("mark-text") },
      },
      minWidth: {
        5: "1.25rem",
      },
      boxShadow: {
        // Nicio umbră pe pagină: cardurile stau pe chenar, nu pe umbră. Rămâne doar popover-ul.
        card: "none",
        "card-hover": "none",
        popover: "var(--shadow-popover)",
      },
      keyframes: {
        "fade-in": {
          from: { opacity: "0" },
          to: { opacity: "1" },
        },
        "slide-up": {
          from: { opacity: "0", transform: "translateY(6px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
        "slide-in-right": {
          from: { transform: "translateX(100%)" },
          to: { transform: "translateX(0)" },
        },
      },
      animation: {
        "fade-in": "fade-in 150ms ease-out",
        "slide-up": "slide-up 160ms ease-out",
        "slide-in-right": "slide-in-right 200ms ease-out",
      },
    },
  },
  plugins: [],
};
