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
package org.codehaus.groovy.grails.web.pages;

import grails.util.BuildSettingsHolder;
import grails.util.Environment;
import grails.util.PluginBuildSettings;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.filters.StringInputStream;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing implementation for GSP taglib files
 *
 * @author Ivo Houbrechts
 */
public class GspTagParser extends GroovyPageParser {

    public static final Log LOG = LogFactory.getLog(GspTagParser.class);

    protected static final Pattern REQUIRED_ATTR_PATTERN = Pattern.compile(
            "@attr\\s+(\\w+)\\s+REQUIRED", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static final String[] DEFAULT_IMPORTS = {
            "org.codehaus.groovy.grails.web.taglib.*"
    };

    protected static final String NAMESPACE_DIRECTIVE = "namespace";
    protected static final String DOCS_DIRECTIVE = "docs";

    protected String tagName;
    //TODO: fetch addRequiredAsserts form Config or BuildConfig? -> should be done the same way as gspEncoding, but that currently doesn't work with pre-compilation
    protected boolean addRequiredAsserts = false;
    protected String tagNamespace;
    protected String tagDocs;

    @SuppressWarnings("rawtypes")
    public GspTagParser(GspTagInfo tagInfo) throws IOException {
        super(tagInfo.getTagName(),
                tagInfo.getTagName(),
                tagInfo.getTagLibFileName(),
                new StringInputStream(tagInfo.getText()));
        className = tagInfo.getTagLibName();
        pageName = tagInfo.getTagLibName();
        sourceName = tagInfo.getTagLibName() + ".gsp";
        packageName = tagInfo.getPackageName();
        tagName = tagInfo.getTagName();

        String filename = tagInfo.getFilePath();
        if (filename != null && BuildSettingsHolder.getSettings() != null) {
            PluginBuildSettings pluginBuildSettings = new PluginBuildSettings(BuildSettingsHolder.getSettings());
            GrailsPluginInfo info = pluginBuildSettings.getPluginInfoForSource(filename);
            if (info != null) {
                pluginAnnotation = "@GrailsPlugin(name='" + info.getName() + "', version='" +
                    info.getVersion() + "')";
            }
        }

        gspEncoding = tagInfo.getGspEncoding();
        String gspSource = tagInfo.getText();
        scan = new GroovyPageScanner(gspSource);
        environment = Environment.getCurrent();
    }


    protected void directPage(String text) {

        text = text.trim();
        Matcher mat = PAGE_DIRECTIVE_PATTERN.matcher(text);
        for (int ix = 0; ; ) {
            if (!mat.find(ix)) {
                return;
            }
            String name = mat.group(1);
            String value = mat.group(2);
            if (name.equals(IMPORT_DIRECTIVE)) {
                pageImport(value);
            }
            if (name.equals(NAMESPACE_DIRECTIVE)) {
                tagNamespace = value;
            }
            if (name.equals(CONTENT_TYPE_DIRECTIVE)) {
                contentType(value);
            }
            if (name.equals(DOCS_DIRECTIVE)) {
                tagDocs = value;
            }
            if (name.equals(DEFAULT_CODEC_DIRECTIVE)) {
                defaultCodecDirectiveValue = value.trim();
            }
            ix = mat.end();
        }
    }

    protected void expr() {
        if (!finalPass) return;

        LOG.debug("parse: expr");

        String text = scan.getToken().trim();
        text = getExpressionText(text);
        if (text != null && text.length() > 2 && text.startsWith("(") && text.endsWith(")")) {
            out.printlnToResponse("out", text.substring(1, text.length() - 1));
        } else {
            out.printlnToResponse("out", text);
        }
    }

    public String getExpressionText(String text, boolean _safeDereference) {
        boolean safeDereference = false;
        if (text.endsWith("?")) {
            text = text.substring(0, text.length() - 1);
            safeDereference = _safeDereference;
        }
        text = "(" + text + ")" + (safeDereference ? "?" : "");
        return text;
    }


    protected void htmlPartPrintlnRaw(int partNumber) {
        out.print("out.print('");
        out.print(escapeGroovy(htmlParts.get(partNumber)));
        out.print("')");
        out.println();
    }

    protected void writeClassStart() {
        out.println();
        if (pluginAnnotation != null) {
            out.println(pluginAnnotation);
        }
        out.print("class ");
        out.print(className);
        out.println(" {");
        if (tagNamespace != null) {
            out.print("static namespace = \"");
            out.print(tagNamespace);
            out.println('"');
        }
        if (tagDocs != null) {
            out.println("/**");
            for (String line : tagDocs.split("\n")) {
                out.print(" * ");
                out.println(line);
            }
            out.println(" */");
        }
        out.println("Closure " + tagName + " = { attrs, body ->");

        if (addRequiredAsserts && tagDocs != null) {
            Matcher m = REQUIRED_ATTR_PATTERN.matcher(tagDocs);
            while (m.find()) {
                String attribute = m.group(1);
                out.println("assert attrs." + attribute + "!= null, \"Required tag attribute " + attribute + " may not be null\"");
            }
        }
    }

    protected void writeClassEnd() {
        if (!tagMetaStack.isEmpty()) {
            throw new GrailsTagException("Grails tags were not closed! [" +
                    tagMetaStack + "] in GSP " + pageName + "", pageName,
                    out.getCurrentLineNumber());
        }

        out.println("}");

        if (shouldAddLineNumbers()) {
            addLineNumbers();
        }

        out.println("}");
    }

    protected void endTag() {
        if (!finalPass) return;

        String tagName = scan.getToken().trim();
        String ns = scan.getNamespace();

        if (tagMetaStack.isEmpty())
            throw new GrailsTagException(
                    "Found closing Grails tag with no opening [" + tagName + "]", pageName,
                    out.getCurrentLineNumber());

        TagMeta tm = tagMetaStack.pop();
        String lastInStack = tm.name;
        String lastNamespaceInStack = tm.namespace;

        // if the tag name is blank then it has been closed by the start tag ie <tag />
        if (StringUtils.isBlank(tagName)) {
            tagName = lastInStack;
        }

        if (!lastInStack.equals(tagName) || !lastNamespaceInStack.equals(ns)) {
            throw new GrailsTagException("Grails tag [" + lastNamespaceInStack +
                    ":" + lastInStack + "] was not closed", pageName, out.getCurrentLineNumber());
        }

        if (GroovyPage.DEFAULT_NAMESPACE.equals(ns) && tagRegistry.isSyntaxTag(tagName)) {
            if (tm.instance instanceof GroovySyntaxTag) {
                GroovySyntaxTag tag = (GroovySyntaxTag) tm.instance;
                tag.doEndTag();
            } else {
                throw new GrailsTagException("Grails tag [" + tagName +
                        "] was not closed", pageName,
                        out.getCurrentLineNumber());
            }
        } else {
            String bodyTagClosureName = "null";
            if (!tm.emptyTag && !tm.bufferMode) {
                bodyTagClosureName = "body" + tagIndex;
                out.println("})");
                closureLevel--;
            }

            if (tm.bufferMode && tm.bufferPartNumber != -1) {
                if (!bodyVarsDefined.contains(tm.tagIndex)) {
                    out.print("def ");
                    bodyVarsDefined.add(tm.tagIndex);
                }
                out.print("body" + tm.tagIndex + " = '" + escapeGroovy(htmlParts.get(tm.bufferPartNumber)));
                out.println("'");
                bodyTagClosureName = "body" + tm.tagIndex;
                tm.bufferMode = false;
            }

            if (jspTags.containsKey(ns)) {
                String uri = jspTags.get(ns);
                out.println("jspTag = tagLibraryResolver?.resolveTagLibrary('" +
                        uri + "')?.getTag('" + tagName + "')");
                out.println("if (!jspTag) throw new GrailsTagException('Unknown JSP tag " +
                        ns + ":" + tagName + "')");
                out.println("jspTag.doTag(out," + attrsVarsMapDefinition.get(tagIndex) + ", " +
                        bodyTagClosureName + ")");
            } else {
                if (tm.hasAttributes) {
                    out.println("out.print(" + ns + '.' + tagName + "(" + attrsVarsMapDefinition.get(tagIndex) +
                            "," + bodyTagClosureName + "))");
                } else {
                    out.println("out.print(" + ns + '.' + tagName + "(" + "[:]," + bodyTagClosureName + "))");
                }
            }
        }

        tm.bufferMode = false;
        //remove all bodyVars with a higher nesting to force a "def " being printed if nesting is increased again
        Iterator<Integer> iter = bodyVarsDefined.iterator();
        while (iter.hasNext()){
            if(iter.next() > tagIndex){
                iter.remove();
            }
        }
        tagIndex--;
    }

    protected void writeTagBodyStart(TagMeta tm) {
        if (tm.bufferMode) {
            tm.bufferMode = false;
            if (!bodyVarsDefined.contains(tm.tagIndex)) {
                out.print("def ");
                bodyVarsDefined.add(tm.tagIndex);
            }
            out.println("body" + tm.tagIndex + " = new GroovyPageTagBody(this,webRequest, {");
            closureLevel++;
        }
    }

    public boolean isAddRequiredAsserts() {
        return addRequiredAsserts;
    }

    public void setAddRequiredAsserts(boolean addRequiredAsserts) {
        this.addRequiredAsserts = addRequiredAsserts;
    }

    protected String[] getDefaultImports(){
        return DEFAULT_IMPORTS;
    }
}
