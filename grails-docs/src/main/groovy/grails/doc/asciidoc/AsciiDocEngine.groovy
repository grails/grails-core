/*
 * Copyright 2024 original authors
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
