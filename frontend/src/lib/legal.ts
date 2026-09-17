/**
 * Textul celor două documente publice: termenii și politica de confidențialitate.
 *
 * <p>Nu stau în `strings.ts`, deși și ele sunt text de ecran, și e o despărțire voită. `strings.ts`
 * ține **eticheta** — un cuvânt pe un buton, o explicație de sub o rubrică — care se schimbă când
 * se schimbă ecranul. Astea două sunt **documente**: se schimbă când se schimbă contractul sau
 * legea, se anunță cu 30 de zile înainte (cap. 15 din termeni), și se citesc de la cap la coadă.
 * Amestecate, primele ar fi îngropat pe ultimele.
 *
 * <p>Sursa lor e `ecoregistru-docs/docs/juridic/` — repo-ul privat, unde stau împreună cu
 * contractul-cadru și cu DPA-ul, care **nu** se publică. Aici ajung doar cele două care se publică
 * prin definiție. Când se schimbă acolo, se schimbă și aici, în aceeași zi.
 *
 * <p>v2, 15.09.2026: plata (perioade lunare, factura în avans, card sau transfer, cardul salvat,
 * doar-citirea la neplată), oprirea și rambursările, utilizatorii invitați, datele după încetare;
 * în politică: datele de facturare și de plată, NETOPIA, FGO, ANAF, a treia intrare din stocarea
 * locală. Termenii au 17 capitole (erau 16): proba de ecran 12 numără la fel.
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
 * de la ea curg cele 30 de zile de preaviz la o modificare (cap. 15 din termeni). Dacă deployul
 * ajunge în altă zi, se schimbă aici — într-un singur loc, pentru amândouă documentele — și în cele
 * două rânduri de dată din `juridic/`.
 */
