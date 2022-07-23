package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import dev.latvian.mods.rhino.ObjArray;

import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class Utils {

    public static void waitFor(BooleanSupplier supplier) {
        while (!supplier.getAsBoolean()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static ExecutorService timeoutAsync = Executors.newCachedThreadPool();

    public static <R> R timeoutWith(int timeout, Supplier<R> supplier) throws ExecutionException, TimeoutException {
        try {
            return CompletableFuture.supplyAsync(supplier, timeoutAsync).get(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            return null;
        }
    }

}
