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
 * File Name: ca.js
 * 	Catalan language file.
 * 
 * File Authors:
 * 		Jordi Cerdan (nan@myp.ad)
 */

var FCKLang =
{
// Language direction : "ltr" (left to right) or "rtl" (right to left).
Dir					: "ltr",

ToolbarCollapse		: "Col·lapsar barra",
ToolbarExpand		: "Expandir barra",

// Toolbar Items and Context Menu
Save				: "Guardar",
NewPage				: "Nova Pàgina",
Preview				: "Vista Prèvia",
Cut					: "Tallar",
Copy				: "Copiar",
Paste				: "Enganxar",
PasteText			: "Enganxar com text planer",
PasteWord			: "Enganxar des de Word",
Print				: "Imprimir",
SelectAll			: "Seleccionar tot",
RemoveFormat		: "Eliminar Format",
InsertLinkLbl		: "Enllaç",
InsertLink			: "Afegir/Editar Enllaç",
RemoveLink			: "Eliminar Enllaç",
Anchor				: "Afegir/Editar Àncora",
InsertImageLbl		: "Imatge",
InsertImage			: "Afegir/Editar Imatge",
InsertFlashLbl		: "Flash",
InsertFlash			: "Afegir/Editar Flash",
InsertTableLbl		: "Taula",
InsertTable			: "Afegir/Editar Taula",
InsertLineLbl		: "Línia",
InsertLine			: "Afegir Línia Horitzontal",
InsertSpecialCharLbl: "Caràcter Especial",
InsertSpecialChar	: "Afegir Caràcter Especial",
InsertSmileyLbl		: "Icona",
InsertSmiley		: "Afegir Icona",
About				: "Sobre FCKeditor",
Bold				: "Negreta",
Italic				: "Itàlica",
Underline			: "Subratllat",
StrikeThrough		: "Tatxat",
Subscript			: "Subscript",
Superscript			: "Superscript",
LeftJustify			: "Justificar Esquerra",
CenterJustify		: "Justificar Centrat",
RightJustify		: "Justificar Dreta",
BlockJustify		: "Justificar Bloc",
DecreaseIndent		: "Disminuir Indentació",
IncreaseIndent		: "Augmentar Indentació",
Undo				: "Desfer",
Redo				: "Refer",
NumberedListLbl		: "Llista Numerada",
NumberedList		: "Afegir/Eliminar Llista Numerada",
BulletedListLbl		: "Llista Marcada",
BulletedList		: "Afegir/Eliminar Llista Marcada",
ShowTableBorders	: "Mostrar Costats de Taules",
ShowDetails			: "Mostrar Detalls",
Style				: "Estil",
FontFormat			: "Format",
Font				: "Font",
FontSize			: "Tamany",
TextColor			: "Color de Text",
BGColor				: "Color de Fons",
Source				: "Font",
Find				: "Cercar",
Replace				: "Remplaçar",
SpellCheck			: "Verificar sintaxi",
UniversalKeyboard	: "Teclat universal",
PageBreakLbl		: "Page Break",	//MISSING
PageBreak			: "Insert Page Break",	//MISSING

Form			: "Formulari",
Checkbox		: "Casella de verificació",
RadioButton		: "Botó ràdio",
TextField		: "Camp de text",
Textarea		: "Àrea de text",
HiddenField		: "Camp ocult",
Button			: "botó",
SelectionField	: "Camp de selecció",
ImageButton		: "Botó imatge",

// Context Menu
EditLink			: "Editar Enllaç",
InsertRow			: "Afegir Fila",
DeleteRows			: "Eliminar Files",
InsertColumn		: "Afegir Columna",
DeleteColumns		: "Eliminar Columnes",
InsertCell			: "Afegir Cel·la",
DeleteCells			: "Eliminar Cel·les",
MergeCells			: "Fusionar Cel·les",
SplitCell			: "Separar Cel·les",
TableDelete			: "Delete Table",	//MISSING
CellProperties		: "Proprietats de Cel·la",
TableProperties		: "Proprietats de Taula",
ImageProperties		: "Proprietats d'Image",
FlashProperties		: "Proprietats Flash",

AnchorProp			: "Proprietats d'àncora",
ButtonProp			: "Proprietats de botó",
CheckboxProp		: "Proprietats de casella de verificació",
HiddenFieldProp		: "Proprietats de camp ocult",
RadioButtonProp		: "Proprietats de botó ràdio",
ImageButtonProp		: "Proprietats de botó imatge",
TextFieldProp		: "Proprietats de camp de text",
SelectionFieldProp	: "Proprietats de camp de selecció",
TextareaProp		: "Proprietats de camp de text",
FormProp			: "Proprietats de formulari",

FontFormats			: "Normal;Formatejat;Adreça;Capçalera 1;Capçalera 2;Capçalera 3;Capçalera 4;Capçalera 5;Capçalera 6",

// Alerts and Messages
ProcessingXHTML		: "Processant XHTML. Si us plau esperi...",
Done				: "Fet",
PasteWordConfirm	: "El text que voleu enganxar sembla provenir de Word. Voleu netejar aquest text abans que sigui enganxat?",
NotCompatiblePaste	: "Aquesta funció és disponible per a Internet Explorer versió 5.5 o superior. Voleu enganxar sense netejar?",
UnknownToolbarItem	: "Element de la Barra d'eines desconegut \"%1\"",
UnknownCommand		: "Nom de comanda desconegut \"%1\"",
NotImplemented		: "Mètode no implementat",
UnknownToolbarSet	: "Conjunt de barra d'eines \"%1\" inexistent",
NoActiveX			: "You browser's security settings could limit some features of the editor. You must enable the option \"Run ActiveX controls and plug-ins\". You may experience errors and notice missing features.",	//MISSING
BrowseServerBlocked : "The resources browser could not be opened. Make sure that all popup blockers are disabled.",	//MISSING
DialogBlocked		: "It was not possible to open the dialog window. Make sure all popup blockers are disabled.",	//MISSING

// Dialogs
DlgBtnOK			: "OK",
DlgBtnCancel		: "Cancelar",
DlgBtnClose			: "Tancar",
DlgBtnBrowseServer	: "Veure servidor",
DlgAdvancedTag		: "Avançat",
DlgOpOther			: "Altres",
DlgInfoTab			: "Info",
DlgAlertUrl			: "Si us plau, afegiu la URL",

// General Dialogs Labels
DlgGenNotSet		: "&lt;no definit&gt;",
DlgGenId			: "Id",
DlgGenLangDir		: "Direcció Idioma",
DlgGenLangDirLtr	: "Esquerra a Dreta (LTR)",
DlgGenLangDirRtl	: "Dreta a Esquerra (RTL)",
DlgGenLangCode		: "Codi de Llengua",
DlgGenAccessKey		: "Clau d'accés",
DlgGenName			: "Nom",
DlgGenTabIndex		: "Index de Tab",
DlgGenLongDescr		: "Descripció Llarga URL",
DlgGenClass			: "Classes del Full d'Estils",
DlgGenTitle			: "Títol Consultiu",
DlgGenContType		: "Tipus de Contingut Consultiu",
DlgGenLinkCharset	: "Conjunt de Caràcters Font Enllaçat",
DlgGenStyle			: "Estil",

// Image Dialog
DlgImgTitle			: "Proprietats d'Imatge",
DlgImgInfoTab		: "Informació d'Imatge",
DlgImgBtnUpload		: "Enviar-la al servidor",
DlgImgURL			: "URL",
DlgImgUpload		: "Pujar",
DlgImgAlt			: "Text Alternatiu",
DlgImgWidth			: "Amplada",
DlgImgHeight		: "Alçada",
DlgImgLockRatio		: "Bloquejar Proporcions",
DlgBtnResetSize		: "Restaurar Tamany",
DlgImgBorder		: "Costat",
DlgImgHSpace		: "HSpace",
DlgImgVSpace		: "VSpace",
DlgImgAlign			: "Alineació",
DlgImgAlignLeft		: "Left",
DlgImgAlignAbsBottom: "Abs Bottom",
DlgImgAlignAbsMiddle: "Abs Middle",
DlgImgAlignBaseline	: "Baseline",
DlgImgAlignBottom	: "Bottom",
DlgImgAlignMiddle	: "Middle",
DlgImgAlignRight	: "Right",
DlgImgAlignTextTop	: "Text Top",
DlgImgAlignTop		: "Top",
DlgImgPreview		: "Vista Prèvia",
DlgImgAlertUrl		: "Si us plau, escriviu la URL de la imatge",
DlgImgLinkTab		: "Enllaç",

// Flash Dialog
DlgFlashTitle		: "Propietats Flash",
DlgFlashChkPlay		: "Reprodució Automàtica",
DlgFlashChkLoop		: "Bucle",
DlgFlashChkMenu		: "Habilitar Menu Flash",
DlgFlashScale		: "Escala",
DlgFlashScaleAll	: "Mostrar tot",
DlgFlashScaleNoBorder	: "Sense Costats",
DlgFlashScaleFit	: "Mida exacta",

// Link Dialog
DlgLnkWindowTitle	: "Enllaç",
DlgLnkInfoTab		: "Informació d'Enllaç",
DlgLnkTargetTab		: "Destí",

DlgLnkType			: "Tipus de Link",
DlgLnkTypeURL		: "URL",
DlgLnkTypeAnchor	: "Àncora en aquesta pàgina",
DlgLnkTypeEMail		: "E-Mail",
DlgLnkProto			: "Protocol",
DlgLnkProtoOther	: "&lt;altra&gt;",
DlgLnkURL			: "URL",
DlgLnkAnchorSel		: "Seleccionar una àncora",
DlgLnkAnchorByName	: "Per nom d'àncora",
DlgLnkAnchorById	: "Per Id d'element",
DlgLnkNoAnchors		: "&lt;No hi ha àncores disponibles en aquest document&gt;",
DlgLnkEMail			: "Adreça d'E-Mail",
DlgLnkEMailSubject	: "Subjecte del Missatge",
DlgLnkEMailBody		: "Cos del Missatge",
DlgLnkUpload		: "Pujar",
DlgLnkBtnUpload		: "Enviar al Servidor",

DlgLnkTarget		: "Destí",
DlgLnkTargetFrame	: "&lt;marc&gt;",
DlgLnkTargetPopup	: "&lt;finestra popup&gt;",
DlgLnkTargetBlank	: "Nova Finestra (_blank)",
DlgLnkTargetParent	: "Finestra Pare (_parent)",
DlgLnkTargetSelf	: "Mateixa Finestra (_self)",
DlgLnkTargetTop		: "Finestra Major (_top)",
DlgLnkTargetFrameName	: "Nom del marc de destí",
DlgLnkPopWinName	: "Nom Finestra Popup",
DlgLnkPopWinFeat	: "Característiques Finestra Popup",
DlgLnkPopResize		: "Redimensionable",
DlgLnkPopLocation	: "Barra d'Adreça",
DlgLnkPopMenu		: "Barra de Menú",
DlgLnkPopScroll		: "Barres d'Scroll",
DlgLnkPopStatus		: "Barra d'Estat",
DlgLnkPopToolbar	: "Barra d'Eines",
DlgLnkPopFullScrn	: "Pantalla completa (IE)",
DlgLnkPopDependent	: "Depenent (Netscape)",
DlgLnkPopWidth		: "Amplada",
DlgLnkPopHeight		: "Alçada",
DlgLnkPopLeft		: "Posició Esquerra",
DlgLnkPopTop		: "Posició Dalt",

DlnLnkMsgNoUrl		: "Si us plau, escrigui l'enllaç URL",
DlnLnkMsgNoEMail	: "Si us plau, escrigui l'adreça e-mail",
DlnLnkMsgNoAnchor	: "Si us plau, escrigui l'àncora",

// Color Dialog
DlgColorTitle		: "Seleccioni Color",
DlgColorBtnClear	: "Netejar",
DlgColorHighlight	: "Realçar",
DlgColorSelected	: "Seleccionat",

// Smiley Dialog
DlgSmileyTitle		: "Afegir una Icona",

// Special Character Dialog
DlgSpecialCharTitle	: "Seleccioneu Caràcter Especial",

// Table Dialog
DlgTableTitle		: "Proprietats de Taula",
DlgTableRows		: "Files",
DlgTableColumns		: "Columnes",
DlgTableBorder		: "Tamany de Costat",
DlgTableAlign		: "Alineació",
DlgTableAlignNotSet	: "<No Definit>",
DlgTableAlignLeft	: "Esquerra",
DlgTableAlignCenter	: "Centre",
DlgTableAlignRight	: "Dreta",
DlgTableWidth		: "Amplada",
DlgTableWidthPx		: "píxels",
DlgTableWidthPc		: "percentatge",
DlgTableHeight		: "Alçada",
DlgTableCellSpace	: "Cell spacing",
DlgTableCellPad		: "Cell padding",
DlgTableCaption		: "Capçalera",
DlgTableSummary		: "Summary",	//MISSING

// Table Cell Dialog
DlgCellTitle		: "Proprietats de Cel·la",
DlgCellWidth		: "Amplada",
DlgCellWidthPx		: "píxels",
DlgCellWidthPc		: "percentatge",
DlgCellHeight		: "Alçada",
DlgCellWordWrap		: "Word Wrap",
DlgCellWordWrapNotSet	: "<No Definit>",
DlgCellWordWrapYes	: "Si",
DlgCellWordWrapNo	: "No",
DlgCellHorAlign		: "Alineació Horitzontal",
DlgCellHorAlignNotSet	: "<No Definit>",
DlgCellHorAlignLeft	: "Esquerra",
DlgCellHorAlignCenter	: "Centre",
DlgCellHorAlignRight: "Dreta",
DlgCellVerAlign		: "Alineació Vertical",
DlgCellVerAlignNotSet	: "<No definit>",
DlgCellVerAlignTop	: "Top",
DlgCellVerAlignMiddle	: "Middle",
DlgCellVerAlignBottom	: "Bottom",
DlgCellVerAlignBaseline	: "Baseline",
DlgCellRowSpan		: "Rows Span",
DlgCellCollSpan		: "Columns Span",
DlgCellBackColor	: "Color de Fons",
DlgCellBorderColor	: "Colr de Costat",
DlgCellBtnSelect	: "Seleccioni...",

// Find Dialog
DlgFindTitle		: "Cercar",
DlgFindFindBtn		: "Cercar",
DlgFindNotFoundMsg	: "El text especificat no ha estat trobat.",

// Replace Dialog
DlgReplaceTitle			: "Remplaçar",
DlgReplaceFindLbl		: "Cercar:",
DlgReplaceReplaceLbl	: "Remplaçar per:",
DlgReplaceCaseChk		: "Sensible a Majúscules",
DlgReplaceReplaceBtn	: "Remplaçar",
DlgReplaceReplAllBtn	: "Remplaçar Tot",
DlgReplaceWordChk		: "Cercar Paraula Completa",

// Paste Operations / Dialog
PasteErrorPaste	: "La seguretat del vostre navigador no permet executar automàticament les operacions d'enganxat. Si us plau, utilitzeu el teclat (Ctrl+V).",
PasteErrorCut	: "La seguretat del vostre navigador no permet executar automàticament les operacions de tallar. Si us plau, utilitzeu el teclat (Ctrl+X).",
PasteErrorCopy	: "La seguretat del vostre navigador no permet executar automàticament les operacions de copiar. Si us plau, utilitzeu el teclat (Ctrl+C).",

PasteAsText		: "Enganxar com Text Planer",
PasteFromWord	: "Enganxar com Word",

DlgPasteMsg2	: "Si us plau, enganxeu dins del següent camp utilitzant el teclat (<STRONG>Ctrl+V</STRONG>) i premeu <STRONG>OK</STRONG>.",
DlgPasteIgnoreFont		: "Ignorar definicions de font",
DlgPasteRemoveStyles	: "Eliminar definicions d'estil",
DlgPasteCleanBox		: "Netejar camp",


// Color Picker
ColorAutomatic	: "Automàtic",
ColorMoreColors	: "Més Colors...",

// Document Properties
DocProps		: "Proprietats de document",

// Anchor Dialog
DlgAnchorTitle		: "Proprietats d'àncora",
DlgAnchorName		: "Nom d'àncora",
DlgAnchorErrorName	: "Si us plau, escrigui el nom de l'ancora",

// Speller Pages Dialog
DlgSpellNotInDic		: "No és al diccionari",
DlgSpellChangeTo		: "Canviar a",
DlgSpellBtnIgnore		: "Ignorar",
DlgSpellBtnIgnoreAll	: "Ignorar tot",
DlgSpellBtnReplace		: "Remplaçar",
DlgSpellBtnReplaceAll	: "Replaçar tot",
DlgSpellBtnUndo			: "Desfer",
DlgSpellNoSuggestions	: "Cap suggestió",
DlgSpellProgress		: "Comprovació de sintaxi en progrés",
DlgSpellNoMispell		: "Comprovació de sintaxi completada",
DlgSpellNoChanges		: "Comprovació de sintaxi: cap paraulada canviada",
DlgSpellOneChange		: "Comprovació de sintaxi: una paraula canviada",
DlgSpellManyChanges		: "Comprovació de sintaxi %1 paraules canviades",

IeSpellDownload			: "Comprovació de sintaxi no instal·lat. Voleu descarregar-ho ara?",

// Button Dialog
DlgButtonText	: "Text (Valor)",
DlgButtonType	: "Tipus",

// Checkbox and Radio Button Dialogs
DlgCheckboxName		: "Nom",
DlgCheckboxValue	: "Valor",
DlgCheckboxSelected	: "Seleccionat",

// Form Dialog
DlgFormName		: "Nom",
DlgFormAction	: "Acció",
DlgFormMethod	: "Mètode",

// Select Field Dialog
DlgSelectName		: "Nom",
DlgSelectValue		: "Valor",
DlgSelectSize		: "Tamany",
DlgSelectLines		: "Línies",
DlgSelectChkMulti	: "Permetre múltiples seleccions",
DlgSelectOpAvail	: "Opcions disponibles",
DlgSelectOpText		: "Text",
DlgSelectOpValue	: "Valor",
DlgSelectBtnAdd		: "Afegir",
DlgSelectBtnModify	: "Modificar",
DlgSelectBtnUp		: "Amunt",
DlgSelectBtnDown	: "Avall",
DlgSelectBtnSetValue : "Seleccionar per defecte",
DlgSelectBtnDelete	: "Esborrar",

// Textarea Dialog
DlgTextareaName	: "Nom",
DlgTextareaCols	: "Columnes",
DlgTextareaRows	: "Files",

// Text Field Dialog
DlgTextName			: "Nom",
DlgTextValue		: "Valor",
DlgTextCharWidth	: "Amplada de caràcter",
DlgTextMaxChars		: "Màxim de caràcters",
DlgTextType			: "Tipus",
DlgTextTypeText		: "Text",
DlgTextTypePass		: "Contrassenya",

// Hidden Field Dialog
DlgHiddenName	: "Nom",
DlgHiddenValue	: "Valor",

// Bulleted List Dialog
BulletedListProp	: "Proprietats de llista marcada",
NumberedListProp	: "Proprietats de llista numerada",
DlgLstType			: "Tipus",
DlgLstTypeCircle	: "Cercle",
DlgLstTypeDisc		: "Disc",	//MISSING
DlgLstTypeSquare	: "Quadrat",
DlgLstTypeNumbers	: "Números (1, 2, 3)",
DlgLstTypeLCase		: "Lletres minúscules (a, b, c)",
DlgLstTypeUCase		: "Lletres majúscules (A, B, C)",
DlgLstTypeSRoman	: "Números romans minúscules (i, ii, iii)",
DlgLstTypeLRoman	: "Números romans majúscules (I, II, III)",

// Document Properties Dialog
DlgDocGeneralTab	: "General",
DlgDocBackTab		: "Fons",
DlgDocColorsTab		: "Colors i marges",
DlgDocMetaTab		: "Dades Meta",

DlgDocPageTitle		: "Títol de la pàgina",
DlgDocLangDir		: "Direcció llenguatge",
DlgDocLangDirLTR	: "Esquerra a dreta (LTR)",
DlgDocLangDirRTL	: "Dreta a esquerra (RTL)",
DlgDocLangCode		: "Codi de llenguatge",
DlgDocCharSet		: "Codificació de conjunt de caràcters",
DlgDocCharSetOther	: "Altra codificació de conjunt de caràcters",

DlgDocDocType		: "Capçalera de tipus de document",
DlgDocDocTypeOther	: "Altra Capçalera de tipus de document",
DlgDocIncXHTML		: "Incloure declaracions XHTML",
DlgDocBgColor		: "Color de fons",
DlgDocBgImage		: "URL de la imatge de fons",
DlgDocBgNoScroll	: "Fons fixe",
DlgDocCText			: "Text",
DlgDocCLink			: "Enllaç",
DlgDocCVisited		: "Enllaç visitat",
DlgDocCActive		: "Enllaç actiu",
DlgDocMargins		: "Marges de pàgina",
DlgDocMaTop			: "Cap",
DlgDocMaLeft		: "Esquerra",
DlgDocMaRight		: "Dreta",
DlgDocMaBottom		: "Peu",
DlgDocMeIndex		: "Mots clau per a indexació (separats per coma)",
DlgDocMeDescr		: "Descripció del document",
DlgDocMeAuthor		: "Autor",
DlgDocMeCopy		: "Copyright",
DlgDocPreview		: "Vista prèvia",

// Templates Dialog
Templates			: "Plantilles",
DlgTemplatesTitle	: "Contingut Plantilles",
DlgTemplatesSelMsg	: "Si us plau, seleccioneu la plantilla per obrir en l'editor<br>(el contingut actual no serà enregistrat):",
DlgTemplatesLoading	: "Carregant la llista de plantilles. Si us plau, esperi...",
DlgTemplatesNoTpl	: "(No hi ha plantilles definides)",

// About Dialog
DlgAboutAboutTab	: "Sobre",
DlgAboutBrowserInfoTab	: "Informació del navigador",
DlgAboutVersion		: "versió",
DlgAboutLicense		: "Sota els termes de la Llicència GNU Lesser General Public License",
DlgAboutInfo		: "Per a més informació aneu a"
}