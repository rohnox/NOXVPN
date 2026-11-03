/* Host integration, GPL-3.0-or-later. */
package de.blinkt.openvpn.core;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/** App-owned IPC reader: neither opening nor draining the service pipe blocks the UI. */
public final class StatusLogReader implements Closeable {
    @FunctionalInterface
    public interface PacketHandler { void accept(byte[] packet) throws IOException; }
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "OpenVPN-status-reader");
        thread.setDaemon(true);
        return thread;
    });
    private Session current;
    private static final class Session {
        volatile InputStream stream;
        volatile boolean cancelled;
        Future<?> task;
    }
    public synchronized void start(Callable<InputStream> open, PacketHandler packet,
                                   Consumer<Exception> failure) {
        cancel();
        Session session = new Session();
        current = session;
        session.task = worker.submit(() -> {
            try (InputStream source = open.call()) {
                session.stream = source;
                if (session.cancelled) return;
                DataInputStream input = new DataInputStream(source);
                while (!session.cancelled) {
                    int length = input.readUnsignedShort();
                    if (length == 0x7fff) return;
                    if (length == 0) throw new IOException("Empty status frame");
                    byte[] bytes = new byte[length];
                    input.readFully(bytes);
                    packet.accept(bytes);
                }
            } catch (Exception error) {
                if (!session.cancelled) failure.accept(error);
            } finally { session.stream = null; }
        });
    }
    public synchronized void cancel() {
        if (current == null) return;
        current.cancelled = true;
        current.task.cancel(true);
        InputStream stream = current.stream;
        if (stream != null) {
            try { stream.close(); } catch (IOException ignored) { }
        }
        current = null;
    }
    @Override public synchronized void close() {
        cancel();
        worker.shutdownNow();
    }
}
