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
      colors: {
        // Verdele de pe Punctul Verde al ambalajelor — acțiunea principală. `DEFAULT`, `fg` și
        // `muted` există de la început și sunt folosite peste tot (`bg-brand`, `text-brand-fg`,
        // `bg-brand-muted`) — nu se ating. Semnul mărcii are culorile lui, în `BrandName`.
        brand: {
          DEFAULT: "#009A44",
          fg: "#ffffff",
          muted: "#EEF6F1",
          50: "#EEF6F1",
          100: "#DDF5E6",
          200: "#B5EBC9",
          300: "#7CD9A1",
          400: "#2DB466",
          500: "#009A44",
          600: "#008A3D",
          700: "#007A36",
          800: "#00622F",
          900: "#004B24",
          950: "#022C16",
        },
        // Tokens semantice, legate de variabilele din index.css. Rostul lor e ca „fundalul unei
        // suprafețe" să fie o singură decizie, luată într-un loc — nu `bg-white` scris de 40 de
        // ori, care nu se poate schimba fără să le atingi pe toate.
        surface: {
          DEFAULT: "rgb(var(--surface) / <alpha-value>)",
          muted: "rgb(var(--surface-muted) / <alpha-value>)",
          sunken: "rgb(var(--surface-sunken) / <alpha-value>)",
        },
        line: {
          DEFAULT: "rgb(var(--border-subtle) / <alpha-value>)",
          strong: "rgb(var(--border-strong) / <alpha-value>)",
        },
        content: {
          DEFAULT: "rgb(var(--content) / <alpha-value>)",
          strong: "rgb(var(--content-strong) / <alpha-value>)",
          muted: "rgb(var(--content-muted) / <alpha-value>)",
          subtle: "rgb(var(--content-subtle) / <alpha-value>)",
        },
        // Panoul: bara din stânga și rama de pe telefon. Grafit, ca carcasa cântarului.
        panel: {
          DEFAULT: "#1B201D",
          hover: "#232925",
          active: "#2A322E",
          line: "#2F3632",
          key: "#3B4440",
          text: "#C9D1CC",
          mid: "#8A968F",
          dim: "#7E8A84",
          faint: "#6B7670",
        },
        // Afișajul LCD din panou: cifra lunii și rândul de alertă.
        lcd: {
          DEFAULT: "#0D1210",
          digit: "#7CF2A9",
          unit: "#4FB57C",
          warn: "#FFB020",
          bad: "#FF6B5E",
        },
        // Intrarea în depozit e albastră — cealaltă direcție față de verdele „predat/ieșit".
        inbound: {
          DEFAULT: "#1C6FD1",
          key: "#155BAD",
          border: "#0F4586",
        },
        // Stările, ca LED-uri: pătrățel + cuvânt. Sensul nu se schimbă: roșu = nu se poate depune
        // așa; galben = o așteptare legitimă. Culoarea pubelei (mai jos) e altceva.
        state: {
          ok: "#009A44",
          "ok-text": "#007A36",
          warn: "#F59E0B",
          "warn-text": "#8A4B00",
          bad: "#D92D20",
          "bad-text": "#B42318",
          off: "#B8BFBB",
        },
        // Pubelele, pe codul de deșeu (`lib/binColor.ts`): o listă explicită, nu un prefix ghicit.
        bin: {
          paper: "#1F5FBF",
          plastic: "#E9B600",
          glass: "#2E8B3E",
          bio: "#7A4E2D",
          residual: "#4D4D4D",
          hazard: "#C8102E",
          metal: "#8A9299",
        },
      },
      minWidth: {
        5: "1.25rem",
      },
      boxShadow: {
        // Nicio umbră pe pagină: cardurile stau pe chenar, nu pe umbră. Rămâne doar popover-ul.
        card: "none",
        "card-hover": "none",
        popover: "0 18px 40px -12px rgb(0 0 0 / 0.35)",
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
