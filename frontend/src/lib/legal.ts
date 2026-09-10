/**
 * Textul celor două documente publice: termenii și politica de confidențialitate.
 *
 * <p>Nu stau în `strings.ts`, deși și ele sunt text de ecran, și e o despărțire voită. `strings.ts`
 * ține **eticheta** — un cuvânt pe un buton, o explicație de sub o rubrică — care se schimbă când
 * se schimbă ecranul. Astea două sunt **documente**: se schimbă când se schimbă contractul sau
 * legea, se anunță cu 30 de zile înainte (cap. 14 din termeni), și se citesc de la cap la coadă.
 * Amestecate, primele ar fi îngropat pe ultimele.
 *
 * <p>Sursa lor e `ecoregistru-docs/docs/juridic/` — repo-ul privat, unde stau împreună cu
 * contractul-cadru și cu DPA-ul, care **nu** se publică. Aici ajung doar cele două care se publică
 * prin definiție. Când se schimbă acolo, se schimbă și aici, în aceeași zi.
 */

/** Ce poate să conțină un capitol. Cât ne trebuie ca să scriem exact documentele astea, nimic mai mult. */
export type LegalBlock =
  | { kind: "p"; text: string }
  | { kind: "h3"; text: string }
  | { kind: "list"; items: string[] }
  | { kind: "table"; head: string[]; rows: string[][] }
  /** Un paragraf care se scoate în evidență: avertismentul de la retenție, excepția de la Cloudinary. */
  | { kind: "note"; text: string };

export interface LegalSection {
  /** Ancora din adresă (`/termeni#raspundere`) și cheia din cuprins. */
  id: string;
  heading: string;
  blocks: LegalBlock[];
}

export interface LegalDoc {
  title: string;
  /** „În vigoare de la" la termeni, „Ultima actualizare" la politică — nu e același lucru. */
  dateLabel: string;
  sections: LegalSection[];
}

/**
 * Ziua de la care sunt în vigoare textele astea.
 *
 * <p>⚠️ **Se pune ziua publicării, nu ziua în care s-a scris fișierul.** E o dată cu efect juridic:
 * de la ea curg cele 30 de zile de preaviz la o modificare (cap. 14 din termeni). Dacă deployul
 * ajunge în altă zi, se schimbă aici — într-un singur loc, pentru amândouă documentele.
 */
export const LEGAL_DATE = "11 septembrie 2026";

const COMPANY =
  "**ONSIA S.R.L.**, societate română cu sediul în sat Sântandrei, comuna Sântandrei, " +
  "Str. Făcliei nr. 79, județul Bihor, înregistrată la Oficiul Registrului Comerțului de pe lângă " +
  "Tribunalul Bihor sub nr. J2025033991008, cod unic de înregistrare **51779887**, " +
  "EUID ROONRC.J2025033991008.";

