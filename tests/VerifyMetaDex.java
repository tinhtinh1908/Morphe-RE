// SPDX-License-Identifier: GPL-3.0-only
import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;

/** Structural audit of the actual patched APK, including permission formats and FCM hooks. */
public final class VerifyMetaDex {
    static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static final Set<String> OLD = Set.of("com.facebook.permission.prod.FB_APP_COMMUNICATION",
        "com.facebook.permission.%s.FB_APP_COMMUNICATION", "com.facebook.receiver.permission.ACCESS");
    public static void main(String[] args) throws Exception {
        boolean facebook = args[1].equals("facebook");
        String pkg = facebook ? "com.facebook.katana" : "com.facebook.orca";
        String helper = facebook ? "Lvn/dtinh/facebook/FacebookMicroGFcmSupportV2;" : "Lvn/dtinh/messenger/MicroGFcmSupportV5;";
        String cache = facebook ? "dtinh.microg.facebook.appid.v2" : "dtinh.microg.firebase.appid.v2";
        var dex = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.getDefault());
        var classes = new HashSet<String>();
        var hooks = new TreeMap<String,Integer>();
        int caches = 0, scopedPermissions = 0, routes = 0;
        for (String entry : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(entry).getDexFile().getClasses()) {
            require(classes.add(c.getType()), "Duplicate class: " + c.getType());
            for (Field f : c.getStaticFields()) if (f.getInitialValue() instanceof StringEncodedValue)
                require(!OLD.contains(((StringEncodedValue) f.getInitialValue()).getValue()), "Old permission field");
            for (Method m : c.getMethods()) if (m.getImplementation() != null) {
                Instruction previous = null;
                for (Instruction i : m.getImplementation().getInstructions()) {
                    if (Set.of(Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT, Opcode.MOVE_RESULT_WIDE).contains(i.getOpcode()))
                        require(previous != null && previous.getOpcode().setsResult(), "Broken move-result: " + c.getType() + "->" + m.getName());
                    previous = i;
                    if (!(i instanceof ReferenceInstruction)) continue;
                    var ref = ((ReferenceInstruction) i).getReference();
                    if (ref instanceof StringReference) {
                        String s = ((StringReference) ref).getString();
                        require(!OLD.contains(s), "Old Meta permission in DEX: " + c.getType());
                        if (s.startsWith(pkg + ".dtinh.")) scopedPermissions++;
                        if (s.startsWith(cache)) caches++;
                        if (s.startsWith("app.revanced.android.c2dm.") || s.equals("app.revanced.iid.TOKEN_REQUEST")) routes++;
                        if (facebook && Set.of("LX/1eN;", "LX/1fi;", "LX/1fs;", "LX/1fu;", "LX/cd1;", "LX/fAd;", "LX/19g;", "LX/6I6;", "LX/7Zj;", "LX/8cE;").contains(c.getType()))
                            require(!s.equals("com.google.android.gms") && !s.startsWith("com.google.android.c2dm.") && !s.equals("com.google.iid.TOKEN_REQUEST"), "Old FCM route: " + c.getType());
                    }
                    if (ref instanceof MethodReference) {
                        MethodReference call = (MethodReference) ref;
                        if (facebook && !c.getType().startsWith("Lvn/dtinh/"))
                            require(!call.getDefiningClass().equals("Lvn/dtinh/facebook/FacebookMicroGFcmSupport;"), "Stale dev.1 hook");
                        if (call.getDefiningClass().equals(helper) && !c.getType().startsWith("Lvn/dtinh/")) {
                            hooks.merge(call.getName(), 1, Integer::sum);
                            if (call.getName().equals("tokenReady"))
                                require(m.getName().equals(facebook ? "Cx7" : "C2F"), "Wrong readiness hook");
                            if (call.getName().equals("availability"))
                                require(c.getType().equals("LX/86K;") && m.getName().equals("Cx7"), "Availability modified outside Facebook FCM");
                        }
                        if (c.getType().startsWith("Lvn/dtinh/facebook/"))
                            require(!call.getDefiningClass().contains("Socket") && !call.getDefiningClass().equals("Landroid/util/Log;") && !call.getDefiningClass().equals("Landroid/widget/Toast;"), "Unwanted runtime behavior");
                    }
                }
            }
        }
        require(caches == (facebook ? 4 : 3), "Wrong token cache count: " + caches);
        require(scopedPermissions > 0 && routes > 0, "Missing permission or FCM changes");
        var expected = new TreeMap<String,Integer>();
        for (String name : List.of("boot", "pendingBroadcast", "tokenReady")) expected.put(name, 1);
        if (facebook) expected.put("availability", 1);
        require(hooks.equals(expected), "Unexpected hooks: " + hooks);
        System.out.println("PASS: " + pkg + "; " + classes.size() + " unique classes; " + hooks + "; " + caches + " cache literals; permission names/formats isolated; move-result adjacency valid");
    }
}
