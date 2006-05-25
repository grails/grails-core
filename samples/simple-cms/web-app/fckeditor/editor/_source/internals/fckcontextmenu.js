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
 * File Name: fckcontextmenu.js
 * 	Defines the FCKContextMenu object that is responsible for all
 * 	Context Menu operations.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

var FCKContextMenu = new Object() ;

FCKContextMenu._Panel = new FCKPanel( FCKBrowserInfo.IsIE ? window : window.parent ) ;
FCKContextMenu._Panel.PanelDiv.className = 'CM_ContextMenu' ;
FCKContextMenu._Panel.AppendStyleSheet( FCKConfig.SkinPath + 'fck_contextmenu.css' ) ;
FCKContextMenu._Panel.IsContextMenu = true ;

FCKContextMenu._Document = FCKContextMenu._Panel.Document ;

// This property is internally used to indicate that the context menu has been created.
FCKContextMenu._IsLoaded = false ;

FCKContextMenu.Show = function( x, y )
{
	if ( !this._IsLoaded )
		this.Reload() ;
	
	this.RefreshState() ;

	// If not IE, x and y are relative to the editing area, so we must "fix" it.
	if ( !FCKBrowserInfo.IsIE )
	{
		var oCoordsA = FCKTools.GetElementPosition( FCK.EditorWindow.frameElement, this._Panel._Window ) ;
		x += oCoordsA.X ;
		y += oCoordsA.Y ;
	}

	this._Panel.Show( x, y ) ;
}

FCKContextMenu.Hide = function()
{
	this._Panel.Hide() ;
}

// This method creates the context menu inside a DIV tag. Take a look at the end of this file for a sample output.
FCKContextMenu.Reload = function()
{
	// Create the Main DIV that holds the Context Menu.
//	this._Div = this._Document.createElement( 'DIV' ) ;
//	this._Div.className			= 'CM_ContextMenu' ;
//	this._Div.style.position	= 'absolute' ;
//	this._Div.style.visibility	= 'hidden' ;
//	this._Document.body.appendChild( this._Div );

	// Create the main table for the menu items.
	var oTable = this._Document.createElement( 'TABLE' ) ;
	oTable.cellSpacing = 0 ;
	oTable.cellPadding = 0 ;
	this._Panel.PanelDiv.appendChild( oTable ) ;
//	this._Div.appendChild( oTable ) ;

	// Load all configured groups.
	this.Groups = new Object() ;
	
	for ( var i = 0 ; i < FCKConfig.ContextMenu.length ; i++ )
	{
		var sGroup = FCKConfig.ContextMenu[i] ;
		this.Groups[ sGroup ] = this._GetGroup( sGroup ) ;
		this.Groups[ sGroup ].CreateTableRows( oTable ) ;
	}

	FCKTools.DisableSelection( this._Panel.Document.body ) ;

	this._IsLoaded = true ;
}

