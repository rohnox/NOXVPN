package app.openconnect.core;

import android.content.Context;

/** Initializes only the VPN libraries/profile owner. No upstream crash reporter is initialized. */
public final class EmbeddedRuntime {
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
