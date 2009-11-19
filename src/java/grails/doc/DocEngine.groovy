/* Copyright 2004-2005 the original author or authors.
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
package grails.doc

import grails.doc.filters.HeaderFilter
import grails.doc.filters.LinkTestFilter
import grails.doc.filters.ListFilter
import org.radeox.api.engine.WikiRenderEngine
import org.radeox.api.engine.context.InitialRenderContext
import org.radeox.engine.BaseRenderEngine
import org.radeox.filter.context.FilterContext
import org.radeox.filter.regex.RegexFilter
import org.radeox.filter.regex.RegexTokenFilter
import org.radeox.macro.BaseMacro
import org.radeox.macro.MacroLoader
import org.radeox.macro.parameter.MacroParameter
import org.radeox.regex.MatchResult
import org.radeox.filter.*


/**
 * A Radeox Wiki engine for generating documentation using a confluence style syntax
 * 
 * @author Graeme Rocher
 * @since 1.2
 */
class DocEngine extends BaseRenderEngine implements WikiRenderEngine {
    static final CONTEXT_PATH = "contextPath"
    static final SOURCE_FILE = "sourceFile"

    static EXTERNAL_DOCS = 	[:]
	static ALIAS = [:]

    private basedir

    Properties engineProperties

    DocEngine(InitialRenderContext context) {
        super(context)
        this.basedir = context.get("base.dir") ?: "."
    }

    boolean exists(String name) {
        int barIndex = name.indexOf('|')
        if(barIndex >-1) {
            def refItem = name[0..barIndex-1]
            def refCategory = name[barIndex+1..-1]


            if(refCategory.startsWith("http://")) return true
            else if(refCategory.startsWith("guide:")) {
				def alias = refCategory[6..-1]


				if(ALIAS[alias]) {
					alias = ALIAS[alias]
				}
                def ref = "${basedir}/src/doc/guide/${alias}.gdoc"
                def file = new File(ref)
                if(file.exists()) {
                    return true
                }
                else {
                    emitWarning(name,ref,"page")
                }
            }
			else if(refCategory.startsWith("api:")) {
				def ref = refCategory[4..-1]
				if(EXTERNAL_DOCS.keySet().find { ref.startsWith(it) }) {
					return true
				}
                else {
                    emitWarning(name,ref,"class")
                }
			}
            else {
                String dir = getNaturalName(refCategory)
                def ref = "${basedir}/src/doc/ref/${dir}/${refItem}.gdoc"
                File file = new File(ref)
                if(file.exists()) {
                    return true
                }
                else {
                    emitWarning(name,ref,"page")
                }
            }
        }

         return false

    }

    private void emitWarning(String name, String ref, String type) {
        println "WARNING: ${initialContext.get(SOURCE_FILE)}: Link '$name' refers to non-existant $type $ref!"
    }

    boolean showCreate() { false }

    protected void init() {
        def props = new Properties()
        try {
            props.load(DocEngine.classLoader.getResourceAsStream("grails/doc/doc.properties"))
            if(engineProperties) {
                props.putAll engineProperties
            }
        }
        catch (e) {
            // ignore
        }
        props.findAll { it.key.startsWith("api.")}.each {
            EXTERNAL_DOCS[it.key[4..-1]] = it.value
        }
        props.findAll { it.key.startsWith("alias.")}.each {
            ALIAS[it.key[6..-1]] = it.value
        }

        if (null == fp) {
          fp = new FilterPipe(initialContext);

            def filters = [
                            ParamFilter,
                            MacroFilter,
                            TextileLinkFilter,
                            HeaderFilter,
                            BlockQuoteFilter,
                            ListFilter,
                            LineFilter,
                            StrikeThroughFilter,
                            NewlineFilter,
                            ParagraphFilter,
                            BoldFilter,
                            CodeFilter,
                            ItalicFilter,
                            LinkTestFilter,
                            ImageFilter,
                            MarkFilter,
                            KeyFilter,
                            TypographyFilter,
                            EscapeFilter]

            for(f in filters) {
                RegexFilter filter = f.newInstance()
                fp.addFilter(filter)

                if(filter instanceof MacroFilter) {
                    MacroLoader loader = new MacroLoader()
                    def repository = filter.getMacroRepository()

                    loader.add(repository, new WarningMacro())
                    loader.add(repository, new NoteMacro())
                }
            }
            fp.init();

        }

    }

