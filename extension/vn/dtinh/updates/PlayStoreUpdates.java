/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2470
 *
 * File-Specific License Notice (GPLv3 Section 7 Terms)
 *
 * This file is part of the Morphe project and is licensed under
 * the GNU General Public License version 3 (GPLv3), with the Additional
 * Terms under Section 7 described in the LICENSE file.
 *
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Section 7b: Notice Preservation
 * -------------------------------
 * This entire comment block must be preserved in all copies,
 * distributions, and derivative works of this file, in both
 * original and modified source forms.
 *
 * Portions of this software are provided "AS IS" by the Morphe software project.
 * Any express or implied warranties, including the implied warranties of
 * merchantability and fitness for a particular purpose, are disclaimed.
 */

// SPDX-License-Identifier: GPL-3.0-only
// Adapted by DTinh on 2026-10-14: package, app-scoped checks, no AndroidX dependency.
// Upstream commit e5464395441e916b643af4e2175c4e253e6df487; see THIRD_PARTY_NOTICES.md.
package vn.dtinh.updates;

import android.content.pm.PackageInfo;

public final class PlayStoreUpdates {
    private PlayStoreUpdates() {}
    private static int originalVersionCode() {
        return 0; // Replaced by the bytecode dependency using saved manifest metadata.
    }
    private static String originalPackageName() {
        return ""; // Replaced during patching; do not spoof other installed packages.
    }
    private static boolean isPatchedApp(PackageInfo info) {
        return originalPackageName().equals(info.packageName);
    }
    @SuppressWarnings("deprecation")
    public static int getVersionCode(PackageInfo info) {
        return isPatchedApp(info) && info.versionCode == Integer.MAX_VALUE
            ? originalVersionCode() : info.versionCode;
    }
    public static long getVersionCodeLong(PackageInfo info) {
        long code = info.getLongVersionCode();
        return isPatchedApp(info) && (code & 0xffffffffL) == Integer.MAX_VALUE
            ? (code & 0xffffffff00000000L) | (originalVersionCode() & 0xffffffffL)
            : code;
    }
}

