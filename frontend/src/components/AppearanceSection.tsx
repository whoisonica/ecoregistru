import { useState } from "react";
import { ChoiceCards } from "@/components/ui/choice-cards";
import { applyTheme, readTheme, THEMES, type Theme } from "@/lib/theme";
import { strings } from "@/lib/strings";

const t = strings.settings.appearance;

/**
 * Miniatura unei teme: meniul, pagina și butonul, în culorile **acelei** teme, oricare ar fi cea aleasă acum.
 * De aceea culorile sunt scrise aici, nu luate din tokeni (sunt cele din `index.css`).
 */
const SWATCH: Record<Theme, { panel: string; page: string; button: string }> = {
  luminos: { panel: "#F4F9EF", page: "#FFFFFF", button: "#0B8C4A" },
  padure: { panel: "#14532D", page: "#FFFFFF", button: "#0B8C4A" },
  intunecat: { panel: "#0D1310", page: "#131A16", button: "#3FAF46" },
};

function Swatch({ theme }: { theme: Theme }) {
  const c = SWATCH[theme];
  return (
    <span className="flex h-10 w-10 overflow-hidden rounded-md border border-line-strong">
      <span className="w-3" style={{ background: c.panel }} />
      <span className="flex flex-1 items-end p-1" style={{ background: c.page }}>
        <span className="h-2 w-full rounded-sm" style={{ background: c.button }} />
      </span>
    </span>
  );
}

/** Tema de culori, pe Setări: se aplică pe loc, la orice rol, fără salvare. */
export function AppearanceSection() {
  const [theme, setTheme] = useState<Theme>(readTheme);

  const choose = (next: Theme) => {
    applyTheme(next);
    setTheme(next);
  };

  return (
    <section aria-labelledby="grup-aspect">
      <h2 id="grup-aspect" className="eyebrow mb-1">
        {t.title}
      </h2>
      <p className="mb-3 text-sm text-content-muted">{t.hint}</p>
      <ChoiceCards<Theme>
        name="tema"
        value={theme}
        onChange={choose}
        columns={3}
        aria-labelledby="grup-aspect"
        className="xl:max-w-4xl"
        options={THEMES.map((value) => ({
          value,
          label: t[value],
          description: t[`${value}Hint`],
          icon: <Swatch theme={value} />,
        }))}
      />
    </section>
  );
}
