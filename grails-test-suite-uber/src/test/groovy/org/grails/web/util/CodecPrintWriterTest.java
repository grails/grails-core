package org.grails.web.util;

import static org.junit.Assert.assertEquals;
import grails.util.Metadata;
import groovy.util.ConfigObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import grails.core.ArtefactHandler;
import grails.core.ArtefactInfo;
import org.grails.commons.DefaultGrailsCodecClass;
import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import org.grails.commons.GrailsCodecClass;
import org.grails.plugins.codecs.HTMLCodec;
import org.grails.plugins.testing.GrailsMockHttpServletRequest;
import org.grails.plugins.testing.GrailsMockHttpServletResponse;
import org.grails.support.encoding.DefaultEncodingStateRegistry;
import org.grails.support.encoding.Encoder;
import org.grails.support.encoding.EncodingStateRegistry;
import org.grails.web.pages.FastStringWriter;
import org.grails.web.pages.GroovyPageOutputStack;
import grails.web.util.GrailsApplicationAttributes;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestContextHolder;

public class CodecPrintWriterTest {
    EncodingStateRegistry registry=new DefaultEncodingStateRegistry();

    private Encoder getEncoder(GrailsApplication grailsApplication, Class<?> codecClass) {
        Encoder encoder=null;
        if (grailsApplication != null && codecClass != null) {
            GrailsCodecClass codecArtefact = (GrailsCodecClass) grailsApplication.getArtefact("Codec", codecClass.getName());
            encoder = codecArtefact.getEncoder();
        }
        return encoder;
    }

    @Test
    public void testPrintString() {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(stringwriter, getEncoder(new MockGrailsApplication(), HTMLCodec.class), registry);
        writer.print("&&");
        writer.flush();
        assertEquals("&amp;&amp;", stringwriter.getValue());
    }

    @Test
    public void testPrintStringWithClosure() {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(stringwriter, getEncoder(new MockGrailsApplication(), CodecWithClosureProperties.class), registry);
        writer.print("hello");
        writer.flush();
        assertEquals("-> hello <-", stringwriter.getValue());
    }

    @Test
    public void testPrintStreamCharBuffer() throws IOException {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(stringwriter, getEncoder(new MockGrailsApplication(), HTMLCodec.class), registry);
        StreamCharBuffer buf=new StreamCharBuffer();
        buf.getWriter().write("&&");
        writer.write(buf);
        writer.flush();
        assertEquals("&amp;&amp;", stringwriter.getValue());
    }

    @Test
    public void testPrintStreamCharBufferWithClosure() throws IOException {
        FastStringWriter stringwriter=new FastStringWriter();
        CodecPrintWriter writer=new CodecPrintWriter(stringwriter, getEncoder(new MockGrailsApplication(), CodecWithClosureProperties.class), registry);
        StreamCharBuffer buf=new StreamCharBuffer();
        buf.getWriter().write("hola");
        writer.write(buf);
        writer.flush();
        assertEquals("-> hola <-", stringwriter.getValue());
    }

    @Test
    public void testCodecAndNoCodecGRAILS8405() throws IOException {
        FastStringWriter target = new FastStringWriter();

        GrailsWebRequest webRequest = bindMockHttpRequest();

        // Initialize out and codecOut as it is done in GroovyPage.initRun
        GroovyPageOutputStack outputStack = GroovyPageOutputStack.currentStack(true, target, false, true);
        GrailsPrintWriter out = outputStack.getOutWriter();
        webRequest.setOut(out);
        GrailsPrintWriter codecOut = new CodecPrintWriter(out, getEncoder(new MockGrailsApplication(), CodecWithClosureProperties.class), registry);

        // print some output
        codecOut.print("hola");
        codecOut.flush();
        out.print("1");
        out.print("2");
        out.print("3");

        // similar as taglib call
        FastStringWriter bufferWriter=new FastStringWriter();
        GrailsPrintWriter out2=new GrailsPrintWriter(bufferWriter);
        outputStack.push(out2);
        out.print("4");
        codecOut.print("A");
        codecOut.flush();
        outputStack.pop();

        // add output before appending "taglib output"
        out.print("added");
        codecOut.print("too");
        codecOut.flush();

        // append "taglib output"
        out.leftShift(bufferWriter.getBuffer());

        // print some more output
        codecOut.print("B");
        codecOut.flush();
        out.print("5");
        codecOut.print("C");
        codecOut.flush();

        // clear thread local
        RequestContextHolder.setRequestAttributes(null);

        assertEquals("-> hola <-123added-> too <-4-> A <--> B <-5-> C <-", target.getValue());

        codecOut.close();
    }

    private GrailsWebRequest bindMockHttpRequest() {
        GrailsMockHttpServletRequest mockRequest=new GrailsMockHttpServletRequest();
        GrailsMockHttpServletResponse mockResponse=new GrailsMockHttpServletResponse();
        GrailsWebRequest webRequest = new GrailsWebRequest(mockRequest, mockResponse, mockRequest.getServletContext());
        mockRequest.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest);
        RequestContextHolder.setRequestAttributes(webRequest);
        return webRequest;
    }
}

@SuppressWarnings({ "rawtypes", "unchecked" })
class MockGrailsApplication implements GrailsApplication {

    private Map<String, DefaultGrailsCodecClass> mockCodecArtefacts = new HashMap<String, DefaultGrailsCodecClass>();

    MockGrailsApplication() {
        mockCodecArtefacts.put(HTMLCodec.class.getName(), new DefaultGrailsCodecClass(HTMLCodec.class));
        mockCodecArtefacts.put(CodecWithClosureProperties.class.getName(), new DefaultGrailsCodecClass(CodecWithClosureProperties.class));
    }

    public GrailsClass getArtefact(String artefactType, String name) {
        return mockCodecArtefacts.get(name);
    }

    public void setApplicationContext(ApplicationContext ctx) {
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

    public GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass addArtefact(String artefactType, Class artefactClass) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass addArtefact(String artefactType, GrailsClass gc) {
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

    public GrailsClass getArtefactByLogicalPropertyName(String type, String logicalName) {
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
