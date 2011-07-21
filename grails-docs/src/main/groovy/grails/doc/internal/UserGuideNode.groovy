package grails.doc.internal

import groovy.transform.ToString

@ToString(excludes="parent, children")
class UserGuideNode {
    UserGuideNode parent
    List children = []

    String name
    String title
    String file
}

