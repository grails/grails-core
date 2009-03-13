package org.codehaus.groovy.grails.orm.hibernate

import javax.persistence.*
import org.hibernate.annotations.*

@Entity
@Table(name="faq_section")
class FaqSection
{
    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    Long id

    @Version
    Long version

    String title

    @OneToMany(cascade = [javax.persistence.CascadeType.ALL], targetEntity = FaqElement.class)
    @JoinColumn(name = "section_id", nullable = false)
    @IndexColumn(name = "pos", base = 0)
    List elements
}