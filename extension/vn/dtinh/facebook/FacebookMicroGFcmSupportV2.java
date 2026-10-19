// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.facebook;

import vn.dtinh.messenger.RegistrationPolicy;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** FCM support only: no new components, UI, logs, token export or background timers. */
public final class FacebookMicroGFcmSupportV2 {
    private static final AtomicBoolean installed = new AtomicBoolean();
    private static final AtomicBoolean worker = new AtomicBoolean();
    private static final RegistrationPolicy policy = new RegistrationPolicy();
    private static Handler handler;
    private static Context app;
    private static boolean migrationDone;
    // Accessed only on the main thread.
    private static int resumed;
    private static int foregroundGeneration;
    private static boolean scheduled;
    private static final Runnable retry = () -> {
        scheduled = false;
        if (resumed == 0 || !worker.compareAndSet(false, true)) return;
        try {
            final int generation = foregroundGeneration;
            new Thread(() -> register(generation), "Facebook-microG").start();
        } catch (Throwable ignored) {
            worker.set(false);
            schedule();
        }
    };

    public static void boot(Context context) {
        try {
            Context application = context.getApplicationContext();
            if (!(application instanceof Application) || !installed.compareAndSet(false, true)) return;
            app = application;
            handler = new Handler(Looper.getMainLooper());
            ((Application) application).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                public void onActivityResumed(Activity a) { resumed++; foregroundGeneration++; schedule(); }
                public void onActivityPaused(Activity a) {
                    resumed = Math.max(0, resumed - 1);
                    if (resumed == 0) { handler.removeCallbacks(retry); scheduled = false; }
                }
                public void onActivityCreated(Activity a, Bundle b) {}
                public void onActivityStarted(Activity a) {}
                public void onActivityStopped(Activity a) {}
                public void onActivitySaveInstanceState(Activity a, Bundle b) {}
                public void onActivityDestroyed(Activity a) {}
            });
        } catch (Throwable ignored) { installed.set(false); }
    }

    private static void schedule() {
        if (resumed == 0 || scheduled || worker.get()) return;
        scheduled = handler.postDelayed(retry, policy.delay(SystemClock.elapsedRealtime()));
    }

    private static String userId(Object session) throws Exception {
        if (session == null) return null;
        Field field = Class.forName("com.facebook.auth.usersession.manager.FbUserSessionImpl").getDeclaredField("A02");
        field.setAccessible(true);
        Object value = field.get(session);
        return value instanceof String && !((String) value).isEmpty() && !"0".equals(value) ? (String) value : null;
    }

    /** Called after Facebook has handed a nonempty token to its existing registration flow.
     * This is local token readiness, not confirmation of a server ACK or push delivery. */
    public static void tokenReady(Object session, String token) {
        if (token == null || token.isEmpty()) return;
        try { policy.tokenReady(userId(session)); } catch (Throwable ignored) { }
    }

    /** Once per backend migration, remove the old provider's local FCM-disable state.
     * Future errors retain Facebook's normal disable/backoff handling. */
    private static void recoverPreviousBackend() {
        if (migrationDone) return;
        try {
            SharedPreferences marker = app.getSharedPreferences("dtinh.microg.facebook.route.v2", Context.MODE_PRIVATE);
            if (!marker.getBoolean("completed", false)) {
                Class<?> type = Class.forName("X.5GG");
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object state = constructor.newInstance();
                Method reset = type.getDeclaredMethod("A00", boolean.class, String.class);
                reset.setAccessible(true);
                reset.invoke(state, false, "microg_route_v2");
                marker.edit().putBoolean("completed", true).commit();
            }
            migrationDone = true;
        } catch (Throwable ignored) {
            // A migration failure must not prevent the normal registration attempt.
        }
    }

    private static void register(int generation) {
        String user = null;
        boolean began = false;
        boolean again = false;
        try {
            Method getter = Class.forName("X.19m").getDeclaredMethod("A0H", Context.class);
            getter.setAccessible(true);
            Object session = getter.invoke(null, app);
            user = userId(session);
            began = policy.begin(SystemClock.elapsedRealtime(), user);
            if (!began) return;
            if (user == null) return;
            PackageManager pm = app.getPackageManager();
            if (!pm.getApplicationInfo("app.revanced.android.gms", 0).enabled ||
                pm.checkPermission("app.revanced.android.c2dm.permission.SEND", "app.revanced.android.gms") != PackageManager.PERMISSION_GRANTED) return;
            ComponentName receiver = new ComponentName(app, "com.google.firebase.iid.FirebaseInstanceIdReceiver");
            if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
                pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            recoverPreviousBackend();
            Class<?> facade = Class.forName("X.86K");
            Constructor<?> constructor = facade.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object initializer = constructor.newInstance();
            Class<?> sessionClass = Class.forName("com.facebook.auth.usersession.FbUserSession");
            Method fetch = facade.getDeclaredMethod("Cx6", sessionClass, String.class);
            fetch.setAccessible(true);
            // Preserve Facebook's token fetch/cache/server flow. The readiness hook
            // stops retries only after a nonempty token reaches its cache handoff.
            fetch.invoke(initializer, session, "microg_fcm_support");
        } catch (Throwable ignored) {
            if (!began) began = policy.begin(SystemClock.elapsedRealtime(), user);
        } finally {
            if (began) again = policy.finish(SystemClock.elapsedRealtime(), user);
            worker.set(false);
            final boolean retryNeeded = again;
            handler.post(() -> {
                if (retryNeeded || generation != foregroundGeneration) schedule();
            });
        }
    }

    public static int availability(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            if (!pm.getApplicationInfo("app.revanced.android.gms", 0).enabled) return 3;
            return pm.checkPermission("app.revanced.android.c2dm.permission.SEND", "app.revanced.android.gms")
                == PackageManager.PERMISSION_GRANTED ? 0 : 1;
        } catch (Throwable ignored) { return 1; }
    }

    public static PendingIntent pendingBroadcast(Context c, int request, Intent intent, int flags) {
        if (Build.VERSION.SDK_INT >= 23 && (flags & (PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_MUTABLE)) == 0)
            flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, request, intent, flags);
    }
}
