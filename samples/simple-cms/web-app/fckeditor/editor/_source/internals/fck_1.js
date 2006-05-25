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
 * File Name: fck_1.js
 * 	This is the first part of the "FCK" object creation. This is the main
 * 	object that represents an editor instance.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCK_StartupValue ;

FCK.Events	= new FCKEvents( FCK ) ;
FCK.Toolbar	= null ;

FCK.TempBaseTag = FCKConfig.BaseHref.length > 0 ? '<base href="' + FCKConfig.BaseHref + '" _fcktemp="true"></base>' : '' ;

FCK.StartEditor = function()
{
	// Get the editor's window and document (DOM)
	this.EditorWindow	= window.frames[ 'eEditorArea' ] ;
	this.EditorDocument	= this.EditorWindow.document ;

	// TODO: Wait stable version and remove the following commented lines.
	// The Base Path of the editor is saved to rebuild relative URL (IE issue).
//	this.BaseUrl = this.EditorDocument.location.protocol + '//' + this.EditorDocument.location.host ;

//	if ( FCKBrowserInfo.IsGecko )
//		this.MakeEditable() ;

	// Set the editor's startup contents
	this.SetHTML( FCKTools.GetLinkedFieldValue() ) ;
	
	// Save the startup value for the "IsDirty()" check.
	this.ResetIsDirty() ;

	// Attach the editor to the form onsubmit event
	FCKTools.AttachToLinkedFieldFormSubmit( this.UpdateLinkedField ) ;

	FCKUndo.SaveUndoStep() ;

	this.SetStatus( FCK_STATUS_ACTIVE ) ;
}

function Window_OnFocus()
{
	FCK.Focus() ;
	FCK.Events.FireEvent( "OnFocus" ) ;
}

function Window_OnBlur()
{
	if ( !FCKDialog.IsOpened )
		return FCK.Events.FireEvent( "OnBlur" ) ;
}

FCK.SetStatus = function( newStatus )
{
	this.Status = newStatus ;

	if ( newStatus == FCK_STATUS_ACTIVE )
	{
		// Force the focus in the window to go to the editor.
		window.frameElement.onfocus	= window.document.body.onfocus = Window_OnFocus ;
		window.frameElement.onblur	= Window_OnBlur ;

		// Force the focus in the editor.
		if ( FCKConfig.StartupFocus )
			FCK.Focus() ;

		// @Packager.Compactor.Remove.Start
		var sBrowserSuffix = FCKBrowserInfo.IsIE ? "ie" : "gecko" ;

		FCKScriptLoader.AddScript( '_source/internals/fck_2.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fck_2_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fckselection.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fckselection_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckpanel_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fcktablehandler.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fcktablehandler_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckxml_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckstyledef.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckstyledef_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckstylesloader.js' ) ;

		FCKScriptLoader.AddScript( '_source/commandclasses/fcknamedcommand.js' ) ;
		FCKScriptLoader.AddScript( '_source/commandclasses/fck_othercommands.js' ) ;
		FCKScriptLoader.AddScript( '_source/commandclasses/fckspellcheckcommand_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/commandclasses/fcktextcolorcommand.js' ) ;
		FCKScriptLoader.AddScript( '_source/commandclasses/fckpasteplaintextcommand.js' ) ;
		FCKScriptLoader.AddScript( '_source/commandclasses/fckpastewordcommand.js' ) ;
		FCKScriptLoader.AddScript( '_source/commandclasses/fcktablecommand.js' ) ;
		FCKScriptLoader.AddScript( '_source/commandclasses/fckstylecommand.js' ) ;

		FCKScriptLoader.AddScript( '_source/internals/fckcommands.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarbutton.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckspecialcombo.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarspecialcombo.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarfontscombo.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarfontsizecombo.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarfontformatcombo.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarstylecombo.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarpanelbutton.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fcktoolbaritems.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbar.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fcktoolbarbreak_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fcktoolbarset.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fckdialog.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fckdialog_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckcontextmenuitem.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckcontextmenuseparator.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckcontextmenugroup.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fckcontextmenu.js' ) ;
//		FCKScriptLoader.AddScript( '_source/internals/fckcontextmenu_' + sBrowserSuffix + '.js' ) ;
		FCKScriptLoader.AddScript( '_source/classes/fckplugin.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fckplugins.js' ) ;
		FCKScriptLoader.AddScript( '_source/internals/fck_last.js' ) ;
		// @Packager.Compactor.Remove.End

		/* @Packager.Compactor.RemoveLine

		if ( FCKBrowserInfo.IsIE )
			FCKScriptLoader.AddScript( 'js/fckeditorcode_ie_2.js' ) ;
		else
			FCKScriptLoader.AddScript( 'js/fckeditorcode_gecko_2.js' ) ;

		@Packager.Compactor.RemoveLine */
	}

	this.Events.FireEvent( 'OnStatusChange', newStatus ) ;
}

