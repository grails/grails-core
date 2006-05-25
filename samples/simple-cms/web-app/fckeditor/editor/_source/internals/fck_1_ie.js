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
 * File Name: fck_1_ie.js
 * 	This is the first part of the "FCK" object creation. This is the main
 * 	object that represents an editor instance.
 * 	(IE specific implementations)
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

FCK.Description = "FCKeditor for Internet Explorer 5.5+" ;

// The behaviors should be pointed using the FullBasePath to avoid security
// errors when using a differente BaseHref.
FCK._BehaviorsStyle =
	'<style type="text/css" _fcktemp="true"> \
		INPUT { behavior: url(' + FCKConfig.FullBasePath + 'css/behaviors/hiddenfield.htc) ; } ' ;

if ( FCKConfig.ShowBorders )
	FCK._BehaviorsStyle += 'TABLE { behavior: url(' + FCKConfig.FullBasePath + 'css/behaviors/showtableborders.htc) ; }' ;

// Disable resize handlers.
var sNoHandlers = 'INPUT, TEXTAREA, SELECT, .FCK__Anchor, .FCK__PageBreak' ;

if ( FCKConfig.DisableImageHandles )
	sNoHandlers += ', IMG' ;

if ( FCKConfig.DisableTableHandles )
	sNoHandlers += ', TABLE' ;

FCK._BehaviorsStyle += sNoHandlers + ' { behavior: url(' + FCKConfig.FullBasePath + 'css/behaviors/disablehandles.htc) ; }' ;

FCK._BehaviorsStyle += '</style>' ;

function Doc_OnMouseUp()
{
	if ( FCK.EditorWindow.event.srcElement.tagName == 'HTML' )
	{
		FCK.Focus() ;
		FCK.EditorWindow.event.cancelBubble	= true ;
		FCK.EditorWindow.event.returnValue	= false ;
	}
}

function Doc_OnPaste()
{
	if ( FCK.Status == FCK_STATUS_COMPLETE )
		return FCK.Events.FireEvent( "OnPaste" ) ;
	else
		return false ;
}

function Doc_OnContextMenu()
{
	var e = FCK.EditorWindow.event ;
	
	FCK.ShowContextMenu( e.screenX, e.screenY ) ;
	return false ;
}

function Doc_OnKeyDown()
{
	var e = FCK.EditorWindow.event ;


	switch ( e.keyCode )
	{
		case 13 :	// ENTER
			if ( FCKConfig.UseBROnCarriageReturn && !(e.ctrlKey || e.altKey || e.shiftKey) )
			{
				Doc_OnKeyDownUndo() ;
				
				// We must ignore it if we are inside a List.
				if ( FCK.EditorDocument.queryCommandState( 'InsertOrderedList' ) || FCK.EditorDocument.queryCommandState( 'InsertUnorderedList' ) )
					return true ;

				// Insert the <BR> (The &nbsp; must be also inserted to make it work)
				FCK.InsertHtml( '<br>&nbsp;' ) ;

				// Remove the &nbsp;
				var oRange = FCK.EditorDocument.selection.createRange() ;
				oRange.moveStart( 'character', -1 ) ;
				oRange.select() ;
				FCK.EditorDocument.selection.clear() ;

				return false ;
			}
			break ;
		
		case 8 :	// BACKSPACE
			// We must delete a control selection by code and cancels the 
			// keystroke, otherwise IE will execute the browser's "back" button.
			if ( FCKSelection.GetType() == 'Control' )
			{
				FCKSelection.Delete() ;
				return false ;
			}
			break ;
		
		case 9 :	// TAB
			if ( FCKConfig.TabSpaces > 0 && !(e.ctrlKey || e.altKey || e.shiftKey) )
			{
				Doc_OnKeyDownUndo() ;
				
				FCK.InsertHtml( window.FCKTabHTML ) ;
				return false ;
			}
			break ;
		case 90 :	// Z
			if ( e.ctrlKey && !(e.altKey || e.shiftKey) )
			{
				FCKUndo.Undo() ;
				return false ;
			}
			break ;
		case 89 :	// Y
			if ( e.ctrlKey && !(e.altKey || e.shiftKey) )
			{
				FCKUndo.Redo() ;
				return false ;
			}
			break ;
	}
	
	if ( !( e.keyCode >=16 && e.keyCode <= 18 ) )
		Doc_OnKeyDownUndo() ;
	return true ;
}

function Doc_OnKeyDownUndo()
{
	if ( !FCKUndo.Typing )
	{
		FCKUndo.SaveUndoStep() ;
		FCKUndo.Typing = true ;
		FCK.Events.FireEvent( "OnSelectionChange" ) ;
	}
	
	FCKUndo.TypesCount++ ;

	if ( FCKUndo.TypesCount > FCKUndo.MaxTypes )
	{
		FCKUndo.TypesCount = 0 ;
		FCKUndo.SaveUndoStep() ;
	}
}

