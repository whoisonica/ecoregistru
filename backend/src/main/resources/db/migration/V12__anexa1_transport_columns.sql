-- ETAPA G4 — ultimele două nomenclatoare din cap. 2 al Anexei 1.
--
-- Capitolul 2 al fişei are trei grupuri de coloane: Stocare (Cant. + Tipul), Tratare (Cant. +
-- Modul + Scopul) şi Transport (Mijlocul + Destinaţia). Primele două grupuri au venit cu `V8`;
-- fără astea două, capitolul se tipăreşte cu jumătate din coloane goale.
--
-- Atenţie la omonimie: `waste_movement_transport_destinations` (V10) e caseta "Destinat:" de pe
-- Anexa 3 — ce se face cu transportul ăla anume, şi pot fi bifate mai multe. Coloana de aici e
-- "Destinaţia5)" din cap. 2 al Anexei 1: unde ajunge deşeul, o singură valoare dintr-o listă
-- închisă. Sunt două rubrici din două formulare diferite, de aceea sunt două coloane.

ALTER TABLE waste_movements ADD COLUMN transport_means VARCHAR(10);
ALTER TABLE waste_movements ADD COLUMN waste_destination VARCHAR(10);
