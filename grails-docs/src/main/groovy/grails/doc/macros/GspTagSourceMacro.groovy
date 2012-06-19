package grails.doc.macros

import java.util.regex.Pattern
import org.radeox.macro.BaseMacro
import org.radeox.macro.CodeMacro
import org.radeox.macro.parameter.BaseMacroParameter
import org.radeox.macro.parameter.MacroParameter
import org.radeox.util.Encoder

class GspTagSourceMacro extends BaseMacro {

    List baseDirs

    GspTagSourceMacro(basedir) {
        if (!(basedir instanceof Collection || basedir.class.array)) basedir = [ basedir ]
        baseDirs = basedir.collect { f -> f as File }
    }

    String getName() { "source" }

    void execute(Writer out, MacroParameter params) {
        def source = params.params.get("0")

        def i = source.indexOf('=')
        def type = source[0..i-1]
        def name = source[i+1..-1]
        String code

        switch (type) {
            case "tag":
                def j = name.indexOf('.')
                def className = name[0..j-1]
                def tagName = name[j+1..-1]
                Pattern regex = ~/(?s)(\s*?def\s+?$tagName\s*?=\s*?\{\s*?attrs\s*?,{0,1}\s*?(body){0,1}\s*?->.+?)(\/\*\*|def\s*[a-zA-Z]+?\s*=\s*\{)/

                // Recursively search for the tag library source file in the
                // configured base directory.
                def tagLibFile = null
                baseDirs.find { dir ->
                    dir.traverse(nameFilter: /${className}.groovy/) { tagLibFile = it }
                    return tagLibFile
                }

                def text = tagLibFile?.text ?: ""
                def matcher = regex.matcher(text)
                if (matcher.find()) {
                    out << '<p><a href="#' + tagName +
                            '" onclick="document.getElementById(\'' + tagName +
                            '\').style.display=\'inline\'">Show Source</a></p>'
                    out << "<div id=\"$tagName\" style=\"display:none;\">"
                    text = Encoder.escape(matcher.group(1))

                    def macro = new CodeMacro()
                    macro.setInitialContext(initialContext)
                    def macroParams = new BaseMacroParameter()
                    macroParams.content = text
                    macro.execute(out, macroParams)
                    out << "</div>"
                }
                break
        }
    }
}

