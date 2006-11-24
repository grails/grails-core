
class BookmarkWebTest extends grails.util.WebTest {

    // Unlike unit tests, functional tests are often sequence dependent.
    // Specify that sequence here.
    void suite() {
        testBookmarkListNewDelete()
        // add tests for more operations here
    }

    def testBookmarkListNewDelete() {
        webtest('Bookmark basic operations: view list, create new entry, view, edit, delete, view'){
            invoke(url:'bookmark')
            verifyText(text:'Home')

            verifyListPage(0)

            clickLink(label:'New Bookmark')
            verifyText(text:'Create Bookmark')           
			setInputField(name:'url', 'http://grails.org')
			setInputField(name:'title', 'Grails')			
			
            clickButton(label:'Create')    

            verifyText(text:'Show Bookmark', description:'Detail page')
            clickLink(label:'List', description:'Back to list view')

            verifyListPage(1)

            group(description:'Edit the Bookmark') {
                clickLink(label:'Show', description:'go to detail view')
                clickButton(label:'Edit')
                verifyText(text:'Edit Bookmark')     	
				setInputField(name:'notes', value:'Test Notes')

                clickButton(label:'Update')
                verifyText(text:'Show Bookmark')         
				verifyText(text:'Test Notes')
                clickLink(label:'List', description:'Back to list view')
            }

            verifyListPage(1)

            group(description:'Delete the Bookmark') {
                clickLink(label:'Show', description:'go to detail view')
                clickButton(label:'Delete')
                verifyXPath(xpath:"//div[@class='message']", text:/Bookmark.*deleted./, regex:true)
            }

            verifyListPage(0)

    }   }

    String ROW_COUNT_XPATH = "count(//td[@class='actionButtons']/..)"

    def verifyListPage(int count) {
        ant.group(description:"verify Bookmark list view with $count row(s)"){
            verifyText(text:'Bookmark List')
            verifyXPath(xpath:ROW_COUNT_XPATH, text:count, description:"$count row(s) of data expected")
       }   
    }

}