FCKContextMenu._GetGroup = function( groupName )
{
	var oGroup ;

	switch ( groupName )
	{
		case 'Generic' :
			// Generic items that are always available.
			oGroup = new FCKContextMenuGroup() ;

			oGroup.Add( new FCKContextMenuItem( this, 'Cut'		, FCKLang.Cut	, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'Copy'	, FCKLang.Copy	, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'Paste'	, FCKLang.Paste	, true ) ) ;

			break ;

		case 'Link' :
			oGroup = new FCKContextMenuGroup() ;

			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'Link'	, FCKLang.EditLink	, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'Unlink'	, FCKLang.RemoveLink, true ) ) ;

			break ;

		case 'TableCell' :
			oGroup = new FCKContextMenuGroup() ;

			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableInsertRow'		, FCKLang.InsertRow, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableDeleteRows'		, FCKLang.DeleteRows, true ) ) ;
			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableInsertColumn'	, FCKLang.InsertColumn, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableDeleteColumns'	, FCKLang.DeleteColumns, true ) ) ;
			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableInsertCell'		, FCKLang.InsertCell, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableDeleteCells'	, FCKLang.DeleteCells, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableMergeCells'		, FCKLang.MergeCells, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableSplitCell'		, FCKLang.SplitCell, true ) ) ;
			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableDelete'			, FCKLang.TableDelete, false ) ) ;
			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableCellProp'		, FCKLang.CellProperties, true ) ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableProp'			, FCKLang.TableProperties, true ) ) ;

			break ;

		case 'Table' :
			oGroup = new FCKContextMenuGroup() ;
			
			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'TableDelete'	, FCKLang.TableDelete, false ) ) ;
			oGroup.Add( new FCKContextMenuSeparator() ) ;
			oGroup.Add( new FCKContextMenuItem( this, 'Table'		, FCKLang.TableProperties, true ) ) ;
			
			break ;

		case 'Image' :
			return new FCKContextMenuGroup( true, this, 'Image', FCKLang.ImageProperties, true ) ;

		case 'Flash' :
			return new FCKContextMenuGroup( true, this, 'Flash', FCKLang.FlashProperties, true ) ;

		case 'Form' :
			return new FCKContextMenuGroup( true, this, 'Form', FCKLang.FormProp, true ) ;

		case 'Checkbox' :
			return new FCKContextMenuGroup( true, this, 'Checkbox', FCKLang.CheckboxProp, true ) ;

		case 'Radio' :
			return new FCKContextMenuGroup( true, this, 'Radio', FCKLang.RadioButtonProp, true ) ;

		case 'TextField' :
			return new FCKContextMenuGroup( true, this, 'TextField', FCKLang.TextFieldProp, true ) ;

		case 'HiddenField' :
			return new FCKContextMenuGroup( true, this, 'HiddenField', FCKLang.HiddenFieldProp, true ) ;

		case 'ImageButton' :
			return new FCKContextMenuGroup( true, this, 'ImageButton', FCKLang.ImageButtonProp, true ) ;

		case 'Button' :
			return new FCKContextMenuGroup( true, this, 'Button', FCKLang.ButtonProp, true ) ;

		case 'Select' :
			return new FCKContextMenuGroup( true, this, 'Select', FCKLang.SelectionFieldProp, true ) ;

		case 'Textarea' :
			return new FCKContextMenuGroup( true, this, 'Textarea', FCKLang.TextareaProp, true ) ;

		case 'BulletedList' :
			return new FCKContextMenuGroup( true, this, 'BulletedList', FCKLang.BulletedListProp, true ) ;

		case 'NumberedList' :
			return new FCKContextMenuGroup( true, this, 'NumberedList', FCKLang.NumberedListProp, true ) ;

		case 'Anchor' :
			return new FCKContextMenuGroup( true, this, 'Anchor', FCKLang.AnchorProp, true ) ;
	}
	
	return oGroup ;
}

