/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // EcoRegistru brand: emerald. `DEFAULT`, `fg` și `muted` există de la început și sunt
        // folosite peste tot (`bg-brand`, `text-brand-fg`, `bg-brand-muted`) — nu se ating.
        // Scara 50–950 e adăugată ca stările (hover, apăsat, chenar) să nu mai fie emerald-uri
        // alese pe loc în câte o clasă.
        brand: {
          DEFAULT: "#047857",
          fg: "#ffffff",
          muted: "#ecfdf5",
          50: "#ecfdf5",
          100: "#d1fae5",
          200: "#a7f3d0",
          300: "#6ee7b7",
          400: "#34d399",
          500: "#10b981",
          600: "#059669",
          700: "#047857",
          800: "#065f46",
          900: "#064e3b",
          950: "#022c22",
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
          muted: "rgb(var(--content-muted) / <alpha-value>)",
          subtle: "rgb(var(--content-subtle) / <alpha-value>)",
        },
      },
      boxShadow: {
        card: "0 1px 2px 0 rgb(0 0 0 / 0.04), 0 1px 3px 0 rgb(0 0 0 / 0.06)",
        "card-hover": "0 2px 4px -1px rgb(0 0 0 / 0.06), 0 4px 10px -2px rgb(0 0 0 / 0.08)",
        popover: "0 4px 6px -2px rgb(0 0 0 / 0.05), 0 10px 24px -4px rgb(0 0 0 / 0.12)",
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
