package org.grails.core.legacy

import grails.core.GrailsDomainClass
import grails.util.GrailsNameUtils
import grails.util.Metadata
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.ArtefactHandler
import org.codehaus.groovy.grails.commons.ArtefactInfo
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Legacy bridge for older GrailsApplication API
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class LegacyGrailsApplication extends GroovyObjectSupport  implements GrailsApplication {

    protected static final Pattern GETCLASSESPROP_PATTERN = Pattern.compile(/(\w+)(Classes)/);
    protected static final Pattern GETCLASSESMETH_PATTERN = Pattern.compile(/(get)(\w+)(Classes)/);
    protected static final Pattern ISCLASS_PATTERN = Pattern.compile(/(is)(\w+)(Class)/);
    protected static final Pattern GETCLASS_PATTERN = Pattern.compile(/(get)(\w+)Class/);


    grails.core.GrailsApplication grailsApplication

    LegacyGrailsApplication(grails.core.GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    @Override
    Object getProperty(String property) {
        // look for getXXXXClasses
        final Matcher match = GETCLASSESPROP_PATTERN.matcher(property)
        // find match
        match.find()
        if (match.matches()) {
            def artefactName = GrailsNameUtils.getClassNameRepresentation(match.group(1))
            if (getArtefactHandler(artefactName)) {
                return getArtefacts(artefactName)
            }
        }
        return super.getProperty(property)
    }

    @Override
    ConfigObject getConfig() {
        def configObject = new ConfigObject()
        configObject.putAll(grailsApplication.config)
        return configObject
    }

    @Override
    Map<String, Object> getFlatConfig() {
        grailsApplication.flatConfig
    }

    @Override
    ClassLoader getClassLoader() {
        grailsApplication.classLoader
    }

    @Override
    Class[] getAllClasses() {
        grailsApplication.allClasses
    }

    @Override
    Class[] getAllArtefacts() {
        grailsApplication.allArtefacts
    }

    @Override
    ApplicationContext getMainContext() {
        grailsApplication.mainContext
    }

    @Override
    void setMainContext(ApplicationContext context) {
        grailsApplication.mainContext = context
    }

    @Override
    ApplicationContext getParentContext() {
        grailsApplication.parentContext
    }

    @Override
    Class getClassForName(String className) {
        grailsApplication.getClassForName(className)
    }

    @Override
    void refreshConstraints() {
        grailsApplication.refreshConstraints()
    }

    @Override
    void refresh() {
        grailsApplication.refresh()
    }

    @Override
    void rebuild() {
        grailsApplication.rebuild()
    }

    @Override
    Resource getResourceForClass(Class theClazz) {
        grailsApplication.getResourceForClass(theClazz)
    }

    @Override
    boolean isArtefact(Class theClazz) {
        grailsApplication.isArtefact(theClazz)
    }

    @Override
    boolean isArtefactOfType(String artefactType, Class theClazz) {
        grailsApplication.isArtefactOfType(artefactType, theClazz)
    }

    @Override
    boolean isArtefactOfType(String artefactType, String className) {
        grailsApplication.isArtefactOfType(artefactType, className)
    }

    @Override
    GrailsClass getArtefact(String artefactType, String name) {
        def grailsClass = (GrailsClass) grailsApplication.getArtefact(artefactType, name)
        return coerceToLegacyType(grailsClass)
    }

    @Override
    ArtefactHandler getArtefactType(Class theClass) {
        return (ArtefactHandler)grailsApplication.getArtefactType(theClass)
    }

    @Override
    ArtefactInfo getArtefactInfo(String artefactType) {
        return (ArtefactInfo)grailsApplication.getArtefactInfo(artefactType)
    }

    @Override
    GrailsClass[] getArtefacts(String artefactType) {
        if(DomainClassArtefactHandler.TYPE.equals(artefactType)) {
            return grailsApplication.getArtefacts(artefactType).collect() { GrailsClass grailsClass ->
                new LegacyGrailsDomainClass((GrailsDomainClass)grailsClass)
            } as GrailsClass[]
        }
        return grailsApplication.getArtefacts(artefactType) as GrailsClass[]
    }

    @Override
    GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        return coerceToLegacyType( (GrailsClass)grailsApplication.getArtefactForFeature(artefactType, featureID) )
    }

    @Override
    GrailsClass addArtefact(String artefactType, Class artefactClass) {
        return coerceToLegacyType((GrailsClass)grailsApplication.addArtefact(artefactType, artefactClass))
    }

    @Override
    GrailsClass addArtefact(String artefactType, GrailsClass artefactGrailsClass) {
        return coerceToLegacyType((GrailsClass)grailsApplication.addArtefact(artefactType, artefactGrailsClass))
    }

    @Override
    void registerArtefactHandler(ArtefactHandler handler) {
        grailsApplication.registerArtefactHandler(handler)
    }

    @Override
    boolean hasArtefactHandler(String type) {
        grailsApplication.hasArtefactHandler(type)
    }

    @Override
    ArtefactHandler[] getArtefactHandlers() {
        return grailsApplication.artefactHandlers as ArtefactHandler[]
    }

    @Override
    void initialise() {
        grailsApplication.initialise()
    }

    @Override
    boolean isInitialised() {
        grailsApplication.isInitialised()
    }

    @Override
    Metadata getMetadata() {
        grailsApplication.metadata
    }

    @Override
    GrailsClass getArtefactByLogicalPropertyName(String type, String logicalName) {
        return (GrailsClass)grailsApplication.getArtefactByLogicalPropertyName(type, logicalName)
    }

    @Override
    void addArtefact(Class artefact) {
        grailsApplication.addArtefact(artefact)
    }

    @Override
    boolean isWarDeployed() {
        grailsApplication.isWarDeployed()
    }

    @Override
    void addOverridableArtefact(Class artefact) {
        grailsApplication.addOverridableArtefact(artefact)
    }

    @Override
    void configChanged() {
        grailsApplication.configChanged()
    }

    @Override
    ArtefactHandler getArtefactHandler(String type) {
        return (ArtefactHandler)grailsApplication.getArtefactHandler(type)
    }

    private GrailsClass coerceToLegacyType(GrailsClass grailsClass) {
        if (grailsClass instanceof GrailsDomainClass) {
            return new LegacyGrailsDomainClass(grailsClass)
        }
        return grailsClass
    }
}
