package cn.com.mjsoft.cms.site.service;

import java.io.File;
import java.io.FileFilter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import cn.com.mjsoft.cms.advert.bean.AdvertContentBean;
import cn.com.mjsoft.cms.advert.dao.AdvertDao;
import cn.com.mjsoft.cms.advert.service.AdvertService;
import cn.com.mjsoft.cms.behavior.InitSiteGroupInfoBehavior;
import cn.com.mjsoft.cms.behavior.JtRuntime;
import cn.com.mjsoft.cms.block.dao.BlockDao;
import cn.com.mjsoft.cms.block.service.BlockService;
import cn.com.mjsoft.cms.channel.bean.ContentClassBean;
import cn.com.mjsoft.cms.channel.bean.ContentCommendTypeBean;
import cn.com.mjsoft.cms.channel.controller.ListCommendTypeInfoTreeController;
import cn.com.mjsoft.cms.channel.controller.ListContentClassInfoTreeController;
import cn.com.mjsoft.cms.channel.dao.ChannelDao;
import cn.com.mjsoft.cms.channel.service.ChannelService;
import cn.com.mjsoft.cms.comment.dao.CommentDao;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.ServiceUtil;
import cn.com.mjsoft.cms.common.datasource.MySqlDataSource;
import cn.com.mjsoft.cms.content.dao.ContentDao;
import cn.com.mjsoft.cms.content.dao.vo.PhotoGroupInfo;
import cn.com.mjsoft.cms.content.service.ContentService;
import cn.com.mjsoft.cms.guestbook.dao.GuestbookDao;
import cn.com.mjsoft.cms.guestbook.service.GuestbookService;
import cn.com.mjsoft.cms.interflow.dao.InterflowDao;
import cn.com.mjsoft.cms.interflow.service.InterflowService;
import cn.com.mjsoft.cms.member.dao.MemberDao;
import cn.com.mjsoft.cms.member.service.MemberService;
import cn.com.mjsoft.cms.metadata.bean.DataModelBean;
import cn.com.mjsoft.cms.metadata.bean.ModelFiledInfoBean;
import cn.com.mjsoft.cms.metadata.bean.ModelPersistenceMySqlCodeBean;
import cn.com.mjsoft.cms.metadata.dao.MetaDataDao;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.cms.pick.dao.PickDao;
import cn.com.mjsoft.cms.pick.service.PickService;
import cn.com.mjsoft.cms.questionnaire.dao.SurveyDao;
import cn.com.mjsoft.cms.questionnaire.service.SurveyService;
import cn.com.mjsoft.cms.resources.service.ResourcesService;
import cn.com.mjsoft.cms.schedule.service.ScheduleService;
import cn.com.mjsoft.cms.security.dao.SecurityDao;
import cn.com.mjsoft.cms.site.bean.SiteFileInfoBean;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.cms.site.dao.SiteGroupDao;
import cn.com.mjsoft.cms.site.dao.vo.SiteFileTransfeState;
import cn.com.mjsoft.cms.site.dao.vo.SiteGroup;
import cn.com.mjsoft.cms.stat.dao.StatDao;
import cn.com.mjsoft.cms.templet.dao.TemplateDao;
import cn.com.mjsoft.framework.config.impl.SystemConfiguration;
import cn.com.mjsoft.framework.exception.FrameworkException;
import cn.com.mjsoft.framework.persistence.core.PersistenceEngine;
import cn.com.mjsoft.framework.persistence.core.support.UpdateState;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.DateAndTimeUtil;
import cn.com.mjsoft.framework.util.FileUtil;
import cn.com.mjsoft.framework.util.StringUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class SiteGroupService
{
    private static Logger log = Logger.getLogger( SiteGroupService.class );

    private static final String SPLIT_CHAR = "*";

    private static SiteGroupService service = null;

    public PersistenceEngine mysqlEngine = new PersistenceEngine( new MySqlDataSource() );

    private ScheduleService scheduleService = ScheduleService.getInstance();

    private ResourcesService resService = ResourcesService.getInstance();

    private MetaDataService metaDataService = MetaDataService.getInstance();

    private ContentService contentService = ContentService.getInstance();

    private SurveyService surveyService = SurveyService.getInstance();

    private AdvertService advertService = AdvertService.getInstance();

    private InterflowService inService = InterflowService.getInstance();

    private BlockService blockService = BlockService.getInstance();

    private PickService pickService = PickService.getInstance();

    private MemberService memberService = MemberService.getInstance();

    private ChannelService channelService = ChannelService.getInstance();

    private GuestbookService gbService = GuestbookService.getInstance();

    private StatDao statDao;

    private SiteGroupDao siteGroupDao;

    private ContentDao contentDao = null;

    private ChannelDao channelDao;

    private GuestbookDao gbDao;

    private CommentDao commentDao;

    private SurveyDao surveyDao;

    private AdvertDao advertDao;

    private InterflowDao inDao;

    private BlockDao blockDao;

    private TemplateDao templetDao;

    private MetaDataDao metaDataDao;

    private PickDao pickDao;

    private SecurityDao securityDao;

    private MemberDao memberDao;

    private SiteGroupService()
    {
        siteGroupDao = new SiteGroupDao( mysqlEngine );

        contentDao = new ContentDao( mysqlEngine );

        metaDataDao = new MetaDataDao( mysqlEngine );

        channelDao = new ChannelDao( mysqlEngine );

        gbDao = new GuestbookDao( mysqlEngine );

        commentDao = new CommentDao( mysqlEngine );

        surveyDao = new SurveyDao( mysqlEngine );

        advertDao = new AdvertDao( mysqlEngine );

        inDao = new InterflowDao( mysqlEngine );

        blockDao = new BlockDao( mysqlEngine );

        templetDao = new TemplateDao( mysqlEngine );

        pickDao = new PickDao( mysqlEngine );

        securityDao = new SecurityDao( mysqlEngine );

        statDao = new StatDao( mysqlEngine );

        memberDao = new MemberDao( mysqlEngine );

    }

    private static synchronized void init()
    {
        if( null == service )
        {
            service = new SiteGroupService();
        }
    }

    public static SiteGroupService getInstance()
    {
        if( null == service )
        {
            init();
        }
        return service;
    }

    public List retrieveAllSiteBean()
    {
        List sitebeanList = siteGroupDao.queryAllSiteBean();

        return sitebeanList;

    }

    public SiteGroupBean retrieveSingleSiteBeanBySiteId( Long siteId )
    {
        SiteGroupBean bean = null;

        bean = siteGroupDao.querySiteBeanById( siteId );

        return bean;
    }

    public void updateSiteStaticUrl( String staticUrl, Long siteId, String flag )
    {

        SiteGroupBean site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupIdInfoCache
            .getEntry( siteId );

        if( StringUtil.isStringNull( staticUrl ) || site == null )
        {
            return;
        }

        if( "pc".equals( flag ) )
        {
            if( !staticUrl.equals( site.getHomePageStaticUrl() ) )
            {
                siteGroupDao.updateSiteStaticUrl( staticUrl, siteId );
            }
        }
        else if( "mob".equals( flag ) )
        {
            if( !staticUrl.equals( site.getMobHomePageStaticUrl() ) )
            {
                siteGroupDao.updateMobSiteStaticUrl( staticUrl, siteId );
            }
        }
        else if( "pad".equals( flag ) )
        {
            if( !staticUrl.equals( site.getPadHomePageStaticUrl() ) )
            {
                siteGroupDao.updatePadSiteStaticUrl( staticUrl, siteId );
            }
        }

    }

    public void deleteSiteGroupNode( List idList )
    {
        if( idList == null || !SecuritySessionKeeper.getSecuritySession().isManager() )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Long siteId = null;

            SiteGroupBean site = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                siteId = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ), -1 ) );

                if( siteId.longValue() < 0 )
                {
                    continue;
                }

                site = siteGroupDao.querySiteBeanById( siteId );

                siteGroupDao.deleteSiteGroupNode( siteId );

                siteGroupDao.saveDeleteSiteGroupTrace( siteId, site.getSiteFlag() );

                channelDao.deleteSiteEditorModuleBySite( siteId );

                channelDao.deleteImageratioBySiteId( siteId );

                /**
                 * 删除扩展数据
                 */
                metaDataService.deleteAndClearDefModelInfo( site.getSiteId(), site
                    .getExtDataModelId(), site.getSiteFlag() );
            }
            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            InitSiteGroupInfoBehavior.bulidSiteGroupInfo();

            ListContentClassInfoTreeController.resizeSiteContentClassCache();
            ListCommendTypeInfoTreeController.resizeSiteCommendTypeCache();
            ChannelDao.clearAllCache();
            ChannelService.clearContentClassCache();

            ContentDao.releaseAllCountCache();
            ContentService.releaseContentCache();
        }
    }

    public void createNewSiteNode( SiteGroup siteGroup )
    {
        if( siteGroup == null )
        {
            return;
        }

        String siteFlag = siteGroup.getSiteFlag();

        if( "core".equalsIgnoreCase( siteFlag ) || "common".equalsIgnoreCase( siteFlag )
            || "WEB-INF".equalsIgnoreCase( siteFlag ) || "sys_temp".equalsIgnoreCase( siteFlag ) )
        {
            return;
        }

        Long siteId = null;

        try
        {
            mysqlEngine.beginTransaction();

            if( !siteGroup.getSiteUrl().endsWith( "/" ) )
            {
                siteGroup.setSiteUrl( siteGroup.getSiteUrl() + "/" );
            }

            siteGroup.setMobSiteUrl( siteGroup.getSiteUrl() + "mob/" );

            siteGroup.setPadSiteUrl( siteGroup.getSiteUrl() + "pab/" );

            siteGroup.setSiteRoot( siteGroup.getSiteFlag() );

            UpdateState us = siteGroupDao.saveSiteGroup( siteGroup );

            if( us.haveKey() )
            {
                /**
                 * 添加站点节点的管理员所在机构以及直系父机构必须同时拥有站点管理权
                 */
                siteId = Long.valueOf( us.getKey() );

                Long newSiteId = Long.valueOf( us.getKey() );

                String systemRoot = SystemConfiguration.getInstance().getSystemConfig()
                    .getSystemRealPath();
                File newDir = new File( systemRoot + siteGroup.getSiteRoot() + File.separator
                    + Constant.CONTENT.TEMPLATE_BASE );

                newDir.mkdirs();

                newDir = new File( systemRoot + siteGroup.getSiteRoot() + File.separator
                    + Constant.CONTENT.TEMPLATE_TEMP_BASE );

                newDir.mkdirs();

                newDir = new File( systemRoot + siteGroup.getSiteRoot() + File.separator
                    + Constant.CONTENT.IMG_BASE );

                newDir.mkdirs();

                newDir = new File( systemRoot + siteGroup.getSiteRoot() + File.separator
                    + Constant.CONTENT.MEDIA_BASE );

                newDir.mkdirs();

                newDir = new File( systemRoot + siteGroup.getSiteRoot() + File.separator
                    + Constant.CONTENT.FILE_BASE );

                newDir.mkdirs();

                newDir = new File( systemRoot + siteGroup.getSiteRoot() + File.separator
                    + Constant.CONTENT.HTML_BASE );

                newDir.mkdirs();

                channelService.addNewEditorAllModuleCode( Long.valueOf( us.getKey() ) );

                siteGroupDao.updateSiteNodeOrder( siteId, Long.valueOf( 0 ) );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            InitSiteGroupInfoBehavior.bulidSiteGroupInfo();

            ListContentClassInfoTreeController.resizeSiteContentClassCache();
            ListCommendTypeInfoTreeController.resizeSiteCommendTypeCache();
            ChannelDao.clearAllCache();
            ChannelService.clearContentClassCache();

            ContentDao.releaseAllCountCache();
            ContentService.releaseContentCache();
        }

        if( siteId != null && siteId.longValue() > 0 )
        {
            scheduleService.startNewCreateContentIndexJob( siteId );
        }

    }

    public void sortSiteNode( Map params )
    {
        try
        {
            mysqlEngine.beginTransaction();

            List anBeanList = siteGroupDao.queryAllSiteBean();

            SiteGroupBean siteBean = null;

            String order = null;

            for ( int i = 0; i < anBeanList.size(); i++ )
            {
                siteBean = ( SiteGroupBean ) anBeanList.get( i );

                order = ( String ) params.get( "orderFlag-" + siteBean.getSiteId() );

                if( StringUtil.getIntValue( order, -1 ) > 0 )
                {
                    siteGroupDao.updateSiteNodeOrder( siteBean.getSiteId(), Long.valueOf( order ) );
                }

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            InitSiteGroupInfoBehavior.bulidSiteGroupInfo();

            ListContentClassInfoTreeController.resizeSiteContentClassCache();
            ListCommendTypeInfoTreeController.resizeSiteCommendTypeCache();
            ChannelDao.clearAllCache();
            ChannelService.clearContentClassCache();

            ContentDao.releaseAllCountCache();
            ContentService.releaseContentCache();

        }

    }

    public void updateSiteNodeBaseInfo( SiteGroup siteGroup )
    {
        if( siteGroup == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            if( !siteGroup.getSiteUrl().endsWith( "/" ) )
            {
                siteGroup.setSiteUrl( siteGroup.getSiteUrl() + "/" );
            }

            {
                siteGroup.setMobSiteUrl( siteGroup.getSiteUrl() + "mob/" );
            }

            {
                siteGroup.setPadSiteUrl( siteGroup.getSiteUrl() + "pab/" );
            }

            siteGroupDao.updateSiteGroupBaseInfo( siteGroup );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            InitSiteGroupInfoBehavior.bulidSiteGroupInfo();

            ListContentClassInfoTreeController.resizeSiteContentClassCache();
            ListCommendTypeInfoTreeController.resizeSiteCommendTypeCache();
            ChannelDao.clearAllCache();
            ChannelService.clearContentClassCache();

            ContentDao.releaseAllCountCache();
            ContentService.releaseContentCache();
        }

    }

    public void updateSiteInfoBySiteId( SiteGroup siteGroup, DataModelBean model,
        List filedBeanList, ModelPersistenceMySqlCodeBean sqlCodeBean, Map requestParams )
    {
        if( siteGroup == null || siteGroup.getSiteId().intValue() < 0 )
        {
            throw new FrameworkException( "传入的站点信息不完全" );
        }

        try
        {
            mysqlEngine.beginTransaction();

            if( !siteGroup.getSiteUrl().endsWith( "/" ) )
            {
                siteGroup.setSiteUrl( siteGroup.getSiteUrl() + "/" );
            }

            {
                siteGroup.setMobSiteUrl( siteGroup.getSiteUrl() + "mob/" );
            }

            {
                siteGroup.setPadSiteUrl( siteGroup.getSiteUrl() + "pab/" );
            }

            if( siteGroup.getImageMaxC().intValue() <= 0 )
            {
                siteGroup.setImageMaxC( Integer.valueOf( 1 ) );
            }

            if( siteGroup.getMediaMaxC().intValue() <= 0 )
            {
                siteGroup.setMediaMaxC( Integer.valueOf( 1 ) );
            }

            if( siteGroup.getFileMaxC().intValue() <= 0 )
            {
                siteGroup.setFileMaxC( Integer.valueOf( 1 ) );
            }

            if( siteGroup.getMemberExpire().intValue() <= 0 )
            {
                siteGroup.setMemberExpire( Integer.valueOf( 60 ) );
            }

            siteGroupDao.updateSiteInfoBySiteId( siteGroup );

            if( siteGroup.getExtDataModelId().longValue() > 0 && model != null
                && filedBeanList != null && sqlCodeBean != null )
            {
                Integer count = contentDao.queryUserDefinedContentExist( model, siteGroup
                    .getSiteId() );

                ModelFiledInfoBean bean = null;

                List needUploadImageGroupInfoList = new ArrayList();

                List userDefineParamList = new ArrayList();

                String reUrl = null;

                Object val = null;

                for ( int j = 0; j < filedBeanList.size(); j++ )
                {
                    bean = ( ModelFiledInfoBean ) filedBeanList.get( j );

                    val = ServiceUtil.disposeDataModelFiledFromWeb( bean, requestParams,
                        needUploadImageGroupInfoList, false );

                    userDefineParamList.add( val );

                    if( Constant.METADATA.UPLOAD_IMG == bean.getHtmlElementId().intValue()
                        && Constant.COMMON.ON.equals( bean.getNeedMark() ) )
                    {
                        reUrl = ServiceUtil.getImageReUrl( ( String ) val );

                        if( !Constant.COMMON.ON.equals( resService.getImageMarkStatus( reUrl ) ) )
                        {
                            if( ServiceUtil.disposeImageMark(
                                ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                                    .getCurrentLoginSiteInfo(), reUrl, Integer.valueOf( ServiceUtil
                                    .getImageW( ( String ) val ) ), Integer.valueOf( ServiceUtil
                                    .getImageH( ( String ) val ) ) ) )
                            {
                                resService.setImageMarkStatus( reUrl, Constant.COMMON.ON );
                            }
                        }
                    }
                }

                userDefineParamList.add( siteGroup.getSiteId() );

                if( count.intValue() == 1 )
                {
                    contentDao.saveOrUpdateModelContent( sqlCodeBean.getUpdateSql(),
                        userDefineParamList.toArray() );
                }
                else if( count.intValue() == 0 )
                {
                    contentDao.saveOrUpdateModelContent( sqlCodeBean.getInsertSql(),
                        userDefineParamList.toArray() );
                }

                /**
                 * 所有图集组件出现的图片入库
                 */
                List oldGroupPhotoList = contentDao.queryGroupPhotoInfoByContentId( siteGroup
                    .getSiteId(), Constant.METADATA.MODEL_TYPE_SITE,
                    ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                        .getCurrentLoginSiteInfo(), true );

                contentDao.deletePhotoGroupInfo( siteGroup.getSiteId(),
                    Constant.METADATA.MODEL_TYPE_SITE );

                PhotoGroupInfo pgi = null;
                Set urlInfoSet = new HashSet();

                for ( int i = 0; i < needUploadImageGroupInfoList.size(); i++ )
                {
                    pgi = ( PhotoGroupInfo ) needUploadImageGroupInfoList.get( i );

                    urlInfoSet.add( pgi.getUrl() );

                    pgi.setContentId( siteGroup.getSiteId() );

                    pgi.setModelType( Constant.METADATA.MODEL_TYPE_SITE );

                    contentDao.saveSingleGroupPhoto( pgi );

                    if( Constant.COMMON.ON.equals( pgi.getNeedMark() ) )
                    {
                        reUrl = ServiceUtil.getImageReUrl( pgi.getUrl() );

                        if( !Constant.COMMON.ON.equals( resService.getImageMarkStatus( reUrl ) ) )
                        {
                            if( ServiceUtil.disposeImageMark(
                                ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                                    .getCurrentLoginSiteInfo(), reUrl, Integer.valueOf( ServiceUtil
                                    .getImageW( pgi.getUrl() ) ), Integer.valueOf( ServiceUtil
                                    .getImageH( pgi.getUrl() ) ) ) )
                            {
                                resService.setImageMarkStatus( reUrl, Constant.COMMON.ON );
                            }
                        }
                    }
                }

                Map pgiInfo = null;
                for ( int i = 0; i < oldGroupPhotoList.size(); i++ )
                {
                    pgiInfo = ( Map ) oldGroupPhotoList.get( i );

                    if( !urlInfoSet.contains( pgiInfo.get( "cmsSysUrl" ) ) )
                    {
                        resService.updateSiteResourceTraceUseStatus( Long.valueOf( StringUtil
                            .getLongValue( ( String ) pgiInfo.get( "resId" ), -1 ) ),
                            Constant.COMMON.OFF );
                    }

                }

                SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                    .getCurrentLoginSiteInfo();

                if( site != null )
                {
                    site.getExt().clear();
                }
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            InitSiteGroupInfoBehavior.bulidSiteGroupInfo();

            ListContentClassInfoTreeController.resizeSiteContentClassCache();
            ListCommendTypeInfoTreeController.resizeSiteCommendTypeCache();
            ChannelDao.clearAllCache();
            ChannelService.clearContentClassCache();

            ContentDao.releaseAllCountCache();
            ContentService.releaseContentCache();
        }

    }

    /**
     * 获取站点文件夹下指定目标层的所有目录,以及文件
     * 
     * @param site 站点名
     * @param entry 需要进入的子目录
     * @param rootPath 根目录物理路径
     * @param folderFilter 目录筛选
     * @param fileFilter 文件筛选
     * @return
     */
    public List retrieveAllSiteFileInfoByPath( String sitse, String entry, String rootPath,
        FileFilter folderFilter, FileFilter fileFilter )
    {
        String fullPath = "";

        if( StringUtil.isStringNull( entry ) )
        {
            fullPath = rootPath;
        }
        else
        {
            if( entry.indexOf( "../" ) != -1 || entry.indexOf( "..%2F" ) != -1
                || entry.indexOf( "..%2f" ) != -1 || entry.indexOf( "WEB-INF" ) != -1 )
            {
                return Collections.EMPTY_LIST;
            }

            String endEntry = StringUtil.replaceString( entry, SPLIT_CHAR, File.separator, false,
                false );

            log.info( "最终入口:" + endEntry );

            fullPath = rootPath + endEntry;

        }

        List fileResultList = new ArrayList();

        File[] folders = FileUtil.getAllFile( fullPath, folderFilter );

        File[] files = FileUtil.getAllFile( fullPath, fileFilter );

        SiteFileInfoBean bean;
        String targetPath = "";
        if( folders != null )
        {

            for ( int i = 0; i < folders.length; i++ )
            {
                bean = new SiteFileInfoBean();
                bean.setFileName( folders[i].getName() );
                bean.setLastModifyTime( DateAndTimeUtil.getFormatDate( folders[i].lastModified(),
                    DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS ) );

                bean.setCreator( "Admin" );

                targetPath = StringUtil.replaceString( folders[i].getPath(), rootPath, "", false,
                    true );

                bean.setEntry( StringUtil.replaceString( targetPath, File.separator, SPLIT_CHAR,
                    false, false ) );
                bean.setSize( folders[i].length() );
                bean.setDir( true );
                fileResultList.add( bean );
            }
        }

        if( files != null )
        {
            for ( int j = 0; j < files.length; j++ )
            {
                bean = new SiteFileInfoBean();
                bean.setFileName( files[j].getName() );
                bean.setLastModifyTime( DateAndTimeUtil.getFormatDate( files[j].lastModified(),
                    DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS ) );
                targetPath = StringUtil.replaceString( files[j].getPath(), rootPath, "", false,
                    true );

                bean.setEntry( StringUtil.replaceString( targetPath, File.separator, SPLIT_CHAR,
                    false, false ) );

                String fileName = files[j].getName();
                String fileType = StringUtil.subString( fileName, fileName.lastIndexOf( "." ) + 1,
                    fileName.length() );

                bean.setCreator( "Admin" );

                bean.setType( fileType );
                bean.setSize( files[j].length() );
                bean.setDir( false );
                fileResultList.add( bean );
            }
        }
        return fileResultList;

    }

    public void addNewSiteFileTransfeState( Map transferData, Long gatewayId )
    {
        if( transferData == null || transferData.isEmpty() )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Iterator iter = transferData.keySet().iterator();
            SiteFileTransfeState bean;
            mysqlEngine.startBatch();
            while ( iter.hasNext() )
            {
                bean = ( ( SiteFileTransfeState ) transferData.get( iter.next() ) );

                bean.setGatewayId( gatewayId );

                siteGroupDao.saveNewFileNotifyInfo( bean );
            }
            mysqlEngine.executeBatch();

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }

    }

    public List retrieveSiteFileTransfeStateBeanByGatewayId( Long gatewayId )
    {
        return siteGroupDao.querySiteFileTransfeStateBeanByGatewayId( gatewayId );
    }

    public void deleteSiteFileTransfeStateBeanByTransferStatus( Integer status )
    {
        siteGroupDao.deleteSiteFileTransfeStateBeanByTransferStatus( status );
    }

    public void deleteSiteFileTransfeStateBeanByTransferId( List ids )
    {
        if( ids == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            for ( int i = 0; i < ids.size(); i++ )
            {
                siteGroupDao.deleteSiteFileTransfeStateBeanByTransferId( ( Long ) ids.get( i ) );
            }
            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }

    }

    public void deleteSiteFileTransfeStateBeanByLastTransferId( Long lastId, Long gatewayId )
    {
        siteGroupDao.deleteSiteFileSuccessTransfeStateBeanByLastId( lastId, gatewayId );
    }

    public void deleteSiteFileTransfeStateBeanByTransferIds( String idsFlag )
    {
        siteGroupDao.deleteSiteFileSuccessTransfeStateBeanByIdsFlag( idsFlag );
    }

    public void updateFileTransferStatus( Long transferFileId, Integer status )
    {
        siteGroupDao.updateFileTransferStatus( transferFileId, status );
    }

    public void addDefaultModelTemplate( Long siteId, Long dataModelId, String listTemplate,
        String contentTemplate )
    {
        siteGroupDao.saveModelTemplate( siteId, dataModelId, listTemplate, contentTemplate );
    }

    public void deleteDefaultModelTemplateBySiteId( Long siteId )
    {
        siteGroupDao.deleteDefaultModelTemplateBySiteId( siteId );
    }

    public void disposeDefaultModelTemplate( Long siteId, Map params )
    {

        try
        {
            mysqlEngine.beginTransaction();

            siteGroupDao.deleteDefaultModelTemplateBySiteId( siteId );

            List dataModelBeanList = metaDataService.retrieveAllDataModelBeanList(
                Constant.METADATA.MD_IS_ALL_STATE, Constant.METADATA.MODEL_TYPE_CONTENT, siteId,
                "-1" );

            DataModelBean model = null;

            String listTemplate = null;
            String contentTemplate = null;

            for ( int i = 0; i < dataModelBeanList.size(); i++ )
            {
                model = ( DataModelBean ) dataModelBeanList.get( i );

                listTemplate = ( String ) params.get( model.getDataModelId() + "-list" );

                contentTemplate = ( String ) params.get( model.getDataModelId() + "-content" );

                siteGroupDao.saveModelTemplate( siteId, model.getDataModelId(), listTemplate,
                    contentTemplate );
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }

    }

    public void deleteCMSSiteAllInfo()
    {
        List deleteSiteIdList = siteGroupDao.queryAllDeleteSiteGroupTrace();

        Map sidInfo = null;

        Long siteId = null;

        String siteFlag = null;

        List classList = null;

        ContentClassBean classBean = null;

        SiteGroupBean site = null;

        DataModelBean modelBean = null;

        List allSiteModelList = null;

        for ( int i = 0; i < deleteSiteIdList.size(); i++ )
        {
            sidInfo = ( Map ) deleteSiteIdList.get( i );

            siteId = ( Long ) sidInfo.get( "siteId" );

            siteFlag = ( String ) sidInfo.get( "siteFlag" );

            siteGroupDao.deleteDeleteSiteGroupTrace( siteId );

            classList = channelService.retrieveAllClassBeanInfoBySiteFlag( siteFlag );

            allSiteModelList = metaDataDao.queryAllDataModelBeanListByModelTypeAndSiteId(
                Constant.METADATA.MODEL_TYPE_CONTENT, siteId );

            site = new SiteGroupBean();

            site.setSiteId( siteId );
            site.setSiteFlag( siteFlag );

            for ( int j = 0; j < classList.size(); j++ )
            {
                classBean = ( ContentClassBean ) classList.get( j );

                /**
                 * 删除所有内容
                 */

                for ( int mb = 0; mb < allSiteModelList.size(); mb++ )
                {
                    modelBean = ( DataModelBean ) allSiteModelList.get( mb );

                    contentService.deleteAllSystemAndUserDefineContentToTrash( site, modelBean
                        .getDataModelId(), classBean.getClassId(), new ArrayList() );

                    contentService.deleteAllSystemAndUserDefineContent( modelBean.getDataModelId(),
                        classBean.getClassId() );
                }

                /**
                 * 删除所有栏目
                 */
                channelService.deleteContentClassAllInfomationNotDisposeTreeInfo( classBean );

                /**
                 * 删除分页辅助页
                 */
                channelDao.deleteClassPublishPageAssistant( classBean.getClassId() );

            }

            contentDao.deleteContentMainInfoBySiteId( siteId );

            /**
             * 删除所有专题栏目
             */
            classList = channelService.retrieveAllSpecClassBeanInfoBySiteFlag( siteFlag );

            for ( int j = 0; j < classList.size(); j++ )
            {
                classBean = ( ContentClassBean ) classList.get( j );

                channelService.deleteContentClassAllInfomationNotDisposeTreeInfo( classBean );
            }

            /**
             * 删除所有推荐位
             */

            List commTypeList = channelDao.queryContentCommendTypeBeanBySiteFlag( siteFlag );

            ContentCommendTypeBean ctBean = null;

            for ( int ct = 0; ct < commTypeList.size(); ct++ )
            {
                ctBean = ( ContentCommendTypeBean ) commTypeList.get( ct );

                contentService.deleteCommendContentColumnInfo( ctBean.getCommFlag(), null, site
                    .getSiteFlag() );

                channelDao.deleteCommendType( ctBean.getCommendTypeId() );
            }

            /**
             * 删除搜索表单数据
             */

            List formModelList = metaDataDao.queryAllDataModelBeanListByModelTypeAndSiteId(
                Constant.METADATA.MODEL_TYPE_DEF_FORM, siteId );

            DataModelBean dfm = null;

            for ( int mo = 0; mo < formModelList.size(); mo++ )
            {
                dfm = ( DataModelBean ) formModelList.get( mo );

                contentService.deleteAllDefFormContent( site, dfm.getDataModelId() );
            }

            /**
             * 删除所有留言
             */
            gbService.deleteGuestbookConfigAllInfoByIds( gbDao
                .queryAllGuestbookConfigIdList( siteId ), site );

            /**
             * 删除所有评论
             */
            commentDao.deleteCommentAllInfoBySiteId( siteId );

            /**
             * 删除所有投票
             */

            List sgIdList = surveyDao.querySurveyGroupIdListBySiteId( siteId );

            surveyService.deleteSurveyGroupInfo( sgIdList );

            /**
             * 删除所有广告
             */

            List advertPosIdList = advertDao.queryAllAdvertPosIdList( siteId );

            Long posId = null;

            for ( int ap = 0; ap < advertPosIdList.size(); ap++ )
            {

                posId = ( Long ) advertPosIdList.get( ap );

                List adList = advertService.retrieveAdvertContentBeanListByPosId( posId );

                AdvertContentBean bean = null;

                StringBuffer buf = new StringBuffer();

                for ( int ad = 0; ad < adList.size(); ad++ )
                {
                    bean = ( AdvertContentBean ) adList.get( ad );

                    buf.append( bean.getAdvertId() + "," );
                }

                advertService.deleteAdvertContent( StringUtil.changeStringToList( buf.toString(),
                    "," ), site );

                advertService.deleteAdvertPositionAndParamValue( posId, site );
            }

            /**
             * 删除所有外链
             */
            List slLtIdList = inDao.queryFriendSiteLinkTypeIdList( siteId );

            inService.deleteFriendSiteType( slLtIdList );

            /**
             * 删除所有公告
             */

            inDao.deleteSiteAnnounceBySiteId( siteId );

            /**
             * 删除区块
             */
            List btIdList = blockDao.queryAllBlockTypeIdList( siteFlag );

            blockService.deleteBlockTypeById( btIdList );

            /**
             * 删除Tag
             */
            channelDao.deleteTagRelateContentBySiteId( siteId );

            channelDao.deleteTagWordInfoBySiteId( siteId );

            channelDao.deleteTagTypeInfoBySiteId( siteId );

            /**
             * 删除模板版本记录
             */
            templetDao.deleteTemplateEditionInfoBySiteId( siteId );

            /**
             * 删除模板辅助记录
             */
            templetDao.deleteTemplateHelperBySiteId( siteId );

            /**
             * 删除采集
             */
            List prList = pickDao.queryPickTaskIdBySiteId( siteId );

            pickService.deletePickContentTask( prList );

            pickDao.deletePickRuleBySiteId( siteId );

            pickService.deleteAllPickWebTrace( siteId );

            /**
             * 删除会员以及相关信息
             */
            Long memberCount = memberService.retrieveMemeberCount( siteId );

            List memberList = memberService.retrieveMemeberList( siteId, Long.valueOf( 0 ), Integer
                .valueOf( memberCount.intValue() ) );

            Map memberInfo = null;

            Long memberId = null;

            for ( int mem = 0; mem < memberList.size(); mem++ )
            {
                memberInfo = ( Map ) memberList.get( mem );

                memberId = ( Long ) memberInfo.get( "memberId" );

                if( !memberInfo.isEmpty() )
                {
                    memberDao.deleteMemberById( memberId );

                    securityDao.deleteMemberRoleRealte( memberId );

                    memberDao.deleteMessage( memberId );
                }
            }

            memberDao.deleteMemberRankBySiteId( siteId );

            memberDao.deleteMemberScoreActBySiteId( siteId );

            memberDao.deleteMemberAccRuleBySiteId( siteId );

            memberDao.deleteMemberRoleRelateSecBySiteId( siteId );

            memberDao.deleteMemberRoleBySiteId( siteId );

            memberDao.deleteMemberSecBySiteId( siteId );

            memberDao.deleteMessageTemplateParamBySiteId( siteId );

            memberDao.deleteMessageTemplateBySiteId( siteId );

            memberDao.deleteMemberLoginTraceBySiteId( siteId );

            memberDao.deleteMemberClassSubmitAccBySiteId( siteId );

            memberDao.deleteMemberClassAccBySiteId( siteId );

            memberDao.deleteMemberAccClassRelateRoleBySiteId( siteId );

            /**
             * 删除模型信息
             */
            List moList = metaDataDao.queryAllDataModelIdListBySiteIdPrivateMode( siteId,
                Constant.COMMON.ON );

            Long moId = null;

            for ( int mo = 0; mo < moList.size(); mo++ )
            {
                moId = ( Long ) moList.get( mo );

                metaDataService.deleteDataModelAllInfo( moId, siteId );

            }

            /**
             * 删除站点权限
             */
            securityDao.deleteRangeOrgRelateResAccBySiteId( siteId );

            securityDao.deleteRangeOrgRelateSiteAccBySiteId( siteId );

            securityDao.deleteOrgRelateResAccBySiteId( siteId );

            /**
             * 访问分析
             */
            statDao.deleteVisitorInfoAnalysisBySiteId( siteId );

            /**
             * 删除所有文件夹
             */
            String baseFileRoot = SystemConfiguration.getInstance().getSystemConfig()
                .getSystemRealPath();

            if( StringUtil.isStringNotNull( siteFlag ) && !"core".equalsIgnoreCase( siteFlag )
                && !"common".equalsIgnoreCase( siteFlag ) && !"WEB-INF".equalsIgnoreCase( siteFlag )
                && !"sys_temp".equalsIgnoreCase( siteFlag ) )
            {
                FileUtil.delFolder( baseFileRoot + File.separator + siteFlag );
            }
        }
    }

    /**
     * 增加待发邮件发送信息
     * 
     * @param siteId
     * @param sendTo
     * @param subject
     * @param mailContent
     * @param createDT
     * @return
     */
    public UpdateState addSiteEmailSendInfo( Long siteId, String sendTo, String subject,
        String mailContent, Timestamp createDT )
    {
        return siteGroupDao.saveSiteEmailSendInfo( siteId, sendTo, subject, mailContent, createDT );
    }

    public Map retrieveSingleSiteEmailSendInfo()
    {
        return siteGroupDao.querySingleSiteEmailSendInfo();
    }

    public void deleteSiteEmailSendInfo( String mailId )
    {
        siteGroupDao.deleteSiteEmailSendInfo( mailId );
    }

    public Map retrieveSingleModelTemplate( Long siteId, Long dataModelId )
    {
        return siteGroupDao.querySingleModelTemplate( siteId, dataModelId );
    }

    public static SiteGroupBean getCurrentSiteInfoFromWebRequest( HttpServletRequest request )
    {
        SiteGroupBean site = ( SiteGroupBean ) request
            .getAttribute( "_____sys__cms__cmd__flow__current__site_____" );

        if( site != null )
        {
            return site;
        }

        String reqUrl = request.getRequestURL().toString();

        String reqUri = request.getRequestURI();

        String siteUrl = StringUtil.replaceString( reqUrl, reqUri, JtRuntime.cmsServer
            .getContextUri(), false, false );

        site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupDomainInfoCache
            .getEntry( siteUrl );

        if( site == null )
        {
            String reqServletPath = request.getServletPath();

            int endPos = reqServletPath.indexOf( "/", 1 );

            if( endPos > 0 )
            {
                String su = siteUrl + StringUtil.subString( reqServletPath, 1, endPos ) + "/";

                site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupDomainInfoCache
                    .getEntry( su );
            }
        }

        return site;
    }

    public List getAllSiteNotSelfForTag()
    {
        List siteList = retrieveAllSiteBean();

        SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
            .getCurrentLoginSiteInfo();

        SiteGroupBean siteObj = null;

        List newSiteList = new ArrayList();

        for ( int i = 0; i < siteList.size(); i++ )
        {
            siteObj = ( SiteGroupBean ) siteList.get( i );

            if( !siteObj.getSiteId().equals( site.getSiteId() ) )
            {
                newSiteList.add( siteObj );
            }
        }

        return newSiteList;
    }

}
