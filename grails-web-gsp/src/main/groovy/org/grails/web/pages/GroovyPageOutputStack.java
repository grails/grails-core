/*
 * Copyright 2011 the original author or authors.
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
package org.grails.web.pages;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.support.encoding.EncodedAppender;
import org.grails.support.encoding.EncodedAppenderFactory;
import org.grails.support.encoding.EncodedAppenderWriterFactory;
import org.grails.support.encoding.Encoder;
import org.grails.support.encoding.EncoderAware;
import org.grails.support.encoding.EncodingStateRegistry;
import org.grails.support.encoding.StreamingEncoder;
import org.grails.support.encoding.StreamingEncoderWriter;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.CodecPrintWriter;
import org.grails.web.util.GrailsLazyProxyPrintWriter;
import org.grails.web.util.GrailsLazyProxyPrintWriter.DestinationFactory;
import org.grails.web.util.GrailsWrappedWriter;
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
        GroovyPageOutputStack outputStack = lookupStack(request);
        if (outputStack == null && allowCreate) {
            outputStack = currentStack(request, allowCreate, null, allowCreate, false);
        }
        return outputStack;
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
            return createNew(attributes);
        }

        return null;
    }

    private static final GroovyPageOutputStack createNew(GroovyPageOutputStackAttributes attributes) {
        if (attributes.getTopWriter() == null) {
            attributes=new GroovyPageOutputStackAttributes.Builder(attributes).topWriter(lookupRequestWriter(attributes.getWebRequest())).build();
        }
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
    private GroovyPageProxyWriterGroup writerGroup = new GroovyPageProxyWriterGroup();

    private static class StackEntry implements Cloneable {
        Writer originalTarget;
        Writer unwrappedTarget;
        Encoder staticEncoder;
        Encoder taglibEncoder;
        Encoder defaultTaglibEncoder;
        Encoder outEncoder;
        Encoder expressionEncoder;

        StackEntry(Writer originalTarget, Writer unwrappedTarget) {
            this.originalTarget = originalTarget;
            this.unwrappedTarget = unwrappedTarget;
        }

        @Override
        public StackEntry clone() {
            StackEntry newEntry = new StackEntry(originalTarget, unwrappedTarget);
            newEntry.staticEncoder = staticEncoder;
            newEntry.outEncoder = outEncoder;
            newEntry.taglibEncoder = taglibEncoder;
            newEntry.defaultTaglibEncoder = defaultTaglibEncoder;
            newEntry.expressionEncoder = expressionEncoder;
            return newEntry;
        }
    }

    static class GroovyPageProxyWriterGroup {
        GroovyPageProxyWriter activeWriter;

        void reset() {
            activateWriter(null);
        }

        void activateWriter(GroovyPageProxyWriter newWriter) {
            if (newWriter != activeWriter) {
                flushActive();
                activeWriter = newWriter;
            }
        }

        void flushActive() {
            if (activeWriter != null) {
                activeWriter.flush();
            }
        }
    }

    public class GroovyPageProxyWriter extends GrailsLazyProxyPrintWriter implements EncodedAppenderFactory, EncoderAware {
        GroovyPageProxyWriterGroup writerGroup;

        GroovyPageProxyWriter(GroovyPageProxyWriterGroup writerGroup, DestinationFactory factory) {
            super(factory);
            this.writerGroup = writerGroup;
        }

        public GroovyPageOutputStack getOutputStack() {
            return GroovyPageOutputStack.this;
        }

        @Override
        public Writer getOut() {
            writerGroup.activateWriter(this);
            return super.getOut();
        }

        @Override
        public EncodedAppender getEncodedAppender() {
            Writer out = getOut();
            if(out instanceof EncodedAppenderFactory) {
                return ((EncodedAppenderFactory)out).getEncodedAppender();
            } else if(out instanceof EncodedAppender) {
                return (EncodedAppender)getOut();
            } else {
                return null;
            }
        }

        @Override
        public Encoder getEncoder() {
            Writer out = getOut();
            if(out instanceof EncoderAware) {
                return ((EncoderAware)out).getEncoder();
            }
            return null;
        }
    }

    private GroovyPageOutputStack(GroovyPageOutputStackAttributes attributes) {
        outWriter = new GroovyPageProxyWriter(writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.outEncoder, encodingStateRegistry, GroovyPageConfig.OUT_CODEC_NAME);
            }
        });
        staticWriter = new GroovyPageProxyWriter(writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                if (stackEntry.staticEncoder == null) {
                    return stackEntry.unwrappedTarget;
                }
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.staticEncoder, encodingStateRegistry, GroovyPageConfig.STATIC_CODEC_NAME);
            }
        });
        expressionWriter = new GroovyPageProxyWriter(writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.expressionEncoder, encodingStateRegistry, GroovyPageConfig.EXPRESSION_CODEC_NAME);
            }
        });
        taglibWriter = new GroovyPageProxyWriter(writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.taglibEncoder != null ? stackEntry.taglibEncoder : stackEntry.defaultTaglibEncoder, encodingStateRegistry, GroovyPageConfig.TAGLIB_CODEC_NAME);
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
        writerGroup.reset();

        if (checkExisting) checkExistingStack(attributes.getTopWriter());

        StackEntry previousStackEntry = null;
        if (stack.size() > 0) {
            previousStackEntry = stack.peek();
        }

        Writer topWriter = attributes.getTopWriter();
        Writer unwrappedWriter = null;
        if (topWriter!=null) {
            if (topWriter instanceof GroovyPageProxyWriter) {
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
        stackEntry.outEncoder = applyEncoder(attributes.getOutEncoder(), previousStackEntry != null ? previousStackEntry.outEncoder : null, attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.staticEncoder = applyEncoder(attributes.getStaticEncoder(), previousStackEntry != null ? previousStackEntry.staticEncoder : null, attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.expressionEncoder = applyEncoder(attributes.getExpressionEncoder(), previousStackEntry != null ? previousStackEntry.expressionEncoder : null, attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.taglibEncoder = applyEncoder(attributes.getTaglibEncoder(), previousStackEntry != null ? previousStackEntry.taglibEncoder : null, attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.defaultTaglibEncoder = applyEncoder(attributes.getDefaultTaglibEncoder(), previousStackEntry != null ? previousStackEntry.defaultTaglibEncoder : null, attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());

        stack.push(stackEntry);

        resetWriters();

        if (autoSync) {
            applyWriterThreadLocals(attributes.getTopWriter());
        }
    }

    private Encoder applyEncoder(Encoder newEncoder, Encoder previousEncoder, boolean allowInheriting, boolean replaceOnly) {
        if (newEncoder != null && (!replaceOnly || previousEncoder==null || replaceOnly && previousEncoder.isSafe())) {
            return newEncoder;
        }
        if (allowInheriting) {
            return previousEncoder;
        }
        return null;
    }

    private void checkExistingStack(final Writer topWriter) {
        if (topWriter != null) {
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

    private Writer createEncodingWriter(Writer out, Encoder encoder, EncodingStateRegistry encodingStateRegistry, String codecWriterName) {
        Writer encodingWriter;
        if (out instanceof EncodedAppenderWriterFactory) {
            encodingWriter=((EncodedAppenderWriterFactory)out).getWriterForEncoder(encoder, encodingStateRegistry);
        } else if (encoder instanceof StreamingEncoder) {
            encodingWriter=new StreamingEncoderWriter(out, (StreamingEncoder)encoder, encodingStateRegistry);
        } else {
            encodingWriter=new CodecPrintWriter(out, encoder, encodingStateRegistry);
        }
        return encodingWriter;
    }

    public void pop() {
        pop(autoSync);
    }

    public void pop(boolean forceSync) {
        writerGroup.reset();
        stack.pop();
        resetWriters();
        if (stack.size() > 0) {
            StackEntry stackEntry = stack.peek();
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

    public Encoder getDefaultTaglibEncoder() {
        return stack.size() > 0 ? stack.peek().defaultTaglibEncoder : null;
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

    public void flushActiveWriter() {
        writerGroup.flushActive();
    }
}