    void appendLink(StringBuffer buffer, String name, String view, String anchor) {
        def contextPath = initialContext.get(CONTEXT_PATH)

        if(name.startsWith("guide:")) {
			def alias = name[6..-1]
			if(ALIAS[alias]) {
				alias = ALIAS[alias]
			}

            buffer <<  "<a href=\"$contextPath/guide/single.html#${alias}\" class=\"guide\">$view</a>"
        }
		else if(name.startsWith("api:")) {
			def link = name[4..-1]

			def externalKey = EXTERNAL_DOCS.keySet().find { link.startsWith(it) }
			link =link.replace('.' as char, '/' as char) + ".html"

			if(externalKey) {
				buffer <<  "<a href=\"${EXTERNAL_DOCS[externalKey]}/$link${anchor ? '#' + anchor : ''}\" class=\"api\">$view</a>"
			}
			else {
				buffer <<  "<a href=\"$contextPath/api/$link${anchor ? '#' + anchor : ''}\" class=\"api\">$view</a>"
			}
		}
        else {
            String dir = getNaturalName(name)
			def link = "$contextPath/ref/${dir}/${view}.html"
            buffer <<  "<a href=\"$link\" class=\"$name\">$view</a>"
        }
    }

    void appendLink(StringBuffer buffer, String name, String view) {
        appendLink(buffer,name,view,"")
    }

    void appendCreateLink(StringBuffer buffer, String name, String view) {
        buffer.append(name)
    }

    /**
     * Converts a property name into its natural language equivalent eg ('firstName' becomes 'First Name')
     * @param name The property name to convert
     * @return The converted property name
     */
    static final nameCache = [:]
    String getNaturalName(String name) {
	    if(nameCache[name]) {
			return nameCache[name]
     	}
		else {
	        List words = new ArrayList();
	        int i = 0;
	        char[] chars = name.toCharArray();
	        for (int j = 0; j < chars.length; j++) {
	            char c = chars[j];
	            String w;
	            if(i >= words.size()) {
	                w = "";
	                words.add(i, w);
	            }
	            else {
	                w = (String)words.get(i);
	            }

	            if(Character.isLowerCase(c) || Character.isDigit(c)) {
	                if(Character.isLowerCase(c) && w.length() == 0)
	                    c = Character.toUpperCase(c);
	                else if(w.length() > 1 && Character.isUpperCase(w.charAt(w.length() - 1)) ) {
	                    w = "";
	                    words.add(++i,w);
	                }

	                words.set(i, w + c);
	            }
	            else if(Character.isUpperCase(c)) {
	                if((i == 0 && w.length() == 0) || Character.isUpperCase(w.charAt(w.length() - 1)) ) 	{
	                    words.set(i, w + c);
	                }
	                else {
	                    words.add(++i, String.valueOf(c));
	                }
	            }

	        }

	        nameCache[name] = words.join(' ')
			return nameCache[name]
		}
    }
}

public class WarningMacro extends BaseMacro {
    String getName() {"warning"}
    void execute(Writer writer, MacroParameter params) {
    writer << '<blockquote class="warning">' << params.content << "</blockquote>"
  }
}
public class NoteMacro extends BaseMacro {
    String getName() {"note"}
    void execute(Writer writer, MacroParameter params) {
    writer << '<blockquote class="note">' << params.content << "</blockquote>"
  }
}
class BlockQuoteFilter extends RegexTokenFilter {
    public BlockQuoteFilter() {
        super(/(?m)^bc.\s*?(.*?)\n\n/);
    }
    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
        buffer << "<pre class=\"bq\"><code>${result.group(1)}</code></pre>\n\n"
    }

}
class ItalicFilter extends RegexTokenFilter {
    public ItalicFilter() {
        super(/\s_([^\n]*?)_\s/);
    }
    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
        buffer << " <em class=\"italic\">${result.group(1)}</em> "
    }
}
class BoldFilter extends RegexTokenFilter {
    public BoldFilter() {
        super(/\*([^\n]*?)\*/);
    }
    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
        buffer << "<strong class=\"bold\">${result.group(1)}</strong>"
    }
}
class CodeFilter extends RegexTokenFilter {

    public CodeFilter() {
        super(/@([^\n]*?)@/);
    }


    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
		def text = result.group(1)
		// are we inside a code block?
		if(text.indexOf('class="code"') > -1) buffer << "@$text@"
		else buffer << "<code>${text}</code>"
    }
}
class ImageFilter  extends RegexTokenFilter {

    public ImageFilter() {
        super(/!([^\n]*?\.(jpg|png|gif))!/);
    }


    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
        def img = result.group(1)
        if(img.startsWith("http://")) {
            buffer << "<img border=\"0\" class=\"center\" src=\"$img\"></img>"
        }
        else {            
            def path = context.renderContext.get("contextPath") ?: "."
            buffer << "<img border=\"0\" class=\"center\" src=\"$path/img/$img\"></img>"
        }
    }
}
class TextileLinkFilter extends RegexTokenFilter {

    public TextileLinkFilter() {
        super(/"([^"]+?)":(\S+?)(\s)/);
    }


    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
        def text = result.group(1)
        def link = result.group(2)
        def space = result.group(3)

        if(link.startsWith("http://")) {
            buffer << "<a href=\"$link\" target=\"blank\">$text</a>$space"
        }
        else {
            buffer << "<a href=\"$link\">$text</a>$space"
        }
    }
}

