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
 * File Name: fr.js
 * 	French language file.
 * 
 * File Authors:
 * 		Hubert Garrido (liane@users.sourceforge.net)
 */

var FCKLang =
{
// Language direction : "ltr" (left to right) or "rtl" (right to left).
Dir					: "ltr",

ToolbarCollapse		: "Masquer Outils",
ToolbarExpand		: "Afficher Outils",

// Toolbar Items and Context Menu
Save				: "Enregistrer",
NewPage				: "Nouvelle Page",
Preview				: "Prévisualisation",
Cut					: "Couper",
Copy				: "Copier",
Paste				: "Coller",
PasteText			: "Coller comme texte",
PasteWord			: "Coller de Word",
Print				: "Imprimer",
SelectAll			: "Tout sélectionner",
RemoveFormat		: "Supprimer Format",
InsertLinkLbl		: "Lien",
InsertLink			: "Insérer/Modifier Lien",
RemoveLink			: "Supprimer Lien",
Anchor				: "Insérer/Modifier Ancre",
InsertImageLbl		: "Image",
InsertImage			: "Insérer/Modifier Image",
InsertFlashLbl		: "Animation Flash",
InsertFlash			: "Insérer/Modifier Animation Flash",
InsertTableLbl		: "Tableau",
InsertTable			: "Insérer/Modifier Tableau",
InsertLineLbl		: "Séparateur",
InsertLine			: "Insérer Séparateur",
InsertSpecialCharLbl: "Caractère Spécial",
InsertSpecialChar	: "Insérer Caractère Spécial",
InsertSmileyLbl		: "Smiley",
InsertSmiley		: "Insérer Smiley",
About				: "A propos de FCKeditor",
Bold				: "Gras",
Italic				: "Italique",
Underline			: "Souligné",
StrikeThrough		: "Barré",
Subscript			: "Indice",
Superscript			: "Exposant",
LeftJustify			: "Aligné à Gauche",
CenterJustify		: "Centré",
RightJustify		: "Aligné à Droite",
BlockJustify		: "Texte Justifié",
DecreaseIndent		: "Diminuer le Retrait",
IncreaseIndent		: "Augmenter le Retrait",
Undo				: "Annuler",
Redo				: "Refaire",
NumberedListLbl		: "Liste Numérotée",
NumberedList		: "Insérer/Supprimer Liste Numérotée",
BulletedListLbl		: "Liste à puces",
BulletedList		: "Insérer/Supprimer Liste à puces",
ShowTableBorders	: "Afficher Bordures de Tableau",
ShowDetails			: "Afficher Caractères Invisibles",
Style				: "Style",
FontFormat			: "Format",
Font				: "Police",
FontSize			: "Taille",
TextColor			: "Couleur de Caractère",
BGColor				: "Couleur de Fond",
Source				: "Source",
Find				: "Chercher",
Replace				: "Remplacer",
SpellCheck			: "Orthographe",
UniversalKeyboard	: "Clavier Universel",
PageBreakLbl		: "Page Break",	//MISSING
PageBreak			: "Insert Page Break",	//MISSING

Form			: "Formulaire",
Checkbox		: "Case à cocher",
RadioButton		: "Bouton Radio",
TextField		: "Champ Texte",
Textarea		: "Zone Texte",
HiddenField		: "Champ caché",
Button			: "Bouton",
SelectionField	: "Liste/Menu",
ImageButton		: "Bouton Image",

// Context Menu
EditLink			: "Modifier Lien",
InsertRow			: "Insérer une Ligne",
DeleteRows			: "Supprimer des Lignes",
InsertColumn		: "Insérer une Colonne",
DeleteColumns		: "Supprimer des Colonnes",
InsertCell			: "Insérer une Cellule",
DeleteCells			: "Supprimer des Cellules",
MergeCells			: "Fusionner les Cellules",
SplitCell			: "Scinder les Cellules",
TableDelete			: "Delete Table",	//MISSING
CellProperties		: "Propriétés de Cellule",
TableProperties		: "Propriétés de Tableau",
ImageProperties		: "Propriétés d'Image",
FlashProperties		: "Propriétés d'Animation Flash",

AnchorProp			: "Propriétés d'Ancre",
ButtonProp			: "Propriétés de Bouton",
CheckboxProp		: "Propriétés de Case à Cocher",
HiddenFieldProp		: "Propriétés de Champ Caché",
RadioButtonProp		: "Propriétés de Bouton Radio",
ImageButtonProp		: "Propriétés de Bouton Image",
TextFieldProp		: "Propriétés de Champ Texte",
SelectionFieldProp	: "Propriétés de Liste/Menu",
TextareaProp		: "Propriétés de Zone Texte",
FormProp			: "Propriétés de Formulaire",

FontFormats			: "Normal;Formatted;Address;Titre 1;Titre 2;Titre 3;Titre 4;Titre 5;Titre 6",

// Alerts and Messages
ProcessingXHTML		: "Calcul XHTML. Veuillez patienter...",
Done				: "Terminé",
PasteWordConfirm	: "Le texte à coller semble provenir de Word. Désirez-vous le nettoyer avant de coller?",
NotCompatiblePaste	: "Cette commande nécessite Internet Explorer version 5.5 minimum. Souhaitez-vous coller sans nettoyage?",
UnknownToolbarItem	: "Elément de barre d'outil inconnu \"%1\"",
UnknownCommand		: "Nom de commande inconnu \"%1\"",
NotImplemented		: "Commande non encore écrite",
UnknownToolbarSet	: "La barre d'outils \"%1\" n'existe pas",
NoActiveX			: "Les paramètres de sécurité de votre navigateur peuvent limiter quelques fonctionnalités de l'éditeur. Veuillez activer l'option \"Exécuter les contrôles ActiveX et les plug-ins\". Il se peut que vous rencontriez des erreurs et remarquiez quelques limitations.",
BrowseServerBlocked : "The resources browser could not be opened. Make sure that all popup blockers are disabled.",	//MISSING
DialogBlocked		: "It was not possible to open the dialog window. Make sure all popup blockers are disabled.",	//MISSING

// Dialogs
DlgBtnOK			: "OK",
DlgBtnCancel		: "Annuler",
DlgBtnClose			: "Fermer",
DlgBtnBrowseServer	: "Parcourir le Serveur",
DlgAdvancedTag		: "Avancé",
DlgOpOther			: "&lt;Autre&gt;",
DlgInfoTab			: "Info",
DlgAlertUrl			: "Veuillez saisir l'URL",

// General Dialogs Labels
DlgGenNotSet		: "&lt;Par Défaut&gt;",
DlgGenId			: "Id",
DlgGenLangDir		: "Sens d'Ecriture",
DlgGenLangDirLtr	: "Gauche vers Droite (LTR)",
DlgGenLangDirRtl	: "Droite vers Gauche (RTL)",
DlgGenLangCode		: "Code Langue",
DlgGenAccessKey		: "Equivalent Clavier",
DlgGenName			: "Nom",
DlgGenTabIndex		: "Ordre de Tabulation",
DlgGenLongDescr		: "URL de Description Longue",
DlgGenClass			: "Classes de Feuilles de Style",
DlgGenTitle			: "Titre Indicatif",
DlgGenContType		: "Type de Contenu Indicatif",
DlgGenLinkCharset	: "Encodage de Caractère de la cible",
DlgGenStyle			: "Style",

// Image Dialog
DlgImgTitle			: "Propriétés d'Image",
DlgImgInfoTab		: "Informations sur l'Image",
DlgImgBtnUpload		: "Envoyer au Serveur",
DlgImgURL			: "URL",
DlgImgUpload		: "Upload",
DlgImgAlt			: "Texte de Remplacement",
DlgImgWidth			: "Largeur",
DlgImgHeight		: "Hauteur",
DlgImgLockRatio		: "Garder proportions",
DlgBtnResetSize		: "Taille Originale",
DlgImgBorder		: "Bordure",
DlgImgHSpace		: "HSpace",
DlgImgVSpace		: "VSpace",
DlgImgAlign			: "Alignement",
DlgImgAlignLeft		: "Gauche",
DlgImgAlignAbsBottom: "Abs Bas",
DlgImgAlignAbsMiddle: "Abs Milieu",
DlgImgAlignBaseline	: "Bas du texte",
DlgImgAlignBottom	: "Bas",
DlgImgAlignMiddle	: "Milieu",
DlgImgAlignRight	: "Droite",
DlgImgAlignTextTop	: "Haut du texte",
DlgImgAlignTop		: "Haut",
DlgImgPreview		: "Prévisualisation",
DlgImgAlertUrl		: "Veuillez saisir l'URL de l'image",
DlgImgLinkTab		: "Lien",

// Flash Dialog
DlgFlashTitle		: "Propriétés d'animation Flash",
DlgFlashChkPlay		: "Lecture automatique",
DlgFlashChkLoop		: "Boucle",
DlgFlashChkMenu		: "Activer menu Flash",
DlgFlashScale		: "Affichage",
DlgFlashScaleAll	: "Par défault (tout montrer)",
DlgFlashScaleNoBorder	: "Sans Bordure",
DlgFlashScaleFit	: "Ajuster aux Dimensions",

// Link Dialog
DlgLnkWindowTitle	: "Propriétés de Lien",
DlgLnkInfoTab		: "Informations sur le Lien",
DlgLnkTargetTab		: "Destination",

DlgLnkType			: "Type de Lien",
DlgLnkTypeURL		: "URL",
DlgLnkTypeAnchor	: "Ancre dans cette page",
DlgLnkTypeEMail		: "E-Mail",
DlgLnkProto			: "Protocole",
DlgLnkProtoOther	: "&lt;autre&gt;",
DlgLnkURL			: "URL",
DlgLnkAnchorSel		: "Sélectionner une Ancre",
DlgLnkAnchorByName	: "Par Nom d'Ancre",
DlgLnkAnchorById	: "Par Id d'Elément",
DlgLnkNoAnchors		: "&lt;Pas d'ancre disponible dans le document&gt;",
DlgLnkEMail			: "Adresse E-Mail",
DlgLnkEMailSubject	: "Sujet du Message",
DlgLnkEMailBody		: "Corps du Message",
DlgLnkUpload		: "Upload",
DlgLnkBtnUpload		: "Envoyer au Serveur",

DlgLnkTarget		: "Destination",
DlgLnkTargetFrame	: "&lt;cadre&gt;",
DlgLnkTargetPopup	: "&lt;fenêtre popup&gt;",
DlgLnkTargetBlank	: "Nouvelle Fenêtre (_blank)",
DlgLnkTargetParent	: "Fenêtre Mère (_parent)",
DlgLnkTargetSelf	: "Même Fenêtre (_self)",
DlgLnkTargetTop		: "Fenêtre Supérieure (_top)",
DlgLnkTargetFrameName	: "Nom du Cadre de Destination",
DlgLnkPopWinName	: "Nom de la Fenêtre Popup",
DlgLnkPopWinFeat	: "Caractéristiques de la Fenêtre Popup",
DlgLnkPopResize		: "Taille Modifiable",
DlgLnkPopLocation	: "Barre d'Adresses",
DlgLnkPopMenu		: "Barre de Menu",
DlgLnkPopScroll		: "Barres de Défilement",
DlgLnkPopStatus		: "Barre d'Etat",
DlgLnkPopToolbar	: "Barre d'Outils",
DlgLnkPopFullScrn	: "Plein Ecran (IE)",
DlgLnkPopDependent	: "Dépendante (Netscape)",
DlgLnkPopWidth		: "Largeur",
DlgLnkPopHeight		: "Hauteur",
DlgLnkPopLeft		: "Position Gauche",
DlgLnkPopTop		: "Position Haut",

DlnLnkMsgNoUrl		: "Veuillez saisir l'URL",
DlnLnkMsgNoEMail	: "Veuillez saisir l'adresse e-mail",
DlnLnkMsgNoAnchor	: "Veuillez sélectionner une ancre",

// Color Dialog
DlgColorTitle		: "Sélectionner",
DlgColorBtnClear	: "Effacer",
DlgColorHighlight	: "Highlight",
DlgColorSelected	: "Sélectionné",

// Smiley Dialog
DlgSmileyTitle		: "Insérer Smiley",

// Special Character Dialog
DlgSpecialCharTitle	: "Insérer Caractère Spécial",

// Table Dialog
DlgTableTitle		: "Propriétés de Tableau",
DlgTableRows		: "Lignes",
DlgTableColumns		: "Colonnes",
DlgTableBorder		: "Bordure",
DlgTableAlign		: "Alignement",
DlgTableAlignNotSet	: "<Par Défaut>",
DlgTableAlignLeft	: "Gauche",
DlgTableAlignCenter	: "Centré",
DlgTableAlignRight	: "Droite",
DlgTableWidth		: "Largeur",
DlgTableWidthPx		: "pixels",
DlgTableWidthPc		: "pourcentage",
DlgTableHeight		: "Hauteur",
DlgTableCellSpace	: "Espacement",
DlgTableCellPad		: "Contour",
DlgTableCaption		: "Titre",
DlgTableSummary		: "Résumé",

// Table Cell Dialog
DlgCellTitle		: "Propriétés de cellule",
DlgCellWidth		: "Largeur",
DlgCellWidthPx		: "pixels",
DlgCellWidthPc		: "pourcentage",
DlgCellHeight		: "Hauteur",
DlgCellWordWrap		: "Retour à la ligne",
DlgCellWordWrapNotSet	: "<Par Défaut>",
DlgCellWordWrapYes	: "Oui",
DlgCellWordWrapNo	: "Non",
DlgCellHorAlign		: "Alignement Horizontal",
DlgCellHorAlignNotSet	: "<Par Défaut>",
DlgCellHorAlignLeft	: "Gauche",
DlgCellHorAlignCenter	: "Centré",
DlgCellHorAlignRight: "Droite",
DlgCellVerAlign		: "Alignement Vertical",
DlgCellVerAlignNotSet	: "<Par Défaut>",
DlgCellVerAlignTop	: "Haut",
DlgCellVerAlignMiddle	: "Milieu",
DlgCellVerAlignBottom	: "Bas",
DlgCellVerAlignBaseline	: "Bas du texte",
DlgCellRowSpan		: "Lignes Fusionnées",
DlgCellCollSpan		: "Colonnes Fusionnées",
DlgCellBackColor	: "Fond",
DlgCellBorderColor	: "Bordure",
DlgCellBtnSelect	: "Choisir...",

// Find Dialog
DlgFindTitle		: "Chercher",
DlgFindFindBtn		: "Chercher",
DlgFindNotFoundMsg	: "Le texte indiqué est introuvable.",

// Replace Dialog
DlgReplaceTitle			: "Remplacer",
DlgReplaceFindLbl		: "Rechercher:",
DlgReplaceReplaceLbl	: "Remplacer par:",
DlgReplaceCaseChk		: "Respecter la casse",
DlgReplaceReplaceBtn	: "Remplacer",
DlgReplaceReplAllBtn	: "Tout remplacer",
DlgReplaceWordChk		: "Mot entier",

// Paste Operations / Dialog
PasteErrorPaste	: "Les paramètres de sécurité de votre navigateur empêchent l'éditeur de coller automatiquement vos données. Veuillez utiliser les équivalents claviers (Ctrl+V).",
PasteErrorCut	: "Les paramètres de sécurité de votre navigateur empêchent l'éditeur de couper automatiquement vos données. Veuillez utiliser les équivalents claviers (Ctrl+X).",
PasteErrorCopy	: "Les paramètres de sécurité de votre navigateur empêchent l'éditeur de copier automatiquement vos données. Veuillez utiliser les équivalents claviers (Ctrl+C).",

PasteAsText		: "Coller comme texte",
PasteFromWord	: "Coller à partir de Word",

DlgPasteMsg2	: "Veuillez coller dans la zone ci-dessous en utilisant le clavier (<STRONG>Ctrl+V</STRONG>) et cliquez sur <STRONG>OK</STRONG>.",
DlgPasteIgnoreFont		: "Ignorer les Polices de Caractères",
DlgPasteRemoveStyles	: "Supprimer les Styles",
DlgPasteCleanBox		: "Effacer le contenu",


// Color Picker
ColorAutomatic	: "Automatique",
ColorMoreColors	: "Plus de Couleurs...",

// Document Properties
DocProps		: "Propriétés du Document",

// Anchor Dialog
DlgAnchorTitle		: "Propriétés de l'Ancre",
DlgAnchorName		: "Nom de l'Ancre",
DlgAnchorErrorName	: "Veuillez saisir le nom de l'ancre",

// Speller Pages Dialog
DlgSpellNotInDic		: "Pas dans le dictionnaire",
DlgSpellChangeTo		: "Changer en",
DlgSpellBtnIgnore		: "Ignorer",
DlgSpellBtnIgnoreAll	: "Ignorer Tout",
DlgSpellBtnReplace		: "Remplacer",
DlgSpellBtnReplaceAll	: "Remplacer Tout",
DlgSpellBtnUndo			: "Annuler",
DlgSpellNoSuggestions	: "- Aucune suggestion -",
DlgSpellProgress		: "Vérification d'orthographe en cours...",
DlgSpellNoMispell		: "Vérification d'orthographe terminée: Aucune erreur trouvée",
DlgSpellNoChanges		: "Vérification d'orthographe terminée: Pas de modifications",
DlgSpellOneChange		: "Vérification d'orthographe terminée: Un mot modifié",
DlgSpellManyChanges		: "Vérification d'orthographe terminée: %1 mots modifiés",

IeSpellDownload			: "Le Correcteur n'est pas installé. Souhaitez-vous le télécharger maintenant?",

// Button Dialog
DlgButtonText	: "Texte (Valeur)",
DlgButtonType	: "Type",

// Checkbox and Radio Button Dialogs
DlgCheckboxName		: "Nom",
DlgCheckboxValue	: "Valeur",
DlgCheckboxSelected	: "Sélectionné",

// Form Dialog
DlgFormName		: "Nom",
DlgFormAction	: "Action",
DlgFormMethod	: "Méthode",

// Select Field Dialog
DlgSelectName		: "Nom",
DlgSelectValue		: "Valeur",
DlgSelectSize		: "Taille",
DlgSelectLines		: "lignes",
DlgSelectChkMulti	: "Sélection multiple",
DlgSelectOpAvail	: "Options Disponibles",
DlgSelectOpText		: "Texte",
DlgSelectOpValue	: "Valeur",
DlgSelectBtnAdd		: "Ajouter",
DlgSelectBtnModify	: "Modifier",
DlgSelectBtnUp		: "Monter",
DlgSelectBtnDown	: "Descendre",
DlgSelectBtnSetValue : "Valeur sélectionnée",
DlgSelectBtnDelete	: "Supprimer",

// Textarea Dialog
DlgTextareaName	: "Nom",
DlgTextareaCols	: "Colonnes",
DlgTextareaRows	: "Lignes",

// Text Field Dialog
DlgTextName			: "Nom",
DlgTextValue		: "Valeur",
DlgTextCharWidth	: "Largeur en Caractères",
DlgTextMaxChars		: "Nombre Maximum de Caractères",
DlgTextType			: "Type",
DlgTextTypeText		: "Texte",
DlgTextTypePass		: "Mot de Passe",

// Hidden Field Dialog
DlgHiddenName	: "Nom",
DlgHiddenValue	: "Valeur",

// Bulleted List Dialog
BulletedListProp	: "Propriétés de Liste à puces",
NumberedListProp	: "Propriétés de Numérotée",
DlgLstType			: "Type",
DlgLstTypeCircle	: "Cercle",
DlgLstTypeDisc		: "Disque",
DlgLstTypeSquare	: "Carré",
DlgLstTypeNumbers	: "Nombres (1, 2, 3)",
DlgLstTypeLCase		: "Lettres Minuscules (a, b, c)",
DlgLstTypeUCase		: "Lettres Majuscules (A, B, C)",
DlgLstTypeSRoman	: "Chiffres Romains Minuscules (i, ii, iii)",
DlgLstTypeLRoman	: "Chiffres Romains Majuscules (I, II, III)",

// Document Properties Dialog
DlgDocGeneralTab	: "Général",
DlgDocBackTab		: "Fond",
DlgDocColorsTab		: "Couleurs et Marges",
DlgDocMetaTab		: "Métadonnées",

DlgDocPageTitle		: "Titre de la Page",
DlgDocLangDir		: "Sens d'Ecriture",
DlgDocLangDirLTR	: "Gauche vers Droite (LTR)",
DlgDocLangDirRTL	: "Droite vers Gauche (RTL)",
DlgDocLangCode		: "Code Langue",
DlgDocCharSet		: "Encodage de Caractère",
DlgDocCharSetOther	: "Autre Encodage de Caractère",

DlgDocDocType		: "Type de Document",
DlgDocDocTypeOther	: "Autre Type de Document",
DlgDocIncXHTML		: "Inclure les déclarations XHTML",
DlgDocBgColor		: "Couleur de Fond",
DlgDocBgImage		: "Image de Fond",
DlgDocBgNoScroll	: "Image fixe sans défilement",
DlgDocCText			: "Texte",
DlgDocCLink			: "Lien",
DlgDocCVisited		: "Lien Visité",
DlgDocCActive		: "Lien Activé",
DlgDocMargins		: "Marges",
DlgDocMaTop			: "Haut",
DlgDocMaLeft		: "Gauche",
DlgDocMaRight		: "Droite",
DlgDocMaBottom		: "Bas",
DlgDocMeIndex		: "Mots Clés (séparés par des virgules)",
DlgDocMeDescr		: "Description",
DlgDocMeAuthor		: "Auteur",
DlgDocMeCopy		: "Copyright",
DlgDocPreview		: "Prévisualisation",

// Templates Dialog
Templates			: "Modèles",
DlgTemplatesTitle	: "Modèles de Contenu",
DlgTemplatesSelMsg	: "Veuillez sélectionner le modèle à ouvrir dans l'éditeur<br>(le contenu actuel sera remplacé):",
DlgTemplatesLoading	: "Chargement de la liste des modèles. Veuillez patienter...",
DlgTemplatesNoTpl	: "(Aucun modèle disponible)",

// About Dialog
DlgAboutAboutTab	: "A propos de",
DlgAboutBrowserInfoTab	: "Navigateur",
DlgAboutVersion		: "version",
DlgAboutLicense		: "License selon les termes de GNU Lesser General Public License",
DlgAboutInfo		: "Pour plus d'informations, aller à"
}