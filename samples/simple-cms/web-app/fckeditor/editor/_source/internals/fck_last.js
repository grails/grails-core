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
 * File Name: fck_last.js
 * 	These are the last script lines executed in the editor loading process.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

// This is the last file loaded to complete the editor initialization and activation

// Just check if the document direction has been correctly applied (at fck_onload.js).
if ( FCKLang && window.document.dir.toLowerCase() != FCKLang.Dir.toLowerCase() )
	window.document.dir = FCKLang.Dir ;
	
// Activate pasting operations.
if ( FCKConfig.ForcePasteAsPlainText || FCKConfig.AutoDetectPasteFromWord )
	FCK.Events.AttachEvent( "OnPaste", FCK.Paste ) ;

// START iCM Modifications
// Override default ENTER key handler for Gecko/Mozilla so 
// it acts more like IE browsers.
/*
if ( FCKBrowserInfo.IsGecko && !FCKConfig.UseBROnCarriageReturn )
{
	FCK.Events.AttachEvent( "OnEnter", FCK.Enter ) ;
	FCK.Events.AttachEvent( "OnBackSpace", FCK.BackSpace ) ;
}
*/
// END iCM Modifications

// Load Plugins.
if ( FCKPlugins.ItemsCount > 0 )
{
	FCKScriptLoader.OnEmpty = CompleteLoading ;
	FCKPlugins.Load() ;
}
else
	CompleteLoading() ;

function CompleteLoading()
{
	// Load the Toolbar
	FCKToolbarSet.Name = FCKURLParams['Toolbar'] || 'Default' ;
	FCKToolbarSet.Load( FCKToolbarSet.Name ) ;
	FCKToolbarSet.Restart() ;

	FCK.AttachToOnSelectionChange( FCKToolbarSet.RefreshItemsState ) ;
	//FCK.AttachToOnSelectionChange( FCKSelection._Reset ) ;

	FCKTools.DisableSelection( document.body ) ;

	FCK.SetStatus( FCK_STATUS_COMPLETE ) ;

	// Call the special "FCKeditor_OnComplete" function that should be present in 
	// the HTML page where the editor is located.
	if ( typeof( window.parent.FCKeditor_OnComplete ) == 'function' )
		window.parent.FCKeditor_OnComplete( FCK ) ;
}