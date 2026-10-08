// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.gmail;
import android.content.Context;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
public final class GmailBackendV7 {
    private GmailBackendV7() {}
    public static int availability(Context context) {
        if (context == null) return 1;
        try {
            PackageManager pm = context.getPackageManager();
            ServiceInfo service = pm.getServiceInfo(new ComponentName("app.revanced.android.gms", "com.google.android.gms.auth.GetToken"), 0);
            if (!service.enabled || !service.exported || !service.applicationInfo.enabled) return 3;
            int state = pm.getComponentEnabledSetting(new ComponentName("app.revanced.android.gms", "com.google.android.gms.auth.GetToken"));
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) return 3;
            if (pm.checkPermission("app.revanced.android.c2dm.permission.SEND", "app.revanced.android.gms") != PackageManager.PERMISSION_GRANTED) return 9;
            return 0;
        } catch (PackageManager.NameNotFoundException e) { return 1; }
          catch (RuntimeException e) { return 9; }
    }
}
