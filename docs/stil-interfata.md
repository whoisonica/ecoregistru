# Stilul interfeței — „Prietenos”

*Decis de proprietar pe 15.09.2026, din trei variante desenate (Liniștit · Prietenos · Birou). Documentul
ăsta e regula pentru orice ecran sau formular nou și pentru orice ecran vechi care se atinge. Dacă un
ecran nou nu arată așa, e un defect, nu o preferință.*

---

## Pentru cine

Clientul care cumpără modulul de generator e patronul unei brutării sau administratorul unui magazin, nu
specialistul de mediu. Deschide aplicația o dată pe săptămână, cu bonul de cântar în mână. Ecranele de
până acum erau corecte, dar vorbeau ca legea („1 linie fără cod R/D la ieșire”). Stilul ăsta mută
aplicația în limbajul lucrurilor pe care omul chiar le face, **fără să schimbe ce se salvează sau ce se
tipărește**.

---

## Tokenii — unde stau și ce valori au

Nicio culoare, colț sau umbră nu se scrie de mână într-un ecran. Se folosesc clasele de mai jos.

| Ce | Unde | Valoare |
|---|---|---|
| Font | `src/index.css` (`@font-face`), `tailwind.config.js` (`fontFamily.sans`) | **Nunito**, servit din `public/fonts/` (licența OFL lângă fișiere), cu subsetul `latin-ext` pentru ă, ș, ț. **Nu** de pe Google Fonts: ar trimite IP-ul clientului unui terț |
| Mărimi de text | `tailwind.config.js` (`fontSize`) | `text-sm` = 15px, `text-xs` = 13px. Nunito are literele mai mici, deci au urcat cu un pixel |
| Fundalul paginii | `bg-surface-muted` | `#EDF4F0`, verde pal |
| Cardurile, tabelele, dialogurile | `bg-surface` | alb |
| Zone adâncite, câmp dezactivat | `bg-surface-sunken` | `#E3EBE6` |
| Chenar obișnuit / la hover | `border-line` / `border-line-strong` | `#E0E8E3` / `#CBD5CF` |
| Text | `text-content` · `-strong` · `-muted` · `-subtle` | `#14201B` · `#37443E` · `#667069` · `#848F89` |
| Marca | `bg-brand`, `text-brand-800`, `bg-brand-50` | emerald `#047857`, scara 50–950 |
| Roșu / galben | `red-*` / `amber-*` | **Roșu = nu se poate depune așa. Galben = o așteptare legitimă** (cântarul). Nu se amestecă |
| Umbre | `shadow-card`, `shadow-card-hover`, `shadow-popover` | cu tentă verde, nu negru |

**Colțurile:** butoane și pastile `rounded-full` · câmpuri `rounded-xl` · carduri, tabele și carduri de
alegere `rounded-2xl` · dialoguri `rounded-3xl`.

**Fără temă întunecată.** Cerut explicit, de două ori.

---

## Primitivele — `frontend/src/components/ui`

| Primitivă | Când |
|---|---|
| `Button`, `LinkButton` | Orice acțiune. Pastilă, text îngroșat. `default` = pasul principal al ecranului (unul singur), `outline` = celelalte, `ghost` = acțiunile de rând, `danger` = ce nu se ia înapoi |
| `Input`, `Select`, `Textarea` | Rubrici. Au aceleași clase (`fieldClasses` din `input.tsx`): chenar de 2px, halou verde la focus |
| `ChoiceCards` | **O alegere între 2–6 variante**, fiecare cu o propoziție care spune ce urmează. Radio nativ dedesubt. Exemplu: „Cine vede prețurile” în Setări |
| `PillGroup` | Răspunsuri scurte, de un cuvânt sau două, simple sau multiple. Codul oficial stă mic lângă cuvinte („Container transportabil · CT”) |
| `Switch` | Un da/nu cu o propoziție, în locul unei bife cu trei rânduri de explicație |
| `Stepper` | Un formular împărțit pe întrebări. **Împarte, nu rearanjează** ordinea rubricilor |
| `Card`, `CardHeader` | Orice bloc de conținut |
| `Dialog` | Formulare scurte și confirmări. Pe telefon urcă de jos |
| `FormSection` | Titlul unei secțiuni dintr-un formular: un titlu citit, nu o etichetă cu majuscule |
| `PageHeader` | Titlul paginii, o propoziție sub el, acțiunile la dreapta. Pe taburi, propoziția spune ce e **tabul** |
| `Badge` | Starea unui rând, în cuvinte („Poate vinde metal”, „De cântărit”) |
| `Tooltip` | Orice explicație la cerere. **Niciodată `title=`**: nu se citește pe telefon și nici din tastatură |
| `Table` | Liste. Coloanele de acțiuni se fixează la dreapta (`sticky="right"`) |

