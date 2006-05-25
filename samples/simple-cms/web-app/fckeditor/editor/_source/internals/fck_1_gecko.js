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
 * File Name: fck_1_gecko.js
 * 	This is the first part of the "FCK" object creation. This is the main
 * 	object that represents an editor instance.
 * 	(Gecko specific implementations)
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

FCK.Description = "FCKeditor for Gecko Browsers" ;

FCK.InitializeBehaviors = function()
{
	/*
	window.document.onblur = function(e)
	{
		return FCK.Events.FireEvent( "OnBlur" ) ;
	}

	window.document.onfocus = function()
	{
		return FCK.Events.FireEvent( "OnFocus" ) ;
	}
	*/

	// Enable table borders visibility.
	if ( FCKConfig.ShowBorders ) 
	{
		var oStyle = FCKTools.AppendStyleSheet( this.EditorDocument, FCKConfig.FullBasePath + 'css/fck_showtableborders_gecko.css' ) ;
		oStyle.setAttribute( '_fcktemp', 'true' ) ;
	}

	// Disable Right-Click
	var oOnContextMenu = function( e )
	{
		e.preventDefault() ;
		FCK.ShowContextMenu( e.clientX, e.clientY ) ;
	}
	this.EditorDocument.addEventListener( 'contextmenu', oOnContextMenu, true ) ;

	// Handle pasting operations.
	var oOnKeyDown = function( e )
	{

		// START iCM Modifications
		/*
		// Need to amend carriage return key handling so inserts block element tags rather than BR all the time
		if ( e.which == 13 && !e.shiftKey && !e.ctrlKey && !e.altKey && !FCKConfig.UseBROnCarriageReturn && !FCK.Events.FireEvent( "OnEnter" ) )
		{
			e.preventDefault() ;
			e.stopPropagation() ;
		}
		// Amend backspace handling so correctly removes empty block elements i.e. those block elements containing nothing or
		// just the bogus BR node
		if ( e.which == 8 && !e.shiftKey && !e.ctrlKey && !e.altKey && !FCKConfig.UseBROnCarriageReturn && !FCK.Events.FireEvent( "OnBackSpace" ) )
		{
			e.preventDefault() ;
			e.stopPropagation() ;
		}
		*/
		// END iCM Modifications

		var bPrevent ;

		if ( e.ctrlKey && !e.shiftKey && !e.altKey )
		{
			switch ( e.which ) 
			{
				case 66 :	// B
				case 98 :	// b
					FCK.ExecuteNamedCommand( 'bold' ) ; bPrevent = true ;
					break;
				case 105 :	// i
				case 73 :	// I
					FCK.ExecuteNamedCommand( 'italic' ) ; bPrevent = true ;
					break;
				case 117 :	// u
				case 85 :	// U
					FCK.ExecuteNamedCommand( 'underline' ) ; bPrevent = true ;
					break;
				case 86 :	// V
				case 118 :	// v
					bPrevent = ( FCK.Status != FCK_STATUS_COMPLETE || !FCK.Events.FireEvent( "OnPaste" ) ) ;
					break ;
			}
		}
		else if ( e.shiftKey && !e.ctrlKey && !e.altKey && e.keyCode == 45 )	// SHIFT + <INS>
			bPrevent = ( FCK.Status != FCK_STATUS_COMPLETE || !FCK.Events.FireEvent( "OnPaste" ) ) ;
		
		if ( bPrevent ) 
		{
			e.preventDefault() ;
			e.stopPropagation() ;
		}
	}
	this.EditorDocument.addEventListener( 'keypress', oOnKeyDown, true ) ;

	this.ExecOnSelectionChange = function()
	{
		FCK.Events.FireEvent( "OnSelectionChange" ) ;
	}

	this.ExecOnSelectionChangeTimer = function()
	{
		if ( FCK.LastOnChangeTimer )
			window.clearTimeout( FCK.LastOnChangeTimer ) ;

		FCK.LastOnChangeTimer = window.setTimeout( FCK.ExecOnSelectionChange, 100 ) ;
	}

	this.EditorDocument.addEventListener( 'mouseup', this.ExecOnSelectionChange, false ) ;

	// On Gecko, firing the "OnSelectionChange" event on every key press started to be too much
	// slow. So, a timer has been implemented to solve performance issues when tipying to quickly.
	this.EditorDocument.addEventListener( 'keyup', this.ExecOnSelectionChangeTimer, false ) ;

	this._DblClickListener = function( e )
	{
		FCK.OnDoubleClick( e.target ) ;
		e.stopPropagation() ;
	}
	this.EditorDocument.addEventListener( 'dblclick', this._DblClickListener, true ) ;

	this._OnLoad = function()
	{
		if ( this._FCK_HTML )
		{
			this.document.body.innerHTML = this._FCK_HTML ;
			this._FCK_HTML = null ;
			
			if ( !FCK_StartupValue )
				FCK.ResetIsDirty() ;
		}
	}
	this.EditorWindow.addEventListener( 'load', this._OnLoad, true ) ;

//	var oEditorWindow_OnUnload = function()
//	{
//		FCK.EditorWindow.location = 'fckblank.html' ;
//	}
//	this.EditorWindow.addEventListener( 'unload', oEditorWindow_OnUnload, true ) ;

//	var oEditorWindow_OnFocus = function()
//	{
//		FCK.MakeEditable() ;
//	}
//	this.EditorWindow.addEventListener( 'focus', oEditorWindow_OnFocus, true ) ;
}

