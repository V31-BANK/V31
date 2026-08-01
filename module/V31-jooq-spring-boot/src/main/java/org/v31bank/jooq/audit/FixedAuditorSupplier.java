package org.v31bank.jooq.audit;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link AuditorSupplier} implementation that always returns a fixed auditor.
 * Used as a fallback when the application does not provide its own
 * {@link AuditorSupplier} bean, typically one backed by the current
 * authentication.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@NullMarked
public class FixedAuditorSupplier implements AuditorSupplier {

    private final @Nullable String auditor;

    public FixedAuditorSupplier(@Nullable String auditor) {
        this.auditor = auditor;
    }

    @Override
    public Optional<String> currentAuditor() {
        return Optional.ofNullable(this.auditor);
    }

}