export const LEGAL_DATE = "15 septembrie 2026";

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
        { kind: "p", text: "**WasteHouse** (app.wastehouse.ro) este o aplicație pusă la dispoziție de " + COMPANY },
        { kind: "p", text: "Contact: **contact@wastehouse.ro**." },
      ],
    },
    {
      id: "acceptare",
      heading: "2. Ce acceptați citind mai departe",
      blocks: [
        {
          kind: "p",
          text:
            "Prin crearea unui cont, prin alegerea parolei la o invitație sau prin utilizarea aplicației, " +
            "acceptați termenii de mai jos. Dacă nu îi acceptați, nu folosiți aplicația.",
        },
        {
          kind: "p",
          text:
            "Termenii se completează cu **contractul de prestări servicii** semnat cu firma dumneavoastră. " +
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
            "WasteHouse este un serviciu **destinat exclusiv profesioniștilor** — operatori economici, " +
            "consultanți de mediu și alte entități care au obligații legale de evidență a gestiunii deșeurilor. " +
            "Folosind aplicația, declarați că acționați în numele unui astfel de profesionist.",
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
            "WasteHouse vă ajută să **țineți evidența** gestiunii deșeurilor și **pregătește documentele** " +
            "cerute de legislația română, în forma cerută la data generării: fișa de evidență a gestiunii " +
            "deșeurilor, evidența centralizată anuală, formularele de transport al deșeurilor (inclusiv cele " +
            "pentru deșeuri periculoase) și avizul de însoțire a mărfii, evidența cronologică a colectorilor, " +
            "declarațiile de ambalaje, dosarul de control și celelalte documente pe care aplicația le pune la " +
            "dispoziție la un moment dat.",
        },
        {
          kind: "p",
          text:
            "Aplicația ține și **calendarul termenelor legale**, cu alerte, și poate importa evidența existentă " +
            "din fișiere Excel în formatul acceptat. Alertele sunt informative și nu înlocuiesc urmărirea " +
            "termenelor de către dumneavoastră.",
        },
      ],
    },
    {
      id: "ce-nu-face",
      heading: "5. Ce NU face aplicația — citiți acest capitol chiar dacă le săriți pe celelalte",
      blocks: [
        {
          kind: "p",
          text:
            "**5.1. Nu transmitem nimic autorităților.** WasteHouse **nu are** o conexiune automată cu Sistemul " +
            "Integrat de Mediu, cu Administrația Fondului pentru Mediu sau cu orice alt sistem al autorităților " +
            "publice. **Documentele le depuneți dumneavoastră.** Aplicația vi le pregătește în forma cerută; " +
            "drumul până la autoritate îl faceți dumneavoastră.",
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
            "dumneavoastră. Un document generat dintr-o dată greșită va fi un document greșit. Verificările " +
            "automate ale aplicației sunt un ajutor, nu o garanție.",
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
        {
          kind: "p",
          text:
            "**6.1.** Conturile se creează la cerere, de către noi, după semnarea contractului. Ne rezervăm " +
            "dreptul de a refuza o cerere, motivat.",
        },
        {
          kind: "p",
          text:
            "**6.2.** Sunteți răspunzător de păstrarea în siguranță a parolei și de tot ce se întâmplă prin " +
            "contul dumneavoastră. Anunțați-ne imediat dacă bănuiți o utilizare neautorizată.",
        },
        {
          kind: "p",
          text:
            "**6.3.** Administratorul firmei dumneavoastră (sau cabinetul de consultanță care ține evidența ei) " +
            "poate crea, dezactiva și schimba rolul utilizatorilor din firmă. Ce fac ei prin aplicație e " +
            "responsabilitatea firmei.",
        },
        {
          kind: "p",
          text:
            "**6.4.** Dacă ați fost invitat, alegerea parolei activează contul și înseamnă că ați citit acești " +
            "termeni și politica de confidențialitate.",
        },
        { kind: "p", text: "**6.5.** Sesiunea expiră automat după **8 ore**." },
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
            "și, pentru clienți, în **contractul de împuternicire** semnat potrivit art. 28 GDPR. Datele " +
            "personale ale terților pe care le introduceți (de exemplu conducătorii auto) le introduceți pe " +
            "răspunderea firmei dumneavoastră, care trebuie să aibă temei pentru ele și să informeze persoanele " +
            "vizate.",
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
            "Aplicația, codul, structura ei, machetele documentelor și marca WasteHouse ne aparțin. Primiți un " +
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
        {
          kind: "p",
          text:
            "**11.1.** Prețurile sunt cele din contractul semnat, în lei, fără TVA (nu suntem înregistrați în " +
            "scopuri de TVA; prețul din contract e prețul de pe factură).",
        },
        {
          kind: "p",
          text:
            "**11.2.** Abonamentul se plătește pe **perioade de o lună**, care încep în ziua de start a " +
            "abonamentului. Factura se emite **în prima zi a fiecărei perioade, în avans**, se transmite prin " +
            "**RO e-Factura** și vine și pe e-mail, la adresa de facturare. O puteți descărca oricând din " +
            "ecranul „Abonament”.",
        },
        { kind: "p", text: "**11.3.** Termenul de plată este de **10 zile de la emiterea facturii**." },
        {
          kind: "p",
          text:
            "**11.4.** Puteți plăti prin **transfer bancar**, în contul de pe factură, sau cu **cardul**, pe " +
            "pagina securizată a procesatorului de plăți **NETOPIA Payments**. Datele cardului le introduceți " +
            "numai acolo; noi nu le vedem și nu le stocăm.",
        },
        {
          kind: "p",
          text:
            "**11.5. Cardul salvat.** Dacă alegeți plata cu cardul și acceptați salvarea cardului pe pagina " +
            "procesatorului, ne autorizați să plătim automat, de pe acel card, **fiecare factură emisă**, la " +
            "valoarea ei, în ziua emiterii; dacă plata e refuzată, încercăm din nou în zilele a 3-a, a 6-a și a " +
            "10-a de la emitere și vă anunțăm prin e-mail la fiecare refuz. Nu debităm nicio sumă fără o factură " +
            "emisă și comunicată. Puteți revoca oricând autorizarea din ecranul „Abonament”, trecând pe transfer " +
            "bancar; cardul salvat se șterge atunci.",
        },
        {
          kind: "note",
          text:
            "**11.6. Neplata.** Dacă o factură nu e plătită la scadență, vă anunțăm prin e-mail a doua zi și, " +
            "din nou, cu 7 zile înainte de următorul pas. Dacă factura rămâne neplătită **15 zile după " +
            "scadență**, contul trece **automat în doar-citire**: vedeți și descărcați tot, puteți plăti, dar nu " +
            "mai puteți introduce sau modifica date. **Datele nu se șterg.** Contul revine automat la normal " +
            "când plata ajunge. Perioadele în care contul a stat în doar-citire se plătesc, câtă vreme " +
            "abonamentul nu a fost oprit.",
        },
      ],
    },
    {
      id: "oprire-rambursari",
      heading: "12. Oprirea abonamentului și rambursări",
      blocks: [
        {
          kind: "p",
          text:
            "**12.1.** Puteți opri abonamentul oricând, cu **preaviz de o lună**, din contract sau printr-un " +
            "e-mail la contact@wastehouse.ro trimis de la adresa de facturare. Ultima perioadă facturată e cea " +
            "în care cade ziua de peste o lună de la cerere. Nu există termen minim și nici penalitate de oprire.",
        },
        {
          kind: "p",
          text:
            "**12.2.** Sumele facturate pentru o perioadă începută **nu se restituie**. Taxa de implementare " +
            "plătită nu se restituie.",
        },
        {
          kind: "p",
          text:
            "**12.3.** O sumă luată din eroare (de două ori pentru aceeași factură, sau fără factură) o " +
            "restituim în cel mult **5 zile lucrătoare** de când o constatăm sau ne-o semnalați, pe același card " +
            "sau în același cont.",
        },
        {
          kind: "p",
          text:
            "**12.4.** După oprire, contul rămâne **90 de zile** în doar-citire, ca să vă exportați datele, și " +
            "puteți cere, în acest interval, păstrarea lui în doar-citire pentru cel mult 36 de luni. Apoi " +
            "datele se șterg (cap. 14).",
        },
      ],
    },
    {
      id: "raspundere",
      heading: "13. Răspundere",
      blocks: [
        { kind: "p", text: "**13.1.** Răspundem pentru ca aplicația să funcționeze conform descrierii de la capitolele 4 și 5." },
        {
          kind: "p",
          text:
            "**13.2.** Nu răspundem pentru datele introduse sau importate de dumneavoastră, pentru interpretarea " +
            "legislației, pentru nedepunerea sau depunerea eronată a documentelor, pentru sancțiunile care " +
            "decurg din acestea, ori pentru consecințele trecerii în doar-citire pentru neplată.",
        },
        {
          kind: "p",
          text:
            "**13.3.** Răspunderea noastră totală este limitată la contravaloarea serviciilor facturate în " +
            "ultimele 12 luni.",
        },
        {
          kind: "p",
          text:
            "**13.4.** Limitele de mai sus **nu se aplică** prejudiciului cauzat cu intenție sau din culpă gravă, " +
            "potrivit art. 1355 din Codul civil, și nu afectează drepturile prevăzute de GDPR.",
        },
      ],
    },
    {
      id: "incetare",
      heading: "14. Datele după încetare",
      blocks: [
        {
          kind: "p",
          text:
            "**14.1.** După încetarea contractului aveți **90 de zile** de doar-citire ca să vă exportați " +
            "datele, sau cel mult 36 de luni dacă ați cerut arhiva (cap. 12.4). Apoi le ștergem din sistemele " +
            "active, iar copiile de siguranță se sting în cel mult 30 de zile.",
        },
        {
          kind: "note",
          text:
            "**14.2.** Obligația de a păstra evidența **cel puțin 3 ani** (12 luni pentru transport), potrivit " +
            "art. 48 alin. (5) din OUG nr. 92/2021, rămâne a dumneavoastră. Exportați înainte.",
        },
      ],
    },
    {
      id: "modificare",
      heading: "15. Modificarea termenilor",
      blocks: [
        {
          kind: "p",
          text:
            "Putem modifica acești termeni. Modificările importante se anunță cu **30 de zile** înainte, prin " +
            "e-mail sau în aplicație. Dacă nu le acceptați, puteți opri abonamentul până la data intrării lor " +
            "în vigoare, fără preaviz.",
        },
      ],
    },
    {
      id: "legea-aplicabila",
      heading: "16. Legea aplicabilă",
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
      heading: "17. Contact",
      blocks: [
        {
          kind: "p",
          text:
            "Pentru orice întrebare despre acești termeni: **contact@wastehouse.ro**, sau ONSIA S.R.L., sat " +
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
        { kind: "p", text: COMPANY.replace(/\.$/, "") + ", operatorul aplicației **WasteHouse**." },
        {
          kind: "p",
          text:
            "**Contact pentru orice chestiune privind datele personale:** **contact@wastehouse.ro**.",
        },
        {
          kind: "p",
          text:
            "E-mailurile trimise automat de aplicație — invitații, resetare de parolă, alerte, facturi — pleacă " +
            "de la aceeași adresă.",
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
            "utilizatorii aplicației în relația lor cu noi, persoanele de contact și de facturare ale firmelor " +
            "cu care avem contract. Pentru ele răspundem noi, direct, iar capitolele de mai jos le descriu.",
        },
        { kind: "h3", text: "2. Datele din aplicație, introduse de clienții noștri (suntem persoană împuternicită)" },
        {
          kind: "p",
          text:
            "Tot ce introduce un client în aplicație — evidența deșeurilor, conducătorii auto, persoanele de " +
            "contact ale partenerilor, fișierele atașate — **îi aparține lui**. Noi doar le găzduim și le " +
            "prelucrăm la instrucțiunea lui, pe baza unui contract de împuternicire încheiat potrivit art. 28 GDPR.",
        },
        {
          kind: "p",
          text:
            "**Ce înseamnă asta practic pentru dumneavoastră.** Dacă datele dumneavoastră au ajuns în aplicație " +
            "pentru că firma la care lucrați (sau pentru care conduceți un transport) ține evidența acolo, " +
            "direct sau printr-un consultant de mediu, **operatorul e acea firmă, nu noi**. Cererea " +
            "dumneavoastră — de acces, de ștergere, de rectificare — se adresează ei. Dacă ne ajunge nouă, o " +
            "transmitem ei fără întârziere și vă anunțăm; nu putem răspunde pe fond în locul ei, fiindcă nu noi " +
            "decidem ce se întâmplă cu acele date.",
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
              "**Contul de utilizator**: e-mail, nume, prenume, rol, firma sau cabinetul din care faceți parte",
              "Ca să existe accesul, autentificarea și comunicarea legată de serviciu",
              "Executarea contractului, art. 6(1)(b)",
              "Durata contractului + 90 de zile (sau durata arhivei cerute de client)",
            ],
            [
              "**Comunicările de serviciu**: alertele de termene, rezumatul zilnic al cabinetului, facturile și mementourile de plată, anunțurile de mentenanță sau de modificare a termenilor",
              "Fac parte din serviciul contractat; nu sunt publicitate",
              "Executarea contractului, art. 6(1)(b)",
              "Nu le arhivăm separat de cont",
            ],
            [
              "**Abonamentul și plata**: datele de facturare (denumire, CUI, adresă, e-mail de facturare), metoda de plată aleasă, iar la plata cu cardul: identificatorul (token) emis de procesatorul de plăți, ultimele patru cifre și data de expirare a cardului. **Numărul cardului nu ajunge la noi**: îl introduceți pe pagina procesatorului",
              "Ca să emitem facturile și să încasăm abonamentul, inclusiv debitarea automată pe care ați autorizat-o",
              "Executarea contractului, art. 6(1)(b)",
              "Identificatorul cardului: până când treceți pe transfer sau contractul încetează. Facturile: 10 ani",
            ],
            [
              "**Datele de facturare** de pe facturile emise: denumire, CUI, adresă, sume",
              "Ca să ținem contabilitatea și să transmitem facturile în RO e-Factura",
              "Obligație legală, art. 6(1)(c)",
              "10 ani, potrivit legii contabilității",
            ],
            [
              "**Corespondența** cu noi: mesaje, cereri de suport",
              "Ca să răspundem și să ținem evidența a ce s-a cerut",
              "Interes legitim, art. 6(1)(f) — să putem dovedi ce am răspuns",
              "3 ani",
            ],
            [
              "**Date tehnice**: adresă IP, momentul cererii, tipul browserului, rapoarte de eroare, jurnalul modificărilor din aplicație (cine, ce, când)",
              "Securitatea serviciului, repararea defectelor, trasabilitatea cerută de evidența legală",
              "Interes legitim, art. 6(1)(f) — un serviciu care funcționează și nu e atacat",
              "Jurnalele tehnice: 90 de zile. Jurnalul modificărilor: cât evidența pe care o descrie",
            ],
          ],
        },
        { kind: "p", text: "Nu vă cerem date de care nu avem nevoie, și nu vă cerem CNP-ul ca să deschideți un cont." },
        {
          kind: "p",
          text:
            "Nu trimitem mesaje comerciale nesolicitate. Dacă vreodată vom trimite noutăți despre produs, o vom " +
            "face numai cu acordul dumneavoastră prealabil, pe care îl veți putea retrage din fiecare mesaj.",
        },
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
            ["**Brevo**", "Trimiterea e-mailurilor automate ale aplicației", "**Uniunea Europeană**"],
            ["**cyber_Folks**", "Căsuța contact@wastehouse.ro și site-ul wastehouse.ro", "**România**"],
            ["**Cloudinary**", "Fișierele atașate în aplicație", "**Statele Unite**"],
            ["**FGO**", "Emiterea facturilor abonamentului și transmiterea lor în RO e-Factura", "**România**"],
            ["**NETOPIA Payments**", "Plata cu cardul: pagina de plată, salvarea cardului, debitarea facturilor", "**România**"],
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
            "**NETOPIA Payments** este o instituție de plată autorizată și, pentru datele cardului și executarea " +
            "plății, acționează ca **operator independent**, potrivit politicii lui de confidențialitate, " +
            "disponibilă pe [netopia-payments.com](https://netopia-payments.com). Noi nu vedem și nu stocăm " +
            "numărul cardului.",
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
            "Mai transmitem date **autorităților publice**, atunci când legea ne obligă și în limita a ce ni se " +
            "cere. Facturile ajung la **ANAF** prin sistemul RO e-Factura, fiindcă legea o cere pentru orice " +
            "factură între firme.",
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
            "Aplicația folosește **stocarea locală a browserului** (`localStorage`), pentru trei lucruri, toate " +
            "strict necesare funcționării:",
        },
        {
          kind: "list",
          items: [
            "**jetonul de sesiune** — ca să rămâneți autentificat între pagini; expiră în 8 ore;",
            "**contul autentificat și firma selectată** — numele, adresa de e-mail și rolul dumneavoastră, ca să nu le cerem serverului la fiecare pagină, și firma pe care lucrați;",
            "**ciornele de formular** — ca să nu pierdeți ce ați scris dacă închideți din greșeală pagina.",
          ],
        },
        {
          kind: "p",
          text:
            "Toate stau **în browserul dumneavoastră** și se șterg la deconectare sau când goliți datele " +
            "site-ului.",
        },
        {
          kind: "p",
          text:
            "Pagina de plată a procesatorului de plăți (NETOPIA) poate folosi propriile cookie-uri, descrise în " +
            "politica lui.",
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
            "Termenele sunt în tabelul de mai sus. La încetarea contractului, contul rămâne **90 de zile** în " +
            "doar-citire, ca datele să poată fi exportate, apoi se șterg din sistemele active; copiile de " +
            "siguranță se sting în cel mult 30 de zile de la acel moment. Un client poate cere, înainte de " +
            "expirarea celor 90 de zile, păstrarea contului în doar-citire pentru cel mult 36 de luni, ca să-și " +
            "poată descărca evidența oricând în termenul legal de păstrare.",
        },
        {
          kind: "p",
          text:
            "Numele și actele de identitate ale conducătorilor auto de pe înregistrările de transport mai vechi " +
            "de trei ani calendaristici întregi se șterg automat, indiferent de starea contului.",
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
            "**dreptul de a nu fi supus unei decizii automate** — nu luăm astfel de decizii; trecerea unui cont în doar-citire pentru neplată e o clauză de contract aplicată automat, nu o evaluare a unei persoane;",
            "**dreptul de a vă retrage consimțământul**, acolo unde el a fost temeiul, fără ca asta să afecteze ce s-a prelucrat legal înainte.",
          ],
        },
        {
          kind: "p",
          text:
            "**Cum le exercitați:** scrieți la **contact@wastehouse.ro**. Răspundem în **cel mult o lună** de la " +
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
            "restaurare verificată, un jurnal al modificărilor care nu reține actele de identitate și CNP-urile, " +
            "un colector de erori care ne anunță când ceva se strică, și niciun număr de card la noi.",
        },
        {
          kind: "p",
          text:
            "Descrierea completă a măsurilor stă în contractul de împuternicire pe care îl semnăm cu fiecare " +
            "client, și v-o putem pune la dispoziție.",
        },
        {
          kind: "p",
          text:
            "Dacă are loc o încălcare a securității datelor care vă poate afecta, vă anunțăm, pe dumneavoastră " +
            "sau pe firma care e operator, potrivit art. 33–34 GDPR.",
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
