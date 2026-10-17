// SPDX-License-Identifier: GPL-3.0-only
import java.io.File;
import java.util.Collections;
import com.reandroid.apk.ApkModule;

/** Check the Gmail identity and GServices permissions on first and repeated patching. */
public final class VerifyGmailManifest {
    public static void main(String[] paths) throws Exception {
        for (String path : paths) {
            try (var apk = ApkModule.loadApkFile(new File(path))) {
                var manifest = apk.getAndroidManifest();
                if (!manifest.getPackageName().equals("com.google.android.gm")) {
                    throw new AssertionError("Gmail package changed");
                }
                var permissions = manifest.getUsesPermissions();
                for (String permission : new String[] {
                    "com.google.android.providers.gsf.permission.READ_GSERVICES",
                    "app.revanced.android.providers.gsf.permission.READ_GSERVICES"
                }) {
                    if (Collections.frequency(permissions, permission) != 1) {
                        throw new AssertionError("Missing or duplicate permission: " + permission);
                    }
                }
                System.out.println("PASS: Gmail identity unchanged; both GServices permissions requested once");
            }
        }
    }
}
