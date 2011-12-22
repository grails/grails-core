package org.codehaus.groovy.grails.support.internal.tools;

import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistryChangeEvent;
import groovy.lang.MetaClassRegistryChangeEventListener;

/**
 * Simple class that reports when meta class changes and where (in what stack frame) those changes took place
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class MetaClassChangeReporter implements MetaClassRegistryChangeEventListener{
    /**
     * Called when the a constant MetaClass is updated. If the new MetaClass is null, then the MetaClass
     * is removed. Be careful, while this method is executed other updates may happen. If you want this
     * method thread safe, you have to take care of that by yourself.
     *
     * @param cmcu - the change event
     */
    public void updateConstantMetaClass(MetaClassRegistryChangeEvent cmcu) {
        Class<?> classToUpdate = cmcu.getClassToUpdate();
        MetaClass newMetaClass = cmcu.getNewMetaClass();

        System.out.println("Class ["+classToUpdate+"] updated MetaClass to ["+newMetaClass+"]");
        Thread.dumpStack();
    }
}