FCKContextMenu.RefreshState = function()
{
  	// Get the actual selected tag (if any).
	var oTag = FCKSelection.GetSelectedElement() ;
	var sTagName ;

	if ( oTag )
		sTagName = oTag.tagName ;

	// Set items visibility.

//	var bIsAnchor = ( sTagName == 'A' && oTag.name.length > 0 && oTag.href.length == 0 ) ;

//	if ( this.Groups['Link'] )			this.Groups['Link'].SetVisible( !bIsAnchor && FCK.GetNamedCommandState( 'Unlink' ) != FCK_TRISTATE_DISABLED ) ;
	if ( this.Groups['Link'] )			this.Groups['Link'].SetVisible( FCK.GetNamedCommandState( 'Unlink' ) != FCK_TRISTATE_DISABLED ) ;

	if ( this.Groups['TableCell'] )		this.Groups['TableCell'].SetVisible( sTagName != 'TABLE' && FCKSelection.HasAncestorNode('TABLE') ) ;
	if ( this.Groups['Table'] )			this.Groups['Table'].SetVisible( sTagName == 'TABLE' ) ;
	
	if ( this.Groups['Image'] )			this.Groups['Image'].SetVisible( sTagName == 'IMG' && !oTag.getAttribute('_fckfakelement') ) ;
	if ( this.Groups['Flash'] )			this.Groups['Flash'].SetVisible( sTagName == 'IMG' && oTag.getAttribute('_fckflash') ) ;
	if ( this.Groups['Anchor'] )		this.Groups['Anchor'].SetVisible( sTagName == 'IMG' && oTag.getAttribute('_fckanchor') ) ;

	if ( this.Groups['BulletedList'] )	this.Groups['BulletedList'].SetVisible( FCKSelection.HasAncestorNode('UL') ) ;
	if ( this.Groups['NumberedList'] )	this.Groups['NumberedList'].SetVisible( FCKSelection.HasAncestorNode('OL') ) ;

	if ( this.Groups['Select'] )		this.Groups['Select'].SetVisible( sTagName == 'SELECT' ) ;
	if ( this.Groups['Textarea'] )		this.Groups['Textarea'].SetVisible( sTagName == 'TEXTAREA' ) ;
	if ( this.Groups['Form'] )			this.Groups['Form'].SetVisible( FCKSelection.HasAncestorNode('FORM') ) ;
	if ( this.Groups['Checkbox'] )		this.Groups['Checkbox'].SetVisible(		sTagName == 'INPUT' && oTag.type == 'checkbox' ) ;
	if ( this.Groups['Radio'] )			this.Groups['Radio'].SetVisible(		sTagName == 'INPUT' && oTag.type == 'radio' ) ;
	if ( this.Groups['TextField'] )		this.Groups['TextField'].SetVisible(	sTagName == 'INPUT' && ( oTag.type == 'text' || oTag.type == 'password' ) ) ;
	if ( this.Groups['HiddenField'] )	this.Groups['HiddenField'].SetVisible(	sTagName == 'INPUT' && oTag.type == 'hidden' ) ;
	if ( this.Groups['ImageButton'] )	this.Groups['ImageButton'].SetVisible(	sTagName == 'INPUT' && oTag.type == 'image' ) ;
	if ( this.Groups['Button'] )		this.Groups['Button'].SetVisible(		sTagName == 'INPUT' && ( oTag.type == 'button' || oTag.type == 'submit' || oTag.type == 'reset' ) ) ;

	// Refresh the state of all visible items (active/disactive)
	for ( var o in this.Groups )
	{
		this.Groups[o].RefreshState() ;
	}
}

/*
Sample Context Menu Output
-----------------------------------------
<div class="CM_ContextMenu">
	<table cellSpacing="0" cellPadding="0" border="0">
		<tr class="CM_Disabled">
			<td class="CM_Icon"><img alt="" src="icons/cut.gif" width="21" height="20"></td>
			<td class="CM_Label">Cut</td>
		</tr>
		<tr class="CM_Disabled">
			<td class="CM_Icon"><img height="20" alt="" src="icons/copy.gif" width="21"></td>
			<td class="CM_Label">Copy</td>
		</tr>
		<tr class="CM_Option" onmouseover="OnOver(this);" onmouseout="OnOut(this);">
			<td class="CM_Icon"><img height="20" alt="" src="icons/paste.gif" width="21"></td>
			<td class="CM_Label">Paste</td>
		</tr>
		<tr class="CM_Separator">
			<td class="CM_Icon"></td>
			<td class="CM_Label"><div></div></td>
		</tr>
		<tr class="CM_Option" onmouseover="OnOver(this);" onmouseout="OnOut(this);">
			<td class="CM_Icon"><img height="20" alt="" src="icons/print.gif" width="21"></td>
			<td class="CM_Label">Print</td>
		</tr>
		<tr class="CM_Separator">
			<td class="CM_Icon"></td>
			<td class="CM_Label"><div></div></td>
		</tr>
		<tr class="CM_Option" onmouseover="OnOver(this);" onmouseout="OnOut(this);">
			<td class="CM_Icon"></td>
			<td class="CM_Label">Do Something</td>
		</tr>
		<tr class="CM_Option" onmouseover="OnOver(this);" onmouseout="OnOut(this);">
			<td class="CM_Icon"></td>
			<td class="CM_Label">Just Testing</td>
		</tr>
	</table>
</div>
*/