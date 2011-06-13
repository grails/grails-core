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
package org.codehaus.groovy.grails.web.metaclass;

import grails.converters.JSON;
import grails.util.GrailsWebUtil;
import grails.web.JSONBuilder;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MissingMethodException;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.xml.StreamingMarkupBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.pages.GSPResponseWriter;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;

/**
 * Allows rendering of text, views, and templates to the response
 *
 * @author Graeme Rocher
 * @since 0.2
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class RenderDynamicMethod extends AbstractDynamicMethodInvocation {
    public static final String METHOD_SIGNATURE = "render";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^' + METHOD_SIGNATURE + '$');

    public static final String ARGUMENT_TEXT = "text";
    public static final String ARGUMENT_STATUS = "status";
    public static final String ARGUMENT_CONTENT_TYPE = "contentType";
    public static final String ARGUMENT_ENCODING = "encoding";
    public static final String ARGUMENT_VIEW = "view";
    public static final String ARGUMENT_MODEL = "model";
    public static final String ARGUMENT_TEMPLATE = "template";
    public static final String ARGUMENT_CONTEXTPATH = "contextPath";
    public static final String ARGUMENT_BEAN = "bean";
    public static final String ARGUMENT_COLLECTION = "collection";
    public static final String ARGUMENT_BUILDER = "builder";
    public static final String ARGUMENT_VAR = "var";
    private static final String DEFAULT_ARGUMENT = "it";
    private static final String BUILDER_TYPE_JSON = "json";

    private static final String TEXT_HTML = "text/html";
    private String gspEncoding = DEFAULT_ENCODING;
    private static final String DEFAULT_ENCODING = "utf-8";
    private Object ARGUMENT_PLUGIN = "plugin";

    public RenderDynamicMethod() {
        super(METHOD_PATTERN);
    }

    public void setGspEncoding(String gspEncoding) {
        this.gspEncoding = gspEncoding;
    }

    @Override
    public Object invoke(Object target, String methodName, Object[] arguments) {
        if (arguments.length == 0) {
            throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments);
        }

        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        HttpServletResponse response = webRequest.getCurrentResponse();

        boolean renderView = true;
        GroovyObject controller = (GroovyObject) target;
        if (arguments[0] instanceof CharSequence) {
            setContentType(response, TEXT_HTML, DEFAULT_ENCODING,true);
            CharSequence text = (CharSequence)arguments[0];
            renderView = renderText(text, response);
        }
        else if (arguments[0] instanceof Closure) {
            setContentType(response, TEXT_HTML, gspEncoding, true);
            Closure closure = (Closure) arguments[arguments.length - 1];
            renderView = renderMarkup(closure, response);
        }
        else if (arguments[0] instanceof Map) {
            Map argMap = (Map) arguments[0];
            Writer out;
            if (argMap.containsKey(ARGUMENT_CONTENT_TYPE) && argMap.containsKey(ARGUMENT_ENCODING)) {
                String contentType = argMap.get(ARGUMENT_CONTENT_TYPE).toString();
                String encoding = argMap.get(ARGUMENT_ENCODING).toString();
                setContentType(response, contentType, encoding);
                out = GSPResponseWriter.getInstance(response);
            }
            else if (argMap.containsKey(ARGUMENT_CONTENT_TYPE)) {
                setContentType(response, argMap.get(ARGUMENT_CONTENT_TYPE).toString(), DEFAULT_ENCODING);
                out = GSPResponseWriter.getInstance(response);
            }
            else {
                setContentType(response, TEXT_HTML, DEFAULT_ENCODING, true);
                out = GSPResponseWriter.getInstance(response);
            }

            boolean statusSet = false;
            if (argMap.containsKey(ARGUMENT_STATUS)) {
                Object statusObj = argMap.get(ARGUMENT_STATUS);
                if (statusObj != null) {
                    try {
                        response.setStatus(Integer.parseInt(statusObj.toString()));
                        statusSet = true;
                    }
                    catch (NumberFormatException e) {
                        throw new ControllerExecutionException(
                                "Argument [status] of method [render] must be a valid integer.");
                    }
                }
            }

            webRequest.setOut(out);

            if (arguments[arguments.length - 1] instanceof Closure) {
                Closure callable = (Closure) arguments[arguments.length - 1];
                if (BUILDER_TYPE_JSON.equals(argMap.get(ARGUMENT_BUILDER)) || isJSONResponse(response)) {
                    renderView = renderJSON(callable, response);
                }
                else {
                    renderView = renderMarkup(callable, response);
                }
            }
            else if (arguments[arguments.length - 1] instanceof CharSequence) {
                CharSequence text = (CharSequence) arguments[arguments.length - 1];
                renderView = renderText(text, out);
            }
            else if (argMap.containsKey(ARGUMENT_TEXT)) {
                Object textArg = argMap.get(ARGUMENT_TEXT);
                CharSequence text = (textArg instanceof CharSequence) ? ((CharSequence)textArg) : textArg.toString();
                renderView = renderText(text, out);
            }
            else if (argMap.containsKey(ARGUMENT_VIEW)) {
                renderView(webRequest, argMap, target, controller);
            }
            else if (argMap.containsKey(ARGUMENT_TEMPLATE)) {
                renderView = renderTemplate(target, controller, webRequest, argMap, out);
            }
            else if (statusSet) {
                // GRAILS-6711 nothing to render, just setting status code, so don't render the map
                renderView = false;
            }
            else {
                Object object = arguments[0];
                renderView = renderObject(object, out);
            }
            try {
                if (!renderView) {
                    out.flush();
                }
            }
            catch (IOException e) {
                throw new ControllerExecutionException("I/O error executing render method for arguments [" +
                        argMap + "]: " + e.getMessage(), e);
            }
        }
        else {
            throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments);
        }
        webRequest.setRenderView(renderView);
        return null;
    }

    private boolean renderTemplate(Object target, GroovyObject controller, GrailsWebRequest webRequest,
            Map argMap, Writer out) {

        boolean renderView;
        String templateName = argMap.get(ARGUMENT_TEMPLATE).toString();
        String contextPath = getContextPath(webRequest, argMap);

        String var = null;
        if (argMap.containsKey(ARGUMENT_VAR)) {
            var = String.valueOf(argMap.get(ARGUMENT_VAR));
        }

        // get the template uri
        String templateUri = webRequest.getAttributes().getTemplateURI(controller, templateName);

        // retrieve gsp engine
        GroovyPagesTemplateEngine engine = webRequest.getAttributes().getPagesTemplateEngine();
        try {
            Template t = engine.createTemplateForUri(new String[]{
                    GrailsResourceUtils.appendPiecesForUri(contextPath, templateUri),
                    GrailsResourceUtils.appendPiecesForUri(contextPath, "/grails-app/views/", templateUri)});

            if (t == null) {
                throw new ControllerExecutionException("Unable to load template for uri [" +
                        templateUri + "]. Template not found.");
            }
            Map binding = new HashMap();

            if (argMap.containsKey(ARGUMENT_BEAN)) {
                Object bean = argMap.get(ARGUMENT_BEAN);
                if (argMap.containsKey(ARGUMENT_MODEL)) {
                    Object modelObject = argMap.get(ARGUMENT_MODEL);
                    if (modelObject instanceof Map) {
                        binding.putAll((Map) modelObject);
                    }
                }
                renderTemplateForBean(t, binding, bean, var, out);
            }
            else if (argMap.containsKey(ARGUMENT_COLLECTION)) {
                Object colObject = argMap.get(ARGUMENT_COLLECTION);
                if (argMap.containsKey(ARGUMENT_MODEL)) {
                    Object modelObject = argMap.get(ARGUMENT_MODEL);
                    if (modelObject instanceof Map) {
                        binding.putAll((Map) modelObject);
                    }
                }
                renderTemplateForCollection(t, binding, colObject, var, out);
            }
            else if (argMap.containsKey(ARGUMENT_MODEL)) {
                Object modelObject = argMap.get(ARGUMENT_MODEL);
                renderTemplateForModel(t, modelObject, target, out);
            }
            else {
                Writable w = t.make(new BeanMap(target));
                w.writeTo(out);
            }
            renderView = false;
        }
        catch (GroovyRuntimeException gre) {
            throw new ControllerExecutionException("Error rendering template [" + templateName + "]: " + gre.getMessage(), gre);
        }
        catch (IOException ioex) {
            throw new ControllerExecutionException("I/O error executing render method for arguments [" + argMap + "]: " + ioex.getMessage(), ioex);
        }
        return renderView;
    }

    private String getContextPath(GrailsWebRequest webRequest, Map argMap) {
        Object cp = argMap.get(ARGUMENT_CONTEXTPATH);
        String contextPath = (cp != null ? cp.toString() : "");

        Object pluginName = argMap.get(ARGUMENT_PLUGIN);
        if (pluginName != null) {
            ApplicationContext applicationContext = webRequest.getApplicationContext();
            GrailsPluginManager pluginManager = (GrailsPluginManager) applicationContext.getBean(GrailsPluginManager.BEAN_NAME);
            GrailsPlugin plugin = pluginManager.getGrailsPlugin(pluginName.toString());
            if (plugin != null && !plugin.isBasePlugin()) contextPath = plugin.getPluginPath();
        }
        return contextPath;
    }

    private void setContentType(HttpServletResponse response, String contentType, String encoding) {
        setContentType(response, contentType, encoding, false);
    }

    private void setContentType(HttpServletResponse response, String contentType, String encoding, boolean contentTypeIsDefault) {
        if (response.getContentType()==null || !contentTypeIsDefault) {
            response.setContentType(GrailsWebUtil.getContentType(contentType,encoding));
        }
    }

    private boolean renderObject(Object object, Writer out) {
        boolean renderView;
        try {
            out.write(DefaultGroovyMethods.inspect(object));
            renderView = false;
        }
        catch (IOException e) {
            throw new ControllerExecutionException("I/O error obtaining response writer: " + e.getMessage(), e);
        }
        return renderView;
    }

    private void renderTemplateForModel(Template template, Object modelObject, Object target, Writer out) throws IOException {
        if (modelObject instanceof Map) {
            template.make((Map) modelObject).writeTo(out);
        }
        else {
            template.make(new BeanMap(target)).writeTo(out);
        }
    }

    private void renderTemplateForCollection(Template template, Map binding, Object colObject, String var, Writer out) throws IOException {
        if (colObject instanceof Collection) {
            Collection c = (Collection) colObject;
            for (Object o : c) {
                if (StringUtils.isBlank(var)) {
                    binding.put(DEFAULT_ARGUMENT, o);
                }
                else {
                    binding.put(var, o);
                }
                template.make(binding).writeTo(out);
            }
        }
        else {
            if (StringUtils.isBlank(var)) {
                binding.put(DEFAULT_ARGUMENT, colObject);
            }
            else {
                binding.put(var, colObject);
            }

            template.make(binding).writeTo(out);
        }
    }

    private void renderTemplateForBean(Template template, Map binding, Object bean, String varName, Writer out) throws IOException {
        if (StringUtils.isBlank(varName)) {
            binding.put(DEFAULT_ARGUMENT, bean);
        }
        else {
            binding.put(varName, bean);
        }
        template.make(binding).writeTo(out);
    }

    private void renderView(GrailsWebRequest webRequest, Map argMap, Object target, GroovyObject controller) {
        String viewName = argMap.get(ARGUMENT_VIEW).toString();
        Object modelObject = argMap.get(ARGUMENT_MODEL);

        String viewUri = webRequest.getAttributes().getNoSuffixViewURI((GroovyObject) target, viewName);
        Map model;
        if (modelObject instanceof Map) {
            model = (Map) modelObject;
        }
        else if (target instanceof GroovyObject) {
            model = new BeanMap(target);
        }
        else {
            model = new HashMap();
        }

        controller.setProperty(ControllerDynamicMethods.MODEL_AND_VIEW_PROPERTY, new ModelAndView(viewUri, model));
    }

    private boolean renderJSON(Closure callable, HttpServletResponse response) {
        boolean renderView = true;
        JSONBuilder builder = new JSONBuilder();
        JSON json = builder.build(callable);
        json.render(response);
        renderView = false;
        return renderView;
    }

    private boolean renderMarkup(Closure closure, HttpServletResponse response) {
        boolean renderView;
        StreamingMarkupBuilder b = new StreamingMarkupBuilder();
        Writable markup = (Writable) b.bind(closure);
        try {
            markup.writeTo(response.getWriter());
        }
        catch (IOException e) {
            throw new ControllerExecutionException("I/O error executing render method for arguments [" + closure + "]: " + e.getMessage(), e);
        }
        renderView = false;
        return renderView;
    }

    private boolean renderText(CharSequence text, HttpServletResponse response) {
        try {
            PrintWriter writer = response.getWriter();
            return renderText(text, writer);
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e);
        }
    }

    private boolean renderText(CharSequence text, Writer writer) {
        try {
            if (writer instanceof PrintWriter) {
                ((PrintWriter)writer).print(text);
            }
            else {
                writer.write(text.toString());
            }
            return false;
        }
        catch (IOException e) {
            throw new ControllerExecutionException(e.getMessage(), e);
        }
    }

    private boolean isJSONResponse(HttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType != null && (contentType.indexOf("application/json") > -1 ||
                contentType.indexOf("text/json") > -1);
    }
}
