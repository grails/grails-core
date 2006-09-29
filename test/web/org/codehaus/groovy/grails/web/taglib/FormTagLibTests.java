package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.xpath.DefaultXPath;

/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the
 * creation of HTML forms
 *
 * @author Graeme
 *
 */
public class FormTagLibTests extends AbstractTagLibTests {

    /** The name used for the datePicker tags created in the test cases. */
    private static final String DATE_PICKER_TAG_NAME = "testDatePicker";

    private static final Collection DATE_PRECISIONS_INCLUDING_MINUTE = Collections.unmodifiableCollection(Arrays.asList(new String[] {"minute", null}));
    private static final Collection DATE_PRECISIONS_INCLUDING_HOUR = Collections.unmodifiableCollection(Arrays.asList(new String[] {"hour", "minute", null}));
    private static final Collection DATE_PRECISIONS_INCLUDING_DAY = Collections.unmodifiableCollection(Arrays.asList(new String[] {"day", "hour", "minute", null}));
    private static final Collection DATE_PRECISIONS_INCLUDING_MONTH = Collections.unmodifiableCollection(Arrays.asList(new String[] {"month", "day", "hour", "minute", null}));

    public void testTextFieldTag() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Closure tag = getTag("textField",pw);

        assertNotNull(tag);

        Map attrs = new HashMap();
        attrs.put("name","testField");
        attrs.put("value", "1");

        tag.call(new Object[]{attrs});

        Document document = DocumentHelper.parseText(sw.toString());
        assertNotNull(document);

        Element inputElement = document.getRootElement();
        assertEquals("input",inputElement.getName());

