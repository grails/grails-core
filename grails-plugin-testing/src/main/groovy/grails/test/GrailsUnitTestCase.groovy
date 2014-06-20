/*
 * Copyright 2008 the original author or authors.
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
package grails.test

import grails.core.DefaultArtefactInfo
import grails.core.DefaultGrailsApplication
import grails.util.Holders
import org.codehaus.groovy.grails.web.converters.marshaller.json.ValidationErrorsMarshaller as JsonErrorsMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.xml.ValidationErrorsMarshaller as XmlErrorsMarshaller

import grails.converters.JSON
import grails.converters.XML
import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import org.grails.core.lifecycle.ShutdownOperations
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.core.support.GrailsDomainConfigurationUtil
import org.grails.core.util.ClassPropertyFetcher
import org.springframework.validation.Errors
import org.codehaus.groovy.grails.commons.*
import grails.validation.ValidationErrors
import org.codehaus.groovy.grails.plugins.testing.GrailsMockErrors

/**
 * Support class for writing unit tests in Grails. It mainly provides
 * access to various mocking options, while making sure that the meta-
 * class magic does not leak outside of a single test.
 */
class GrailsUnitTestCase extends GroovyTestCase {

    Set loadedCodecs
    def applicationContext
    Map errorsMap
    private MetaClassRegistryCleaner registryCleaner = MetaClassRegistryCleaner.createAndRegister()

    /**
     * Keeps track of the domain classes mocked within a single test so
     * that the relationships can be configured for cascading validation.
     */
    DefaultArtefactInfo domainClassesInfo

    private previousConfig

    protected void setUp() {
        super.setUp()
        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener(registryCleaner)

        loadedCodecs = []
        applicationContext = new MockApplicationContext()
        domainClassesInfo = new DefaultArtefactInfo()
        errorsMap = new IdentityHashMap()

        // Register some common classes so that they can be converted to XML, JSON, etc.
        def convertersInit = new ConvertersConfigurationInitializer()
        convertersInit.initialize(new DefaultGrailsApplication())
        [ List, Set, Map, Errors ].each { addConverters(it) }
        def xmlErrorMarshaller = new XmlErrorsMarshaller()
        XML.registerObjectMarshaller(xmlErrorMarshaller)
        def jsonErrorMarshaller = new JsonErrorsMarshaller()
        JSON.registerObjectMarshaller(jsonErrorMarshaller)

        previousConfig = Holders.config

        Holders.pluginManager = [hasGrailsPlugin: { String name -> true }] as GrailsPluginManager
        registerMetaClass(ValidationErrors)
        ValidationErrors.metaClass.getAt = {String field ->
            def code = getFieldError(field)?.code
            return GrailsMockErrors.ERROR_CODE_TABLE[code] ?: code
        }
    }

    protected void tearDown() {
        super.tearDown()

        registryCleaner.clean()
        GroovySystem.metaClassRegistry.removeMetaClassRegistryChangeEventListener(registryCleaner)
        ShutdownOperations.runOperations()
        MockUtils.TEST_INSTANCES.clear()
        Holders.config = previousConfig
        MockUtils.resetIds()
        ClassPropertyFetcher.clearClassPropertyFetcherCache()
    }

    /**
     * Use this method when you plan to perform some meta-programming
     * on a class. It ensures that any modifications you make will be
     * cleared at the end of the test.
     * @param clazz The class to register.
     */
    protected void registerMetaClass(Class clazz) {
        // Create a new EMC for the class and attach it.
        MetaClassRegistryCleaner.addAlteredMetaClass(clazz, GroovySystem.getMetaClassRegistry().getMetaClass(clazz))
        def emc = new ExpandoMetaClass(clazz, true, true)
        emc.initialize()
        GroovySystem.metaClassRegistry.setMetaClass(clazz, emc)
    }

