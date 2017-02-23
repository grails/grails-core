package org.grails.web.mapping;

import grails.gorm.validation.Constrained;
import grails.gorm.validation.ConstrainedProperty;
import grails.gorm.validation.Constraint;
import grails.util.GrailsWebMockUtil;
import grails.web.mapping.UrlMapping;
import grails.web.mapping.UrlMappingInfo;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.List;
import java.util.Map;

import org.grails.web.servlet.mvc.GrailsWebRequest;

@SuppressWarnings("rawtypes")
public class DefaultUrlMappingEvaluatorTests extends AbstractGrailsMappingTests {

    public void testNamedMappings() throws Exception {
        GroovyShell shell = new GroovyShell();
        Binding binding = new Binding();
        Script script = shell.parse("mappings = {\n" +
                "name firstMapping: \"/first/one\" {\n" +
                "}\n" +
                "\"/second/one\" {" +
                "}\n" +
                "name thirdMapping: \"/third/one\" {\n" +
                "}\n" +
        "}");

        script.setBinding(binding);
        script.run();

        Closure closure = (Closure)binding.getVariable("mappings");
        List mappings = evaluator.evaluateMappings(closure);
        assertEquals(3, mappings.size());
    }

    public void testRedirectMappings() throws Exception {
        GroovyShell shell = new GroovyShell();
        Binding binding = new Binding();
        Script script = shell.parse("mappings = {\n" +
                "\"/first\"(redirect:[controller: 'foo', action: 'bar'])\n" +
                "\"/second\"(redirect: '/bing/bang')\n" +
        "}");

        script.setBinding(binding);
        script.run();

        Closure closure = (Closure)binding.getVariable("mappings");
        List<UrlMapping> mappings = evaluator.evaluateMappings(closure);
        assertEquals(2, mappings.size());
        Object redirectInfo = mappings.get(0).getRedirectInfo();
        assertTrue(redirectInfo instanceof Map);
        Map redirectMap = (Map)redirectInfo;
        assertEquals(2, redirectMap.size());
        assertEquals("foo", redirectMap.get("controller"));
        assertEquals("bar", redirectMap.get("action"));
        assertEquals("/bing/bang", mappings.get(1).getRedirectInfo());
    }

    public void testNewMethod() throws Exception {
        GroovyShell shell = new GroovyShell();
        Binding binding = new Binding();
        Script script = shell.parse (
                "mappings = {\n" +
                "    \"/$controller/$action?/$id?\" { \n" +
                "        constraints {\n" +
                "            id(matches:/\\d+/)\n" +
                "        }\n" +
                "    }\n" +
                "}\n");

        script.setBinding(binding);
        script.run();

        Closure closure = (Closure) binding.getVariable("mappings");
        List mappings = evaluator.evaluateMappings(closure);

        assertEquals(1, mappings.size());

        UrlMapping mapping = (UrlMapping) mappings.get(0);

        assertNull(mapping.getActionName());
        assertNull(mapping.getControllerName());
        assertEquals("(*)",mapping.getUrlData().getTokens()[0]);
        assertEquals("(*)?",mapping.getUrlData().getTokens()[1]);
        assertEquals("(*)?",mapping.getUrlData().getTokens()[2]);

        assertNotNull(mapping.getConstraints());

        assertTrue(makeSureMatchesConstraintExistsOnId(mapping));

        GrailsWebRequest r = GrailsWebMockUtil.bindMockWebRequest();

        UrlMappingInfo info = mapping.match("/mycontroller");
        info.configure(r);

        assertEquals("mycontroller", info.getControllerName());
        assertNull(mapping.match("/mycontroller").getActionName());
        assertNull(mapping.match("/mycontroller").getId());

        UrlMappingInfo info2 = mapping.match("/mycontroller/test");
        info2.configure(r);
        assertEquals("test", info2.getActionName());
        assertNull(mapping.match("/mycontroller/test").getId());
        assertEquals("234", mapping.match("/blog/test/234").getId());
    }

    public void testResourceMappingsWithVersionAndNamespace() throws Exception {
        GroovyShell shell = new GroovyShell();
        Binding binding = new Binding();
        // Resource Entry: "/api/foo"(resources:"foo", version:'1.0', namespace:'v1')
        Script script = shell.parse("mappings = {\n" +
                "\"/api/foo\"(resources: 'foo', version: '1.0', namespace: 'v1')\n" +
        "}");

        script.setBinding(binding);
        script.run();

        Closure closure = (Closure)binding.getVariable("mappings");
        List<UrlMapping> mappings = evaluator.evaluateMappings(closure);
        assertTrue(mappings.size() > 0);
        //Check that version and namespace are correct for each mapping
        for (UrlMapping mapping: mappings) {
            assertEquals("1.0", mapping.getVersion());
            assertEquals("v1", mapping.getNamespace());
        }

    }

    private boolean makeSureMatchesConstraintExistsOnId(UrlMapping mapping) {
        Constrained[] props = mapping.getConstraints();
        for (int i = 0; i < props.length; i++) {

            ConstrainedProperty prop = (ConstrainedProperty) props[i];
            if ("id".equals(prop.getPropertyName())) {
                Constraint[] constraints = prop.getAppliedConstraints().toArray(new Constraint[0]);
                for (int j = 0; j < constraints.length; j++) {
                    if (constraints[j].getClass().getName().endsWith("MatchesConstraint")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
