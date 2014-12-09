/*
 * Copyright 2014 the original author or authors.
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
package grails.artefact.controller.support

import static org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod.*
import grails.async.Promise
import grails.converters.JSON
import grails.io.IOUtils
import grails.plugins.GrailsPlugin
import grails.plugins.GrailsPluginManager
import grails.util.GrailsStringUtils
import grails.util.GrailsWebUtil
import grails.web.JSONBuilder
import grails.web.http.HttpHeaders
import grails.web.mime.MimeType
import grails.web.mime.MimeUtility
import grails.web.util.GrailsApplicationAttributes
import groovy.text.Template
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods
import org.grails.io.support.GrailsResourceUtils
import org.grails.io.support.SpringIOUtils
import org.grails.web.converters.Converter
import org.grails.web.pages.GroovyPageTemplate
import org.grails.web.servlet.mvc.ActionResultTransformer
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.servlet.mvc.exceptions.ControllerExecutionException
import org.grails.web.servlet.view.GroovyPageView
import org.grails.web.sitemesh.GrailsLayoutDecoratorMapper
import org.grails.web.sitemesh.GrailsLayoutView
import org.grails.web.sitemesh.GroovyPageLayoutFinder
import org.grails.web.support.ResourceAwareTemplateEngine
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View

/**
 * 
 * @author Jeff Brown
 * @since 3.0
 */
class RenderHelper {
    
    private Collection<ActionResultTransformer> actionResultTransformers
    private MimeUtility mimeUtility
    
    private boolean applyContentType(HttpServletResponse response, Map argMap, Object renderArgument) {
        applyContentType response, argMap, renderArgument, true
    }

    private boolean applyContentType(HttpServletResponse response, Map argMap, Object renderArgument, boolean useDefault) {
        boolean contentTypeIsDefault = true
        String contentType = resolveContentTypeBySourceType(renderArgument, useDefault ? TEXT_HTML : null)
        String encoding = DEFAULT_ENCODING
        if (argMap != null) {
            if(argMap.containsKey(ARGUMENT_CONTENT_TYPE)) {
                contentType = argMap.get(ARGUMENT_CONTENT_TYPE).toString()
                contentTypeIsDefault = false
            }
            if(argMap.containsKey(ARGUMENT_ENCODING)) {
                encoding = argMap.get(ARGUMENT_ENCODING).toString()
                contentTypeIsDefault = false
            }
        }
        if(contentType != null) {
            setContentType response, contentType, encoding, contentTypeIsDefault
            return true
        }
        false
    }

    private void renderAView(GrailsWebRequest webRequest, Map argMap, Object target, GroovyObject controller) {
        String viewName = argMap.get(ARGUMENT_VIEW).toString()
        String viewUri = webRequest.getAttributes().getNoSuffixViewURI((GroovyObject) target, viewName)
        String contextPath = getContextPath(webRequest, argMap)
        if(contextPath != null) {
            viewUri = contextPath + viewUri
        }
        Object modelObject = argMap.get(ARGUMENT_MODEL)
        if (modelObject != null) {
            modelObject = argMap.get(ARGUMENT_MODEL)
            boolean isPromise = modelObject instanceof Promise
            Collection<ActionResultTransformer> resultTransformers = getActionResultTransformers(webRequest)
            for (ActionResultTransformer resultTransformer : resultTransformers) {
                modelObject = resultTransformer.transformActionResult(webRequest,viewUri, modelObject)
            }
            if (isPromise) return
        }

        applyContentType webRequest.getCurrentResponse(), argMap, null

        Map model
        if (modelObject instanceof Map) {
            model = (Map) modelObject
        }
        else {
            model = [:]
        }

        controller.setProperty ControllerDynamicMethods.MODEL_AND_VIEW_PROPERTY, new ModelAndView(viewUri, model)
    }

    private boolean renderMarkup(Closure closure, HttpServletResponse response) {
        StreamingMarkupBuilder b = new StreamingMarkupBuilder()
        b.setEncoding(response.getCharacterEncoding())
        Writable markup = (Writable)b.bind(closure)
        renderWritable(markup, response)
    }

