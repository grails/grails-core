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
 * File Name: fckplugin.js
 * 	This plugin register the required Toolbar items to be able to insert the
 * 	toolbar commands in the toolbar.
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

FCKToolbarItems.RegisterItem( 'TableInsertRow'		, new FCKToolbarButton( 'TableInsertRow'	, FCKLang.InsertRow ) ) ;
FCKToolbarItems.RegisterItem( 'TableDeleteRows'		, new FCKToolbarButton( 'TableDeleteRows'	, FCKLang.DeleteRows ) ) ;
FCKToolbarItems.RegisterItem( 'TableInsertColumn'	, new FCKToolbarButton( 'TableInsertColumn'	, FCKLang.InsertColumn ) ) ;
FCKToolbarItems.RegisterItem( 'TableDeleteColumns'	, new FCKToolbarButton( 'TableDeleteColumns', FCKLang.DeleteColumns ) ) ;
FCKToolbarItems.RegisterItem( 'TableInsertCell'		, new FCKToolbarButton( 'TableInsertCell'	, FCKLang.InsertCell ) ) ;
FCKToolbarItems.RegisterItem( 'TableDeleteCells'	, new FCKToolbarButton( 'TableDeleteCells'	, FCKLang.DeleteCells ) ) ;
FCKToolbarItems.RegisterItem( 'TableMergeCells'		, new FCKToolbarButton( 'TableMergeCells'	, FCKLang.MergeCells ) ) ;
FCKToolbarItems.RegisterItem( 'TableSplitCell'		, new FCKToolbarButton( 'TableSplitCell'	, FCKLang.SplitCell ) ) ;