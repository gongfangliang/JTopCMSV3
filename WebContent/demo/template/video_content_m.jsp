<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib uri="/cmsTag" prefix="cms"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<html>
<head>
<!--[if IE 7]>

<![endif]-->
<base href="${ResBase}"/>
<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<cms:Content id="${param.id}">
<cms:ContentStatus contentId="${param.id}">
<title>${Info.title}</title>
<!--[if IE 7]>
<link rel="stylesheet" href="css/font-awesome-ie7.css">
<![endif]-->
<link href="css/font-awesome.min.css" rel="stylesheet" type="text/css"></link>
<link href="css/base.css" rel="stylesheet" type="text/css"></link>
<link href="css/content.css" rel="stylesheet" type="text/css"></link>
<link href="css/video_content.css" rel="stylesheet" type="text/css"></link>
 

<script type="text/javascript" src="${ResBase}js/jquery-1.9.1.min.js"></script>
<script type="text/javascript" src="${ResBase}tool/player/images/swfobject.js"></script>
<script type="text/javascript" src="${ResBase}js/commonUtil_src.js"></script>
<script type="text/javascript" src="${ResBase}js/common.js"></script>
 
<script type="text/javascript" src="${ResBase}tool/player/cpMobile/js/action.js"></script>
 

<script>

var basePath = '${SiteBase}';


</script>


</head>


<body>
<!--头部开始-->
<div class="header">
  <div class="module-play">
  <div class="top_nav">
    <div class="top_nav_inner">
      <div class="fl nav-collect"> <a href="${SiteBase}"> 网站首页</a></div>
      <div class="siderNav fr">
        <ul class="topmenu" id="jq_topmenu">
          <!--nav-map-->
          <li class="webnav down_con"> <cms:Class id="${Info.classId}"><a href="${Class.classUrl}"></cms:Class> 返回视频栏目</b></a>
            
          </li>
          <!--help-->
          <li><a href="${SiteBase}"><b>${Info.title}</b></a></li>
        </ul>
      </div>
    </div>
  </div>
<!--top-nav-结束-->
<div class="main_box">
	 
 
	 
<SCRIPT LANGUAGE=JavaScript>
<!--
var vID        = ""; 
var vWidth     = "100%";
var vHeight    = 549;
var vFile      = "${ResBase}tool/player/cpMobile/CuSunV2set.xml";
var vPlayer    = "${ResBase}tool/player/cpMobile/player.swf?v=2.5";

<cms:if test="${empty Info.mh_sp_fileMediaC}">
		
		var vPic       = "${ResBase}tool/player/cpMobile/images/start.jpg";
	
	</cms:if>
	<cms:else>
	
		var vPic       = "${Info.mh_sp_fileMediaC}"; 
	
</cms:else>
	
	

var vCssurl    = "${ResBase}tool/player/cpMobile/images/mini.css";

//PC,安卓,iOS
var vMp4url    = "${Info.mh_sp_file}";

//-->
</SCRIPT>
<script class="CuPlayerVideo" data-mce-role="CuPlayerVideo" type="text/javascript" src="${ResBase}tool/player/cpMobile/js/CuSunX1.min.js"></script>	 
	 
</div>
</div>

 </div>
<!--头部结束-->
<div class="main_br"></div>

<!--主体开始--><!--主体结束-->
<div class="main_box"><!--左侧-->
   	<div class="layoutcon news-br mt15">
    	 
        <div class="layoutLeft pr15">
        	<!--新闻详情页开始-->
          	<div class="mainBody">
           	  <div class="leftShare">
              		<span><a href="javascript:digg('${Info.contentId}', 's');"><i class="fa fa-thumbs-up"></i>顶<b id="contentSuCount">0</b></a></span>
                 	<span><a href="javascript:digg('${Info.contentId}', 'a');"><i class="fa fa-thumbs-down"></i>踩<b id="contentAgCount">0</b></a></span>
                 	
                 	
                 	
                 	<ins class="fr"><a href="#"><i class="fa fa-share-square"></i>分享</a></ins>
              	</div>
       		  <div class="play-tit-long">
              		<div class="play-tit-l"><h1>${Info.title}</h1><p><cms:ChannelPath classId="${Info.classId}">${PathClass.className} > </cms:ChannelPath>内容</p></div>

                  <div class="play-tit-r fr">播放：<span id="contentClickCount">0</span></div>
                                		
                  <script>
                  
                   
			        var url = '${SiteBase}content/status.do?id=${Info.contentId}';
 		
			 		$.ajax({
			      		type: "POST",
			       		url: url,
			       		data:'',
			   
			       		success: function(msg)
			            {     
			               var status = eval("("+msg+")");
			               
			               $('#contentClickCount').text(status.clickCount+'');
			               $('#contentSuCount').text(status.supportCount);
			               $('#contentAgCount').text(status.againstCount);
			            }
			     	});	
                  
                  
                  </script>
              	</div>
              	
          		<!--评论-->
                 <cms:Include page="include/comment.jsp?targetId=${Info.contentId}&classId=${Info.classId}"/>
                  
                  
                  
           	  	<div class="bs-Comments">
                      <ul>
                          <cms:Comment size="7"  contentId="${Info.contentId}">

									<li class="media">
										<a class="pull-left" href="javascript:"> <img src="${ResBath}images/menber_def.png" width="96" height="96" /> </a>
										<div class="media-body">
											<h4 class="media-heading">
												<a href="javascript:">${Comment.userName}</a><span>${Comment.commDT}</span>
											</h4>
											<span class="txt"><pre>${Comment.commentText}</pre></span>
											<ins class="np-post">
												<span><a href="javascript:diggComment('${Comment.commentId}','s');"><i class="fa fa-thumbs-up">顶</i>[ ${Comment.supportCount} ]</a>&nbsp;<a href="javascript:diggComment('${Comment.commentId}','a');"><i class="fa fa-thumbs-o-down"></i>踩[ ${Comment.againstCount} ]</a> </span>
											</ins>
										</div>
									</li>

								</cms:Comment>
                          
                      </ul>
   			  </div>
                 
                  
       	  </div>
          	<!--新闻详情页结束-->
          
        </div>
        
    
   	</div>
    
   	<!--左侧结束-->
  <div class="area-sub fr mt15">
    <cms:Include page="block/content_click_order.jsp"/>
       <%--<cms:Block flag="mh_sy_hd"></cms:Block>--%>
       
      <!--排行结束-->
      <div class="main_br"></div>
      <!--专题-->
      <cms:Include page="block/commend_spec.jsp"/>
        <!--时政聚焦结束-->
        <div class="main_br"></div>
        <!--月点击-->
       <cms:Include page="block/content_click_month_order.jsp"/>
        <div class="main_br"></div>
       <!--周点击-->
       <cms:Include page="block/content_click_week_order.jsp"/>
  </div>
    

</div>
<div class="main_br"></div>
<cms:Include page="include/foot.jsp"/>


</body>

</html>

<cms:VisStat contentId="${Info.contentId}" classId="${Info.classId}"/>

</cms:ContentStatus>



</cms:Content>
