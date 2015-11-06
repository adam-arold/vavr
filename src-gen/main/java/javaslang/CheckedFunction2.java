/*     / \____  _    _  ____   ______  / \ ____  __    _ _____
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  / /  _  \   Javaslang
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/  \__/  /   Copyright 2014-now Daniel Dietrich
 * /___/\_/  \_/\____/\_/  \_/\__\/__/___\_/  \_//  \__/_____/    Licensed under the Apache License, Version 2.0
 */
package javaslang;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javaslang.CheckedFunction2Module.Memoized;
import javaslang.control.Try;

/**
 * Represents a function with two arguments.
 *
 * @param <T1> argument 1 of the function
 * @param <T2> argument 2 of the function
 * @param <R> return type of the function
 * @author Daniel Dietrich
 * @since 1.1.0
 */
@FunctionalInterface
public interface CheckedFunction2<T1, T2, R> extends λ<R> {

    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Creates a {@code CheckedFunction2} based on
     * <ul>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html">method reference</a></li>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html#syntax">lambda expression</a></li>
     * </ul>
     *
     * Examples (w.l.o.g. referring to Function1):
     * <pre><code>// using a lambda expression
     * Function1&lt;Integer, Integer&gt; add1 = Function1.of(i -&gt; i + 1);
     *
     * // using a method reference (, e.g. Integer method(Integer i) { return i + 1; })
     * Function1&lt;Integer, Integer&gt; add2 = Function1.of(this::method);
     *
     * // using a lambda reference
     * Function1&lt;Integer, Integer&gt; add3 = Function1.of(add1::apply);
     * </code></pre>
     * <p>
     * <strong>Caution:</strong> Reflection loses type information of lambda references.
     * <pre><code>// type of a lambda expression
     * Type&lt;?, ?&gt; type1 = add1.getType(); // (Integer) -&gt; Integer
     *
     * // type of a method reference
     * Type&lt;?, ?&gt; type2 = add2.getType(); // (Integer) -&gt; Integer
     *
     * // type of a lambda reference
     * Type&lt;?, ?&gt; type3 = add3.getType(); // (Object) -&gt; Object
     * </code></pre>
     *
     * @param methodReference (typically) a method reference, e.g. {@code Type::method}
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @return a {@code CheckedFunction2}
     */
    static <T1, T2, R> CheckedFunction2<T1, T2, R> of(CheckedFunction2<T1, T2, R> methodReference) {
        return methodReference;
    }

    /**
     * Applies this function to two arguments and returns the result.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @return the result of function application
     * @throws Throwable if something goes wrong applying this function to the given arguments
     */
    R apply(T1 t1, T2 t2) throws Throwable;

    /**
     * Checks if this function is applicable to the given objects,
     * i.e. each of the given objects is either null or the object type is assignable to the parameter type.
     * <p>
     * Please note that it is not checked if this function is defined for the given objects.
     *
     * @param o1 object 1
     * @param o2 object 2
     * @return true, if this function is applicable to the given objects, false otherwise.
     */
    default boolean isApplicableTo(Object o1, Object o2) {
        final Class<?>[] paramTypes = getType().parameterTypes();
        return
                (o1 == null || paramTypes[0].isAssignableFrom(o1.getClass())) &&
                (o2 == null || paramTypes[1].isAssignableFrom(o2.getClass()));
    }

    /**
     * Checks if this function is generally applicable to objects of the given types.
     *
     * @param type1 type 1
     * @param type2 type 2
     * @return true, if this function is applicable to objects of the given types, false otherwise.
     */
    default boolean isApplicableToTypes(Class<?> type1, Class<?> type2) {
        Objects.requireNonNull(type1, "type1 is null");
        Objects.requireNonNull(type2, "type2 is null");
        final Class<?>[] paramTypes = getType().parameterTypes();
        return
                paramTypes[0].isAssignableFrom(type1) &&
                paramTypes[1].isAssignableFrom(type2);
    }

    /**
     * Applies this function partially to one argument.
     *
     * @param t1 argument 1
     * @return a partial application of this function
     * @throws Throwable if something goes wrong partially applying this function to the given arguments
     */
    default CheckedFunction1<T2, R> apply(T1 t1) throws Throwable {
        return (T2 t2) -> apply(t1, t2);
    }

    @Override
    default int arity() {
        return 2;
    }

    @Override
    default CheckedFunction1<T1, CheckedFunction1<T2, R>> curried() {
        return t1 -> t2 -> apply(t1, t2);
    }

    @Override
    default CheckedFunction1<Tuple2<T1, T2>, R> tupled() {
        return t -> apply(t._1, t._2);
    }

    @Override
    default CheckedFunction2<T2, T1, R> reversed() {
        return (t2, t1) -> apply(t1, t2);
    }

    @Override
    default CheckedFunction2<T1, T2, R> memoized() {
        if (isMemoized()) {
            return this;
        } else {
            final Object lock = new Object();
            final Map<Tuple2<T1, T2>, R> cache = new HashMap<>();
            final CheckedFunction1<Tuple2<T1, T2>, R> tupled = tupled();
            return (CheckedFunction2<T1, T2, R> & Memoized) (t1, t2) -> {
                synchronized (lock) {
                    return cache.computeIfAbsent(Tuple.of(t1, t2), t -> Try.of(() -> tupled.apply(t)).get());
                }
            };
        }
    }

    @Override
    default boolean isMemoized() {
        return this instanceof Memoized;
    }

    /**
     * Returns a composed function that first applies this CheckedFunction2 to the given argument and then applies
     * {@linkplain CheckedFunction1} {@code after} to the result.
     *
     * @param <V> return type of after
     * @param after the function applied after this
     * @return a function composed of this and after
     * @throws NullPointerException if after is null
     */
    default <V> CheckedFunction2<T1, T2, V> andThen(CheckedFunction1<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after is null");
        return (t1, t2) -> after.apply(apply(t1, t2));
    }

    @Override
    default Type<T1, T2, R> getType() {
        return new Type<>(this);
    }

    /**
     * Represents the type of a {@code CheckedFunction2} which consists of two parameter types
     * and a return type.
     *
     *
     * @param <T1> the 1st parameter type of the function
     * @param <T2> the 2nd parameter type of the function
     * @param <R> the return type of the function
     * @author Daniel Dietrich
     * @since 2.0.0
     */
    final class Type<T1, T2, R> extends λ.Type<R> {

        private static final long serialVersionUID = 1L;

        private Type(CheckedFunction2<T1, T2, R> λ) {
            super(λ);
        }

        @SuppressWarnings("unchecked")
        public Class<T1> parameterType1() {
            return (Class<T1>) parameterTypes()[0];
        }

        @SuppressWarnings("unchecked")
        public Class<T2> parameterType2() {
            return (Class<T2>) parameterTypes()[1];
        }
    }
}

interface CheckedFunction2Module {

    /**
     * Tagging ZAM interface for Memoized functions.
     */
    interface Memoized {
    }
}