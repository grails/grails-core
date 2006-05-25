/*
 * FCKeditor - The text editor for internet
 * Copyright (C) 2003-2005 Frederico Caldeira Knabben
 * 
 * Licensed under the terms of the GNU Lesser General Public License:
 * 		http://www.opensource.org/licenses/lgpl-license.php
 * 
 * For further information visit:
 * 		http://www.fckeditor.net/
 * 
 * "Support Open Source software. What about a donation today?"
 * 
 * File Name: fcktools.js
 * 	Utility functions.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKTools = new Object() ;

//**
// FCKTools.GetLinkedFieldValue: Gets the value of the hidden INPUT element
// that is associated to the editor. This element has its ID set to the 
// editor's instance name so the user reffers to the instance name when getting
// the posted data.
FCKTools.GetLinkedFieldValue = function()
{
	return FCK.LinkedField.value ;
}

//**
// FCKTools.AttachToLinkedFieldFormSubmit: attaches a function call to the 
// submit event of the linked field form. This function us generally used to
// update the linked field value before submitting the form.
FCKTools.AttachToLinkedFieldFormSubmit = function( functionPointer )
{
	// Gets the linked field form
	var oForm = FCK.LinkedField.form ;
	
	// Return now if no form is available
	if (!oForm) return ;

	// Attaches the functionPointer call to the onsubmit event
	if ( FCKBrowserInfo.IsIE )
		oForm.attachEvent( "onsubmit", functionPointer ) ;
	else
		oForm.addEventListener( 'submit', functionPointer, true ) ;
	
	//**
	// Attaches the functionPointer call to the submit method 
	// This is done because IE doesn't fire onsubmit when the submit method is called
	// BEGIN --
	
	// Creates a Array in the form object that will hold all Attached function call
	// (in the case there are more than one editor in the same page)
	if (! oForm.updateFCKeditor) oForm.updateFCKeditor = new Array() ;
	
	// Adds the function pointer to the array of functions to call when "submit" is called
	oForm.updateFCKeditor[oForm.updateFCKeditor.length] = functionPointer ;

	// Switches the original submit method with a new one that first call all functions
	// on the above array and the call the original submit
	// IE sees it oForm.submit function as an 'object'.
	if (! oForm.originalSubmit && ( typeof( oForm.submit ) == 'function' || ( !oForm.submit.tagName && !oForm.submit.length ) ) )
	{
		// Creates a copy of the original submit
		oForm.originalSubmit = oForm.submit ;
		
		// Creates our replacement for the submit
		oForm.submit = FCKTools_SubmitReplacer ;
	}
	// END --
}

function FCKTools_SubmitReplacer()
{
	if (this.updateFCKeditor)
	{
		// Calls all functions in the functions array
		for (var i = 0 ; i < this.updateFCKeditor.length ; i++)
			this.updateFCKeditor[i]() ;
	}
	// Calls the original "submit"
	this.originalSubmit() ;
}

//**
// FCKTools.AddSelectOption: Adds a option to a SELECT element.
FCKTools.AddSelectOption = function( targetDocument, selectElement, optionText, optionValue )
{
	var oOption = targetDocument.createElement("OPTION") ;

	oOption.text	= optionText ;
	oOption.value	= optionValue ;	

	selectElement.options.add(oOption) ;

	return oOption ;
}
/*
FCKTools.RemoveAllSelectOptions = function( selectElement )
{
	for ( var i = selectElement.options.length - 1 ; i >= 0 ; i-- )
	{
		selectElement.options.remove(i) ;
	}
}

FCKTools.SelectNoCase = function( selectElement, value, defaultValue )
{
	var sNoCaseValue = value.toString().toLowerCase() ;
	
	for ( var i = 0 ; i < selectElement.options.length ; i++ )
	{
		if ( sNoCaseValue == selectElement.options[i].value.toLowerCase() )
		{
			selectElement.selectedIndex = i ;
			return ;
		}
	}
	
	if ( defaultValue != null ) FCKTools.SelectNoCase( selectElement, defaultValue ) ;
}
*/
FCKTools.HTMLEncode = function( text )
{
	if ( !text )
		return '' ;

	text = text.replace( /&/g, "&amp;" ) ;
	text = text.replace( /"/g, "&quot;" ) ;
	text = text.replace( /</g, "&lt;" ) ;
	text = text.replace( />/g, "&gt;" ) ;
	text = text.replace( /'/g, "&#39;" ) ;

	return text ;
}
/*
//**
// FCKTools.GetResultingArray: Gets a array from a string (where the elements 
// are separated by a character), a fuction (that returns a array) or a array.
FCKTools.GetResultingArray = function( arraySource, separator )
{
	switch ( typeof( arraySource ) )
	{
		case "string" :
			return arraySource.split( separator ) ;
		case "function" :
			return separator() ;
		default :
			if ( isArray( arraySource ) ) return arraySource ;
			else return new Array() ;
	}
}
*/
FCKTools.GetElementPosition = function( el, relativeWindow )
{
// Initializes the Coordinates object that will be returned by the function.
	var c = { X:0, Y:0 } ;
	
	var oWindow = relativeWindow || window ;

	// Loop throw the offset chain.
	while ( el )
	{
		c.X += el.offsetLeft ;
		c.Y += el.offsetTop ;

		if ( el.offsetParent == null )
		{
			var oOwnerWindow = FCKTools.GetElementWindow( el ) ;
			
			if ( oOwnerWindow != oWindow )
				el = oOwnerWindow.frameElement ;
			else
				break ;
		}
		else
			el = el.offsetParent ;
	}

	// Return the Coordinates object
	return c ;
}

// START iCM MODIFICATIONS
// Amended to accept a list of one or more ascensor tag names
// Amended to check the element itself before working back up through the parent hierarchy
FCKTools.GetElementAscensor = function( element, ascensorTagNames )
{
//	var e = element.parentNode ;
	var e = element ;
	var lstTags = "," + ascensorTagNames.toUpperCase() + "," ;

	while ( e )
	{
		if ( lstTags.indexOf( "," + e.nodeName.toUpperCase() + "," ) != -1 )
			return e ;

		e = e.parentNode ;
	}
	return null ;
}
// END iCM MODIFICATIONS

FCKTools.Pause = function( miliseconds )
{
	var oStart = new Date() ;

	while (true)
	{ 
		var oNow = new Date() ;
		if ( miliseconds < oNow - oStart ) 
			return ;
	}
}

FCKTools.ConvertStyleSizeToHtml = function( size )
{
	return size.endsWith( '%' ) ? size : parseInt( size ) ;
}

FCKTools.ConvertHtmlSizeToStyle = function( size )
{
	return size.endsWith( '%' ) ? size : ( size + 'px' ) ;
}

// Get the window object where the element is placed in.
FCKTools.GetElementWindow = function( element )
{
	var oDocument = element.ownerDocument || element.document ;
	
	// With Safari, there is not way to retrieve the window from the document, so we must fix it.
	if ( FCKBrowserInfo.IsSafari && !oDocument.parentWindow )
		FCKTools._FixDocumentParentWindow( window.top ) ;
	
	return oDocument.parentWindow || oDocument.defaultView ;
}

/*
	This is a Safari specific function that fix the reference to the parent 
	window from the document object.
*/
FCKTools._FixDocumentParentWindow = function( targetWindow )
{
	targetWindow.document.parentWindow = targetWindow ; 
	
	for ( var i = 0 ; i < targetWindow.frames.length ; i++ )
		FCKTools._FixDocumentParentWindow( targetWindow.frames[i] ) ;
}

FCKTools.CancelEvent = function( e )
{
	return false ;
}

// START iCM MODIFICATIONS
/*
// Transfers the supplied attributes to the supplied node
FCKTools.SetElementAttributes = function( oElement, oAttributes ) 
{
	for ( var i = 0; i < oAttributes.length; i++ ) 
	{
		if ( oAttributes[i].specified ) // Needed for IE which always returns all attributes whether set or not
			oElement.setAttribute( oAttributes[i].nodeName, oAttributes[i].nodeValue, 0 ) ;
	}
}

// Get immediate block node (P, H1, for example) for the supplied node - the supplied node may itself be a block node in which
// case it will be returned. If no block node found, returns null.
FCKTools.GetParentBlockNode = function( oNode )
{
	if ( oNode.nodeName.toUpperCase() == "BODY" )
		return null ;
	else if ( oNode.nodeType == 1 && FCKRegexLib.BlockElements.test(oNode.tagName) )
		return oNode ;
	else
		return FCKTools.GetParentBlockNode( oNode.parentNode ) ;
}

// Run through any children of the supplied node. If there are none, or they only comprise 
// empty text nodes and BR nodes, then the node is effectively empty.
// Sometimes (on Gecko) a seemingly empty node is coming back with several children that are solely
// empty text nodes and BRs e.g. the first item in an OL list, for example, when 
// UseBROnCarriageReturn is set to false. 
// Seems to be due to the use of the <br _moz_editor_bogus_node="TRUE"> (GECKO_BOGUS) as fillers both
// in fck_gecko_1.js when html is empty and in ENTER key handler ? If normal BR tags are
// used instead this doesn't seem to happen....
FCKTools.NodeIsEmpty = function( oNode )
{
	var oSibling = oNode.childNodes[0] ;
	while ( oSibling )
	{
		if ( ( oSibling.nodeType != 1 && oSibling.nodeType != 3 ) || ( oSibling.nodeType == 1 && oSibling.nodeName.toUpperCase() != "BR" ) || ( oSibling.nodeType == 3 && oSibling.nodeValue && oSibling.nodeValue.trim() != '' ) )
			return false ;
		
		oSibling = oSibling.nextSibling ;
	}

	return true ;
}

// Returns a document fragment that contains a copy of the specified range of nodes
FCKTools.GetDocumentFragment = function( oParentNode, oFromNode, oToNode, bIncludeFromNode, bIncludeToNode, bClone )
{	
	if ( typeof bIncludeFromNode == "undefined" )  bIncludeFromNode = true ;
	if ( typeof bIncludeToNode == "undefined" )  bIncludeToNode = true ;
	if ( typeof bClone == "undefined" )  bClone = true ;

	var oFragment = FCK.EditorDocument.createDocumentFragment() ;
	
	var oNode = oFromNode ;
	while ( oNode && oNode != oToNode )
	{
		if ( oNode != oFromNode || bIncludeFromNode )
			oFragment.appendChild( bClone ? oNode.cloneNode( true ) : oNode ) ;
			
		oNode = oNode.nextSibling ;
	}

	if ( oNode && (oFromNode != oToNode && bIncludeToNode) )
		oFragment.appendChild( bClone ? oNode.cloneNode( true ) : oNode ) ; // Include To Node

	return oFragment ;
}
*/
// END iCM MODIFICATIONS
