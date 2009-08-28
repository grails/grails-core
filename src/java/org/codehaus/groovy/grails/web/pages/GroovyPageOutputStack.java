package org.codehaus.groovy.grails.web.pages;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class GroovyPageOutputStack {
	public static final Log log = LogFactory.getLog(GroovyPageOutputStack.class);
	
	private static final ThreadLocal<GroovyPageOutputStack> instances=new ThreadLocal<GroovyPageOutputStack>();
	
	public static final GroovyPageOutputStack currentStack() {
		return currentStack(true);
	}
	
	public static final GroovyPageOutputStack currentStack(boolean allowCreate) {
		return currentStack(allowCreate, (Writer)RequestContextHolder.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.OUT, RequestAttributes.SCOPE_REQUEST), true, false);
	}
	
	public static final GroovyPageOutputStack currentStack(boolean allowCreate, Writer topWriter, boolean autoSync, boolean pushTop) {
		GroovyPageOutputStack outputStack=instances.get();
		if(outputStack!=null) {
			if(pushTop) {
				outputStack.push(topWriter);
			}
			return outputStack;
		} else if(allowCreate) {
			return createNew(topWriter, autoSync);
		} else {
			return null;
		} 
	}
	
	public static final GroovyPageOutputStack createNew(Writer topWriter) {
		return createNew(topWriter, false);
	}
	
	private static final GroovyPageOutputStack createNew(Writer topWriter, boolean autoCreated) {
		GroovyPageOutputStack instance=new GroovyPageOutputStack(topWriter, autoCreated);
		instances.set(instance);
		return instance;
	}
	
	public static final void removeCurrentInstance() {
		instances.remove();
	}
	
	public static final Writer currentWriter() {
		GroovyPageOutputStack outputStack=currentStack(false);
		if(outputStack != null) {
			return outputStack.getProxyWriter();
		} else {
			return (Writer)RequestContextHolder.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.OUT, RequestAttributes.SCOPE_REQUEST);
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
			super(new StringWriter());
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
		if(newWriter == proxyWriter) {
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
		if(unwrappedWriter == proxyWriter) {
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
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		if(webRequest != null) {
			webRequest.setOut(writer);
		}
	}
}
