-- Semnalul „de verificat" pe predările de marfă preluată — scos.
--
-- `resale_suspected` marca liniile unde acelaşi punct de lucru şi cod avea şi preluări de la terţi
-- (registrul art. 48): o predare de deşeu propriu şi una care dă mai departe marfă preluată arată
-- identic, deci linia era semnalată în loc să fie reclasificată.
--
-- Specialista a închis subiectul pe 24.08.2026: pentru modulul de generatori **nu ne interesează
-- preluarea de la terţi** (întrebarea 4, `docs/intrebari-specialist.md`). Un generator pur nici nu
-- poate înregistra `COLLECTED` — `CompanyType.allowedOperations()` nu i-o oferă —, deci steagul era
-- mereu fals şi promitea o verificare pe care nimeni n-o cerea.
--
-- Ce NU se schimbă: separarea celor două registre. Marfa preluată de la terţi rămâne în afara
-- Anexei 1 (HG 856/2002 art. 2 alin. (1)) — filtrul pe `register = ANEXA_1` din motorul de evidenţă
-- rămâne pe loc, cu testul lui. Se pierde doar avertismentul, nu regula.

ALTER TABLE monthly_evidences DROP COLUMN resale_suspected;
