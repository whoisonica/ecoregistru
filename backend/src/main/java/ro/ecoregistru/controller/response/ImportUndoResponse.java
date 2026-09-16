package ro.ecoregistru.controller.response;

/** Ce a făcut „Anulează”: câte mișcări a șters și câte a lăsat, fiindcă fuseseră modificate între timp. */
public record ImportUndoResponse(int deleted, int kept) {}
