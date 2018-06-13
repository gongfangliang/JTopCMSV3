<%@ page contentType="text/html; charset=utf-8" session="false"%>
<%@ taglib uri="/cmsTag" prefix="cms"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
		<meta http-equiv="content-type" content="text/html; charset=utf-8" />

		<title></title>
		<link href="../../style/blue/css/main.css" type="text/css" rel="stylesheet" />
		<link href="../../style/blue/css/reset-min.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="../../common/js/jquery-1.7.gzjs"></script>
		<script language="javascript" type="text/javascript" src="../../javascript/commonUtil_src.js"></script>
		<script>
			
			 
			
			
		</script>
	</head>
	<body>
	
	<form id="renameForm" name="renameForm" method="post">
		<table width="100%" border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td align="left" valign="top">
					<!--main start-->
				

					
						<table width="100%" border="0" cellpadding="0" cellspacing="0" class="form-table">
							
							<tr>
								<td width="15%" class="input-title">
									<strong>建议</strong>
								</td>
								<td class="td-input">
									<textarea id="flowSug" style="width:397px; height:165px;" class="form-textarea"></textarea>
									<br/><span class="ps">流程建议将附送给下一步骤审核者,一般为当前执行者的本次执行理由和建议</span>
								</td>
							</tr>
						

						</table>
						<!-- hidden -->
						<input type="hidden" id="entry" name="entry" value="${param.entry}" />
							
							 
					</form>
					<div style="height:15px"></div>
					<div class="breadnavTab"  >
						<table width="100%" border="0" cellspacing="0" cellpadding="0" >
							<tr class="btnbg100">
								<div style="float:right">
									<a  href="javascript:addSuggest();"  class="btnwithico"><img src="../../style/icons/tick.png" width="16" height="16" id="submitTagClassFormImg"/><b>确定&nbsp;</b> </a>
									<a href="javascript:close();"  class="btnwithico" onclick=""><img src="../../style/icon/close.png" width="16" height="16"/><b>取消&nbsp;</b> </a>
								</div>
								 
							</tr>
						</table>
					</div>
				</td>
			</tr>
	
	
	
	</body>
</html>

<script type="text/javascript">

var api = frameElement.api, W = api.opener; 


$('#flowSug').val(W.$('#jtopcms_sys_flow_suguest').val());


function addSuggest()
{
	var flowSug = $('#flowSug').val();
	
	 
	api.get('main_content').$('#jtopcms_sys_flow_suguest').val(flowSug);
	 	
	 api.close();      
}


function close()
{
	     
    api.close();      
}
				
									
</script>

