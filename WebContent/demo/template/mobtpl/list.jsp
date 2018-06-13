<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="/cmsTag" prefix="cms"%>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<cms:Class id="${param.cid}">
	<cms:Class id="${Class.parent}" objName="PClass">
	<title>zswap</title>	
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
	<link rel="stylesheet" href="${ResBase}mobtpl/css/mui.css" type="text/css">
	<link rel="stylesheet" href="${ResBase}mobtpl/css/style.css" type="text/css">
	<script type="text/javascript" src="${ResBase}mobtpl/js/jquery-1.7.min.js"></script>
	<script type="text/javascript" src="${ResBase}mobtpl/js/commonUtil_src.js"></script>
</head>
<body>
<header class="mui-bar mui-text-center">
	<a href="javascript:return_prepage();" class="mui-action-back mui-icon mui-icon-left-nav mui-pull-left text-color-999"></a>
	<h1 class="mui-title"><span class="logo">栏目-${Class.className}</span></h1>
</header>
<div class="mui-content">
	<!-- 路径导航 -->
	<ol class="breadcrumb">
	  	<li><a class="text-color-default" href="${SiteBaseMob}">首页</a></li>
	  	<li class="active"><a href="${Class.mobClassUrl}" >${Class.className}</a></li>
			 
	</ol>
	<!-- end -->
	<div class="zs-list" id="div-${Class.classId}">
				
				<div class="zs-list-box">

					<ul class="mui-table-view" id="tEvent-${Class.classId}">
					
					 
					 <cms:Content page="true" order="default-down" pageSize="3" classId="${Class.classId}">
					 
					 		<li class="mui-table-view-cell mui-media">
								<a href="${Info.mobContentUrl}">
								
									<cms:if test="${Info.classImgFlag == 1}">
									<img class="mui-media-jtop mui-pull-left" src="${Info.classImage}">											
									</cms:if>
									
									<div class="mui-media-body">
										<h4 class="mui-ellipsis text-color-333 line-30">${Info.title}</h4>
										<p class="mui-ellipsis-2">${Info.summary}</p>
									</div>
								</a>
							</li>
							
							 
					 
					 </cms:Content>
					 
						
						 <cms:PageInfo>
            <div class="release-paging">
            	     
                  <span class="f-l" style="padding:0px 8px;"><a href="${Page.headQueryMob}" style="width:80px;border-right:1px solid #cecece;"> << 首页</a></span>
                  <span class="f-l" style="padding:0px 8px;"><a href="${Page.prevQueryMob}" style="width:80px;border-right:1px solid #cecece;"> < 上一页   </a></span>
                  <span class="f-l" style="padding:0px 8px;"><a href="${Page.nextQueryMob}" style="width:80px;border-right:1px solid #cecece;"> 下一页 > </a></span>
                 
                   <span class="f-l" style="padding:0px 8px;"><a href="${Page.endQueryMob}" style="width:80px;border-right:1px solid #cecece;"> 末页 >> </a></span>
                   
                 <span class="f-l" style="padding:0px 10px;">共 ${Page.totalCount} 笔 ，当前第 ${Page.currentPage}页 / ${Page.pageCount} 页，跳转到</span>
                
                <form id="form1" name="form1" action="" style="float:left;">
                <input id="pageJumpPos" name="pageJumpPos" type="text" class="release-paging-k"/>
                <span class="f-l" style="padding:0px 10px;"> 页 </span>
                <input  type="submit" value="确定" class="release-paging-a" onclick="javascript:jump()" />
                </form>
                
                <script>
																										function query(flag)
																										{
																											var cp = 0;
																											
																											if('p' == flag)
																											{
													                                                             cp = parseInt('${Page.currentPage-1}');
																											}
												
																											if('n' == flag)
																											{
													                                                             cp = parseInt('${Page.currentPage+1}');
																											}
												
																											if('h' == flag)
																											{
													                                                             cp = 1;
																											}
												
																											if('l' == flag)
																											{
													                                                             cp = parseInt('${Page.pageCount}');
																											}
												
																											if(cp < 1)
																											{
													                                                           cp=1;
																											}
																										
																											
																											replaceUrlParam(window.location,'tab=2&pn='+cp);		
																										}
																							
																										function jump()
																										{
																											jumpPos(document.getElementById('pageJumpPos').value);
																										}
																										
																										function jumpPos(pos)
																										{
																										
																											var endPos = parseInt('${Page.endPos}');
																											
																											var jumpQuery = '${Page.jumpQuery}';
																											
																											var jumpStatic = '${Page.jumpStatic}';
																											
																											
																											if('' != jumpStatic && pos <=endPos)
																											{
																													if(pos==1)
																													{
																														window.location=jumpStatic.replace('_{pn}','');
																													}
																													else
																													{
																														window.location=jumpStatic.replace('{pn}',pos);
																													}																								
																											}
																											else
																											{
																												window.location="${Page.jumpQuery}&cp="+pos;
																											}
																										
																										
																										}
    	
    		</script>
                </cms:PageInfo>

					</ul>
					
						<div class="comment-more mui-text-center">
							<a href="javascript:getContent('${Class.classId}')">
								<img id="load-${Class.classId}" style="display:none" src="${ResBase}mobtpl/images/loading.gif"height="18" /><span id="over-${Class.classId}">加载更多</span>
							</a>
						</div>

				</div>
		 
		 
		
	</div>

	<!-- footer -->
	<footer class="footer mui-text-center">
	    <ul class="footer-nav">
	    	<li><a href="${SiteBase}">首页</a></li>
	    	<cms:Class idList="child:root" flagMode="true">
				<li><a href="${Class.mobClassUrl}">${Class.className}</a></li>
			</cms:Class>
	    	 
	    </ul>
	    		<ul class="view">
			        <li><a href="${SiteBase}">手机版</a></li>
			        <li><a href="http://demo.jtopcms.com">桌面版</a></li>
			    </ul>
			    <div class="copyright-box">
			       <a target="_blank" href="http://www.miibeian.gov.cn/">皖ICP备13006219号-1</a>
			    </div>
	</footer>
    <!-- footer end -->
