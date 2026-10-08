// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.messenger;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** FCM support only: no new components, UI, logs, token export or background timers. */
public final class MicroGFcmSupportV5 {
    private static final AtomicBoolean installed = new AtomicBoolean();
    private static final AtomicBoolean worker = new AtomicBoolean();
    private static final RegistrationPolicy policy = new RegistrationPolicy();
    private static Handler handler;
    private static Context app;
    // Accessed only on the main thread.
    private static int resumed;
    private static int foregroundGeneration;
    private static boolean scheduled;
    private static final Runnable retry = () -> {
        scheduled = false;
        if (resumed == 0 || !worker.compareAndSet(false, true)) return;
        try {
            final int generation = foregroundGeneration;
            new Thread(() -> register(generation), "Messenger-microG").start();
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
        Method method = session.getClass().getMethod("B4d");
        method.setAccessible(true);
        Object value = method.invoke(session);
        return value instanceof String && !((String) value).isEmpty() && !"0".equals(value) ? (String) value : null;
    }

    /** Called after Messenger has handed a nonempty token to its existing registration flow.
     * This is local token readiness, not confirmation of a server ACK or push delivery. */
    public static void tokenReady(Object session, String token) {
        if (token == null || token.isEmpty()) return;
        try { policy.tokenReady(userId(session)); } catch (Throwable ignored) { }
    }

    private static void register(int generation) {
        String user = null;
        boolean began = false;
        boolean again = false;
        try {
            Method getter = Class.forName("X.17x").getDeclaredMethod("A0A");
            getter.setAccessible(true);
            Object session = getter.invoke(null);
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
            Class<?> facade = Class.forName("X.1Gv");
            Constructor<?> constructor = facade.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object initializer = constructor.newInstance();
            Class<?> sessionClass = Class.forName("com.facebook.auth.usersession.FbUserSession");
            Method init = facade.getDeclaredMethod("ATw", sessionClass);
            init.setAccessible(true);
            init.invoke(initializer, session);
            Method fetch = facade.getDeclaredMethod("C2E", sessionClass, String.class);
            fetch.setAccessible(true);
            // C2E can return false on success as well as failure; use tokenReady instead.
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

    public static PendingIntent pendingBroadcast(Context c, int request, Intent intent, int flags) {
        if (Build.VERSION.SDK_INT >= 23 && (flags & (PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_MUTABLE)) == 0)
            flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, request, intent, flags);
    }
}
