package grails.doc.asciidoc

import grails.doc.DocEngine
import groovy.transform.InheritConstructors
import org.asciidoctor.Options
import org.asciidoctor.OptionsBuilder
import org.asciidoctor.SafeMode
import org.radeox.api.engine.context.RenderContext

import static org.asciidoctor.Asciidoctor.Factory.create;
import org.asciidoctor.Asciidoctor;
/**
 * A DocEngine implementation that uses Asciidoctor to render pages
 *
 * @author Graeme Rocher
 * @since 3.2.0
 */
@InheritConstructors
class AsciiDocEngine extends DocEngine {
    Asciidoctor asciidoctor = create();
    Map attributes = [
        'imagesdir': '../img',
        'source-highlighter':'coderay',
        'icons':'font'
    ]
    @Override
    String render(String content, RenderContext context) {
        def optionsBuilder = OptionsBuilder.options()
                                        .headerFooter(false)
                                        .attributes(attributes)
        if(attributes.containsKey('safe')) {
            optionsBuilder.safe(SafeMode.valueOf(attributes.get('safe').toString()))
        }
        asciidoctor.convert(content,
            optionsBuilder
                .get()
        )
    }

}
