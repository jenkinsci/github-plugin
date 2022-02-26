/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jenkinsci.plugins.github.util;


import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mostly copypaste from guava's FluentIterable
 */
@Restricted(NoExternalUse.class)
public abstract class FluentIterableWrapper<E> implements Iterable<E> {
    private final Iterable<E> iterable;

    FluentIterableWrapper(Iterable<E> iterable) {
        this.iterable = checkNotNull(iterable);
    }

    @Override
    public Iterator<E> iterator() {
        return iterable.iterator();
    }

    /**
     * Returns a fluent iterable that wraps {@code iterable}, or {@code iterable} itself if it
     * is already a {@code FluentIterable}.
     */
    public static <E> FluentIterableWrapper<E> from(final Iterable<E> iterable) {
        return (iterable instanceof FluentIterableWrapper)
                ? (FluentIterableWrapper<E>) iterable
                : new FluentIterableWrapper<E>(iterable) { };
    }

    /**
     * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
     * followed by those of {@code other}. The iterators are not polled until necessary.
     *
     * <p>The returned iterable's {@code Iterator} supports {@code remove()} when the corresponding
     * {@code Iterator} supports it.
     */
    @CheckReturnValue
    public final FluentIterableWrapper<E> append(Iterable<? extends E> other) {
        return from(Iterables.concat(iterable, other));
    }

    /**
     * Returns the elements from this fluent iterable that satisfy a predicate. The
     * resulting fluent iterable's iterator does not support {@code remove()}.
     */
    @CheckReturnValue
    public final FluentIterableWrapper<E> filter(Predicate<? super E> predicate) {
        return from(Iterables.filter(iterable, predicate));
    }

    /**
     * Returns the elements from this fluent iterable that are instances of the supplied type. The
     * resulting fluent iterable's iterator does not support {@code remove()}.
     * @since 1.25.0
     */
    @CheckReturnValue
    public final <F extends E> FluentIterableWrapper<F> filter(Class<F> clazz) {
        return from(Iterables.filter(iterable, clazz));
    }

    /**
     * Returns a fluent iterable that applies {@code function} to each element of this
     * fluent iterable.
     *
     * <p>The returned fluent iterable's iterator supports {@code remove()} if this iterable's
     * iterator does. After a successful {@code remove()} call, this fluent iterable no longer
     * contains the corresponding element.
     */
    public final <T> FluentIterableWrapper<T> transform(Function<? super E, T> function) {
        return from(Iterables.transform(iterable, function));
    }

    /**
     * Applies {@code function} to each element of this fluent iterable and returns
     * a fluent iterable with the concatenated combination of results.  {@code function}
     * returns an Iterable of results.
     *
     * <p>The returned fluent iterable's iterator supports {@code remove()} if this
     * function-returned iterables' iterator does. After a successful {@code remove()} call,
     * the returned fluent iterable no longer contains the corresponding element.
     */
    public <T> FluentIterableWrapper<T> transformAndConcat(
            Function<? super E, ? extends Iterable<? extends T>> function) {
        return from(Iterables.concat(transform(function)));
    }

    /**
     * Returns an {@link Optional} containing the first element in this fluent iterable that
     * satisfies the given predicate, if such an element exists.
     *
     * <p><b>Warning:</b> avoid using a {@code predicate} that matches {@code null}. If {@code null}
     * is matched in this fluent iterable, a {@link NullPointerException} will be thrown.
     */
    public final Optional<E> firstMatch(Predicate<? super E> predicate) {
        return Iterables.tryFind(iterable, predicate);
    }

    /**
     * Returns an {@link Optional} containing the first element in this fluent iterable.
     * If the iterable is empty, {@code Optional.absent()} is returned.
     *
     * @throws NullPointerException if the first element is null; if this is a possibility, use
     *                              {@code iterator().next()} or {@link Iterables#getFirst} instead.
     */
    public final Optional<E> first() {
        Iterator<E> iterator = iterable.iterator();
        return iterator.hasNext()
                ? Optional.of(iterator.next())
                : Optional.<E>absent();
    }

    /**
     * Returns list from wrapped iterable
     */
    public List<E> toList() {
        return Lists.newArrayList(iterable);
    }

    /**
     * Returns an {@code ImmutableSet} containing all of the elements from this fluent iterable with
     * duplicates removed.
     */
    public final ImmutableSet<E> toSet() {
        return ImmutableSet.copyOf(iterable);
    }

}
