package vn.dtinh.patches

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.*

private fun fixture(instructions: List<Instruction>, registers: Int = 256) = MutableMethod(
    ImmutableMethod("Lfixture/Version;", "read", emptyList(), "V", 9, emptySet(), emptySet(),
        ImmutableMethodImplementation(registers, instructions, emptyList(), emptyList())))

fun main() {
    check(originalUpdateVersion("12345", null) == 12345)
    check(originalUpdateVersion("2147483647", "12345") == 12345)
    for ((current, saved) in listOf("0" to null, "bad" to null, "2147483647" to null,
        "2147483647" to "2147483647", "12345" to "54321")) {
        check(runCatching { originalUpdateVersion(current, saved) }.isFailure)
    }
    val field = ImmutableFieldReference("Landroid/content/pm/PackageInfo;", "versionCode", "I")
    for ((dest, receiver) in listOf(0 to 1, 15 to 15, 4 to 0)) {
        val m = fixture(listOf(ImmutableInstruction22c(Opcode.IGET, dest, receiver, field),
            ImmutableInstruction10x(Opcode.RETURN_VOID)))
        check(rewriteUpdateVersionReads(m) == 1)
        val out = m.implementation!!.instructions.toList()
        check(out[0].opcode == Opcode.INVOKE_STATIC_RANGE)
        check((out[0] as RegisterRangeInstruction).startRegister == receiver)
        check(out[1].opcode == Opcode.MOVE_RESULT && (out[1] as OneRegisterInstruction).registerA == dest)
        check(out[2].opcode == Opcode.RETURN_VOID)
        check(rewriteUpdateVersionReads(m) == 0) // Reapply does not duplicate hooks.
    }
    val longRef = ImmutableMethodReference("Landroid/content/pm/PackageInfo;", "getLongVersionCode", emptyList(), "J")
    val m = fixture(listOf(
        ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 15, 0, 0, 0, 0, longRef),
        ImmutableInstruction11x(Opcode.MOVE_RESULT_WIDE, 200),
        ImmutableInstruction3rc(Opcode.INVOKE_VIRTUAL_RANGE, 255, 1, longRef),
        ImmutableInstruction10x(Opcode.RETURN_VOID)))
    check(rewriteUpdateVersionReads(m) == 2)
    val out = m.implementation!!.instructions.toList()
    check((out[0] as RegisterRangeInstruction).startRegister == 15)
    check(out[1].opcode == Opcode.MOVE_RESULT_WIDE && (out[1] as OneRegisterInstruction).registerA == 200)
    check((out[2] as RegisterRangeInstruction).startRegister == 255)
    check(((out[2] as ReferenceInstruction).reference as MethodReference).name == "getVersionCodeLong")
    check(rewriteUpdateVersionReads(m) == 0)
    val other = fixture(listOf(ImmutableInstruction22c(Opcode.IGET, 0, 1,
        ImmutableFieldReference("Lother/Info;", "versionCode", "I")), ImmutableInstruction10x(Opcode.RETURN_VOID)))
    check(rewriteUpdateVersionReads(other) == 0)
    println("Update blocking DEX tests: PASS (original/reapply/reject metadata, same registers, long/range/high registers, unrelated fields)")
}
