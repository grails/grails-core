package org.codehaus.groovy.grails.web.util;

import grails.util.Metadata;
import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec;
import org.codehaus.groovy.grails.web.pages.FastStringWriter;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CodecPrintWriterTest {

    private static GrailsApplication initialGrailsApplication = null;


    @Test
    public void testPrintString() {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(new MockGrailsApplication(), stringwriter, HTMLCodec.class);
        writer.print("&&");
        assertEquals("&amp;&amp;", stringwriter.getValue());
    }

    @Test
    public void testPrintStringWithClosure() {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(new MockGrailsApplication(), stringwriter, CodecWithClosureProperties.class);
        writer.print("hello");
        assertEquals("-> hello <-", stringwriter.getValue());
    }

    @Test
    public void testPrintStreamCharBuffer() throws IOException {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(new MockGrailsApplication(), stringwriter, HTMLCodec.class);
        StreamCharBuffer buf=new StreamCharBuffer();
        buf.getWriter().write("&&");
        writer.write(buf);
        assertEquals("&amp;&amp;", stringwriter.getValue());
    }

    @Test
    public void testPrintStreamCharBufferWithClosure() throws IOException {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(new MockGrailsApplication(), stringwriter, CodecWithClosureProperties.class);
        StreamCharBuffer buf=new StreamCharBuffer();
        buf.getWriter().write("hola");
        writer.write(buf);
        assertEquals("-> hola <-", stringwriter.getValue());
    }
}

class MockGrailsApplication implements GrailsApplication {

    private Map<String, DefaultGrailsCodecClass> mockCodecArtefacts = new HashMap<String, DefaultGrailsCodecClass>() {{
        put(HTMLCodec.class.getName(), new DefaultGrailsCodecClass(HTMLCodec.class));
        put(CodecWithClosureProperties.class.getName(), new DefaultGrailsCodecClass(CodecWithClosureProperties.class));
    }};

    public GrailsClass getArtefact(String artefactType, String name) {
        return mockCodecArtefacts.get(name);
    }

    public void setApplicationContext(ApplicationContext arg0)
            throws BeansException {
        throw new UnsupportedOperationException();
    }

    public ConfigObject getConfig() {
        throw new UnsupportedOperationException();
    }

    public Map getFlatConfig() {
        throw new UnsupportedOperationException();
    }

    public ClassLoader getClassLoader() {
        throw new UnsupportedOperationException();
    }

    public Class[] getAllClasses() {
        throw new UnsupportedOperationException();
    }

    public Class[] getAllArtefacts() {
        throw new UnsupportedOperationException();
    }

    public ApplicationContext getMainContext() {
        throw new UnsupportedOperationException();
    }

    public void setMainContext(ApplicationContext context) {
        throw new UnsupportedOperationException();
    }

    public ApplicationContext getParentContext() {
        throw new UnsupportedOperationException();
    }

    public Class getClassForName(String className) {
        throw new UnsupportedOperationException();
    }

    public void refreshConstraints() {
        throw new UnsupportedOperationException();
    }

    public void refresh() {
        throw new UnsupportedOperationException();
    }

    public void rebuild() {
        throw new UnsupportedOperationException();
    }

    public Resource getResourceForClass(Class theClazz) {
        throw new UnsupportedOperationException();
    }

    public boolean isArtefact(Class theClazz) {
        throw new UnsupportedOperationException();
    }

    public boolean isArtefactOfType(String artefactType, Class theClazz) {
        throw new UnsupportedOperationException();
    }

    public boolean isArtefactOfType(String artefactType, String className) {
        throw new UnsupportedOperationException();
    }

    public ArtefactHandler getArtefactType(Class theClass) {
        throw new UnsupportedOperationException();
    }

    public ArtefactInfo getArtefactInfo(String artefactType) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass[] getArtefacts(String artefactType) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass getArtefactForFeature(String artefactType,
            Object featureID) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass addArtefact(String artefactType, Class artefactClass) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass addArtefact(String artefactType,
            GrailsClass artefactGrailsClass) {
        throw new UnsupportedOperationException();
    }

    public void registerArtefactHandler(ArtefactHandler handler) {
        throw new UnsupportedOperationException();
    }

    public boolean hasArtefactHandler(String type) {
        throw new UnsupportedOperationException();
    }

    public ArtefactHandler[] getArtefactHandlers() {
        throw new UnsupportedOperationException();
    }

    public void initialise() {
        throw new UnsupportedOperationException();
    }

    public boolean isInitialised() {
        throw new UnsupportedOperationException();
    }

    public Metadata getMetadata() {
        throw new UnsupportedOperationException();
    }

    public GrailsClass getArtefactByLogicalPropertyName(String type,
            String logicalName) {
        throw new UnsupportedOperationException();
    }

    public void addArtefact(Class artefact) {
        throw new UnsupportedOperationException();
    }

    public boolean isWarDeployed() {
        throw new UnsupportedOperationException();
    }

    public void addOverridableArtefact(Class artefact) {
        throw new UnsupportedOperationException();
    }

    public void configChanged() {
        throw new UnsupportedOperationException();
    }

    public ArtefactHandler getArtefactHandler(String type) {
        throw new UnsupportedOperationException();
    }
}
