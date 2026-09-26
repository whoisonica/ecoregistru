package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * D2.4 — pe ce depozite lucrează utilizatorul de acum. Un operator de cântar din Baciu nu vede Turda
 * (Legea 190/2018, minimizarea: operațiunile poartă persoane fizice).
 *
 * <p>Restrâns poate fi doar OPERATOR sau CLIENT_VIEWER cu {@code allWorkPoints = false}; adminul, consultantul
 * și platforma văd mereu tot, iar implicit — și pentru conturile de dinainte — oricine vede tot (V69).
 *
 * <p>Ce e al altui depozit se poartă ca inexistent (404 cu codul obiectului), ca la izolarea între firme:
 * un id din Turda nu confirmă că există. Totalurile pe firmă (evidența, rapoartele) nu trec pe aici.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepotAccess {

    static final Set<Role> RESTRICTABLE = EnumSet.of(Role.OPERATOR, Role.CLIENT_VIEWER);

    AppUserRepository userRepository;

    /** Depozitele permise, sau {@code null} când utilizatorul le vede pe toate. */
    public Set<UUID> allowed() {
        // Fără utilizator e un apel al sistemului (refacerea evidenței, o tipărire pe alt fir, un job): o cerere
        // HTTP fără sesiune se oprește în filtre, înainte de servicii. Sistemul vede tot, ca la CNP (BUG-043).
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            return null;
        }
        if (!RESTRICTABLE.contains(user.getRole()) || user.isAllWorkPoints()) {
            return null;
        }
        return Set.copyOf(userRepository.findWorkPointIds(user.getId()));
    }

    public boolean allows(UUID workPointId) {
        Set<UUID> allowed = allowed();
        return allowed == null || allowed.contains(workPointId);
    }

    /** Un depozit pe care se scrie: altul decât ale lui e „negăsit”, ca unul al altei firme. */
    public WorkPoint require(WorkPoint workPoint) {
        if (!allows(workPoint.getId())) {
            throw new NotFoundException(WORK_POINT_NOT_FOUND);
        }
        return workPoint;
    }

    /** Filtrul unei liste: o singură citire a drepturilor pentru toate rândurile. */
    public <T> List<T> filter(Collection<T> rows, Function<T, UUID> workPointOf) {
        Set<UUID> allowed = allowed();
        return allowed == null ? List.copyOf(rows)
                : rows.stream().filter(r -> allowed.contains(workPointOf.apply(r))).toList();
    }
}
