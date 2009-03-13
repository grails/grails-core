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
        gcl.parseClass('''

class BidirectionalOneToManyAndCircularOneToManyUser implements java.io.Serializable {
    Long id
    Long version

	static hasMany = [users: BidirectionalOneToManyAndCircularOneToManyUser, uploads: BidirectionalOneToManyAndCircularOneToManyUpload, uploadLogs: BidirectionalOneToManyAndCircularOneToManyUploadLog]
    Set users
    Set uploads
    Set uploadLogs
	static mappedBy = [uploads: 'recipient', uploadLogs: 'sender']
	BidirectionalOneToManyAndCircularOneToManyUser manager
}
class BidirectionalOneToManyAndCircularOneToManyUploadLog implements java.io.Serializable {
    Long id
    Long version
	static belongsTo = BidirectionalOneToManyAndCircularOneToManyUser
	BidirectionalOneToManyAndCircularOneToManyUser sender
}
class BidirectionalOneToManyAndCircularOneToManyUpload implements java.io.Serializable {
    Long id
    Long version
	static belongsTo = [BidirectionalOneToManyAndCircularOneToManyUser]

	BidirectionalOneToManyAndCircularOneToManyUser sender
	BidirectionalOneToManyAndCircularOneToManyUser recipient
}

''')
    }


    void testBidirectionalOneToManyAndCircularOneToMany() {
        GrailsDomainClass userClass = ga.getDomainClass("BidirectionalOneToManyAndCircularOneToManyUser")
        GrailsDomainClass uploadClass = ga.getDomainClass("BidirectionalOneToManyAndCircularOneToManyUpload")
        GrailsDomainClass uploadLogClass = ga.getDomainClass("BidirectionalOneToManyAndCircularOneToManyUploadLog")


        assert userClass.getPropertyByName("users").isBidirectional()
        assert userClass.getPropertyByName("users").isCircular()

        def uploadsProperty = userClass.getPropertyByName("uploads")

        assert uploadsProperty.isBidirectional()
        assert uploadsProperty.isOneToMany()

        def recipientProperty = uploadClass.getPropertyByName("recipient")

        assert recipientProperty.isBidirectional()
        assert recipientProperty.isManyToOne()

        
        assertEquals uploadsProperty.getOtherSide(), recipientProperty
        assertEquals recipientProperty.getOtherSide(), uploadsProperty




        def uploadLogsProperty = userClass.getPropertyByName("uploadLogs")

        assert uploadLogsProperty.isBidirectional()
        assert uploadLogsProperty.isOneToMany()

        def senderProperty = uploadLogClass.getPropertyByName("sender")

        assert senderProperty.isBidirectional()
        assert senderProperty.isManyToOne()


        assertEquals uploadLogsProperty.getOtherSide(), senderProperty
        assertEquals senderProperty.getOtherSide(), uploadLogsProperty



        def uploadSenderProperty = uploadClass.getPropertyByName("sender")


        assert uploadSenderProperty.isOneToOne()
        assert !uploadSenderProperty.isBidirectional()

        
    }

}