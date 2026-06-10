package cam72cam.mod.loader;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;

public class ProxyTransformer implements IClassTransformer {

    //TODO Dev environment proxy
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        ClassReader reader = new ClassReader(basicClass);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        if (node.visibleAnnotations == null) {
            return basicClass;
        }

        AnnotationNode proxyAnnotation = node.visibleAnnotations.stream()
                                                                .filter(a -> a.desc.equals("Lcam72cam/mod/loader/Proxied;"))
                                                                .findFirst()
                                                                .orElse(null);

        if (proxyAnnotation == null) {
            return basicClass;
        }

        String target = getAnnotationValue(proxyAnnotation, "target", "");
        if (target.isEmpty()) {
            return basicClass;
        }

        String targetInternal = target.replace('.', '/');
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0 || method.name.equals("<clinit>")) {
                continue;
            }
            redirectStatic(method, targetInternal);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void redirectStatic(MethodNode method, String targetInternal) {
        method.instructions.clear();
        InsnList il = new InsnList();
        //Load all args
        int slot = 0;
        for (Type arg : Type.getArgumentTypes(method.desc)) {
            il.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
            slot += arg.getSize();
        }

        il.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                targetInternal,
                method.name,
                method.desc,
                targetInternal.charAt(0) != '['
        ));

        Type returnType = Type.getReturnType(method.desc);
        il.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));

        method.instructions.add(il);
    }

    private static String getAnnotationValue(AnnotationNode annotation, String key, String defaultValue) {
        if (annotation.values == null) return defaultValue;
        List<Object> values = annotation.values;
        for (int i = 0; i < values.size() - 1; i += 2) {
            if (key.equals(String.valueOf(values.get(i)))) {
                Object val = values.get(i + 1);
                return val == null ? defaultValue : val.toString();
            }
        }
        return defaultValue;
    }
}