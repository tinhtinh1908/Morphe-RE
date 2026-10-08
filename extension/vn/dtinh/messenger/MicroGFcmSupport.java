// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.messenger;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** FCM runtime support only. No added Android components, UI, logs or token reporting. */
public final class MicroGFcmSupport {
    private static final AtomicBoolean installed = new AtomicBoolean();
    private static final AtomicBoolean attempted = new AtomicBoolean();
    private static final AtomicBoolean scheduled = new AtomicBoolean();

    public static void boot(Context context) {
        try {
            final Context app = context.getApplicationContext();
            if (!(app instanceof Application) || !installed.compareAndSet(false, true)) return;
            ((Application) app).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                public void onActivityResumed(Activity activity) {
                    if (attempted.get() || !scheduled.compareAndSet(false, true)) return;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        scheduled.set(false);
                        if (!attempted.compareAndSet(false, true)) return;
                        new Thread(() -> register(app), "Messenger-microG").start();
                    }, 20000);
                }
                public void onActivityCreated(Activity a, Bundle b) {}
                public void onActivityStarted(Activity a) {}
                public void onActivityPaused(Activity a) {}
                public void onActivityStopped(Activity a) {}
                public void onActivitySaveInstanceState(Activity a, Bundle b) {}
                public void onActivityDestroyed(Activity a) {}
            });
        } catch (Throwable ignored) { }
    }

    private static void register(Context app) {
        try {
            PackageManager pm = app.getPackageManager();
            if (!pm.getApplicationInfo("app.revanced.android.gms", 0).enabled ||
                pm.checkPermission("app.revanced.android.c2dm.permission.SEND", "app.revanced.android.gms") != PackageManager.PERMISSION_GRANTED) return;
            Object session;
            try {
                Method getter = Class.forName("X.17x").getDeclaredMethod("A0A");
                getter.setAccessible(true);
                session = getter.invoke(null);
            } catch (Throwable ignored) { attempted.set(false); return; }
            if (session == null) { attempted.set(false); return; }
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
            fetch.invoke(initializer, session, "microg_fcm_support");
        } catch (Throwable ignored) { }
    }

    public static PendingIntent pendingBroadcast(Context c, int request, Intent intent, int flags) {
        if (Build.VERSION.SDK_INT >= 23 && (flags & (PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_MUTABLE)) == 0)
            flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, request, intent, flags);
    }
}
