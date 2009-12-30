package org.codehaus.groovy.grails.orm.hibernate

import javax.persistence.*

@Entity
@Table(name="faq_element")
class FaqElement
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Version
    Long version

    String question
    String answer

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false, updatable = false, insertable = false)
    FaqSection section
}