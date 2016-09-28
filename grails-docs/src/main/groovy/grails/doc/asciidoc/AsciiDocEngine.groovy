package grails.doc.asciidoc

import grails.doc.DocEngine
import groovy.transform.InheritConstructors
import org.asciidoctor.Options
import org.asciidoctor.OptionsBuilder
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
        'icons':'font',
        'javaee': 'https://docs.oracle.com/javaee/7/api/',
        'javase': 'https://docs.oracle.com/javase/7/docs/api/',
        'groovyapi': 'http://docs.groovy-lang.org/latest/html/gapi/',
        'springapi': 'https://docs.spring.io/spring/docs/current/javadoc-api/'
    ]
    @Override
    String render(String content, RenderContext context) {
        asciidoctor.convert(content,
            new OptionsBuilder()
                .headerFooter(false)
                .attributes(
                    attributes
                )
                .get()
        )
    }

}