// Deprecated : returns the same value as GetXHTML.
FCK.GetHTML = function( format )
{
	FCK.GetXHTML( format ) ;
/*	
	var sHTML ;

	if ( FCK.EditMode == FCK_EDITMODE_WYSIWYG )
	{
		// TODO: Wait stable version and remove the following commented lines.
//		if ( FCKBrowserInfo.IsIE )
//			FCK.CheckRelativeLinks() ;

		if ( FCKBrowserInfo.IsIE )
			sHTML = this.EditorDocument.body.innerHTML.replace( FCKRegexLib.ToReplace, '$1' ) ;
		else
			sHTML = this.EditorDocument.body.innerHTML ;
	}
	else
		sHTML = document.getElementById('eSourceField').value ;

	if ( format )
		return FCKCodeFormatter.Format( sHTML ) ;
	else
		return sHTML ;
*/
}

FCK.GetXHTML = function( format )
{
	var bSource = ( FCK.EditMode == FCK_EDITMODE_SOURCE ) ;

	if ( bSource )
		this.SwitchEditMode() ;

	var sXHTML ;
	
	if ( FCKConfig.FullPage )
		sXHTML = FCKXHtml.GetXHTML( this.EditorDocument.getElementsByTagName( 'html' )[0], true, format ) ;
	else
	{
		if ( FCKConfig.IgnoreEmptyParagraphValue && this.EditorDocument.body.innerHTML == '<P>&nbsp;</P>' )
			sXHTML = '' ;
		else
			sXHTML = FCKXHtml.GetXHTML( this.EditorDocument.body, false, format ) ;
	}

	if ( bSource )
		this.SwitchEditMode() ;

	if ( FCKBrowserInfo.IsIE )
		sXHTML = sXHTML.replace( FCKRegexLib.ToReplace, '$1' ) ;

	if ( FCK.DocTypeDeclaration && FCK.DocTypeDeclaration.length > 0 )
		sXHTML = FCK.DocTypeDeclaration + '\n' + sXHTML ;

	if ( FCK.XmlDeclaration && FCK.XmlDeclaration.length > 0 )
		sXHTML = FCK.XmlDeclaration + '\n' + sXHTML ;

	return FCKConfig.ProtectedSource.Revert( sXHTML ) ;
}

FCK.UpdateLinkedField = function()
{
	// EnableXHTML has been deprecated
//	if ( FCKConfig.EnableXHTML )
		FCK.LinkedField.value = FCK.GetXHTML( FCKConfig.FormatOutput ) ;
//	else
//		FCK.LinkedField.value = FCK.GetHTML( FCKConfig.FormatOutput ) ;
		
	FCK.Events.FireEvent( 'OnAfterLinkedFieldUpdate' ) ;
}

FCK.ShowContextMenu = function( x, y )
{
	if ( this.Status != FCK_STATUS_COMPLETE )
		return ;

	FCKContextMenu.Show( x, y ) ;
	this.Events.FireEvent( "OnContextMenu" ) ;
}

FCK.RegisteredDoubleClickHandlers = new Object() ;

FCK.OnDoubleClick = function( element )
{
	var oHandler = FCK.RegisteredDoubleClickHandlers[ element.tagName ] ;
	if ( oHandler )
		oHandler( element ) ;
}

// Register objects that can handle double click operations.
FCK.RegisterDoubleClickHandler = function( handlerFunction, tag )
{
	FCK.RegisteredDoubleClickHandlers[ tag.toUpperCase() ] = handlerFunction ;
}

FCK.OnAfterSetHTML = function()
{
	var oProcessor, i = 0 ;
	while( ( oProcessor = FCKDocumentProcessors[i++] ) )
		oProcessor.ProcessDocument( FCK.EditorDocument ) ;

	this.Events.FireEvent( 'OnAfterSetHTML' ) ;
}

// Saves URLs on links and images on special attributes, so they don't change when 
// moving around.
FCK.ProtectUrls = function( html )
{
	// <A> href
	html = html.replace( FCKRegexLib.ProtectUrlsAApo	, '$1$2$3$2 _fcksavedurl=$2$3$2' ) ;
	html = html.replace( FCKRegexLib.ProtectUrlsANoApo	, '$1$2 _fcksavedurl="$2"' ) ;

	// <IMG> src
	html = html.replace( FCKRegexLib.ProtectUrlsImgApo	, '$1$2$3$2 _fcksavedurl=$2$3$2' ) ;
	html = html.replace( FCKRegexLib.ProtectUrlsImgNoApo, '$1$2 _fcksavedurl="$2"' ) ;
	
	return html ;
}

