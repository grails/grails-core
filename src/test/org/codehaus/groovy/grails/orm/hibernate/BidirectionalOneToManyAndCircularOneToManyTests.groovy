package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Apr 9, 2008
 */
class BidirectionalOneToManyAndCircularOneToManyTests extends AbstractGrailsMockTests {

    protected void onSetUp() {
        gcl.parseClass '''

class BidirectionalOneToManyAndCircularOneToManyUser implements java.io.Serializable {
    Long id
    Long version

   static hasMany = [users: BidirectionalOneToManyAndCircularOneToManyUser,
                     uploads: BidirectionalOneToManyAndCircularOneToManyUpload,
                     uploadLogs: BidirectionalOneToManyAndCircularOneToManyUploadLog]
    Set users
    Set uploads
    Set uploadLogs
    static mappedBy = [uploads: 'recipient', uploadLogs: 'sender']
    BidirectionalOneToManyAndCircularOneToManyUser manager
}

class BidirectionalOneToManyAndCircularOneToManyUploadLog implements Serializable {
    Long id
    Long version
    static belongsTo = BidirectionalOneToManyAndCircularOneToManyUser
    BidirectionalOneToManyAndCircularOneToManyUser sender
}

class BidirectionalOneToManyAndCircularOneToManyUpload implements Serializable {
    Long id
    Long version
    static belongsTo = [BidirectionalOneToManyAndCircularOneToManyUser]

    BidirectionalOneToManyAndCircularOneToManyUser sender
    BidirectionalOneToManyAndCircularOneToManyUser recipient
}
'''
    }

    void testBidirectionalOneToManyAndCircularOneToMany() {
        GrailsDomainClass userClass = ga.getDomainClass("BidirectionalOneToManyAndCircularOneToManyUser")
        GrailsDomainClass uploadClass = ga.getDomainClass("BidirectionalOneToManyAndCircularOneToManyUpload")
        GrailsDomainClass uploadLogClass = ga.getDomainClass("BidirectionalOneToManyAndCircularOneToManyUploadLog")

        assertTrue userClass.getPropertyByName("users").isBidirectional()
        assertTrue userClass.getPropertyByName("users").isCircular()

        def uploadsProperty = userClass.getPropertyByName("uploads")

        assertTrue uploadsProperty.isBidirectional()
        assertTrue uploadsProperty.isOneToMany()

        def recipientProperty = uploadClass.getPropertyByName("recipient")

        assertTrue recipientProperty.isBidirectional()
        assertTrue recipientProperty.isManyToOne()

        assertEquals uploadsProperty.getOtherSide(), recipientProperty
        assertEquals recipientProperty.getOtherSide(), uploadsProperty

        def uploadLogsProperty = userClass.getPropertyByName("uploadLogs")

        assertTrue uploadLogsProperty.isBidirectional()
        assertTrue uploadLogsProperty.isOneToMany()

        def senderProperty = uploadLogClass.getPropertyByName("sender")

        assertTrue senderProperty.isBidirectional()
        assertTrue senderProperty.isManyToOne()

        assertEquals uploadLogsProperty.getOtherSide(), senderProperty
        assertEquals senderProperty.getOtherSide(), uploadLogsProperty

        def uploadSenderProperty = uploadClass.getPropertyByName("sender")

        assertTrue uploadSenderProperty.isOneToOne()
        assertFalse uploadSenderProperty.isBidirectional()
    }
}
