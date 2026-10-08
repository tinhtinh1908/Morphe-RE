// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.zalo;

import vn.dtinh.messenger.RegistrationPolicy;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** FCM support only: no new components, UI, logs, token export or background timers. */
public final class ZaloMicroGSupportV6 {
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
            new Thread(() -> register(generation), "Zalo-microG").start();
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

    private static String userId() throws Exception {
        Method loggedIn = Class.forName("xo1.k").getDeclaredMethod("f");
        loggedIn.setAccessible(true);
        if (!Boolean.TRUE.equals(loggedIn.invoke(null))) return null;
        Field id = Class.forName("com.zing.zalocore.CoreUtility").getDeclaredField("i");
        id.setAccessible(true);
        Object value = id.get(null);
        return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
    }

    /** Local handoff to Zalo's existing submission flow; not a server ACK. */
    public static void tokenReady(String token) {
        if (token == null || token.isEmpty()) return;
        try { policy.tokenReady(userId()); } catch (Throwable ignored) { }
    }

    private static void register(int generation) {
        String user = null;
        boolean began = false;
        boolean again = false;
        try {
            user = userId();
            began = policy.begin(SystemClock.elapsedRealtime(), user);
            if (!began) return;
            if (user == null) return;
            PackageManager pm = app.getPackageManager();
            if (!pm.getApplicationInfo("app.revanced.android.gms", 0).enabled ||
                pm.checkPermission("app.revanced.android.c2dm.permission.SEND", "app.revanced.android.gms") != PackageManager.PERMISSION_GRANTED) return;
            ComponentName receiver = new ComponentName(app, "com.google.firebase.iid.FirebaseInstanceIdReceiver");
            if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
                pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            Method getter = Class.forName("yi0.d").getDeclaredMethod("a");
            getter.setAccessible(true);
            Object manager = getter.invoke(null);
            Class<?> managerClass = Class.forName("yi0.f");
            Field listener = managerClass.getDeclaredField("a");
            listener.setAccessible(true);
            if (listener.get(manager) == null) return;
            Class<?> cloudType = Class.forName("yi0.a");
            Field firebaseField = cloudType.getDeclaredField("FIREBASE");
            firebaseField.setAccessible(true);
            Object firebase = firebaseField.get(null);
            Method selected = managerClass.getDeclaredMethod("b");
            selected.setAccessible(true);
            if (selected.invoke(manager) != firebase) return;
            // Zalo's own provider uses the SDK's token request deduplication and cache.
            Method fetch = managerClass.getDeclaredMethod("a", Context.class);
            fetch.setAccessible(true);
            Object token = fetch.invoke(manager, app);
            if (!(token instanceof String) || ((String) token).isEmpty()) return;
            Method submit = managerClass.getDeclaredMethod("e", cloudType, Context.class, String.class);
            submit.setAccessible(true);
            submit.invoke(manager, firebase, app, token);
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

}
