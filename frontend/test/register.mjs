// Pentru `npm test`: Node rulează TypeScript direct (ca `mobile/`, fără Jest), iar hookul de mai jos
// îi spune doar unde e `@/` și că un import fără extensie e un `.ts`. Se testează numai module fără
// JSX și fără React — regulile din `src/lib` și din `components/movements/movementRules.ts`.
import { register } from "node:module";

register("./resolve-alias.mjs", import.meta.url);
