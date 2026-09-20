import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import jsxA11y from "eslint-plugin-jsx-a11y";

/**
 * Ce nu poate spune `tsc`.
 *
 * <p>Până pe 20.09.2026 `npm run lint` era chiar `tsc --noEmit`: tipurile, şi nimic altceva. Două
 * familii de greşeli treceau pe lângă el şi se plăteau la om — dependenţele lipsă dintr-un hook
 * (un ecran care nu se mai reîmprospătează, ca BUG-031) şi accesibilitatea (o etichetă nelegată de
 * câmpul ei, un `onClick` pe un `div`). Amândouă se citesc din cod, deci nu e nevoie să le
 * observe cineva.
 *
 * <p>Regulile de stil lipsesc dinadins: formatarea n-a fost niciodată o problemă aici, iar un lint
 * care ţipă despre virgule ajunge să fie rulat cu `--fix` fără să fie citit.
 */
export default tseslint.config(
  {
    ignores: ["dist/**", "node_modules/**", "e2e/**", "scripts/**", "test/**", "*.config.js"],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ["src/**/*.{ts,tsx}"],
    languageOptions: {
      globals: { ...globals.browser },
    },
    plugins: {
      "react-hooks": reactHooks,
      "jsx-a11y": jsxA11y,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      ...jsxA11y.flatConfigs.recommended.rules,

      // Regula casei, din `CLAUDE.md`: explicaţiile stau în `Tooltip`, niciodată în `title=`.
      "jsx-a11y/no-access-key": "error",

      // ── Reguli potrivite pe ce face chiar codul ăsta ───────────────────────────────────────

      // Eticheta îşi înfăşoară câmpul (`<label><input/><span>text</span></label>`), care e o
      // asociere validă; textul stă într-un `span` imbricat, deci trebuie căutat mai adânc decât
      // implicitul de 2. Fără asta, regula raporta nouă false pozitive pe casete de bifat corecte.
      "jsx-a11y/label-has-associated-control": ["error", { depth: 3 }],

      // Toate cele 22 de folosiri sunt primul câmp al unui dialog sau al unui pas tocmai deschis
      // de om, adică exact locul unde mutarea focusului e comportamentul aşteptat. Regula e bună
      // pe o pagină care se încarcă singură; aici ar cere să stricăm ce e corect.
      "jsx-a11y/no-autofocus": "off",

      // Tiparul „ref-ul cu ultima valoare" (`const r = useRef(x); r.current = x;`), la patru
      // locuri, fiecare cu comentariul lui: `dialog.tsx`, `useHotkey.ts`, `useFormDraft.ts`. Ţine
      // ascultătorul să nu se re-abonezeze la fiecare randare. Regula nouă a compilatorului îl
      // vede ca scriere în timpul randării; noi îl vedem ca decizia care a rezolvat repornirile.
      "react-hooks/refs": "off",

      // ⚠️ Douăzeci şi două de locuri vechi, toate acoperite de probele e2e. Rămân **avertisment**,
      // nu eroare, dinadins: rescrise toate odată, fără teste de componentă, ar fi exact felul de
      // reparaţie sigură pe sine care strică un ecran. Se repară fişier cu fişier, când se umblă
      // oricum la el.
      "react-hooks/set-state-in-effect": "warn",
      "react-hooks/preserve-manual-memoization": "warn",

      // `any` e deja zero în `src/` (măsurat 20.09.2026) — de aici încolo e o eroare, ca să rămână.
      "@typescript-eslint/no-explicit-any": "error",

      // Argumentele neflosite cu `_` în faţă sunt intenţia, nu scăparea.
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
    },
  },
);
