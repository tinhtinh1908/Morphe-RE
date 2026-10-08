package vn.dtinh.tests
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.w3c.dom.Element
private fun original(s:String):String? = if(s == "app.revanced.android.gms" || s.startsWith("app.revanced.android.c2dm.") || s == "app.revanced.iid.TOKEN_REQUEST" || s == "app.revanced.android.gcm.intent.SEND") s.replaceFirst("app.revanced", "com.google") else if(s.startsWith("dtinh.microg.firebase.appid.v2")) s.replace("dtinh.microg.firebase.appid.v2", "com.google.android.gms.appid") else null
private val manifestFixture = resourcePatch {
 execute {
  document("AndroidManifest.xml").use { doc ->
   val nodes=doc.getElementsByTagName("*")
   val remove=mutableListOf<Element>()
   for(i in 0 until nodes.length){val e=nodes.item(i) as Element
    if(e.tagName=="meta-data" && e.getAttribute("android:name") in setOf("app.revanced.MICROG_PACKAGE_NAME","app.revanced.android.gms.SPOOFED_PACKAGE_NAME","app.revanced.android.gms.SPOOFED_PACKAGE_SIGNATURE"))remove+=e
    if(e.tagName in setOf("action","permission","uses-permission","package")) original(e.getAttribute("android:name"))?.let { e.setAttribute("android:name",it) }
    original(e.getAttribute("android:permission"))?.let { e.setAttribute("android:permission",it) }
   }
   remove.forEach { it.parentNode.removeChild(it) }
  }
 }
}
val unrouteFixture=bytecodePatch(name="Test fixture remove microG route",default=false) {
 dependsOn(manifestFixture)
 execute {
  classDefForEach { c ->
   for(source in c.methods) {
    val changes=source.implementation?.instructions?.mapIndexedNotNull { index,i ->
     val value=((i as? ReferenceInstruction)?.reference as? StringReference)?.string
     if(value==null)null else original(value)?.let { Triple(index,(i as OneRegisterInstruction).registerA,it) }
    } ?: continue
    val instructions = source.implementation?.instructions?.toList() ?: continue
    val supportCalls = instructions.mapIndexedNotNull { index, i ->
     val ref = ((i as? ReferenceInstruction)?.reference as? MethodReference) ?: return@mapIndexedNotNull null
     if (!c.type.startsWith("Lvn/dtinh/messenger/") && ref.definingClass in setOf("Lvn/dtinh/messenger/MicroGFcmSupport;", "Lvn/dtinh/messenger/MicroGFcmSupportV5;")) index to i else null
    }
    if(changes.isNotEmpty() || supportCalls.isNotEmpty()) {
     val m=mutableClassDefBy(c).methods.first { it.name==source.name && it.parameterTypes==source.parameterTypes && it.returnType==source.returnType }
     changes.forEach { (index,reg,value) -> m.replaceInstruction(index,BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,reg,ImmutableStringReference(value))) }
     supportCalls.sortedByDescending { it.first }.forEach { (index, i) ->
      val ref = (i as ReferenceInstruction).reference as MethodReference
      if (ref.name == "pendingBroadcast") {
       val args = i as FiveRegisterInstruction
       m.replaceInstruction(index, "invoke-static {v${args.registerC}, v${args.registerD}, v${args.registerE}, v${args.registerF}}, Landroid/app/PendingIntent;->getBroadcast(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;")
      } else m.removeInstruction(index)
     }
    }
   }
  }
 }
}
