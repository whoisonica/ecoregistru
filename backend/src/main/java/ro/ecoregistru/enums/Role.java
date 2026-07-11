package ro.ecoregistru.enums;

/**
 * Roles within (and above) a tenant.
 * PLATFORM_ADMIN is global (staff running the done-for-you service across tenants).
 * ADMIN / OPERATOR / CLIENT_VIEWER are scoped to a single tenant (Company).
 */
public enum Role {
    PLATFORM_ADMIN,
    ADMIN,
    OPERATOR,
    CLIENT_VIEWER
}
