package org.codehaus.groovy.grails.plugins;

// Used in CoreGrailsPluginTests#testAopConfigurationIsEffective
// Must be Java because Groovy classes are always proxy by class
class CoreGrailsPluginTestsAopConfig {
    static public class MyAspect {
        public void myBeforeAdvice() {}
    }
    
    static public interface MyInterface { 
        void myMethod1(); 
    }
    
    static public class MyInterfaceImpl implements MyInterface {
        public void myMethod1() {}
        public void myMethod2() {}
    }
}