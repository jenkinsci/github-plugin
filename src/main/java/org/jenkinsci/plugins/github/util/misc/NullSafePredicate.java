package org.jenkinsci.plugins.github.util.misc;

import com.google.common.base.Predicate;

import edu.umd.cs.findbugs.annotations.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This abstract class calls {@link #applyNullSafe(Object)} only after success validation of inner object for null
 *
 * @author lanwen (Merkushev Kirill)
 */

public abstract class NullSafePredicate<T> implements Predicate<T> {

    @Override
    public boolean apply(T input) {
        return applyNullSafe(checkNotNull(input, "Argument for this predicate can't be null"));
    }

    /**
     * This method will be called inside of {@link #apply(Object)}
     */
    protected abstract boolean applyNullSafe(@NonNull T input);
}
