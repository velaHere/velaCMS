package com.vela.velaCMS.core.result;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}
