package com.warmthdawn.kubejsdebugadapter.adapter;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class DebuggerLauncher {
    private static final Logger log = LogManager.getLogger();

    private static volatile boolean kubejsLoaded = false;
    private static volatile Runnable loadedCallback;
    private static ReentrantLock lock = new ReentrantLock();

    public static int launch() {

        String portStr = System.getProperty("kubejs_debug_port");
        int port = 8000;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid port number: " + portStr);
        }
        int portFinal = port;
        Thread thread = new Thread(() -> {
            DebuggerLauncher.launchSocket(portFinal);
            log.info("Waiting for Debugger to connect on port: " + portFinal);
        });
        thread.setDaemon(true);
        thread.start();
        return port;
    }

    public static void launchSocket(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            while (true) {
                Socket accept = socket.accept();
                log.info("Accepted connection from" + accept.getInetAddress().getHostAddress());
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        InputStream input = accept.getInputStream();
                        OutputStream output = accept.getOutputStream();

                        launch(input, output);

                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Throwable e) {
            log.error("Failed to launch debugger", e);
        }

    }


    public static void launch(InputStream input, OutputStream output) {


        KubeDebugAdapter server = new KubeDebugAdapter();

        ExecutorService threads = Executors.newSingleThreadExecutor();


        Launcher<IDebugProtocolClient> launcher = new DebugLauncher.Builder<IDebugProtocolClient>()
            .setLocalService(server)
            .setRemoteInterface(IDebugProtocolClient.class)
            .setInput(input)
            .setOutput(output)
            .setExecutorService(threads)
            .setExceptionHandler(DebuggerLauncher::handleException)
            .create();

        server.connect(launcher.getRemoteProxy());

        launcher.startListening();

    }

    private static ResponseError handleException(Throwable throwable) {
        if (throwable instanceof CompletionException || (throwable instanceof InvocationTargetException && throwable.getCause() != null)) {
            return handleException(throwable.getCause());
        }

        if (throwable instanceof ResponseErrorException) {
            return ((ResponseErrorException) throwable).getResponseError();
        }


        log.error("Internal Error", throwable);
        DebugRuntime.getInstance().sendError("Error occurred: " + throwable.getMessage());

        return new ResponseError(500, "Internal Error", throwable.getMessage());
    }

    public static boolean isKubeJSLoaded() {
        return kubejsLoaded;
    }

    public static void runOnLoaded(Runnable callback) {
        lock.lock();
        try {
            if (!isKubeJSLoaded()) {
                loadedCallback = callback;
            } else {
                callback.run();
            }
        } finally {
            lock.unlock();
        }
    }

    public static void markKubeJSLoaded() {
        lock.lock();
        try {
            kubejsLoaded = true;
            if (loadedCallback != null) {
                loadedCallback.run();
            }
        } finally {
            loadedCallback = null;
            lock.unlock();
        }

    }
}
