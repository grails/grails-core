package org.grails.plugins;

import grails.plugins.PluginFilter;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("rawtypes")
public class PluginFilterFactoryTests {

    @Test
    public void testIncludeFilterOne() throws Exception {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter("one", null);
        assertTrue(bean instanceof IncludingPluginFilter);

        IncludingPluginFilter filter = (IncludingPluginFilter)bean;
        Set suppliedNames = filter.getSuppliedNames();
        assertEquals(1, suppliedNames.size());
        assertTrue(suppliedNames.contains("one"));
    }

    @Test
    public void testIncludeFilter() throws Exception {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter("one, two", " three , four ");
        assertTrue(bean instanceof IncludingPluginFilter);

        IncludingPluginFilter filter = (IncludingPluginFilter)bean;
        Set suppliedNames = filter.getSuppliedNames();
        assertEquals(2, suppliedNames.size());
        assertTrue(suppliedNames.contains("two"));
    }

    @Test
    public void testExcludeFilter() throws Exception {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter(null, " three , four ");
        assertTrue(bean instanceof ExcludingPluginFilter);

        ExcludingPluginFilter filter = (ExcludingPluginFilter)bean;
        Set suppliedNames = filter.getSuppliedNames();
        assertEquals(2, suppliedNames.size());
        assertTrue(suppliedNames.contains("four"));
    }

    @Test
    public void testDefaultFilter() throws Exception {
        PluginFilterRetriever fb = new PluginFilterRetriever();
        PluginFilter bean = fb.getPluginFilter(null, null);
        assertTrue(bean instanceof IdentityPluginFilter);
    }
}
