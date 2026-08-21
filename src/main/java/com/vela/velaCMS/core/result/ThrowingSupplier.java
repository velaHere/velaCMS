package com.vela.velaCMS.core.result;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
