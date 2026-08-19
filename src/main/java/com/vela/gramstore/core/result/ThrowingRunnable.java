package com.vela.gramstore.core.result;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}
