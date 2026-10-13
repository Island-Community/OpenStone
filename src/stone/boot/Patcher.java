package stone.boot;

import stone.core.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

final class Patcher {
    private static final String SCOPE = "patch";

    private Patcher() {}

    static byte[] apply(byte[] bytes, List<Hook> hooks, String bridge) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);

        Map<MethodNode, AbstractInsnNode> heads = new HashMap<>();
        for (MethodNode method : node.methods) {
            heads.put(method, method.instructions.getFirst());
        }
        Set<MethodNode> cancelled = new HashSet<>();
        int applied = 0;
        for (Hook hook : hooks) {
            MethodNode method = node.methods.stream()
                    .filter(m -> m.name.equals(hook.method()) && m.desc.equals(hook.descriptor()))
                    .findFirst().orElse(null);
            if (method == null) {
                Log.warn(SCOPE, "%s.%s%s not found; hook %s skipped",
                        hook.owner(), hook.method(), hook.descriptor(), hook.handler());
                continue;
            }
            List<AbstractInsnNode> anchors = anchors(method, hook.at(), heads.get(method));
            if (anchors.isEmpty()) {
                Log.warn(SCOPE, "%s.%s: no anchor %s; hook %s skipped",
                        hook.owner(), hook.method(), hook.at(), hook.handler());
                continue;
            }
            for (AbstractInsnNode anchor : anchors) {
                switch (hook.at().kind()) {
                    case HEAD, RETURN, BEFORE -> method.instructions.insertBefore(anchor, call(hook, method, bridge));
                    case AFTER -> method.instructions.insert(anchor, call(hook, method, bridge));
                    case REDIRECT -> method.instructions.set(anchor, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            bridge, Bridge.methodName(hook), hook.handlerDescriptor(), false));
                }
            }
            if (hook.cancellable()) {
                cancelled.add(method);
            }
            ++applied;
        }
        if (applied == 0) {
            return null;
        }
        cancelled.forEach(Patcher::dropShadowedFrames);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static List<AbstractInsnNode> anchors(MethodNode method, Hook.At at, AbstractInsnNode head) {
        List<AbstractInsnNode> found = new ArrayList<>();
        if (head == null) {
            return found;
        }
        switch (at.kind()) {
            case HEAD -> found.add(head);
            case RETURN -> {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                        found.add(insn);
                    }
                }
            }
            default -> {
                int seen = 0;
                for (AbstractInsnNode insn : method.instructions) {
                    if (!matches(insn, at)) {
                        continue;
                    }
                    if (at.ordinal() < 0 || seen == at.ordinal()) {
                        found.add(insn);
                    }
                    ++seen;
                }
            }
        }
        return found;
    }

    private static boolean matches(AbstractInsnNode insn, Hook.At at) {
        if (insn.getOpcode() != at.opcode()) {
            return false;
        }
        if (insn instanceof MethodInsnNode invoke) {
            return invoke.owner.equals(at.owner()) && invoke.name.equals(at.name()) && invoke.desc.equals(at.descriptor());
        }
        if (insn instanceof FieldInsnNode field) {
            return field.owner.equals(at.owner()) && field.name.equals(at.name()) && field.desc.equals(at.descriptor());
        }
        return false;
    }

    private static InsnList call(Hook hook, MethodNode method, String bridge) {
        InsnList list = new InsnList();
        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        if (hook.self()) {
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        Type[] params = Type.getArgumentTypes(method.desc);
        if (hook.args()) {
            int slot = isStatic ? 0 : 1;
            for (Type type : params) {
                list.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), slot));
                slot += type.getSize();
            }
        }
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, bridge, Bridge.methodName(hook),
                hook.handlerDescriptor(), false));
        if (hook.cancellable()) {
            LabelNode proceed = new LabelNode();
            Type returned = Type.getReturnType(method.desc);
            list.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
            if (returned.getSort() != Type.VOID) {
                list.add(new InsnNode(Bridge.zero(returned)));
            }
            list.add(new InsnNode(Bridge.returnOpcode(returned)));
            list.add(proceed);
            Object[] locals = Bridge.frameLocals(isStatic ? null : hook.owner(), params);
            list.add(new FrameNode(Opcodes.F_NEW, locals.length, locals, 0, new Object[0]));
        }
        return list;
    }

    private static void dropShadowedFrames(MethodNode method) {
        FrameNode previous = null;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof FrameNode frame) {
                if (previous != null) {
                    method.instructions.remove(previous);
                }
                previous = frame;
            } else if (insn.getOpcode() >= 0) {
                previous = null;
            }
        }
    }
}
