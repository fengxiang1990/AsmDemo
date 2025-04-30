package com.chaoxing.transformer.replace;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class MethodReplaceTransformer extends ClassVisitor {
    private String className;
    private final Map<String, MethodReplaceInfo> methodReplaceMap;

    public MethodReplaceTransformer(ClassVisitor classVisitor, Map<String, MethodReplaceInfo> methodReplaceMap) {
        super(Opcodes.ASM9, classVisitor);
        this.methodReplaceMap = methodReplaceMap;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodReplaceMethodVisitor(Opcodes.ASM9, mv, className, methodReplaceMap);
    }
}