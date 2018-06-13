<%@ page contentType="text/html; charset=utf-8" session="false"%>
<%@ taglib uri="/cmsTag" prefix="cms"%>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
		<meta http-equiv="content-type" content="text/html; charset=utf-8" />
		<link href="../style/blue/css/main.css" type="text/css" rel="stylesheet" />
		<link href="../style/blue/css/reset-min.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="../javascript/commonUtil_src.js"></script>
		<script type="text/javascript" src="../common/js/jquery-1.7.gzjs"></script>
		<script language="javascript" type="text/javascript" src="../javascript/My97DatePicker/WdatePicker.js"></script>

		<script type="text/javascript" src="../javascript/showImage/fb/jquery.mousewheel-3.0.4.pack.js"></script>
		<script type="text/javascript" src="../javascript/showImage/fb/jquery.fancybox-1.3.4.pack.js"></script>
		<link rel="stylesheet" type="text/css" href="../javascript/showImage/fb/jquery.fancybox-1.3.4.css" media="screen" />
		<script>  
		
		basePath = '<cms:BasePath/>';
		
		var hasError = false;
		//验证
		$(window).load(function()
		{
			$("#configName").bind('focus', function() 
			{
				var target = $("#configName").val();
				//alert(target);
			    if(target == '')
				{
					hasError = true;
  					showTips('configName','不可为空');
  				}
  				else
  				{
  					hasError = false;
  				}
			});	
			
			$("#configName").bind('propertychange', function() 
						{
						   $( 'div.configName_jtop_ui_tips_class' ).remove();
  							
							var target = $("#configName").val();

    						if(target == '')
    						{
    							hasError = true;
    							showTips('configName','不可为空');               					
  							}
  							else
  							{
  								hasError = false;
  							}
  							
  							
						});
						
						
			
		
		})
	
	     var api = frameElement.api, W = api.opener; 
		
		 function showMsg(msg)
		 {
		
		    W.$.dialog(
		    { 
		   					title :'提示',
		    				width: '130px', 
		    				height: '60px', 
		                    lock: true, 
		                    parent:api,
		    				icon: '32X32/i.png', 
		    				
		                    content: msg,

		    				cancel: true
			});
		}
      
	
		 $(function()
		 {
        	//图片查看效果
 		  	loadImageShow();
 		 }) 
         
         
        	
      </script>
	</head>
	<body>

		

		<cms:SystemGbInfo configFlag="${param.configFlag}" gbId="${param.gbId}">
			<form id="guestbookForm" name="guestbookForm" method="post">
				<table width="100%" border="0" cellspacing="0" cellpadding="0">
					<tr>
						<td align="left" valign="top">

							<!--main start-->
							<div class="auntion_tagRoom" style="margin-top:2px">
								<ul>
									<li id="two1" onclick="setTab('two',1,2)" class="selectTag">
										<a href="javascript:;"><img src="../style/icons/socket.png" width="16" height="16" />留言内容&nbsp;</a>
									</li>
									<li id="two2" onclick="setTab('two',2,2)">
										<a href="javascript:;"><img src="../style/icons/table.png" width="16" height="16" />附加信息&nbsp;</a>
									</li>

								</ul>
							</div>

							<div class="auntion_tagRoom_Content">
								<div id="g3_two_1" class="auntion_Room_C_imglist" style="display:block;">
									<ul>
										<li>
											<table width="100%" border="0" cellpadding="0" cellspacing="0" class="form-table">
												<tr>
													<td width="14%" class="input-title">
														<strong>标题</strong>
													</td>
													<td class="td-input">
														<input type="text" style="width:698px" readonly class="form-input" value="${GbInfo.gbTitle}"></input>

													</td>
												</tr>

												<tr id="dataTypeDIV">
													<td class="input-title">
														<strong>留言</strong>
													</td>
													<td class="td-input">
														<textarea style="height:220px;width:698px" class="form-textarea" readonly>${GbInfo.gbText}</textarea>
													</td>
												</tr>

												<tr>
													<td class="input-title">
														<strong>回复</strong>
													</td>
													<td class="td-input">
														<textarea id="replyText" name="replyText" style="height:170px;width:698px" class="form-textarea">${GbInfo.replyText}</textarea>
													</td>
												</tr>

												<tr>
													<td class="input-title">
														<strong> </strong>
													</td>
													<td class="td-input">
														<table width="100%" border="0" cellpadding="0" cellspacing="0" class="form-table-upload">
															<tr>
																<td width="31%">
																	留言人: ${GbInfo.gbMan}
																</td>
																<td width="40%">
																	留言时间:
																	<cms:FormatDate date="${GbInfo.addDate}" />
																</td>
																<td width="40%">
																	IP: ${GbInfo.ip}
																</td>

															</tr>

														</table>
													</td>
												</tr>

												<tr>
													<td class="input-title">
														<strong> </strong>
													</td>
													<td class="td-input">

														<table width="100%" border="0" cellpadding="0" cellspacing="0" class="form-table-upload">
															<tr>
																<td width="31%">
																	回复人:
																	<cms:if test="${GbInfo.replyMan==null || GbInfo.replyMan==''}">
																		<font style="font-style:italic;"> 没有回复 </font>
																	</cms:if>
																	<cms:else>
																		${GbInfo.replyMan}
																	</cms:else>
																</td>

																<td width="40%">
																	回复时间:
																	<cms:if test="${GbInfo.replyDate==null || GbInfo.replyDate==''}">
																		<font style="font-style:italic;"> 无信息 </font>
																	</cms:if>
																	<cms:else>
																		<cms:FormatDate date="${GbInfo.replyDate}" />
																	</cms:else>
																</td>
																<td width="40%">
																	状态:
																	<cms:if test="${GbInfo.isCensor==1}">
																		<font color="green">通过</font>
																	</cms:if>
																	<cms:else>
																		未通过
																	</cms:else>
																	|
																	<cms:if test="${GbInfo.isOpen==1}">
																		<font color="green">公开</font>
																	</cms:if>
																	<cms:else>
																		不公开
																	</cms:else>
																</td>

															</tr>

														</table>
													</td>
												</tr>

												<!-- 以下为独立选项 start -->


											</table>

											<div style="height:15px;"></div>
											<div class="breadnavTab"  >
												<table width="100%" border="0" cellpadding="0" cellspacing="0">
													<tr class="btnbg100">
														<div style="float:right">
																	
															<cms:if test="${GbInfo.isCensor==1}">
																<a href="javascript:changeGbInfoStatus('censor','0');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>不通过&nbsp;</b> </a>
															</cms:if>
															<cms:else>
																<a href="javascript:changeGbInfoStatus('censor','1');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>通过审核&nbsp;</b> </a>
															</cms:else>

															<cms:if test="${GbInfo.isOpen==1}">
																<a href="javascript:changeGbInfoStatus('open','0');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>不公开&nbsp;</b> </a>
															</cms:if>
															<cms:else>
																<a href="javascript:changeGbInfoStatus('open','1');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>公开留言&nbsp;</b> </a>
															</cms:else>

															<cms:if test="${GbInfo.replyMan == null}">
																<a href="javascript:replyGbInfo();"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>回复&nbsp;</b> </a>

															</cms:if>
															<cms:else>
																<a href="javascript:replyGbInfo();"  class="btnwithico"><img src="../style/icon/close.png" width="16" height="16"><b>修改回复&nbsp;</b> </a>

															</cms:else>


														</div>
													</tr>
												</table>
											</div>
										</li>
									</ul>
								</div>

								<!-- 第二部分: -->
								<div id="g3_two_2" class="auntion_Room_C_imglist" style="display:none;">
									
									<ul>
										<li>
											
											
											<cms:SystemGbConfig configId="${param.configId}">
												
											<table width="100%" border="0" cellpadding="0" cellspacing="0" class="form-table-big-content" >
												<cms:SystemModelFiledList modelId="${GbCfg.infoModelId}" showMode="true">
													<cms:SystemModelFiled>
														<tr>
															<td class="input-title" width="20%">
																<cms:SystemModelRowFiled >
																	<!-- 图集字段区域 -->
																	<cms:if test="${RowFiled.htmlElementId==14}">
																	${RowFiled.showName}:										
															
															</td>
															<td class="td-input">
																<table border="0" cellpadding="0" cellspacing="0" class="form-table-upload" style="padding-top:0px;">
																	<tr>
																		<td>
																			<!-- 图集操作 -->
																			<table width="100%" border="0" cellpadding="0" cellspacing="0" style="padding-top:0px;">
																				<tr style="padding-top:0px;">
																					<td style="padding-top:0px;">
																					
																			                <a onclick="javascript:deleteAllGroupPhoto('${RowFiled.fieldSign}');" class="btnwithico"> <img src="../../core/style/icons/image--minus.png" alt="" /><b>全部删除&nbsp;</b> </a>
																							<a onclick="javascript:showModuleImageGroupDialog('${RowFiled.fieldSign}');" class="btnwithico"> <img src="../../core/style/icons/images.png" alt="" /><b>多图上传&nbsp;</b> </a>
				
																					</td>
																					<td style="padding-top:0px;">
																						&nbsp;&nbsp;&nbsp;&nbsp;
																						<input type="text" size="5" id="${RowFiled.fieldSign}CmsSysMaxWidth" class="form-input" value="${RowFiled.fieldInfo.imageW}"></input>
																						宽度&nbsp;&nbsp;&nbsp;&nbsp;
																						<input type="text" size="5" id="${RowFiled.fieldSign}CmsSysMaxHeight" class="form-input" value="${RowFiled.fieldInfo.imageH}"></input>
																						高度&nbsp;&nbsp;&nbsp;&nbsp;
																						<select class="form-select" id="${RowFiled.fieldSign}CmsSysDisposeMode">
																							<option value="0">
																								原宽高
																							</option>
																							<option value="1">
																								按宽度
																							</option>
																							<option value="2">
																								按高度
																							</option>
																							<option value="3">
																								按宽高&nbsp;&nbsp;
																							</option>
																						</select>
																						缩放&nbsp;&nbsp;&nbsp;水印: 
																						<input class="form-checkbox" disabled type="checkbox" value="1" id="${RowFiled.fieldSign}CmsSysNeedMark" />
																						&nbsp;&nbsp;图片数 [
																						<font color=red><span id="${RowFiled.fieldSign}CmsSysImageGroupCount">-1</span></font> ]&nbsp;&nbsp;张
				
																						<script>
							
																						initSelect('${RowFiled.fieldSign}CmsSysDisposeMode','${RowFiled.fieldInfo.imageDisposeMode}');	
																						initRadio('${RowFiled.fieldSign}CmsSysNeedMark','${RowFiled.fieldInfo.needMark}');	
																
																						</script>
																					</td>
																				</tr>
				
																			</table>
																			<!-- 图集操作结束 -->
																		</td>
																	</tr>
																</table>
															</td>
														</tr>
														<!-- 图集内容开始 -->
														<tr>
															<td class="input-title" width="20%">
																<!-- 图集内容空标题 -->
															</td>
															<td class="td-input" style="padding-top:0px;">
																<table border="0" cellpadding="0" cellspacing="0" class="form-table-upload" style="padding-top:0px;">
																	<tr style="padding-top:0px;">
																		<td style="padding-top:0px;">
																			<input type="hidden" id="${RowFiled.fieldSign}CmsSysImageCurrentCount" name="${RowFiled.fieldSign}CmsSysImageCurrentCount" value="0" />
																			<input type="hidden" id="${RowFiled.fieldSign}CmsSysImageCover" name="${RowFiled.fieldSign}CmsSysImageCover" value="" />
																			<input type="hidden" id="${RowFiled.fieldSign}CmsSysImageArrayLength" name="${RowFiled.fieldSign}CmsSysImageArrayLength" value="0" />
				
																			<div id="${RowFiled.fieldSign}CmsSysImageUploadTab">
																				<!-- 图片信息区 -->
																				<script>
																		
																				//派序用
				
																				allImageGroupSortInfo['${RowFiled.fieldSign}'] = new Array();
																				
																				<cms:PhotoGroup  contentId="${RowFiled.info.contentId}" serverMode="true">
																				
																					addGroupPhotoToPage('${RowFiled.fieldSign}','${RowFiled.fieldInfo.imageW}','${RowFiled.fieldInfo.imageH}','${Photo.orderFlag}','${Photo.url}','${Photo.resizeUrl}','${Photo.resId}','${Photo.reUrl}','${Photo.photoName}','${Photo.height}','${Photo.width}','${Photo.photoDesc}');
																					//封面
																					if('${Photo.isCover}' == 1)
																					{
																						document.getElementById('${RowFiled.fieldSign}-cover-${Photo.orderFlag-1}').checked = true;
																						$("#${RowFiled.fieldSign}CmsSysImageCover").val('${Photo.reUrl}');
																					}	
																				
																				</cms:PhotoGroup>
																				
																				//图片数
																				document.getElementById('${RowFiled.fieldSign}CmsSysImageGroupCount').innerHTML = document.getElementById('${RowFiled.fieldSign}CmsSysImageCurrentCount').value;
																				
																				var sortArray = allImageGroupSortInfo['${RowFiled.fieldSign}'];
																				
																				document.getElementById('${RowFiled.fieldSign}CmsSysImageArrayLength').value = allImageGroupSortInfo['${RowFiled.fieldSign}'].length;
																				
																				//alert(document.getElementById('${RowFiled.fieldSign}CmsSysImageCurrentCount').value);
																				</script>
																			</div>
				
																		</td>
				
																		<!-- 图集内容结束 -->
																		</cms:if>
				
																		<!-- 普通字段区域开始 -->
																		<cms:else>
																			<cms:if test="${status.index == 0}">
																			${RowFiled.showName}:									
																		</td>
																				<td class="td-input">
																				
																					<table border="0" cellpadding="0" cellspacing="0" class="form-table-upload">
																						<tr>
																							<td>
																								<table  border="0"  cellpadding="0" cellspacing="0">
																									 <tr>
																									 	<td>
																									 		<div id="sys-obj-${RowFiled.fieldSign}">${RowFiled.editModeLayoutHtml}
																											<cms:if test="${RowFiled.isMustFill==1}">
																												<span class="red">*</span>
																												<span class="ps"></span>
																											</cms:if>
																											</div>
																									 	</td>
																									 	<td>
																									 		<div style="width:${RowFiled.blankCount}px;"></div>
																									 	</td>
																									 </tr>
																								 
																								 </table>
																								
																							</td>
																							
																							</cms:if>
																							<cms:else>
																								<td>
																									<span class="input-title">${RowFiled.showName}:</span>&nbsp;
																								</td>
																								<td>
																									<table  border="0"  cellpadding="0" cellspacing="0">
																									 <tr>
																									 	<td>
																									 		<div id="sys-obj-${RowFiled.fieldSign}">${RowFiled.editModeLayoutHtml}
																											<cms:if test="${RowFiled.isMustFill==1}">
																												<span class="red">*</span>
																												<span class="ps"></span>
																											</cms:if>
																											</div>
																									 	</td>
																									 	<td>
																									 		<div style="width:${RowFiled.blankCount}px;"></div>
																									 	</td>
																									 </tr>
																								 
																								  </table>
																									
																								</td>
																								
																							</cms:else>
																							</cms:else>
																							</cms:SystemModelRowFiled>
																						</tr>
																					</table>
																					
																				</td>
																	</tr>
																	</cms:SystemModelFiled>
																	</cms:SystemModelFiledList>
																	
																	
																	<cms:Empty flag="ModelFiled">
														

																		<tr>
																			<td>
																				<center>
																				<table class="listdate" width="98%" cellpadding="0" cellspacing="0">
				
																					<tr>
																						<td class="tdbgyew" style="height:20px;">
				
																							
																								<center>当前没有数据!</center>
																							
																							</div>
				
																						</td>
																					</tr>
				
																				</table>
																				</center>
																			</td>
																		</tr>
				
																	
		
									
																</cms:Empty>
																	
																</table>
														
															</table>
												

											</cms:SystemGbConfig>
											
											<div style="height:15px;"></div>
											<div class="breadnavTab" >
												<table width="100%" border="0" cellpadding="0" cellspacing="0">
													<tr class="btnbg100">
														<div style="float:right">
															<a href="javascript:openSelectReplyDialog();"  class="btnwithico"><img src="../style/icons/mail.png" width="16" height="16"><b>指定回复人&nbsp;</b> </a>
														
															<cms:if test="${GbInfo.isCensor==1}">
																<a href="javascript:changeGbInfoStatus('censor','0');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>不通过&nbsp;</b> </a>
															</cms:if>
															<cms:else>
																<a href="javascript:changeGbInfoStatus('censor','1');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>通过审核&nbsp;</b> </a>
															</cms:else>

															<cms:if test="${GbInfo.isOpen==1}">
																<a href="javascript:changeGbInfoStatus('open','0');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>不公开&nbsp;</b> </a>
															</cms:if>
															<cms:else>
																<a href="javascript:changeGbInfoStatus('open','1');"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>公开留言&nbsp;</b> </a>
															</cms:else>

															<cms:if test="${GbInfo.replyMan == null}">
																<a href="javascript:replyGbInfo();"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>回复&nbsp;</b> </a>

															</cms:if>
															<cms:else>
																<a href="javascript:replyGbInfo();"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>修改回复&nbsp;</b> </a>

															</cms:else>
															<a href="javascript:close();"  class="btnwithico"><img src="../style/icon/close.png" width="16" height="16"><b>关闭&nbsp;</b> </a>
														
															

														</div>
													</tr>
												</table>
											</div>

											
										</li>
									</ul>
								</div>


							</div>

						</td>
					</tr>
				</table>

				<!-- hidden -->
				<input type="hidden" name="configFlag" id="configFlag" value="${param.configFlag}" />

				<input type="hidden" name="gbId" id="gbId" value="${param.gbId}" />
				
				<input type="hidden" name="configId" id="configId" value="${param.configId}" />

			</form>
			<!--[if lt IE 7]>
        <script type="text/javascript" src="js/unitpngfix.js"></script>
