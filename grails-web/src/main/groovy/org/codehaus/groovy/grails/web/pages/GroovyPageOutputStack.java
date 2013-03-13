package org.codehaus.groovy.grails.web.pages;

import java.io.Writer;
import java.util.Stack;

import org.apache.commons.io.output.NullWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncoderAwareWriterFactory;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistry;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.CodecPrintWriter;
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;
import org.codehaus.groovy.grails.web.util.GrailsWrappedWriter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class GroovyPageOutputStack {
    
    public static final Log log = LogFactory.getLog(GroovyPageOutputStack.class);

    private static final String ATTRIBUTE_NAME_OUTPUT_STACK="org.codehaus.groovy.grails.GSP_OUTPUT_STACK";
    
    public static GroovyPageOutputStack currentStack() {
        return currentStack(true);
    }

    public static GroovyPageOutputStack currentStack(GrailsWebRequest request) {
        return currentStack(request, true);
    }

    public static GroovyPageOutputStack currentStack(boolean allowCreate) {
        return currentStack(GrailsWebRequest.lookup(), allowCreate);
    }

    public static GroovyPageOutputStack currentStack(GrailsWebRequest request, boolean allowCreate) {
        if(!allowCreate) {
            return lookupStack(request);
        } else {
            return currentStack(request, allowCreate, null, allowCreate, false);
        }
    }

    public static GroovyPageOutputStack currentStack(boolean allowCreate, Writer topWriter, boolean autoSync, boolean pushTop) {
        return currentStack(GrailsWebRequest.lookup(), allowCreate, topWriter, autoSync, pushTop);
    }
    
    public static GroovyPageOutputStack currentStack(GrailsWebRequest request, boolean allowCreate, Writer topWriter, boolean autoSync, boolean pushTop) {
        return currentStack(new GroovyPageOutputStackAttributes.Builder().webRequest(request).allowCreate(allowCreate).topWriter(topWriter).autoSync(autoSync).pushTop(pushTop).build());
    }

    public static GroovyPageOutputStack currentStack(GroovyPageOutputStackAttributes attributes) {
        GroovyPageOutputStack outputStack = lookupStack(attributes.getWebRequest());
        if (outputStack != null) {
            if (attributes.isPushTop() && attributes.getTopWriter() != null) {
                outputStack.push(attributes, false);
            }
            return outputStack;
        }

        if (attributes.isAllowCreate()) {
            attributes.setTopWriter(lookupRequestWriter(attributes.getWebRequest()));
            return createNew(attributes);
        }

        return null;
    }

    private static final GroovyPageOutputStack createNew(GroovyPageOutputStackAttributes attributes) {
        GroovyPageOutputStack instance = new GroovyPageOutputStack(attributes);
        attributes.getWebRequest().setAttribute(
                ATTRIBUTE_NAME_OUTPUT_STACK, instance, RequestAttributes.SCOPE_REQUEST);
        return instance;
    }
    
    private static GroovyPageOutputStack lookupStack(GrailsWebRequest webRequest) {
        GroovyPageOutputStack outputStack = (GroovyPageOutputStack) webRequest.getAttribute(
                ATTRIBUTE_NAME_OUTPUT_STACK, RequestAttributes.SCOPE_REQUEST);
        return outputStack;
    }

    public static final void removeCurrentInstance() {
        RequestContextHolder.currentRequestAttributes().removeAttribute(
                ATTRIBUTE_NAME_OUTPUT_STACK, RequestAttributes.SCOPE_REQUEST);
    }

    public static final Writer currentWriter() {
        GroovyPageOutputStack outputStack=currentStack(false);
        if (outputStack != null) {
            return outputStack.getPageWriter();
        }

        return lookupRequestWriter();
    }

    private static Writer lookupRequestWriter() {
        GrailsWebRequest webRequest=GrailsWebRequest.lookup();
        return lookupRequestWriter(webRequest);
    }

    private static Writer lookupRequestWriter(GrailsWebRequest webRequest) {
        if (webRequest != null) {
            return webRequest.getOut();
        }
        return null;
    }

    private Stack<StackEntry> stack = new Stack<StackEntry>();
    private GroovyPageProxyWriter pageWriter;
    private GroovyPageProxyWriter templateWriter;
    private GroovyPageProxyWriter codecWriter;
    private boolean autoSync;
    private EncodingStateRegistry encodingStateRegistry;

    private static class StackEntry implements Cloneable {
        Writer originalTarget;
        Writer unwrappedTarget;
        Encoder templateEncoder;
        Encoder pageEncoder;
        Encoder defaultEncoder;

        StackEntry(Writer originalTarget, Writer unwrappedTarget) {
            this.originalTarget = originalTarget;
            this.unwrappedTarget = unwrappedTarget;
        }
        
        public StackEntry clone() {
            StackEntry newEntry = new StackEntry(originalTarget, unwrappedTarget);
            newEntry.templateEncoder = templateEncoder;
            newEntry.pageEncoder = pageEncoder;
            newEntry.defaultEncoder = defaultEncoder;
            return newEntry;
        }
    }

    private class GroovyPageProxyWriter extends GrailsPrintWriter {
        public GroovyPageProxyWriter() {
            super(new NullWriter());
        }

        @SuppressWarnings("unused")
        public GroovyPageOutputStack getOutputStack() {
            return GroovyPageOutputStack.this;
        }
    }

    private GroovyPageOutputStack(GroovyPageOutputStackAttributes attributes) {
        pageWriter = new GroovyPageProxyWriter();
        templateWriter = new GroovyPageProxyWriter();
        codecWriter = new GroovyPageProxyWriter();
        this.autoSync = attributes.isAutoSync();
        push(attributes, false);
        if (!autoSync) {
            applyWriterThreadLocals(pageWriter);
        }
        this.encodingStateRegistry = attributes.getWebRequest().getEncodingStateRegistry();
    }

    private Writer unwrapTargetWriter(Writer targetWriter) {
        if (targetWriter instanceof GrailsWrappedWriter) {
            return ((GrailsWrappedWriter)targetWriter).unwrap();
        }
        return targetWriter;
    }
    
    public void push(final Writer newWriter) {
        push(newWriter, false);
    }

    public void push(final Writer newWriter, final boolean checkExisting) {
        if(cloneStackIfSameWriter(newWriter)) {
            return;
        }
        if(checkExisting) checkExistingStack(newWriter);
        
        GroovyPageOutputStackAttributes.Builder attributesBuilder=new GroovyPageOutputStackAttributes.Builder();
        if (stack.size() > 0) {
            StackEntry stackEntry = stack.peek();
            attributesBuilder.pageEncoder(stackEntry.pageEncoder);
            attributesBuilder.templateEncoder(stackEntry.templateEncoder);
            attributesBuilder.defaultEncoder(stackEntry.defaultEncoder);
        }
        attributesBuilder.topWriter(newWriter);
        push(attributesBuilder.build(), false);
    }

    public void push(final GroovyPageOutputStackAttributes attributes, final boolean checkExisting) {
        if(cloneStackIfSameWriter(attributes.getTopWriter())) {
            return;
        }
        if(checkExisting) checkExistingStack(attributes.getTopWriter());

        Writer unwrappedWriter = unwrapTargetWriter(attributes.getTopWriter());
        if (unwrappedWriter == pageWriter && stack.size() > 0) {
            stack.push(stack.peek().clone());
            return;
        }

        StackEntry stackEntry = new StackEntry(attributes.getTopWriter(), unwrappedWriter);
        stackEntry.pageEncoder = attributes.getPageEncoder();
        stackEntry.templateEncoder = attributes.getTemplateEncoder();
        stackEntry.defaultEncoder = attributes.getDefaultEncoder();
        stack.push(stackEntry);

        updateWriters(stackEntry);

        if (autoSync) {
            applyWriterThreadLocals(attributes.getTopWriter());
        }
    }

    private void checkExistingStack(final Writer topWriter) {
        for (StackEntry item : stack) {
            if (item.originalTarget == topWriter) {
                log.warn("Pushed a writer to stack a second time. Writer type " +
                        topWriter.getClass().getName(), new Exception());
            }
        }
    }

    private boolean cloneStackIfSameWriter(final Writer topWriter) {
        if (topWriter == pageWriter && stack.size() > 0) {
            stack.push(stack.peek().clone());
            return true;
        }
        return false;
    }

    private void updateWriters(StackEntry stackEntry) {
        pageWriter.setOut(createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.pageEncoder, encodingStateRegistry));
        Writer templateWriterTarget = null;
        if(stackEntry.templateEncoder != null) {
            templateWriterTarget = createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.templateEncoder, encodingStateRegistry);
        } else {
            templateWriterTarget = stackEntry.unwrappedTarget;            
        }
        templateWriter.setOut(templateWriterTarget);
        codecWriter.setOut(createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.defaultEncoder, encodingStateRegistry));
    }
    
    private Writer createEncodingWriter(Writer out, Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        Writer encodingWriter;
        if(out instanceof EncoderAwareWriterFactory) {
            encodingWriter=((EncoderAwareWriterFactory)out).getWriterForEncoder(encoder, encodingStateRegistry);
        } else {
            encodingWriter=new CodecPrintWriter(out, encoder, encodingStateRegistry);
        }
        return encodingWriter;
    }

    public void pop() {
        pop(autoSync);
    }

    public void pop(boolean forceSync) {
        stack.pop();
        if (stack.size() > 0) {
            StackEntry stackEntry = stack.peek();
            updateWriters(stackEntry);
            if (forceSync) {
                applyWriterThreadLocals(stackEntry.originalTarget);
            }
        }
    }

    public GroovyPageProxyWriter getPageWriter() {
        return pageWriter;
    }
    
    public GroovyPageProxyWriter getTemplateWriter() {
        return templateWriter;
    }

    public GroovyPageProxyWriter getCodecWriter() {
        return codecWriter;
    }

    public Writer getCurrentOriginalWriter() {
        return stack.peek().originalTarget;
    }

    public void restoreThreadLocalsToOriginals() {
        Writer originalTopWriter = stack.firstElement().originalTarget;
        applyWriterThreadLocals(originalTopWriter);
    }

    private void applyWriterThreadLocals(Writer writer) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest != null) {
            webRequest.setOut(writer);
        }
    }
}
