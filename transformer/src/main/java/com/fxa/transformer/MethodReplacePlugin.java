package com.fxa.transformer;

import com.fxa.transformer.replace.FindReplaceRuleUtil;
import com.fxa.transformer.replace.MethodReplaceRuleProcessor;
import com.fxa.transformer.replace.MethodReplaceTransformer;
import com.fxa.transformer.replace.MethodReplaceInfo;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.LibraryVariant;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class MethodReplacePlugin implements Plugin<Project> {

    public static final String Jar_modify_dir = "libs/modified";

    /** 是否覆盖原始 JAR 文件，默认 false */
    public static final boolean Jar_overwriteOriginal = true;

    private  Map<String, MethodReplaceInfo> methodReplaceMap = null;

    @Override
    public void apply(Project project) {
        System.out.println("MethodReplacePlugin start thread->"+Thread.currentThread().getName()+" "+Thread.currentThread().getId());
        this.methodReplaceMap = FindReplaceRuleUtil.getInstance().getMethodReplaceMap();
        this.methodReplaceMap.clear();
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

            processAllJarFiles(proj);

        });

        System.out.println("MethodReplacePlugin end");
    }

    private void processAllJarFiles(Project project) {
        // JAR 文件所在的 libs 目录
        File libsDir = new File(project.getProjectDir(), "libs");
        if (!libsDir.exists() || !libsDir.isDirectory()) {
            return; // 如果没有 libs 目录则跳过
        }

        // 确定输出目录（默认 libs/modified）
        File outputDir = new File(project.getProjectDir(), MethodReplacePlugin.Jar_modify_dir);
        if (!MethodReplacePlugin.Jar_overwriteOriginal && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 列出所有 .jar 文件
        File[] jarFiles = libsDir.listFiles(file ->
            file.isFile() && file.getName().endsWith(".jar")
        );
        if (jarFiles == null) {
            return;
        }

        // 对每个 JAR 文件进行处理
        for (File jar : jarFiles) {
            try {
                processJarFile(project,jar, MethodReplacePlugin.Jar_overwriteOriginal, outputDir, this.methodReplaceMap);
            } catch (IOException e) {
                throw new RuntimeException("处理 JAR 文件失败: " + jar, e);
            }
        }
    }

    private void processJarFile(Project project,File jarFile, boolean overwrite, File outputDir,
                                Map<String, MethodReplaceInfo> methodReplaceMap) throws IOException {
        // Step 1: 在项目根目录下准备备份目录
        File projectRoot = project.getRootProject().getProjectDir(); // 在 Gradle 插件中获取项目根目录
        File backupDir = new File(projectRoot, "jar_backup");
        if (!backupDir.exists()) {
            boolean created = backupDir.mkdirs();
            if (!created) {
                System.err.println("无法创建备份目录: " + backupDir.getAbsolutePath());
            }
        }

        // Step 2: 检查同名文件是否已存在，决定是否需要复制
        File backupJar = new File(backupDir, jarFile.getName());
        if (backupJar.exists()) {
            // 如果备份目录中已有同名文件，则跳过备份
            System.err.println("备份已存在，跳过复制：" + backupJar.getName());
        } else {
            // 备份目录没有同名文件，执行复制
            try {
                Files.copy(jarFile.toPath(), backupJar.toPath());
                System.err.println("已备份 JAR 文件到：" + backupJar.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("备份 JAR 文件失败：" + e.getMessage());
            }
        }

        // 打开 JAR 文件进行读取
        JarFile jar = new JarFile(jarFile);
        File tempJar;
        try {
            // 准备输出 JAR 路径（若覆盖，则先输出到临时文件再替换）
            if (overwrite) {
                tempJar = new File(jarFile.getParent(), jarFile.getName() + ".tmp");
            } else {
                tempJar = new File(outputDir, jarFile.getName());
            }

            // 准备输出流：如果源 JAR 含 Manifest，则传入原 Manifest 保持不变
            Manifest manifest = jar.getManifest();
            try (JarOutputStream jos = (manifest != null)
                ? new JarOutputStream(new FileOutputStream(tempJar), manifest)
                : new JarOutputStream(new FileOutputStream(tempJar))) {

                // 遍历 JAR 中的所有条目
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    // 跳过已由 JarOutputStream 处理的 Manifest（如果传入了 manifest）
                    if (manifest != null && "META-INF/MANIFEST.MF".equalsIgnoreCase(name)) {
                        continue;
                    }

                    // 保证路径格式：使用正斜杠，且不要以 '/' 开头:contentReference[oaicite:5]{index=5}:contentReference[oaicite:6]{index=6}
                    name = name.replace("\\", "/");
                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }

                    // 如果是目录条目，则确保以 '/' 结尾并写入空目录
                    if (entry.isDirectory()) {
                        if (!name.endsWith("/")) {
                            name += "/";
                        }
                        JarEntry dirEntry = new JarEntry(name);
                        dirEntry.setTime(entry.getTime());
                        jos.putNextEntry(dirEntry);
                        jos.closeEntry();
                        continue;
                    }

                    // 对非目录文件进行处理
                    try (InputStream is = jar.getInputStream(entry)) {
                        if (name.endsWith(".class")) {
                            // 1) 使用 ASM 对 .class 文件进行修改
                            byte[] origClassBytes = is.readAllBytes();
                            try {
                                byte[] newClassBytes = transformClass(origClassBytes, methodReplaceMap);
                                // 2) 写入修改后的字节码
                                JarEntry newEntry = new JarEntry(name);
                                newEntry.setTime(entry.getTime());
                                jos.putNextEntry(newEntry);
                                jos.write(newClassBytes);
                                jos.closeEntry();
                            }catch (Exception e){
                                e.printStackTrace();
                                //delete tmp
                                if(tempJar != null){
                                    tempJar.delete();
                                }
                            }
                        } else {
                            // 3) 复制其它资源文件（例如图片、配置等）
                            JarEntry resourceEntry = new JarEntry(name);
                            resourceEntry.setTime(entry.getTime());
                            jos.putNextEntry(resourceEntry);
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = is.read(buffer)) != -1) {
                                jos.write(buffer, 0, len);
                            }
                            jos.closeEntry();
                        }
                    }
                }
            }
        }finally {
            jar.close();
        }
        // 如果覆盖原文件，将临时文件重命名回原名
        if (overwrite) {
            File original = jarFile;
            // 如果原文件还在运行中或被占用，删除会失败
            if (!original.delete()) {
                System.err.println("删除原文件失败，可能正被占用: " + original.getAbsolutePath());
            }
            if (!tempJar.renameTo(original)) {
                throw new IOException("无法重命名临时 JAR: " + tempJar.getAbsolutePath() + " -> " + original.getAbsolutePath());
            }
        }
    }

    private byte[] transformClass(byte[] classBytes, Map<String, MethodReplaceInfo> methodReplaceMap) {
        try {
            ClassReader classReader = new ClassReader(classBytes);
            // 使用 COMPUTE_FRAMES/COMPUTE_MAXS 标志自动计算栈帧和栈大小
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            // MethodReplaceTransformer 是自定义的 ASM ClassVisitor，用于替换方法（已实现具体逻辑）
            MethodReplaceTransformer transformer = new MethodReplaceTransformer(classWriter, methodReplaceMap);
            // 触发访问：EXPAND_FRAMES 让 ASM 转换所有 GOTO/WIDE 等指令为通用形式
            classReader.accept(transformer, ClassReader.EXPAND_FRAMES);
            return classWriter.toByteArray();
        }catch (Exception e){
           throw new IllegalArgumentException("fxa transformClass err",e);
        }
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

        //scan libs/jar
        File libsDir = new File(project.getProjectDir(), "libs");
        if (libsDir.exists()) {
            System.out.println("Scanning local libs directory: " + libsDir.getAbsolutePath());
            File[] libFiles = libsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (libFiles != null) {
                for (File jarFile : libFiles) {
                    System.out.println("Scanning local JAR: " + jarFile.getAbsolutePath());
                    try (JarFile jar = new JarFile(jarFile)) {
                        jar.entries().asIterator().forEachRemaining(entry -> {
                            if (entry.getName().endsWith(".class")) {
                                classFilePaths.add("jar:" + jarFile.getAbsolutePath() + "!" + entry.getName());
                            }
                        });
                    } catch (IOException e) {
                        System.out.println("Error scanning local JAR " + jarFile + ": " + e.getMessage());
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
    if (classPath.toString().startsWith("jar:")) {
        return;
    }
    byte[] bytes;
//    if (classPath.toString().startsWith("jar:")) {
//        String[] parts = classPath.toString().split("!");
//        String jarPath = parts[0].substring(4); // Remove "jar:"
//        String entryPath = parts[1];
//        try (JarFile jar = new JarFile(jarPath)) {
//            JarEntry entry = jar.getJarEntry(entryPath);
//            try (InputStream is = jar.getInputStream(entry)) {
//                bytes = is.readAllBytes();
//            }
//        }
//    } else {
//        bytes = Files.readAllBytes(classPath);
//    }

    bytes = Files.readAllBytes(classPath);

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