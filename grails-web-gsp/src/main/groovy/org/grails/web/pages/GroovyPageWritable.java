/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.grails.web.pages;

import grails.util.Environment;
import groovy.lang.Binding;
import groovy.lang.Writable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.web.pages.exceptions.GroovyPagesException;
import grails.web.util.GrailsApplicationAttributes;
import org.grails.web.servlet.WrappedResponseHolder;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Writes itself to the specified writer, typically the response writer.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 0.5
 */
class GroovyPageWritable implements Writable {
    private static final Log LOG = LogFactory.getLog(GroovyPageWritable.class);
    private static final String ATTRIBUTE_NAME_DEBUG_TEMPLATES_ID_COUNTER = "org.codehaus.groovy.grails.web.pages.DEBUG_TEMPLATES_COUNTER";
    private static final String GSP_NONE_CODEC_NAME = "none";
    private HttpServletResponse response;
    private HttpServletRequest request;
    private GroovyPageMetaInfo metaInfo;
    private boolean showSource;
    private boolean debugTemplates;
    private AtomicInteger debugTemplatesIdCounter;
    private GrailsWebRequest webRequest;
    private boolean allowSettingContentType;

    @SuppressWarnings("rawtypes")
    private Map additionalBinding = new HashMap();
    private static final String GROOVY_SOURCE_CONTENT_TYPE = "text/plain";

    public GroovyPageWritable(GroovyPageMetaInfo metaInfo, boolean allowSettingContentType) {
        this.metaInfo = metaInfo;
        this.allowSettingContentType = allowSettingContentType;
        webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        if (webRequest != null) {
            request = webRequest.getCurrentRequest();
            HttpServletResponse wrapped = WrappedResponseHolder.getWrappedResponse();
            response = wrapped != null ? wrapped : webRequest.getCurrentResponse();
        }
        showSource = shouldShowGroovySource();
        debugTemplates = shouldDebugTemplates();
        if (debugTemplates) {
            debugTemplatesIdCounter=(AtomicInteger)request.getAttribute(ATTRIBUTE_NAME_DEBUG_TEMPLATES_ID_COUNTER);
            if (debugTemplatesIdCounter==null) {
                debugTemplatesIdCounter=new AtomicInteger(0);
                request.setAttribute(ATTRIBUTE_NAME_DEBUG_TEMPLATES_ID_COUNTER, debugTemplatesIdCounter);
            }
        }
    }

    private boolean shouldDebugTemplates() {
        return request != null && request.getParameter("debugTemplates") != null && Environment.getCurrent() == Environment.DEVELOPMENT;
    }

    private boolean shouldShowGroovySource() {
        return request != null && request.getParameter("showSource") != null &&
            (Environment.getCurrent() == Environment.DEVELOPMENT) &&
            metaInfo.getGroovySource() != null;
    }

    /**
     * This sets any additional variables that need to be placed in the Binding of the GSP page.
     *
     * @param binding The additional variables
     */
    @SuppressWarnings("rawtypes")
    public void setBinding(Map binding) {
        if (binding != null) {
            additionalBinding = binding;
        }
    }

    /**
     * Set to true if the generated source should be output instead
     * @param showSource True if source output should be output
     */
    public void setShowSource(boolean showSource) {
        this.showSource = showSource;
    }

    /**
     * Writes the template to the specified Writer
     *
     * @param out The Writer to write to, normally the HttpServletResponse
     * @return Returns the passed Writer
     * @throws IOException
     */
    public Writer writeTo(Writer out) throws IOException {
        try {
            return doWriteTo(out);
        } finally {
            doCleanUp(out);
        }
    }

    protected void doCleanUp(Writer out) {
        metaInfo.writeToFinished(out);
    }

