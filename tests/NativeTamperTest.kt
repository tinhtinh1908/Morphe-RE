package vn.dtinh.patches.zalo.native

import java.io.File

private val nop = byteArrayOf(0x1f, 0x20, 0x03, 0xd5.toByte())
private val sites = mapOf(
    0x2812c to byteArrayOf(0, 1, 0, 0x36),
    0x28160 to byteArrayOf(0xa9.toByte(), 0x10, 0, 0x94.toByte()),
    0x2c4ec to byteArrayOf(0, 1, 0x3f, 0xd6.toByte())
)
private fun fixture(): ByteArray = ByteArray(0x2c500) { 0x55 }.also { bytes ->
    sites.forEach { (offset, expected) -> expected.copyInto(bytes, offset) }
}
private fun test(input: ByteArray, success: Boolean, expected: ByteArray = input) {
    val file = File.createTempFile("dtinh-native-test", ".so")
    try {
        file.writeBytes(input)
        val result = runCatching { patchNativeLibrary(file) }
        check(result.isSuccess == success) { "Unexpected patch result" }
        check(file.readBytes().contentEquals(expected)) { "Unexpected bytes changed" }
    } finally { file.delete() }
}
fun main() {
    val original = fixture()
    val patched = original.copyOf().also { nop.copyInto(it, 0x2812c); nop.copyInto(it, 0x2c4ec) }
    test(original, true, patched)
    test(patched, true, patched)
    for (offset in listOf(0x2812c, 0x2c4ec)) {
        test(original.copyOf().also { nop.copyInto(it, offset) }, true, patched)
    }
    for (offset in sites.keys) {
        test(original.copyOf().also { it[offset] = 0x7f }, false)
    }
    test(original.copyOf(0x2c4ed), false)
    println("Native tamper tests: PASS (fresh, repeated, partial, three mismatches, truncated; full-byte comparison)")
}
