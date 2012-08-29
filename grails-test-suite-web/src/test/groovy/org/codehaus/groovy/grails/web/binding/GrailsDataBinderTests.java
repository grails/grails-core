/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.binding;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

import junit.framework.TestCase;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Graeme Rocher
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class GrailsDataBinderTests extends TestCase {

    public static final String DATE_TIME_FORMAT = "MM/dd/yyyy hh:mm a";

    class TestBean {
        private Date myDate;
        private TestDateTimeBean myDateTime;
        private java.sql.Date mySqlDate;
        private String name;
        private Long securityNumber;
        private BigInteger bigNumber;
        private BigDecimal credit;
        private Double angle;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getMyDate() {
            return myDate;
        }

        public void setMyDate(Date myDate) {
            this.myDate = myDate;
        }

        public TestDateTimeBean getMyDateTime() {
            return myDateTime;
        }

        public void setMyDateTime(TestDateTimeBean myDateTime) {
            this.myDateTime = myDateTime;
        }

        public java.sql.Date getMySqlDate() {
            return mySqlDate;
        }

        public void setMySqlDate(java.sql.Date mySqlDate) {
            this.mySqlDate = mySqlDate;
        }

        public Long getSecurityNumber() {
            return securityNumber;
        }

        public void setSecurityNumber(Long securityNumber) {
            this.securityNumber = securityNumber;
        }

        public BigInteger getBigNumber() {
            return bigNumber;
        }

        public void setBigNumber(BigInteger bigNumber) {
            this.bigNumber = bigNumber;
        }

        public BigDecimal getCredit() {
            return credit;
        }

        public void setCredit(BigDecimal credit) {
            this.credit = credit;
        }

        public Double getAngle() {
            return angle;
        }

        public void setAngle(Double angle) {
            this.angle = angle;
        }
    }

    class TestDateTimeBean {
        private Date dateTime;

        public Date getDateTime() {
            return dateTime;
        }

        public void setDateTime(Date dateTime) {
            this.dateTime = dateTime;
        }
    }

    class TestDateTimePropertyEditor extends PropertyEditorSupport implements StructuredPropertyEditor {

        SimpleDateFormat dateFormat;

        public TestDateTimePropertyEditor(String format) {
            this.dateFormat = new SimpleDateFormat(format, Locale.US);
        }

        public List getRequiredFields() {
            List requiredFields = new ArrayList();
            requiredFields.add("date");
            return requiredFields;
        }

        public List getOptionalFields() {
            List optionalFields = new ArrayList();
            optionalFields.add("time");
            return optionalFields;
        }

        public TestDateTimeBean assemble(Class type, Map fieldValues) throws IllegalArgumentException {
            String date = (String) fieldValues.get("date");
            if (StringUtils.isBlank(date)) {
                return null;
            }

            String time = (String) fieldValues.get("time");
            if (StringUtils.isBlank(time)) time = "00:00 AM";

            String dateTime = date + " " + time;

            try {
                TestDateTimeBean dt = new TestDateTimeBean();
                dt.setDateTime(dateFormat.parse(dateTime));
                return dt;
            }
            catch (Exception nfe) {
                throw new IllegalArgumentException("Unable to parse structured DateTime from request for date (" + dateTime + ").");
            }
        }
    }

    public void testBindStructuredDateWithYearPrecision() throws Exception {
        testBindStructuredDate("2006", null, null, null, null); // January 1st, 2006 - 00:00
    }

    public void testBindStructuredDateWithMonthPrecision() throws Exception {
        testBindStructuredDate("1999", "1", null, null, null); // January 1st, 1999 - 00:00
        testBindStructuredDate("1999", "12", null, null, null); // December 1st, 1999 - 00:00
    }

    public void testAllowedAndDissallowedDefaultToEmptyArray() {
        TestBean testBean = new TestBean();
        GrailsDataBinder binder = new GrailsDataBinder(testBean,"testBean");
        assertNotNull(binder.getAllowedFields());
        assertEquals(0, binder.getAllowedFields().length);
        assertNotNull(binder.getDisallowedFields());
        assertEquals(2, binder.getDisallowedFields().length);
    }

    public void testBindStructuredDateWithDayPrecision() throws Exception {
        testBindStructuredDate("2012", "2", "1", null, null); // February 1, 2012 - 00:00
        testBindStructuredDate("2012", "2", "29", null, null); // February 29, 2012 - 00:00
    }

    public void testBindStructuredDateWithHourPrecision() throws Exception {
        testBindStructuredDate("2001", "8", "19", "0", null); // August 19, 2001 - 00:00
        testBindStructuredDate("2001", "8", "12", "0", null); // August 19, 2001 - 12:00
        testBindStructuredDate("2001", "8", "23", "0", null); // August 19, 2001 - 23:00
    }

    public void testBindStructuredDateWithMinutePrecision() throws Exception {
        testBindStructuredDate("2006", "6", "3", "1", "0"); // June 3rd, 2006 - 01:26
        testBindStructuredDate("2006", "6", "3", "1", "26"); // June 3rd, 2006 - 01:26
        testBindStructuredDate("2006", "6", "3", "1", "59"); // June 3rd, 2006 - 01:26
    }

    public void testBindStructuredDateWithNoYear() throws Exception {
        testBindInvalidStructuredDate("", "2", "1"); // February 1, ????
        testBindInvalidStructuredDate(null, "2", "29"); // February 29, ????
    }

    public void testBindStructuredDateTimeWithValidData() {
        TestBean bean = bindDateTimeWithCustomEditor("01/01/2009",  "01:00 PM");
        assertNotNull(bean.getMyDateTime());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.US);
        assertEquals("01/01/2009 01:00 PM", dateFormat.format(bean.getMyDateTime().getDateTime()));

        bean = bindDateTimeWithCustomEditor("01/01/2009",  "");
        assertNotNull(bean.getMyDateTime());
        assertEquals("01/01/2009 12:00 AM", dateFormat.format(bean.getMyDateTime().getDateTime()));
    }

    public void testBindStructuredDateTimeWithBlankRequiredFieldYieldsNullBinding() {
        TestBean bean = bindDateTimeWithCustomEditor("",  "01:00 PM");
        assertNull(bean.getMyDateTime());
    }

    private void testBindInvalidStructuredDate(String year, String month, String  day) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("myDate","struct");


        if (year!= null) {
            request.addParameter("myDate_year",year);
        }

        // If the month is null, we expect a default value of January
        if (month != null) {
            request.addParameter("myDate_month",month);
        }

        // If the day is null, we expect a default value of the 1st of the month
        if (day != null) {
            request.addParameter("myDate_day",day);
        }

        TestBean testBean = new TestBean();
        GrailsDataBinder binder = new GrailsDataBinder(testBean,"testBean");
        binder.bind(request);

        assertNull(testBean.getMyDate());
    }

    public void testFiltersRequestParams() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("joe.name","joe");
        request.addParameter("tom.name","tom");
        TestBean testBean = new TestBean();
        GrailsDataBinder binder = new GrailsDataBinder(testBean,"testBean");
        binder.bind(request, "joe");
        assertEquals("joe",testBean.getName());
    }

    public void testFiltersPropertyValues() {
        MutablePropertyValues vals = new MutablePropertyValues();
        vals.addPropertyValue("joe.name","joe");
        vals.addPropertyValue("tom.name","tom");
        TestBean testBean = new TestBean();
        GrailsDataBinder binder = new GrailsDataBinder(testBean,"testBean");
        binder.bind(vals, "tom");
        assertEquals("tom",testBean.getName());
    }

    static class Author {

        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public void testBindingWithPrefix() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addParameter("author1.name","Graeme Rocher");
        request.addParameter("author1.age","33");

        request.addParameter("author2.name","Marc Palmer");
        request.addParameter("author2.age","33");

        Author author1 = new Author();
        Author author2 = new Author();

        GrailsDataBinder binder1 = GrailsDataBinder.createBinder(author1, "graeme");
        binder1.bind(request, "author1");


        GrailsDataBinder binder2 = GrailsDataBinder.createBinder(author2, "marc");
        binder2.bind(request, "author2");

        assertEquals("Graeme Rocher", author1.getName());
        assertEquals(33, author1.getAge());
        assertEquals("Marc Palmer", author2.getName());
        assertEquals(33, author2.getAge());
   }

    public void testNumberBinding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.US);

        // JDK 1.4 and Spring 2.5.4 do not support localised parsing
        // of extra large numbers. Until we have resolved this problem,
        // we won't test a proper big integer.
        String securityNumber = "349,587,927,070";
        String bigNumber = "4,120,834,546";
        String credit = "1,203.45";
        String angle = "103.48674";
        request.addParameter("securityNumber", securityNumber);
        request.addParameter("bigNumber", bigNumber);
        request.addParameter("credit", credit);
        request.addParameter("angle", angle);

        TestBean testBean = new TestBean();
        GrailsDataBinder binder = GrailsDataBinder.createBinder(testBean, "testBean", request);
        binder.bind(request);

        assertEquals(349587927070L, testBean.getSecurityNumber().longValue());
        assertEquals(new BigInteger("4120834546"), testBean.getBigNumber());
        assertEquals(new BigDecimal("1203.45"), testBean.getCredit());
        assertEquals(103.48674D, testBean.getAngle().doubleValue(), 0.1D);

        // Now try German, which uses '.' and ',' instead of ',' and '.'.
        request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.GERMANY);

        securityNumber = "349.587.927.070";
        bigNumber = "4.120.834.546";
        credit = "1.203,45";
        angle = "103,48674";
        request.addParameter("securityNumber", securityNumber);
        request.addParameter("bigNumber", bigNumber);
        request.addParameter("credit", credit);
        request.addParameter("angle", angle);

        testBean = new TestBean();
        binder = GrailsDataBinder.createBinder(testBean, "testBean", request);
        binder.bind(request);

        assertEquals(349587927070L, testBean.getSecurityNumber().longValue());
        assertEquals(new BigInteger("4120834546"), testBean.getBigNumber());
        assertEquals(new BigDecimal("1203.45"), testBean.getCredit());
        assertEquals(103.48674D, testBean.getAngle().doubleValue(), 0.1D);
    }

    // Test an embedded type
    public static class Book {
        private String name;
        private Author author;

        // Make it embedded type
        static List embedded = new ArrayList(Arrays.asList("author"));

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Author getAuthor() { return author; }
        public void setAuthor(Author author) { this.author = author; }
    }

    // Test a list of beans which contain both simple and embedded types
    public static class TestCommand {
        private List<Book> books = ListUtils.lazyList(new ArrayList(), FactoryUtils.instantiateFactory(Book.class));
        public List<Book> getBooks() {
            return books;
        }
        public void setBooks(List<Book> books) {
            this.books = books;
        }
    }

    public void testBasicPropertyCollectionBinding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.US);

        request.addParameter("books[0].name", "Grails In Action 2");

        TestCommand instance = new TestCommand();
        GrailsDataBinder binder = GrailsDataBinder.createBinder(instance, "testCommand", request);
        binder.bind(request);

        assertEquals(1, instance.getBooks().size());
        assertEquals("Grails In Action 2", instance.getBooks().get(0).getName());
    }

    public void testEmbeddedCollectionBinding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPreferredLocale(Locale.US);

        request.addParameter("books[0].name", "Grails In Action 2");
        request.addParameter("books[0].author.name", "Aaron");

        TestCommand instance = new TestCommand();
        GrailsDataBinder binder = GrailsDataBinder.createBinder(instance, "testCommand", request);
        binder.bind(request);

        assertEquals(1, instance.getBooks().size());
        assertEquals("Grails In Action 2", instance.getBooks().get(0).getName());
        assertNotNull(instance.getBooks().get(0).getAuthor());
        assertEquals("Aaron", instance.getBooks().get(0).getAuthor().getName());
    }

    /**
     * Tests the <code>GrailsDataBinder</code> using the specified request parameters.  Assumes that each of the
     * specified request parameters is either null or a valid integer value for the given parameter.  Asserts that the
     * date dervied by the <code>GrailsDataBinder</code> corresponds correctly to the given request parameters.
     *
     * @param year a four-digit year value
     * @param month a month value between 1 (January) and 12 (December); or null
     * @param day a day value between 1 and 31; or null
     * @param hour an hour value between 0 and 23; or null
     * @param minute a minute value between 0 and 59; or null
     * @throws Exception
     */
    private void testBindStructuredDate(String year, String month, String  day, String  hour, String  minute) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("myDate","struct");

        // We assume that we always have at least a year value
        assertNotNull(year);
        int expectedYearValue = Integer.parseInt(year);
        request.addParameter("myDate_year",year);
        request.addParameter("mySqlDate_year",year);

        // If the month is null, we expect a default value of January
        int expectedMonthValue = 0;
        if (month != null) {
            request.addParameter("myDate_month",month);
            request.addParameter("mySqlDate_month",month);
            expectedMonthValue = Integer.parseInt(month)-1; // Subtract 1, because Calendar treats January as 0, February as 1, etc.
        }

        // If the day is null, we expect a default value of the 1st of the month
        int expectedDayValue = 1;
        if (day != null) {
            expectedDayValue = Integer.parseInt(day);
            request.addParameter("myDate_day",day);
            request.addParameter("mySqlDate_day",day);
        }

        // If the hour is null, we expect a default hour value of 00 (i.e., 12:00 AM)
        int expectedHourValue = 0;
        if (hour != null) {
            expectedHourValue = Integer.parseInt(hour);
            request.addParameter("myDate_hour",hour);
        }

        // If the day is null, we expect a default value of 0 minutes past the hour
        int expectedMinuteValue = 0;
        if (minute != null) {
            expectedMinuteValue = Integer.parseInt(minute);
            request.addParameter("myDate_minute",minute);
        }

        TestBean testBean = new TestBean();
        GrailsDataBinder binder = GrailsDataBinder.createBinder(testBean,"testBean",request);
        binder.bind(request);

        assertNotNull(testBean.getMyDate());
        Calendar c = new GregorianCalendar();
        c.setTime(testBean.getMyDate());

        assertEquals(expectedYearValue,c.get(Calendar.YEAR));
        assertEquals(expectedMonthValue,c.get(Calendar.MONTH));
        assertEquals(expectedDayValue,c.get(Calendar.DAY_OF_MONTH));
        assertEquals(expectedHourValue,c.get(Calendar.HOUR_OF_DAY));
        assertEquals(expectedMinuteValue,c.get(Calendar.MINUTE));

        c.setTime(testBean.getMySqlDate());

        assertEquals(expectedYearValue,c.get(Calendar.YEAR));
        assertEquals(expectedMonthValue,c.get(Calendar.MONTH));
        assertEquals(expectedDayValue,c.get(Calendar.DAY_OF_MONTH));
    }

    private TestBean bindDateTimeWithCustomEditor(String date, String time) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("myDateTime", "struct");
        request.addParameter("myDateTime_date", date);
        request.addParameter("myDateTime_time", time);

        TestBean testBean = new TestBean();
        GrailsDataBinder binder = GrailsDataBinder.createBinder(testBean, "testBean", request);
        binder.registerCustomEditor(TestDateTimeBean.class, new TestDateTimePropertyEditor(DATE_TIME_FORMAT));
        binder.bind(request);
        return testBean;
    }
}
