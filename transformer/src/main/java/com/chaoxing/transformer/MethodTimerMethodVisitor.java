package com.chaoxing.transformer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.Type;

public class MethodTimerMethodVisitor extends LocalVariablesSorter {
    private String className;
    private String methodName;
    private int startTimeVarIndex;
    private int endTimeVarIndex;
    private int durationVarIndex;
    private int threadVarIndex;

    public MethodTimerMethodVisitor(MethodVisitor methodVisitor, String className, String methodName, int access, String desc) {
        super(Opcodes.ASM9, access, desc, methodVisitor);
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        // 记录方法开始时间
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        startTimeVarIndex = newLocal(Type.LONG_TYPE);
        mv.visitVarInsn(Opcodes.LSTORE, startTimeVarIndex);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            // 结束时间
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            endTimeVarIndex = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, endTimeVarIndex);

            // 计算耗时
            mv.visitVarInsn(Opcodes.LLOAD, endTimeVarIndex);
            mv.visitVarInsn(Opcodes.LLOAD, startTimeVarIndex);
            mv.visitInsn(Opcodes.LSUB);
            durationVarIndex = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, durationVarIndex);

            // 加载方法名
            mv.visitLdcInsn(className + "." + methodName);

            // 加载耗时
            mv.visitVarInsn(Opcodes.LLOAD, durationVarIndex);

            // 加载线程名
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);

            // 调用 log 方法
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                "com/chaoxing/transformer/MethodTimerLogUtil",
                "log",
                "(Ljava/lang/String;JLjava/lang/String;)V",
                false);
        }
        super.visitInsn(opcode);
    }
}
