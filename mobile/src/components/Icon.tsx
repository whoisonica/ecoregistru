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

export function Logo({ size = 20 }: { size?: number }) {
  const xml =
    '<svg viewBox="0 0 24 24"><rect x="3" y="14" width="18" height="6" rx="1.5" fill="#009A44"/><path d="M6 14 12 5l6 9" fill="none" stroke="#7CF2A9" stroke-width="2.2" stroke-linejoin="round"/><circle cx="12" cy="5" r="1.6" fill="#7CF2A9"/></svg>';
  return <SvgXml xml={xml} width={size} height={size} />;
}