    private boolean renderText(CharSequence text, HttpServletResponse response) {
        try {
            PrintWriter writer = response.getWriter()
            return renderText(text, writer)
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e)
        }
    }

    private boolean renderText(CharSequence text, Writer writer) {
        try {
            if (writer instanceof PrintWriter) {
                ((PrintWriter)writer).print(text)
            }
            else {
                writer.write(text.toString())
            }
            writer.flush()
            return false
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e)
        }
    }

    private boolean isJSONResponse(HttpServletResponse response) {
        String contentType = response.getContentType()
        return contentType != null && (contentType.indexOf("application/json") > -1 ||
        contentType.indexOf("text/json") > -1)
    }

    private boolean renderJSON(Closure callable, HttpServletResponse response) {
        boolean renderView = true
        JSONBuilder builder = new JSONBuilder()
        JSON json = builder.build(callable)
        json.render response
        renderView = false
        renderView
    }

    private String resolveContentTypeBySourceType(final Object renderArgument, String defaultEncoding) {
        renderArgument instanceof GPathResult ? APPLICATION_XML : defaultEncoding
    }


    private boolean renderJSON(JSONElement object, HttpServletResponse response) {
        response.setContentType GrailsWebUtil.getContentType("application/json", DEFAULT_ENCODING)
        renderWritable object, response
    }

    private boolean detectContentTypeFromFileName(GrailsWebRequest webRequest, HttpServletResponse response, Map argMap, String fileName) {
        MimeUtility mimeUtility = lookupMimeUtility(webRequest);
        if (mimeUtility != null) {
            MimeType mimeType = mimeUtility.getMimeTypeForExtension(GrailsStringUtils.getFilenameExtension(fileName))
            if (mimeType != null) {
                String contentType = mimeType.getName()
                Object encodingObj = argMap.get(ARGUMENT_ENCODING);
                String encoding = encodingObj != null ? encodingObj.toString() : DEFAULT_ENCODING
                setContentType response, contentType, encoding
                return true;
            }
        }
        false
    }
    
    private boolean renderTemplate(Object target, GroovyObject controller, GrailsWebRequest webRequest,
            Map argMap, String explicitSiteMeshLayout) {
        boolean renderView
        boolean hasModel = argMap.containsKey(ARGUMENT_MODEL)
        def modelObject
        if(hasModel) {
            modelObject = argMap.get(ARGUMENT_MODEL)
        }
        String templateName = argMap.get(ARGUMENT_TEMPLATE).toString()
        String contextPath = getContextPath(webRequest, argMap)

        String var
        if (argMap.containsKey(ARGUMENT_VAR)) {
            var = String.valueOf(argMap.get(ARGUMENT_VAR))
        }

        // get the template uri
        String templateUri = webRequest.getAttributes().getTemplateURI(controller, templateName)

        // retrieve gsp engine
        ResourceAwareTemplateEngine engine = webRequest.getAttributes().getPagesTemplateEngine()
        try {
            Template t = engine.createTemplateForUri([
                GrailsResourceUtils.appendPiecesForUri(contextPath, templateUri),
                GrailsResourceUtils.appendPiecesForUri(contextPath, "/grails-app/views/", templateUri)] as String[]);

            if (t == null) {
                throw new ControllerExecutionException("Unable to load template for uri [" +
                templateUri + "]. Template not found.")
            }

            if (t instanceof GroovyPageTemplate) {
                ((GroovyPageTemplate)t).setAllowSettingContentType(true)
            }

            GroovyPageView gspView = new GroovyPageView()
            gspView.setTemplate t
            try {
                gspView.afterPropertiesSet()
            } catch (Exception e) {
                throw new RuntimeException("Problem initializing view", e)
            }

            View view = gspView
            boolean renderWithLayout = (explicitSiteMeshLayout != null || webRequest.getCurrentRequest().getAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE) != null)
            if(renderWithLayout) {
                applySiteMeshLayout webRequest.getCurrentRequest(), false, explicitSiteMeshLayout
                try {
                    GroovyPageLayoutFinder groovyPageLayoutFinder = webRequest.getApplicationContext().getBean("groovyPageLayoutFinder", GroovyPageLayoutFinder.class)
                    view = new GrailsLayoutView(groovyPageLayoutFinder, gspView)
                } catch (NoSuchBeanDefinitionException e) {
                    // ignore
                }
            }

            Map binding = [:]

            if (argMap.containsKey(ARGUMENT_BEAN)) {
                Object bean = argMap.get(ARGUMENT_BEAN)
                if (hasModel) {
                    if (modelObject instanceof Map) {
                        setTemplateModel(webRequest, binding, (Map) modelObject)
                    }
                }
                renderTemplateForBean(webRequest, view, binding, bean, var)
            }
            else if (argMap.containsKey(ARGUMENT_COLLECTION)) {
                Object colObject = argMap.get(ARGUMENT_COLLECTION)
                if (hasModel) {
                    if (modelObject instanceof Map) {
                        setTemplateModel webRequest, binding, (Map)modelObject
                    }
                }
                renderTemplateForCollection webRequest, view, binding, colObject, var
            }
            else if (hasModel) {
                if (modelObject instanceof Map) {
                    setTemplateModel webRequest, binding, (Map)modelObject
                }
                renderViewForTemplate webRequest, view, binding
            }
            else {
                renderViewForTemplate webRequest, view, binding
            }
            renderView = false
        }
        catch (GroovyRuntimeException gre) {
            throw new ControllerExecutionException("Error rendering template [" + templateName + "]: " + gre.getMessage(), gre)
        }
        catch (IOException ioex) {
            throw new ControllerExecutionException("I/O error executing render method for arguments [" + argMap + "]: " + ioex.getMessage(), ioex)
        }
        renderView
    }

    private void renderTemplateForBean(GrailsWebRequest webRequest, View view, Map binding, Object bean, String varName) throws IOException {
        if (GrailsStringUtils.isBlank(varName)) {
            binding.put DEFAULT_ARGUMENT, bean
        }
        else {
            binding.put varName, bean
        }
        renderViewForTemplate webRequest, view, binding
    }

    private boolean renderWritable(Writable writable, HttpServletResponse response) {
        try {
            PrintWriter writer = response.getWriter()
            writable.writeTo writer
            writer.flush()
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e)
        }
        false
    }
    
    private void renderTemplateForCollection(GrailsWebRequest webRequest, View view, Map binding, Object colObject, String var) throws IOException {
        if (colObject instanceof Iterable) {
            Iterable c = (Iterable) colObject
            for (Object o : c) {
                if (GrailsStringUtils.isBlank(var)) {
                    binding.put DEFAULT_ARGUMENT, o
                }
                else {
                    binding.put var, o
                }
                renderViewForTemplate webRequest, view, binding
            }
        }
        else {
            if (GrailsStringUtils.isBlank(var)) {
                binding.put DEFAULT_ARGUMENT, colObject
            }
            else {
                binding.put var, colObject
            }

            renderViewForTemplate webRequest, view, binding
        }
    }

    private void setContentType(HttpServletResponse response, String contentType, String encoding) {
        setContentType response, contentType, encoding, false
    }

    private void setContentType(HttpServletResponse response, String contentType, String encoding, boolean contentTypeIsDefault) {
        if (!contentTypeIsDefault || response.getContentType()==null) {
            response.setContentType GrailsWebUtil.getContentType(contentType, encoding)
        }
    }

    private boolean invokeRenderObject(Object object, Writer out) {
        boolean renderView
        try {
            out.write object.inspect()
            renderView = false
        }
        catch (IOException e) {
            throw new ControllerExecutionException("I/O error obtaining response writer: " + e.getMessage(), e)
        }
        renderView
    }
    
    private void setTemplateModel(GrailsWebRequest webRequest, Map binding, Map modelObject) {
        Map modelMap = modelObject
        webRequest.setAttribute GrailsApplicationAttributes.TEMPLATE_MODEL, modelMap, RequestAttributes.SCOPE_REQUEST
        binding.putAll modelMap
    }

    private String getContextPath(GrailsWebRequest webRequest, Map argMap) {
        def cp = argMap.get(ARGUMENT_CONTEXTPATH)
        String contextPath = (cp != null ? cp.toString() : "")

        Object pluginName = argMap.get(ARGUMENT_PLUGIN)
        if (pluginName != null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext()
            GrailsPluginManager pluginManager = (GrailsPluginManager) applicationContext.getBean(GrailsPluginManager.BEAN_NAME)
            GrailsPlugin plugin = pluginManager.getGrailsPlugin(pluginName.toString())
            if (plugin != null && !plugin.isBasePlugin()) contextPath = plugin.getPluginPath()
        }
        contextPath
    }
    private void applySiteMeshLayout(HttpServletRequest request, boolean renderView, String explicitSiteMeshLayout) {
        if(explicitSiteMeshLayout == null && request.getAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE) != null) {
            // layout has been set already
            return
        }
        String siteMeshLayout = explicitSiteMeshLayout != null ? explicitSiteMeshLayout : (renderView ? null : GrailsLayoutDecoratorMapper.NONE_LAYOUT)
        if(siteMeshLayout != null) {
            request.setAttribute GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE, siteMeshLayout
        }
    }

    private boolean renderConverter(Converter<?> converter, HttpServletResponse response) {
        converter.render response
        false
    }

    private MimeUtility lookupMimeUtility(GrailsWebRequest webRequest) {
        if (mimeUtility == null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext()
            if (applicationContext != null) {
                mimeUtility = applicationContext.getBean("grailsMimeUtility", MimeUtility.class)
            }
        }
        mimeUtility
    }

    private void renderViewForTemplate(GrailsWebRequest webRequest, View view, Map binding) {
        try {
            view.render binding, webRequest.getCurrentRequest(), webRequest.getResponse()
        }
        catch (Exception e) {
            throw new ControllerExecutionException(e.getMessage(), e)
        }
    }

    private Collection<ActionResultTransformer> getActionResultTransformers(GrailsWebRequest webRequest) {
        if (actionResultTransformers == null) {

            ApplicationContext applicationContext = webRequest.getApplicationContext()
            if (applicationContext != null) {
                actionResultTransformers = applicationContext.getBeansOfType(ActionResultTransformer.class).values()
            }
            if (actionResultTransformers == null) {
                actionResultTransformers = Collections.emptyList()
            }
        }

        actionResultTransformers
    }
    
    public void invokeRender(target, Object... arguments) {
        if (arguments.length == 0) {
            throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments)
        }

        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes()
        HttpServletResponse response = webRequest.getCurrentResponse()

        boolean renderView = true
        GroovyObject controller = target

        String explicitSiteMeshLayout

        final renderArgument = arguments[0]
        if (renderArgument instanceof Converter<?>) {
            renderView = renderConverter(renderArgument, response)
        } else if (renderArgument instanceof Writable) {
            applyContentType response, null, renderArgument
            renderView = renderWritable(renderArgument, response)
        } else if (renderArgument instanceof CharSequence) {
            applyContentType response, null, renderArgument
            renderView = renderText(renderArgument, response)
        }
        else {
            final renderObject = arguments[arguments.length - 1]
            if (renderArgument instanceof Closure) {
                setContentType response, TEXT_HTML, DEFAULT_ENCODING, true
                renderView = renderMarkup(renderArgument, response)
            }
            else if (renderArgument instanceof Map) {
                Map argMap = (Map)renderArgument

                if (argMap.containsKey(ARGUMENT_LAYOUT)) {
                    explicitSiteMeshLayout = String.valueOf(argMap.get(ARGUMENT_LAYOUT))
                }

                boolean statusSet = false
                if (argMap.containsKey(ARGUMENT_STATUS)) {
                    def statusObj = argMap.get(ARGUMENT_STATUS)
                    if (statusObj != null) {
                        try {
                            final int statusCode = statusObj instanceof Number ? ((Number)statusObj).intValue() : Integer.parseInt(statusObj.toString())
                            response.setStatus statusCode
                            statusSet = true
                        }
                        catch (NumberFormatException e) {
                            throw new ControllerExecutionException(
                            "Argument [status] of method [render] must be a valid integer.")
                        }
                    }
                }

                if (renderObject instanceof Writable) {
                    applyContentType response, argMap, renderObject
                    renderView = renderWritable(renderObject, response)
                }
                else if (renderObject instanceof Closure) {
                    applyContentType response, argMap, renderObject
                    if (BUILDER_TYPE_JSON.equals(argMap.get(ARGUMENT_BUILDER)) || isJSONResponse(response)) {
                        renderView = renderJSON(renderObject, response)
                    }
                    else {
                        renderView = renderMarkup(renderObject, response)
                    }
                }
                else if (renderObject instanceof CharSequence) {
                    applyContentType response, argMap, renderObject
                    renderView = renderText(renderObject, response)
                }
                else if (argMap.containsKey(ARGUMENT_TEXT)) {
                    def textArg = argMap.get(ARGUMENT_TEXT)
                    applyContentType response, argMap, textArg
                    if (textArg instanceof Writable) {
                        renderView = renderWritable(textArg, response)
                    } else {
                        CharSequence text = (textArg instanceof CharSequence) ? ((CharSequence)textArg) : textArg.toString()
                        renderView = renderText(text, response)
                    }
                }
                else if (argMap.containsKey(ARGUMENT_VIEW)) {
                    renderAView webRequest, argMap, target, controller
                }
                else if (argMap.containsKey(ARGUMENT_TEMPLATE)) {
                    applyContentType response, argMap, null, false
                    renderView = renderTemplate(target, controller, webRequest, argMap, explicitSiteMeshLayout)
                }
                else if (argMap.containsKey(ARGUMENT_FILE)) {
                    renderView = false

                    def o = argMap.get(ARGUMENT_FILE)
                    def fnO = argMap.get(ARGUMENT_FILE_NAME)
                    String fileName = fnO != null ? fnO.toString() : ((o instanceof File) ? ((File)o).getName(): null )
                    if (o != null) {
                        boolean hasContentType = applyContentType(response, argMap, null, false)
                        if (fileName != null) {
                            if(!hasContentType) {
                                hasContentType = detectContentTypeFromFileName(webRequest, response, argMap, fileName)
                            }
                            if (fnO != null) {
                                response.setHeader HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_HEADER_PREFIX + fileName
                            }
                        }
                        if (!hasContentType) {
                            throw new ControllerExecutionException(
                            "Argument [file] of render method specified without valid [contentType] argument")
                        }

                        InputStream input
                        try {
                            if (o instanceof File) {
                                input = IOUtils.openStream(o)
                            }
                            else if (o instanceof InputStream) {
                                input = (InputStream)o
                            }
                            else if (o instanceof byte[]) {
                                input = new ByteArrayInputStream((byte[])o)
                            }
                            else {
                                input = IOUtils.openStream(new File(o.toString()))
                            }
                            SpringIOUtils.copy input, response.getOutputStream()
                        } catch (IOException e) {
                            throw new ControllerExecutionException(
                            "I/O error copying file to response: " + e.getMessage(), e)
                        }
                        finally {
                            if (input != null) {
                                try {
                                    input.close()
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
                else if (statusSet) {
                    // GRAILS-6711 nothing to render, just setting status code, so don't render the map
                    renderView = false
                }
                else {
                    Object object = renderArgument
                    if (object instanceof JSONElement) {
                        renderView = renderJSON(object, response)
                    }
                    else{
                        try {
                            renderView = invokeRenderObject(object, response.getWriter())
                        }
                        catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
            else {
                throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments);
            }
        }
        applySiteMeshLayout webRequest.getCurrentRequest(), renderView, explicitSiteMeshLayout
        webRequest.setRenderView renderView
        null
    }
}
