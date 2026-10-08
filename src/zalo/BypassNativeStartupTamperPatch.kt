// SPDX-License-Identifier: GPL-3.0-only
// Derived from zeldrisho/morphe-patches, commit 9418386935c67b382a58cbac7084eb67bc20ae86.
// Modified by DTinh on 2026-10-08: Vietnamese metadata, independent compatibility,
// accept already-patched sites, preserve all other bytes. See THIRD_PARTY_NOTICES.md.
package vn.dtinh.patches.zalo.native

import app.morphe.patcher.patch.rawResourcePatch
import java.io.RandomAccessFile

private const val LIBRARY_PATH = "lib/arm64-v8a/libnative_utils.so"
private const val INIT_BRANCH_OFFSET = 0x2812cL
private val ORIGINAL_INIT_BRANCH = byteArrayOf(0x00, 0x01, 0x00, 0x36)

private const val CONFIG_CALL_OFFSET = 0x28160L
private val ORIGINAL_CONFIG_CALL = byteArrayOf(0xa9.toByte(), 0x10, 0x00, 0x94.toByte())

// CallStaticVoidMethodV through JNIEnv, inside the helper reached by 0x2c404.
private const val EXIT_CALL_OFFSET = 0x2c4ecL
private val ORIGINAL_EXIT_CALL = byteArrayOf(0x00, 0x01, 0x3f, 0xd6.toByte())
private val NOP = byteArrayOf(0x1f, 0x20, 0x03, 0xd5.toByte())
private const val HEX_RADIX = 16

/**
 * Forces the successful native cryptographic initialization path while
 * suppressing the JNI CallStaticVoidMethodV that dispatches
 * java/lang/System.exit(I)V.
 *
 * This is intentionally pinned to Zalo 26.08.01 and the arm64 native library.
 * All three instruction sites are checked before writing. Already-patched sites are accepted.
 */
@Suppress("unused")
val bypassZaloNativeStartupTamperPatch = rawResourcePatch(
    name = "Bỏ qua kiểm tra chỉnh sửa khi khởi động Zalo",
    description = "Zalo 26.08.01 (ARM64): bỏ qua nhánh kiểm tra native và lệnh JNI gọi System.exit, giữ nguyên lệnh khởi tạo cấu hình. Nguồn: zeldrisho/morphe-patches, GPL v3. Chọn độc lập với bản vá microG.",
    default = false,
) {
    compatibleWith("com.zing.zalo"("26.08.01"))

    execute {
        patchNativeLibrary(get(LIBRARY_PATH, true))
    }
}

/** Validates every pinned site before mutating the native library. */
internal fun patchNativeLibrary(library: java.io.File) {
    RandomAccessFile(library, "rw").use { file ->
        fun readAt(offset: Long, expected: ByteArray, label: String, allowNop: Boolean = false) {
            file.seek(offset)
            val actual = ByteArray(expected.size)
            file.readFully(actual)
            check(actual.contentEquals(expected) || (allowNop && actual.contentEquals(NOP))) {
                "Unexpected $label bytes at 0x${offset.toString(HEX_RADIX)}"
            }
        }

        readAt(INIT_BRANCH_OFFSET, ORIGINAL_INIT_BRANCH, "initialization-branch", true)
        readAt(CONFIG_CALL_OFFSET, ORIGINAL_CONFIG_CALL, "config-call")
        readAt(EXIT_CALL_OFFSET, ORIGINAL_EXIT_CALL, "JNI exit-call", true)

        file.seek(INIT_BRANCH_OFFSET)
        file.write(NOP)
        file.seek(EXIT_CALL_OFFSET)
        file.write(NOP)
    }
}
