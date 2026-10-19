// SPDX-License-Identifier: GPL-3.0-only
import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Follow the real modern IPC Intent and the backend-version lookup in the APK. */
public final class VerifyFacebookRpc {
    static final String MICROG = "app.revanced.android.gms";
    static final Map<String,ClassDef> classes = new HashMap<>();
    static void require(boolean v, String s) { if (!v) throw new AssertionError(s); }
    static Method method(String type, String name) {
        for (Method m : classes.get(type).getMethods()) if (m.getName().equals(name)) return m;
        throw new AssertionError("Missing method: " + type + "->" + name);
    }
    static String stringTable(int key) {
        Method m = method("LX/19g;", "A00");
        var code = new TreeMap<Integer,Instruction>();
        int pc = 0;
        for (Instruction i : m.getImplementation().getInstructions()) { code.put(pc, i); pc += i.getCodeUnits(); }
        for (var entry : code.entrySet()) if (entry.getValue().getOpcode() == Opcode.PACKED_SWITCH) {
            var payload = (SwitchPayload) code.get(entry.getKey() + ((OffsetInstruction)entry.getValue()).getCodeOffset());
            for (SwitchElement e : payload.getSwitchElements()) if (e.getKey() == key) {
                Instruction target = code.get(entry.getKey() + e.getOffset());
                require(target instanceof ReferenceInstruction, "Unexpected action table target");
                return ((StringReference)((ReferenceInstruction)target).getReference()).getString();
            }
        }
        throw new AssertionError("Missing action key " + key);
    }
    static void versionLookup(String type, String name) {
        var strings = new HashMap<Integer,String>();
        boolean verified = false;
        for (Instruction i : method(type,name).getImplementation().getInstructions()) {
            if (!(i instanceof ReferenceInstruction)) continue;
            var r = ((ReferenceInstruction)i).getReference();
            if (r instanceof StringReference && i instanceof OneRegisterInstruction)
                strings.put(((OneRegisterInstruction)i).getRegisterA(), ((StringReference)r).getString());
            if (r instanceof MethodReference && r.toString().equals("LX/1eN;->A00(LX/1eN;Ljava/lang/String;)Landroid/content/pm/PackageInfo;")) {
                int packageRegister = ((FiveRegisterInstruction)i).getRegisterD();
                require(MICROG.equals(strings.get(packageRegister)), type + " still reads Google backend: " + strings.get(packageRegister));
                verified = true;
            }
        }
        require(verified,"No backend lookup exercised in " + type);
    }
    static final class IntentValue { String action, pkg; IntentValue(String a) { action=a; } }
    static void modernBind() {
        Map<Integer,Object> registers = new HashMap<>();
        Object result = null;
        boolean verified = false;
        for (Instruction i : method("LX/cd1;","A02").getImplementation().getInstructions()) {
            if (i.getOpcode() == Opcode.MOVE_RESULT_OBJECT || i.getOpcode() == Opcode.MOVE_RESULT) {
                registers.put(((OneRegisterInstruction)i).getRegisterA(),result); result=null;
                continue;
            }
            if (i instanceof NarrowLiteralInstruction && i instanceof OneRegisterInstruction)
                registers.put(((OneRegisterInstruction)i).getRegisterA(),((NarrowLiteralInstruction)i).getNarrowLiteral());
            if (!(i instanceof ReferenceInstruction)) continue;
            var ref = ((ReferenceInstruction)i).getReference();
            if (ref instanceof StringReference) {
                registers.put(((OneRegisterInstruction)i).getRegisterA(), ((StringReference)ref).getString());
                continue;
            }
            if (!(ref instanceof MethodReference)) continue;
            String call = ref.toString();
            if (call.equals("LX/19g;->A00(I)Ljava/lang/String;"))
                result=stringTable((Integer)registers.get(((FiveRegisterInstruction)i).getRegisterC()));
            else if (call.equals("LX/001;->A07(Ljava/lang/String;)Landroid/content/Intent;"))
                result=new IntentValue((String)registers.get(((FiveRegisterInstruction)i).getRegisterC()));
            else if (call.equals("Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;")) {
                var args = (FiveRegisterInstruction)i;
                var intent=(IntentValue)registers.get(args.getRegisterC());
                intent.pkg=(String)registers.get(args.getRegisterD()); result=intent;
            } else if (call.startsWith("LX/4M5;->A01(Landroid/content/Context;Landroid/content/Intent;")) {
                var intent=(IntentValue)registers.get(((RegisterRangeInstruction)i).getStartRegister()+1);
                require(intent!=null,"No concrete Intent at service bind");
                require(MICROG.equals(intent.pkg),"Modern IPC still binds to " + intent.pkg);
                require("app.revanced.android.c2dm.intent.REGISTER".equals(intent.action),"Wrong IPC action: " + intent.action);
                verified=true; break;
            } else result=null;
        }
        require(verified,"No modern IPC bind call exercised");
    }
    public static void main(String[] args) throws Exception {
        var dex=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
        for (String entry:dex.getDexEntryNames()) for (ClassDef c:dex.getEntry(entry).getDexFile().getClasses()) classes.put(c.getType(),c);
        modernBind();
        versionLookup("LX/fAd;","run");
        versionLookup("LX/1fi;","A00");
        var calls=new HashSet<String>(); boolean threshold=false;
        for (Instruction i:method("LX/fAd;","run").getImplementation().getInstructions()) {
            if (i instanceof ReferenceInstruction) calls.add(((ReferenceInstruction)i).getReference().toString());
            if (i instanceof NarrowLiteralInstruction && ((NarrowLiteralInstruction)i).getNarrowLiteral()==12000000) threshold=true;
        }
        require(threshold && calls.stream().anyMatch(s->s.startsWith("LX/cd3;->A00(")) && calls.stream().anyMatch(s->s.startsWith("LX/1fs;->A00(")),"Transport choice/fallback changed");
        Method register=method("Lvn/dtinh/facebook/FacebookMicroGFcmSupportV2;", "register");
        boolean migrationCalled=false;
        for (Instruction i:register.getImplementation().getInstructions()) if (i instanceof ReferenceInstruction)
            if (((ReferenceInstruction)i).getReference().toString().equals("Lvn/dtinh/facebook/FacebookMicroGFcmSupportV2;->recoverPreviousBackend()V")) migrationCalled=true;
        require(migrationCalled,"Stale dev.1 runtime retained; migration hook missing");
        require(method("Lvn/dtinh/facebook/FacebookMicroGFcmSupportV2;", "recoverPreviousBackend").getImplementation()!=null,"Missing migration runtime");
        System.out.println("PASS: modern IPC bind Intent targets microG with relocated REGISTER action; both backend version lookups use microG; modern/legacy branch and fallback preserved");
    }
}