Select-ul nativ rămâne pentru liste lungi (parteneri, puncte de lucru, coduri R/D din profil). **Sub șapte
opțiuni, într-un formular nou, se folosesc `ChoiceCards` sau `PillGroup`.**

---

## Cele douăsprezece reguli ale formularelor

1. **O întrebare pe ecran, cu verb.** „Cât ai predat?”, nu „Cantitate”. Pașii păstrează ordinea stabilită cu specialista.
2. **Carduri în loc de liste derulante**, sub șapte opțiuni.
3. **Codul legii rămâne, dar mic.** „Container transportabil” mare, `CT` mic. Pe hârtie se tipărește codul.
4. **„Ca data trecută” e un buton (varianta A, decizia proprietarului, 15.09.2026).** Aplicația propune ce s-a ales la predarea anterioară pe același cod și aplică numai la clic. Nimic nu pornește bifat, nicio cifră nu se copiază.
5. **CUI-ul completează firma** (ANAF, `GET /api/v1/company-lookup/{cui}`), numai în rubricile goale.
6. **Poza în loc de tastat:** bonul de cântar, avizul, autorizația — prin atașamentele existente.
7. **Ciorna se salvează singură** (`useFormDraft`).
8. **„Nu știu” e un răspuns bun** acolo unde golul e valid, și are nume: „Nu l-am tratat, doar l-am predat”.
9. **Erori care spun ce să faci**, cu exemplu, la ieșirea din rubrică, nu abia la salvare.
10. **Explicația legală sub „De ce?”**, nu în titlu și nu în propoziția de sub titlu.
11. **Bonul din dreapta:** ce s-a completat și ce documente ies din răspunsuri.
12. **Salvarea spune pasul următor** („Tipărește Anexa 3”, „Încă una la fel”).

## Limbajul

- Textele stau în `frontend/src/lib/strings.ts`, în română, și numesc **ce face omul**: „Am predat deșeuri”, „Colectori și clienți”, „Poate vinde metal”.
- Temeiul legal (actul, articolul) se păstrează, dar în explicație, nu în etichetă.
- Numeralul are trei forme (`1 linie · 2 linii · 20 de linii`) — trece prin `countOf` / `withCount`.

## Ce nu se schimbă, orice stil

- Ordinea rubricilor din formularul de mișcare (stabilită cu specialista).
- Documentele tipărite: numele din acte rămân pe hârtie. Limbajul de om e doar pe ecran.
- Nicio cifră ghicită sau precompletată pe un formular oficial.
- Roșu ≠ galben.

---

## Feliile

| # | Ce | Stare |
|---|---|---|
| 1 | Tokenii, fontul, primitivele restilizate, primitivele noi (`ChoiceCards`, `PillGroup`, `Switch`, `Stepper`), bara laterală; „Cine vede prețurile” pe carduri; căutarea după CUI la ANAF | ✅ 15.09.2026 |
| 2 | Predarea în pași pe generator, cu codurile firmei ca butoane și „Ca data trecută” (A) | ⬜ |
| 3 | Mesajul de după salvare cu pasul următor; „A venit bonul de cântar?” | ⬜ |
| 4 | Formularul de partener pe trei ecrane (Cine e · Ce face · Autorizația) | ⬜ |
| 5 | Cererea de cont pe pași | ⬜ |
| D1.15 | Operațiunea de depozit (intrare la cântar, ieșire) se construiește **direct** în stilul ăsta, cu primitivele din felia 1 | ⬜ |

Machetele celor cinci formulare (predarea, partenerul, cererea de cont, intrarea la cântar) stau în
documentele private (`todo-ui-prietenos.md`).

## Cum se probează

`tsc` și build-ul nu văd cum arată un ecran. O schimbare de interfață se rulează prin `npm run e2e`
(`frontend/e2e/README.md`), iar capturile din `frontend/e2e/shots/` se **deschid și se privesc**, la 1280px
și la 375px.
