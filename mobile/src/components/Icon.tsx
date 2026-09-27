import { SvgXml } from "react-native-svg";

// Desenele din prototipul aprobat (`const I` în wastehouse-mobil-prototip.html), toate pe 24×24, cu contur.
const PATHS = {
  home: '<path d="M3.5 10.5 12 4l8.5 6.5V20a1 1 0 0 1-1 1H15v-6H9v6H4.5a1 1 0 0 1-1-1z"/>',
  list: '<rect x="4" y="3.5" width="16" height="17" rx="2.5"/><path d="M8 8.5h8M8 12h8M8 15.5h5"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  shield: '<path d="M12 3.5 19.5 6v6c0 4.6-3.2 7.6-7.5 8.5-4.3-.9-7.5-3.9-7.5-8.5V6z"/><path d="M8.8 12.2 11 14.4l4.4-4.6"/>',
  bell: '<path d="M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.5h-15zM10 20.5h4"/>',
  left: '<path d="M14.5 6 8.5 12l6 6"/>',
  right: '<path d="M9.5 6l6 6-6 6"/>',
  // M1a: intrarea și ieșirea din depozit, firma din comutator, telefonul din „Dispozitive conectate”.
  in: '<path d="M12 4v11M7.5 10.5 12 15l4.5-4.5M4.5 19.5h15"/>',
  out: '<path d="M12 20V9M7.5 13.5 12 9l4.5 4.5M4.5 4.5h15"/>',
  building: '<path d="M4 20.5V5a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v15.5M14 9.5h5a1 1 0 0 1 1 1v10M3 20.5h18M7.5 8h3M7.5 12h3M7.5 16h3"/>',
  phone: '<rect x="6" y="2.5" width="12" height="19" rx="2.5"/><path d="M10.5 18.5h3"/>',
  check: '<path d="M5 12.5 9.5 17 19 7.5"/>',
  alert: '<path d="M12 4.5 20.5 19H3.5z"/><path d="M12 10v4"/><path d="M12 16.6v.1"/>',
  // Refresh-ul (27.09.2026): desenele din `wastehouse-mobil-refresh.html`, același contur 24×24.
  cam: '<path d="M4 8.5h3l1.8-2.5h6.4L17 8.5h3v10.5H4z"/><circle cx="12" cy="13.5" r="3.6"/>',
  again: '<path d="M4.5 12a7.5 7.5 0 0 1 13-5.1L20 4.5v6h-6l2.4-2.4A4.8 4.8 0 1 0 17 14"/>',
  back: '<path d="M15 5 8 12l7 7"/>',
  send: '<path d="M20.5 3.5 3.5 10.5l7 3 3 7z"/><path d="m10.5 13.5 4-4"/>',
  bin: '<path d="M6 7.5h12l-1 12.5H7z"/><path d="M4.5 7.5h15M10 4.5h4"/>',
  doc: '<path d="M6.5 3.5h7l4 4v13h-11z"/><path d="M13.5 3.5v4h4M9 12h6M9 15.5h6"/>',
  nosig: '<path d="M4 20 20 4"/><path d="M8.5 15.5a5 5 0 0 1 3.5-1.4M5.5 12.2a9.5 9.5 0 0 1 6.5-2.6M12 18.5h.01"/>',
  cal: '<rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M3.5 9.5h17M8 3v4M16 3v4"/>',
  arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4"/>',
  user: '<circle cx="12" cy="8.5" r="4"/><path d="M4.5 20.5a7.5 7.5 0 0 1 15 0"/>',
  info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5"/><path d="M12 7.6v.1"/>',
  logout: '<path d="M10 4.5H5.5a1 1 0 0 0-1 1v13a1 1 0 0 0 1 1H10M15 8l4 4-4 4M19 12H9"/>',
} as const;

export type IconName = keyof typeof PATHS;

export function Icon({ name, size = 24, color, strokeWidth = 1.8 }: {
  name: IconName;
  size?: number;
  color: string;
  strokeWidth?: number;
}) {
  const xml = `<svg viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="${strokeWidth}" stroke-linecap="round" stroke-linejoin="round">${PATHS[name]}</svg>`;
  return <SvgXml xml={xml} width={size} height={size} />;
}

/**
 * Semnul WasteHouse — „bucla-casă”, alb pe #047857 (proprietarul, 15.09.2026: „peste tot, același logo”);
 * același path ca `frontend/src/components/BrandName.tsx` și iconițele din `assets/`.
 */
export function Logo({ size = 20 }: { size?: number }) {
  const xml =
    '<svg viewBox="0 0 32 32"><rect width="32" height="32" rx="8" fill="#047857"/><path d="M11.5 23.5H7V13L16 5l9 8v10.5h-7.5M21 20l-3.5 3.5L21 27" fill="none" stroke="#ffffff" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/></svg>';
  return <SvgXml xml={xml} width={size} height={size} />;
}
