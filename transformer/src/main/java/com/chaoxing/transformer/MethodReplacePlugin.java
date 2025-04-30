package com.chaoxing.transformer;

import com.chaoxing.transformer.replace.MethodReplaceAnnotationProcessor;
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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodReplacePlugin implements Plugin<Project> {
    private final Map<String, MethodReplaceInfo> methodReplaceMap = new HashMap<>();

    @Override
    public void apply(Project project) {
        System.out.println("MethodReplacePlugin start");

        project.afterEvaluate(proj -> {
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

                        // First Pass: Collect @MethodReplace annotations
                        for (String classFilePath : classFiles) {
                            System.out.println("First pass - collecting annotations from: " + classFilePath);
                            collectAnnotations(Path.of(classFilePath));
                        }

                        // Debug: Log the methodReplaceMap contents
                        System.out.println("methodReplaceMap after first pass: " + methodReplaceMap);

                        Thread.sleep(3000);
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
                classFilePaths.add(file.getAbsolutePath());
            }
        }
    }

    private void collectAnnotations(Path classPath) throws IOException {
        byte[] bytes;
        if (classPath.toString().startsWith("jar:")) {
            String[] parts = classPath.toString().split("!");
            String jarPath = parts[0].substring(4); // Remove "jar:"
            String entryPath = parts[1];
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
                java.util.jar.JarEntry entry = jar.getJarEntry(entryPath);
                try (java.io.InputStream is = jar.getInputStream(entry)) {
                    bytes = is.readAllBytes();
                }
            }
        } else {
            bytes = Files.readAllBytes(classPath);
        }
        ClassReader cr = new ClassReader(bytes);
//        String internalName = cr.getClassName();
        // ← 在这里加一行，跳过 util 类
//        if ("com/chaoxing/transformer/MethodReplaceUtil".equals(internalName)) {
//            System.out.println("fxa Skipping annotation collection for " + internalName);
//            return;
//        }
        ClassWriter cw = new ClassWriter(0);
        MethodReplaceAnnotationProcessor processor = new MethodReplaceAnnotationProcessor(cw, methodReplaceMap);
        cr.accept(processor, ClassReader.EXPAND_FRAMES);
    }

    private void applyReplacements(Path classPath) throws IOException {
        // Skip JAR entries in the second pass; only apply replacements to compiled classes
        if (classPath.toString().startsWith("jar:")) {
            System.out.println("Skipping replacement for JAR entry: " + classPath);
            return;
        }

        byte[] bytes = Files.readAllBytes(classPath);

        // Validate input bytecode
        System.out.println("Validating input bytecode for: " + classPath);
        try {
            ClassReader crValidate = new ClassReader(bytes);
            CheckClassAdapter.verify(crValidate, true, new PrintWriter(System.out));
        } catch (Exception e) {
            System.out.println("Invalid input bytecode for: " + classPath);
            e.printStackTrace();
            return;
        }

        ClassReader cr = new ClassReader(bytes);
        String internalName = cr.getClassName();

        // ← 在这里也加同样的跳过逻辑
        if ("com/chaoxing/transformer/MethodReplaceUtil".equals(internalName)) {
            System.out.println("fxa Skipping replacements for util class: " + internalName);
            return;
        }
        // Remove COMPUTE_FRAMES to avoid frame computation issues
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        // Add debugging to dump the bytecode before transformation
        System.out.println("Dumping bytecode before transformation for: " + classPath);
        TraceClassVisitor tracer = new TraceClassVisitor(null, new PrintWriter(System.out));
        cr.accept(tracer, 0);

        MethodReplaceTransformer transformer = new MethodReplaceTransformer(cw, methodReplaceMap);
        cr.accept(transformer, ClassReader.SKIP_FRAMES);

        byte[] newBytes = cw.toByteArray();

        // Validate the generated bytecode
        System.out.println("Validating modified bytecode for: " + classPath);
        ClassReader crValidate = new ClassReader(newBytes);
        CheckClassAdapter.verify(crValidate, true, new PrintWriter(System.out));

        // Dump the bytecode after transformation
        System.out.println("Dumping bytecode after transformation for: " + classPath);
        ClassReader crAfter = new ClassReader(newBytes);
        crAfter.accept(new TraceClassVisitor(null, new PrintWriter(System.out)), 0);

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