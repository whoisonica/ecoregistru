package ro.ecoregistru.controller.response;

import java.util.UUID;

/**
 * Un rând din lista de persoane fizice (D1.7b). CNP-ul iese doar cu ultimele patru cifre (Legea 190/2018
 * art. 4, minimizare): lista o vede oricine din firmă, inclusiv vizualizatorul, iar ultimele cifre ajung
 * ca omul de la cântar să deosebească doi „Ion Popescu”. Actul de identitate nu iese deloc.
 *
 * @param cnpLastDigits ultimele patru cifre, sau null dacă fișa n-are CNP
 * @param metalReady    fișa are tot ce cere borderoul la metal: CNP valid, act și domiciliu
 */
public record NaturalPersonSummary(
        UUID id,
        String name,
        String cnpLastDigits,
        boolean metalReady,
        boolean active,
        boolean hasOperations) {
}
