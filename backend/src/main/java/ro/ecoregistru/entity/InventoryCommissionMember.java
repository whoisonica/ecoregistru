package ro.ecoregistru.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** D3.5 — un membru al comisiei de inventariere (pct. 6 alin. (1)): nume și funcție tastate, nu un cont. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryCommissionMember {

    @Column(nullable = false, length = 200)
    String name;

    @Column(length = 200)
    String role;

    @Column(nullable = false)
    boolean president;
}
