package org.codehaus.groovy.grails.orm.hibernate
import javax.persistence.*
import org.hibernate.annotations.*

@Entity
@Table(name="faq_element")
class FaqElement
{
    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    Long id

    @Version
    Long version

    String question
    String answer

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false, updatable = false, insertable = false)
    FaqSection section

}