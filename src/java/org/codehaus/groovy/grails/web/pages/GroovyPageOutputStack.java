package org.codehaus.groovy.grails.web.pages;

import java.io.Writer;
import java.util.Stack;

import org.apache.commons.io.output.NullWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class GroovyPageOutputStack {
	public static final Log log = LogFactory.getLog(GroovyPageOutputStack.class);
	
	private static final String ATTRIBUTE_NAME_OUTPUT_STACK="org.codehaus.groovy.grails.GSP_OUTPUT_STACK";
	
	public static final GroovyPageOutputStack currentStack() {
		return currentStack(true);
	}
	
	public static final GroovyPageOutputStack currentStack(boolean allowCreate) {
		return currentStack(allowCreate, null, allowCreate, false);
	}
	
	public static final GroovyPageOutputStack currentStack(boolean allowCreate, Writer topWriter, boolean autoSync, boolean pushTop) {
		GroovyPageOutputStack outputStack=(GroovyPageOutputStack)RequestContextHolder.currentRequestAttributes().getAttribute(ATTRIBUTE_NAME_OUTPUT_STACK, RequestAttributes.SCOPE_REQUEST);
		if(outputStack!=null) {
			if(pushTop && topWriter != null) {
				outputStack.push(topWriter);
			}
			return outputStack;
		} else if(allowCreate) {
			if(topWriter==null) {
				topWriter=defaultRequest();
			}
			return createNew(topWriter, autoSync);
		} else {
			return null;
		} 
	}
	
	private static GrailsWebRequest getGrailsWebRequest() {
		Object requestAttributes=RequestContextHolder.currentRequestAttributes();
		if(requestAttributes instanceof GrailsWebRequest) {
			return (GrailsWebRequest)requestAttributes;
		} else {
			return null;
		}
	}
	
	private static Writer defaultRequest() {
		GrailsWebRequest webRequest=getGrailsWebRequest();
		if(webRequest != null) {
			return webRequest.getOut();
		} else {
			return null;
		}
	}
	
	public static final GroovyPageOutputStack createNew(Writer topWriter) {
		return createNew(topWriter, false);
	}
	
	private static final GroovyPageOutputStack createNew(Writer topWriter, boolean autoSync) {
		GroovyPageOutputStack instance=new GroovyPageOutputStack(topWriter, autoSync);
		RequestContextHolder.currentRequestAttributes().setAttribute(ATTRIBUTE_NAME_OUTPUT_STACK, instance, RequestAttributes.SCOPE_REQUEST);
		return instance;
	}
	
	public static final void removeCurrentInstance() {
		RequestContextHolder.currentRequestAttributes().removeAttribute(ATTRIBUTE_NAME_OUTPUT_STACK, RequestAttributes.SCOPE_REQUEST);
	}
	
	public static final Writer currentWriter() {
		GroovyPageOutputStack outputStack=currentStack(false);
		if(outputStack != null) {
			return outputStack.getProxyWriter();
		} else {
			return defaultRequest();
		}
	}
	
	private Stack<WriterPair> stack=new Stack<WriterPair>();
	private GroovyPageProxyWriter proxyWriter;
	private boolean autoSync;

	private class WriterPair {
		Writer originalTarget;
		Writer unwrappedTarget;

		WriterPair(Writer originalTarget, Writer unwrappedTarget) {
			this.originalTarget = originalTarget;
			this.unwrappedTarget = unwrappedTarget;
		}
	}
	
	private class GroovyPageProxyWriter extends GrailsPrintWriter {
		public GroovyPageProxyWriter() {
			super(new NullWriter());
		}

		public void setOut(Writer newOut) {
			this.out=newOut;
		}
		
		public GroovyPageOutputStack getOutputStack() {
			return GroovyPageOutputStack.this;
		}
	}

	private GroovyPageOutputStack(Writer topWriter, boolean autoSync) {
		this.proxyWriter=new GroovyPageProxyWriter();
		this.autoSync=autoSync;
		push(topWriter);
		if(!autoSync) {
			applyWriterThreadLocals(proxyWriter);
		}
	}

	private Writer unwrapTargetWriter(Writer targetWriter) {
		if(targetWriter instanceof GrailsPrintWriter) {
			return ((GrailsPrintWriter)targetWriter).getOut();
		} else {
			return targetWriter;
		}
	}
	
	public void push(final Writer newWriter) {
		push(newWriter, false);
	}
	
	public void push(final Writer newWriter, final boolean checkExisting) {
		if(newWriter == proxyWriter && stack.size() > 0) {
			stack.push(stack.peek());
			return;
		}
		if(checkExisting) {
			for(WriterPair item : stack) {
				if(item.originalTarget==newWriter) {
					log.warn("Pushed a writer to stack a second time. Writer type " + newWriter.getClass().getName(), new Exception());
				}
			}
		}
		Writer unwrappedWriter = unwrapTargetWriter(newWriter);
		if(unwrappedWriter == proxyWriter && stack.size() > 0) {
			stack.push(stack.peek());
			return;
		}
		stack.push(new WriterPair(newWriter, unwrappedWriter));

		proxyWriter.setOut(newWriter);
		if(autoSync) {
			applyWriterThreadLocals(newWriter);
		}
	}
	
	public void pop() {
		pop(autoSync);
	}
	
	public void pop(boolean forceSync) {
		stack.pop();
		if(stack.size() > 0) {
			WriterPair pair=stack.peek();
			proxyWriter.setOut(pair.unwrappedTarget);
			if(forceSync) {
				applyWriterThreadLocals(pair.originalTarget);
			}
		}
	}
	
	public GroovyPageProxyWriter getProxyWriter() {
		return proxyWriter;
	}
	
	public Writer getCurrentOriginalWriter() {
		return stack.peek().originalTarget;
	}
	
	public void restoreThreadLocalsToOriginals() {
		Writer originalTopWriter=stack.firstElement().originalTarget;
		applyWriterThreadLocals(originalTopWriter);
	}

	private void applyWriterThreadLocals(Writer writer) {
		GrailsWebRequest webRequest = getGrailsWebRequest();
		if(webRequest != null) {
			webRequest.setOut(writer);
		}
	}
}
