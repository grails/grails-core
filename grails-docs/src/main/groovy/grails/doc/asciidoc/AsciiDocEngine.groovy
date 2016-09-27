package grails.doc.asciidoc

import grails.doc.DocEngine
import groovy.transform.InheritConstructors
import org.asciidoctor.Options
import org.asciidoctor.OptionsBuilder
import org.radeox.api.engine.context.RenderContext

import static org.asciidoctor.Asciidoctor.Factory.create;
import org.asciidoctor.Asciidoctor;
/**
 * Created by graemerocher on 26/09/2016.
 */
@InheritConstructors
class AsciiDocEngine extends DocEngine {
    Asciidoctor asciidoctor = create();

    @Override
    String render(String content, RenderContext context) {
        asciidoctor.convert(content,
            new OptionsBuilder()
                .headerFooter(false)
                .attributes(
                    'imagesdir': '../img',
                    'source-highlighter':'coderay',
                    'icons':'font'
                )
                .get()
        )
    }

}
