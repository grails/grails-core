package org.grails.web.converters;

/**
 * Created by jameskleeh on 2/7/17.
 */
public class Author {
    private String name;
    private Integer age;

    Author(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
