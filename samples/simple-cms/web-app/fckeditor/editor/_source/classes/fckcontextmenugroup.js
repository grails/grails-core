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
 * File Name: fckcontextmenugroup.js
 * 	FCKContextMenuGroup Class: represents a group of items in the context 
 * 	menu. Generaly a group of items is directly dependent of the same rules.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKContextMenuGroup = function( addSeparator, contextMenu, firstItemCommand, firstItemLabel, hasIcon )
{
	this.IsVisible = true ;
	
	// Array with all available context menu items of this group.
	this.Items = new Array() ;
	
	if ( addSeparator )
		this.Add( new FCKContextMenuSeparator() ) ;
	
	if ( contextMenu && firstItemCommand && firstItemLabel )
		this.Add( new FCKContextMenuItem( contextMenu, firstItemCommand, firstItemLabel, hasIcon ) ) ;

	// This OPTIONAL function checks if the group must be shown.
	this.ValidationFunction = null ;
}

// Adds an item to the group's items collecion.
FCKContextMenuGroup.prototype.Add = function( contextMenuItem )
{
	this.Items[ this.Items.length ] = contextMenuItem ;
}

// Creates the <TR> elements that represent the item in a table (usually the rendered context menu).
FCKContextMenuGroup.prototype.CreateTableRows = function( table )
{
	for ( var i = 0 ; i < this.Items.length ; i++ )
	{
		this.Items[i].CreateTableRow( table ) ;
	}
}

FCKContextMenuGroup.prototype.SetVisible = function( isVisible )
{
	for ( var i = 0 ; i < this.Items.length ; i++ )
	{
		this.Items[i].SetVisible( isVisible ) ;
	}
	
	this.IsVisible = isVisible ;
}

FCKContextMenuGroup.prototype.RefreshState = function()
{
	if ( ! this.IsVisible ) return ;
	
	for ( var i = 0 ; i < this.Items.length ; i++ )
	{
		this.Items[i].RefreshState() ;
	}
}