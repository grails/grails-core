/* Copyright 2008 the original author or authors.
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
package grails.test;

import groovy.lang.Closure;

/**
 * Skeleton implementation of a wrapper class for closures that allows
 * you to intercept invocations of the closure. The wrapper can be used
 * anywhere that the target closure can be used.
 */
public abstract class AbstractClosureProxy extends Closure {
    private Closure target;

    /**
     * Creates a new instance that wraps the target closure and sends
     * profiling events to the given profiler log.
     * @param closure The target closure to wrap.
     */
    public AbstractClosureProxy(Closure closure) {
        super(closure.getOwner(), closure.getThisObject());
        this.target = closure;
    }

    /**
     * This method is called before the target closure is invoked.
     * This is a passive interceptor, so you cannot prevent the
     * call to the target closure. You can modify the arguments,
     * though, but it's not recommended unless you really know
     * what you're doing.
     * @param args The arguments passed to the closure.
     */
    protected abstract void doBeforeCall(Object[] args);

    /**
     * This method is called after the target closure is invoked.
     * It will be triggered whether or not an exception is thrown
     * by the target closure.
     * @param args The arguments passed to the closure.
     */
    protected abstract void doAfterCall(Object[] args);

    /**
     * Called when a new instance of the proxy needs to be created for
     * the given closure. Usually the implementation simply creates a
     * new instance of the current class, copying over the existing
     * proxy properties:
     * <pre>
     *    return new MyClosureProxy(c, this.field1, ...)
     * </pre>
     * @param c The closure to wrap/proxy.
     */
    protected abstract Closure createWrapper(Closure c);

    /**
     * This is the important one: logs entry and exit of the closure
     * call.
     */
    public Object call(Object[] objects) {
        doBeforeCall(objects);

        try {
            return this.target.call(objects);
        }
        finally {
            doAfterCall(objects);
        }
    }

    /**
     * Compares based on identities, but unlike the standard implementation
     * this one will return <code>true</code> if the given object is the
     * target closure for this wrapper as well.
     */
    public boolean equals(Object obj) {
        return this == obj || this.target == obj;
    }

    public int hashCode() {
        return this.target.hashCode();
    }

    public Closure curry(Object[] objects) {
        return createWrapper(this.target.curry(objects));
    }

    public boolean isCase(Object o) {
        return this.target.isCase(o);
    }

    public Closure asWritable() {
        return this.target.asWritable();
    }

    public Object getProperty(String property) {
        return this.target.getProperty(property);
    }

    public void setProperty(String s, Object o) {
        this.target.setProperty(s, o);
    }

    public int getMaximumNumberOfParameters() {
        return this.target.getMaximumNumberOfParameters();
    }

    public Class[] getParameterTypes() {
        return this.target.getParameterTypes();
    }

    public Object getDelegate() {
        return this.target.getDelegate();
    }

    public void setDelegate(Object o) {
        this.target.setDelegate(o);
    }

    public int getDirective() {
        return this.target.getDirective();
    }

    public void setDirective(int i) {
        this.target.setDirective(i);
    }

    public int getResolveStrategy() {
        return this.target.getResolveStrategy();
    }

    public void setResolveStrategy(int i) {
        this.target.setResolveStrategy(i);
    }
}
