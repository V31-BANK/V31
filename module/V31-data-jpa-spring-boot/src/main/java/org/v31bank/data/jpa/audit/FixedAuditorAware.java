package org.v31bank.data.jpa.audit;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AuditorAware;

/**
 * {@link AuditorAware} implementation that always returns a fixed auditor. Used as
 * a fallback when the application does not provide its own {@link AuditorAware}
 * bean, typically one backed by the current authentication.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@NullMarked
public class FixedAuditorAware implements AuditorAware<String> {

    private final @Nullable String auditor;

    public FixedAuditorAware(@Nullable String auditor) {
        this.auditor = auditor;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(this.auditor);
    }

}
