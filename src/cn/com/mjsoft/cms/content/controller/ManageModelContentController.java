package cn.com.mjsoft.cms.content.controller;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;

import cn.com.mjsoft.cms.behavior.InitSiteGroupInfoBehavior;
import cn.com.mjsoft.cms.behavior.JtRuntime;
import cn.com.mjsoft.cms.channel.bean.ContentClassBean;
import cn.com.mjsoft.cms.channel.controller.ListCommendTypeInfoTreeController;
import cn.com.mjsoft.cms.channel.controller.ListContentClassInfoTreeController;
import cn.com.mjsoft.cms.channel.dao.ChannelDao;

import cn.com.mjsoft.cms.channel.service.ChannelService;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.ServiceUtil;
import cn.com.mjsoft.cms.common.spring.annotation.ActionInfo;
import cn.com.mjsoft.cms.content.bean.ContentMainInfoBean;
import cn.com.mjsoft.cms.content.bean.UploadImageJsonBean;
import cn.com.mjsoft.cms.content.dao.vo.ContentMainInfo;
import cn.com.mjsoft.cms.content.service.ContentService;
import cn.com.mjsoft.cms.member.bean.MemberAccRuleBean;
import cn.com.mjsoft.cms.member.bean.MemberBean;
import cn.com.mjsoft.cms.message.service.MessageService;
import cn.com.mjsoft.cms.metadata.bean.DataModelBean;
import cn.com.mjsoft.cms.metadata.bean.ModelPersistenceMySqlCodeBean;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.cms.resources.bean.SiteResourceBean;
import cn.com.mjsoft.cms.resources.dao.vo.SiteResource;
import cn.com.mjsoft.cms.resources.service.ResourcesService;
import cn.com.mjsoft.cms.security.service.SecurityService;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.cms.site.service.SiteGroupService;
import cn.com.mjsoft.cms.stat.bean.ContentTempTraceBean;
import cn.com.mjsoft.cms.stat.service.StatService;

import cn.com.mjsoft.framework.behavior.Behavior;
import cn.com.mjsoft.framework.config.impl.SystemConfiguration;
import cn.com.mjsoft.framework.exception.FrameworkException;
import cn.com.mjsoft.framework.persistence.core.support.UpdateState;
import cn.com.mjsoft.framework.security.Role;
import cn.com.mjsoft.framework.security.session.SecuritySession;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.DateAndTimeUtil;
import cn.com.mjsoft.framework.util.ImageUtil;
import cn.com.mjsoft.framework.util.MathUtil;
import cn.com.mjsoft.framework.util.MediaUtil;
import cn.com.mjsoft.framework.util.ObjectUtility;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.util.SystemSafeCharUtil;
import cn.com.mjsoft.framework.web.wing.ServletUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
@Controller
@RequestMapping( "/content" )
public class ManageModelContentController
{
    private static Logger log = Logger.getLogger( ManageModelContentController.class );

   
    public static final Map ivTemp = new ConcurrentHashMap();

    private static MetaDataService metaDataService = MetaDataService.getInstance();

    private static ContentService contentService = ContentService.getInstance();

    private static ChannelService channelService = ChannelService.getInstance();

    private static SecurityService securityService = SecurityService.getInstance();

    private static MessageService messageService = MessageService.getInstance();

    private static StatService statService = StatService.getInstance();

    private static ResourcesService resService = ResourcesService.getInstance();

    private static SiteGroupService siteService = SiteGroupService.getInstance();

    @ResponseBody
    @RequestMapping( value = "/addContent.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "添加内容", token = true )
    public Object addContent( HttpServletRequest request, HttpServletResponse response )
        throws InstantiationException, IllegalAccessException
    {
        /**
         * 获取栏目信息
         */
        // 由于存在一些字段允许html,所以不能在这里先使用getHttpRequestSnapshot(),故使用普通方式获取classId
        Long classId = StringUtil.getLongValue( request.getParameter( "classId" ), -1 );
 

        ContentClassBean classBean = channelService.retrieveSingleClassBeanInfoByClassId( classId );

        if( classBean != null && classBean.getClassId().longValue() < 0 )
        {
            throw new FrameworkException( "栏目信息丢失,classId:" + classId );
        }

       

        SiteGroupBean site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
            .getEntry( classBean.getSiteFlag() );

        /**
         * 获取对应数据模型元数据
         */
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( classBean
            .getContentType() );
        List filedBeanList = metaDataService.retrieveModelFiledInfoBeanList( classBean
            .getContentType() );

        // 处理复制内容,检查复制栏目是否模型一致

        // 处理站点共享内容

        // 处理相关内容

        // 处理推荐内容 ,推荐内容ID不在main-info里

        /**
         * 根据不同模型进入处理,现系统模型和自定义模型分开
         */

        Map returnParams = new HashMap();

        returnParams.put( "modelId", classBean.getContentType() );
        returnParams.put( "classId", classId );

        ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( classBean.getContentType() );

        if( modelBean == null || filedBeanList.isEmpty() || sqlCodeBean == null )
        {
            log.error( "模型元数据丢失，模型ID:" + classBean.getContentType() );
            throw new FrameworkException( "模型元数据信息不全！" );
        }

        // 编辑器类型字段
        Set editorFieldSet = ServiceUtil.checkEditorField( filedBeanList );

        Map params = ServletUtil.getRequestInfo( request, editorFieldSet );

        /**
         * 处理数据模式标题
         */
        disposeTitleMode( false, modelBean, params );

        /**
         * 文本特殊处理
         */
        Iterator editorFieldIter = editorFieldSet.iterator();

        String fieldName = null;

        String text = null;

        List dlImgList = new ArrayList();

        while ( editorFieldIter.hasNext() )
        {
            fieldName = ( String ) editorFieldIter.next();

            text = ( String ) params.get( fieldName );

            if( Constant.COMMON.OFF.equals( site.getSiteCollType() ) )
            {
              
                text = ServiceUtil.cleanEditorHtmlByWhiteRule( text );
            }

             
            if( Constant.COMMON.ON.equals( site.getDeleteOutLink() ) )
            {
                text = contentService.deleteContentTextOutHref( text, site );
            }

            

            if( Constant.COMMON.ON.equals( site.getDownOutImage() ) )
            {
                text = contentService.downloadImageFormWeb( null, text, site, classBean
                    .getClassId(), dlImgList );
            }

            params.put( fieldName, text );
        }

     

        String vbClassName = modelBean.getBeforeBehavior();

        
        if( StringUtil.isStringNotNull( vbClassName ) )
        {
            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    ( ( Behavior ) valiedateBehavior ).operation( params, new Object[] { modelBean,
                        filedBeanList, sqlCodeBean } );
                }
            }
        }

        List execWorkflowActionList = new ArrayList( 5 );

        
        params.put( "_sys_content_image_res_count_", Integer.valueOf( 0 ) );

        params.put( "_sys_content_video_res_count_", Integer.valueOf( 0 ) );