export const TERMS: LegalDoc = {
  title: "Termeni și condiții de utilizare",
  dateLabel: "În vigoare de la",
  sections: [
    {
      id: "cine-ofera-serviciul",
      heading: "1. Cine oferă serviciul",
      blocks: [
        { kind: "p", text: "**EcoRegistru** este o aplicație pusă la dispoziție de " + COMPANY },
        { kind: "p", text: "Contact: **whoecom@gmail.com**." },
      ],
    },
    {
      id: "acceptare",
      heading: "2. Ce acceptați citind mai departe",
      blocks: [
        {
          kind: "p",
          text:
            "Prin crearea unui cont sau prin utilizarea aplicației, acceptați termenii de mai jos. " +
            "Dacă nu îi acceptați, nu folosiți aplicația.",
        },
        {
          kind: "p",
          text:
            "Termenii se completează cu **contractul de prestări servicii** semnat cu dumneavoastră. " +
            "**În caz de neconcordanță, contractul semnat prevalează.**",
        },
      ],
    },
    {
      id: "cui-se-adreseaza",
      heading: "3. Cui i se adresează serviciul",
      blocks: [
        {
          kind: "p",
          text:
            "EcoRegistru este un serviciu **destinat exclusiv profesioniștilor** — operatori economici, " +
            "consultanți de mediu și alte entități care au obligații legale de evidență a gestiunii deșeurilor.",
        },
        {
          kind: "p",
          text:
            "**Nu se adresează consumatorilor** în sensul legislației de protecție a consumatorilor. " +
            "Prin urmare, dreptul de retragere în 14 zile prevăzut pentru contractele la distanță încheiate " +
            "cu consumatorii **nu se aplică**.",
        },
      ],
    },
    {
      id: "ce-face",
      heading: "4. Ce face aplicația",
      blocks: [
        {
          kind: "p",
          text:
            "EcoRegistru vă ajută să **țineți evidența** gestiunii deșeurilor și **pregătește documentele** " +
            "cerute de legislația română: fișa de evidență a gestiunii deșeurilor, declarația anuală, " +
            "formularele de transport al deșeurilor (inclusiv cele pentru deșeuri periculoase), declarațiile " +
            "de ambalaje și celelalte documente pe care aplicația le pune la dispoziție la un moment dat.",
        },
        { kind: "p", text: "Aplicația ține și **calendarul termenelor legale**, cu alerte." },
      ],
    },
    {
      id: "ce-nu-face",
      heading: "5. Ce NU face aplicația — citiți acest capitol chiar dacă le săriți pe celelalte",
      blocks: [
        {
          kind: "p",
          text:
            "**5.1. Nu transmitem nimic autorităților.** EcoRegistru **nu are** și nu poate avea o conexiune " +
            "automată cu Sistemul Integrat de Mediu, cu ANMAP, cu AFM sau cu orice alt sistem al autorităților " +
            "publice, fiindcă autoritățile române nu pun la dispoziția terților o astfel de interfață. " +
            "**Documentele le depuneți dumneavoastră.** Aplicația vi le pregătește în forma cerută; drumul până " +
            "la autoritate îl faceți dumneavoastră.",
        },
        {
          kind: "p",
          text:
            "**5.2. Nu suntem consultanți de mediu.** Nu interpretăm legislația în locul dumneavoastră, nu " +
            "alegem codurile de deșeuri, nu stabilim ce obligații aveți și nu preluăm rolul de persoană " +
            "desemnată cu gestiunea deșeurilor.",
        },
        {
          kind: "p",
          text:
            "**5.3. Documentele reflectă exact ce ați introdus.** Nu verificăm și nu corectăm datele " +
            "dumneavoastră. Un document generat dintr-o dată greșită va fi un document greșit.",
        },
        {
          kind: "p",
          text:
            "**5.4. Verificați înainte de a semna.** Fiecare document generat trebuie citit și verificat " +
            "înainte de a fi semnat sau depus. Semnătura de pe el e a dumneavoastră.",
        },
        {
          kind: "p",
          text:
            "**5.5. Nu garantăm evitarea sancțiunilor.** Nu promitem că nu veți fi sancționat la un control. " +
            "Aplicația vă ajută să vă țineți evidența în ordine — ceea ce e cu totul altceva decât o garanție, " +
            "iar noi nu vindem garanții de acest fel.",
        },
      ],
    },
    {
      id: "contul",
      heading: "6. Contul dumneavoastră",
      blocks: [
        { kind: "p", text: "**6.1.** Conturile se creează la cerere. Ne rezervăm dreptul de a refuza o cerere, motivat." },
        {
          kind: "p",
          text:
            "**6.2.** Sunteți răspunzător de păstrarea în siguranță a parolei și de tot ce se întâmplă prin " +
            "contul dumneavoastră. Anunțați-ne imediat dacă bănuiți o utilizare neautorizată.",
        },
        {
          kind: "p",
          text:
            "**6.3.** Administratorul firmei dumneavoastră poate crea, dezactiva și schimba rolul " +
            "utilizatorilor din firmă. Ce fac ei prin aplicație e responsabilitatea firmei.",
        },
        { kind: "p", text: "**6.4.** Sesiunea expiră automat după **8 ore**." },
      ],
    },
    {
      id: "interdictii",
      heading: "7. Ce nu aveți voie să faceți",
      blocks: [
        {
          kind: "list",
          items: [
            "să accesați sau să încercați să accesați datele altui client;",
            "să ocoliți sau să testați măsurile de securitate fără acordul nostru scris;",
            "să decompilați, să dezasamblați sau să încercați extragerea codului sursă;",
            "să folosiți aplicația pentru a construi un produs concurent;",
            "să încărcați fișiere cu conținut ilegal, cu programe dăunătoare, sau date personale pe care nu aveți dreptul să le prelucrați;",
            "să suprasolicitați deliberat serviciul sau să îl folosiți automatizat, în afara interfeței puse la dispoziție.",
          ],
        },
        { kind: "p", text: "Încălcarea acestor reguli poate duce la suspendarea imediată a accesului." },
      ],
    },
    {
      id: "datele-va-apartin",
      heading: "8. Datele dumneavoastră vă aparțin",
      blocks: [
        {
          kind: "p",
          text:
            "**8.1.** Tot ce introduceți în aplicație rămâne al dumneavoastră. Nu dobândim niciun drept asupra " +
            "acestor date și nu le folosim în alt scop decât prestarea serviciului.",
        },
        { kind: "p", text: "**8.2.** Le puteți exporta oricând, în formatele pe care aplicația le pune la dispoziție." },
        {
          kind: "p",
          text:
            "**8.3.** Prelucrarea datelor cu caracter personal e descrisă în **Politica de confidențialitate** " +
            "și, pentru clienți, în **contractul de împuternicire** semnat potrivit art. 28 GDPR.",
        },
        {
          kind: "p",
          text:
            "**8.4.** Putem folosi date **agregate și anonimizate**, din care nu se poate identifica nici firma " +
            "dumneavoastră, nici o persoană, pentru a îmbunătăți serviciul.",
        },
      ],
    },
    {
      id: "drepturile-noastre",
      heading: "9. Drepturile noastre asupra aplicației",
      blocks: [
        {
          kind: "p",
          text:
            "Aplicația, codul, structura ei, machetele documentelor și marca EcoRegistru ne aparțin. Primiți un " +
            "drept de utilizare neexclusiv, netransferabil, pe durata abonamentului. Atât — nimic din ce e al " +
            "nostru nu vă este transferat.",
        },
      ],
    },
    {
      id: "disponibilitate",
      heading: "10. Disponibilitate și întreruperi",
      blocks: [
        {
          kind: "p",
          text:
            "**10.1.** Ne străduim ca aplicația să fie disponibilă permanent, dar nu putem garanta funcționarea " +
            "neîntreruptă.",
        },
        {
          kind: "p",
          text:
            "**10.2.** Mentenanța planificată se anunță cu cel puțin 48 de ore înainte și se face, pe cât " +
            "posibil, în afara programului de lucru.",
        },
        { kind: "p", text: "**10.3.** Nivelul de serviciu convenit contractual e în anexa contractului dumneavoastră." },
      ],
    },
    {
      id: "preturi",
      heading: "11. Prețuri și plată",
      blocks: [
        { kind: "p", text: "**11.1.** Prețurile sunt cele din contractul semnat." },
        { kind: "p", text: "**11.2.** Facturile se emit prin **RO e-Factura** și se plătesc în termenul din contract." },
        {
          kind: "p",
          text:
            "**11.3.** Putem suspenda accesul pentru facturi neachitate, după notificare prealabilă. " +
            "**Datele nu se șterg pe durata suspendării.**",
        },
      ],
    },
    {
      id: "raspundere",
      heading: "12. Răspundere",
      blocks: [
        { kind: "p", text: "**12.1.** Răspundem pentru ca aplicația să funcționeze conform descrierii de la capitolele 4 și 5." },
        {
          kind: "p",
          text:
            "**12.2.** Nu răspundem pentru datele introduse de dumneavoastră, pentru interpretarea legislației, " +
            "pentru nedepunerea sau depunerea eronată a documentelor, ori pentru sancțiunile care decurg din acestea.",
        },
        {
          kind: "p",
          text:
            "**12.3.** Răspunderea noastră totală este limitată la contravaloarea serviciilor facturate în " +
            "ultimele 12 luni.",
        },
        {
          kind: "p",
          text:
            "**12.4.** Limitele de mai sus **nu se aplică** prejudiciului cauzat cu intenție sau din culpă gravă, " +
            "potrivit art. 1355 din Codul civil, și nu afectează drepturile prevăzute de GDPR.",
        },
      ],
    },
    {
      id: "incetare",
      heading: "13. Încetare",
      blocks: [
        { kind: "p", text: "**13.1.** Puteți renunța oricând, cu preavizul din contract." },
        { kind: "p", text: "**13.2.** După încetare aveți **30 de zile** ca să vă exportați datele. Apoi le ștergem." },
        {
          kind: "note",
          text:
            "**13.3.** Obligația de a păstra evidența **cel puțin 3 ani** (12 luni pentru transport), potrivit " +
            "art. 48 alin. (5) din OUG nr. 92/2021, rămâne a dumneavoastră. Exportați înainte.",
        },
      ],
    },
    {
      id: "modificare",
      heading: "14. Modificarea termenilor",
      blocks: [
        {
          kind: "p",
          text:
            "Putem modifica acești termeni. Modificările importante se anunță cu **30 de zile** înainte, prin " +
            "e-mail sau în aplicație. Dacă nu le acceptați, puteți denunța contractul până la data intrării lor " +
            "în vigoare.",
        },
      ],
    },
    {
      id: "legea-aplicabila",
      heading: "15. Legea aplicabilă",
      blocks: [
        {
          kind: "p",
          text:
            "Se aplică **legea română**. Litigiile se încearcă mai întâi pe cale amiabilă; dacă nu reușim, " +
            "competente sunt instanțele de la sediul nostru.",
        },
      ],
    },
    {
      id: "contact",
      heading: "16. Contact",
      blocks: [
        {
          kind: "p",
          text:
            "Pentru orice întrebare despre acești termeni: **whoecom@gmail.com**, sau ONSIA S.R.L., sat " +
            "Sântandrei, comuna Sântandrei, Str. Făcliei nr. 79, județul Bihor.",
        },
      ],
    },
  ],
};

