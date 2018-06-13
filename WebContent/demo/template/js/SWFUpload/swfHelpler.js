


function initUpload(siteBase, resBase,   sid)
{
	try{
	return new SWFUpload({
			upload_url: siteBase+"content/multiUpload.do;jsessionid="+sid,
			
			// File Upload Settings
			file_size_limit : "3 MB", 
			file_types : "*.png;*.jpeg;*.gif;*.bmp;*.jpg",//���ÿ��ϴ�������
			file_types_description : "�ϴ��ļ�",
			file_upload_limit : "100",
			button_action : -100,

			
			file_queue_error_handler : fileQueueError,//ѡ���ļ������
			file_dialog_complete_handler : fileDialogComplete,//ѡ����ļ����ύ
			file_queued_handler : fileQueuedH,
			upload_progress_handler : uploadProgressH,
			upload_error_handler : uploadError,
			upload_success_handler : uploadSuccess,
			upload_complete_handler : uploadCompleteModuleH,
			
			// Button Settings
			button_image_url : resBase+"js/SWFUpload/upload.png",
			button_placeholder_id : "spanButtonPlaceholderH",
			button_width: 61,
			button_height: 22,
			button_text : '',
			button_text_style : '',
			button_text_top_padding: 0,
			button_text_left_padding: 18,
			button_window_mode: SWFUpload.WINDOW_MODE.TRANSPARENT,
			button_cursor: SWFUpload.CURSOR.HAND,
			
			
			// Flash Settings
			flash_url : resBase+"js/SWFUpload/swfupload.swf",
			use_query_string : true,
			custom_settings : {
			upload_target : ""
			},
			
			// Debug Settings
			debug: false //�Ƿ���ʾ���Դ���
			
			
			});
			}catch(ex)
{
	alert(ex);
}
	
	
}