function Doc_OnDblClick()
{
	FCK.OnDoubleClick( FCK.EditorWindow.event.srcElement ) ;
	FCK.EditorWindow.event.cancelBubble = true ;
}

function Doc_OnSelectionChange()
{
	FCK.Events.FireEvent( "OnSelectionChange" ) ;
}

FCK.InitializeBehaviors = function( dontReturn )
{
	// Set the focus to the editable area when clicking in the document area.
	// TODO: The cursor must be positioned at the end.
	this.EditorDocument.attachEvent( 'onmouseup', Doc_OnMouseUp ) ;

	// Intercept pasting operations
	this.EditorDocument.body.attachEvent( 'onpaste', Doc_OnPaste ) ;

	// Disable Right-Click and shows the context menu.
	this.EditorDocument.attachEvent('oncontextmenu', Doc_OnContextMenu ) ;

	// Build the "TAB" key replacement (if necessary).
	if ( FCKConfig.TabSpaces > 0 )
	{
		window.FCKTabHTML = '' ;
		for ( i = 0 ; i < FCKConfig.TabSpaces ; i++ )
			window.FCKTabHTML += "&nbsp;" ;
	}
	this.EditorDocument.attachEvent("onkeydown", Doc_OnKeyDown ) ;

	this.EditorDocument.attachEvent("ondblclick", Doc_OnDblClick ) ;

	// Catch cursor movements
	this.EditorDocument.attachEvent("onselectionchange", Doc_OnSelectionChange ) ;

	//Enable editing
//	this.EditorDocument.body.contentEditable = true ;
}

FCK.Focus = function()
{
	try
	{
		if ( FCK.EditMode == FCK_EDITMODE_WYSIWYG )
			FCK.EditorDocument.body.focus() ;
		else
			document.getElementById('eSourceField').focus() ;
	}
	catch(e) {}
}

FCK.SetHTML = function( html, forceWYSIWYG )
{
	if ( forceWYSIWYG || FCK.EditMode == FCK_EDITMODE_WYSIWYG )
	{
		html = FCKConfig.ProtectedSource.Protect( html ) ;
		html = FCK.ProtectUrls( html ) ;

		var sHtml ;

		if ( FCKConfig.FullPage )
		{
			var sHtml =
				FCK._BehaviorsStyle +
				'<link href="' + FCKConfig.FullBasePath + 'css/fck_internal.css' + '" rel="stylesheet" type="text/css" _fcktemp="true" />' ;

			if ( FCK.TempBaseTag.length > 0 && !FCKRegexLib.HasBaseTag.test( html ) )
				sHtml += FCK.TempBaseTag ;

			sHtml = html.replace( FCKRegexLib.HeadOpener, '$&' + sHtml ) ;
		}
		else
		{
			sHtml =
				FCKConfig.DocType +
				'<html dir="' + FCKConfig.ContentLangDirection + '"' ;
			
			if ( FCKConfig.IEForceVScroll )
				sHtml += ' style="overflow-y: scroll"' ;
			
			sHtml +=
				'><head><title></title>' +
				'<link href="' + FCKConfig.EditorAreaCSS + '" rel="stylesheet" type="text/css" />' +
				'<link href="' + FCKConfig.FullBasePath + 'css/fck_internal.css' + '" rel="stylesheet" type="text/css" _fcktemp="true" />' ;

			sHtml += FCK._BehaviorsStyle ;
			sHtml += FCK.TempBaseTag ;
			sHtml += '</head><body>' + html  + '</body></html>' ;
		}

//		this.EditorDocument.open( '', '_self', '', true ) ;		// This one opens popups in IE 5.5 - BUG 1204220 (I was not able to reproduce the problem).
		this.EditorDocument.open( '', 'replace' ) ;
		this.EditorDocument.write( sHtml ) ;
		this.EditorDocument.close() ;

		this.InitializeBehaviors() ;
		this.EditorDocument.body.contentEditable = true ;

		FCK.OnAfterSetHTML() ;
	}
	else
		document.getElementById('eSourceField').value = html ;
}

FCK.InsertHtml = function( html )
{
	html = FCKConfig.ProtectedSource.Protect( html ) ;
	html = FCK.ProtectUrls( html ) ;

	FCK.Focus() ;

	FCKUndo.SaveUndoStep() ;

	// Gets the actual selection.
	var oSel = FCK.EditorDocument.selection ;

	// Deletes the actual selection contents.
	if ( oSel.type.toLowerCase() != "none" )
		oSel.clear() ;

	// Insert the HTML.
	oSel.createRange().pasteHTML( html ) ;
}

FCK.SetInnerHtml = function( html )		// IE Only
{
	var oDoc = FCK.EditorDocument ;
	// Using the following trick, any comment in the begining of the HTML will
	// be preserved.
	oDoc.body.innerHTML = '<div id="__fakeFCKRemove__">&nbsp;</div>' + html ;
	oDoc.getElementById('__fakeFCKRemove__').removeNode( true ) ;
}