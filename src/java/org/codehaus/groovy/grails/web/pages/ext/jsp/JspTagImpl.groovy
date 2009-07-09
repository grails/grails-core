/*
 * Copyright 2004-2005 the original author or authors.
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


package org.codehaus.groovy.grails.web.pages.ext.jsp

import javax.servlet.jsp.tagext.SimpleTag
import org.springframework.beans.BeanWrapperImpl
import javax.servlet.jsp.tagext.BodyTag
import javax.servlet.jsp.tagext.Tag
import javax.servlet.jsp.tagext.TagAdapter
import javax.servlet.jsp.tagext.IterationTag
import javax.servlet.jsp.tagext.BodyContent
import org.apache.commons.logging.LogFactory
import javax.servlet.jsp.tagext.TryCatchFinally
import javax.servlet.jsp.tagext.JspFragment
import javax.servlet.jsp.JspContext
import javax.servlet.jsp.JspWriter
import org.codehaus.groovy.grails.web.pages.FastStringWriter

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Apr 30, 2008
 */
class JspTagImpl implements JspTag {

    static final LOG = LogFactory.getLog(JspTagImpl)

    Class tagClass
    boolean tryCatchFinally
    boolean body
    boolean iteration

    public JspTagImpl(Class tagClass) {
        this.tagClass = tagClass;
        this.tryCatchFinally = TryCatchFinally.isAssignableFrom(tagClass)
        this.body = BodyTag.isAssignableFrom(tagClass)
        this.iteration = IterationTag.isAssignableFrom(tagClass)
    }

    public void doTag(Writer targetWriter, Map attributes) {
        doTag targetWriter,attributes,null
        
    }

    private createTagInstance() {
        tagClass.newInstance()
    }

    public void doTag(Writer targetWriter, Map attributes, Closure body) {
        def tag = createTagInstance()
        GroovyPagesPageContext pageContext = PageContextFactory.getCurrent();
        if(tag.metaClass.hasProperty(tag, "jspContext"))
            tag.jspContext = pageContext
        if(tag.metaClass.hasProperty(tag, "pageContext"))
            tag.pageContext = pageContext

        
        def parentTag = pageContext.peekTopTag(javax.servlet.jsp.tagext.JspTag.class)
        if (parentTag) {
            if(tag instanceof Tag && parentTag instanceof SimpleTag) {
                tag.parent = new TagAdapter(parentTag)
            } else {
                tag.parent = parentTag
            }
        }

        def tagBean = new BeanWrapperImpl(tag)

        for (entry in attributes) {
            if (tagBean.isWritableProperty(entry.key))
                tagBean.setPropertyValue entry.key, entry.value
        }

        if(tag instanceof SimpleTag) {
            handleSimpleTag(tag, attributes, pageContext, targetWriter, body)
        }
        else if(tag instanceof Tag) {
            withJspWriterDelegate pageContext, targetWriter, {

                try {
                    pageContext.pushTopTag tag
                    int state = tag.doStartTag()
                    BodyContent bodyContent
                    def out = pageContext.getOut()
                    if((state == Tag.EVAL_BODY_INCLUDE || state == IterationTag.EVAL_BODY_AGAIN) && body) {
                        if(state == BodyTag.EVAL_BODY_BUFFERED && isBody()) {
                            bodyContent = pageContext.pushBody()
                            out = bodyContent
                            tag.bodyContent = bodyContent
                            tag.doInitBody()
                        }
                        out << body.call()
                        if(isIterationTag()) {
                            state = tag.doAfterBody()

                            while(state != IterationTag.SKIP_BODY) {
                                out << body.call(); state = tag.doAfterBody()
                            }
                        }
                    }
                    if(bodyContent) {
                        pageContext.popBody()
                    }
                    state = tag.doEndTag()
                    if(state == Tag.SKIP_PAGE) {
                        LOG.warn "Tag ${tag.getClass().getName()} returned SKIP_PAGE which is not supported in GSP"
                    }
                }
                catch(Throwable t) {
                   if(isTryCatchFinally() && tag) {
                        tag?.doCatch(t)
                   }
                   else {
                       throw t
                   }
                }
                finally {
                    if(isTryCatchFinally()) {
                        tag?.doFinally()
                    }
                    pageContext.popTopTag()
                    tag?.release()
                }
            }
        }

    }

    void withJspWriterDelegate(GroovyPagesPageContext pageContext,Writer delegate, Closure callable) {
        pageContext.pushWriter new JspWriterDelegate(delegate);
        try {
            callable()
        }
        finally {
            pageContext.popWriter()
        }
    }

    protected def handleSimpleTag(SimpleTag tag, Map attributes,GroovyPagesPageContext pageContext,Writer targetWriter, Closure body) {
        withJspWriterDelegate pageContext, targetWriter, {
            if (body) {
                pageContext.pushTopTag tag
                try {
                    FastStringWriter buffer = new FastStringWriter()
                    JspWriter bufferedOut = new JspWriterDelegate(buffer)
                    pageContext.pushWriter bufferedOut
                    if(body) {
                        bufferedOut << body.call()
                        tag.setJspBody(new JspFragmentImpl(pageContext, buffer))
                    }
                }
                finally {
                    pageContext.popWriter()
                    pageContext.popTopTag()
                }
            }
            tag.doTag()
        }
    }

//----------------------------------------------------------------------------
// Workarounds for http://jira.codehaus.org/browse/GROOVY-2897
//
    public boolean isBodyTag() {
        return body
    }

    public boolean isIterationTag() {
        return iteration
    }

    public boolean isTryCatchFinallyTag() {
        return tryCatchFinally
    }

}

class JspFragmentImpl extends JspFragment {

    GroovyPagesPageContext pageContext
    FastStringWriter body
    
    public JspFragmentImpl(GroovyPagesPageContext pageContext, FastStringWriter body) {
        this.pageContext = pageContext
        this.body = body
    }
    
    public JspContext getJspContext() {
        return pageContext
    }
    
    public void invoke(Writer out) {
        out << body.toString()
    }

}
