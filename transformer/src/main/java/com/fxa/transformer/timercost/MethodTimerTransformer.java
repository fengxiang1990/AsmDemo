package com.fxa.transformer.timercost;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MethodTimerTransformer {

    public static void transform(String classFilePath) throws IOException {
        FileInputStream fis = new FileInputStream(classFilePath);
        ClassReader classReader = new ClassReader(fis);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor classVisitor = new MethodTimerClassVisitor(classWriter);

        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

        byte[] transformedClass = classWriter.toByteArray();
        FileOutputStream fos = new FileOutputStream(classFilePath);
        fos.write(transformedClass);
        fos.close();
    }
}
