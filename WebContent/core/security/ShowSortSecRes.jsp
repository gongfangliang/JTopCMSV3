<%@ page contentType="text/html; charset=utf-8" session="false"%>
<%@ taglib uri="/cmsTag" prefix="cms"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
		<meta http-equiv="content-type" content="text/html; charset=utf-8" />
		<link href="../style/blue/css/main.css" type="text/css" rel="stylesheet" />
		<link href="../style/blue/css/reset-min.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="../common/js/jquery-1.7.gzjs"></script>
		<script type="text/javascript" src="../javascript/commonUtil_src.js"></script>
		<script>
		var size = 0;  
      	var classIdMap = new HashMapJs();
      	
		 var api = frameElement.api, W = api.opener; 
		 
         if("true"==="${param.fromFlow}")
         {     	
            W.$.dialog.tips('排序成功...',1); 
            //api.close(); 
         	//api.reload( api.get('cwa') );        
       		//W.window.location.reload();
         }
         
      	</script>

		</script>
	</head>
	<body>
		<table width="100%" border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td align="left" valign="top">
					<!--main start-->
					

					<div style="height:4px"></div>
					<table class="listdate" width="100%" cellpadding="0" cellspacing="0">

						<tr class="datahead">
							<td width="18%">
								<strong>&nbsp;&nbsp;资源名称</strong>
							</td>

							<td width="10%">
								<center><strong>操作</strong></center>
							</td>
						</tr>
					
							<cms:ResourceList protectType="100" roleId="all" linear="${param.parentResLinear}" parentLinearMode="true">

								<cms:Resource >
									<tr>
										<td>
											&nbsp;&nbsp;${Res.resourceName}
										</td>

										<td>
											<div align="center">
												<span class="STYLE4"><a href="javascript:upAction(${status.index+1},${Res.secResId});"><img src="../../core/style/default/images/up.png" width="16" height="16" />上移</a>&nbsp; &nbsp; <a href="javascript:downAction(${status.index+1},${Res.secResId});"><img src="../../core/style/default/images/down.png" width="16" height="16" />下移</a> </span>
											</div>
										</td>
									</tr>
									<script type="text/javascript">	
								size++;
                        		classIdMap.put(${status.index+1},"${Res.secResId}");
                   				</script>
								</cms:Resource>
								<cms:Empty flag="Res">
											<tr >
												<td class="tdbgyew" colspan="2">
													<center>
														当前没有数据!
													</center>
												</td>
											</tr>
								</cms:Empty>
							</cms:ResourceList>
					

					</table>

					<form method="post" id="sortForm" name="sortForm">
						<input type="hidden" id="currentResId" name="currentResId" />
						<input type="hidden" id="targetResId" name="targetResId" />
						<input type="hidden" id="parent" name="parent" value='${param.parentResLinear}' />
						<cms:Token mode="html"/>
					</form>

					<div style="height:40px"></div>
					<div class="breadnavTab"  >
						<table width="100%" border="0" cellspacing="0" cellpadding="0" >
							<tr class="btnbg100">
								<div style="float:right">
									<a href="javascript:close();"  class="btnwithico" onclick=""><img src="../style/icon/close.png" width="16" height="16"/><b>取消&nbsp;</b> </a>
								</div>
							</tr>
						</table>
					</div>

				</td>
			</tr>


		</table>
		<!--[if lt IE 7]>
        <script type="text/javascript" src="js/unitpngfix.js"></script>
<![endif]-->
	</body>
</html>
<script>  
 

  
function close()
{
	api.close();
}

function upAction(index,currentId)
{
	if(index == 1)//第一位的node
	{
		W.$.dialog({ 
		   		title :'提示',
		    	width: '165px', 
		    	height: '60px', 
		    	parent:api,
		        lock: true, 
		    	icon: '32X32/i.png', 
		    				
		        content: '当前资源已经是第一位！', 
		       	cancel: true 
		});
		return;
	}
	
	var targetClassId = classIdMap.get(index-1);
	
	document.sortForm.action="<cms:BasePath/>security/sortSecRes.do";
	document.getElementById("currentResId").value=currentId;
    document.getElementById("targetResId").value=targetClassId;
  
    document.sortForm.submit();
}

function downAction(index,currentId)
{
	if(index == size)//最后一位的node
	{
		W.$.dialog({ 
		   		title :'提示',
		    	width: '165px', 
		    	height: '60px', 
		    	parent:api,
		        lock: true, 
		    	icon: '32X32/i.png', 
		    				
		        content: '当前资源已经是最后一位！', 
		       	cancel: true 
		});
		return;
	}
   
    var targetClassId = classIdMap.get(index+1);

	document.sortForm.action="<cms:BasePath/>security/sortSecRes.do";
	document.getElementById("currentResId").value=currentId;
    document.getElementById("targetResId").value=targetClassId;
  
    document.sortForm.submit();
}

</script>
