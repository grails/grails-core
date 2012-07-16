/*
 * Copyright 2012 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.cli.fork

import groovy.transform.CompileStatic
import grails.build.logging.GrailsConsole
import org.apache.commons.logging.Log
import gant.Gant
import grails.util.BuildSettings

/**
 * Helper class for kicking off forked JVM processes, helpful in managing the setup and
 * execution of the forked process. Subclasses should provided a static void main method.
 *
 * @author Graeme Rocher
 * @since 2.2
 */
@CompileStatic
abstract class ForkedGrailsProcess {

    int maxMemory = 1024;
    int minMemory = 64;
    int maxPerm = 256;
    boolean debug = false;
    File reloadingAgent;

    Process fork() {
        ExecutionContext executionContext = createExecutionContext()
        def processBuilder = new ProcessBuilder()
        def cp = new StringBuilder()
        for (File file : executionContext.getBuildDependencies()) {
            cp << file << File.pathSeparator
        }

        def baseName = executionContext.getBaseDir().canonicalFile.name
        File tempFile = File.createTempFile(baseName, "grails-execution-context")

        tempFile.deleteOnExit()

        tempFile.withOutputStream { OutputStream fos ->
            def oos = new ObjectOutputStream(fos)
            oos.writeObject(executionContext)
        }

        List<String> cmd = ["java", "-Xmx${maxMemory}M".toString(), "-Xms${minMemory}M".toString(), "-XX:MaxPermSize=${maxPerm}m".toString(),"-Dgrails.fork.active=true", "-Dgrails.build.execution.context=${tempFile.canonicalPath}".toString(), "-cp", cp.toString()]
        if(debug) {
            cmd.addAll( ["-Xdebug","-Xnoagent","-Dgrails.full.stacktrace=true", "-Djava.compiler=NONE", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"] )
        }
        final console = GrailsConsole.getInstance()
        if(console.isVerbose()) {
            cmd.add("-Dgrails.verbose=true")
            cmd.add("-Dgrails.full.stacktrace=true")
        }
        if(console.isStacktrace()) {
            cmd.add("-Dgrails.show.stacktrace=true")
        }
        if(reloadingAgent != null) {
            cmd.addAll(["-javaagent:" + reloadingAgent.getCanonicalPath(), "-noverify", "-Dspringloaded=profile=grails"])
        }
        cmd << getClass().name

        processBuilder
                .directory(executionContext.getBaseDir())
                .redirectErrorStream(false)
                .command(cmd)

        def process = processBuilder.start()

        def is = process.inputStream
        def es = process.errorStream
        def t1 = new Thread(new TextDumper(is, System.out))
        def t2 = new Thread(new TextDumper(es, System.err))
        t1.start()
        t2.start()

        int result = process.waitFor()
        if (result == 1) {
            try { t1.join() } catch (InterruptedException ignore) {}
            try { t1.join() } catch (InterruptedException ignore) {}
            try { es.close() } catch (IOException ignore) {}
            try { is.close() } catch (IOException ignore) {}

            throw new RuntimeException("Forked Grails VM exited with error")
        }
        return process
    }

    abstract ExecutionContext createExecutionContext()

    ExecutionContext readExecutionContext() {
        String location = System.getProperty("grails.build.execution.context");

        if (location != null) {
            final file = new File(location)
            if (file.exists()) {
                return (ExecutionContext)file.withInputStream { InputStream fis ->
                    def ois = new ObjectInputStream(fis)
                    return (ExecutionContext)ois.readObject()
                }
            }
        }
        return null
    }

    @CompileStatic
    public static List<File> buildMinimalIsolatedClasspath(BuildSettings buildSettings) {
        List<File> buildDependencies = []


        buildDependencies.add findJarFile(GroovySystem)
        buildDependencies.add findJarFile(Log)
        buildDependencies.add findJarFile(Gant)

        List<File> bootstrapJars = []
        for(File f in buildSettings.runtimeDependencies) {
            final fileName = f.name
            if( fileName.contains('log4j') ) {
                bootstrapJars.add(f)
            }
        }
        for(File f in buildSettings.buildDependencies) {
            final fileName = f.name
            if( fileName.contains('grails-bootstrap') ||
                    fileName.contains('slf4j-api') ||
                    fileName.contains('ivy') ||
                    fileName.contains('ant') ||
                    fileName.contains('jline') ||
                    fileName.contains('jansi') ) {
                bootstrapJars.add(f)
            }
        }

        buildDependencies.addAll  bootstrapJars
        buildDependencies
    }

    @CompileStatic
    private static File findJarFile(Class targetClass) {
        def absolutePath = targetClass.getResource('/' + targetClass.name.replace(".", "/") + ".class").getPath()
        final jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"))
        final jarFile = new File(jarPath)
        jarFile
    }

    static class TextDumper implements Runnable {
        InputStream input
        Appendable app

        TextDumper(InputStream input, Appendable app) {
            this.input = input
            this.app = app
        }

        void run() {
            def isr = new InputStreamReader(input)
            def br = new BufferedReader(isr)
            br.eachLine { String next ->
                if(next)
                    app.append(next)
            }
        }
    }
}

@CompileStatic
class ExecutionContext {
    List<File> runtimeDependencies
    List<File> buildDependencies
    List<File> providedDependencies

    File grailsWorkDir
    File projectWorkDir
    File classesDir
    File testClassesDir
    File resourcesDir
    File projectPluginsDir
    File baseDir

    String env
}