        params.put( "_sys_content_image_res_del_count_", Integer.valueOf( 0 ) );

        params.put( "_sys_content_video_res_del_count_", Integer.valueOf( 0 ) );

        ContentMainInfo mainInfo = contentService.addOrEditUserDefineContent( classBean, params,
            execWorkflowActionList, filedBeanList, sqlCodeBean, false );

        int imageCount = ( ( Integer ) params.get( "_sys_content_image_res_count_" ) ).intValue();

        int videoCount = ( ( Integer ) params.get( "_sys_content_video_res_count_" ) ).intValue();

        int imageDelCount = ( ( Integer ) params.get( "_sys_content_image_res_del_count_" ) )
            .intValue();

        int videoDelCount = ( ( Integer ) params.get( "_sys_content_video_res_del_count_" ) )
            .intValue();

        int addCount = 0;

        int pubCount = 0;

        if( Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( mainInfo.getCensorState() )
            || Constant.WORKFLOW.CENSOR_STATUS_WAIT_PUBLISH.equals( mainInfo.getCensorState() ) )
        {
            pubCount = 1;

            addCount = 0;

            ContentTempTraceBean ctt = ( ContentTempTraceBean ) ivTemp
                .get( mainInfo.getContentId() );

            Integer ti = 0;

            Integer tv = 0;

            if( ctt != null )
            {

                ti = ctt.getImgCount();

                tv = ctt.getVideoCount();

                ivTemp.remove( mainInfo.getContentId() );
            }

            statService.collAndAnalysisContentStat( false, null, mainInfo.getContentId(), null,
                site.getSiteId(), mainInfo.getClassId(), addCount, pubCount, ti + imageCount
                    - imageDelCount, tv + videoCount - videoDelCount );

        }
        else
        {
            ContentTempTraceBean ctt = ( ContentTempTraceBean ) ivTemp
                .get( mainInfo.getContentId() );

            if( ctt == null )
            {
                ctt = new ContentTempTraceBean();

                ctt.setContentId( mainInfo.getContentId() );
                ctt.setImgCount( imageCount - imageDelCount );
                ctt.setVideoCount( videoCount - videoDelCount );

                ivTemp.put( mainInfo.getContentId(), ctt );
            }
            else
            {
                ctt.setImgCount( ctt.getImgCount() + imageCount - imageDelCount );
                ctt.setVideoCount( ctt.getVideoCount() + videoCount - videoDelCount );
            }

            pubCount = 0;

            addCount = 1;

            statService.collAndAnalysisContentStat( false, null, mainInfo.getContentId(), null,
                site.getSiteId(), mainInfo.getClassId(), addCount, pubCount, 0, 0 );
        }

       
        Long resId = null;
        for ( int i = 0; i < dlImgList.size(); i++ )
        {
            resId = ( Long ) dlImgList.get( i );

            contentService.addDownloadImgRes( mainInfo.getContentId(), resId );
        }

      
        params.put( Constant.CONTENT.ID_ORDER_VAR, mainInfo.getContentId() );

       
        vbClassName = modelBean.getAfterBehavior();

