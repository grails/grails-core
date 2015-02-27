package org.grails.io.support

import grails.util.BuildSettings
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.ClassReader
import groovyjarjarasm.asm.ClassVisitor
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class MainClassFinder {

    private static final Type STRING_ARRAY_TYPE = Type.getType(String[].class)

    private static final Type MAIN_METHOD_TYPE = Type.getMethodType(Type.VOID_TYPE, STRING_ARRAY_TYPE)

    private static final String MAIN_METHOD_NAME = "main"

    static String mainClassName = null

    static String searchMainClass() {
        if(mainClassName) return mainClassName

        def classesDir = BuildSettings.CLASSES_DIR
        Collection<File> searchDirs
        if (classesDir == null) {
            def tokens = System.getProperty('java.class.path').split(System.getProperty('path.separator'))
            def dirs = tokens.findAll() { String str -> !str.endsWith('.jar') }.collect() { String str -> new File(str) }
            searchDirs = dirs.findAll() { File f -> f.isDirectory() }
        } else {
            searchDirs = [classesDir]
        }
        def userDir = new File(System.getProperty('user.dir'))
        if(new File(userDir, 'settings.gradle').exists()) {
            userDir.eachDir { File dir ->
                // looking for subproject build directories...
                def mainClassesDir = new File(dir, 'build/classes/main')
                if(mainClassesDir.exists()) {
                    searchDirs << mainClassesDir
                }
            }
        }

        String mainClass = null

        for (File dir in searchDirs) {
            mainClass = findMainClass(dir)
            if (mainClass) break
        }
        mainClass
    }

    static String findMainClass(File rootFolder = BuildSettings.CLASSES_DIR) {
        if(mainClassName) return mainClassName

        if (!rootFolder.exists()) {
            return null // nothing to do
        }
        if (!rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid root folder '$rootFolder'")
        }
        String prefix =  "$rootFolder.absolutePath/"
        def stack = new ArrayDeque<File>()
        stack.push rootFolder

        while (!stack.empty) {
            File file = stack.pop()
            if (file.isFile()) {
                InputStream inputStream = file.newInputStream()
                try {
                    def classReader = new ClassReader(inputStream)

                    if (isMainClass(classReader)) {
                        mainClassName = classReader.getClassName().replace('/', '.').replace('\\', '.')
                        return mainClassName
                    }
                } finally {
                    inputStream?.close()
                }
            }
            if (file.isDirectory()) {
                def files = file.listFiles()?.findAll { File f ->
                    (f.isDirectory() && !f.name.startsWith('.') && !f.hidden) ||
                            (f.isFile() && f.name.endsWith(GrailsResourceUtils.CLASS_EXTENSION))
                }

                if(files) {
                    for(File sub in files) {
                        stack.push(sub)
                    }
                }

            }
        }
        return null
    }


    protected static boolean isMainClass(ClassReader classReader) {
        if(classReader.superName?.startsWith('grails/boot/config/')) {
            def mainMethodFinder = new MainMethodFinder()
            classReader.accept(mainMethodFinder, ClassReader.SKIP_CODE)
            return mainMethodFinder.found
        }
        return false
    }

    @CompileStatic
    static class MainMethodFinder extends ClassVisitor  {

        boolean found = false

        MainMethodFinder() {
            super(Opcodes.ASM4)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if(!found) {
                if (isAccess(access, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC)
                        && MAIN_METHOD_NAME.equals(name)
                        && MAIN_METHOD_TYPE.getDescriptor().equals(desc)) {


                    this.found = true
                }
            }
            return null
        }


        private boolean isAccess(int access, int... requiredOpsCodes) {
            return !requiredOpsCodes.any { int requiredOpsCode -> (access & requiredOpsCode) == 0 }
        }
    }

}
