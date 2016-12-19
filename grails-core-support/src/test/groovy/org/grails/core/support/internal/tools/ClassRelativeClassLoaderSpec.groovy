package org.grails.core.support.internal.tools

import spock.lang.Specification

/**
 * Created by graemerocher on 30/09/2016.
 */
class ClassRelativeClassLoaderSpec extends Specification {

    void "test class relative classloader"() {
        when:"A classloader is created for only resources relative to this class"
        def classLoader = new ClassRelativeClassLoader(ClassRelativeClassLoaderSpec)

        then:"The resources are found"
        classLoader.getResource('org/grails/core/support/internal/tools/ClassRelativeClassLoaderSpec.class')

        and:"other classpath resources are not found"
        !classLoader.getResource('springloaded.properties')
    }
}