        if( StringUtil.isStringNotNull( vbClassName ) )
        {

            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    ( ( Behavior ) valiedateBehavior ).operation( params, new Object[] { modelBean,
                        filedBeanList, sqlCodeBean } );
                }
            }
        }
 

        if( Constant.SITE_CHANNEL.CLASS_TYPE_SINGLE_PAGE.equals( classBean.getClassType() ) )
        {
            channelService.setChannelClassSinglePageModeContentId( classBean.getClassId(), mainInfo
                .getContentId() );
 

            ListContentClassInfoTreeController.resizeSiteContentClassCache();
            ListCommendTypeInfoTreeController.resizeSiteCommendTypeCache();
            ChannelDao.clearAllCache();
            ChannelService.clearContentClassCache();

            
            returnParams.put( "contentId", mainInfo.getContentId() );

        }

       
       
        List contentIdList = new ArrayList( 1 );
        contentIdList.add( mainInfo.getContentId() );

        String pubSuccessIds = "";

       
        List idList = new ArrayList();

        idList.add( mainInfo.getContentId() );

        
  
        boolean isTrace = false;
        if( "true".equals( ( String ) params.get( "fromCensor" ) ) )
        {
            Map param = new HashMap();

            param.put( "modelId", classBean.getContentType() );
           
            param.put( "classId", classBean.getClassId() );
        

            param.put( "contentId", mainInfo.getContentId() );

            param.put( "innerWidth", "1200" );

            if( execWorkflowActionList.size() == 1 )
            {
                
                isTrace = true;

                param.put( "actId", ( ( String[] ) execWorkflowActionList.get( 0 ) )[0] );

                param.put( "actSuccess", ( ( String[] ) execWorkflowActionList.get( 0 ) )[1] );
            }

            return ServletUtil.redirect( JtRuntime.cmsServer.getDomainFullPath()
                + "core/content/CheckContentAndCensor.jsp", param );

        }

        // 由发布逻辑进行发布活动,包含主内容和要复制的内容
        if( StringUtil.isStringNotNull( pubSuccessIds ) )
        {
            if( Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE
                .equals( classBean.getContentProduceType() )
                && Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( mainInfo.getCensorState() ) )
            {
                pubSuccessIds = pubSuccessIds + "," + mainInfo.getContentId();
            }

            request.setAttribute( "someContentId", pubSuccessIds );
            request.setAttribute( "modelId", classBean.getContentType() );
            request.setAttribute( "contentId", mainInfo.getContentId() );
            request.setAttribute( "classId", classBean.getClassId() );
            request.setAttribute( "staticType", Integer.valueOf( 2 ) );
            request.setAttribute( "addOrEditFlag", "true" );

            request.setAttribute( "editMode", Boolean.valueOf( false ) );

            request.setAttribute( "fromFlow", Boolean.TRUE );

            request.setAttribute( "fromAction", Boolean.TRUE );

            request.setAttribute( "manageParam", SystemSafeCharUtil.resumeHTML( ( String ) params
                .get( "_jtop_sys_manage_param" ) ) );

            return ServletUtil.forward( "/publish/generateContent.do" );

        }
        else
        {
         
            if( Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE
                .equals( classBean.getContentProduceType() )
                && Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( mainInfo.getCensorState() ) )
            {

                request.setAttribute( "singleContentPublish", Boolean.TRUE );
                request.setAttribute( "modelId", classBean.getContentType() );
                request.setAttribute( "contentId", mainInfo.getContentId() );
                request.setAttribute( "classId", classBean.getClassId() );

                request.setAttribute( "editMode", Boolean.valueOf( false ) );

                request.setAttribute( "fromFlow", Boolean.TRUE );

                request.setAttribute( "fromAction", Boolean.TRUE );

                request.setAttribute( "manageParam", SystemSafeCharUtil
                    .resumeHTML( ( String ) params.get( "_jtop_sys_manage_param" ) ) );

                if( !isTrace )
                {
                    String actionName = "发布内容";

                    if( Constant.WORKFLOW.CENSOR_STATUS_DRAFT.equals( mainInfo.getCensorState() ) )
                    {
                        actionName = "存稿件";
                    }

                    contentService.addContentOperInfo( mainInfo.getContentId(),
                        ( String ) SecuritySessionKeeper.getSecuritySession().getAuth()
                            .getApellation(), actionName, ( String ) params
                            .get( "jtopcms_sys_flow_suguest" ) );
                }

                return ServletUtil.forward( "/publish/generateContent.do" );
            }
        }

        {
            if( Constant.SITE_CHANNEL.CLASS_TYPE_SINGLE_PAGE.equals( classBean.getClassType() ) )
            {

                return ServletUtil.redirect( JtRuntime.cmsServer.getDomainFullPath()
                    + "core/content/EditUserDefineModelContent.jsp", returnParams );

            }

            Map param = new HashMap();

            param.put( "modelId", classBean.getContentType() );
            param.put( "censorCId", mainInfo.getContentId() );

            param.put( "contentId", mainInfo.getContentId() );

            param.put( "classId", classBean.getClassId() );

            param.put( "fromFlow", Boolean.TRUE );

            param.put( "fromDraft", params.get( "contentAddStatus" ) );

            param.put( "fromAddFlow", Boolean.TRUE );

            param.put( "manageParam", "?classId=" + classId + "&modelId="
                + classBean.getContentType() );

            // 记录操作

            if( !isTrace )
            {
                String actionName = "添加内容";

                if( Constant.WORKFLOW.CENSOR_STATUS_DRAFT.equals( mainInfo.getCensorState() ) )
                {
                    actionName = "存稿件";
                }

                contentService
                    .addContentOperInfo( mainInfo.getContentId(), ( String ) SecuritySessionKeeper
                        .getSecuritySession().getAuth().getApellation(), actionName,
                        ( String ) params.get( "jtopcms_sys_flow_suguest" ) );
            }

            return ServletUtil.redirect( JtRuntime.cmsServer.getDomainFullPath()
                + "core/content/EditUserDefineModelContent.jsp", param );

        }

    }

    @ResponseBody
    @RequestMapping( value = "/editContent.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "编辑内容", token = true )
    public Object editContent( HttpServletRequest request, HttpServletResponse response )
        throws InstantiationException, IllegalAccessException
    {
 
        Long classId = StringUtil.getLongValue( request.getParameter( "classId" ), -1 );

      
        ContentClassBean classBean = channelService.retrieveSingleClassBeanInfoByClassId( classId );

        if( classBean != null && classBean.getClassId().longValue() < 0 )
        {
            throw new FrameworkException( "栏目信息丢失,classId:" + classId );
        }

        

        SiteGroupBean site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
            .getEntry( classBean.getSiteFlag() );

        /**
         * 获取对应数据模型元数据
         */
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( classBean
            .getContentType() );
        List filedBeanList = metaDataService.retrieveModelFiledInfoBeanList( classBean
            .getContentType() );

        // 处理复制内容,检查复制栏目是否模型一致

        // 处理站点共享内容

        // 处理相关内容

        // 处理推荐内容 ,推荐内容ID不在main-info里

        /**
         * 根据不同模型进入处理,现系统模型和自定义模型分开
         */

        Map returnParams = new HashMap();

        returnParams.put( "modelId", classBean.getContentType() );
        returnParams.put( "classId", classId );

        ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( classBean.getContentType() );

        if( modelBean == null || filedBeanList.isEmpty() || sqlCodeBean == null )
        {
            log.error( "模型元数据丢失，模型ID:" + classBean.getContentType() );
            throw new FrameworkException( "模型元数据信息不全！" );
        }

        // 编辑器类型字段
        Set editorFieldSet = ServiceUtil.checkEditorField( filedBeanList );

        Map params = ServletUtil.getRequestInfo( request, editorFieldSet );

       
        disposeTitleMode( true, modelBean, params );
 
        Iterator editorFieldIter = editorFieldSet.iterator();

        String fieldName = null;

        String text = null;

        List dlImgList = new ArrayList();

        while ( editorFieldIter.hasNext() )
        {
            fieldName = ( String ) editorFieldIter.next();

            text = ( String ) params.get( fieldName );

            if( Constant.COMMON.OFF.equals( site.getSiteCollType() ) )
            {
               
                text = ServiceUtil.cleanEditorHtmlByWhiteRule( text );
            }

           
            if( Constant.COMMON.ON.equals( site.getDeleteOutLink() ) )
            {
                text = contentService.deleteContentTextOutHref( text, site );
            }

           
            if( Constant.COMMON.ON.equals( site.getDownOutImage() ) )
            {
                text = contentService.downloadImageFormWeb( null, text, site, classBean
                    .getClassId(), dlImgList );
            }

            params.put( fieldName, text );
        }
 
        Long fromStepId = StringUtil.getLongValue( ( String ) params.get( "fromStepId" ), -1 );

        Long toStepId = StringUtil.getLongValue( ( String ) params.get( "toStepId" ), -1 );

        boolean reEditMode = false;

        if( Constant.WORKFLOW.RE_EDIT_ACTION_ID_VALUE.equals( fromStepId )
            && Constant.WORKFLOW.START_ACTION_ID_VALUE.equals( toStepId ) )
        {
            reEditMode = true;
        }

        String vbClassName = modelBean.getBeforeBehavior();

        // 进行前置参数处理行为

        if( StringUtil.isStringNotNull( vbClassName ) )
        {
            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    ( ( Behavior ) valiedateBehavior ).operation( params, new Object[] { modelBean,
                        filedBeanList, sqlCodeBean } );
                }
            }
        }

        List execWorkflowActionList = new ArrayList( 5 );

        
        params.put( "_sys_content_image_res_count_", Integer.valueOf( 0 ) );

        params.put( "_sys_content_video_res_count_", Integer.valueOf( 0 ) );

        params.put( "_sys_content_image_res_del_count_", Integer.valueOf( 0 ) );

        params.put( "_sys_content_video_res_del_count_", Integer.valueOf( 0 ) );

        ContentMainInfo mainInfo = contentService.addOrEditUserDefineContent( classBean, params,
            execWorkflowActionList, filedBeanList, sqlCodeBean, true );

        int imageCount = ( ( Integer ) params.get( "_sys_content_image_res_count_" ) ).intValue();

        int videoCount = ( ( Integer ) params.get( "_sys_content_video_res_count_" ) ).intValue();

        int imageDelCount = ( ( Integer ) params.get( "_sys_content_image_res_del_count_" ) )
            .intValue();

        int videoDelCount = ( ( Integer ) params.get( "_sys_content_video_res_del_count_" ) )
            .intValue();

        int addCount = 0;

        int pubCount = 0;

        if( Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( mainInfo.getCensorState() )
            || Constant.WORKFLOW.CENSOR_STATUS_WAIT_PUBLISH.equals( mainInfo.getCensorState() ) )
        {
            pubCount = 1;

            addCount = 0;

            ContentTempTraceBean ctt = ( ContentTempTraceBean ) ivTemp
                .get( mainInfo.getContentId() );

            Integer ti = 0;

            Integer tv = 0;

            if( ctt != null )
            {

                ti = ctt.getImgCount();

                tv = ctt.getVideoCount();

                ivTemp.remove( mainInfo.getContentId() );
            }

            statService.collAndAnalysisContentStat( false, null, mainInfo.getContentId(), null,
                site.getSiteId(), mainInfo.getClassId(), addCount, pubCount, ti + imageCount
                    - imageDelCount, tv + videoCount - videoDelCount );

        }
        else
        {
            ContentTempTraceBean ctt = ( ContentTempTraceBean ) ivTemp
                .get( mainInfo.getContentId() );

            if( ctt == null )
            {
                ctt = new ContentTempTraceBean();

                ctt.setContentId( mainInfo.getContentId() );
                ctt.setImgCount( imageCount - imageDelCount );
                ctt.setVideoCount( videoCount - videoDelCount );

                ivTemp.put( mainInfo.getContentId(), ctt );
            }
            else
            {
                ctt.setImgCount( ctt.getImgCount() + imageCount - imageDelCount );
                ctt.setVideoCount( ctt.getVideoCount() + videoCount - videoDelCount );
            }

            pubCount = 0;

            addCount = 1;

            statService.collAndAnalysisContentStat( false, null, mainInfo.getContentId(), null,
                site.getSiteId(), mainInfo.getClassId(), addCount, pubCount, 0, 0 );
        }

        
        Long resId = null;
        for ( int i = 0; i < dlImgList.size(); i++ )
        {
            resId = ( Long ) dlImgList.get( i );

            contentService.addDownloadImgRes( mainInfo.getContentId(), resId );
        }

       
        params.put( Constant.CONTENT.ID_ORDER_VAR, mainInfo.getContentId() );

      
        vbClassName = modelBean.getAfterBehavior();

        if( StringUtil.isStringNotNull( vbClassName ) )
        {

            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    ( ( Behavior ) valiedateBehavior ).operation( params, new Object[] { modelBean,
                        filedBeanList, sqlCodeBean } );
                }
            }
        }

        

        if( Constant.SITE_CHANNEL.CLASS_TYPE_SINGLE_PAGE.equals( classBean.getClassType() ) )
        {
            channelService.setChannelClassSinglePageModeContentId( classBean.getClassId(), mainInfo
                .getContentId() );

           
            ListContentClassInfoTreeController.resizeSiteContentClassCache();
            ListCommendTypeInfoTreeController.resizeSiteCommendTypeCache();
            ChannelDao.clearAllCache();
            ChannelService.clearContentClassCache();

            // 注册返回ID
            returnParams.put( "contentId", mainInfo.getContentId() );

        }

        /**
         * 以下为当前内容辅助信息操作,需要注意的是,确定主数据更新正确,以及是发布通过状态后才可进行以下操作
         */
        // 将改动为只有通过发布的内容才可复制和共享
        // 处理复制栏目和共享
        List contentIdList = new ArrayList( 1 );
        contentIdList.add( mainInfo.getContentId() );

        String pubSuccessIds = "";

       
        List idList = new ArrayList();

        idList.add( mainInfo.getContentId() );
 
       
        
        boolean isTrace = false;
        if( "true".equals( ( String ) params.get( "fromCensor" ) ) )
        {
            Map param = new HashMap();

            param.put( "modelId", classBean.getContentType() );
            
            param.put( "classId", classBean.getClassId() );
           

            param.put( "contentId", mainInfo.getContentId() );

            param.put( "innerWidth", "1200" );

            if( execWorkflowActionList.size() == 1 )
            {
               
             
                isTrace = true;

                param.put( "actId", ( ( String[] ) execWorkflowActionList.get( 0 ) )[0] );

                param.put( "actSuccess", ( ( String[] ) execWorkflowActionList.get( 0 ) )[1] );
            }

            return ServletUtil.redirect( JtRuntime.cmsServer.getDomainFullPath()
                + "core/content/CheckContentAndCensor.jsp", param );
        }

        // 由发布逻辑进行发布活动,包含主内容和要复制的内容
        if( StringUtil.isStringNotNull( pubSuccessIds ) )
        {
            if( Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE
                .equals( classBean.getContentProduceType() )
                && Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( mainInfo.getCensorState() ) )
            {
                pubSuccessIds = pubSuccessIds + "," + mainInfo.getContentId();
            }

            request.setAttribute( "someContentId", pubSuccessIds );
            request.setAttribute( "modelId", classBean.getContentType() );
            request.setAttribute( "contentId", mainInfo.getContentId() );
            request.setAttribute( "classId", classBean.getClassId() );
            request.setAttribute( "staticType", Integer.valueOf( 2 ) );
            request.setAttribute( "addOrEditFlag", "true" );

            request.setAttribute( "editMode", Boolean.valueOf( true ) );

            request.setAttribute( "fromFlow", Boolean.TRUE );

            request.setAttribute( "fromAction", Boolean.TRUE );

            request.setAttribute( "manageParam", SystemSafeCharUtil.resumeHTML( ( String ) params
                .get( "_jtop_sys_manage_param" ) ) );

            return ServletUtil.forward( "/publish/generateContent.do" );

        }
        else
        {
           
            if( Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE
                .equals( classBean.getContentProduceType() )
                && Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( mainInfo.getCensorState() ) )
            {
                request.setAttribute( "singleContentPublish", Boolean.TRUE );
                request.setAttribute( "modelId", classBean.getContentType() );
                request.setAttribute( "contentId", mainInfo.getContentId() );
                request.setAttribute( "classId", classBean.getClassId() );

                request.setAttribute( "editMode", Boolean.valueOf( true ) );

                request.setAttribute( "fromFlow", Boolean.TRUE );

                request.setAttribute( "fromAction", Boolean.TRUE );

                request.setAttribute( "manageParam", SystemSafeCharUtil
                    .resumeHTML( ( String ) params.get( "_jtop_sys_manage_param" ) ) );

                if( !isTrace )
                {
                    String actionName = "发布内容";

                    if( Constant.WORKFLOW.CENSOR_STATUS_DRAFT.equals( mainInfo.getCensorState() ) )
                    {
                        actionName = "存稿件";
                    }

                    contentService.addContentOperInfo( mainInfo.getContentId(),
                        ( String ) SecuritySessionKeeper.getSecuritySession().getAuth()
                            .getApellation(), actionName, ( String ) params
                            .get( "jtopcms_sys_flow_suguest" ) );
                }

                return ServletUtil.forward( "/publish/generateContent.do" );
            }
        }

        if( reEditMode )
        {
            Map param = new HashMap();

            param.put( "modelId", classBean.getContentType() );
            param.put( "contentId", mainInfo.getContentId() );
            param.put( "classId", classBean.getClassId() );
            param.put( "fromDraft", params.get( "contentAddStatus" ) );

            param.put( "prePub", Boolean.TRUE );
            param.put( "manageParam", SystemSafeCharUtil.resumeHTML( ( String ) params
                .get( "_jtop_sys_manage_param" ) ) );

            // 记录操作

            if( !isTrace )
            {
                String actionName = "进入工作流";

                contentService
                    .addContentOperInfo( mainInfo.getContentId(), ( String ) SecuritySessionKeeper
                        .getSecuritySession().getAuth().getApellation(), actionName,
                        ( String ) params.get( "jtopcms_sys_flow_suguest" ) );
            }

            return ServletUtil.redirect( "/core/content/EditUserDefineModelContent.jsp", param );

        }

        if( Constant.SITE_CHANNEL.CLASS_TYPE_SINGLE_PAGE.equals( classBean.getClassType() ) )
        {
            // 记录操作

            if( !isTrace )
            {
                String actionName = "发布";

                contentService
                    .addContentOperInfo( mainInfo.getContentId(), ( String ) SecuritySessionKeeper
                        .getSecuritySession().getAuth().getApellation(), actionName,
                        ( String ) params.get( "jtopcms_sys_flow_suguest" ) );
            }

            return ServletUtil.redirect( JtRuntime.cmsServer.getDomainFullPath()
                + "core/content/EditUserDefineModelContent.jsp", returnParams );

        }
        else
        {
            Map param = new HashMap();

            param.put( "modelId", classBean.getContentType() );
            param.put( "censorCId", mainInfo.getContentId() );

            param.put( "contentId", mainInfo.getContentId() );

            param.put( "classId", classBean.getClassId() );

            param.put( "fromFlow", Boolean.TRUE );

            param.put( "fromDraft", params.get( "contentAddStatus" ) );

            param.put( "censorState", Boolean.TRUE );

            param.put( "manageParam", SystemSafeCharUtil.resumeHTML( ( String ) params
                .get( "_jtop_sys_manage_param" ) ) );

            if( execWorkflowActionList.size() == 1 )
            {
                
                isTrace = true;

                param.put( "actId", ( ( String[] ) execWorkflowActionList.get( 0 ) )[0] );

                param.put( "actSuccess", ( ( String[] ) execWorkflowActionList.get( 0 ) )[1] );
            }

            // 记录操作

            if( !isTrace )
            {
                String actionName = "发布内容";

                if( Constant.WORKFLOW.CENSOR_STATUS_DRAFT.equals( mainInfo.getCensorState() ) )
                {
                    actionName = "存稿件";
                }

                contentService
                    .addContentOperInfo( mainInfo.getContentId(), ( String ) SecuritySessionKeeper
                        .getSecuritySession().getAuth().getApellation(), actionName,
                        ( String ) params.get( "jtopcms_sys_flow_suguest" ) );
            }

            return ServletUtil.redirect( JtRuntime.cmsServer.getDomainFullPath()
                + "core/content/EditUserDefineModelContent.jsp", param );

        }

    }

    @ResponseBody
    @RequestMapping( value = "/contentValidate.do", method = { RequestMethod.POST } )
    public Object contentValidate( HttpServletRequest request, HttpServletResponse response )
        throws InstantiationException, IllegalAccessException
    { 
        // 由于存在一些字段允许html,所以不能在这里先使用getHttpRequestSnapshot(),故使用普通方式获取classId
        Long classId = StringUtil.getLongValue( request.getParameter( "classId" ), -1 );

        

        ContentClassBean classBean = channelService.retrieveSingleClassBeanInfoByClassId( classId );

        if( classBean != null && classBean.getClassId().longValue() < 0 )
        {
            throw new FrameworkException( "栏目信息丢失,classId:" + classId );
        }
 
        SiteGroupBean site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
            .getEntry( classBean.getSiteFlag() );

        
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( classBean
            .getContentType() );
        List filedBeanList = metaDataService.retrieveModelFiledInfoBeanList( classBean
            .getContentType() );

       
        Map returnParams = new HashMap();

        returnParams.put( "modelId", classBean.getContentType() );
        returnParams.put( "classId", classId );

        ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( classBean.getContentType() );

        if( modelBean == null || filedBeanList.isEmpty() || sqlCodeBean == null )
        {
            log.error( "模型元数据丢失，模型ID:" + classBean.getContentType() );
            Map errorMap = new HashMap();
            errorMap.put( "title", "模型元数据信息不全！" );
            return StringUtil.changeMapToJSON( errorMap );
        }

        Map params = ServletUtil.getRequestDecodeInfo( request, ServiceUtil
            .checkEditorField( filedBeanList ) );

        boolean editMode = false;
 
        if( request.getServletPath().endsWith( "addContent.do" ) )
        {
            editMode = false;
        }
        else if( request.getServletPath().endsWith( "editContent.do" ) )
        {
            editMode = true;
        }

        
        disposeTitleMode( editMode, modelBean, params );

        
        String testCid = ( String ) params.get( "contentId" );

        
        if( Constant.COMMON.OFF.equals( site.getSameTitle() ) && ( testCid == null ) )
        {
            if( contentService.checkContentTitleExist( site.getSiteId(), ( String ) params
                .get( "title" ) ) )
            {
                Map errorMap = new HashMap();
                errorMap.put( "title", "内容标题已存在" );
                return StringUtil.changeMapToJSON( errorMap );
            }
        }

        String vbClassName = modelBean.getValidateBehavior();

         
        String errorJSON = null;
        if( StringUtil.isStringNotNull( vbClassName ) )
        {
            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    errorJSON = ( String ) ( ( Behavior ) valiedateBehavior ).operation( params,
                        new Object[] { modelBean, filedBeanList, sqlCodeBean } );
                }
            }
        }

        return errorJSON;

    }
 

    

    @ResponseBody
    @RequestMapping( value = { "/clientAddContent.do", "/clientEditContent.do" }, method = { RequestMethod.POST } )
    public Object clientAddOrEditContent( HttpServletRequest request, HttpServletResponse response )
        throws InstantiationException, IllegalAccessException
    {
    
        Long classId = StringUtil.getLongValue( request.getParameter( "classId" ), -1 );

        ContentClassBean classBean = channelService.retrieveSingleClassBeanInfoByClassId( classId );

        if( classBean != null && classBean.getClassId().longValue() < 0 )
        {
            throw new FrameworkException( "栏目信息丢失,classId:" + classId );
        }

        /**
         * 获取对应数据模型元数据
         */
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( classBean
            .getContentType() );
        List filedBeanList = metaDataService.retrieveModelFiledInfoBeanList( classBean
            .getContentType() );

        // 处理复制内容,检查复制栏目是否模型一致

        // 处理站点共享内容

        // 处理相关内容

        // 处理推荐内容 ,推荐内容ID不在main-info里

        /**
         * 根据不同模型进入处理,现系统模型和自定义模型分开
         */

        Map returnParams = new HashMap();

        returnParams.put( "modelId", classBean.getContentType() );
        returnParams.put( "classId", classId );

        SiteGroupBean site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
            .getEntry( classBean.getSiteFlag() );

        if( site == null )
        {
            // 无登陆站点
            return "-2";
        }

        // 验证码
        HttpServletRequest req = request;

        HttpSession ssn = req.getSession();

        String checkCode = ( String ) ssn
            .getAttribute( Constant.SITE_CHANNEL.RANDOM_INPUT_RAND_CODE_KEY );

        String checkCodeTest = ( String ) req.getParameter( "sysCheckCode" );

        if( StringUtil.isStringNull( checkCode ) || !checkCode.equalsIgnoreCase( checkCodeTest ) )
        {
            // 验证码错误
            return "-1";
        }

        if( Constant.COMMON.OFF.equals( classBean.getMemberAddContent() ) )
        {
            // 当前栏目不允许会员投稿
            return "0";
        }

        /**
         * 投稿细粒度权限
         */
        SecuritySession session = SecuritySessionKeeper.getSecuritySession();

        boolean accFalse = false;

        if( session == null || session.getAuth() == null || session.isManager()
            || session.getMember() == null )
        {
            accFalse = true;
        }

        MemberBean member = ( MemberBean ) session.getMember();

        int lever = member.getMemLevel().intValue();

        long score = member.getScore().longValue();

        Long cid = classId;

        MemberAccRuleBean acBean = securityService.retrieveSingleMemberSubmitAccRule( cid );

        if( acBean.getAccRuleId().longValue() > 0 )
        {
            Set authorizationRoleIdSet = acBean.getRoleIdSet();

            Role[] rs = session.getAuth().getUserRole();

            boolean accRoleOk = false;

            for ( int i = 0; i < rs.length; i++ )
            {
                if( authorizationRoleIdSet.contains( ( ( Role ) rs[i] ).getRoleID() ) )
                {
                    accRoleOk = true;
                    break;
                }
            }

            // 确定是否可访问
            boolean accLevelOk = ( lever >= acBean.getMinLever().intValue() ) ? true : false;

            boolean accScoreOk = ( score >= acBean.getMinScore().longValue() ) ? true : false;

            if( acBean.getEft().intValue() == 1 )
            {
                if( !( accRoleOk || accLevelOk || accScoreOk ) )
                {
                    accFalse = true;
                }
            }
            else
            {
                if( !( accRoleOk && accLevelOk && accScoreOk ) )
                {
                    accFalse = true;
                }
            }
        }

        if( accFalse )
        {
            // 无权限
            return "-3";
        }

        ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( classBean.getContentType() );

        if( modelBean == null || filedBeanList.isEmpty() || sqlCodeBean == null )
        {
            log.error( "模型元数据丢失，模型ID:" + classBean.getContentType() );
            return "-2";
        }

        // 确定是否更新模式
        boolean editMode = false;

        // 确定是否更新模式
        if( request.getServletPath().endsWith( "clientAddContent.do" ) )
        {
            editMode = false;
        }
        else if( request.getServletPath().endsWith( "clientEditContent.do" ) )
        {
            editMode = true;
        }

        Map params = ServletUtil.getRequestDecodeInfo( request, ServiceUtil
            .checkEditorField( filedBeanList ) );

        /**
         * 处理数据模式标题
         */
        disposeTitleMode( editMode, modelBean, params );

        // 强制第三方添加内容状态
        params.put( "otherFlag", Constant.COMMON.ON );

        // 强制creator为会员

        params.put( "creator", member.getMemberName() );

        // 编辑模式存在ID
        String testCid = ( String ) params.get( "contentId" );

        // 标题重复验证
        if( !editMode )
        {
            if( Constant.COMMON.OFF.equals( site.getSameTitle() ) && ( testCid == null ) )
            {
                if( contentService.checkContentTitleExist( site.getSiteId(), ( String ) params
                    .get( "title" ) ) )
                {
                    Map errorMap = new HashMap();
                    errorMap.put( "title", "内容标题已存在" );

                    return StringUtil.changeMapToJSON( errorMap );
                }
            }
        }

        String vbClassName = modelBean.getValidateBehavior();

        // 进行自定义验证行为
        String errorJSON = null;
        if( StringUtil.isStringNotNull( vbClassName ) )
        {
            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    errorJSON = ( String ) ( ( Behavior ) valiedateBehavior ).operation( params,
                        new Object[] { modelBean, filedBeanList, sqlCodeBean } );
                }
            }
        }

        if( errorJSON != null )
        {
            return errorJSON;
        }

        /**
         * 进入提交，任何会员提交的内容不可直接发布，需要审核是否有非法或危险内容
         */

        /**
         * 文本特殊处理
         */
        Set editorFieldSet = ServiceUtil.checkEditorField( filedBeanList );

        Iterator editorFieldIter = editorFieldSet.iterator();

        String fieldName = null;

        String text = null;

        List dlImgList = new ArrayList();

        while ( editorFieldIter.hasNext() )
        {
            fieldName = ( String ) editorFieldIter.next();

            text = ( String ) params.get( fieldName );

            // html白名单
            text = ServiceUtil.cleanEditorHtmlByWhiteRule( text );

            // 站外链接
            if( Constant.COMMON.ON.equals( site.getDeleteOutLink() ) )
            {
                text = contentService.deleteContentTextOutHref( text, site );
            }

            // 下载外站图片

            if( Constant.COMMON.ON.equals( site.getDownOutImage() ) )
            {
                text = contentService.downloadImageFormWeb( null, text, site, classBean
                    .getClassId(), dlImgList );
            }

            params.put( fieldName, text );
        }

        vbClassName = modelBean.getBeforeBehavior();

        // 进行前置参数处理行为

        if( StringUtil.isStringNotNull( vbClassName ) )
        {
            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    ( ( Behavior ) valiedateBehavior ).operation( params, new Object[] { modelBean,
                        filedBeanList, sqlCodeBean } );
                }
            }
        }

        List execWorkflowActionList = new ArrayList( 5 );

        ContentMainInfo mainInfo = contentService.addOrEditUserDefineContent( classBean, params,
            execWorkflowActionList, filedBeanList, sqlCodeBean, editMode );

        // 记录下载的图片
        Long resId = null;
        for ( int i = 0; i < dlImgList.size(); i++ )
        {
            resId = ( Long ) dlImgList.get( i );

            contentService.addDownloadImgRes( mainInfo.getContentId(), resId );
        }

        // 记录已经增加或更新的contentId
        params.put( Constant.CONTENT.ID_ORDER_VAR, mainInfo.getContentId() );

        // 进行后置参数处理行为
        vbClassName = modelBean.getAfterBehavior();

        if( StringUtil.isStringNotNull( vbClassName ) )
        {

            Class classObj = ObjectUtility.getClassInstance( vbClassName );

            if( classObj != null )
            {
                Object valiedateBehavior = classObj.newInstance();

                if( valiedateBehavior instanceof Behavior )
                {
                    ( ( Behavior ) valiedateBehavior ).operation( params, new Object[] { modelBean,
                        filedBeanList, sqlCodeBean } );
                }
            }
        }

        // 设置成功标志

        request.setAttribute( "successFlag", Boolean.TRUE );

        return "1";

    }

 

    @ResponseBody
    @RequestMapping( value = "/moveContent.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "移动内容", token = true )
    public Object moveContent( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        String flag = ( String ) params.get( "all" );

        if( "true".equals( flag ) )
        {
            contentService.moveAllContentToSiteClass( StringUtil.getLongValue( ( String ) params
                .get( "classId" ), -1 ), StringUtil.getLongValue( ( String ) params
                .get( "moveClassId" ), -1 ) );
        }
        else
        {
            // 处理移动栏目

            // 移动会导致复制内容复制信息失效,需要更新信息表的新classId
            boolean needPub = contentService.moveContentToSiteClass( StringUtil.changeStringToList(
                ( String ) params.get( "contentIds" ), "," ), StringUtil.getLongValue(
                ( String ) params.get( "moveClassId" ), -1 ) );

            // 由发布逻辑进行发布活动
            if( needPub )
            {
                request.setAttribute( "someContentId", ( String ) params.get( "contentIds" ) );
                request.setAttribute( "staticType", Integer.valueOf( 2 ) );

                return ServletUtil.forward( "/publish/generateContent.do" );
            }
        }

        return "success";
    }

    @ResponseBody
    @RequestMapping( value = "/getMainInfoAjax.do", method = { RequestMethod.POST,
        RequestMethod.GET } )
    public Object getContent( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List cidArrayList = StringUtil.changeStringToList( ( String ) params.get( "contentIds" ),
            "," );

        List contentMainInfoList = contentService.retrieveContentMainInfoByIds( cidArrayList );

        List jsonList = new ArrayList();

        ContentMainInfoBean bean = null;

        for ( int i = 0; i < contentMainInfoList.size(); i++ )
        {
            bean = ( ContentMainInfoBean ) contentMainInfoList.get( i );

            jsonList.add( bean );
        }

        return jsonList;
    }

    @ResponseBody
    @RequestMapping( value = "/addMutiTag.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "添加多个Tag词", token = true )
    public Object addMutiTag( HttpServletRequest request, HttpServletResponse response )
    {

        Map params = ServletUtil.getRequestInfo( request );

        List cidList = StringUtil.changeStringToList( ( String ) params.get( "cIds" ), "," );

        List tagIdList = StringUtil.changeStringToList( ( String ) params.get( "ids" ), "\\*" );

        contentService.addTagRelateContent( cidList, tagIdList );

        return "success";
    }

    @ResponseBody
    @RequestMapping( value = "/deleteRelateContent.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "删除关联内容", token = true )
    public Object deleteRelateContent( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List deleteRIdList = StringUtil.changeStringToList( ( String ) params.get( "deleteRId" ),
            "," );

        Long contentId = Long.valueOf( StringUtil.getLongValue(
            ( String ) params.get( "contentId" ), -1 ) );

        contentService.deleteRelateContent( contentId, deleteRIdList );

        return "success";
    }
 

    @ResponseBody
    @RequestMapping( value = { "/deleteContent.do", "/deleteMySelfSiteContentToTrash.do",
        "/deleteContentToTrash.do", "/deleteSearchContent.do" }, method = { RequestMethod.POST } )
    @ActionInfo( traceName = "删除内容", token = true )
    public Object deleteContent( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List idList = StringUtil.changeStringToList( ( String ) params.get( "ids" ), "," );

        Long modelId = Long.valueOf( StringUtil.getLongValue( ( String ) params.get( "modelId" ),
            -1 ) );

        String classId = ( String ) params.get( "classId" );

        Long targetClassId = StringUtil.getLongValue( classId, -1 );

        String flag = ( String ) params.get( "flag" );

        SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
            .getCurrentLoginSiteInfo();

        List memberList = new ArrayList();

        if( request.getServletPath().endsWith( "deleteContentToTrash.do" )
            || request.getServletPath().endsWith( "deleteMySelfSiteContentToTrash.do" ) )
        {
            if( "allContent".equals( flag ) )
            {
                contentService.deleteAllSystemAndUserDefineContentToTrash( site, modelId,
                    targetClassId, memberList );
            }
            else
            {

                contentService.deleteSystemAndUserDefineContentToTrash( site, idList, memberList );

                contentService.deleteSystemAndUserDefineContentToTrash( site, contentService
                    .retrieveLinkInfo( idList ), memberList );
            }
        }
        else if( request.getServletPath().endsWith( "deleteContent.do" ) )
        {

            if( "allTrash".equals( flag ) )
            {
                contentService.deleteAllSystemAndUserDefineContent( modelId, targetClassId );
            }
            else
            {
                contentService.deleteSystemAndUserDefineContent( idList );

                contentService.deleteSystemAndUserDefineContent( contentService
                    .retrieveTrashLinkInfo( idList ) );
            }
        }
        else if( request.getServletPath().endsWith( "deleteSearchContent.do" ) )
        {
            if( "allContent".equals( flag ) )
            {
                contentService.deleteAllSystemAndUserDefineContentToTrash( site, modelId,
                    targetClassId, memberList );
            }
            else
            {
                contentService.deleteSystemAndUserDefineContentToTrash( site, idList, memberList );

                contentService.deleteSystemAndUserDefineContentToTrash( site, contentService
                    .retrieveLinkInfo( idList ), memberList );
            }
        }

        
        request.setAttribute( "successFlag", Boolean.TRUE );

        request.setAttribute( "memberId", memberList );

      
        ContentClassBean classBean = channelService
            .retrieveSingleClassBeanInfoByClassId( targetClassId );
        if( classBean != null && classBean.getClassId().longValue() > 0 )
        {
            if( classBean.getSyncPubClass().equals( Constant.COMMON.ON )
                && ( Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE.equals( classBean
                    .getClassProduceType() ) || Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE
                    .equals( classBean.getClassHomeProduceType() ) ) )
            {

                request.setAttribute( "classIdInfo", targetClassId.toString() );
                request.setAttribute( "staticType", Integer.valueOf( 1 ) );

                return ServletUtil.forward( "/publish/generateContent.do" );
            }
        }

        return "success";
    }

    @ResponseBody
    @RequestMapping( value = "/recoverContent.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "恢复内容", token = true )
    public Object recoverContent( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List idList = StringUtil.changeStringToList( ( String ) params.get( "ids" ), "," );

        String flag = ( String ) params.get( "all" );

        if( "true".equals( flag ) )
        {

            String classId = ( String ) params.get( "classId" );

            Long targetClassId = Long.valueOf( StringUtil.getLongValue( classId, -1 ) );

            contentService.recoverAllSystemAndUserDefineContent( targetClassId );
        }
        else
        {
            contentService.recoverContentForTrash( idList );

            contentService.recoverContentForTrash( contentService.retrieveTrashLinkInfo( idList ) );
        }

        return "success";
    }

    @ResponseBody
    @RequestMapping( value = "/setTopFlag.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "置顶内容", token = true )
    public Object setTopFlag( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List idList = StringUtil.changeStringToList( ( String ) params.get( "ids" ), "," );

        String topFlag = ( String ) params.get( "top" );

        contentService.setContentTopFlag( idList, topFlag );

        return "success";
    }

   

    @ResponseBody
    @RequestMapping( value = "/sortContent.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "内容排序", token = true )
    public Object sortContent( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        Long targetId = StringUtil.getLongValue( ( String ) params.get( "targetId" ), -1 );
        Long nextId = StringUtil.getLongValue( ( String ) params.get( "nextId" ), -1 );

        contentService.sortContent( targetId, nextId );

        Map returnParam = new HashMap();

        returnParam.put( "fromFlow", "true" );

        return ServletUtil.redirect( "/core/content/dialog/SortContent.jsp", returnParam );

    }

    @ResponseBody
    @RequestMapping( value = "/snapshotImage.do", method = { RequestMethod.POST } )
    public Object snapshotImage( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        Double pos = Double
            .valueOf( StringUtil.getDoubleValue( ( String ) params.get( "pos" ), 0 ) );

        String resInfo = ( String ) params.get( "resInfo" );

        SiteResourceBean resBean = resService.retrieveSingleResourceBeanByResId( Long
            .valueOf( resInfo ) );

        String[] resolutionInfo = StringUtil.split( resBean.getResolution(), "x" );

        int width = StringUtil.getIntValue( resolutionInfo[0], 800 );

        int height = StringUtil.getIntValue( resolutionInfo[1], 600 );

        Long classId = resBean.getClassId();

        String videoResName = resBean.getResName();

        ContentClassBean classBean = channelService.retrieveSingleClassBeanInfoByClassId( classId );

        if( classBean != null && classBean.getClassId().longValue() < 0
            && classId.longValue() != -9999 )
        {
            throw new FrameworkException( "无法取到对应的栏目信息 ，classId:" + classId );
        }

        SiteGroupBean siteBean = null;

        if( classId.longValue() != -9999 )
        {
            siteBean = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
                .getEntry( classBean.getSiteFlag() );
        }
        else
        {
            siteBean = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                .getCurrentLoginSiteInfo();
        }

        String baseRealPath = SystemConfiguration.getInstance().getSystemConfig()
            .getSystemRealPath();

        String videoRealPath = baseRealPath + siteBean.getSiteRoot() + File.separator
            + siteBean.getMediaRoot() + File.separator;

        String videoImgRealPath = baseRealPath + siteBean.getSiteRoot() + File.separator
            + siteBean.getImageRoot() + File.separator;

        // 当前时间日期
        String day = DateAndTimeUtil.getCunrrentDayAndTime( DateAndTimeUtil.DEAULT_FORMAT_YMD );

        File testMediaFile = new File( videoRealPath + resBean.getResSource() );

        if( !testMediaFile.exists() )
        {
            return "no file";
        }

        testMediaFile = new File( videoImgRealPath + day );

        if( !testMediaFile.exists() )
        {
            testMediaFile.mkdirs();
        }

        String genName = StringUtil.getUUIDString() + ".jpg";

        String coverImageRelateUrl = day + File.separator + genName;

        MediaUtil.processVideoImg( videoRealPath + resBean.getResSource(), videoImgRealPath
            + coverImageRelateUrl, Double.valueOf( MathUtil.round( pos.doubleValue(), 0 ) )
            .intValue(), width + "*" + height );

        testMediaFile = new File( videoImgRealPath + coverImageRelateUrl );

        if( !testMediaFile.exists() )
        {
            return "no file";
        }

        // 更新目前截图
        resService.updateSingleVideoResourceCoverByResId( day + Constant.CONTENT.URL_SEP + genName,
            resBean.getResId() );

        SiteResource res = new SiteResource();

        res.setClassId( classId );
        res.setSiteId( siteBean.getSiteId() );
        res.setFileType( "jpg" );
        res.setResType( Constant.RESOURCE.IMAGE_RES_TYPE );
        res.setWidth( Integer.valueOf( width ) );
        res.setHeight( Integer.valueOf( height ) );
        res.setModifyTime( new Timestamp( DateAndTimeUtil.clusterTimeMillis() ) );
        res.setResName( videoResName + "_" + pos + "s" );
        res.setResSource( day + Constant.CONTENT.URL_SEP + genName );

        UpdateState us = resService.addSiteResourceAndUploadTrace( res );

        Long newCoverResId = Long.valueOf( us.getKey() );

        String resize = videoImgRealPath + day + File.separator + "imgResize" + genName;

        if( width >= height )
        {
            ImageUtil.resizeImage( 140, -1, videoImgRealPath + coverImageRelateUrl, resize,
                Constant.RESOURCE.IMAGE_RESIZE_Q_MID );
        }
        else
        {
            ImageUtil.resizeImage( -1, 140, videoImgRealPath + coverImageRelateUrl, resize,
                Constant.RESOURCE.IMAGE_RESIZE_Q_MID );
        }

        List resList = new ArrayList( 2 );

        resList.add( res );

          // //
        // contentService.updateVideoCoverByContentId( day
        // + Constant.CONTENT.URL_SEP + genName, relateUrl );

        UploadImageJsonBean jsonBean = new UploadImageJsonBean();

        jsonBean.setRelatePath( day + Constant.CONTENT.URL_SEP + genName );
        jsonBean.setImageUrl( siteBean.getSiteImagePrefixUrl() + day + Constant.CONTENT.URL_SEP
            + genName );
        jsonBean.setResId( newCoverResId );
        jsonBean.setWidth( Integer.valueOf( width ) );
        jsonBean.setHeight( Integer.valueOf( height ) );
        jsonBean.setImageName( res.getResName() );

        List beanList = new ArrayList();
        beanList.add( jsonBean );

        return StringUtil.changeJSON( beanList, "obj_" );
    }

    private void disposeTitleMode( Boolean editMode, DataModelBean modelBean, Map params )
    {

        if( Constant.COMMON.OFF.equals( modelBean.getTitleMode() ) )
        {
            String titleCol = "";

            if( editMode )
            {
                if( StringUtil.isStringNull( ( String ) params.get( "title" ) ) )
                {
                    titleCol = ( String ) params.get( modelBean.getTitleCol() );

                    if( StringUtil.isStringNull( titleCol ) )
                    {
                        titleCol = StringUtil.getUUIDString();
                    }

                }
                else
                {
                    titleCol = ( String ) params.get( "title" );
                }

                params.put( "title", titleCol );
            }
            else
            {
                titleCol = ( String ) params.get( modelBean.getTitleCol() );

                if( StringUtil.isStringNull( titleCol ) )
                {
                    titleCol = StringUtil.getUUIDString();
                }

                params.put( "title", titleCol );
            }
        }
    }
}
