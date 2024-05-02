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
package grails.doc

import spock.lang.Specification

class PdfBuilderSpec extends Specification {

    void "remove CssLinks"() {
        given:
        String html = """
<head>
        <title>The Grails Framework 3.2.11</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <link rel="stylesheet" href="../css/main.css" type="text/css" media="screen, print" title="Style" charset="utf-8" />
        <link rel="stylesheet" href="../css/pdf.css" type="text/css" media="print" title="PDF" charset="utf-8" />
    <script type="text/javascript">
function addJsClass(el) {
    var classes = document.body.className.split(" ");
    classes.push("js");
    document.body.className = classes.join(" ");
}
    </script>
    </head>
    """
        String expected = """
<head>
        <title>The Grails Framework 3.2.11</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        
        
    <script type="text/javascript">
function addJsClass(el) {
    var classes = document.body.className.split(" ");
    classes.push("js");
    document.body.className = classes.join(" ");
}
    </script>
    </head>
    """
        when:
        String output = PdfBuilder.removeCssLinks(html)

        then:
        output == expected

    }
}
