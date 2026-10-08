// SPDX-License-Identifier: GPL-3.0-only
import java.io.File;
import java.util.HashSet;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
public final class VerifyGmail {
 public static void main(String[] args)throws Exception {
  var dex=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
  var classes=new HashSet<String>();int stores=0,accounts=0,getToken=0,check=0,ext=0;
  for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses()){
   if(!classes.add(c.getType()))throw new AssertionError("Duplicate class");
   boolean helper=c.getType().startsWith("Lvn/dtinh/gmail/");
   if(helper){ext++;if(!c.getSuperclass().equals("Ljava/lang/Object;"))throw new AssertionError("Added component");}
   if(c.getType().startsWith("Lvn/dtinh/messenger/")||c.getType().startsWith("Lvn/dtinh/zalo/"))throw new AssertionError("Wrong runtime merged");
   for(Method m:c.getMethods())if(m.getImplementation()!=null){Instruction last=null;
    for(Instruction i:m.getImplementation().getInstructions()){
     if((i.getOpcode()==Opcode.MOVE_RESULT||i.getOpcode()==Opcode.MOVE_RESULT_OBJECT||i.getOpcode()==Opcode.MOVE_RESULT_WIDE)&&(last==null||!last.getOpcode().setsResult()))throw new AssertionError("Broken move-result: "+m);
     last=i;if(!(i instanceof ReferenceInstruction))continue;
     var r=((ReferenceInstruction)i).getReference();
     if(r instanceof StringReference){String s=((StringReference)r).getString();
      if(s.startsWith("dtinh.microg.gmail.fcm.v1"))stores++;
      if(!helper && s.equals("app.revanced"))accounts++;
      if(c.getType().equals("Laboa;")&&s.equals("com.google.android.gms.auth.GetToken"))getToken++;
      if(s.equals("app.revanced.android.gms.auth.GetToken"))throw new AssertionError("Wrong component class");
      if(!helper&&s.equals("com.google"))throw new AssertionError("Old account type retained");
     }
     if(r instanceof MethodReference){var method=(MethodReference)r;
      if(!helper&&method.getDefiningClass().equals("Lvn/dtinh/gmail/GmailBackendV7;")){if(!method.getName().equals("availability"))throw new AssertionError("Extra runtime hook");check++;}
      if(helper&&(method.getDefiningClass().equals("Landroid/util/Log;")||method.getDefiningClass().equals("Landroid/widget/Toast;")||method.getDefiningClass().contains("Clipboard")||method.getDefiningClass().contains("Socket")))throw new AssertionError("Unwanted helper behavior");
     }
    }
   }
  }
  if(stores!=6||accounts!=237||getToken!=1||check!=1||ext!=1)throw new AssertionError("Unexpected stores/accounts/auth/check/extension "+stores+"/"+accounts+"/"+getToken+"/"+check+"/"+ext);
  System.out.println("PASS: "+classes.size()+" unique classes; 237 account literals; 6 caches; preserved GetToken class; 1 availability hook; no extra runtime/UI/socket; valid move-result adjacency");
 }
}
