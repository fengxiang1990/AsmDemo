package com.fxa.transformer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.LibraryVariant;
import com.fxa.transformer.timercost.MethodTimerTransformer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MethodTimerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("MethodTimerPlugin start");
        project.getExtensions().create("methodTimerConfig", MethodTimerExtension.class);

        project.afterEvaluate(proj -> {
            System.out.println("MethodTimerPlugin proj->"+proj);
            AppExtension android = proj.getExtensions().findByType(AppExtension.class);
            LibraryExtension library = proj.getExtensions().findByType(LibraryExtension.class);
            if (android != null) {
                android.getApplicationVariants().all(variant -> {
                    configureTaskForApplicationVariant(project, variant);
                });
            }
            if (library != null) {
                library.getLibraryVariants().all(variant -> {
                    configureTaskForLibraryVariant(project,variant);
                });
            }
        });

        System.out.println("MethodTimerPlugin end");
    }

    private void configureTaskForApplicationVariant(Project project, ApplicationVariant variant) {
        String variantName = variant.getName();
        configureTaskForVariant(project,variantName);
    }
    private void configureTaskForLibraryVariant(Project project, LibraryVariant variant) {
        String variantName = variant.getName();
        configureTaskForVariant(project,variantName);
    }


    private void configureTaskForVariant(Project project, String variantName) {
        String capitalizedVariantName = variantName.substring(0, 1).toUpperCase() + variantName.substring(1);

        project.getTasks().configureEach(task -> {
            if (task.getName().equals(getKotlinTaskName(capitalizedVariantName)) ||
                task.getName().equals(getJavaTaskName( capitalizedVariantName))) {
                System.out.println("MethodTimerPlugin task in :" + task.getName());
                task.doLast(t -> {
                    System.out.println("MethodTimerPlugin task->" + t.getName());
                    MethodTimerExtension extension = project.getExtensions().findByType(MethodTimerExtension.class);
                    if (extension != null) {
                        //对指定包名下的全部类修改
                        List<String> packagesToTransform = extension.getPackagesToTransform();
                        if(packagesToTransform != null){
                            for (String packageName : packagesToTransform) {
                                List<String> classFiles = getClassFilesForPackage(project, variantName, task.getName(), packageName);
                                for (String classFilePath : classFiles) {
                                    try {
                                        MethodTimerTransformer.transform(classFilePath);
                                    } catch (Exception e) {
                                        System.out.println("MethodTimerPlugin package err->" + classFilePath + " e:" + e.getMessage());
                                    }
                                }
                            }
                        }

                        //最指定的类修改
                        List<String> classesToTransform = extension.getClassesToTransform();
                        if(classesToTransform != null){
                            for (String className : classesToTransform) {
                                String classFilePath = getClassFilePath(project, variantName, task.getName(), className);
                                try {
                                    MethodTimerTransformer.transform(classFilePath);
                                } catch (Exception e) {
                                    System.out.println("MethodTimerPlugin err->" + className + " e:" + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    private List<String> getClassFilesForPackage(Project project, String variantName, String taskName, String packageName) {
        String basePath = project.getBuildDir().getPath();
        List<String> classFilePaths = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');
        String java = getJavaTaskName(variantName);
        if (taskName.toLowerCase().contains(java.toLowerCase())) {
            File javaClassesDir = new File(basePath + "/intermediates/javac/" + variantName +"/"+java+"/"+ "/classes/");
            findClassFiles(javaClassesDir, packagePath, classFilePaths);
        }

        if (taskName.toLowerCase().contains(getKotlinTaskName(variantName).toLowerCase())) {
            File kotlinClassesDir = new File(basePath + "/tmp/kotlin-classes/" + variantName+"/");
            findClassFiles(kotlinClassesDir, packagePath, classFilePaths);
        }
        return classFilePaths;
    }

    private void findClassFiles(File dir, String packagePath, List<String> classFilePaths) {
        if (dir.exists()) {
            File packageDir = new File(dir, packagePath);
            if (packageDir.exists() && packageDir.isDirectory()) {
                File[] files = packageDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if(file.isDirectory()){
                            System.out.println("findClassFiles packageDir2:"+(packagePath+"/"+file.getName()));
                            findClassFiles(dir,packagePath+"/"+file.getName(),classFilePaths);
                        }else{
                            if(file.getName().endsWith(".class")){
                                classFilePaths.add(file.getAbsolutePath());
                            }
                        }
                    }
                }else{
                    System.out.println("findClassFiles files null");
                }
            }
        }
    }


    private String getJavaTaskName(String capitalizedVariantName){
        return "compile" + capitalizedVariantName + "JavaWithJavac";
    }

    private String getKotlinTaskName(String capitalizedVariantName){
        return "compile" + capitalizedVariantName + "Kotlin";
    }

    private String getClassFilePath(Project project, String variantName, String taskName, String className) {
        String basePath = project.getBuildDir().getPath();
        String classFilePath = null;
        String java = getJavaTaskName(variantName);
        String kotlin  = getKotlinTaskName(variantName);
        System.out.println("MethodTimerPlugin getClassFilePath->"+"basePath:"+basePath+" variantName:"+variantName+" java:"+java+" kotlin:"+kotlin+ " taskName:"+taskName);
        if (taskName.toLowerCase().contains(java.toLowerCase())) {
            System.out.println("MethodTimerPlugin java in ");
            classFilePath = basePath + "/intermediates/javac/" + variantName +"/"+java+"/"+ "/classes/" + className.replace('.', '/') + ".class";
        }
        if (taskName.toLowerCase().contains(kotlin.toLowerCase())) {
            System.out.println("MethodTimerPlugin kotlin in");
            classFilePath = basePath + "/tmp/kotlin-classes/" + variantName + "/" + className.replace('.', '/') + ".class";
        }

        return classFilePath;
    }
}

class MethodTimerExtension {

    private Project project;

    private List<String> packagesToTransform;
    private List<String> classesToTransform;

    public List<String> getPackagesToTransform() {
        return packagesToTransform;
    }

    public void setPackagesToTransform(List<String> packagesToTransform) {
        this.packagesToTransform = packagesToTransform;
    }

    public MethodTimerExtension(Project project) {
        this.project = project;
    }

    public List<String> getClassesToTransform() {
        return classesToTransform;
    }

    public void setClassesToTransform(List<String> classesToTransform) {
        this.classesToTransform = classesToTransform;
    }
}
