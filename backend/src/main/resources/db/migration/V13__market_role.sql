-- ETAPA G6 — „ce tip de generator": producător, importator, comerciant.
--
-- Întrebarea din meeting-ul cu specialista (schiţe, pag. 4): „Ce tip generator (imp/prod/
-- comercial)", cu nota „comercial — nu are deşeuri proprii". Trioul e clasificarea din actul de
-- ambalaje: Legea 249/2015, anexa nr. 1, îi enumeră împreună — „operatori economici - referitor la
-- ambalaje, înseamnă furnizorii de materiale de ambalare, producătorii de ambalaje şi produse
-- ambalate, IMPORTATORII, COMERCIANŢII, DISTRIBUITORII, autorităţile publice şi organizaţiile
-- neguvernamentale" —, iar declaraţia construită pe ea se cheamă chiar „Producători şi importatori
-- de ambalaje de desfacere, DE PRODUSE AMBALATE, supraambalatori" (Ordinul MMP 794/2012, anexa 1).
--
-- Ce decide: cine depune declaraţia aceea şi cine datorează contribuţia pe ambalaje la AFM.
-- Comerciantul vinde marfă ambalată de altcineva, deci nu el a pus ambalajul pe piaţă.
--
-- Ce NU decide: fişa de evidenţă a gestiunii deşeurilor (HG 856/2002, anexa 1 — alt document cu
-- acelaşi nume). Art. 1 alin. (1) o cere oricui generează deşeu, indiferent ce vinde.
--
-- Aditiv, ca tot profilul: un set gol înseamnă „nu s-a răspuns", nu „nu".

CREATE TABLE company_market_roles (
    company_id  UUID        NOT NULL REFERENCES companies (id),
    market_role VARCHAR(20) NOT NULL,
    PRIMARY KEY (company_id, market_role)
);

-- Aceeaşi formă ca pe firmă, ca aprobarea unei cereri să rămână o copiere, nu o traducere.
CREATE TABLE account_request_market_roles (
    account_request_id UUID        NOT NULL REFERENCES account_requests (id),
    market_role        VARCHAR(20) NOT NULL,
    PRIMARY KEY (account_request_id, market_role)
);
