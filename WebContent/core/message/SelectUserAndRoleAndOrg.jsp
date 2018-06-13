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

		<script>  
		  var api = frameElement.api, W = api.opener; 
         
         
        	
      </script>
	</head>
	<body>


		 
		<form id="flowStepForm" name="flowStepForm" method="post">
			<table width="100%" border="0" cellspacing="0" cellpadding="0">
				<tr>
					<td align="left" valign="top">

						<!--main start-->
						<div class="auntion_tagRoom" style="margin-top:2px">
							<ul>
								<%--<li id="two1" onclick="setTab2('two',1,3)" class="selectTag">
									<a href="javascript:;"><img src="../style/icons/balance.png" width="16" height="16" />参与审核机构&nbsp;</a>
								</li>
								<li id="two2" onclick="setTab2('two',2,3)">
									<a href="javascript:;"><img src="../style/icons/users.png" width="16" height="16" />参与审核角色&nbsp;</a>
								</li>
								--%><li id="two1" onclick="setTab2('two',1,1)">
									<a href="javascript:;"><img src="../style/icons/user-green.png" width="16" height="16" />系统管理员&nbsp;</a>
								</li>
							</ul>
						</div>

						<div class="auntion_tagRoom_Content">

 						<!-- 第四部分：审核用户 -->
						<div id="g3_two_1" class="auntion_Room_C_imglist" style="display:block;">
							<div style="height:10px;"></div>
							<ul>
								<li>
									<div class="DataGrid">
										<table class="listtable" width="100%" border="0" cellpadding="0" cellspacing="0">
											<tr>
												<td id="uid_td25" style="padding: 2px 6px;">
													<div class="DataGrid">
														<table class="listdate" width="100%" cellpadding="0" cellspacing="0">
															<tr class="datahead">
																<td width="1%" height="30">
																	<input class="inputCheckbox"   onclick="javascript:selectAll('checkedUserId',this);" type="checkbox" />
																</td>
																<td width="2%" height="30">
																	<strong>Id</strong>
																</td>
																<td width="8%">
																	<strong>用户名称</strong>
																</td>
																<td width="8%">
																	<strong>真实名称</strong>
																</td>
															</tr>
															<cms:SystemUserList allMode="true">
																<cms:SystemUser>
																	<tr>
																		<td>
																			<input class="inputCheckbox" id="checkedUserId-${SysUser.userId}" name="checkedUserId" value="${SysUser.userId}-${SysUser.userName}" type="checkbox" />
																		</td>
																		<td>
																			${SysUser.userId}
																		</td>
																		<td>
																			${SysUser.userName}
																		</td>
																		<td>
																			${SysUser.userTrueName}
																		</td>
																	</tr>

																</cms:SystemUser>
															</cms:SystemUserList>
														</table>
													</div>
													</li>
													</ul>
													</div>
													<div style="height:40px;"></div>
													<div class="breadnavTab"  >
														<table width="100%" border="0" cellpadding="0" cellspacing="0">
															<tr class="btnbg100">
																<div style="float:right">
																	<a href="javascript:selectReMan();"  class="btnwithico"><img src="../style/icons/tick.png" width="16" height="16"><b>确认收信人&nbsp;</b> </a>

																	<a href="javascript:close();"  class="btnwithico"><img src="../style/icon/close.png" width="16" height="16"><b>关闭&nbsp;</b> </a>



																</div>
															</tr>
														</table>
													</div>

												</td>
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
			<input type="hidden" id="checkOrgIds" name="checkOrgIds" />
			<input type="hidden" id="checkRoleIds" name="checkRoleIds" />
			<input type="hidden" id="checkUserIds" name="checkUserIds" />
			<input type="hidden" id="actions" name="actions" />

			<input type="hidden" id="flowId" name="flowId" value="${param.flowId}" />
			<input type="hidden" id="stepId" name="stepId" value="${param.stepId}" />
		</form>
		<!--[if lt IE 7]>
        <script type="text/javascript" src="js/unitpngfix.js"></script>
<![endif]-->
</html>
<script type="text/javascript">
  
function close()
{
	api.close();
}

function selectReMan()
{
	var ids='';
	
	var temp;
	
	var show = '';

	//org
	var cidCheck = document.getElementsByName('checkedOrgId');
	
	for(var i=0; i<cidCheck.length;i++)
	{
		if(cidCheck[i].checked)
		{
			temp = cidCheck[i].value.split('-');
			ids += temp[0]+',';
			show += temp[1]+', ';
		}
	}
	
	api.get('osmd').document.getElementById('orgIds').value =ids;
	
	//role
	ids='';
	
	cidCheck = document.getElementsByName('checkedRoleId');
	
	for(var i=0; i<cidCheck.length;i++)
	{
		if(cidCheck[i].checked)
		{
			temp = cidCheck[i].value.split('-');
			ids += temp[0]+',';
			show += temp[1]+', ';
		}
	}
	
	api.get('osmd').document.getElementById('roleIds').value =ids;
	
	//user
	ids='';
	
	cidCheck = document.getElementsByName('checkedUserId');
	
	for(var i=0; i<cidCheck.length;i++)
	{
		if(cidCheck[i].checked)
		{
			temp = cidCheck[i].value.split('-');
			ids += temp[0]+',';
			show += temp[1]+', ';
		}
	}
	
	api.get('osmd').document.getElementById('userIds').value =ids;
	
	//显示名称
	api.get('osmd').document.getElementById('showSelectMan').value = show;

	close();
}


</script>

