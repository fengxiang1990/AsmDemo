package com.chaoxing.transformer;

import com.chaoxing.transformer.replace.FindReplaceRuleUtil;
import com.chaoxing.transformer.replace.MethodReplaceRuleProcessor;
import com.chaoxing.transformer.replace.MethodReplaceTransformer;
import com.chaoxing.transformer.replace.MethodReplaceInfo;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.LibraryVariant;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MethodReplacePlugin implements Plugin<Project> {
    private  Map<String, MethodReplaceInfo> methodReplaceMap = null;

    @Override
    public void apply(Project project) {
        System.out.println("MethodReplacePlugin start thread->"+Thread.currentThread().getName()+" "+Thread.currentThread().getId());
        this.methodReplaceMap = FindReplaceRuleUtil.getInstance().getMethodReplaceMap();
        new MethodReplaceRuleProcessor(this.methodReplaceMap).process();
        project.afterEvaluate(proj -> {
            System.out.println("MethodReplacePlugin proj->"+proj);
            AppExtension android = proj.getExtensions().findByType(AppExtension.class);
            LibraryExtension library = proj.getExtensions().findByType(LibraryExtension.class);

            if (android != null) {
                android.getApplicationVariants().all(variant -> {
                    configureTaskForApplicationVariant(project, variant);
                });
            }
            if (library != null) {
                library.getLibraryVariants().all(variant -> {
                    configureTaskForLibraryVariant(project, variant);
                });
            }
        });

        System.out.println("MethodReplacePlugin end");
    }

    private void configureTaskForApplicationVariant(Project project, ApplicationVariant variant) {
        String variantName = variant.getName();
        configureTaskForVariant(project, variantName);
    }

    private void configureTaskForLibraryVariant(Project project, LibraryVariant variant) {
        String variantName = variant.getName();
        configureTaskForVariant(project, variantName);
    }

    private void configureTaskForVariant(Project project, String variantName) {
        String capitalizedVariantName = variantName.substring(0, 1).toUpperCase() + variantName.substring(1);

        project.getTasks().configureEach(task -> {
            String javaTaskName = getJavaTaskName(capitalizedVariantName);
            String kotlinTaskName = getKotlinTaskName(capitalizedVariantName);
            if (task.getName().equals(javaTaskName) || task.getName().equals(kotlinTaskName)) {
                System.out.println("MethodReplacePlugin task in: " + task.getName());
                task.doLast(t -> {
                    System.out.println("MethodReplacePlugin task -> " + t.getName());
                    try {
                        // Get all class files (Java and Kotlin) regardless of the task
                        List<String> classFiles = getAllClassFiles(project, variantName);

                        for (String key : methodReplaceMap.keySet()) {
                            System.out.println("fxa Replacement map1: " + key + " -> " + methodReplaceMap.get(key));
                        }

                        // Debug: Log the methodReplaceMap contents
                        System.out.println("methodReplaceMap after first pass: " + methodReplaceMap);
                        // Second Pass: Apply replacements
                        for (String classFilePath : classFiles) {
                            System.out.println("Second pass - applying replacements to: " + classFilePath);
                            applyReplacements(Path.of(classFilePath));
                        }
                    } catch (Exception e) {
                        System.out.println("MethodReplacePlugin error: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private List<String> getAllClassFiles(Project project, String variantName) {
        String basePath = project.getBuildDir().getPath();
        List<String> classFilePaths = new ArrayList<>();

        System.out.println("fxa getAllClassFiles project->"+project+" "+variantName);
        // Scan Java classes (include the task name in the path)
        String javaTaskName = getJavaTaskName(variantName);
        File javaClassesDir = new File(basePath + "/intermediates/javac/" + variantName + "/" + javaTaskName + "/classes/");
        System.out.println("Scanning Java classes directory: " + javaClassesDir.getAbsolutePath());
        findClassFiles(javaClassesDir, "", classFilePaths);

        // Scan Kotlin classes
        File kotlinClassesDir = new File(basePath + "/tmp/kotlin-classes/" + variantName + "/");
        System.out.println("Scanning Kotlin classes directory: " + kotlinClassesDir.getAbsolutePath());
        findClassFiles(kotlinClassesDir, "", classFilePaths);

        // Scan dependency classes (e.g., from the plugin JAR)
        File intermediatesDir = new File(basePath + "/intermediates/compile_library_classes_jar/" + variantName + "/");
        if (intermediatesDir.exists()) {
            File[] jarFiles = intermediatesDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null) {
                for (File jarFile : jarFiles) {
                    System.out.println("Scanning dependency JAR: " + jarFile.getAbsolutePath());
                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                        jar.entries().asIterator().forEachRemaining(entry -> {
                            if (entry.getName().endsWith(".class")) {
                                classFilePaths.add("jar:" + jarFile.getAbsolutePath() + "!" + entry.getName());
                            }
                        });
                    } catch (IOException e) {
                        System.out.println("Error scanning JAR " + jarFile + ": " + e.getMessage());
                    }
                }
            }
        }

        return classFilePaths;
    }

    private void findClassFiles(File dir, String relativePath, List<String> classFilePaths) {
        if (!dir.exists()) {
            System.out.println("Directory does not exist: " + dir.getAbsolutePath());
            return;
        }

        File currentDir = relativePath.isEmpty() ? dir : new File(dir, relativePath);
        if (!currentDir.exists()) {
            System.out.println("Subdirectory does not exist: " + currentDir.getAbsolutePath());
            return;
        }

        File[] files = currentDir.listFiles();
        if (files == null) {
            System.out.println("No files found in: " + currentDir.getAbsolutePath());
            return;
        }

        for (File file : files) {
            String newRelativePath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
            if (file.isDirectory()) {
                findClassFiles(dir, newRelativePath, classFilePaths);
            } else if (file.getName().endsWith(".class")) {
                System.out.println("fxa scanning class:"+file.getName());
                classFilePaths.add(file.getAbsolutePath());
            }
        }
    }

//    private void applyReplacements(Path classPath) throws IOException {
//        // Skip JAR entries in the second pass; only apply replacements to compiled classes
////        if (classPath.toString().startsWith("jar:")) {
////            System.out.println("Skipping replacement for JAR entry: " + classPath);
////            return;
////        }
//        byte[] bytes = Files.readAllBytes(classPath);
//        ClassReader cr = new ClassReader(bytes);
//        String internalName = cr.getClassName();
//
//        // ← 在这里也加同样的跳过逻辑
//        if ("com/chaoxing/transformer/MethodReplaceUtil".equals(internalName)) {
//            System.out.println("fxa Skipping replacements for util class: " + internalName);
//            return;
//        }
//        // Remove COMPUTE_FRAMES to avoid frame computation issues
//        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//
//        // Add debugging to dump the bytecode before transformation
//        System.out.println("Dumping bytecode before transformation for: " + classPath);
//        TraceClassVisitor tracer = new TraceClassVisitor(null, new PrintWriter(System.out));
//        cr.accept(tracer, 0);
//
//
//        for (String key : methodReplaceMap.keySet()) {
//            System.out.println("fxa Replacement map2: " + key + " -> " + methodReplaceMap.get(key));
//        }
//
//        MethodReplaceTransformer transformer = new MethodReplaceTransformer(cw, methodReplaceMap);
//        cr.accept(transformer, ClassReader.SKIP_FRAMES);
//
//        byte[] newBytes = cw.toByteArray();
//
//        // Dump the bytecode after transformation
//        System.out.println("Dumping bytecode after transformation for: " + classPath);
//        ClassReader crAfter = new ClassReader(newBytes);
//        crAfter.accept(new TraceClassVisitor(null, new PrintWriter(System.out)), 0);
//
//        Files.write(classPath, newBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//        System.out.println("Processed class with replacements: " + classPath);
//    }

private void applyReplacements(Path classPath) throws IOException {
    byte[] bytes;
    if (classPath.toString().startsWith("jar:")) {
        String[] parts = classPath.toString().split("!");
        String jarPath = parts[0].substring(4); // Remove "jar:"
        String entryPath = parts[1];
        try (JarFile jar = new JarFile(jarPath)) {
            JarEntry entry = jar.getJarEntry(entryPath);
            try (InputStream is = jar.getInputStream(entry)) {
                bytes = is.readAllBytes();
            }
        }
    } else {
        bytes = Files.readAllBytes(classPath);
    }

    ClassReader cr = new ClassReader(bytes);
    String internalName = cr.getClassName();

    if ("com/chaoxing/transformer/MethodReplaceUtil".equals(internalName)) {
        System.out.println("fxa Skipping replacements for util class: " + internalName);
        return;
    }

    // 使用 COMPUTE_FRAMES 自动计算栈映射表
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    // 调试：打印字节码转换前的内容
    System.out.println("Dumping bytecode before transformation for: " + classPath);
    TraceClassVisitor tracer = new TraceClassVisitor(null, new PrintWriter(System.out));
    cr.accept(tracer, 0);

    // 打印 methodReplaceMap 内容
    for (String key : methodReplaceMap.keySet()) {
        System.out.println("fxa Replacement map2: " + key + " -> " + methodReplaceMap.get(key));
    }

    MethodReplaceTransformer transformer = new MethodReplaceTransformer(cw, methodReplaceMap);
    cr.accept(transformer, 0); // 移除 ClassReader.SKIP_FRAMES

    byte[] newBytes = cw.toByteArray();

    // 调试：打印字节码转换后的内容
    System.out.println("Dumping bytecode after transformation for: " + classPath);
    ClassReader crAfter = new ClassReader(newBytes);
    crAfter.accept(new TraceClassVisitor(null, new PrintWriter(System.out)), 0);

    // 验证生成的字节码 todo 这里读取不到ANDROID SDK
//    System.out.println("Validating modified bytecode for: " + classPath);
//    CheckClassAdapter.verify(crAfter, true, new PrintWriter(System.out));

    Files.write(classPath, newBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    System.out.println("Processed class with replacements: " + classPath);
}

    private String getJavaTaskName(String capitalizedVariantName) {
        return "compile" + capitalizedVariantName + "JavaWithJavac";
    }

    private String getKotlinTaskName(String capitalizedVariantName) {
        return "compile" + capitalizedVariantName + "Kotlin";
    }
}