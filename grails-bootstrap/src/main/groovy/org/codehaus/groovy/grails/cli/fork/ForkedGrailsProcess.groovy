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
    int minMemory = 512;
    int maxPerm = 256;
    boolean debug;
    File reloadingAgent;

    void fork() {
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

        List<String> cmd = ["java", "-Xmx${maxMemory}M".toString(), "-Xms${minMemory}M".toString(), "-XX:MaxPermSize=${maxPerm}m".toString(), "-Dgrails.build.execution.context=${tempFile.canonicalPath}".toString(), "-cp", cp.toString()]
        if (debug) {
            cmd.addAll( ["-Xdebug","-Xnoagent","-Dgrails.full.stacktrace=true", "-Djava.compiler=NONE", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"] )
        }
        if (reloadingAgent != null) {
            cmd.addAll(["-javaagent:" + reloadingAgent.getCanonicalPath(), "-noverify", "-Dspringloaded=profile=grails"])
        }
        cmd << getClass().name

//        println "Forking Grails ${cmd.join(' ')}"
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
                if (app != null) {
                    app.append(next).append( '\n' )
                }
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