export const PRIVACY: LegalDoc = {
  title: "Politica de confidențialitate",
  dateLabel: "Ultima actualizare",
  sections: [
    {
      id: "cine-suntem",
      heading: "Cine suntem",
      blocks: [
        { kind: "p", text: COMPANY.replace(/\.$/, "") + ", operatorul aplicației **EcoRegistru**." },
        {
          kind: "p",
          text:
            "**Contact pentru orice chestiune privind datele personale:** **whoecom@gmail.com** — adresa " +
            "înregistrată a societății.",
        },
        {
          kind: "p",
          text:
            "E-mailurile trimise automat de aplicație — invitații, resetare de parolă, alerte — pleacă de la " +
            "`contact@ecoregistru.ro`. Cererile privind datele personale se trimit la adresa de mai sus.",
        },
        {
          kind: "p",
          text:
            "Nu avem obligația legală de a desemna un responsabil cu protecția datelor (art. 37 GDPR), fiindcă " +
            "nu facem monitorizare sistematică pe scară largă și nu prelucrăm categorii speciale de date. " +
            "Cererile merg la adresa de mai sus și primesc răspuns de la noi.",
        },
      ],
    },
    {
      id: "doua-roluri",
      heading: "Două roluri diferite, și de ce contează care e care",
      blocks: [
        {
          kind: "p",
          text:
            "Politica aceasta acoperă **două situații**, iar drepturile dumneavoastră se exercită diferit după " +
            "cum e vorba de una sau de alta.",
        },
        { kind: "h3", text: "1. Datele pe care le prelucrăm în nume propriu (suntem operator)" },
        {
          kind: "p",
          text:
            "Sunt datele oamenilor care intră în legătură cu noi: vizitatorii site-ului, cei care cer un cont, " +
            "persoanele de contact ale firmelor cu care avem contract. Pentru ele răspundem noi, direct, iar " +
            "capitolele de mai jos le descriu.",
        },
        { kind: "h3", text: "2. Datele din aplicație, introduse de clienții noștri (suntem persoană împuternicită)" },
        {
          kind: "p",
          text:
            "Tot ce introduce un client în aplicație — evidența deșeurilor, conducătorii auto, persoanele de " +
            "contact, fișierele atașate — **îi aparține lui**. Noi doar le găzduim și le prelucrăm la " +
            "instrucțiunea lui, pe baza unui contract de împuternicire încheiat potrivit art. 28 GDPR.",
        },
        {
          kind: "p",
          text:
            "**Ce înseamnă asta practic pentru dumneavoastră.** Dacă datele dumneavoastră au ajuns în aplicație " +
            "pentru că firma la care lucrați (sau pentru care conduceți un transport) ține evidența acolo, " +
            "**operatorul e acea firmă, nu noi**. Cererea dumneavoastră — de acces, de ștergere, de rectificare — " +
            "se adresează ei. Dacă ne ajunge nouă, o transmitem ei fără întârziere și vă anunțăm; nu putem " +
            "răspunde pe fond în locul ei, fiindcă nu noi decidem ce se întâmplă cu acele date.",
        },
      ],
    },
    {
      id: "ce-date",
      heading: "Ce date prelucrăm în nume propriu, de ce, și în ce temei",
      blocks: [
        {
          kind: "table",
          head: ["Ce", "De ce", "Temei (art. 6 GDPR)", "Cât păstrăm"],
          rows: [
            [
              "**Cererea de cont**: denumirea firmei, CUI, adresă, nume și date de contact ale persoanei care cere, informații despre activitate",
              "Ca să evaluăm cererea și să deschidem contul",
              "Măsuri precontractuale, art. 6(1)(b)",
              "12 luni de la soluționare, dacă nu s-a încheiat un contract",
            ],
            [
              "**Contul de utilizator**: e-mail, nume, prenume, rol",
              "Ca să existe accesul, autentificarea și comunicarea legată de serviciu",
              "Executarea contractului, art. 6(1)(b)",
              "Durata contractului + 30 de zile",
            ],
            [
              "**Corespondența** cu noi: mesaje, cereri de suport",
              "Ca să răspundem și să ținem evidența a ce s-a cerut",
              "Interes legitim, art. 6(1)(f) — să putem dovedi ce am răspuns",
              "3 ani",
            ],
            [
              "**Datele de facturare**: denumire, CUI, adresă, sume",
              "Ca să emitem factura și să ținem contabilitatea",
              "Obligație legală, art. 6(1)(c)",
              "10 ani, potrivit legii contabilității",
            ],
            [
              "**Date tehnice**: adresă IP, momentul cererii, tipul browserului, rapoarte de eroare",
              "Securitatea serviciului și repararea defectelor",
              "Interes legitim, art. 6(1)(f) — un serviciu care funcționează și nu e atacat",
              "90 de zile",
            ],
          ],
        },
        { kind: "p", text: "Nu vă cerem date de care nu avem nevoie, și nu vă cerem CNP-ul." },
      ],
    },
    {
      id: "cui-transmitem",
      heading: "Cui transmitem datele",
      blocks: [
        { kind: "p", text: "Nu vindem și nu închiriem date. Nu facem publicitate cu ele." },
        {
          kind: "p",
          text:
            "Le transmitem doar furnizorilor care ne fac serviciul să funcționeze, fiecare pe bază de contract, " +
            "fiecare doar cu ce îi trebuie:",
        },
        {
          kind: "table",
          head: ["Furnizor", "Pentru ce", "Unde stau datele"],
          rows: [
            ["**Heroku (Salesforce)**", "Găzduirea aplicației și baza de date", "**Uniunea Europeană**"],
            ["**Sentry**", "Rapoarte de eroare", "**Uniunea Europeană** (Germania)"],
            ["**Zoho**", "Trimiterea e-mailurilor", "**Uniunea Europeană**"],
            ["**Cloudinary**", "Fișierele atașate în aplicație", "**Statele Unite**"],
          ],
        },
        { kind: "p", text: "**Baza de date, e-mailurile și rapoartele de eroare sunt stocate în Uniunea Europeană.**" },
        {
          kind: "note",
          text:
            "**Fișierele pe care le atașați în aplicație se stochează la Cloudinary, în Statele Unite.** Vă " +
            "spunem asta deschis, fiindcă e singura excepție. Transferul este acoperit de **clauzele " +
            "contractuale standard** aprobate de Comisia Europeană (Decizia (UE) 2021/914) și de certificarea " +
            "furnizorului în **Cadrul UE–SUA privind confidențialitatea datelor**. Lista subîmputerniciților lui " +
            "este publică, la [cloudinary.com/subprocessors](https://cloudinary.com/subprocessors).",
        },
        {
          kind: "p",
          text:
            "**Heroku, Sentry și Cloudinary** au societate-mamă în Statele Unite. Pentru primele două asta nu " +
            "schimbă locul stocării, care rămâne în Uniune; pentru Cloudinary, transferul e acoperit așa cum am " +
            "spus mai sus.",
        },
        {
          kind: "p",
          text:
            "Mai transmitem date **autorităților publice**, atunci când legea ne obligă și în limita a ce ni se cere.",
        },
      ],
    },
    {
      id: "cookieuri",
      heading: "Cookie-uri și stocare locală",
      blocks: [
        {
          kind: "p",
          text:
            "**Nu folosim cookie-uri de urmărire, nu avem instrumente de analiză a traficului și nu avem " +
            "publicitate.** De aceea nu vedeți la noi o casetă de consimțământ pentru cookie-uri: n-am pus nimic " +
            "pentru care să fie nevoie de ea.",
        },
        {
          kind: "p",
          text:
            "Aplicația folosește **stocarea locală a browserului** (`localStorage`), pentru două lucruri, ambele " +
            "strict necesare funcționării:",
        },
        {
          kind: "list",
          items: [
            "**jetonul de sesiune** — ca să rămâneți autentificat între pagini; expiră în 8 ore;",
            "**ciornele de formular** — ca să nu pierdeți ce ați scris dacă închideți din greșeală pagina.",
          ],
        },
        {
          kind: "p",
          text:
            "Ambele stau **în browserul dumneavoastră** și se șterg la deconectare sau când goliți datele " +
            "site-ului.",
        },
      ],
    },
    {
      id: "cat-pastram",
      heading: "Cât timp păstrăm datele",
      blocks: [
        {
          kind: "p",
          text:
            "Termenele sunt în tabelul de mai sus. La încetarea contractului, datele din aplicație rămân " +
            "disponibile pentru export **30 de zile**, apoi se șterg din sistemele active; copiile de siguranță " +
            "se sting în cel mult 30 de zile de la acel moment.",
        },
        {
          kind: "note",
          text:
            "**Atenție, pentru clienți:** obligația legală de a păstra evidența gestiunii deșeurilor **cel puțin " +
            "3 ani** (12 luni pentru activitățile de transport), potrivit art. 48 alin. (5) din OUG nr. 92/2021, " +
            "este a dumneavoastră, nu a noastră. Noi nu suntem arhivă legală. Exportați datele înainte de ștergere.",
        },
      ],
    },
    {
      id: "drepturi",
      heading: "Drepturile dumneavoastră",
      blocks: [
        { kind: "p", text: "Aveți, potrivit GDPR:" },
        {
          kind: "list",
          items: [
            "**dreptul de acces** — să aflați ce date avem despre dumneavoastră și să primiți o copie;",
            "**dreptul la rectificare** — să corectăm ce e greșit sau incomplet;",
            "**dreptul la ștergere** — în situațiile prevăzute de art. 17 GDPR;",
            "**dreptul la restricționarea prelucrării** — art. 18;",
            "**dreptul la portabilitate** — să primiți datele într-un format citibil de o mașină, sau să le transmitem altcuiva, când e tehnic posibil;",
            "**dreptul la opoziție** — față de prelucrările întemeiate pe interesul nostru legitim;",
            "**dreptul de a nu fi supus unei decizii automate** — nu luăm astfel de decizii;",
            "**dreptul de a vă retrage consimțământul**, acolo unde el a fost temeiul, fără ca asta să afecteze ce s-a prelucrat legal înainte.",
          ],
        },
        {
          kind: "p",
          text:
            "**Cum le exercitați:** scrieți la **whoecom@gmail.com**. Răspundem în **cel mult o lună** de la " +
            "primirea cererii. Dacă cererea e complexă, termenul se poate prelungi cu două luni, și vă anunțăm " +
            "în prima lună de ce.",
        },
        { kind: "p", text: "Vă putem cere să vă dovediți identitatea, ca să nu dăm datele dumneavoastră altcuiva." },
        {
          kind: "p",
          text:
            "**Dacă credeți că v-am încălcat drepturile**, puteți depune plângere la **Autoritatea Națională de " +
            "Supraveghere a Prelucrării Datelor cu Caracter Personal (ANSPDCP)**, B-dul G-ral. Gheorghe Magheru " +
            "nr. 28-30, sector 1, București, [www.dataprotection.ro](https://www.dataprotection.ro), și vă puteți " +
            "adresa instanței. Ne-am bucura să încercați mai întâi cu noi.",
        },
      ],
    },
    {
      id: "securitate",
      heading: "Cum protejăm datele",
      blocks: [
        {
          kind: "p",
          text:
            "Pe scurt, ce e implementat efectiv: conexiuni criptate (HTTPS), parole stocate doar ca amprentă " +
            "criptografică, sesiuni de 8 ore cu revocare imediată la dezactivarea contului, izolare completă " +
            "între datele firmelor, atașamente cu acces restricționat livrate prin adrese semnate cu durată " +
            "limitată, limitarea încercărilor de autentificare, copii de siguranță zilnice cu procedură de " +
            "restaurare verificată, și un colector de erori care ne anunță când ceva se strică.",
        },
        {
          kind: "p",
          text:
            "Descrierea completă a măsurilor stă în contractul de împuternicire pe care îl semnăm cu fiecare " +
            "client, și v-o putem pune la dispoziție.",
        },
      ],
    },
    {
      id: "modificari",
      heading: "Modificări ale acestei politici",
      blocks: [
        {
          kind: "p",
          text:
            "Când o modificăm, schimbăm data de la început. Dacă modificarea e importantă, anunțăm clienții prin " +
            "e-mail cu cel puțin 30 de zile înainte.",
        },
      ],
    },
  ],
};
