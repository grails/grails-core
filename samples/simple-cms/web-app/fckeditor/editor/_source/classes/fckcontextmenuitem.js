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
 * File Name: fckcontextmenuitem.js
 * 	FCKContextMenuItem Class: represents a item in the context menu.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKContextMenuItem = function( contextMenu, commandName, label, hasIcon )
{
	this.ContextMenu	= contextMenu ;
	this.Command		= FCKCommands.GetCommand( commandName ) ;
	this.Label			= label ? label : commandName ;
	this.HasIcon		= hasIcon ? true : false ;
}

function FCKContextMenuItem_OnMouseOver()
{
	if ( this.className != 'CM_Disabled' )
		this.className = 'CM_Over' ;
}
	
function FCKContextMenuItem_OnMouseOut()
{
	if ( this.className != 'CM_Disabled' )
		this.className = 'CM_Option' ;
}
	
function FCKContextMenuItem_OnClick()
{
	if ( this.className != 'CM_Disabled' )
	{
		this.FCKContextMenuItem.ContextMenu.Hide() ;
		this.FCKContextMenuItem.Command.Execute() ;
	}
	return false ;
}

FCKContextMenuItem.prototype.CreateTableRow = function( targetTable )
{
	// Creates the <TR> element.
	this._Row = targetTable.insertRow(-1) ;
	this._Row.className = 'CM_Disabled' ;
	this._Row.FCKContextMenuItem = this ;
	
	this._Row.onmouseover	= FCKContextMenuItem_OnMouseOver ;
	this._Row.onmouseout	= FCKContextMenuItem_OnMouseOut ;
	this._Row.onclick		= FCKContextMenuItem_OnClick ;
	
	var oCell = this._Row.insertCell(-1) ;
	oCell.className = 'CM_Icon' ;
	
	if ( this.HasIcon ) oCell.innerHTML = '<img alt="" src="' + FCKConfig.SkinPath + 'toolbar/' + this.Command.Name.toLowerCase() + '.gif" width="21" height="20">' ;
	
	oCell = this._Row.insertCell(-1) ;
	oCell.className		= 'CM_Label' ;
	oCell.noWrap		= true ;
	oCell.innerHTML		= this.Label ;
}

FCKContextMenuItem.prototype.SetVisible = function( isVisible )
{
	this._Row.style.display = isVisible ? '' : 'none' ;
}

FCKContextMenuItem.prototype.RefreshState = function()
{
	switch ( this.Command.GetState() )
	{
		case FCK_TRISTATE_ON :
		case FCK_TRISTATE_OFF :
			this._Row.className = 'CM_Option' ;
			break ;
		default :
			this._Row.className = 'CM_Disabled' ;
			break ;
	}
} 

/*
Sample output.
-----------------------------------------
<tr class="CM_Disabled">
	<td class="CM_Icon"><img alt="" src="icons/cut.gif" width="21" height="20"></td>
	<td class="CM_Label">Cut</td>
</tr>
-----------------------------------------
<tr class="CM_Option" onmouseover="OnOver(this);" onmouseout="OnOut(this);">
	<td class="CM_Icon"></td>
	<td class="CM_Label">Do Something</td>
</tr>
*/