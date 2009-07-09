package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.springframework.util.Assert
import org.codehaus.groovy.grails.web.pages.FastStringWriter

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Apr 30, 2008
 */
class JspTagLibImpl implements JspTagLib {

    private uri
    private tags = [:]

    JspTagLibImpl(String uri, Map tagClasses) {
        Assert.notNull uri, "The URI of the tag library must be specified!"
        this.uri = uri
        for(t in tagClasses) {
            this.tags[t.key] = new JspTagImpl(t.value)
        }
    }

    public JspTag getTag(String name) {
        return tags[name]
    }

    public String getURI() {
        return this.uri
    }

    /**
     * Overrides invoke method so tags can be invoked as methods
     */
    public Object invokeMethod(String name, Object args) {
        JspTag tag = getTag(name)

        if(tag) {
            args = args ?: [[:]] // default to an list with an empty map inside
            def sw = new FastStringWriter()

            Map attrs = args[0] instanceof Map ? args[0] : [:]
            def body = args[0] instanceof Closure ? args[0] : null
            if(args.size() > 1) body = args[1] instanceof Closure ? args[1] : null
            if(body == null && args.size() > 1) {
                body = { args[1] }
            }
            else{
                body = {}
            }

            tag.doTag sw,attrs,body

            return sw.toString()            
        }
        else {
            return super.invokeMethod(name, args);
        }
    }


}