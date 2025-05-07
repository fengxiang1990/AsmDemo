package com.fxa.transformer.timercost;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodTimerClassVisitor extends ClassVisitor {
    private String className;

    public MethodTimerClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        if (mv != null && !name.equals("<init>") && !name.equals("<clinit>")) {
            mv = new MethodTimerMethodVisitor(mv, className, name,access,descriptor);
        }
        return mv;
    }
}