    /**
     * Creates a new Grails mock for the given class. Use it as you
     * would use MockFor and StubFor.
     * @param clazz The class to mock.
     * @param loose If <code>true</code>, the method returns a loose-
     * expectation mock, otherwise it returns a strict one. The default
     * is a strict mock.
     */
    protected GrailsMock mockFor(Class clazz, boolean loose = false) {
        registerMetaClass(clazz)
        return new GrailsMock(clazz, loose)
    }

    /**
     * Mocks the given class (either a domain class or a command object)
     * so that a "validate()" method is added. This can then be used
     * to test the constraints on the class.
     */
    protected void mockForConstraintsTests(Class clazz, List instances = []) {
        registerMetaClass(clazz)
        MockUtils.prepareForConstraintsTests(clazz, errorsMap, instances)
    }

    /**
     * Mocks a domain class, providing working implementations of the
     * standard dynamic methods. A list of domain instances can be
     * provided as a source of data for the methods, in particular
     * the queries.
     * @param domainClass The class to mock.
     * @param instances An optional list of domain instances to use
     * as the data backing the dynamic methods.
     */
    protected void mockDomain(Class domainClass, List instances = []) {
        registerMetaClass(domainClass)
        def dc = MockUtils.mockDomain(domainClass, errorsMap, instances)

        domainClassesInfo.addGrailsClass(dc)
        addConverters(domainClass, false)
    }

    /**
     * Enables the cascading validation support for domain classes.
     * This should be called <i>after</i> all the relevant domain
     * classes have been mocked.
     */
    protected void enableCascadingValidation() {
        // We need to call this before we access the "grailsClasses"
        // property, otherwise that property will be null.
        domainClassesInfo.updateComplete()

        // This method is required to enable cascading validation
        // because it populates some important relationship information.
        GrailsDomainConfigurationUtil.configureDomainClassRelationships(
                domainClassesInfo.grailsClasses,
                domainClassesInfo.grailsClassesByName)
    }

    /**
     * Mocks a controller class, providing implementations of standard methods
     * like render and redirect
     */
    protected void mockController(Class controllerClass) {
        registerMetaClass(controllerClass)
        MockUtils.mockController(controllerClass)
    }

    /**
     * Mocks a tag library, providing the common web properties as
     * well as "out", "throwTagError()", and an implementation of
     * the "render" tag.
     */
    protected void mockTagLib(Class tagLibClass) {
        registerMetaClass(tagLibClass)
        MockUtils.mockTagLib(tagLibClass)
    }

    /**
     * Provides a mock implementation of the "log" property for the
     * given class. By default, debug and trace levels are ignored
     * but you can enable printing of debug statements via the <code>
     * enableDebug</code> argument.
     * @param clazz The class to add the log method to.
     * @param enableDebug An optional flag to switch on printing of
     * debug statements.
     */
    protected void mockLogging(Class clazz, boolean enableDebug = false) {
        registerMetaClass(clazz)
        MockUtils.mockLogging(clazz, enableDebug)
    }

    protected ConfigObject mockConfig(String config) {
        Holders.config = new ConfigSlurper().parse(config)
        return Holders.config
    }

    /**
     * Loads the given codec, adding the "encodeAs...()" and "decode...()"
     * methods to objects.
     * @param codecClass The codec to load, e.g. HTMLCodec.
     */
    protected void loadCodec(Class codecClass) {
        registerMetaClass(Object)

        if (loadedCodecs.contains(codecClass)) {
            return
        }

        loadedCodecs << codecClass

        new DefaultGrailsCodecClass(codecClass).configureCodecMethods()
    }

    protected void addConverters(Class clazz, boolean register = true) {
        if (register) {
            registerMetaClass(clazz)
        }
        clazz.metaClass.asType = { Class asClass ->
            if (ConverterUtil.isConverterClass(asClass)) {
                return ConverterUtil.createConverter(asClass, delegate, applicationContext)
            }

            return ConverterUtil.invokeOriginalAsTypeMethod(delegate, asClass)
        }
    }
}
