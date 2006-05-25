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
 * File Name: fcktoolbarset.js
 * 	Defines the FCKToolbarSet object that is used to load and draw the 
 * 	toolbar.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKToolbarSet = FCK.ToolbarSet = new Object() ;

document.getElementById( 'ExpandHandle' ).title		= FCKLang.ToolbarExpand ;
document.getElementById( 'CollapseHandle' ).title	= FCKLang.ToolbarCollapse ;

FCKToolbarSet.Toolbars = new Array() ;

// Array of toolbat items that are active only on WYSIWYG mode.
FCKToolbarSet.ItemsWysiwygOnly = new Array() ;

// Array of toolbar items that are sensitive to the cursor position.
FCKToolbarSet.ItemsContextSensitive = new Array() ;

FCKToolbarSet.Expand = function()
{
	document.getElementById( 'Collapsed' ).style.display = 'none' ;
	document.getElementById( 'Expanded' ).style.display = '' ;
	
	if ( ! FCKBrowserInfo.IsIE )
	{
		// I had to use "setTimeout" because Gecko was not responding in a right
		// way when calling window.onresize() directly.
		window.setTimeout( "window.onresize()", 1 ) ;
	}
}

FCKToolbarSet.Collapse = function()
{
	document.getElementById( 'Collapsed' ).style.display = '' ;
	document.getElementById( 'Expanded' ).style.display = 'none' ;
	
	if ( ! FCKBrowserInfo.IsIE )
	{
		// I had to use "setTimeout" because Gecko was not responding in a right
		// way when calling window.onresize() directly.
		window.setTimeout( "window.onresize()", 1 ) ;
	}
}

FCKToolbarSet.Restart = function()
{
	if ( !FCKConfig.ToolbarCanCollapse || FCKConfig.ToolbarStartExpanded )
		this.Expand() ;
	else
		this.Collapse() ;
	
	document.getElementById( 'CollapseHandle' ).style.display = FCKConfig.ToolbarCanCollapse ? '' : 'none' ;
}

FCKToolbarSet.Load = function( toolbarSetName )
{
	this.DOMElement = document.getElementById( 'eToolbar' ) ;
	
	var ToolbarSet = FCKConfig.ToolbarSets[toolbarSetName] ;
	
	if (! ToolbarSet)
	{
		alert( FCKLang.UnknownToolbarSet.replace( /%1/g, toolbarSetName ) ) ;
		return ;
	}
	
	this.Toolbars = new Array() ;
	
	for ( var x = 0 ; x < ToolbarSet.length ; x++ ) 
	{
		var oToolbarItems = ToolbarSet[x] ;
		
		var oToolbar ;
		
		if ( typeof( oToolbarItems ) == 'string' )
		{
			if ( oToolbarItems == '/' )
				oToolbar = new FCKToolbarBreak() ;
		}
		else
		{
			oToolbar = new FCKToolbar() ;
			
			for ( var j = 0 ; j < oToolbarItems.length ; j++ ) 
			{
				var sItem = oToolbarItems[j] ;
				
				if ( sItem == '-')
					oToolbar.AddSeparator() ;
				else
				{
					var oItem = FCKToolbarItems.GetItem( sItem ) ;
					if ( oItem )
					{
						oToolbar.AddItem( oItem ) ;

						if ( !oItem.SourceView )
							this.ItemsWysiwygOnly[this.ItemsWysiwygOnly.length] = oItem ;
						
						if ( oItem.ContextSensitive )
							this.ItemsContextSensitive[this.ItemsContextSensitive.length] = oItem ;
					}
				}
			}
			
			oToolbar.AddTerminator() ;
		}

		this.Toolbars[ this.Toolbars.length ] = oToolbar ;
	}
}

FCKToolbarSet.RefreshModeState = function()
{
	if ( FCK.EditMode == FCK_EDITMODE_WYSIWYG )
	{
		// Enable all buttons that are available on WYSIWYG mode only.
		for ( var i = 0 ; i < FCKToolbarSet.ItemsWysiwygOnly.length ; i++ )
			FCKToolbarSet.ItemsWysiwygOnly[i].Enable() ;

		// Refresh the buttons state.
		FCKToolbarSet.RefreshItemsState() ;
	}
	else
	{
		// Refresh the buttons state.
		FCKToolbarSet.RefreshItemsState() ;

		// Disable all buttons that are available on WYSIWYG mode only.
		for ( var i = 0 ; i < FCKToolbarSet.ItemsWysiwygOnly.length ; i++ )
			FCKToolbarSet.ItemsWysiwygOnly[i].Disable() ;
	}	
}

FCKToolbarSet.RefreshItemsState = function()
{

	for ( var i = 0 ; i < FCKToolbarSet.ItemsContextSensitive.length ; i++ )
		FCKToolbarSet.ItemsContextSensitive[i].RefreshState() ;
/*
	TODO: Delete this commented block on stable version.
	for ( var i = 0 ; i < FCKToolbarSet.Toolbars.length ; i++ )
	{
		var oToolbar = FCKToolbarSet.Toolbars[i] ;
		for ( var j = 0 ; j < oToolbar.Items.length ; j++ )
		{
			oToolbar.Items[j].RefreshState() ;
		}
	}
*/
}
