package org.jenkinsci.plugins.github.util.misc;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This abstract class calls {@link #applyNullSafe(Object)} only after success validation of inner object for null
 * 
 * {@inheritDoc}
 *
 * @author lanwen (Merkushev Kirill)
 */
public abstract class NullSafeFunction<F, T> implements Function<F, T> {
    /**
     * {@inheritDoc}
     */
    @Override
    public T apply(@Nullable F input) {
        return applyNullSafe(Preconditions.checkNotNull(input, "This function not allows to use null as argument"));
    }

    /**
     * This method will be called inside of {@link #apply(Object)}
     */
    protected abstract T applyNullSafe(@Nonnull F input);
}
