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
 * File Name: fck_2.js
 * 	This is the second part of the "FCK" object creation. This is the main
 * 	object that represents an editor instance.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

// This collection is used by the browser specific implementations to tell
// wich named commands must be handled separately.
FCK.RedirectNamedCommands = new Object() ;

FCK.ExecuteNamedCommand = function( commandName, commandParameter, noRedirect )
{
	FCKUndo.SaveUndoStep() ;

	if ( !noRedirect && FCK.RedirectNamedCommands[ commandName ] != null )
		FCK.ExecuteRedirectedNamedCommand( commandName, commandParameter ) ;
	else
	{
		FCK.Focus() ;
		FCK.EditorDocument.execCommand( commandName, false, commandParameter ) ; 
		FCK.Events.FireEvent( 'OnSelectionChange' ) ;
	}
	
	FCKUndo.SaveUndoStep() ;
}

FCK.GetNamedCommandState = function( commandName )
{
	try
	{
		if ( !FCK.EditorDocument.queryCommandEnabled( commandName ) )
			return FCK_TRISTATE_DISABLED ;
		else
			return FCK.EditorDocument.queryCommandState( commandName ) ? FCK_TRISTATE_ON : FCK_TRISTATE_OFF ;
	}
	catch ( e )
	{
		return FCK_TRISTATE_OFF ;
	}
}

FCK.GetNamedCommandValue = function( commandName )
{
	var sValue = '' ;
	var eState = FCK.GetNamedCommandState( commandName ) ;
	
	if ( eState == FCK_TRISTATE_DISABLED ) 
		return null ;
	
	try
	{
		sValue = this.EditorDocument.queryCommandValue( commandName ) ;
	}
	catch(e) {}
	
	return sValue ? sValue : '' ;
}

FCK.PasteFromWord = function()
{
	FCKDialog.OpenDialog( 'FCKDialog_Paste', FCKLang.PasteFromWord, 'dialog/fck_paste.html', 400, 330, 'Word' ) ;
}

// TODO: Wait Stable and remove this block.
//FCK.CleanAndPaste = function( html )
//{
	// Remove all SPAN tags
//	html = html.replace(/<\/?SPAN[^>]*>/gi, "" );

//	html = html.replace(/<o:p>&nbsp;<\/o:p>/g, "") ;
//	html = html.replace(/<o:p><\/o:p>/g, "") ;
	
	// Remove mso-xxx styles.
//	html = html.replace( /mso-.[^:]:.[^;"]/g, "" ) ;
	
	// Remove Class attributes
//	html = html.replace(/<(\w[^>]*) class=([^ |>]*)([^>]*)/gi, "<$1$3") ;
	
	// Remove Style attributes
//	html = html.replace(/<(\w[^>]*) style="([^"]*)"([^>]*)/gi, "<$1$3") ;
	
	// Remove Lang attributes
//	html = html.replace(/<(\w[^>]*) lang=([^ |>]*)([^>]*)/gi, "<$1$3") ;
	
	// Remove XML elements and declarations
//	html = html.replace(/<\\?\?xml[^>]*>/gi, "") ;
	
	// Remove Tags with XML namespace declarations: <o:p></o:p>
//	html = html.replace(/<\/?\w+:[^>]*>/gi, "") ;
	
	// Replace the &nbsp;
//	html = html.replace(/&nbsp;/, " " );
	// Replace the &nbsp; from the beggining.
//	html = html.replace(/^&nbsp;[\s\r\n]*/, ""); 
	
	// Transform <P> to <DIV>
//	var re = new RegExp("(<P)([^>]*>.*?)(<\/P>)","gi") ;	// Different because of a IE 5.0 error
//	html = html.replace( re, "<div$2</div>" ) ;
	
//	FCK.InsertHtml( html ) ;
//}

FCK.Preview = function()
{
	var iWidth	= FCKConfig.ScreenWidth * 0.8 ;
	var iHeight	= FCKConfig.ScreenHeight * 0.7 ;
	var iLeft	= ( FCKConfig.ScreenWidth - iWidth ) / 2 ;
	var oWindow = window.open( '', null, 'toolbar=yes,location=no,status=yes,menubar=yes,scrollbars=yes,resizable=yes,width=' + iWidth + ',height=' + iHeight + ',left=' + iLeft ) ;
	
	var sHTML ;
	
	if ( FCKConfig.FullPage )
	{
		if ( FCK.TempBaseTag.length > 0 )
			sHTML = FCK.GetXHTML().replace( FCKRegexLib.HeadOpener, '$&' + FCK.TempBaseTag ) ;
		else
			sHTML = FCK.GetXHTML() ;
	}
	else
	{
		sHTML = 
			FCKConfig.DocType +
			'<html dir="' + FCKConfig.ContentLangDirection + '">' +
			'<head><title>' + FCKLang.Preview + '</title>' +
			'<link href="' + FCKConfig.EditorAreaCSS + '" rel="stylesheet" type="text/css" />' +
			FCK.TempBaseTag +
			'</head><body>' + 
			FCK.GetXHTML() + 
			'</body></html>' ;
	}
	
	oWindow.document.write( sHTML );
	oWindow.document.close();
}

FCK.SwitchEditMode = function()
{
	// Check if the actual mode is WYSIWYG.
	var bWYSIWYG = ( FCK.EditMode == FCK_EDITMODE_WYSIWYG ) ;
	
	// Display/Hide the TRs.
	document.getElementById('eWysiwyg').style.display	= bWYSIWYG ? 'none' : '' ;
	document.getElementById('eSource').style.display	= bWYSIWYG ? '' : 'none' ;

	// Update the HTML in the view output to show.
	if ( bWYSIWYG )
	{
		if ( FCKBrowserInfo.IsIE )
			FCKUndo.SaveUndoStep() ;

		// EnableXHTML and EnableSourceXHTML has been deprecated
//		document.getElementById('eSourceField').value = ( FCKConfig.EnableXHTML && FCKConfig.EnableSourceXHTML ? FCK.GetXHTML( FCKConfig.FormatSource ) : FCK.GetHTML( FCKConfig.FormatSource ) ) ;
		document.getElementById('eSourceField').value = FCK.GetXHTML( FCKConfig.FormatSource ) ;
	}
	else
		FCK.SetHTML( document.getElementById('eSourceField').value, true ) ;

	// Updates the actual mode status.
	FCK.EditMode = bWYSIWYG ? FCK_EDITMODE_SOURCE : FCK_EDITMODE_WYSIWYG ;
	
	// Update the toolbar.
	FCKToolbarSet.RefreshModeState() ;

	// Set the Focus.
	FCK.Focus() ;
}

FCK.CreateElement = function( tag )
{
	var e = FCK.EditorDocument.createElement( tag ) ;
	return FCK.InsertElementAndGetIt( e ) ;
}

FCK.InsertElementAndGetIt = function( e )
{
	e.setAttribute( '__FCKTempLabel', 1 ) ;
	
	this.InsertElement( e ) ;
	
	var aEls = FCK.EditorDocument.getElementsByTagName( e.tagName ) ;
	
	for ( var i = 0 ; i < aEls.length ; i++ )
	{
		if ( aEls[i].getAttribute( '__FCKTempLabel' ) )
		{
			aEls[i].removeAttribute( '__FCKTempLabel' ) ;
			return aEls[i] ;
		}
	}
	return null ;
}
