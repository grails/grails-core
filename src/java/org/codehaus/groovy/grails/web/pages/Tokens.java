package org.codehaus.groovy.grails.web.pages;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Interface defining an enumeration of tokens for the different scriptlet types
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 *
 * Date: Jan 10, 2004
 */
interface Tokens {
    int EOF = -1;
    int HTML = 0;
    int JEXPR = 1;   // <%= ... %>
    int JSCRIPT = 2; // <% .... %>
    int JDIRECT = 3; // <%@ ... %>
    int JDECLAR = 4; // <%! ... %>
    int GEXPR = 11;   // ${ ... }
    int GSCRIPT = 12; // %{ ... }%
    int GDIRECT = 13; // @{ ... }
    int GDECLAR = 14; // !{ ... }!
    int GSTART_TAG = 15; // <g:..>
    int GEND_TAG = 16; // </g:..>
    int GTAG_EXPR = 17; // ${..}
    int GEND_EMPTY_TAG = 18;
}
