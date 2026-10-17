// SPDX-License-Identifier: GPL-3.0-only
import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
/** Verify endpoint redirection without rewriting the identically named Feedback Binder. */
public final class VerifyGmailRouting {
 static final String FEEDBACK="com.google.android.gms.feedback.internal.IFeedbackService";
 static final List<String> SUFFIXES=List.of("android.gms.phenotype.service.START","android.gms.clearcut.service.START","android.gms.clearcut.bootcount.service.START","android.gms.pseudonymous.service.START","android.mobstore.service.START","android.gms.wearable.BIND","android.gms.people.service.START","android.gms.signin.service.START","android.gms.fonts.service.START","android.gms.chimera","android.gms.fonts","android.gms.phenotype","android.gsf.gservices","android.providers.gsf.permission.READ_GSERVICES");
 static Map<String,Integer> strings(String path)throws Exception {
  var counts=new HashMap<String,Integer>();
  var dex=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
  for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses()){
   if(c.getType().startsWith("Lvn/dtinh/"))continue;
   for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())
    if(i instanceof ReferenceInstruction && ((ReferenceInstruction)i).getReference() instanceof StringReference){
     String s=((StringReference)((ReferenceInstruction)i).getReference()).getString();
     counts.merge(s,1,Integer::sum);
     if(s.equals(FEEDBACK)||s.equals(FEEDBACK.replace("com.google.","app.revanced.")))counts.merge(c.getType()+"->"+m.getName()+":"+s,1,Integer::sum);
    }
  }
  return counts;
 }
 static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  var original=strings(args[0]);var out=strings(args[1]);int redirected=0;
  for(String suffix:SUFFIXES){String from="com.google."+suffix,to="app.revanced."+suffix;
   int n=original.getOrDefault(from,0);
   require(out.getOrDefault(from,0)==0,"Old endpoint retained: "+from);
   require(out.getOrDefault(to,0)==n+original.getOrDefault(to,0),"Wrong replacement count: "+to);
   redirected+=n;
  }
  for(String from:List.of("content://com.google.android.gsf.gservices", "content://com.google.android.gsf.gservices/prefix")) {
   String to=from.replace("com.google.","app.revanced.");
   require(original.getOrDefault(from,0)>0,"GServices path not exercised");
   require(out.getOrDefault(from,0)==0 && out.getOrDefault(to,0)==original.getOrDefault(from,0),"GServices route missed: "+from);
  }
  var dex=DexFileFactory.loadDexContainer(new File(args[1]),Opcodes.getDefault());
  int classifiers=0;
  for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())if(c.getType().equals("Lawrl;"))
   for(Method m:c.getMethods())if(m.getName().equals("D") && m.getParameterTypes().toString().equals("[Ljava/lang/String;]")) {
    boolean hash=false,literal=false;
    for(Instruction i:m.getImplementation().getInstructions()) {
     if(i instanceof NarrowLiteralInstruction) {
      int v=((NarrowLiteralInstruction)i).getNarrowLiteral();
      require(v!="com.google".hashCode(),"Stale Google account hash");
      if(v=="app.revanced".hashCode())hash=true;
     }
     if(i instanceof ReferenceInstruction && ((ReferenceInstruction)i).getReference() instanceof StringReference)
      if(((StringReference)((ReferenceInstruction)i).getReference()).getString().equals("app.revanced"))literal=true;
    }
    require(hash&&literal,"microG account classifier is unreachable");classifiers++;
   }
  require(classifiers==1,"Missing account classifier");
  require(redirected>0,"No routing changes exercised");
  require(out.getOrDefault("Lacjh;->c:"+FEEDBACK,0)==1,"Feedback descriptor was modified");
  require(out.getOrDefault("Lacjh;->b:"+FEEDBACK,0)==1,"Feedback queryLocalInterface descriptor was modified");
  require(out.getOrDefault("Lacjj;-><init>:"+FEEDBACK,0)==1,"Feedback Binder proxy descriptor was modified");
  require(out.getOrDefault("Lacjh;->d:"+FEEDBACK.replace("com.google.","app.revanced."),0)==1,"Feedback action was not redirected");
  System.out.println("PASS: "+redirected+" endpoint literals redirected; account hash and both GServices paths verified; Feedback action routed and all three Binder descriptor literals preserved");
 }
}
