package com.vela.gramstore.core.result;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
