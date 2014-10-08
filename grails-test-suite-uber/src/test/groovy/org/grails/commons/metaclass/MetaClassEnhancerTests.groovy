package org.grails.commons.metaclass

import org.grails.core.metaclass.MetaClassEnhancer

class MetaClassEnhancerTests extends GroovyTestCase {

    void testEnhanceMetaClass() {
        def enhancer = new MetaClassEnhancer()
        enhancer.addApi new SomeApi()

        enhancer.enhance ClassToEnhance.metaClass
        def instance = new ClassToEnhance()

        assert 42 == instance.magicNumber
    }
}

class ClassToEnhance {
}

class SomeApi {
    int getMagicNumber(Object instance) {
        42
    }
}
