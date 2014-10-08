package grails.dev

import grails.util.Environment
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.springframework.util.ClassUtils

import java.lang.management.ManagementFactory

/**
 * Created by graemerocher on 03/10/14.
 */
@CompileStatic
@Commons
class Support {

    /**
     * Enables the reloading agent at runtime if it isn't present
     */
    static void enableAgentIfNotPresent() {
        def environment = Environment.current
        if(environment.isReloadEnabled() && !ClassUtils.isPresent("org.springsource.loaded.SpringLoaded", System.classLoader)) {
            def grailsHome = System.getenv(Environment.ENV_GRAILS_HOME)

            if(grailsHome) {
                def file = new File(grailsHome, "lib/org.springframework/springloaded/jars/springloaded-1.2.1.RELEASE.jar")
                if(file.exists()) {
                    def runtimeMxBean = ManagementFactory.runtimeMXBean
                    def arguments = runtimeMxBean.inputArguments
                    if(!arguments.contains('-Xverify:none') && !arguments.contains('-noverify')) {
                        log.warn("Reloading is disabled. Development time reloading requires disabling the Java verifier. Please pass the argument '-Xverify:none' to the JVM")
                    }
                    else {
                        def vmName = runtimeMxBean.name
                        int i = vmName.indexOf('@')
                        String pid = vmName.subSequence(0, i)
                        if(ClassUtils.isPresent('com.sun.tools.attach.VirtualMachine', System.classLoader)) {
                            def vmClass = Support.classLoader.loadClass('com.sun.tools.attach.VirtualMachine')
                            attachAgentClassToProcess(vmClass, pid, file)
                        }

                    }
                }
            }
        }
    }

    @CompileDynamic
    private static void attachAgentClassToProcess(Class<?> vmClass, String pid, File file) {
        try {
            def vm = vmClass.attach(pid)
            vm.loadAgent(file.absolutePath, "")
            vm.detach()
        } catch (e) {
            System.err.println("WARNING: Could not attach reloading agent. Reloading disabled. Message: $e.message")
        }
    }
}
