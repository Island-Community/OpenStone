package stone.boot;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class Bridge {
    static final String FAILED = "failed";
    static final String FAILED_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/Throwable;)V";

    private static final String HANDLE = "java/lang/invoke/MethodHandle";
    private static final String HANDLE_DESCRIPTOR = "L" + HANDLE + ";";

    private Bridge() {}

    static String internalName() {
        StringBuilder shape = new StringBuilder();
        for (Hook hook : Hook.ALL) {
            shape.append(hook.handler()).append(hook.handlerDescriptor());
            if (hook.at().kind() == Hook.At.Kind.REDIRECT) {
                shape.append(hook.at());
            }
            shape.append(';');
        }
        return "stone/Bridge_" + Integer.toHexString(shape.toString().hashCode());
    }

    static String methodName(Hook hook) {
        return hook.handler();
    }

    static byte[] generate(String name) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                name, null, "java/lang/Object", null);
        int fieldAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE;
        writer.visitField(fieldAccess, FAILED, HANDLE_DESCRIPTOR, null, null).visitEnd();
        for (Hook hook : Hook.ALL) {
            writer.visitField(fieldAccess, methodName(hook), HANDLE_DESCRIPTOR, null, null).visitEnd();
            method(writer, name, hook);
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void method(ClassWriter writer, String owner, Hook hook) {
        String descriptor = hook.handlerDescriptor();
        Type[] params = Type.getArgumentTypes(descriptor);
        Type returned = Type.getReturnType(descriptor);
        MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                methodName(hook), descriptor, null, null);
        mv.visitCode();
        Label start = new Label();
        Label handler = new Label();
        mv.visitTryCatchBlock(start, handler, handler, "java/lang/Throwable");

        mv.visitLabel(start);
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, methodName(hook), HANDLE_DESCRIPTOR);
        int thrown = load(mv, params);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, HANDLE, "invokeExact", descriptor, false);
        mv.visitInsn(returnOpcode(returned));

        mv.visitLabel(handler);
        Object[] locals = frameLocals(null, params);
        mv.visitFrame(Opcodes.F_FULL, locals.length, locals, 1, new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(Opcodes.ASTORE, thrown);
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, FAILED, HANDLE_DESCRIPTOR);
        mv.visitLdcInsn(hook.handler());
        mv.visitVarInsn(Opcodes.ALOAD, thrown);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, HANDLE, "invokeExact", FAILED_DESCRIPTOR, false);
        Hook.At at = hook.at();
        if (at.kind() == Hook.At.Kind.REDIRECT) {
            load(mv, params);
            if (at.isField()) {
                mv.visitFieldInsn(at.opcode(), at.owner(), at.name(), at.descriptor());
            } else {
                mv.visitMethodInsn(at.opcode(), at.owner(), at.name(), at.descriptor(),
                        at.opcode() == Opcodes.INVOKEINTERFACE);
            }
        } else if (returned.getSort() != Type.VOID) {
            mv.visitInsn(Opcodes.ICONST_0);
        }
        mv.visitInsn(returnOpcode(returned));

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static int load(MethodVisitor mv, Type[] params) {
        int slot = 0;
        for (Type type : params) {
            mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), slot);
            slot += type.getSize();
        }
        return slot;
    }

    static int returnOpcode(Type type) {
        return type.getSort() == Type.VOID ? Opcodes.RETURN : type.getOpcode(Opcodes.IRETURN);
    }

    static int zero(Type type) {
        return switch (type.getSort()) {
            case Type.LONG -> Opcodes.LCONST_0;
            case Type.FLOAT -> Opcodes.FCONST_0;
            case Type.DOUBLE -> Opcodes.DCONST_0;
            case Type.OBJECT, Type.ARRAY -> Opcodes.ACONST_NULL;
            default -> Opcodes.ICONST_0;
        };
    }

    static Object[] frameLocals(String self, Type[] params) {
        List<Object> locals = new ArrayList<>();
        if (self != null) {
            locals.add(self);
        }
        for (Type type : params) {
            locals.add(switch (type.getSort()) {
                case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> Opcodes.INTEGER;
                case Type.FLOAT -> Opcodes.FLOAT;
                case Type.LONG -> Opcodes.LONG;
                case Type.DOUBLE -> Opcodes.DOUBLE;
                default -> type.getInternalName();
            });
        }
        return locals.toArray();
    }
}