    protected Writer doWriteTo(Writer out) throws IOException {
        if (showSource) {
            // Set it to TEXT
            response.setContentType(GROOVY_SOURCE_CONTENT_TYPE); // must come before response.getOutputStream()
            writeGroovySourceToResponse(metaInfo, out);
        }
        else {
            // Set it to HTML by default
            if (metaInfo.getCompilationException()!=null) {
                throw metaInfo.getCompilationException();
            }

            // Set up the script context
            GroovyPageBinding parentBinding = null;
            boolean hasRequest = request != null;
            boolean newParentCreated = false;

            if (hasRequest) {
                parentBinding = (GroovyPageBinding) request.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE);
                if (parentBinding == null) {
                    if (webRequest != null) {
                        parentBinding = new GroovyPageBinding(new GroovyPageRequestBinding(webRequest));
                        parentBinding.setRoot(true);
                        newParentCreated = true;
                    }
                }
            }

            if (allowSettingContentType && response != null) {
                // only try to set content type when evaluating top level GSP
                boolean contentTypeAlreadySet = response.isCommitted() || response.getContentType() != null;
                if (!contentTypeAlreadySet) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Writing response to ["+response.getClass()+"] with content type: " + metaInfo.getContentType());
                    }
                    response.setContentType(metaInfo.getContentType()); // must come before response.getWriter()
                }
            }

            GroovyPageBinding binding = createBinding(parentBinding);
            String previousGspCode = GSP_NONE_CODEC_NAME;
            if (hasRequest) {
                request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, binding);
                previousGspCode = (String)request.getAttribute(GrailsApplicationAttributes.GSP_CODEC);
            }

            makeLegacyCodecVariablesAvailable(hasRequest, binding);

            binding.setVariableDirectly(GroovyPage.RESPONSE, response);
            binding.setVariableDirectly(GroovyPage.REQUEST, request);
            // support development mode's evaluate (so that doesn't search for missing variable in parent bindings)

            GroovyPage page = null;
            try {
                page = (GroovyPage)metaInfo.getPageClass().newInstance();
            } catch (Exception e) {
                throw new GroovyPagesException("Problem instantiating page class", e);
            }
            page.setBinding(binding);
            binding.setOwner(page);

            page.initRun(out, webRequest, metaInfo);

            int debugId = 0;
            long debugStartTimeMs = 0;
            if (debugTemplates) {
                debugId = debugTemplatesIdCounter.incrementAndGet();
                out.write("<!-- GSP #");
                out.write(String.valueOf(debugId));
                out.write(" START template: ");
                out.write(page.getGroovyPageFileName());
                out.write(" precompiled: ");
                out.write(String.valueOf(metaInfo.isPrecompiledMode()));
                out.write(" lastmodified: ");
                out.write(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(metaInfo.getLastModified())));
                out.write(" -->");
                debugStartTimeMs=System.currentTimeMillis();
            }
            try {
                page.run();
            }
            finally {
                page.cleanup();
                if (hasRequest) {
                    if (newParentCreated) {
                        request.removeAttribute(GrailsApplicationAttributes.PAGE_SCOPE);
                    } else  {
                        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, parentBinding);
                    }
                    request.setAttribute(GrailsApplicationAttributes.GSP_CODEC, previousGspCode != null ? previousGspCode : GSP_NONE_CODEC_NAME);
                }
            }
            if (debugTemplates) {
                out.write("<!-- GSP #");
                out.write(String.valueOf(debugId));
                out.write(" END template: ");
                out.write(page.getGroovyPageFileName());
                out.write(" rendering time: ");
                out.write(String.valueOf(System.currentTimeMillis() - debugStartTimeMs));
                out.write(" ms -->");
            }
        }
        return out;
    }

    private void makeLegacyCodecVariablesAvailable(boolean hasRequest, GroovyPageBinding binding) {
        if (metaInfo.getExpressionEncoder() != null) {
            if (hasRequest) {
                request.setAttribute(GrailsApplicationAttributes.GSP_CODEC, metaInfo.getExpressionEncoder().getCodecIdentifier().getCodecName());
            }
            binding.setVariableDirectly(GroovyPage.CODEC_VARNAME, metaInfo.getExpressionEncoder());
        } else {
            if (hasRequest) {
                request.setAttribute(GrailsApplicationAttributes.GSP_CODEC, GSP_NONE_CODEC_NAME);
            }
            binding.setVariableDirectly(GroovyPage.CODEC_VARNAME, gspNoneCodeInstance);
        }
    }

    private static final GspNoneCodec gspNoneCodeInstance = new GspNoneCodec();

    private static final class GspNoneCodec {
        @SuppressWarnings("unused")
        public final Object encode(Object object) {
            return object;
        }
    }

    private GroovyPageBinding createBinding(Binding parent) {
        GroovyPageBinding binding = new GroovyPageBinding();
        binding.setParent(parent);
        binding.setVariableDirectly("it", null);
        if (additionalBinding != null) {
            binding.addMap(additionalBinding);
        }
        // set plugin context path for top level rendering, this means actual view + layout
        // view is top level when parent is GroovyPageRequestBinding
        // pluginContextPath is also resetted when a plugin template is overrided by an application view
        if (parent==null || (parent instanceof GroovyPageBinding && ((GroovyPageBinding)parent).isRoot()) || "".equals(metaInfo.getPluginPath())) {
            binding.setPluginContextPath(metaInfo.getPluginPath());
        }
        binding.setPagePlugin(metaInfo.getPagePlugin());
        return binding;
    }

    /**
     * Copy all of input to output.
     * @param in The input stream to writeInputStreamToResponse from
     * @param out The output to write to
     * @throws IOException When an error occurs writing to the response Writer
     */
    protected void writeInputStreamToResponse(InputStream in, Writer out) throws IOException {
        try {
            in.reset();
            Reader reader = new InputStreamReader(in, "UTF-8");
            char[] buf = new char[8192];

            for (;;) {
                int read = reader.read(buf);
                if (read <= 0) break;
                out.write(buf, 0, read);
            }
        }
        finally {
            out.close();
            in.close();
        }
    }

    /**
     * Writes the Groovy source code attached to the given info object
     * to the response, prefixing each line with its line number. The
     * line numbers make it easier to match line numbers in exceptions
     * to the generated source.
     * @param info The meta info for the GSP page that we want to write
     * the generated source for.
     * @param out The writer to send the source to.
     * @throws IOException If there is either a problem with the input
     * stream for the Groovy source, or the writer.
     */
    protected void writeGroovySourceToResponse(GroovyPageMetaInfo info, Writer out) throws IOException {
        InputStream in = info.getGroovySource();
        if (in == null) return;
        try {
            try {
                in.reset();
            }
            catch (IOException e) {
                // ignore
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            int lineNum = 1;
            int maxPaddingSize = 3;

            // Prepare the buffer containing the whitespace padding.
            // The padding is used to right-align the line numbers.
            StringBuilder paddingBuffer = new StringBuilder(maxPaddingSize);
            for (int i = 0; i < maxPaddingSize; i++) {
                paddingBuffer.append(' ');
            }

            // Set the initial padding.
            String padding = paddingBuffer.toString();

            // Read the Groovy source line by line and write it to the
            // response, prefixing each line with the line number.
            for (String line = reader.readLine(); line != null; line = reader.readLine(), lineNum++) {
                // Get the current line number as a string.
                String numberText = String.valueOf(lineNum);

                // Decrease the padding if the current line number has
                // more digits than the previous one.
                if (padding.length() + numberText.length() > 4) {
                    paddingBuffer.deleteCharAt(padding.length() - 1);
                    padding = paddingBuffer.toString();
                }

                // Write out this line.
                out.write(padding);
                out.write(numberText);
                out.write(": ");
                out.write(line);
                out.write('\n');
            }
        }
        finally {
            out.close();
            in.close();
        }
    }
}
