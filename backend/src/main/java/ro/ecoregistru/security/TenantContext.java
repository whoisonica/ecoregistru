package ro.ecoregistru.security;

import ro.ecoregistru.exception.BusinessException;

import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.TENANT_REQUIRED;

/**
 * Holds the effective tenant (Company) id for the current request thread.
 *
 * Populated by {@link TenantFilter} after authentication:
 *  - normal users  -> their own company id
 *  - PLATFORM_ADMIN -> the company id from the X-Tenant-Id header (tenant switcher)
 *
 * Every domain repository query MUST scope by {@link #require()} so cross-tenant
 * access is structurally impossible.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    /** Returns the current tenant id or throws if none is set. */
    public static UUID require() {
        UUID tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new BusinessException(TENANT_REQUIRED);
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
