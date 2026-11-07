package de.blinkt.openvpn.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/** Host integration only: keeps the upstream status IPC owner, without another Application. */
public final class EmbeddedRuntime {
    public interface ConnectionGate { void beforeConnect(Context context, Runnable connect); }
    private static volatile ConnectionGate connectionGate;
    public interface FailureListener { void onFailure(String operation, String profileId, Exception error); }
    private static volatile FailureListener failureListener;
    public static void setFailureListener(FailureListener listener) { failureListener = listener; }
    public static void reportFailure(String operation, String profileId, Exception error) {
        FailureListener listener = failureListener;
        if (listener != null) listener.onFailure(operation, profileId, error);
    }
    public static void setConnectionGate(ConnectionGate gate) { connectionGate = gate; }
    public static void beforeConnect(Context context, Runnable connect) {
        ConnectionGate gate = connectionGate;
        if (gate == null) connect.run(); else gate.beforeConnect(context, connect);
    }
    private static StatusListener listener;
    public static synchronized void initialize(Context context) {
        if (listener != null) return;
        Context app = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = app.getSystemService(NotificationManager.class);
            for (String channel : new String[]{OpenVPNService.NOTIFICATION_CHANNEL_BG_ID,
                    OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID, OpenVPNService.NOTIFICATION_CHANNEL_USERREQ_ID}) {
                manager.createNotificationChannel(new NotificationChannel(channel, "OpenVPN", NotificationManager.IMPORTANCE_LOW));
            }
        }
        ProfileManager.getInstance(app);
        listener = new StatusListener();
        listener.init(app);
    }
}
