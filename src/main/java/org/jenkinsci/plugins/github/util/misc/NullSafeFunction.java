package org.jenkinsci.plugins.github.util.misc;

import com.google.common.base.Function;

import edu.umd.cs.findbugs.annotations.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This abstract class calls {@link #applyNullSafe(Object)} only after success validation of inner object for null
 *
 * @author lanwen (Merkushev Kirill)
 */
public abstract class NullSafeFunction<F, T> implements Function<F, T> {

    @Override
    public T apply(F input) {
        return applyNullSafe(checkNotNull(input, "This function does not allow using null as argument"));
    }

    /**
     * This method will be called inside of {@link #apply(Object)}
     */
    protected abstract T applyNullSafe(@NonNull F input);
}
