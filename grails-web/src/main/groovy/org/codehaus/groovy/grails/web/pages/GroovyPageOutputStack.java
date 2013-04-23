package org.codehaus.groovy.grails.web.pages;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.support.encoding.EncodedAppenderWriterFactory;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistry;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.CodecPrintWriter;
import org.codehaus.groovy.grails.web.util.GrailsLazyProxyPrintWriter;
import org.codehaus.groovy.grails.web.util.GrailsLazyProxyPrintWriter.DestinationFactory;
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
            if (attributes.isPushTop()) {
                outputStack.push(attributes, false);
            }
            return outputStack;
        }

        if (attributes.isAllowCreate()) {
            if(attributes.getTopWriter()==null) {
                attributes=new GroovyPageOutputStackAttributes.Builder(attributes).topWriter(lookupRequestWriter(attributes.getWebRequest())).build();
            }
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
            return outputStack.getOutWriter();
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
    private GroovyPageProxyWriter taglibWriter;
    private GroovyPageProxyWriter outWriter;
    private GroovyPageProxyWriter staticWriter;
    private GroovyPageProxyWriter expressionWriter;
    private boolean autoSync;
    private EncodingStateRegistry encodingStateRegistry;

    private static class StackEntry implements Cloneable {
        Writer originalTarget;
        Writer unwrappedTarget;
        Encoder staticEncoder;
        Encoder taglibEncoder;
        Encoder outEncoder;
        Encoder expressionEncoder;

        StackEntry(Writer originalTarget, Writer unwrappedTarget) {
            this.originalTarget = originalTarget;
            this.unwrappedTarget = unwrappedTarget;
        }
        
        public StackEntry clone() {
            StackEntry newEntry = new StackEntry(originalTarget, unwrappedTarget);
            newEntry.staticEncoder = staticEncoder;
            newEntry.outEncoder = outEncoder;
            newEntry.taglibEncoder = taglibEncoder;
            newEntry.expressionEncoder = expressionEncoder;
            return newEntry;
        }
    }

    public class GroovyPageProxyWriter extends GrailsLazyProxyPrintWriter {
        GroovyPageProxyWriter(DestinationFactory factory) {
            super(factory);
        }

        public GroovyPageOutputStack getOutputStack() {
            return GroovyPageOutputStack.this;
        }
    }

    private GroovyPageOutputStack(GroovyPageOutputStackAttributes attributes) {
        outWriter = new GroovyPageProxyWriter(new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.outEncoder, encodingStateRegistry);
            }
        });
        staticWriter = new GroovyPageProxyWriter(new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                if(stackEntry.staticEncoder != null) {
                    return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.staticEncoder, encodingStateRegistry);
                } else {
                    return stackEntry.unwrappedTarget;            
                }
            }
        });
        expressionWriter = new GroovyPageProxyWriter(new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.expressionEncoder, encodingStateRegistry);
            }
        });
        taglibWriter = new GroovyPageProxyWriter(new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.taglibEncoder, encodingStateRegistry);
            }
        });
        this.autoSync = attributes.isAutoSync();
        push(attributes, false);
        if (!autoSync) {
            applyWriterThreadLocals(outWriter);
        }
        this.encodingStateRegistry = attributes.getWebRequest().getEncodingStateRegistry();
    }

    private Writer unwrapTargetWriter(Writer targetWriter) {
        if (targetWriter instanceof GrailsWrappedWriter && ((GrailsWrappedWriter)targetWriter).isAllowUnwrappingOut()) {
            return ((GrailsWrappedWriter)targetWriter).unwrap();
        }
        return targetWriter;
    }
    
    public void push(final Writer newWriter) {
        push(newWriter, false);
    }

    public void push(final Writer newWriter, final boolean checkExisting) {
        GroovyPageOutputStackAttributes.Builder attributesBuilder=new GroovyPageOutputStackAttributes.Builder();
        attributesBuilder.inheritPreviousEncoders(true);
        attributesBuilder.topWriter(newWriter);
        push(attributesBuilder.build(), checkExisting);
    }
    
    public void push(final GroovyPageOutputStackAttributes attributes) {
        push(attributes, false);
    }

    public void push(final GroovyPageOutputStackAttributes attributes, final boolean checkExisting) {
        if(checkExisting) checkExistingStack(attributes.getTopWriter());

        StackEntry previousStackEntry = null;
        if (stack.size() > 0) {
            previousStackEntry = stack.peek();
        }
        
        Writer topWriter = attributes.getTopWriter();
        Writer unwrappedWriter = null;
        if(topWriter!=null) {
            if(topWriter instanceof GroovyPageProxyWriter) {
                topWriter = ((GroovyPageProxyWriter)topWriter).getOut();
            }
            unwrappedWriter = unwrapTargetWriter(topWriter);
        } else if (previousStackEntry != null) {
            topWriter = previousStackEntry.originalTarget;
            unwrappedWriter = previousStackEntry.unwrappedTarget;
        } else {
            throw new NullPointerException("attributes.getTopWriter() is null and there is no previous stack item");
        }

        StackEntry stackEntry = new StackEntry(topWriter, unwrappedWriter);
        stackEntry.outEncoder = applyEncoder(attributes.getOutEncoder(), previousStackEntry != null ? previousStackEntry.outEncoder : null, attributes.isInheritPreviousEncoders());
        stackEntry.staticEncoder = applyEncoder(attributes.getStaticEncoder(), previousStackEntry != null ? previousStackEntry.staticEncoder : null, attributes.isInheritPreviousEncoders());
        stackEntry.expressionEncoder = applyEncoder(attributes.getExpressionEncoder(), previousStackEntry != null ? previousStackEntry.expressionEncoder : null, attributes.isInheritPreviousEncoders());
        stackEntry.taglibEncoder = applyEncoder(attributes.getTaglibEncoder(), previousStackEntry != null ? previousStackEntry.taglibEncoder : null, attributes.isInheritPreviousEncoders());
        stack.push(stackEntry);

        resetWriters();

        if (autoSync) {
            applyWriterThreadLocals(attributes.getTopWriter());
        }
    }
    
    private Encoder applyEncoder(Encoder newEncoder, Encoder previousEncoder, boolean allowInheriting) {
        if(newEncoder != null) {
            return newEncoder;
        } else if (allowInheriting) {
            return previousEncoder;
        } else {
            return null;
        }
    }

    private void checkExistingStack(final Writer topWriter) {
        if(topWriter != null) {
            for (StackEntry item : stack) {
                if (item.originalTarget == topWriter) {
                    log.warn("Pushed a writer to stack a second time. Writer type " +
                            topWriter.getClass().getName(), new Exception());
                }
            }
        }
    }

    private void resetWriters() {
        outWriter.setDestinationActivated(false);
        staticWriter.setDestinationActivated(false);
        expressionWriter.setDestinationActivated(false);
        taglibWriter.setDestinationActivated(false);
    }
    
    private Writer createEncodingWriter(Writer out, Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        Writer encodingWriter;
        if(out instanceof EncodedAppenderWriterFactory) {
            encodingWriter=((EncodedAppenderWriterFactory)out).getWriterForEncoder(encoder, encodingStateRegistry);
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
            resetWriters();
            if (forceSync) {
                applyWriterThreadLocals(stackEntry.originalTarget);
            }
        }
    }

    public GroovyPageProxyWriter getOutWriter() {
        return outWriter;
    }
    
    public GroovyPageProxyWriter getStaticWriter() {
        return staticWriter;
    }

    public GroovyPageProxyWriter getExpressionWriter() {
        return expressionWriter;
    }
    
    public GroovyPageProxyWriter getTaglibWriter() {
        return taglibWriter;
    }
    
    public Encoder getOutEncoder() {
        return stack.size() > 0 ? stack.peek().outEncoder : null;
    }

    public Encoder getStaticEncoder() {
        return stack.size() > 0 ? stack.peek().staticEncoder : null;
    }

    public Encoder getExpressionEncoder() {
        return stack.size() > 0 ? stack.peek().expressionEncoder : null;
    }
    
    public Encoder getTaglibEncoder() {
        return stack.size() > 0 ? stack.peek().taglibEncoder : null;
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
