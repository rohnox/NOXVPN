package app.openconnect.core;

import android.content.Context;

/** Initializes only the VPN libraries/profile owner. No upstream crash reporter is initialized. */
public final class EmbeddedRuntime {
    public interface FailureListener { void onFailure(String operation, Exception error); }
    private static volatile FailureListener failureListener;
    public static void setFailureListener(FailureListener listener) { failureListener = listener; }
    public static void reportFailure(String operation, Exception error) {
        FailureListener listener = failureListener;
        if (listener != null) listener.onFailure(operation, error);
    }
    /** Only the engine's management state publishes connection; profile selection is not connection. */
    public interface ConnectionStatusListener { void onChanged(String profileId); }
    private static final java.util.Set<ConnectionStatusListener> statusListeners = new java.util.concurrent.CopyOnWriteArraySet<>();
    private static String connectedProfileId;
    public static synchronized String getConnectedProfileId() { return connectedProfileId; }
    public static synchronized void addStatusListener(ConnectionStatusListener listener) {
        statusListeners.add(listener); listener.onChanged(connectedProfileId);
    }
    public static synchronized void removeStatusListener(ConnectionStatusListener listener) { statusListeners.remove(listener); }
    public static synchronized void publishConnectionStatus(String profileId, boolean connected) {
        connectedProfileId = connected ? profileId : null;
        for (ConnectionStatusListener listener : statusListeners) listener.onChanged(connectedProfileId);
    }
    /** Host process installs the handover gate without loading the VPN native libraries. */
    public interface ConnectionGate { void beforeConnect(Context context, Runnable connect); }
    private static volatile ConnectionGate connectionGate;
    public static void setConnectionGate(ConnectionGate gate) { connectionGate = gate; }
    public static void beforeConnect(Context context, Runnable connect) {
        ConnectionGate gate = connectionGate;
        if (gate == null) connect.run(); else gate.beforeConnect(context, connect);
    }
    private static volatile boolean initialized;
    public static boolean isInitialized() { return initialized; }
    public static void returnToHost(android.app.Activity activity) {
        activity.startActivity(new android.content.Intent().setClassName(activity.getPackageName(),
            "com.v2ray.ang.ui.protocols.ProtocolsActivity"));
        activity.finish();
    }
    public static synchronized void initialize(Context context) {
        if (initialized) return;
        System.loadLibrary("openconnect");
        System.loadLibrary("stoken");
        ProfileManager.init(context.getApplicationContext());
        FragCache.init();
        initialized = true;
    }
}
