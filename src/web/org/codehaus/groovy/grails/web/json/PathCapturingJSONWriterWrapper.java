package org.codehaus.groovy.grails.web.json;

import java.io.Writer;
import java.util.Stack;

/**
 * TODO Proof of concept
 * Should capture the JSON Path to the current element
 * 
 * @author Siegfried Puchbauer
 */
public class PathCapturingJSONWriterWrapper extends JSONWriter {

    private JSONWriter delegate;

    private Stack<PathElement> pathStack = new Stack<PathElement>();

    public PathCapturingJSONWriterWrapper(Writer w, JSONWriter delegate) {
        super(w);
        this.delegate = delegate;
    }

    public JSONWriter append(String s) {
        return delegate.append(s);
    }

    public void comma() {
        delegate.comma();
    }

    public JSONWriter array() {
        return delegate.array();
    }

    public JSONWriter end(Mode m, char c) {
        return delegate.end(m, c);
    }

    public JSONWriter endArray() {
        return delegate.endArray();
    }

    public JSONWriter endObject() {
        return delegate.endObject();
    }

    public JSONWriter key(String s) {
        pathStack.push(new PropertyElement(s));
        return delegate.key(s);
    }

    public JSONWriter object() {
        return delegate.object();
    }

    public void pop(Mode c) {
        delegate.pop(c);
    }

    public void push(Mode c) {
        delegate.push(c);
    }

    public JSONWriter value(boolean b) {
        pathStack.pop();
        if(mode == Mode.ARRAY) {
            pathStack.push(new IndexElement(((IndexElement)pathStack.peek()).index));
        }
        return delegate.value(b);
    }

    public JSONWriter value(double d) {
        pathStack.pop();
        return delegate.value(d);
    }

    public JSONWriter value(long l) {
        pathStack.pop();
        return delegate.value(l);
    }

    public JSONWriter value(Object o) {
        pathStack.pop();
        return delegate.value(o);
    }

    private class PathElement {

    }
    private class PropertyElement extends PathElement {
        private String property;

        private PropertyElement(String property) {
            this.property = property;
        }

        public String toString() {
            return "." + property;
        }
    }

    private class IndexElement extends PathElement {
        private int index;

        private IndexElement(int index) {
            this.index = index;
        }

        public String toString() {
            return "["+index+"]";
        }
    }

}