        assertEquals("testField",inputElement.attributeValue("name"));
        assertEquals("text",inputElement.attributeValue("type"));
        assertEquals("1",inputElement.attributeValue("value"));
    }
    public void testHiddenFieldTag() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Closure tag = getTag("hiddenField",pw);

        assertNotNull(tag);

        Map attrs = new HashMap();
        attrs.put("name","testField");
        attrs.put("value", "1");

        tag.call(new Object[]{attrs});

        Document document = DocumentHelper.parseText(sw.toString());
        assertNotNull(document);

        Element inputElement = document.getRootElement();
        assertEquals("input",inputElement.getName());

        assertEquals("testField",inputElement.attributeValue("name"));
        assertEquals("hidden",inputElement.attributeValue("type"));
        assertEquals("1",inputElement.attributeValue("value"));
    }

    public void testRadioTag() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Closure tag = getTag("radio",pw);

        assertNotNull(tag);

        Map attrs = new HashMap();
        attrs.put("name","testRadio");
        attrs.put("checked", "true");
        attrs.put("value", "1");

        tag.call(new Object[]{attrs});

        Document document = DocumentHelper.parseText(sw.toString());
        assertNotNull(document);

        Element inputElement = document.getRootElement();
        assertEquals("input",inputElement.getName());

        assertEquals("testRadio",inputElement.attributeValue("name"));
        assertEquals("checked",inputElement.attributeValue("checked"));
        assertEquals("1",inputElement.attributeValue("value"));

        sw.getBuffer().delete(0,sw.getBuffer().length());

        attrs.remove("checked");
        attrs.put("name","testRadio");
        attrs.put("value","2");

        tag.call(new Object[]{attrs});

        document = DocumentHelper.parseText(sw.toString());
        assertNotNull(document);

        System.out.println(sw.toString());
        inputElement = document.getRootElement();
        assertEquals("input",inputElement.getName());

        assertEquals("testRadio",inputElement.attributeValue("name"));
        assertNull(inputElement.attributeValue("checked"));
        assertEquals("2",inputElement.attributeValue("value"));
    }

    public void testCheckboxTag() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Closure tag = getTag("checkBox",pw);

        Map attrs = new HashMap();
        attrs.put("name","testCheck");
        attrs.put("value", "true");
        attrs.put("extra","1");

        assertNotNull(tag);

        tag.call(new Object[]{attrs});

        String enclosed  = "<test>" + sw.toString() + "</test>";

        System.out.println(enclosed);
        Document document = DocumentHelper.parseText(enclosed);
        assertNotNull(document);

        Element root = document.getRootElement();

        List els = root.elements();
        assertEquals(2, els.size());

        Element hidden = (Element)els.get(0);
        Element checkbox = (Element)els.get(1);

        assertEquals("hidden", hidden.attributeValue("type"));
        assertEquals("_testCheck", hidden.attributeValue("name"));

        assertEquals("checkbox", checkbox.attributeValue("type"));
        assertEquals("testCheck", checkbox.attributeValue("name"));
        assertEquals("checked", checkbox.attributeValue("checked"));
        assertEquals("true", checkbox.attributeValue("value"));
        assertEquals("1", checkbox.attributeValue("extra"));
    }

    public void testDatePickerTagWithDefaultDateAndPrecision() throws Exception {
        testDatePickerTag(null, null);
    }

    public void testDatePickerTagWithYearPrecision() throws Exception {
        testDatePickerTag(null, "year");
    }

    public void testDatePickerTagWithMonthPrecision() throws Exception {
        testDatePickerTag(null, "month");
    }

    public void testDatePickerTagWithDayPrecision() throws Exception {
        testDatePickerTag(null, "day");
    }

    public void testDatePickerTagWithHourPrecision() throws Exception {
        testDatePickerTag(null, "hour");
    }

    public void testDatePickerTagWithMinutePrecision() throws Exception {
        testDatePickerTag(null, "minute");
    }

    public void testDatePickerTagWithCustomDate() throws Exception {
        testDatePickerTag(new Date(0), null);
    }

    public void testDatePickerTagWithCustomDateAndPrecision() throws Exception {
        testDatePickerTag(new Date(0), "day");
    }

    private void testDatePickerTag(Date date, String precision) throws Exception {
        Document document = getDatePickerOutput(date, precision);
        assertNotNull(document);

        // validate presence and structure of hidden date picker form field
        XPath xpath = new DefaultXPath("//input[@name='" + DATE_PICKER_TAG_NAME + "' and @type='hidden' and @value='struct']");
        assertTrue(xpath.booleanValueOf(document));

        // if no date was given, default to the current date
        Calendar calendar = new GregorianCalendar();
        if (date != null) {
                calendar.setTime(date);
        }

        // validate presence and value of selected date fields
        validateSelectedYearValue(document, calendar);
        validateSelectedMonthValue(document, calendar, precision);
        validateSelectedDayValue(document, calendar, precision);
        validateSelectedHourValue(document, calendar, precision);
        validateSelectedMinuteValue(document, calendar, precision);
    }

    private void validateSelectedYearValue(Document document, Calendar calendar) {
        assertSelectFieldPresentWithSelectedValue(document, DATE_PICKER_TAG_NAME + "_year", Integer.toString(calendar.get(Calendar.YEAR)));
    }

    private void validateSelectedMonthValue(Document document, Calendar calendar, String precision) {
        final String FIELD_NAME = DATE_PICKER_TAG_NAME + "_month";

        String expectedMonthValue = Integer.toString(1); // January
        if (DATE_PRECISIONS_INCLUDING_MONTH.contains(precision)) {
            expectedMonthValue = Integer.toString(calendar.get(Calendar.MONTH)+1);
            assertSelectFieldPresentWithSelectedValue(document, FIELD_NAME, expectedMonthValue);
        }
        else {
            assertSelectFieldNotPresentValue(document, FIELD_NAME);
        }
    }

    private void validateSelectedDayValue(Document document, Calendar calendar, String precision) {
        final String FIELD_NAME = DATE_PICKER_TAG_NAME + "_day";

        String expectedDayValue = Integer.toString(1); // 1st day of the month
        if (DATE_PRECISIONS_INCLUDING_DAY.contains(precision)) {
            expectedDayValue = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
            assertSelectFieldPresentWithSelectedValue(document, FIELD_NAME, expectedDayValue);
        }
        else {
            assertSelectFieldNotPresentValue(document, FIELD_NAME);
        }
    }

    private void validateSelectedHourValue(Document document, Calendar calendar, String precision) {
        final String FIELD_NAME = DATE_PICKER_TAG_NAME + "_hour";

        String expectedHourValue = "00";
        if (DATE_PRECISIONS_INCLUDING_HOUR.contains(precision)) {
            int rawHourValue = calendar.get(Calendar.HOUR_OF_DAY);
            expectedHourValue = (rawHourValue < 10) ? ("0" + rawHourValue) : Integer.toString(rawHourValue);
            assertSelectFieldPresentWithSelectedValue(document, FIELD_NAME, expectedHourValue);
        }
        else {
            assertSelectFieldNotPresentValue(document, FIELD_NAME);
        }
    }

    private void validateSelectedMinuteValue(Document document, Calendar calendar, String precision) {
        final String FIELD_NAME = DATE_PICKER_TAG_NAME + "_minute";

        String expectedMinuteValue = "00";
        if (DATE_PRECISIONS_INCLUDING_MINUTE.contains(precision)) {
            int rawMinuteValue = calendar.get(Calendar.MINUTE);
            expectedMinuteValue = (rawMinuteValue < 10) ? ("0" + rawMinuteValue) : Integer.toString(rawMinuteValue);
            assertSelectFieldPresentWithSelectedValue(document, FIELD_NAME, expectedMinuteValue);
        }
        else {
            assertSelectFieldNotPresentValue(document, FIELD_NAME);
        }
    }

    private void assertSelectFieldPresentWithSelectedValue(Document document, String fieldName, String value) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']/option[@selected='selected' and @value='" + value + "']");
//        assertTrue(xpath.booleanValueOf(document)); TODO
    }

    private void assertSelectFieldNotPresentValue(Document document, String fieldName) {
        XPath xpath = new DefaultXPath("//select[@name='" + fieldName + "']");
        assertFalse(xpath.booleanValueOf(document));
    }

    private Document getDatePickerOutput(Date date, String precision) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Closure tag = getTag("datePicker", pw);

        assertNotNull(tag);

        Map attrs = new HashMap();
        attrs.put("name", DATE_PICKER_TAG_NAME);

        if (date != null) {
            attrs.put("value", date);
        }

        if (precision != null) {
            attrs.put("precision", precision);
        }

        tag.call(new Object[] { attrs });

        String enclosed = "<test>" + sw.toString() + "</test>";

        return DocumentHelper.parseText(enclosed);
    }
}
