package com.smartstart.portal.centre.tenancy;

import java.util.function.Supplier;

/**
 * The centre the current request or job is working in. Every query on centre-owned data is filtered to it (see
 * {@link CentreTenantResolver}); with no centre set, those queries match nothing.
 */
public final class TenantContext {

    /** Matches no centre, so data stays invisible when nothing was set. */
    public static final long NONE = 0L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long current() {
        return CURRENT.get();
    }

    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    /** The current centre id, or throws when there is none (a bug: the caller should have set one). */
    public static long require() {
        Long id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No centre selected for this request");
        }
        return id;
    }

    public static void set(Long centreId) {
        if (centreId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(centreId);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Runs {@code work} as centre {@code centreId}, then restores whatever was set before. */
    public static <T> T callAs(Long centreId, Supplier<T> work) {
        Long previous = CURRENT.get();
        set(centreId);
        try {
            return work.get();
        } finally {
            set(previous);
        }
    }

    public static void runAs(Long centreId, Runnable work) {
        callAs(centreId, () -> {
            work.run();
            return null;
        });
    }
}
