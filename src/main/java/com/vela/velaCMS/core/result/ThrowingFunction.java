package com.vela.velaCMS.core.result;

@FunctionalInterface
public interface ThrowingFunction<T, R> {
    R apply(T value) throws Exception;
}
