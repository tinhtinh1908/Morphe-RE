import java.io.*;import java.util.*;import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
public class VerifyNoUi { public static void main(String[] a)throws Exception {
 var dex=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.getDefault());var seen=new HashSet<String>();var calls=new TreeMap<String,Integer>();int stores=0;int extension=0;
 for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses()) {
  if(!seen.add(c.getType()))throw new AssertionError("Duplicate class");
  if(c.getType().contains("FcmDiagnostics")||c.getType().contains("DiagnosticsActivity"))throw new AssertionError("Diagnostic class retained");
  if(c.getType().startsWith("Lvn/dtinh/messenger/")) {
   extension++;
   if("Landroid/app/Activity;".equals(c.getSuperclass()))throw new AssertionError("Added Activity");
  }
  for(Method m:c.getMethods())if(m.getImplementation()!=null) {
   Instruction previous=null;
   for(Instruction i:m.getImplementation().getInstructions()) {
    if((i.getOpcode()==Opcode.MOVE_RESULT || i.getOpcode()==Opcode.MOVE_RESULT_OBJECT || i.getOpcode()==Opcode.MOVE_RESULT_WIDE) &&
       (previous==null || !previous.getOpcode().setsResult()))throw new AssertionError("Broken invoke/move-result in "+m);
    previous=i;
    if(i instanceof ReferenceInstruction) {
   var ref=((ReferenceInstruction)i).getReference();
   if(ref.toString().contains("FcmDiagnostics")||ref.toString().contains("DiagnosticsActivity"))throw new AssertionError("Diagnostic reference retained");
   if(ref instanceof StringReference && ((StringReference)ref).getString().startsWith("dtinh.microg.firebase.appid.v2"))stores++;
   if(ref instanceof MethodReference){var r=(MethodReference)ref;
    if(!c.getType().startsWith("Lvn/dtinh/messenger/") && r.getDefiningClass().equals("Lvn/dtinh/messenger/MicroGFcmSupport;"))throw new AssertionError("Old 0.4 runtime hook remains active");
    if(c.getType().startsWith("Lvn/dtinh/messenger/") && (r.getDefiningClass().equals("Landroid/widget/Toast;")||r.getDefiningClass().equals("Landroid/util/Log;")||r.getDefiningClass().equals("Landroid/content/ClipboardManager;")||r.getDefiningClass().equals("Landroid/content/SharedPreferences;")))throw new AssertionError("UI/logging call retained");
    if(!c.getType().startsWith("Lvn/dtinh/messenger/") && r.getDefiningClass().equals("Lvn/dtinh/messenger/MicroGFcmSupportV5;"))calls.merge(r.getName(),1,Integer::sum);
   }
  }
   }
  }
 }
 if(stores!=3 || calls.size()!=3 || calls.getOrDefault("boot",0)!=1 || calls.getOrDefault("pendingBroadcast",0)!=1 || calls.getOrDefault("tokenReady",0)!=1)throw new AssertionError("Unexpected hooks "+calls+" stores="+stores);
 System.out.println("PASS: "+seen.size()+" unique classes; "+extension+" runtime extension classes; no diagnostic classes/references, added Activity or UI/logging calls; 3 token store literals; 3 FCM support hooks.");
}}