FCK.MakeEditable = function()
{
	try 
	{
		FCK.EditorDocument.designMode = 'on' ;

		// Tell Gecko to use or not the <SPAN> tag for the bold, italic and underline.
		FCK.EditorDocument.execCommand( 'useCSS', false, !FCKConfig.GeckoUseSPAN ) ;

		// Analysing Firefox 1.5 source code, it seams that there is support for a 
		// "insertBrOnReturn" command. Applying it gives no error, but it doesn't 
		// gives the same behavior that you have with IE. It works only if you are
		// already inside a paragraph and it doesn't render correctly in the first enter.
		// FCK.EditorDocument.execCommand( 'insertBrOnReturn', false, false ) ;
		
		// Tell Gecko (Firefox 1.5+) to enable or not live resizing of objects (by Alfonso Martinez)
		FCK.EditorDocument.execCommand( 'enableObjectResizing', false, !FCKConfig.DisableImageHandles ) ;
		
		// Disable the standard table editing features of Firefox.
		FCK.EditorDocument.execCommand( 'enableInlineTableEditing', false, !FCKConfig.DisableTableHandles ) ;
	}
	catch (e) {}
}

FCK.Focus = function()
{
	try
	{
//		window.focus() ;
		FCK.EditorWindow.focus() ;
	}
	catch(e) {}
}

