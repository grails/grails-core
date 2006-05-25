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
 * File Name: vi.js
 * 	Vietnamese language file.
 * 
 * File Authors:
 * 		Phan Binh Giang (bbbgiang@yahoo.com)
 * 		Hà Thanh Hải (thanhhai.ha@gmail.com)
 */

var FCKLang =
{
// Language direction : "ltr" (left to right) or "rtl" (right to left).
Dir					: "ltr",

ToolbarCollapse		: "Thu hẹp Thanh công cụ",
ToolbarExpand		: "Mở rộng Thanh công cụ",

// Toolbar Items and Context Menu
Save				: "Lưu",
NewPage				: "Trang mới",
Preview				: "Xem trước",
Cut					: "Cắt",
Copy				: "Sao",
Paste				: "Dán",
PasteText			: "Dán ký tự dạng văn bản thuần",
PasteWord			: "Dán với định dạng Word",
Print				: "In",
SelectAll			: "Chọn Tất cả",
RemoveFormat		: "Xoá Định dạng",
InsertLinkLbl		: "Liên kết",
InsertLink			: "Chèn/Sửa Liên kết",
RemoveLink			: "Xoá Liên kết",
Anchor				: "Chèn/Sửa Neo",
InsertImageLbl		: "Hình ảnh",
InsertImage			: "Chèn/Sửa Hình ảnh",
InsertFlashLbl		: "Flash",
InsertFlash			: "Chèn/Sửa Flash",
InsertTableLbl		: "Bảng",
InsertTable			: "Chèn/Sửa bảng",
InsertLineLbl		: "Đường phân cách",
InsertLine			: "Chèn đường phân cách ngang",
InsertSpecialCharLbl: "Ký tự đặt biệt",
InsertSpecialChar	: "Chèn Ký tự đặc biệt",
InsertSmileyLbl		: "Hình biểu lộ cảm xúc",
InsertSmiley		: "Chèn Hình biểu lộ cảm xúc (hình mặt cười)",
About				: "Giới thiệu về FCKeditor",
Bold				: "Đậm",
Italic				: "Nghiêng",
Underline			: "Gạch chân",
StrikeThrough		: "Gạch ngang",
Subscript			: "Chỉ số dưới",
Superscript			: "Chỉ số trên",
LeftJustify			: "Canh Trái",
CenterJustify		: "Canh Giữa",
RightJustify		: "Canh Phải",
BlockJustify		: "Canh Đều hai bên",
DecreaseIndent		: "Dịch sang Trái",
IncreaseIndent		: "Dịch sang Phải",
Undo				: "Khôi phục thao tác",
Redo				: "Làm lại thao tác",
NumberedListLbl		: "Danh sách có thứ tự",
NumberedList		: "Chèn/Xoá Danh sách có thứ tự",
BulletedListLbl		: "Danh sách không thứ tự",
BulletedList		: "Chèn/Xoá Danh sách không thứ tự",
ShowTableBorders	: "Hiện thị Đường viền bảng",
ShowDetails			: "Hiển thị Chi tiết",
Style				: "Mẫu",
FontFormat			: "Định dạng",
Font				: "Font",
FontSize			: "Cỡ Chữ",
TextColor			: "Màu Chữ",
BGColor				: "Màu Nền",
Source				: "Mã nguồn",
Find				: "Tìm",
Replace				: "Thay thế",
SpellCheck			: "Kiểm tra Chính tả",
UniversalKeyboard	: "Bàn phím Quốc tế",
PageBreakLbl		: "Page Break",	//MISSING
PageBreak			: "Insert Page Break",	//MISSING

Form			: "Form",
Checkbox		: "Checkbox",
RadioButton		: "Radio Button",
TextField		: "Text Field",
Textarea		: "Textarea",
HiddenField		: "Hidden Field",
Button			: "Button",
SelectionField	: "Selection Field",
ImageButton		: "Image Button",

// Context Menu
EditLink			: "Sửa Liên kết",
InsertRow			: "Chèn Dòng",
DeleteRows			: "Xoá Dòng",
InsertColumn		: "Chèn Cột",
DeleteColumns		: "Xoá Cột",
InsertCell			: "Chèn Ô",
DeleteCells			: "Xoá Ô",
MergeCells			: "Trộn Ô",
SplitCell			: "Chia Ô",
TableDelete			: "Delete Table",	//MISSING
CellProperties		: "Thuộc tính Ô",
TableProperties		: "Thuộc tính Bảng",
ImageProperties		: "Thuộc tính Hình ảnh",
FlashProperties		: "Thuộc tính Flash",

AnchorProp			: "Thuộc tính Neo",
ButtonProp			: "Thuộc tính Button",
CheckboxProp		: "Thuộc tính Nốt kiểm",
HiddenFieldProp		: "Thuộc tính Hidden Field",
RadioButtonProp		: "Thuộc tính Nốt đài",
ImageButtonProp		: "Thuộc tính Image Button",
TextFieldProp		: "Thuộc tính Text Field",
SelectionFieldProp	: "Thuộc tính Selection Field",
TextareaProp		: "Thuộc tính Textarea",
FormProp			: "Thuộc tính Form",

FontFormats			: "Normal;Formatted;Address;Heading 1;Heading 2;Heading 3;Heading 4;Heading 5;Heading 6;Paragraph (DIV)",

// Alerts and Messages
ProcessingXHTML		: "Đang xử lý XHTML. Vui lòng đợi trong giây lát...",
Done				: "Đã hoàn thành",
PasteWordConfirm	: "Văn bản bạn muốn dán có kèm định dạng của Word. Bạn có muốn loại bỏ định dạng Word trước khi dán?",
NotCompatiblePaste	: "Lệnh này chỉ được hỗ trợ từ trình duyệt Internet Explorer phiên bản 5.5 hoặc mới hơn. Bạn có muốn dán nguyên mẫu?",
UnknownToolbarItem	: "Không rõ mục trên thanh công cụ \"%1\"",
UnknownCommand		: "Không rõ lệnh \"%1\"",
NotImplemented		: "Lệnh không được thực hiện",
UnknownToolbarSet	: "Thanh công cụ \"%1\" không tồn tại",
NoActiveX			: "Các thiết lập bảo mật của trình duyệt có thể giới hạn một số chức năng của trình biên tập. Bạn phải bật tùy chọn \"Run ActiveX controls and plug-ins\". Bạn có thể gặp một số lỗi và thấy thiếu đi một số chức năng.",
BrowseServerBlocked : "The resources browser could not be opened. Make sure that all popup blockers are disabled.",	//MISSING
DialogBlocked		: "It was not possible to open the dialog window. Make sure all popup blockers are disabled.",	//MISSING

// Dialogs
DlgBtnOK			: "Đồng ý",
DlgBtnCancel		: "Bỏ qua",
DlgBtnClose			: "Đóng",
DlgBtnBrowseServer	: "Duyệt trên máy chủ",
DlgAdvancedTag		: "Mở rộng",
DlgOpOther			: "&lt;Khác&gt;",
DlgInfoTab			: "Thông tin",
DlgAlertUrl			: "Hãy đưa vào một URL",

// General Dialogs Labels
DlgGenNotSet		: "&lt;không thiết lập&gt;",
DlgGenId			: "Định danh",
DlgGenLangDir		: "Đường dẫn Ngôn ngữ",
DlgGenLangDirLtr	: "Trái sang Phải (LTR)",
DlgGenLangDirRtl	: "Phải sang Trái (RTL)",
DlgGenLangCode		: "Mã Ngôn ngữ",
DlgGenAccessKey		: "Phím Hỗ trợ truy cập",
DlgGenName			: "Tên",
DlgGenTabIndex		: "Chỉ số của Tab",
DlgGenLongDescr		: "Mô tả URL",
DlgGenClass			: "Stylesheet Classes",
DlgGenTitle			: "Advisory Title",
DlgGenContType		: "Advisory Content Type",
DlgGenLinkCharset	: "Bảng mã của tài nguyên được liên kết đến",
DlgGenStyle			: "Mẫu",

// Image Dialog
DlgImgTitle			: "Thuộc tính Hình ảnh",
DlgImgInfoTab		: "Thông tin Hình ảnh",
DlgImgBtnUpload		: "Tải lên Máy chủ",
DlgImgURL			: "URL",
DlgImgUpload		: "Tải lên",
DlgImgAlt			: "Chú thích Hình ảnh",
DlgImgWidth			: "Rộng",
DlgImgHeight		: "Cao",
DlgImgLockRatio		: "Giữ tỷ lệ",
DlgBtnResetSize		: "Kích thước gốc",
DlgImgBorder		: "Đường viền",
DlgImgHSpace		: "HSpace",
DlgImgVSpace		: "VSpace",
DlgImgAlign			: "Vị trí",
DlgImgAlignLeft		: "Trái",
DlgImgAlignAbsBottom: "Dưới tuyệt đối",
DlgImgAlignAbsMiddle: "Giữa tuyệt đối",
DlgImgAlignBaseline	: "Baseline",
DlgImgAlignBottom	: "Dưới",
DlgImgAlignMiddle	: "Giữa",
DlgImgAlignRight	: "Phải",
DlgImgAlignTextTop	: "Phía trên chữ",
DlgImgAlignTop		: "Trên",
DlgImgPreview		: "Xem trước",
DlgImgAlertUrl		: "Hãy đưa vào URL của hình ảnh",
DlgImgLinkTab		: "Liên kết",

// Flash Dialog
DlgFlashTitle		: "Thuộc tính Flash",
DlgFlashChkPlay		: "Tự động Chạy",
DlgFlashChkLoop		: "Lặp",
DlgFlashChkMenu		: "Cho phép bật Menu của Flash",
DlgFlashScale		: "Tỷ lệ",
DlgFlashScaleAll	: "Hiển thị tất cả",
DlgFlashScaleNoBorder	: "Không đường viền",
DlgFlashScaleFit	: "Vừa vặn chính xác",

// Link Dialog
DlgLnkWindowTitle	: "Liên kết",
DlgLnkInfoTab		: "Thông tin Liên kết",
DlgLnkTargetTab		: "Đích",

DlgLnkType			: "Kiểu Liên kết",
DlgLnkTypeURL		: "URL",
DlgLnkTypeAnchor	: "Neo trong trang này",
DlgLnkTypeEMail		: "Thư điện tử",
DlgLnkProto			: "Giao thức",
DlgLnkProtoOther	: "&lt;khác&gt;",
DlgLnkURL			: "URL",
DlgLnkAnchorSel		: "Chọn một Neo",
DlgLnkAnchorByName	: "Theo Tên Neo",
DlgLnkAnchorById	: "Theo Định danh Element",
DlgLnkNoAnchors		: "&lt;Không có Neo nào trong tài liệu&gt;",
DlgLnkEMail			: "Thư điện tử",
DlgLnkEMailSubject	: "Tựa đề Thông điệp",
DlgLnkEMailBody		: "Nội dung Thông điệp",
DlgLnkUpload		: "Tải lên",
DlgLnkBtnUpload		: "Tải lên Máy chủ",

DlgLnkTarget		: "Đích",
DlgLnkTargetFrame	: "&lt;frame&gt;",
DlgLnkTargetPopup	: "&lt;cửa sổ popup&gt;",
DlgLnkTargetBlank	: "Cửa sổ mới (_blank)",
DlgLnkTargetParent	: "Cửa sổ cha (_parent)",
DlgLnkTargetSelf	: "Cùng cửa sổ (_self)",
DlgLnkTargetTop		: "Cửa sổ trên cùng(_top)",
DlgLnkTargetFrameName	: "Tên Frame đích",
DlgLnkPopWinName	: "Tên Cửa sổ Popup",
DlgLnkPopWinFeat	: "Đặc điểm của Cửa sổ Popup",
DlgLnkPopResize		: "Kích thước thay đổi",
DlgLnkPopLocation	: "Thanh vị trí",
DlgLnkPopMenu		: "Thanh Menu",
DlgLnkPopScroll		: "Thanh cuộn",
DlgLnkPopStatus		: "Thanh trạng thái",
DlgLnkPopToolbar	: "Thanh công cụ",
DlgLnkPopFullScrn	: "Toàn màn hình (IE)",
DlgLnkPopDependent	: "Phụ thuộc (Netscape)",
DlgLnkPopWidth		: "Rộng",
DlgLnkPopHeight		: "Cao",
DlgLnkPopLeft		: "Vị trí Trái",
DlgLnkPopTop		: "Vị trí Trên",

DlnLnkMsgNoUrl		: "Hãy đưa vào Liên kết URL",
DlnLnkMsgNoEMail	: "Hãy đưa vào địa chỉ thư điện tử",
DlnLnkMsgNoAnchor	: "Hãy chọn một Neo",

// Color Dialog
DlgColorTitle		: "Chọn màu",
DlgColorBtnClear	: "Xoá",
DlgColorHighlight	: "Tô sáng",
DlgColorSelected	: "Đã chọn",

// Smiley Dialog
DlgSmileyTitle		: "Chèn hình biểu lộ cảm xúc",

// Special Character Dialog
DlgSpecialCharTitle	: "Chọn ký tự đặc biệt",

// Table Dialog
DlgTableTitle		: "Thuộc tính bảng",
DlgTableRows		: "Dòng",
DlgTableColumns		: "Cột",
DlgTableBorder		: "Cỡ Đường viền",
DlgTableAlign		: "Canh lề",
DlgTableAlignNotSet	: "<Không thiết lập>",
DlgTableAlignLeft	: "Trái",
DlgTableAlignCenter	: "Giữa",
DlgTableAlignRight	: "Phải",
DlgTableWidth		: "Rộng",
DlgTableWidthPx		: "điểm",
DlgTableWidthPc		: "%",
DlgTableHeight		: "Cao",
DlgTableCellSpace	: "Khoảng cách Ô",
DlgTableCellPad		: "Đệm Ô",
DlgTableCaption		: "Đầu đề",
DlgTableSummary		: "Summary",	//MISSING

// Table Cell Dialog
DlgCellTitle		: "Thuộc tính Ô",
DlgCellWidth		: "Rộng",
DlgCellWidthPx		: "điểm",
DlgCellWidthPc		: "%",
DlgCellHeight		: "Cao",
DlgCellWordWrap		: "Bọc từ",
DlgCellWordWrapNotSet	: "&lt;Không thiết lập&gt;",
DlgCellWordWrapYes	: "Đồng ý",
DlgCellWordWrapNo	: "Không",
DlgCellHorAlign		: "Sắp xếp Ngang",
DlgCellHorAlignNotSet	: "&lt;Không thiết lập&gt;",
DlgCellHorAlignLeft	: "Trái",
DlgCellHorAlignCenter	: "Giữa",
DlgCellHorAlignRight: "Phải",
DlgCellVerAlign		: "Sắp xếp Dọc",
DlgCellVerAlignNotSet	: "&lt;Không thiết lập&gt;",
DlgCellVerAlignTop	: "Trên",
DlgCellVerAlignMiddle	: "Giữa",
DlgCellVerAlignBottom	: "Dưới",
DlgCellVerAlignBaseline	: "Baseline",
DlgCellRowSpan		: "Rows Span",
DlgCellCollSpan		: "Columns Span",
DlgCellBackColor	: "Màu nền",
DlgCellBorderColor	: "Màu viền",
DlgCellBtnSelect	: "Chọn...",

// Find Dialog
DlgFindTitle		: "Tìm",
DlgFindFindBtn		: "Tìm",
DlgFindNotFoundMsg	: "Không tìm thấy chuỗi cần tìm.",

// Replace Dialog
DlgReplaceTitle			: "Thay thế",
DlgReplaceFindLbl		: "Tìm chuỗi:",
DlgReplaceReplaceLbl	: "Thay bằng:",
DlgReplaceCaseChk		: "Đúng chữ Hoa/Thường",
DlgReplaceReplaceBtn	: "Thay thế",
DlgReplaceReplAllBtn	: "Thay thế Tất cả",
DlgReplaceWordChk		: "Đúng toàn bộ từ",

// Paste Operations / Dialog
PasteErrorPaste	: "Các thiết lập bảo mật của trình duyệt không cho phép trình biên tập tự động thực thi lệnh dán. Hãy sử dụng bàn phím cho lệnh này (Ctrl+V).",
PasteErrorCut	: "Các thiết lập bảo mật của trình duyệt không cho phép trình biên tập tự động thực thi lệnh cắt. Hãy sử dụng bàn phím cho lệnh này (Ctrl+X).",
PasteErrorCopy	: "Các thiết lập bảo mật của trình duyệt không cho phép trình biên tập tự động thực thi lệnh sao chép. Hãy sử dụng bàn phím cho lệnh này (Ctrl+C).",

PasteAsText		: "Dán theo định dạng văn bản thuần",
PasteFromWord	: "Dán với định dạng Word",

DlgPasteMsg2	: "Hãy dán vào trong khung bên dưới, sử dụng tổ hợp phím (<STRONG>Ctrl+V</STRONG>) và nhấn vào nút <STRONG>Đồng ý</STRONG>.",
DlgPasteIgnoreFont		: "Chấp nhận các định dạng Font",
DlgPasteRemoveStyles	: "Xoá tất cả các định dạng Styles",
DlgPasteCleanBox		: "Xoá sạch",


// Color Picker
ColorAutomatic	: "Tự động",
ColorMoreColors	: "Màu khác...",

// Document Properties
DocProps		: "Thuộc tính tài liệu",

// Anchor Dialog
DlgAnchorTitle		: "Thuộc tính Neo",
DlgAnchorName		: "Tên của Neo",
DlgAnchorErrorName	: "Hãy đưa vào tên của Neo",

// Speller Pages Dialog
DlgSpellNotInDic		: "Không có trong từ điển",
DlgSpellChangeTo		: "Chuyển thành",
DlgSpellBtnIgnore		: "Bỏ qua",
DlgSpellBtnIgnoreAll	: "Bỏ qua Tất cả",
DlgSpellBtnReplace		: "Thay thế",
DlgSpellBtnReplaceAll	: "Thay thế Tất cả",
DlgSpellBtnUndo			: "Phục hồi lại",
DlgSpellNoSuggestions	: "- Không đưa ra gợi ý về từ -",
DlgSpellProgress		: "Đang tiến hành kiểm tra chính tả...",
DlgSpellNoMispell		: "Hoàn tất kiểm tra chính tả: Không có lỗi chính tả",
DlgSpellNoChanges		: "Hoàn tất kiểm tra chính tả: Không từ nào được thay đổi",
DlgSpellOneChange		: "Hoàn tất kiểm tra chính tả: Một từ đã được thay đổi",
DlgSpellManyChanges		: "Hoàn tất kiểm tra chính tả: %1 từ đã được thay đổi",

IeSpellDownload			: "Chức năng kiểm tra chính tả chưa được cài đặt. Bạn có tải về ngay bây giờ?",

// Button Dialog
DlgButtonText	: "Văn bản (Giá trị)",
DlgButtonType	: "Kiểu",

// Checkbox and Radio Button Dialogs
DlgCheckboxName		: "Tên",
DlgCheckboxValue	: "Giá trị",
DlgCheckboxSelected	: "Được chọn",

// Form Dialog
DlgFormName		: "Tên",
DlgFormAction	: "Hành động",
DlgFormMethod	: "Phương thức",

// Select Field Dialog
DlgSelectName		: "Tên",
DlgSelectValue		: "Giá trị",
DlgSelectSize		: "Kích cỡ",
DlgSelectLines		: "dòng",
DlgSelectChkMulti	: "Cho phép chọn nhiều",
DlgSelectOpAvail	: "Các tùy chọn có thể sử dụng",
DlgSelectOpText		: "Văn bản",
DlgSelectOpValue	: "Giá trị",
DlgSelectBtnAdd		: "Thêm",
DlgSelectBtnModify	: "Thay đổi",
DlgSelectBtnUp		: "Lên",
DlgSelectBtnDown	: "Xuống",
DlgSelectBtnSetValue : "Giá trị được chọn",
DlgSelectBtnDelete	: "Xoá",

// Textarea Dialog
DlgTextareaName	: "Tên",
DlgTextareaCols	: "Cột",
DlgTextareaRows	: "Dòng",

// Text Field Dialog
DlgTextName			: "Tên",
DlgTextValue		: "Giá trị",
DlgTextCharWidth	: "Rộng",
DlgTextMaxChars		: "Số Ký tự tối đa",
DlgTextType			: "Kiểu",
DlgTextTypeText		: "Ký tự",
DlgTextTypePass		: "Mật khẩu",

// Hidden Field Dialog
DlgHiddenName	: "Tên",
DlgHiddenValue	: "Giá trị",

// Bulleted List Dialog
BulletedListProp	: "Thuộc tính Danh sách không thứ tự",
NumberedListProp	: "Thuộc tính Danh sách có thứ tự",
DlgLstType			: "Kiểu",
DlgLstTypeCircle	: "Hình tròn",
DlgLstTypeDisc		: "Hình đĩa",
DlgLstTypeSquare	: "Hình vuông",
DlgLstTypeNumbers	: "Số thứ tự (1, 2, 3)",
DlgLstTypeLCase		: "Chữ cái thường (a, b, c)",
DlgLstTypeUCase		: "Chữ cái hoa (A, B, C)",
DlgLstTypeSRoman	: "Số La-mã thường (i, ii, iii)",
DlgLstTypeLRoman	: "Số La-mã hoa (I, II, III)",

// Document Properties Dialog
DlgDocGeneralTab	: "Toàn thể",
DlgDocBackTab		: "Nền",
DlgDocColorsTab		: "Màu sắc và Đường biên",
DlgDocMetaTab		: "Siêu dữ liệu (Meta Data)",

DlgDocPageTitle		: "Tiêu đề Trang",
DlgDocLangDir		: "Đường dẫn Ngôn Ngữ",
DlgDocLangDirLTR	: "Trái sang Phải (LTR)",
DlgDocLangDirRTL	: "Phải sang Trái (RTL)",
DlgDocLangCode		: "Mã Ngôn ngữ",
DlgDocCharSet		: "Bảng mã ký tự",
DlgDocCharSetOther	: "Bảng mã ký tự khác",

DlgDocDocType		: "Kiểu Đề mục Tài liệu",
DlgDocDocTypeOther	: "Kiểu Đề mục Tài liệu khác",
DlgDocIncXHTML		: "Bao gồm cả định nghĩa XHTML",
DlgDocBgColor		: "Màu nền",
DlgDocBgImage		: "URL của Hình ảnh nền",
DlgDocBgNoScroll	: "Không cuộn nền",
DlgDocCText			: "Văn bản",
DlgDocCLink			: "Liên kết",
DlgDocCVisited		: "Liên kết Đã ghé thăm",
DlgDocCActive		: "Liên kết Hiện hành",
DlgDocMargins		: "Đường biên của Trang",
DlgDocMaTop			: "Trên",
DlgDocMaLeft		: "Trái",
DlgDocMaRight		: "Phải",
DlgDocMaBottom		: "Dưới",
DlgDocMeIndex		: "Các từ khóa chỉ mục tài liệu (phân cách bởi dấu phẩy)",
DlgDocMeDescr		: "Mô tả tài liệu",
DlgDocMeAuthor		: "Tác giả",
DlgDocMeCopy		: "Bản quyền",
DlgDocPreview		: "Xem trước",

// Templates Dialog
Templates			: "Mẫu dựng sẵn",
DlgTemplatesTitle	: "Nội dung Mẫu dựng sẵn",
DlgTemplatesSelMsg	: "Hãy chọn Mẫu dựng sẵn để mở trogn trình biên tập<br>(nội dung hiện tại sẽ bị mất):",
DlgTemplatesLoading	: "Đang nạp Danh sách Mẫu dựng sẵn. Vui lòng đợi trong giây lát...",
DlgTemplatesNoTpl	: "(Không có Mẫu dựng sẵn nào được định nghĩa)",

// About Dialog
DlgAboutAboutTab	: "Giới thiệu",
DlgAboutBrowserInfoTab	: "Thông tin trình duyệt",
DlgAboutVersion		: "phiên bản",
DlgAboutLicense		: "Licensed under the terms of the GNU Lesser General Public License",
DlgAboutInfo		: "Để biết thêm thông tin, hãy truy cập"
}