<![endif]-->
	</body>
</html>
<script type="text/javascript">


function setTab(flag,pos,size)
{
	if(!hasError)
	{
		setTab2(flag,pos,size);
	}

}

  
  
function close()
{
	api.close();
	
	if(W.window.location.href.indexOf('/core/console/Workbench.jsp') != -1)
	{
		replaceUrlParam(W.window.location, 'tab=2');
	}
	else
	{
		W.window.location.reload();
	}
}

function openSelectReplyDialog()
{
	 W.$.dialog({ 
    	title : '选取回复人',
    	width: '650px', 
    	height: '540px',
    	parent:api,
    	lock: true, 
    	max: false,
        min: false,
        resize: false,
        
        content: 'url:<cms:BasePath/>core/guestbook/SelectManagerToReply.jsp?gbId=${param.gbId}&configId=${param.configId}'
	});
}


function replyGbInfo()
{
    var url = "../../guestbook/replayGbInfo.do"+"?<cms:Token mode='param'/>";
 		
 	 
		var postData = encodeURI($("#replyText,#configFlag,#configId,#gbId").serialize());
		
		postData = postData.replace(/\+/g, " ");
	    postData = encodeData(postData);
 		
 		$.ajax({
      		type: "POST",
       		url: url,
       		data:postData,
   
       		success: function(mg)
            {     
               var msg = eval("("+mg+")");
            	 
               if('success' == msg)
               {
               		 
               		W.$.dialog.tips('回复留言成功...',1); 
               		window.location.reload();
               } 	
               else
               {
               	     W.$.dialog(
					{ 
								   					title :'提示',
								    				width: '200px', 
								    				height: '60px', 
								                    lock: true, 
								                    parent:api,
								    				icon: '32X32/fail.png', 
								    				
								                    content: "执行失败，无权限请联系管理员！",
						
								    				cancel: true
				   });
               }   
              
            }
     	});	
}


function changeGbInfoStatus(action,flag)
{
	var url = "../../guestbook/changeStatus.do?action="+action+"&flag="+flag+'&id='+$("#gbId").val()+"&configId=${param.configId}&<cms:Token mode='param'/>";
 		
 		$.ajax({
      		type: "POST",
       		url: url,
       		data:'',
   
       		success: function(mg)
            {     
            	 var msg = eval("("+mg+")");
            	 
               if('success' == msg)
               {
               		 
               		window.location.reload();
               		W.$.dialog.tips('留言状态改动成功...',1); 
               		
                
               } 	
               else
               {
               	    W.$.dialog(
					{ 
								   					title :'提示',
								    				width: '200px', 
								    				height: '60px', 
								                    lock: true, 
								                    parent:api,
								    				icon: '32X32/fail.png', 
								    				
								                    content: "执行失败，无权限请联系管理员！",
						
								    				cancel: true
				   });
               }   
              
            }
     	});	
}

 







</script>
</cms:SystemGbInfo>
