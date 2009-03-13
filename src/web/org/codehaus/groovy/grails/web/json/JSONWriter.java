package org.codehaus.groovy.grails.web.json;

import static org.codehaus.groovy.grails.web.json.JSONWriter.Mode.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;
/*
Copyright (c) 2006 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * JSONWriter provides a quick and convenient way of producing JSON text.
 * The texts produced strictly conform to JSON syntax rules. No whitespace is
 * added, so the results are ready for transmission or storage. Each instance of
 * JSONWriter can produce one JSON text.
 * <p/>
 * A JSONWriter instance provides a <code>value</code> method for appending
 * values to the
 * text, and a <code>key</code>
 * method for adding keys before values in objects. There are <code>array</code>
 * and <code>endArray</code> methods that make and bound array values, and
 * <code>object</code> and <code>endObject</code> methods which make and bound
 * object values. All of these methods return the JSONWriter instance,
 * permitting a cascade style. For example, <pre>
 * new JSONWriter(myWriter)
 *     .object()
 *         .key("JSON")
 *         .value("Hello, World!")
 *     .endObject();</pre> which writes <pre>
 * {"JSON":"Hello, World!"}</pre>
 * <p/>
 * The first method called must be <code>array</code> or <code>object</code>.
 * There are no methods for adding commas or colons. JSONWriter adds them for
 * you. Objects and arrays can be nested up to 20 levels deep.
 * <p/>
 * This can sometimes be easier than using a JSONObject to build a string.
 *
 * @author JSON.org
 * @version 2
 */
public class JSONWriter {

    /**
     * The comma flag determines if a comma should be output before the next
     * value.
     */
    protected boolean comma;

    /**
     * The current mode. Values:
     */
    protected Mode mode;

    /**
     * The Mode stack.
     */
    private Stack<Mode> stack = new Stack<Mode>();

    /**
     * The writer that will receive the output.
     */
    protected Writer writer;

    /**
     * Make a fresh JSONWriter. It can be used to build one JSON text.
     */
    public JSONWriter(Writer w) {
        this.comma = false;
        this.mode = INIT;
        this.writer = w;
    }

    /**
     * Append a value.
     * @param s A string value.
     * @return this
     */
    protected JSONWriter append(String s) {
        if (s == null) {
            throw new JSONException("Null pointer");
        }
        if (this.mode == OBJECT || this.mode == ARRAY) {
            try {
                if (this.comma && this.mode == ARRAY) {
                    this.comma();
                }
                this.writer.write(s);
            } catch (IOException e) {
                throw new JSONException(e);
            }
            if (this.mode == OBJECT) {
                this.mode = KEY;
            }
            this.comma = true;
            return this;
        }
        throw new JSONException("Value out of sequence.");
    }

    protected void comma() {
        try {
            this.writer.write(',');
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }

    /**
     * Begin appending a new array. All values until the balancing
     * <code>endArray</code> will be appended to this array. The
     * <code>endArray</code> method must be called to mark the array's end.
     *
     * @return this
     */
    public JSONWriter array() {
        if (this.mode == INIT || this.mode == OBJECT || this.mode == ARRAY) {
            this.push(ARRAY);
            this.append("[");
            this.comma = false;
            return this;
        } else throw new JSONException("Misplaced array.");
    }

    /**
     * End something.
     *
     * @param m Mode
     * @param c Closing character
     * @return this
     */
    protected JSONWriter end(Mode m, char c) {
        if (this.mode != m) {
            throw new JSONException(m == OBJECT ? "Misplaced endObject." :
                    "Misplaced endArray.");
        }
        this.pop(m);
        try {
            this.writer.write(c);
        } catch (IOException e) {
            throw new JSONException(e);
        }
        this.comma = true;
        return this;
    }

    /**
     * End an array. This method most be called to balance calls to
     * <code>array</code>.
     *
     * @return this
     */
    public JSONWriter endArray() {
        return this.end(ARRAY, ']');
    }

    /**
     * End an object. This method most be called to balance calls to
     * <code>object</code>.
     *
     * @return this
     */
    public JSONWriter endObject() {
        return this.end(KEY, '}');
    }

    /**
     * Append a key. The key will be associated with the next value. In an
     * object, every value must be preceded by a key.
     *
     * @param s A key string.
     * @return this
     */
    public JSONWriter key(String s) {
        if (s == null) {
            throw new JSONException("Null key.");
        }
        if (this.mode == KEY) {
            try {
                if (this.comma) {
                    this.comma();
                }
                this.writer.write(JSONObject.quote(s));
                this.writer.write(':');
                this.comma = false;
                this.mode = OBJECT;
                return this;
            } catch (IOException e) {
                throw new JSONException(e);
            }
        }
        throw new JSONException("Misplaced key.");
    }


    /**
     * Begin appending a new object. All keys and values until the balancing
     * <code>endObject</code> will be appended to this object. The
     * <code>endObject</code> method must be called to mark the object's end.
     *
     * @return this
     */
    public JSONWriter object() {
        if (this.mode == INIT) {
            this.mode = OBJECT;
        }
        if (this.mode == OBJECT || this.mode == ARRAY) {
            this.append("{");
            this.push(KEY);
            this.comma = false;
            return this;
        }
        throw new JSONException("Misplaced object.");

    }


    /**
     * Pop an array or object scope.
     *
     * @param c The scope to close.
     */
    protected void pop(Mode c) {
        if (this.stack.size() == 0 || this.stack.pop() != c) {
            throw new JSONException("Nesting error.");
        }
        if (this.stack.size() > 0)
            this.mode = this.stack.peek();
        else
            this.mode = DONE;

    }

    /**
     * Push an array or object scope.
     *
     * @param c The scope to open.
     */
    protected void push(Mode c) {
        this.stack.push(c);
        this.mode = c;
    }


    /**
     * Append either the value <code>true</code> or the value
     * <code>false</code>.
     *
     * @param b A boolean.
     * @return this
     */
    public JSONWriter value(boolean b) {
        return this.append(b ? "true" : "false");
    }

    /**
     * Append a double value.
     *
     * @param d A double.
     * @return this
     */
    public JSONWriter value(double d) {
        return this.value(new Double(d));
    }

    /**
     * Append a long value.
     *
     * @param l A long.
     * @return this
     */
    public JSONWriter value(long l) {
        return this.append(Long.toString(l));
    }


    /**
     * Append an object value.
     *
     * @param o The object to append. It can be null, or a Boolean, Number,
     *          String, JSONObject, or JSONArray.
     * @return this
     */
    public JSONWriter value(Object o) {
        return this.append(JSONObject.valueToString(o));
    }

    /**
     * Enumeration of the possible modes of the JSONWriter
     */
    protected enum Mode {
        INIT,
        OBJECT,
        ARRAY,
        KEY,
        DONE
    }

}