</div>
<script type="text/javascript" src="js/mui.min.js"></script>	
</body>
</html>

<script>

 

var ep = 8;

 
var isEnd = false;

function getContent(classId)
{
		 			 
				 	if(isEnd)
					{
					 	$('#over-'+classId).text('已经达到最后');
					}
					else
					{
				
						var nextQueryCd = 'classId='+classId;
						 
 
	 					next(classId);
 					}
 				 
				

}



function next(classId)
{
 
 					$('#load-'+classId).show();
					
					var url = "${SiteBase}content/clientGetContent.do?uid="+Math.random();
 					
 				 	var postData = 'classId='+classId+'&pa=2&ep='+ep+'&nz=3&page=true&order=default-down';
 		
 				 
			 		$.ajax({
			      		type: "POST",
			      		async:true,
			       		url: url,
			       		data:postData,
			   
			       		success: function(msg)
			            {     
			              
			              	var jso = eval('(' + msg + ')');
			              
			                 
			             	$('#load-'+classId).hide();
			                         
			           
			             
				            var cep = jso.endPos;
				            
				            ep = cep;
				              
				              $.each(jso.content,function(name,val) {
								 
								
								genConHtml(classId, val.title, val.classImage, val.mobContentUrl, val.summary, val.pubDate, val.author, val.supportCount);
							});
						 
						 
						  	if(jso.isEnd)
				            {
				             	isEnd = true;
				            	$('#over-'+classId).text('已经达到最后');
				            	 
				            }
			             
						 
			            }
			     	});	

}


function genConHtml(classId, title, img, url, summary, pdt, source, sg)
{
              	 var   c = '';
              	 
				 if(typeof(img) == 'undefined')
				 {
				 		c= '<li class="mui-table-view-cell mui-media">'+
								'<a href="'+url+'">'+
								
								 	'<div class="mui-media-body">'+
										'<h4 class="mui-ellipsis text-color-333 line-30">'+title+'</h4>'+
										'<p class="mui-ellipsis-2">'+summary+'</p>'+
									'</div>'+
								'</a>'+
							'</li>';
				 }
				 else
				 {
				 	
				 	c= '<li class="mui-table-view-cell mui-media">'+
								'<a href="'+url+'">'+
									'<img class="mui-media-jtop mui-pull-left" src="'+img+'">'+											
								 	'<div class="mui-media-body">'+
										'<h4 class="mui-ellipsis text-color-333 line-30">'+title+'</h4>'+
										'<p class="mui-ellipsis-2">'+summary+'</p>'+
									'</div>'+
								'</a>'+
							'</li>';
				 
				 
				 
				 }
	        	
						
			 
	  $("#tEvent-"+classId).append(c);         
	            
	            
	            


}
 
</script>
</cms:Class>
</cms:Class>