// @Packager.Compactor.Remove.Start
if ( FCKBrowserInfo.IsSafari )
{
FCK.SetHTML = function( html, forceWYSIWYG )
{
	if( window.console ) window.console.log( 'FCK.SetHTML()' ) ;	// @Packager.Compactor.RemoveLine

	sHtml =
		FCKConfig.DocType +
		'<html dir="' + FCKConfig.ContentLangDirection + '">' +
		'<head><title></title>' +
		'<link href="' + FCKConfig.EditorAreaCSS + '" rel="stylesheet" type="text/css" />' +
		'<link href="' + FCKConfig.FullBasePath + 'css/fck_internal.css' + '" rel="stylesheet" type="text/css" _fcktemp="true" />' ;

//	sHtml += FCK.TempBaseTag ;
	sHtml += '</head><body>' + html  + '</body></html>' ;
	
	this.EditorDocument.open() ;
	this.EditorDocument.write( sHtml ) ;
	this.EditorDocument.close() ;

//	this.InitializeBehaviors() ;
	
//	FCK.MakeEditable() ;
	FCK.EditorDocument.designMode = 'on' ;
	FCK.OnAfterSetHTML() ;
}
}
else
{
// @Packager.Compactor.Remove.End
FCK.SetHTML = function( html, forceWYSIWYG )
{
	// Firefox can't handle correctly the editing of the STRONG and EM tags. 
	// We must replace them with B and I.
	html = html.replace( FCKRegexLib.StrongOpener, '<b$1' ) ;
	html = html.replace( FCKRegexLib.StrongCloser, '<\/b>' ) ;
	html = html.replace( FCKRegexLib.EmOpener, '<i$1' ) ;
	html = html.replace( FCKRegexLib.EmCloser, '<\/i>' ) ;

	if ( forceWYSIWYG || FCK.EditMode == FCK_EDITMODE_WYSIWYG )
	{
		html = FCKConfig.ProtectedSource.Protect( html ) ;
		html = FCK.ProtectUrls( html ) ;

		// Gecko has a lot of bugs mainly when handling editing features.
		// To avoid an Aplication Exception (that closes the browser!) we
		// must first write the <HTML> contents with an empty body, and
		// then insert the body contents.
		// (Oh yes... it took me a lot of time to find out this workaround)

		if ( FCKConfig.FullPage && FCKRegexLib.BodyContents.test( html ) )
		{
			// Add the <BASE> tag to the input HTML.
			if ( FCK.TempBaseTag.length > 0 && !FCKRegexLib.HasBaseTag.test( html ) )
				html = html.replace( FCKRegexLib.HeadOpener, '$&' + FCK.TempBaseTag ) ;

			html = html.replace( FCKRegexLib.HeadCloser, '<link href="' + FCKConfig.BasePath + 'css/fck_internal.css' + '" rel="stylesheet" type="text/css" _fcktemp="true" /></head>' ) ;

			// Extract the BODY contents from the html.
			var oMatch		= html.match( FCKRegexLib.BodyContents ) ;
			var sOpener		= oMatch[1] ;	// This is the HTML until the <body...> tag, inclusive.
			var sContents	= oMatch[2] ;	// This is the BODY tag contents.
			var sCloser		= oMatch[3] ;	// This is the HTML from the </body> tag, inclusive.

			var sHtml = sOpener + '&nbsp;' + sCloser ;

/*
			if ( !this._Initialized )
			{
				FCK.EditorDocument.designMode = "on" ;

				// Tell Gecko to use or not the <SPAN> tag for the bold, italic and underline.
				FCK.EditorDocument.execCommand( "useCSS", false, !FCKConfig.GeckoUseSPAN ) ;

				this._Initialized = true ;
			}
*/
			FCK.MakeEditable() ;

			this.EditorDocument.open() ;
			this.EditorDocument.write( sHtml ) ;
			this.EditorDocument.close() ;

			if ( this.EditorDocument.body )
				this.EditorDocument.body.innerHTML = sContents ;
			else
				this.EditorWindow._FCK_HTML = sContents ;

			this.InitializeBehaviors() ;
		}
		else
		{
			/* TODO: Wait stable and remove it.
			sHtml =
				'<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">' +
				'<html dir="' + FCKConfig.ContentLangDirection + '">' +
				'<head><title></title>' +
				'<link href="' + FCKConfig.EditorAreaCSS + '" rel="stylesheet" type="text/css" />' +
				'<link href="' + FCKConfig.BasePath + 'css/fck_internal.css' + '" rel="stylesheet" type="text/css" _fcktemp="true" />' ;

			sHtml += FCK.TempBaseTag ;

			sHtml += '</head><body>&nbsp;</body></html>' ;
			*/

			if ( !this._Initialized )
			{
				this.EditorDocument.dir = FCKConfig.ContentLangDirection ;

				var sHtml =
					'<title></title>' +
					'<link href="' + FCKConfig.EditorAreaCSS + '" rel="stylesheet" type="text/css" />' +
					'<link href="' + FCKConfig.BasePath + 'css/fck_internal.css' + '" rel="stylesheet" type="text/css" _fcktemp="true" />' +
					FCK.TempBaseTag ;

				this.EditorDocument.getElementsByTagName("HEAD")[0].innerHTML = sHtml ;

				this.InitializeBehaviors() ;

				this._Initialized = true ;
			}

			// On Gecko we must disable editing before setting the BODY innerHTML.
//			FCK.EditorDocument.designMode = 'off' ;

			if ( html.length == 0 )
				FCK.EditorDocument.body.innerHTML = GECKO_BOGUS ;
			else if ( FCKRegexLib.EmptyParagraph.test( html ) )
				FCK.EditorDocument.body.innerHTML = html.replace( FCKRegexLib.TagBody, '>' + GECKO_BOGUS + '<' ) ;
			else
				FCK.EditorDocument.body.innerHTML = html ;
			
			FCK.MakeEditable() ;
		}

		FCK.OnAfterSetHTML() ;
	}
	else
		document.getElementById('eSourceField').value = html ;
}
}	// @Packager.Compactor.RemoveLine