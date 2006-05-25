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
 * File Name: da.js
 * 	Danish language file.
 * 
 * File Authors:
 * 		Jørgen Nordstrøm (jn@FirstWeb.dk)
 * 		Jesper Michelsen (jm@i-deVision.dk)
 */

var FCKLang =
{
// Language direction : "ltr" (left to right) or "rtl" (right to left).
Dir					: "ltr",

ToolbarCollapse		: "Sammenfold Værktøjskasse",
ToolbarExpand		: "Udvid Værktøjskasse",

// Toolbar Items and Context Menu
Save				: "Gem",
NewPage				: "Ny side",
Preview				: "Vis eksempel",
Cut					: "Klip",
Copy				: "Kopier",
Paste				: "Indsæt",
PasteText			: "Indsæt som ren tekst",
PasteWord			: "Indsæt fra Word",
Print				: "Udskriv",
SelectAll			: "Vælg alt",
RemoveFormat		: "Slet formatering",
InsertLinkLbl		: "Link",
InsertLink			: "Indsæt/Rediger Link",
RemoveLink			: "Slet Link",
Anchor				: "Insert/Rediger Anker",
InsertImageLbl		: "Indsæt Billed",
InsertImage			: "Indsæt/Rediger Billed",
InsertFlashLbl		: "Flash",
InsertFlash			: "Indsæt/rediger Flash",
InsertTableLbl		: "Table",
InsertTable			: "Indsæt/Rediger Tabel",
InsertLineLbl		: "Linie",
InsertLine			: "Indsæt horisontal Linie",
InsertSpecialCharLbl: "Special Karakter",
InsertSpecialChar	: "Indsæt Special Karakter",
InsertSmileyLbl		: "Smiley",
InsertSmiley		: "Indsæt Smiley",
About				: "Om FCKeditor",
Bold				: "Fed",
Italic				: "Kursiv",
Underline			: "Understreget",
StrikeThrough		: "Overstreget",
Subscript			: "Sænket skrift",
Superscript			: "Hævet skrift",
LeftJustify			: "Venstrestillet",
CenterJustify		: "Centreret",
RightJustify		: "Højrestillet",
BlockJustify		: "Lige margener",
DecreaseIndent		: "Formindsk indrykning",
IncreaseIndent		: "Forøg indrykning",
Undo				: "Fortryd",
Redo				: "Anuller fortryd",
NumberedListLbl		: "Tal opstilling",
NumberedList		: "Indsæt/Slet Tal opstilling",
BulletedListLbl		: "Punkttegn opstilling",
BulletedList		: "Indsæt/Slet Punkttegn opstilling",
ShowTableBorders	: "Vis tabel kanter",
ShowDetails			: "Vis detaljer",
Style				: "Typografi",
FontFormat			: "Formatering",
Font				: "Skrifttype",
FontSize			: "Skriftstørrelse",
TextColor			: "Tekstfarve",
BGColor				: "Baggrundsfarve",
Source				: "Kilde",
Find				: "Søg",
Replace				: "Erstat",
SpellCheck			: "Stavekontrol",
UniversalKeyboard	: "Universalt Tastatur",
PageBreakLbl		: "Page Break",	//MISSING
PageBreak			: "Insert Page Break",	//MISSING

Form			: "Indsæt Form",
Checkbox		: "Indsæt Afkrydsningsboks",
RadioButton		: "Indsæt Radioknap",
TextField		: "Indsæt Tekstfelt",
Textarea		: "Indsæt Tekstboks",
HiddenField		: "Indsæt Skjultfelt",
Button			: "Indsæt Knap",
SelectionField	: "Indsæt Valgfelt",
ImageButton		: "Indsæt Billedknap",

// Context Menu
EditLink			: "Rediger link",
InsertRow			: "Indsæt række",
DeleteRows			: "Slet rækker",
InsertColumn		: "Indsæt kolonne",
DeleteColumns		: "Slet kolonner",
InsertCell			: "Indsæt celle",
DeleteCells			: "Slet celle",
MergeCells			: "Flet celler",
SplitCell			: "Opdel celler",
TableDelete			: "Delete Table",	//MISSING
CellProperties		: "Celle egenskaber",
TableProperties		: "Tabel egenskaber",
ImageProperties		: "Billed egenskaber",
FlashProperties		: "Flash egenskaber",

AnchorProp			: "Anker egenskaber",
ButtonProp			: "Knap egenskaber",
CheckboxProp		: "Afkrydsningsboks egenskaber",
HiddenFieldProp		: "Skjultfelt egenskaber",
RadioButtonProp		: "Radioknap egenskaber",
ImageButtonProp		: "Billedknap egenskaber",
TextFieldProp		: "Tekstfelt egenskaber",
SelectionFieldProp	: "Valgfelt egenskaber",
TextareaProp		: "Tekstboks egenskaber",
FormProp			: "Form egenskaber",

FontFormats			: "Normal;Formateret;Adresse;Overskrift 1;Overskrift 2;Overskrift 3;Overskrift 4;Overskrift 5;Overskrift 6",

// Alerts and Messages
ProcessingXHTML		: "Behandler XHTML. Vent venligst...",
Done				: "Færdig",
PasteWordConfirm	: "Den tekst du forsøger at indsætte ser ud til at komme fra Word. Vil du rense teksten før den indsættes ?",
NotCompatiblePaste	: "Denne kommando er tilgændelig i Internet Explorer 5.5 og senere. Vil du indsætte teksten uden at rense den ?",
UnknownToolbarItem	: "Ukendt værktøjslinje objekt \"%1\"",
UnknownCommand		: "Ukendt kommando navn \"%1\"",
NotImplemented		: "Kommandoen er ikke implementeret",
UnknownToolbarSet	: "Værktøjslinjen \"%1\" eksisterer ikke",
NoActiveX			: "Din browsers sikkerhedsindstillinger kan begrænse nogle af editorens muligheder. Du skal slå \"Kør ActiveX-objekter og plug-ins\" til. Du vil måske opleve fejl og manglende muligheder.",
BrowseServerBlocked : "The resources browser could not be opened. Make sure that all popup blockers are disabled.",	//MISSING
DialogBlocked		: "It was not possible to open the dialog window. Make sure all popup blockers are disabled.",	//MISSING

// Dialogs
DlgBtnOK			: "OK",
DlgBtnCancel		: "Anuller",
DlgBtnClose			: "Luk",
DlgBtnBrowseServer	: "Gennemse Server",
DlgAdvancedTag		: "Avanceret",
DlgOpOther			: "&lt;Andet&gt;",
DlgInfoTab			: "Info",
DlgAlertUrl			: "Indtast URL",

// General Dialogs Labels
DlgGenNotSet		: "&lt;ikke sat&gt;",
DlgGenId			: "Id",
DlgGenLangDir		: "Tekstretning",
DlgGenLangDirLtr	: "Venstre mod højre (LTR)",
DlgGenLangDirRtl	: "Højre mod venstre (RTL)",
DlgGenLangCode		: "Sprog kode",
DlgGenAccessKey		: "Adgangsnøgle",
DlgGenName			: "Navn",
DlgGenTabIndex		: "Tabulator Indeks",
DlgGenLongDescr		: "Udvidet beskrivelse",
DlgGenClass			: "Typografiark",
DlgGenTitle			: "Titel",
DlgGenContType		: "Indholdstype",
DlgGenLinkCharset	: "Tegnsæt",
DlgGenStyle			: "Typografi",

// Image Dialog
DlgImgTitle			: "Billed egenskaber",
DlgImgInfoTab		: "Billed info",
DlgImgBtnUpload		: "Send til serveren",
DlgImgURL			: "URL",
DlgImgUpload		: "Upload",
DlgImgAlt			: "Alternativ tekst",
DlgImgWidth			: "Bredde",
DlgImgHeight		: "Højde",
DlgImgLockRatio		: "Lås størrelsesforhold",
DlgBtnResetSize		: "Nulstil størrelse",
DlgImgBorder		: "Ramme",
DlgImgHSpace		: "HMargin",
DlgImgVSpace		: "VMargin",
DlgImgAlign			: "Justering",
DlgImgAlignLeft		: "Venstre",
DlgImgAlignAbsBottom: "Abs bund",
DlgImgAlignAbsMiddle: "Abs Midte",
DlgImgAlignBaseline	: "Bundlinje",
DlgImgAlignBottom	: "Bund",
DlgImgAlignMiddle	: "Midte",
DlgImgAlignRight	: "Højre",
DlgImgAlignTextTop	: "Tekst top",
DlgImgAlignTop		: "Top",
DlgImgPreview		: "Vis eksempel",
DlgImgAlertUrl		: "Indtast stien til billedet",
DlgImgLinkTab		: "Link",

// Flash Dialog
DlgFlashTitle		: "Flash egenskaber",
DlgFlashChkPlay		: "Automatisk afspilning",
DlgFlashChkLoop		: "Gentagelse",
DlgFlashChkMenu		: "Vis Flash menu",
DlgFlashScale		: "Skalér",
DlgFlashScaleAll	: "Vis alt",
DlgFlashScaleNoBorder	: "Ingen ramme",
DlgFlashScaleFit	: "Tilpas størrelse",

// Link Dialog
DlgLnkWindowTitle	: "Link",
DlgLnkInfoTab		: "Link info",
DlgLnkTargetTab		: "Mål",

DlgLnkType			: "Link type",
DlgLnkTypeURL		: "URL",
DlgLnkTypeAnchor	: "Anker på denne side",
DlgLnkTypeEMail		: "Email",
DlgLnkProto			: "Protokol",
DlgLnkProtoOther	: "&lt;anden&gt;",
DlgLnkURL			: "URL",
DlgLnkAnchorSel		: "Vælg et anker",
DlgLnkAnchorByName	: "Efter anker navn",
DlgLnkAnchorById	: "Efter element Id",
DlgLnkNoAnchors		: "&lt;Der er ingen ankre tilgængelige i dette dokument&gt;",
DlgLnkEMail			: "Email Adresse",
DlgLnkEMailSubject	: "Emne",
DlgLnkEMailBody		: "Besked",
DlgLnkUpload		: "Upload",
DlgLnkBtnUpload		: "Send til serveren",

DlgLnkTarget		: "Mål",
DlgLnkTargetFrame	: "&lt;ramme&gt;",
DlgLnkTargetPopup	: "&lt;popup vindue&gt;",
DlgLnkTargetBlank	: "Nyt vindue (_blank)",
DlgLnkTargetParent	: "Overliggende vindue (_parent)",
DlgLnkTargetSelf	: "Samme vindue (_self)",
DlgLnkTargetTop		: "Øverste vindue (_top)",
DlgLnkTargetFrameName	: "Visnings vinduets navn",
DlgLnkPopWinName	: "Popup vinduets navn",
DlgLnkPopWinFeat	: "Popup vinduets egenskaber",
DlgLnkPopResize		: "Skalering",
DlgLnkPopLocation	: "Lokationslinje",
DlgLnkPopMenu		: "Menulinje",
DlgLnkPopScroll		: "Scrollbars",
DlgLnkPopStatus		: "Statuslinje",
DlgLnkPopToolbar	: "Værktøjslinje",
DlgLnkPopFullScrn	: "Fuld skærm (IE)",
DlgLnkPopDependent	: "Afhængig (Netscape)",
DlgLnkPopWidth		: "Bredde",
DlgLnkPopHeight		: "Højde",
DlgLnkPopLeft		: "Position fra venstre",
DlgLnkPopTop		: "Position fra toppen",

DlnLnkMsgNoUrl		: "Indtast link URL",
DlnLnkMsgNoEMail	: "Indtast e-mail addressen",
DlnLnkMsgNoAnchor	: "Vælg Anker",

// Color Dialog
DlgColorTitle		: "Vælg farve",
DlgColorBtnClear	: "Slet alt",
DlgColorHighlight	: "Marker",
DlgColorSelected	: "valgt",

// Smiley Dialog
DlgSmileyTitle		: "Insæt en smiley",

// Special Character Dialog
DlgSpecialCharTitle	: "Vælg specialkarakter",

// Table Dialog
DlgTableTitle		: "Tabel egenskaber",
DlgTableRows		: "Rækker",
DlgTableColumns		: "Kolonner",
DlgTableBorder		: "Ramme størrelse",
DlgTableAlign		: "Justering",
DlgTableAlignNotSet	: "<Ikke sat>",
DlgTableAlignLeft	: "Venstrestillet",
DlgTableAlignCenter	: "Centreret",
DlgTableAlignRight	: "Højrestillet",
DlgTableWidth		: "Bredde",
DlgTableWidthPx		: "pixels",
DlgTableWidthPc		: "procent",
DlgTableHeight		: "Højde",
DlgTableCellSpace	: "Afstand mellem celler",
DlgTableCellPad		: "Celle margin",
DlgTableCaption		: "Titel",
DlgTableSummary		: "Summary",	//MISSING

// Table Cell Dialog
DlgCellTitle		: "Celle egenskaber",
DlgCellWidth		: "Bredde",
DlgCellWidthPx		: "pixels",
DlgCellWidthPc		: "procent",
DlgCellHeight		: "Højde",
DlgCellWordWrap		: "Orddeling",
DlgCellWordWrapNotSet	: "<Ikke sat>",
DlgCellWordWrapYes	: "Ja",
DlgCellWordWrapNo	: "Nej",
DlgCellHorAlign		: "Horisontal justering",
DlgCellHorAlignNotSet	: "<Ikke sat>",
DlgCellHorAlignLeft	: "Venstrestillet",
DlgCellHorAlignCenter	: "Centreret",
DlgCellHorAlignRight: "Højrestillet",
DlgCellVerAlign		: "Vertikal Justering",
DlgCellVerAlignNotSet	: "<Ikke sat>",
DlgCellVerAlignTop	: "Top",
DlgCellVerAlignMiddle	: "Midte",
DlgCellVerAlignBottom	: "Bund",
DlgCellVerAlignBaseline	: "Bundlinje",
DlgCellRowSpan		: "Antal rækker cellen spænder over",
DlgCellCollSpan		: "Antal kolonner cellen spænder over",
DlgCellBackColor	: "Baggrundsfarve",
DlgCellBorderColor	: "rammefarve",
DlgCellBtnSelect	: "Vælg...",

// Find Dialog
DlgFindTitle		: "Find",
DlgFindFindBtn		: "Find",
DlgFindNotFoundMsg	: "Den angivne tekst blev ikke fundet",

// Replace Dialog
DlgReplaceTitle			: "Erstat",
DlgReplaceFindLbl		: "Find:",
DlgReplaceReplaceLbl	: "Erstat med:",
DlgReplaceCaseChk		: "Forskel på store og små bogstaver",
DlgReplaceReplaceBtn	: "Erstat",
DlgReplaceReplAllBtn	: "Erstat alle",
DlgReplaceWordChk		: "Kun hele ord",

// Paste Operations / Dialog
PasteErrorPaste	: "Din browsers sikkerhedsindstillinger tillader ikke editoren at indsætte tekst automatisk. Brug i stedet tastaturet til at indsætte teksten (Ctrl+V).",
PasteErrorCut	: "Din browsers sikkerhedsindstillinger tillader ikke editoren at klippe tekst automatisk. Brug i stedet tastaturet til at klippe teksten (Ctrl+X).",
PasteErrorCopy	: "Din browsers sikkerhedsindstillinger tillader ikke editoren at kopiere tekst automatisk. Brug i stedet tastaturet til at kopiere teksten (Ctrl+V).",

PasteAsText		: "Indsæt som ren tekst",
PasteFromWord	: "Indsæt fra Word",

DlgPasteMsg2	: "Indsæt i boksen herunder (<STRONG>Ctrl+V</STRONG>) og klik <STRONG>OK</STRONG>.",
DlgPasteIgnoreFont		: "Ignorer font definitioner",
DlgPasteRemoveStyles	: "Fjern typografi definitioner",
DlgPasteCleanBox		: "Slet indhold",


// Color Picker
ColorAutomatic	: "Automatisk",
ColorMoreColors	: "Flere farver...",

// Document Properties
DocProps		: "Dokument egenskaber",

// Anchor Dialog
DlgAnchorTitle		: "Anker egenskaber",
DlgAnchorName		: "Anker navn",
DlgAnchorErrorName	: "Indtast Anker navn",

// Speller Pages Dialog
DlgSpellNotInDic		: "Findes ikke i ordbogen",
DlgSpellChangeTo		: "Ændre til",
DlgSpellBtnIgnore		: "Ignorere",
DlgSpellBtnIgnoreAll	: "Ignorere alle",
DlgSpellBtnReplace		: "Udskift",
DlgSpellBtnReplaceAll	: "Udskift alle",
DlgSpellBtnUndo			: "Tilbage",
DlgSpellNoSuggestions	: "- Intet forslag -",
DlgSpellProgress		: "Stavekontrolen arbejder...",
DlgSpellNoMispell		: "Stavekontrol færdig: Ingen fejl fundet",
DlgSpellNoChanges		: "Stavekontrol færdig: Ingen ord ændret",
DlgSpellOneChange		: "Stavekontrol færdig: Et ord ændret",
DlgSpellManyChanges		: "Stavekontrol færdig: %1 ord ændret",

IeSpellDownload			: "Stavekontrol ikke installeret. Vil du hente den nu?",

// Button Dialog
DlgButtonText	: "Tekst (Værdi)",
DlgButtonType	: "Type",

// Checkbox and Radio Button Dialogs
DlgCheckboxName		: "Navn",
DlgCheckboxValue	: "Værdi",
DlgCheckboxSelected	: "Valgt",

// Form Dialog
DlgFormName		: "Navn",
DlgFormAction	: "Handling",
DlgFormMethod	: "Metod",

// Select Field Dialog
DlgSelectName		: "Navn",
DlgSelectValue		: "Værdi",
DlgSelectSize		: "Størrelse",
DlgSelectLines		: "linier",
DlgSelectChkMulti	: "Tillad flere valg",
DlgSelectOpAvail	: "Valgmulighedder",
DlgSelectOpText		: "Tekst",
DlgSelectOpValue	: "Værdi",
DlgSelectBtnAdd		: "Tilføj",
DlgSelectBtnModify	: "Ændre",
DlgSelectBtnUp		: "Op",
DlgSelectBtnDown	: "Ned",
DlgSelectBtnSetValue : "Sæt som udvalgt",
DlgSelectBtnDelete	: "Slet",

// Textarea Dialog
DlgTextareaName	: "Navn",
DlgTextareaCols	: "Kolonne",
DlgTextareaRows	: "Række",

// Text Field Dialog
DlgTextName			: "Navn",
DlgTextValue		: "Værdi",
DlgTextCharWidth	: "Synligt antal bogstaver",
DlgTextMaxChars		: "Maximum antal bogstaver",
DlgTextType			: "Type",
DlgTextTypeText		: "Tekst",
DlgTextTypePass		: "Kodeord",

// Hidden Field Dialog
DlgHiddenName	: "Navn",
DlgHiddenValue	: "Værdi",

// Bulleted List Dialog
BulletedListProp	: "Punkttegnopstilling egenskaber",
NumberedListProp	: "Talopstilling egenskaber",
DlgLstType			: "Type",
DlgLstTypeCircle	: "Cirkel",
DlgLstTypeDisc		: "Flade",
DlgLstTypeSquare	: "Firkant",
DlgLstTypeNumbers	: "Nummereret (1, 2, 3)",
DlgLstTypeLCase		: "Små bogstaver (a, b, c)",
DlgLstTypeUCase		: "Store bogstaver (A, B, C)",
DlgLstTypeSRoman	: "Små Romertal (i, ii, iii)",
DlgLstTypeLRoman	: "Store Romertal (I, II, III)",

// Document Properties Dialog
DlgDocGeneralTab	: "Generelt",
DlgDocBackTab		: "Baggrund",
DlgDocColorsTab		: "Farver og Margin",
DlgDocMetaTab		: "Meta Information",

DlgDocPageTitle		: "Side Titel",
DlgDocLangDir		: "Sprog",
DlgDocLangDirLTR	: "Venstre Til Højre (LTR)",
DlgDocLangDirRTL	: "Højre Til Venstre (RTL)",
DlgDocLangCode		: "Landekode",
DlgDocCharSet		: "Karakter sæt kode",
DlgDocCharSetOther	: "Anden karakter sæt kode",

DlgDocDocType		: "Dokument type kategori",
DlgDocDocTypeOther	: "Anden dokument type kategori",
DlgDocIncXHTML		: "Inkludere XHTML deklartion",
DlgDocBgColor		: "Baggrundsfarve",
DlgDocBgImage		: "Baggrundsbilled URL",
DlgDocBgNoScroll	: "Ikke scrollbar baggrund",
DlgDocCText			: "Tekst",
DlgDocCLink			: "Link",
DlgDocCVisited		: "Besøgt link",
DlgDocCActive		: "Aktivt link",
DlgDocMargins		: "Side margin",
DlgDocMaTop			: "Top",
DlgDocMaLeft		: "Venstre",
DlgDocMaRight		: "Højre",
DlgDocMaBottom		: "Bund",
DlgDocMeIndex		: "Dokument index nøgleord (komma sepereret)",
DlgDocMeDescr		: "Dokument beskrivelse",
DlgDocMeAuthor		: "Forfatter",
DlgDocMeCopy		: "Copyright",
DlgDocPreview		: "Vis",

// Templates Dialog
Templates			: "Skabeloner",
DlgTemplatesTitle	: "Indholdsskabeloner",
DlgTemplatesSelMsg	: "Vælg den skabelon, som skal åbnes i editoren<br>(Nuværende indhold vil blive overskrevet):",
DlgTemplatesLoading	: "Henter liste over skabeloner. Vent venligst...",
DlgTemplatesNoTpl	: "(Der er ikke defineret nogen skabelon)",

// About Dialog
DlgAboutAboutTab	: "About",
DlgAboutBrowserInfoTab	: "Browser Info",
DlgAboutVersion		: "version",
DlgAboutLicense		: "Licens under vilkår for GNU Lesser General Public License",
DlgAboutInfo		: "For yderlig information gå til"
}