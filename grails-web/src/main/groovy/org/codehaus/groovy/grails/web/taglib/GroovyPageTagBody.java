/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.io.Writer;
import java.util.Map;

import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.util.Assert;

/**
 * Represents the body of a tag and captures its output returning the result
 * when invoked.
 * 
 * @author Graeme Rocher
 * @since 0.5
 */
@SuppressWarnings("rawtypes")
public class GroovyPageTagBody extends Closure {

	private static final long serialVersionUID = 4396762064131558457L;
	private Closure<?> bodyClosure;
	private Binding originalBinding;
	private boolean preferSubChunkWhenWritingToOtherBuffer;
	private GrailsWebRequest webRequest;
	private GroovyPage originalBindingPage;

	public GroovyPageTagBody(Object owner, GrailsWebRequest webRequest,
			Closure<?> bodyClosure) {
		this(owner, webRequest, bodyClosure, false);
	}

	public GroovyPageTagBody(Object owner, GrailsWebRequest webRequest,
			Closure<?> bodyClosure,
			boolean preferSubChunkWhenWritingToOtherBuffer) {
		super(owner);

		Assert.notNull(bodyClosure, "Argument [bodyClosure] cannot be null!");
		Assert.notNull(webRequest, "Argument [webRequest] cannot be null!");

		this.bodyClosure = bodyClosure;
		this.webRequest = webRequest;
		this.preferSubChunkWhenWritingToOtherBuffer = preferSubChunkWhenWritingToOtherBuffer;

		findPageScopeBinding();
	}

	private void findPageScopeBinding() {
		originalBinding = (Binding) webRequest.getCurrentRequest().getAttribute(GrailsApplicationAttributes.PAGE_SCOPE);
		if(originalBinding instanceof GroovyPageBinding) {
			originalBindingPage=((GroovyPageBinding)originalBinding).getOwner();
		}
	}

	private void changeBinding(Binding currentBinding) {
		if(originalBindingPage != null) {
			originalBindingPage.setBinding(currentBinding);
		}
		webRequest.getCurrentRequest().setAttribute(
				GrailsApplicationAttributes.PAGE_SCOPE, currentBinding);
	}

	public boolean isPreferSubChunkWhenWritingToOtherBuffer() {
		return preferSubChunkWhenWritingToOtherBuffer;
	}

	public void setPreferSubChunkWhenWritingToOtherBuffer(boolean prefer) {
		this.preferSubChunkWhenWritingToOtherBuffer = prefer;
	}

	@SuppressWarnings("unchecked")
	private Object captureClosureOutput(Object args) {
		final GroovyPageTagWriter capturedOut = new GroovyPageTagWriter(
				preferSubChunkWhenWritingToOtherBuffer);
		Binding currentBinding = originalBinding;
		boolean itChanged = false;
		Object originalIt = null;
		try {
			pushCapturedOut(capturedOut);

			Object bodyResult;

			if (args != null) {
				if (args instanceof Map && ((Map)args).size() > 0) {
					// The body can be passed a set of variables as a map that
					// are then made available in the binding. This allows the
					// contents of the body to reference any of these variables
					// directly.
					//
					// For example, body(foo: 1, bar: 'test') would allow this
					// GSP fragment to work:
					//
					// <td>Foo: ${foo} and bar: ${bar}</td>
					//
					// Note that any variables with the same name as one of the
					// new ones will be overridden for the scope of the host
					// tag's body.

					// GRAILS-2675: Copy the current binding so that we can
					// restore
					// it to its original state.
					currentBinding = new GroovyPageBinding(originalBinding);
					((GroovyPageBinding)currentBinding).addMap((Map) args);
					
					// Binding is only changed currently when body gets a map argument
					changeBinding(currentBinding);
				} else {
					originalIt = currentBinding.getVariables().get("it");
					currentBinding.getVariables().put("it", args);
					itChanged = true;
				}
				bodyResult = executeClosure(args);
			} else {
				bodyResult = executeClosure(null);
			}

			if (!capturedOut.isUsed() && bodyResult != null
					&& !(bodyResult instanceof Writer)) {
				return bodyResult;
			}
			return capturedOut.getBuffer();
		} finally {
			if(currentBinding != originalBinding) {
				changeBinding(originalBinding);
			}
			if(itChanged) {
				currentBinding.getVariables().put("it", originalIt);
			}
			popCapturedOut();
		}
	}

	private void popCapturedOut() {
		if (webRequest != null && webRequest.isActive()) {
			GroovyPageOutputStack.currentStack(webRequest).pop();
		} else {
			GroovyPageOutputStack.currentStack().pop();
		}
	}

	private void pushCapturedOut(GroovyPageTagWriter capturedOut) {
		if (webRequest != null && webRequest.isActive()) {
			GroovyPageOutputStack.currentStack(webRequest).push(capturedOut);
		} else {
			GroovyPageOutputStack.currentStack().push(capturedOut);
		}
	}

	private Object executeClosure(Object args) {
		if (args != null) {
			return bodyClosure.call(args);
		}

		return bodyClosure.call();
	}

	public Object doCall() {
		return captureClosureOutput(null);
	}

	public Object doCall(Object arguments) {
		return captureClosureOutput(arguments);
	}

	@Override
	public Object call() {
		return captureClosureOutput(null);
	}

	@Override
	public Object call(Object... args) {
		return captureClosureOutput(args);
	}

	@Override
	public Object call(Object arguments) {
		return captureClosureOutput(arguments);
	}
}