FCK.IsDirty = function()
{
	return ( FCK_StartupValue != FCK.EditorDocument.body.innerHTML ) ;
}

FCK.ResetIsDirty = function()
{
	if ( FCK.EditorDocument.body )
		FCK_StartupValue = FCK.EditorDocument.body.innerHTML ;
}

// Advanced document processors.

var FCKDocumentProcessors = new Array() ;

var FCKDocumentProcessors_CreateFakeImage = function( fakeClass, realElement )
{
	var oImg = FCK.EditorDocument.createElement( 'IMG' ) ;
	oImg.className = fakeClass ;
	oImg.src = FCKConfig.FullBasePath + 'images/spacer.gif' ;
	oImg.setAttribute( '_fckfakelement', 'true', 0 ) ;
	oImg.setAttribute( '_fckrealelement', FCKTempBin.AddElement( realElement ), 0 ) ;
	return oImg ;
}

// Link Anchors
var FCKAnchorsProcessor = new Object() ;
FCKAnchorsProcessor.ProcessDocument = function( document )
{
	var aLinks = document.getElementsByTagName( 'A' ) ;

	var oLink ;
	var i = aLinks.length - 1 ;
	while ( i >= 0 && ( oLink = aLinks[i--] ) )
	{
		// If it is anchor.
		if ( oLink.name.length > 0 && ( !oLink.getAttribute('href') || oLink.getAttribute('href').length == 0 ) )
		{
			var oImg = FCKDocumentProcessors_CreateFakeImage( 'FCK__Anchor', oLink.cloneNode(true) ) ;
			oImg.setAttribute( '_fckanchor', 'true', 0 ) ;
			
			oLink.parentNode.insertBefore( oImg, oLink ) ;
			oLink.parentNode.removeChild( oLink ) ;
		}
	}
}

FCKDocumentProcessors.addItem( FCKAnchorsProcessor ) ;

// Page Breaks
var FCKPageBreaksProcessor = new Object() ;
FCKPageBreaksProcessor.ProcessDocument = function( document )
{
	var aDIVs = document.getElementsByTagName( 'DIV' ) ;

	var eDIV ;
	var i = aDIVs.length - 1 ;
	while ( i >= 0 && ( eDIV = aDIVs[i--] ) )
	{
		if ( eDIV.style.pageBreakAfter == 'always' && eDIV.childNodes.length == 1 && eDIV.childNodes[0].style && eDIV.childNodes[0].style.display == 'none' )
		{
			var oFakeImage = FCKDocumentProcessors_CreateFakeImage( 'FCK__PageBreak', eDIV.cloneNode(true) ) ;
			
			eDIV.parentNode.insertBefore( oFakeImage, eDIV ) ;
			eDIV.parentNode.removeChild( eDIV ) ;
		}
	}
/*
	var aCenters = document.getElementsByTagName( 'CENTER' ) ;

	var oCenter ;
	var i = aCenters.length - 1 ;
	while ( i >= 0 && ( oCenter = aCenters[i--] ) )
	{
		if ( oCenter.style.pageBreakAfter == 'always' && oCenter.innerHTML.trim().length == 0 )
		{
			var oFakeImage = FCKDocumentProcessors_CreateFakeImage( 'FCK__PageBreak', oCenter.cloneNode(true) ) ;
			
			oCenter.parentNode.insertBefore( oFakeImage, oCenter ) ;
			oCenter.parentNode.removeChild( oCenter ) ;
		}
	}
*/
}

FCKDocumentProcessors.addItem( FCKPageBreaksProcessor ) ;

// Flash Embeds.
var FCKFlashProcessor = new Object() ;
FCKFlashProcessor.ProcessDocument = function( document )
{
	/*
	Sample code:
	This is some <embed src="/UserFiles/Flash/Yellow_Runners.swf"></embed><strong>sample text</strong>. You are&nbsp;<a name="fred"></a> using <a href="http://www.fckeditor.net/">FCKeditor</a>.
	*/

	var aEmbeds = document.getElementsByTagName( 'EMBED' ) ;

	var oEmbed ;
	var i = aEmbeds.length - 1 ;
	while ( i >= 0 && ( oEmbed = aEmbeds[i--] ) )
	{
		if ( oEmbed.src.endsWith( '.swf', true ) )
		{
			var oCloned = oEmbed.cloneNode( true ) ;
			
			// On IE, some properties are not getting clonned properly, so we 
			// must fix it. Thanks to Alfonso Martinez.
			if ( FCKBrowserInfo.IsIE )
			{
				oCloned.setAttribute( 'scale', oEmbed.getAttribute( 'scale' ) );
				oCloned.setAttribute( 'play', oEmbed.getAttribute( 'play' ) );
				oCloned.setAttribute( 'loop', oEmbed.getAttribute( 'loop' ) );
				oCloned.setAttribute( 'menu', oEmbed.getAttribute( 'menu' ) );
			}
		
			var oImg = FCKDocumentProcessors_CreateFakeImage( 'FCK__Flash', oCloned ) ;
			oImg.setAttribute( '_fckflash', 'true', 0 ) ;
			
			FCKFlashProcessor.RefreshView( oImg, oEmbed ) ;

			oEmbed.parentNode.insertBefore( oImg, oEmbed ) ;
			oEmbed.parentNode.removeChild( oEmbed ) ;

//			oEmbed.setAttribute( '_fckdelete', 'true', 0) ;
//			oEmbed.style.display = 'none' ;
//			oEmbed.hidden = true ;
		}
	}
}

