import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class Widen {
    private static int widen(int access) {
        return (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[1]);
        Files.createDirectories(out.toAbsolutePath().getParent());
        try (InputStream in = Files.newInputStream(Path.of(args[0]));
             ZipInputStream zin = new ZipInputStream(in);
             OutputStream os = Files.newOutputStream(out);
             ZipOutputStream zout = new ZipOutputStream(os)) {
            for (ZipEntry entry; (entry = zin.getNextEntry()) != null; ) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                ClassReader reader = new ClassReader(zin.readAllBytes());
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        super.visit(Opcodes.V17, widen(access), name, signature, superName, interfaces);
                    }

                    @Override
                    public void visitInnerClass(String name, String outerName, String innerName, int access) {
                        super.visitInnerClass(name, outerName, innerName, widen(access));
                    }

                    @Override
                    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                        return super.visitField(widen(access), name, descriptor, signature, value);
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return super.visitMethod(widen(access), name, descriptor, signature, exceptions);
                    }
                }, ClassReader.SKIP_CODE);
                zout.putNextEntry(new ZipEntry(entry.getName()));
                zout.write(writer.toByteArray());
                zout.closeEntry();
            }
        }
    }
}