FCKFlashProcessor.RefreshView = function( placholderImage, originalEmbed )
{
	if ( originalEmbed.width > 0 )
		placholderImage.style.width = FCKTools.ConvertHtmlSizeToStyle( originalEmbed.width ) ;
		
	if ( originalEmbed.height > 0 )
		placholderImage.style.height = FCKTools.ConvertHtmlSizeToStyle( originalEmbed.height ) ;
}

FCKDocumentProcessors.addItem( FCKFlashProcessor ) ;

FCK.GetRealElement = function( fakeElement )
{
	var e = FCKTempBin.Elements[ fakeElement.getAttribute('_fckrealelement') ] ;

	if ( fakeElement.getAttribute('_fckflash') )
	{
		if ( fakeElement.style.width.length > 0 )
				e.width = FCKTools.ConvertStyleSizeToHtml( fakeElement.style.width ) ;
		
		if ( fakeElement.style.height.length > 0 )
				e.height = FCKTools.ConvertStyleSizeToHtml( fakeElement.style.height ) ;
	}
	
	return e ;
}

// START iCM MODIFICATIONS
/*
var FCKTablesProcessor = new Object() ;
FCKTablesProcessor.ProcessDocument = function( document )
{
	FCKTablesProcessor.CheckTablesNesting( document ) ;
}

// Ensure that tables are not incorrectly nested within P, H1, H2, etc tags
FCKTablesProcessor.CheckTablesNesting = function( document )
{
	var aTables = document.getElementsByTagName( "TABLE" ) ;
	var oParentNode ;
	
	for ( var i=0; i<aTables.length; i++ )
	{
		FCKTablesProcessor.CheckTableNesting( aTables[i] ) ;
	}
}

// Corrects nesting of the supplied table as necessary.
// Also called by fck_table.html to check that a newly inserted table is correctly nested.
FCKTablesProcessor.CheckTableNesting = function( oTableNode )
{
	var oParentBlockNode = FCKTools.GetParentBlockNode( oTableNode.parentNode ) ;
	
	if ( oParentBlockNode && !FCKRegexLib.TableBlockElements.test( oParentBlockNode.nodeName ) )
	{
		// Create a new tag which holds the content of the child nodes located before the table
		var oNode1 = FCK.EditorDocument.createElement( oParentBlockNode.tagName ) ;
		var oFragment1 = FCKTools.GetDocumentFragment( oParentBlockNode, oParentBlockNode.firstChild, oTableNode, true, false, true ) ;
		oNode1.appendChild( oFragment1 ) ;
		FCKTools.SetElementAttributes( oNode1, oParentBlockNode.attributes ) ; 	// Transfer across any class attributes, etc
	
		// Create a new tag which holds the content of the child nodes located after the table
		var oNode2 = FCK.EditorDocument.createElement( oParentBlockNode.tagName );
		var oFragment2 = FCKTools.GetDocumentFragment( oParentBlockNode, oTableNode, oParentBlockNode.lastChild, false, true, true ) ;
		oNode2.appendChild( oFragment2 ) ;
		FCKTools.SetElementAttributes( oNode2, oParentBlockNode.attributes ) ; 	// Transfer across any class attributes, etc
		
		// Create a document fragment that contains the two new elements with the table element inbetween
		var oNewNode = FCK.EditorDocument.createDocumentFragment() ;
		if ( !FCKTools.NodeIsEmpty( oNode1 ) )
			oNewNode.appendChild( oNode1 ) ;
		oNewNode.appendChild( oTableNode ) ;
		if ( !FCKTools.NodeIsEmpty( oNode2 ) )
			oNewNode.appendChild( oNode2 ) ; 
		
		// Replace the existing parent node with the nodes in the fragment
		oParentBlockNode.parentNode.replaceChild( oNewNode, oParentBlockNode ) ;
	}
}		

FCKDocumentProcessors.addItem( FCKTablesProcessor ) ;
*/
// END iCM MODIFICATIONS