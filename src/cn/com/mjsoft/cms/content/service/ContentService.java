package cn.com.mjsoft.cms.content.service;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import cn.com.mjsoft.cms.behavior.InitSiteGroupInfoBehavior;
import cn.com.mjsoft.cms.channel.bean.ContentClassBean;
import cn.com.mjsoft.cms.channel.bean.ContentCommendTypeBean;
import cn.com.mjsoft.cms.channel.dao.ChannelDao;
import cn.com.mjsoft.cms.channel.service.ChannelService;
import cn.com.mjsoft.cms.cluster.adapter.ClusterCacheAdapter;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.ServiceUtil;
import cn.com.mjsoft.cms.common.dao.ValiDao;
import cn.com.mjsoft.cms.common.datasource.MySqlDataSource;
import cn.com.mjsoft.cms.common.page.Page;
import cn.com.mjsoft.cms.content.bean.ContentCommendPushInfoBean;
import cn.com.mjsoft.cms.content.bean.ContentMainInfoBean;
import cn.com.mjsoft.cms.content.dao.ContentDao;
import cn.com.mjsoft.cms.content.dao.vo.ContentAssistantPageInfo;
import cn.com.mjsoft.cms.content.dao.vo.ContentCommendPushInfo;
import cn.com.mjsoft.cms.content.dao.vo.ContentMainInfo;
import cn.com.mjsoft.cms.content.dao.vo.PhotoGroupInfo;
import cn.com.mjsoft.cms.member.bean.MemberBean;
import cn.com.mjsoft.cms.member.dao.MemberDao;
import cn.com.mjsoft.cms.metadata.bean.DataModelBean;
import cn.com.mjsoft.cms.metadata.bean.ModelFiledInfoBean;
import cn.com.mjsoft.cms.metadata.bean.ModelPersistenceMySqlCodeBean;
import cn.com.mjsoft.cms.metadata.dao.MetaDataDao;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.cms.pick.dao.PickDao;
import cn.com.mjsoft.cms.resources.bean.SiteResourceBean;
import cn.com.mjsoft.cms.resources.dao.ResourcesDao;
import cn.com.mjsoft.cms.resources.dao.vo.SiteResource;
import cn.com.mjsoft.cms.resources.service.ResourcesService;
import cn.com.mjsoft.cms.search.dao.vo.SearchIndexContentState;
import cn.com.mjsoft.cms.search.service.SearchService;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.cms.stat.bean.StatContentVisitOrCommentDWMCount;
import cn.com.mjsoft.cms.stat.service.StatService;
import cn.com.mjsoft.framework.cache.Cache;
import cn.com.mjsoft.framework.config.SystemRuntimeConfig;
import cn.com.mjsoft.framework.config.impl.SystemConfiguration;
import cn.com.mjsoft.framework.exception.FrameworkException;
import cn.com.mjsoft.framework.persistence.core.PersistenceEngine;
import cn.com.mjsoft.framework.persistence.core.support.UpdateState;
import cn.com.mjsoft.framework.security.Auth;
import cn.com.mjsoft.framework.security.session.SecuritySession;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.DateAndTimeUtil;
import cn.com.mjsoft.framework.util.FileUtil;
import cn.com.mjsoft.framework.util.HtmlUtil;
import cn.com.mjsoft.framework.util.ImageUtil;
import cn.com.mjsoft.framework.util.MathUtil;
import cn.com.mjsoft.framework.util.StringUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class ContentService
{
    private static Logger log = Logger.getLogger( ContentService.class );

    public static final int KEY_SIZE = 5;// 最大5个关键词

    private static final int RELATE_SIZE = 5;// 最大25个关联内容

    private static final int SW_MAX = 1;// 最大匹配一个

    private static final Integer DELETE_QUERY_COUNT = Integer.valueOf( 1000 );// 每次删除1000个

    public static Cache singleContentCache = new ClusterCacheAdapter( 20000,
        "contentService.singleContentCache" );

    public static Cache listContentCache = new ClusterCacheAdapter( 20000,
        "contentService.listContentCache" );

    public static Cache listTagContentCountCache = new ClusterCacheAdapter( 4000,
        "contentService.listTagContentCountCache" );

    public static Cache listTagContentCache = new ClusterCacheAdapter( 6000,
        "contentService.listTagContentCache" );

    public static Cache fastListContentCache = new ClusterCacheAdapter( 6000,
        "contentService.fastListContentCache" );

    public static Cache fastContentStatusCache = new ClusterCacheAdapter( 4000,
        "contentService.fastContentStatusCache" );

    private static ContentService service = null;

    public PersistenceEngine mysqlEngine = new PersistenceEngine( new MySqlDataSource() );

    private ChannelService channelService = ChannelService.getInstance();

    private MetaDataService metaDataService = MetaDataService.getInstance();

    private ResourcesService resService = ResourcesService.getInstance();

    private SearchService searchService = SearchService.getInstance();

    private SensitiveWord swFilter = new SensitiveWord();

    private PickDao pickDao = null;

    private MemberDao memberDao;

    private ContentDao contentDao;

    private ChannelDao channelDao;

    private MetaDataDao metaDataDao;

    private ResourcesDao resourcesDao;

    private ValiDao valiDao = null;

    private ContentService()
    {
        pickDao = new PickDao( mysqlEngine );
        contentDao = new ContentDao( mysqlEngine );
        channelDao = new ChannelDao( mysqlEngine );
        metaDataDao = new MetaDataDao( mysqlEngine );

        resourcesDao = new ResourcesDao( mysqlEngine );

        memberDao = new MemberDao( mysqlEngine );

        valiDao = new ValiDao( mysqlEngine );
    }

    private static synchronized void init()
    {
        if( null == service )
        {
            service = new ContentService();
        }
    }

    public static ContentService getInstance()
    {
        if( null == service )
        {
            init();
        }
        return service;
    }

    public Integer retrieveContentCensorStatusById( Long contentId )
    {
        return contentDao.queryContentCensorStatusById( contentId );
    }

    public ContentMainInfoBean retrieveSingleContentMainInfoBean( Long contentId )
    {
        return contentDao.querySingleContentMainInfoBean( contentId );
    }

    public Map retrieveSingleContentMainInfoMap( Long contentId )
    {
        return contentDao.querySingleContentMainInfo( contentId );
    }

    public List retrieveSingleContentMainInfoBeanByIds( List idList )
    {
        return contentDao.querySingleContentMainInfoBeanByIds( idList );
    }

    public List retrieveSingleTrashContentMainInfoBeanByIds( List idList )
    {
        return contentDao.querySingleTrashContentMainInfoBeanByIds( idList );
    }

    public List retrieveContentMainInfoByIds( List cidArrayList )
    {
        List result = new ArrayList();

        long contentId = -1;

        for ( int i = 0; i < cidArrayList.size(); i++ )
        {
            contentId = StringUtil.getLongValue( ( String ) cidArrayList.get( i ), -1 );

            if( contentId < 0 )
            {
                continue;
            }

            result.add( contentDao.querySingleContentMainInfoBean( Long.valueOf( contentId ) ) );
        }

        return result;
    }

    /**
     * 更新指定的内容静态化URL数据
     * 
     * @param endStaticClassFilePath
     * @param videoId
     */
    public void setContentStaticPageURL( String endStaticClassFilePath, Long contentId )
    {
        if( endStaticClassFilePath == null )
        {
            return;
        }

        boolean isUpdate = false;

        try
        {
            mysqlEngine.beginTransaction();

            ContentMainInfoBean mainInfo = contentDao.querySingleContentMainInfoBean( contentId );

            if( mainInfo != null && !endStaticClassFilePath.equals( mainInfo.getStaticPageUrl() ) )
            {
                contentDao.updateContentStaticPageURL( endStaticClassFilePath, contentId );

                isUpdate = true;
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            if( isUpdate )
            {
                ContentDao.releaseAllCountCache();
                releaseContentCache();
            }
        }

    }

    /**
     * 获取指定媒体资源的所有缩略图
     * 
     * @param contentId
     * @return
     */
    public List retrieveMediaSnapshotImageByContentId( Long contentId )
    {
        return contentDao.queryMediaSnapshotImageByContentId( contentId );
    }

    /**
     * 删除指定的图片,包括数据库以及对应所有磁盘文件
     * 
     * @param photoId
     */

    public void deleteSomeImageByPathInfo( String[] pathArray, String cmsRoot,
        SiteGroupBean siteBean )
    {
        if( pathArray == null || cmsRoot == null || siteBean == null )
        {
            return;
        }

        String targetFileBasePath = siteBean.getSiteRoot() + File.separator
            + siteBean.getImageRoot() + File.separator;

        StringBuilder buf = new StringBuilder();

        String path = null;
        for ( int i = 0; i < pathArray.length; i++ )
        {
            path = pathArray[i];

            if( StringUtil.isStringNull( path ) )
            {
                continue;
            }

            if( FileUtil.delFile( cmsRoot
                + targetFileBasePath
                + StringUtil.replaceString( path, Constant.CONTENT.URL_SEP, File.separator, false,
                    false ) ) )
            {
                log.info( "[Service] contentService - deleteSomeImageByPathInfo() 成功删除临时文件:"
                    + targetFileBasePath
                    + StringUtil.replaceString( path, Constant.CONTENT.URL_SEP, File.separator,
                        false, false ) );

                buf.append( targetFileBasePath + path + "," );

            }

            if( FileUtil.delFile( cmsRoot
                + targetFileBasePath
                + StringUtil.replaceString( path, Constant.CONTENT.URL_SEP, File.separator
                    + Constant.CONTENT.RESIZE_IMG_FLAG, false, false ) ) )
            {
                log.info( "[Service] contentService - deleteSomeImageByPathInfo() 成功删除临时文件:"
                    + targetFileBasePath
                    + StringUtil.replaceString( path, Constant.CONTENT.URL_SEP, File.separator
                        + Constant.CONTENT.RESIZE_IMG_FLAG, false, false ) );

                buf.append( targetFileBasePath
                    + StringUtil.replaceString( path, Constant.CONTENT.URL_SEP, "/"
                        + Constant.CONTENT.RESIZE_IMG_FLAG, false, false ) + "," );
            }

            // 删除res信息
            resourcesDao.deleteResInfoByRePath( path );

        }

    }

    /**
     * 添加新的图集附属图片信息
     * 
     * @param pgi
     */
    public Long addSingleGroupPhotoNotUse( PhotoGroupInfo pgi )
    {
        pgi.setPhotoAddTime( new Timestamp( DateAndTimeUtil.clusterTimeMillis() ) );

        return Long.valueOf( contentDao.saveSingleGroupPhoto( pgi ).getKey() );
    }

    /**
     * 对文章内容分页操作
     * 
     * @param text
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private List disposeContentPage( Long contentId, String fullContent )
    {
        if( fullContent == null )
        {
            return new ArrayList();
        }

        String targetContent = fullContent;

        List pageFlagList = new ArrayList();

        org.jsoup.nodes.Document flagContentDoc = Jsoup.parse( targetContent );

        Elements esfc = flagContentDoc
            .getElementsByClass( Constant.CONTENT.CONTENT_PAGE_SPLIT_CLASS );

        Iterator itfc = esfc.iterator();

        ContentAssistantPageInfo capInfoBean = null;

        while ( itfc.hasNext() )
        {
            pageFlagList.add( ( ( org.jsoup.nodes.Element ) itfc.next() ).outerHtml() );
        }

        List pageInfoList = new ArrayList();
        int pageCount = pageFlagList.size();

        for ( int i = 0; i < pageCount; i++ )
        {
            org.jsoup.nodes.Document infoDoc = Jsoup.parse( ( String ) pageFlagList.get( i ) );

            Elements esinfo = infoDoc
                .getElementsByClass( Constant.CONTENT.CONTENT_PAGE_SPLIT_CLASS );
            Iterator itinfo = esinfo.iterator();

            String flagInfoTemp = null;
            String[] flagInfoArray = null;

            int start = 0;
            int end = 0;

            if( itinfo.hasNext() )
            {
                start = targetContent.indexOf( ( String ) pageFlagList.get( i ) );

                if( i + 1 != pageCount )
                {
                    end = targetContent.indexOf( ( String ) pageFlagList.get( i + 1 ) );
                }
                else
                {
                    end = targetContent.length();
                }

                capInfoBean = new ContentAssistantPageInfo();
                flagInfoTemp = ( ( org.jsoup.nodes.Element ) itinfo.next() ).id();

                flagInfoArray = StringUtil.split( flagInfoTemp,
                    Constant.CONTENT.CONTENT_PAGE_SPLIT_STR );

                if( flagInfoArray.length < 2 )
                {
                    continue;
                }

                capInfoBean.setContentId( contentId );
                capInfoBean.setPos( Integer.valueOf( i + 1 ) );
                capInfoBean.setPageTitle( flagInfoArray[1] );

                capInfoBean.setStartPos( Integer.valueOf( start ) );
                capInfoBean.setEndPos( Integer.valueOf( end ) );

                capInfoBean.setPageContent( StringUtil.subString( targetContent, start
                    + ( ( String ) pageFlagList.get( i ) ).length(), end ) );

                pageInfoList.add( capInfoBean );

            }

        }

        return pageInfoList;
    }

    /**
     * 对文章内容分页操作
     * 
     * @param text
     */
    public String deleteContentTextOutHref( String fullContent, SiteGroupBean site )
    {
        if( site == null )
        {
            return null;
        }

        String webBase = site.getHostMainUrl();

        String targetContent = fullContent;

        if( StringUtil.isStringNull( targetContent ) )
        {
            return targetContent;
        }

        org.jsoup.nodes.Document flagContentDoc = Jsoup.parse( targetContent );

        Elements esfc = flagContentDoc.getElementsByTag( "a" );

        Iterator itfc = esfc.iterator();

        org.jsoup.nodes.Element te = null;

        String hrefHtml = null;

        String allHrefHtml = null;

        String href = null;
        while ( itfc.hasNext() )
        {
            te = ( ( org.jsoup.nodes.Element ) itfc.next() );

            href = te.attr( "href" );

            if( href != null && href.indexOf( webBase ) != -1 )
            {
                continue;
            }

            allHrefHtml = te.outerHtml();

            hrefHtml = te.text();

            targetContent = StringUtil.replaceString( targetContent, allHrefHtml, hrefHtml, false,
                false );
        }

        return targetContent;
    }

    public void setContentCensorState( Long contentId, Integer censorStatus )
    {
        try
        {
            mysqlEngine.beginTransaction();

            contentDao.updateContentCensorState( contentId, censorStatus );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            ContentDao.releaseAllCountCache();
            releaseContentCache();
        }
    }

    /**
     * 下载文章中出现的所有图片到本地
     * 
     * @param text
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public String downloadImageFormWeb( String prefixUrl, String text, SiteGroupBean site,
        Long classId, List dfList )
    {
        if( StringUtil.isStringNull( text ) )
        {
            return null;
        }

        if( site == null )
        {
            return null;
        }

        List resList = new ArrayList();

        SystemRuntimeConfig config = SystemConfiguration.getInstance().getSystemConfig();

        String webBase = site.getSiteImagePrefixUrl();

        // String systemBase = config.getSystemRealPath();

        String uploadImageBasePath = config.getSystemRealPath() + File.separator
            + site.getSiteRoot() + File.separator + site.getImageRoot();

        String date = DateAndTimeUtil.getCunrrentDayAndTime( DateAndTimeUtil.DEAULT_FORMAT_YMD );

        Map imgHandlerMap = new HashMap();

        String targetContent = text;

        int startFlag = targetContent.indexOf( "<img", 0 );

        while ( startFlag != -1 )
        {
            int end = targetContent.indexOf( ">", startFlag ) + 1;

            String targetImg = StringUtil.subString( targetContent, startFlag, end );

            int srcStart = targetImg.indexOf( "\"", targetImg.indexOf( " src" ) );

            int endPos = targetImg.indexOf( "\" ", srcStart );

            if( endPos <= srcStart )
            {
                startFlag = targetContent.indexOf( "<img", end );
                continue;
            }

            String targetSrc = StringUtil.subString( targetImg, srcStart + 1, endPos );

            // 如果图片已经是本地的，则不需要做下载处理，后期若出现在图片服务器中的地址也不需要处理

            if( targetSrc.indexOf( webBase ) != -1 )
            {
                startFlag = targetContent.indexOf( "<img", end );
                continue;
            }

            String downTrueUrl = targetSrc;

            if( StringUtil.isStringNotNull( prefixUrl ) && targetSrc != null
                && !targetSrc.toLowerCase().startsWith( "http:" ) )
            {
                downTrueUrl = prefixUrl + targetSrc;
            }

            File endImgFile = HtmlUtil.downloadImageByUrl( downTrueUrl, uploadImageBasePath
                + File.separator + date + Constant.CONTENT.URL_SEP );

            if( endImgFile != null && endImgFile.exists() )
            {

                String successDownloadImgName = endImgFile.getName();

                if( StringUtil.isStringNotNull( successDownloadImgName ) )
                {

                    imgHandlerMap.put( targetSrc, successDownloadImgName );

                    // 加入res
                    SiteResource resInfo = new SiteResource();

                    Object[] imgOffset = ImageUtil.getImageHeightAndWidth( endImgFile.getPath() );

                    Integer width = ( ( Integer ) imgOffset[0] );
                    Integer height = ( ( Integer ) imgOffset[1] );

                    // 缩略图
                    String resize = uploadImageBasePath + File.separator + date
                        + Constant.CONTENT.URL_SEP + "imgResize" + endImgFile.getName();

                    // ImageUtil.zoomPicture( targetFile, resize, 177 );
                    int maxResize = 140;

                    try
                    {
                        if( width.intValue() >= height.intValue() )
                        {

                            ImageUtil.resizeImage( maxResize, -1, endImgFile.getPath(), resize,
                                Constant.RESOURCE.IMAGE_RESIZE_Q_MID );

                        }
                        else
                        {
                            ImageUtil.resizeImage( -1, maxResize, endImgFile.getPath(), resize,
                                Constant.RESOURCE.IMAGE_RESIZE_Q_MID );
                        }
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                    resInfo.setHeight( height );
                    resInfo.setWidth( width );

                    resInfo.setClassId( classId );
                    resInfo.setSiteId( site.getSiteId() );

                    resInfo.setResName( StringUtil.subString( successDownloadImgName, 0,
                        successDownloadImgName.lastIndexOf( "." ) ) );

                    String fileType = StringUtil.subString( successDownloadImgName,
                        successDownloadImgName.lastIndexOf( "." ) + 1,
                        successDownloadImgName.length() ).toLowerCase();
                    resInfo.setFileType( fileType );

                    resInfo.setResType( Constant.RESOURCE.IMAGE_RES_TYPE );

                    resInfo.setModifyTime( new Timestamp( DateAndTimeUtil.clusterTimeMillis() ) );
                    resInfo.setResSize( Long.valueOf( endImgFile.length() ) );
                    resInfo.setResSource( date + Constant.CONTENT.URL_SEP + endImgFile.getName() );

                    UpdateState us = resService
                        .addSiteResourceAndUploadTraceSuccessStatus( resInfo );

                    if( dfList != null )
                    {
                        dfList.add( Long.valueOf( us.getKey() ) );
                    }

                    resList.add( resInfo );

                }

            }
            else
            {
                log.warn( "无法下载图片，目标地址：" + targetSrc );
            }

            startFlag = targetContent.indexOf( "<img", end );
        }

        Iterator it = imgHandlerMap.entrySet().iterator();
        // 替换内容所有出现的已经下载成功的图片

        while ( it.hasNext() )
        {
            Entry entry = ( Entry ) it.next();

            targetContent = StringUtil.replaceString( targetContent, ( String ) entry.getKey(),
                webBase + date + Constant.CONTENT.URL_SEP + entry.getValue(), false, false );
        }

        return targetContent;

    }

    /**
     * 下载文章中出现的所有附件到本地
     * 
     * @param text
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public String downloadFileFormWeb( String prefixUrl, String text, SiteGroupBean site,
        Long classId, List dfList )
    {
        if( StringUtil.isStringNull( text ) )
        {
            return null;
        }

        if( site == null )
        {
            return null;
        }

        List resList = new ArrayList();

        SystemRuntimeConfig config = SystemConfiguration.getInstance().getSystemConfig();

        String webBase = site.getSiteFilePrefixUrl();

        // String systemBase = config.getSystemRealPath();

        String uploadImageBasePath = config.getSystemRealPath() + File.separator
            + site.getSiteRoot() + File.separator + site.getFileRoot();

        String date = DateAndTimeUtil.getCunrrentDayAndTime( DateAndTimeUtil.DEAULT_FORMAT_YMD );

        Map imgHandlerMap = new HashMap();

        Map newImgHandlerMap = new HashMap();

        String targetContent = text;

        /*
         * Document doc = Jsoup.parse( targetContent );
         * 
         * Elements es = doc.getElementsByTag( "img" );
         * 
         * Iterator iter = es.iterator();
         * 
         * Element el = null;
         * 
         * while ( iter.hasNext() ) { el = ( Element ) iter.next(); }
         */

        int startFlag = targetContent.indexOf( "<a", 0 );

        while ( startFlag != -1 )
        {
            int end = targetContent.indexOf( ">", startFlag ) + 1;

            String targetImg = StringUtil.subString( targetContent, startFlag, end );

            int srcStart = targetImg.indexOf( "\"", targetImg.indexOf( " href" ) );

            int endPos = targetImg.indexOf( "\" ", srcStart );

            if( endPos <= srcStart )
            {
                startFlag = targetContent.indexOf( "<a", end );
                continue;
            }

            String targetSrc = StringUtil.subString( targetImg, srcStart + 1, endPos );

            if( targetSrc.indexOf( webBase ) != -1 )
            {
                startFlag = targetContent.indexOf( "<a", end );
                continue;
            }

            String downTrueUrl = targetSrc;

            if( StringUtil.isStringNotNull( prefixUrl ) && targetSrc != null
                && !targetSrc.toLowerCase().startsWith( "http:" ) )
            {
                if( prefixUrl.endsWith( "/" ) && targetSrc.startsWith( "/" ) )
                {
                    downTrueUrl = StringUtil.subString( prefixUrl, 0, prefixUrl.length() - 1 )
                        + targetSrc;
                }
                else
                {
                    downTrueUrl = prefixUrl + targetSrc;
                }
            }

            String testName = StringUtil.subString( downTrueUrl,
                downTrueUrl.lastIndexOf( "." ) + 1, downTrueUrl.length() );

            if( site.getFileAllowType().indexOf( testName + "," ) == -1 )
            {
                startFlag = targetContent.indexOf( "<a", end );
                continue;
            }

            File endFileFile = HtmlUtil.downloadFileByUrl( downTrueUrl, uploadImageBasePath
                + File.separator + date + Constant.CONTENT.URL_SEP, true );

            if( endFileFile != null && endFileFile.exists() )
            {

                String successDownloadImgName = endFileFile.getName();

                if( StringUtil.isStringNotNull( successDownloadImgName ) )
                {

                    imgHandlerMap.put( targetSrc, successDownloadImgName );

                    SiteResource resInfo = new SiteResource();

                    resInfo.setClassId( classId );
                    resInfo.setSiteId( site.getSiteId() );

                    resInfo.setResName( StringUtil.subString( successDownloadImgName, 0,
                        successDownloadImgName.lastIndexOf( "." ) ) );

                    String fileType = StringUtil.subString( successDownloadImgName,
                        successDownloadImgName.lastIndexOf( "." ) + 1,
                        successDownloadImgName.length() ).toLowerCase();
                    resInfo.setFileType( fileType );

                    resInfo.setResType( Constant.RESOURCE.ANY_RES_TYPE );

                    resInfo.setModifyTime( new Timestamp( DateAndTimeUtil.clusterTimeMillis() ) );
                    resInfo.setResSize( Long.valueOf( endFileFile.length() ) );
                    resInfo.setResSource( date + Constant.CONTENT.URL_SEP + endFileFile.getName() );

                    UpdateState us = resService
                        .addSiteResourceAndUploadTraceSuccessStatus( resInfo );

                    if( dfList != null )
                    {
                        dfList.add( Long.valueOf( us.getKey() ) );

                        newImgHandlerMap.put( successDownloadImgName, Long.valueOf( us.getKey() ) );

                    }

                    resList.add( resInfo );

                }

            }
            else
            {
                log.warn( "无法下载文件，目标地址：" + targetSrc );
            }

            startFlag = targetContent.indexOf( "<a", end );
        }

        Iterator it = imgHandlerMap.entrySet().iterator();

        while ( it.hasNext() )
        {
            Entry entry = ( Entry ) it.next();

            Long resId = ( Long ) newImgHandlerMap.get( entry.getValue() );

            if( targetContent.indexOf( ( String ) entry.getKey() + "\"" ) != -1 )
            {
                targetContent = StringUtil.replaceString( targetContent, ( String ) entry.getKey()
                    + "\"", webBase + date + Constant.CONTENT.URL_SEP + entry.getValue()
                    + "\" id='jtopcms_content_file_" + resId + "' name='jtopcms_content_file' ",
                    false, false );
            }
            else
            {
                targetContent = StringUtil.replaceString( targetContent, ( String ) entry.getKey()
                    + "'", webBase + date + Constant.CONTENT.URL_SEP + entry.getValue()
                    + "' id='jtopcms_content_file_" + resId + "' name='jtopcms_content_file' ",
                    false, false );
            }
        }

        return targetContent;

    }

    /**
     * 检查内容中出现的图片和视频数
     * 
     * @param contentId
     * @return
     */
    public Integer[] checkContentImgAndVideoCount( Long contentId )
    {
        Map main = contentDao.querySingleContentMainInfo( contentId );

        /**
         * 获取对应元数据
         */
        ContentClassBean classBean = channelService
            .retrieveSingleClassBeanInfoByClassId( ( Long ) main.get( "classId" ) );

        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( classBean
            .getContentType() );

        ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( classBean.getContentType() );

        if( sqlCodeBean == null )
        {
            return new Integer[] { 0, 0 };
        }

        Map info = contentDao.querySingleUserDefineContentManageMode( sqlCodeBean, modelBean
            .getRelateTableName(), contentId );

        List filedBeanList = metaDataService.retrieveModelFiledInfoBeanList( classBean
            .getContentType() );

        ModelFiledInfoBean filedInfoBean = null;

        Integer ic = 0;

        Integer iv = 0;

        for ( int j = 0; j < filedBeanList.size(); j++ )
        {
            filedInfoBean = ( ModelFiledInfoBean ) filedBeanList.get( j );

            if( Constant.METADATA.INNER_DATA == filedInfoBean.getHtmlElementId().intValue() )
            {
                continue;
            }

            if( Constant.METADATA.UPLOAD_IMG == filedInfoBean.getHtmlElementId().intValue() )
            {
                Long id = StringUtil.getLongValue( ( String ) info.get( filedInfoBean
                    .getFieldSign()
                    + "ResId" ), -1 );

                if( id.longValue() > 0 )
                {
                    ic++;
                }
            }
            if( Constant.METADATA.UPLOAD_MEDIA == filedInfoBean.getHtmlElementId().intValue() )
            {
                Long id = StringUtil.getLongValue( ( String ) info.get( filedInfoBean
                    .getFieldSign()
                    + "ResId" ), -1 );

                if( id.longValue() > 0 )
                {
                    iv++;
                }
            }
            else if( Constant.METADATA.UPLOAD_IMG_GROUP == filedInfoBean.getHtmlElementId()
                .intValue() )
            {
                int count = StringUtil.getIntValue( ( String ) info.get( filedInfoBean
                    .getFieldSign()
                    + "CmsSysCount" ), 0 );

                ic += count;
            }
            else if( Constant.METADATA.EDITER == filedInfoBean.getHtmlElementId().intValue() )
            {

                String text = ( String ) info.get( filedInfoBean.getFieldSign() );

                if( text == null )
                {
                    text = "";
                }

                Document doc = Jsoup.parse( text );

                Iterator eles = doc.getAllElements().iterator();

                Element ele = null;

                String id = null;

                while ( eles.hasNext() )
                {
                    ele = ( Element ) eles.next();

                    id = ele.id();

                    if( id.startsWith( "jtopcms_content_image_" ) )
                    {
                        ic++;
                    }
                    else if( id.startsWith( "jtopcms_content_media_" ) )
                    {
                        iv++;
                    }

                }
            }

        }

        return new Integer[] { ic, iv };
    }

    /**
     * 新增加或改动系统内容模型信息,根据各模型特点进行数据处理
     * 
     * @param classBean
     * @param params
     * @param wfActionList
     * @param filedBeanList
     * @param sqlCodeBean
     * @param editMode
     * @return
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public ContentMainInfo addOrEditUserDefineContent( ContentClassBean classBean, Map params,
        List wfActionList, List filedBeanList, ModelPersistenceMySqlCodeBean sqlCodeBean,
        boolean editMode )
    {
        if( params == null || filedBeanList == null || sqlCodeBean == null || classBean == null )
        {
            return null;
        }

        /**
         * 处理main数据
         */
        SiteGroupBean site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
            .getEntry( classBean.getSiteFlag() );

        DataModelBean model = metaDataDao.querySingleDataModelBeanById( classBean.getContentType() );

        Map workValue = new HashMap();

        ContentMainInfo info = getSystemModelValueFromWebParam( editMode, classBean, params,
            workValue, model.getMainEditorFieldSign(), site );

        ModelFiledInfoBean bean = null;

        List needUploadImageGroupInfoList = new ArrayList();

        List userDefineParamList = new ArrayList();

        Object val = null;

        String reUrl = null;

        Map currentObj = null;

        if( editMode )
        {
            currentObj = contentDao.querySingleUserDefineContent( sqlCodeBean, model
                .getRelateTableName(), info.getContentId() );
        }

        for ( int j = 0; j < filedBeanList.size(); j++ )
        {
            bean = ( ModelFiledInfoBean ) filedBeanList.get( j );

            if( Constant.METADATA.INNER_DATA == bean.getHtmlElementId().intValue() )
            {
                continue;
            }

            val = ServiceUtil.disposeDataModelFiledFromWeb( bean, params,
                needUploadImageGroupInfoList, false );

            if( val == null )
            {
                val = bean.getDefaultValue();
            }

            if( editMode && !params.containsKey( bean.getFieldSign() ) )
            {
                val = currentObj.get( bean.getFieldSign() );
            }

            userDefineParamList.add( val );

            if( Constant.METADATA.UPLOAD_IMG == bean.getHtmlElementId().intValue()
                && Constant.COMMON.ON.equals( bean.getNeedMark() ) )
            {
                reUrl = ServiceUtil.getImageReUrl( ( String ) val );

                if( !Constant.COMMON.ON.equals( resService.getImageMarkStatus( reUrl ) ) )
                {
                    if( ServiceUtil.disposeImageMark( site, reUrl, Integer.valueOf( ServiceUtil
                        .getImageW( ( String ) val ) ), Integer.valueOf( ServiceUtil
                        .getImageH( ( String ) val ) ) ) )
                    {
                        resService.setImageMarkStatus( reUrl, Constant.COMMON.ON );
                    }
                }

            }

            if( Constant.METADATA.MYSQL_DATETIME.equals( bean.getPerdureType() )
                && Constant.COMMON.ON.equals( bean.getOrderFlag() ) )
            {
                if( val instanceof Timestamp )
                {
                    userDefineParamList.add( Long.valueOf( ( ( Timestamp ) val ).getTime() ) );
                }
                else
                {
                    userDefineParamList.add( DateAndTimeUtil.clusterTimeMillis() );
                }
            }
        }

        /**
         * 处理自动关键字
         */
        if( Constant.COMMON.ON.equals( site.getGenKw() )
            && StringUtil.isStringNull( ( String ) params.get( "keywords" ) ) )
        {
            List keyInfoList = null;

            keyInfoList = searchService.disposeTextKeyword( ( String ) params.get( "title" ) );

            String key = null;

            StringBuffer buf = new StringBuffer();

            for ( int i = 0; i < keyInfoList.size(); i++ )
            {
                key = ( String ) keyInfoList.get( i );

                if( i < KEY_SIZE )
                {
                    buf.append( key + " " );
                }

            }

            info.setKeywords( buf.toString() );
        }

        /**
         * 处理自动相关内容
         */

        if( StringUtil.isStringNull( ( String ) params.get( "relateIds" ) )
            && StringUtil.isStringNotNull( ( String ) params.get( "keywords" ) ) )
        {
            String keyVal = ( String ) params.get( "keywords" );

            Object[] result = searchService
                .searchContentByKey( site, null, null, null, channelService
                    .retrieveSiteNotUseRelateFunClassId( site.getSiteFlag() ),
                    Long.valueOf( StringUtil
                        .getLongValue( ( String ) params.get( "contentId" ), -1 ) ),
                    new String[] {}, keyVal, RELATE_SIZE, 1, false, false );

            List reIdList = ( List ) result[0];

            StringBuffer buf = new StringBuffer();

            String id = null;

            for ( int i = 0; i < reIdList.size(); i++ )
            {
                id = ( String ) reIdList.get( i );

                if( i < KEY_SIZE )
                {
                    buf.append( id + "*" );
                }

            }

            info.setRelateIds( buf.toString() );
        }

        Integer endCensorState = null;

        try
        {
            mysqlEngine.beginTransaction();

            Map currentInfo = contentDao.querySingleContentMainInfo( Long.valueOf( StringUtil
                .getLongValue( ( String ) params.get( Constant.METADATA.CONTENT_ID_NAME ), -1 ) ) );

            if( editMode )
            {

                userDefineParamList.add( Long.valueOf( StringUtil.getLongValue( ( String ) params
                    .get( Constant.METADATA.CONTENT_ID_NAME ), -1 ) ) );

                if( info.getAppearStartDateTime() == null )
                {
                    info.setAppearStartDateTime( ( Date ) currentInfo.get( "appearStartDateTime" ) );
                }

                UpdateState updateState = contentDao.updateContentMainInfo( info );

                if( updateState.getRow() > 0 )
                {
                    contentDao.saveOrUpdateModelContent( sqlCodeBean.getUpdateSql(),
                        userDefineParamList.toArray() );
                }

                endCensorState = disposeWorkflowState( params, info.getContentId(), classBean,
                    info, editMode, wfActionList, currentInfo );

            }
            else
            {
                UpdateState updateState = contentDao.saveContentMainInfo( info );

                if( updateState.haveKey() )
                {
                    Long contentId = Long.valueOf( updateState.getKey() );

                    info.setContentId( contentId );

                    userDefineParamList.add( contentId );

                    contentDao.saveOrUpdateModelContent( sqlCodeBean.getInsertSql(),
                        userDefineParamList.toArray() );

                    contentDao.updateSystemContentOrderIdFlag( Double
                        .valueOf( updateState.getKey() ), contentId );

                    endCensorState = disposeWorkflowState( params, info.getContentId(), classBean,
                        info, editMode, wfActionList, currentInfo );

                    for ( int j = 0; j < filedBeanList.size(); j++ )
                    {
                        bean = ( ModelFiledInfoBean ) filedBeanList.get( j );

                        if( Constant.METADATA.INNER_DATA == bean.getHtmlElementId().intValue() )
                        {
                            metaDataService.updateFieldMetadataDefValAndId( model,
                                Constant.METADATA.PREFIX_COLUMN_NAME + bean.getFieldSign(), bean
                                    .getDefaultValue(), contentId );
                        }
                    }

                }

            }

            /**
             * 处理tag信息,只有通过审核发布的内容
             */
            if( Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( endCensorState ) )
            {
                List tagIdList = StringUtil.changeStringToList( ( String ) params.get( "tagKey" ),
                    "\\*" );

                Long tagId = null;

                channelDao.deleteTagRelateContentByContentId( info.getContentId() );

                for ( int i = 0; i < tagIdList.size(); i++ )
                {
                    if( tagIdList.get( i ) instanceof Long )
                    {
                        tagId = ( Long ) tagIdList.get( i );
                    }
                    else
                    {
                        tagId = Long.valueOf( StringUtil.getLongValue(
                            ( String ) tagIdList.get( i ), -1 ) );
                    }

                    if( tagId.longValue() < 0 )
                    {
                        continue;
                    }

                    channelDao.saveTagWordRelateContent( tagId, info.getContentId() );

                    channelDao.updateTagWordRelateContentCount( tagId );
                }
            }

            /**
             * 文章资源类型模型内容分页处理
             */

            List pageInfoList = null;
            if( Constant.METADATA.MODEL_RES_ARTICLE.equals( model.getModelResType() ) )
            {
                contentDao.deleteContentAssistantPageInfoByContentId( info.getContentId() );

                pageInfoList = disposeContentPage( info.getContentId(), ( String ) params
                    .get( model.getMainEditorFieldSign() ) );

                if( !pageInfoList.isEmpty() )
                {
                    contentDao.updateContentPageModeInfo( info.getContentId(), Constant.COMMON.ON );
                }
                else
                {
                    contentDao.updateContentPageModeInfo( info.getContentId(), Constant.COMMON.OFF );
                }
            }

            List oldGroupPhotoList = contentDao.queryGroupPhotoInfoByContentId(
                info.getContentId(), Constant.METADATA.MODEL_TYPE_CONTENT, site, true );

            contentDao.deletePhotoGroupInfo( info.getContentId(),
                Constant.METADATA.MODEL_TYPE_CONTENT );

            PhotoGroupInfo pgi = null;
            Set urlInfoSet = new HashSet();

            for ( int i = 0; i < needUploadImageGroupInfoList.size(); i++ )
            {
                pgi = ( PhotoGroupInfo ) needUploadImageGroupInfoList.get( i );

                urlInfoSet.add( pgi.getUrl() );

                pgi.setContentId( info.getContentId() );

                pgi.setModelType( Constant.METADATA.MODEL_TYPE_CONTENT );

                contentDao.saveSingleGroupPhoto( pgi );

                if( Constant.COMMON.ON.equals( pgi.getNeedMark() ) )
                {
                    reUrl = ServiceUtil.getImageReUrl( pgi.getUrl() );

                    if( !Constant.COMMON.ON.equals( resService.getImageMarkStatus( reUrl ) ) )
                    {
                        if( ServiceUtil.disposeImageMark( site, reUrl, Integer.valueOf( ServiceUtil
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

                    Integer ic = ( Integer ) params.get( "_sys_content_image_res_del_count_" );

                    ic++;

                    params.put( "_sys_content_image_res_del_count_", Integer.valueOf( ic ) );
                }

            }

            if( pageInfoList != null )
            {
                for ( int i = 0; i < pageInfoList.size(); i++ )
                {
                    contentDao
                        .saveArticleContentPageInfo( ( ContentAssistantPageInfo ) pageInfoList
                            .get( i ) );
                }
            }

            SearchIndexContentState searchIndexState = new SearchIndexContentState();

            searchIndexState.setClassId( classBean.getClassId() );
            searchIndexState.setContentId( info.getContentId() );

            searchIndexState.setCensor( endCensorState );

            searchIndexState.setIndexDate( info.getAddTime() );
            searchIndexState.setBoost( info.getBoost() );
            searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_ADD );

            searchIndexState.setModelId( info.getModelId() );
            searchIndexState
                .setSiteId( ( ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
                    .getEntry( classBean.getSiteFlag() ) ).getSiteId() );

            searchService.addIndexContentState( searchIndexState );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            ContentDao.releaseAllCountCache();
            releaseContentCache();
        }

        info.setCensorState( endCensorState );

        return info;
    }

    /**
     * 移动内容到指定栏目
     * 
     * @param contentId
     * @param classIdList
     */
    public boolean moveContentToSiteClass( List contentIdList, Long classId )
    {
        log.info( "[service] moveContentToSiteClass : contentIdList=" + contentIdList
            + ", classId:" + classId );

        if( classId == null || classId.longValue() < 0 )
        {
            return false;
        }

        try
        {
            mysqlEngine.beginTransaction();
            Long contentId = null;
            Long selfClassId = null;
            for ( int x = contentIdList.size() - 1; x >= 0; x-- )
            {
                if( contentIdList.get( x ) instanceof Long )
                {
                    contentId = ( Long ) contentIdList.get( x );
                }
                else
                {
                    contentId = Long.valueOf( StringUtil.getLongValue( ( String ) contentIdList
                        .get( x ), -1 ) );
                }

                if( contentId == null || contentId.longValue() < 1 )
                {
                    continue;
                }

                Map mainInfo = contentDao.querySingleContentMainInfo( contentId );

                DataModelBean modelBean = metaDataDao
                    .querySingleDataModelBeanById( ( Long ) mainInfo.get( "modelId" ) );

                ContentClassBean classBean = channelService
                    .retrieveSingleClassBeanInfoByClassId( classId );

                if( classBean == null )
                {
                    continue;
                }

                if( !classBean.getContentType().equals( modelBean.getDataModelId() ) )
                {
                    continue;
                }

                selfClassId = ( Long ) mainInfo.get( "classId" );

                if( classId.equals( selfClassId ) )
                {
                    continue;
                }

                contentDao.updateContentMoveClassInfo( contentId, classId, classBean
                    .getContentProduceType(), classBean.getOpenComment() );

                Integer endCensor = null;

                if( classBean.getWorkflowId().longValue() > 0 )
                {
                    contentDao.updateContentMainInfoCensorStatus( contentId,
                        Constant.WORKFLOW.CENSOR_STATUS_DRAFT );

                    endCensor = Constant.WORKFLOW.CENSOR_STATUS_DRAFT;
                }
                else
                {

                    endCensor = pendingCensorStateByStartAndEndPublishDate( new Timestamp(
                        ( ( Timestamp ) mainInfo.get( "appearStartDateTime" ) ).getTime() ),
                        new Timestamp( ( ( Timestamp ) mainInfo.get( "appearEndDateTime" ) )
                            .getTime() ), Constant.WORKFLOW.CENSOR_STATUS_SUCCESS );

                    contentDao.updateContentMainInfoCensorStatus( contentId, endCensor );
                }

                SearchIndexContentState searchIndexState = new SearchIndexContentState();

                searchIndexState.setClassId( classId );
                searchIndexState.setContentId( contentId );

                searchIndexState.setCensor( endCensor );
                searchIndexState.setBoost( ( Float ) mainInfo.get( "boost" ) );
                searchIndexState
                    .setIndexDate( new Timestamp( DateAndTimeUtil.clusterTimeMillis() ) );
                searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_EDIT );

                searchIndexState.setModelId( classBean.getContentType() );
                searchIndexState
                    .setSiteId( ( ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
                        .getEntry( classBean.getSiteFlag() ) ).getSiteId() );

                searchService.addIndexContentState( searchIndexState );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            ContentDao.releaseAllCountCache();
            releaseContentCache();
        }

        log.info( "[service] moveContentToSiteClass ...over..." );

        return true;
    }

    public void moveAllContentToSiteClass( Long classId, Long targetMoveToClassId )
    {

        Long prevCid = Long.valueOf( Constant.CONTENT.MAX_ID_FLAG );

        Long modelId = null;

        ContentClassBean classBean = channelDao.querySingleClassBeanInfoByClassId( classId );

        if( classId != null )
        {
            modelId = classBean.getContentType();
        }

        List needMoveContentList = contentDao.queryMainContentIdByClassIdAndModelId( classId,
            modelId, prevCid, DELETE_QUERY_COUNT );

        while ( !needMoveContentList.isEmpty() )
        {
            prevCid = ( Long ) needMoveContentList.get( needMoveContentList.size() - 1 );

            moveContentToSiteClass( needMoveContentList, targetMoveToClassId );

            needMoveContentList = contentDao.queryMainContentIdByClassIdAndModelId( classId,
                modelId, prevCid, DELETE_QUERY_COUNT );
        }
    }

    private String copyEditorRes( String content, Element fileEle, String targetId,
        SiteGroupBean targetSite, String day, long classId )
    {
        String fullSrc = null;

        String newContent = content;

        SiteResourceBean res = null;

        if( StringUtil.isStringNotNull( targetId )
            && targetId.startsWith( "jtopcms_content_image_" ) )
        {

            String id = StringUtil.replaceString( targetId, "jtopcms_content_image_", "", false,
                false );

            if( StringUtil.getLongValue( id, -1 ) > 0 )
            {

                res = resourcesDao.querySingleResourceBeanByResId( Long.valueOf( StringUtil
                    .getLongValue( id, -1 ) ) );

                fullSrc = res != null ? res.getUrl() : "";

                String resInfo = ServiceUtil.copyImageRes( "id=" + id + ";", targetSite, day, Long
                    .valueOf( classId ) );

                String newUrl = targetSite.getSiteImagePrefixUrl()
                    + StringUtil.subString( resInfo, resInfo.indexOf( "reUrl=" ) + 6, resInfo
                        .indexOf( ";", resInfo.indexOf( "reUrl=" ) + 6 ) );

                Long newId = Long.valueOf( StringUtil.getLongValue( StringUtil.subString( resInfo,
                    resInfo.indexOf( "id=" ) + 3, resInfo.indexOf( ";",
                        resInfo.indexOf( "id=" ) + 3 ) ), -1 ) );

                newContent = StringUtil.replaceString( content, targetId, "jtopcms_content_image_"
                    + newId, false, false );

                newContent = StringUtil.replaceString( newContent, fullSrc, newUrl, false, false );
            }

        }
        else if( StringUtil.isStringNotNull( targetId )
            && targetId.startsWith( "jtopcms_content_media_" ) )
        {

            String id = StringUtil.replaceString( targetId, "jtopcms_content_media_", "", false,
                false );

            if( StringUtil.getLongValue( id, -1 ) > 0 )
            {

                res = resourcesDao.querySingleResourceBeanByResId( Long.valueOf( StringUtil
                    .getLongValue( id, -1 ) ) );

                fullSrc = res != null ? res.getUrl() : "";

                String resInfo = ServiceUtil.copyMediaRes( "id=" + id + ";", targetSite, day, Long
                    .valueOf( classId ) );

                String newUrl = targetSite.getSiteMediaPrefixUrl()
                    + StringUtil.subString( resInfo, resInfo.indexOf( "reUrl=" ) + 6, resInfo
                        .indexOf( ";", resInfo.indexOf( "reUrl=" ) + 6 ) );

                Long newId = Long.valueOf( StringUtil.getLongValue( StringUtil.subString( resInfo,
                    resInfo.indexOf( "id=" ) + 3, resInfo.indexOf( ";",
                        resInfo.indexOf( "id=" ) + 3 ) ), -1 ) );

                newContent = StringUtil.replaceString( content, targetId, "jtopcms_content_media_"
                    + newId, false, false );

                newContent = StringUtil.replaceString( newContent, fullSrc, newUrl, false, false );
            }

        }
        else if( StringUtil.isStringNotNull( targetId )
            && targetId.startsWith( "jtopcms_content_file_" ) )
        {
            fullSrc = fileEle.attr( "src" );

            String id = StringUtil.replaceString( targetId, "jtopcms_content_file_", "", false,
                false );

            if( StringUtil.getLongValue( id, -1 ) > 0 )
            {
                res = resourcesDao.querySingleResourceBeanByResId( Long.valueOf( StringUtil
                    .getLongValue( id, -1 ) ) );

                fullSrc = res != null ? res.getUrl() : "";

                String resInfo = ServiceUtil.copyFileRes( "id=" + id + ";", targetSite, day, Long
                    .valueOf( classId ) );

                String newUrl = targetSite.getSiteFilePrefixUrl()
                    + StringUtil.subString( resInfo, resInfo.indexOf( "reUrl=" ) + 6, resInfo
                        .indexOf( ";", resInfo.indexOf( "reUrl=" ) + 6 ) );

                Long newId = Long.valueOf( StringUtil.getLongValue( StringUtil.subString( resInfo,
                    resInfo.indexOf( "id=" ) + 3, resInfo.indexOf( ";",
                        resInfo.indexOf( "id=" ) + 3 ) ), -1 ) );

                newContent = StringUtil.replaceString( content, targetId, "jtopcms_content_file_"
                    + newId, false, false );

                newContent = StringUtil.replaceString( newContent, "content/clientDf.do?id=" + id,
                    "content/clientDf.do?id=" + newId, false, false );

            }

        }

        return newContent;
    }

    /***************************************************************************
     * **************************以下为自定义数据模型业务* **************************
     **************************************************************************/

    public Long retrieveContentMainInfoModelIdByCid( Long id )
    {
        return contentDao.queryContentMainInfoModelIdByCid( id );
    }

    /**
     * 获取指定条件下内容总数目
     * 
     * @param targetClassId
     * @param modelBean
     * @param typeBy 内容属性分类
     * @param orderBy
     * @param orderWay
     * @param currentPublishDateTime
     * @param startDate
     * @param endDate
     * @return
     */
    public Integer getUserDefineContentAllCount( Long targetClassId, DataModelBean modelBean,
        String typeBy, Integer censorBy, Timestamp startDate, Timestamp endDate )
    {
        if( modelBean == null )
        {
            return Integer.valueOf( 0 );
        }

        Integer result = Integer.valueOf( 0 );

        Long startDateContentId = null;

        Long endDateContentId = null;

        if( startDate != null || endDate != null )
        {
            startDateContentId = contentDao.queryMinAddDateContentIdByDate( targetClassId,
                modelBean.getDataModelId(), startDate, endDate );

            endDateContentId = contentDao.queryMaxAddDateContentIdByDate( targetClassId, modelBean
                .getDataModelId(), startDate, endDate );

            if( startDateContentId == null || endDateContentId == null )
            {
                return Integer.valueOf( 0 );
            }
        }

        if( typeBy.length() > 0 )
        {
            if( startDate != null || endDate != null )
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, typeBy, startDateContentId, endDateContentId );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, typeBy, startDateContentId, endDateContentId, censorBy );
                }
            }
            else
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCount( typeBy, targetClassId,
                        modelBean );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCount( typeBy, targetClassId,
                        modelBean, censorBy );
                }
            }
        }
        else
        {
            if( startDate != null || endDate != null )
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, startDateContentId, endDateContentId );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, startDateContentId, endDateContentId, censorBy );
                }
            }
            else
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCount( targetClassId, modelBean );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCount( targetClassId, modelBean,
                        censorBy );
                }
            }
        }

        return result;
    }

    /**
     * 获取指定条件下内容总数目,classIds模式
     * 
     * @param targetClassId
     * @param modelBean
     * @param typeBy 内容属性分类
     * @param orderBy
     * @param orderWay
     * @param currentPublishDateTime
     * @param startDate
     * @param endDate
     * @return
     */
    public Integer getUserDefineContentAllCount( String classIds, String typeBy, Integer censorBy )
    {

        Integer result = Integer.valueOf( 0 );

        if( typeBy.length() > 0 )
        {

            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
            {
                result = contentDao.queryUserDefineContentAllCountIdsTb( typeBy, classIds );
            }
            else
            {
                result = contentDao.queryUserDefineContentAllCountIdsTbCen( typeBy, classIds,
                    censorBy );
            }

        }
        else
        {

            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
            {
                result = contentDao.queryUserDefineContentAllCountIds( classIds );
            }
            else
            {
                result = contentDao.queryUserDefineContentAllCountIdsCen( classIds, censorBy );
            }

        }

        return result;
    }

    /**
     * 获取指定条件下内容总数目
     * 
     * @param targetClassId
     * @param modelBean
     * @param typeBy 内容属性分类
     * @param orderBy
     * @param orderWay
     * @param currentPublishDateTime
     * @param startDate
     * @param endDate
     * @return
     */
    public Integer getUserDefineContentAllCountOrderFilterMode( Long targetClassId,
        DataModelBean modelBean, String typeBy, Integer censorBy, Timestamp startDate,
        Timestamp endDate, String orderFilter )
    {
        if( modelBean == null )
        {
            return Integer.valueOf( 0 );
        }

        Integer result = Integer.valueOf( 0 );

        Long startDateContentId = null;

        Long endDateContentId = null;

        if( startDate != null || endDate != null )
        {
            startDateContentId = contentDao.queryMinAddDateContentIdByDate( targetClassId,
                modelBean.getDataModelId(), startDate, endDate );

            endDateContentId = contentDao.queryMaxAddDateContentIdByDate( targetClassId, modelBean
                .getDataModelId(), startDate, endDate );

            if( startDateContentId == null || endDateContentId == null )
            {
                return Integer.valueOf( 0 );
            }
        }

        if( typeBy.length() > 0 )
        {
            if( startDate != null || endDate != null )
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, typeBy, startDateContentId, endDateContentId, orderFilter );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, typeBy, startDateContentId, endDateContentId, censorBy,
                        orderFilter );
                }
            }
            else
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCount( typeBy, targetClassId,
                        modelBean, orderFilter );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCount( typeBy, targetClassId,
                        modelBean, censorBy, orderFilter );
                }
            }
        }
        else
        {
            if( startDate != null || endDate != null )
            {

                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, startDateContentId, endDateContentId, orderFilter );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCountDateMode( targetClassId,
                        modelBean, startDateContentId, endDateContentId, censorBy, orderFilter );
                }
            }
            else
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    result = contentDao.queryUserDefineContentAllCount( targetClassId, modelBean,
                        orderFilter );
                }
                else
                {
                    result = contentDao.queryUserDefineContentAllCount( targetClassId, modelBean,
                        censorBy, orderFilter );
                }
            }
        }

        return result;
    }

    /**
     * 获取指定条件下内容总数目,多classId模式
     * 
     * @param targetClassId
     * @param modelBean
     * @param typeBy 内容属性分类
     * @param orderBy
     * @param orderWay
     * @param currentPublishDateTime
     * @param startDate
     * @param endDate
     * @return
     */
    public Integer getUserDefineContentAllCountOrderFilterMode( String classIds, String typeBy,
        Integer censorBy, String orderFilter )
    {

        Integer result = Integer.valueOf( 0 );

        if( typeBy.length() > 0 )
        {

            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
            {
                result = contentDao.queryUserDefineContentAllCountIdsTb( typeBy, classIds,
                    orderFilter );
            }
            else
            {
                result = contentDao.queryUserDefineContentAllCountIdsTb( typeBy, classIds,
                    censorBy, orderFilter );
            }

        }
        else
        {

            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
            {
                result = contentDao.queryUserDefineContentAllCountIds( classIds, orderFilter );
            }
            else
            {
                result = contentDao.queryUserDefineContentAllCountIds( classIds, censorBy,
                    orderFilter );
            }

        }

        return result;
    }

    public Map retrieveContentPublishInfo( Long contentId )
    {
        return contentDao.queryContentPublishInfo( contentId );
    }

    /**
     * 根据相关条件获取内容数据,TOP数据永远排最前,按照传入的各种排序标准对内容进行排序,根据数据模型和栏目以及每页数据数量,发布时间等条件控制获取范围
     * 
     * @param modelBean 当前数据模型bena
     * @param classId 当前栏目
     * @param startDate 起始发布时间
     * @param endDate 结束发布时间
     * @param startDate 起始时间
     * @param endDate 结束时间
     * @param orderIdFlag 排序标志
     * @param typeBy 数据过滤标志
     * @param censorBy 数据发布标志
     * @param orderBy 排序依赖标准
     * @param orderWay 排序规则
     * @param pageSize 查询每页数值大小
     * @param currentLimitSize 本次查询实际数据大小
     * @param pageActionFlag 查询动作标志
     * @param currentPage 当前页
     * @return
     */
    public List retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag( boolean showAll,
        DataModelBean modelBean, long classId, Timestamp startDate, Timestamp endDate,
        Object idFlag, String typeBy, Integer censorBy, String orderBy, String orderWay,
        int pageSize, int currentLimitSize, int pageActionFlag, Page pageInfo )
    {
        if( modelBean == null )
        {
            return new ArrayList();
        }

        String key = "retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag:" + showAll + "|"
            + modelBean.getDataModelId() + "|" + classId + "|" + startDate + "|" + endDate + "|"
            + idFlag + "|" + typeBy + "|" + censorBy + "|" + orderBy + "|" + orderWay + "|"
            + pageSize + "|" + currentLimitSize + "|" + pageActionFlag + "|"
            + pageInfo.getCurrentPage();

        List resList = ( List ) listContentCache.getEntry( key );

        if( resList == null )
        {
            if( modelBean == null )
            {
                resList = new ArrayList();
                listContentCache.putEntry( key, resList );
                return resList;
            }

            Long startDateContentId = null;

            Long endDateContentId = null;

            if( startDate != null || endDate != null )
            {
                startDateContentId = contentDao.queryMinAddDateContentIdByDate( Long
                    .valueOf( classId ), modelBean.getDataModelId(), startDate, endDate );

                endDateContentId = contentDao.queryMaxAddDateContentIdByDate( Long
                    .valueOf( classId ), modelBean.getDataModelId(), startDate, endDate );

                if( startDateContentId == null || endDateContentId == null )
                {
                    return new ArrayList( 1 );
                }
            }

            ModelPersistenceMySqlCodeBean perMysqlCodebean = metaDataService
                .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() );

            if( perMysqlCodebean == null )
            {
                resList = new ArrayList();
                listContentCache.putEntry( key, resList );
                return resList;
            }

            boolean inTopArea = false;
            boolean lastTopArea = false;

            Integer[] topPageInfo = null;

            if( typeBy.length() > 0 )
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    topPageInfo = contentDao.queryTopContentPageInfo( typeBy, Long
                        .valueOf( classId ), modelBean.getDataModelId(), startDateContentId,
                        endDateContentId, pageSize );
                }
                else
                {
                    topPageInfo = contentDao.queryTopContentPageInfo( typeBy, Long
                        .valueOf( classId ), modelBean.getDataModelId(), censorBy,
                        startDateContentId, endDateContentId, pageSize );
                }
            }
            else
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    topPageInfo = contentDao.queryTopContentPageInfo( Long.valueOf( classId ),
                        modelBean.getDataModelId(), startDateContentId, endDateContentId, pageSize );
                }
                else
                {
                    topPageInfo = contentDao.queryTopContentPageInfo( Long.valueOf( classId ),
                        modelBean.getDataModelId(), censorBy, startDateContentId, endDateContentId,
                        pageSize );
                }
            }

            if( pageInfo.getCurrentPage() > 0 )
            {
                if( pageInfo.getCurrentPage() <= topPageInfo[0].intValue() )
                {
                    inTopArea = true;

                    if( pageInfo.getCurrentPage() == topPageInfo[0].intValue() )
                    {
                        lastTopArea = true;
                    }
                }
            }

            if( pageActionFlag == Constant.PAGE.PAGE_ACTION_PREV
                || pageActionFlag == Constant.PAGE.PAGE_ACTION_END )
            {
                if( classId == -1 )
                {
                    // resList = contentDao.queryLimitContentByModelAndFlagId(
                    // modelBean, "desc", topFlag, orderIdFlag, currentLimitSize
                    // );
                }
                else
                {
                    if( inTopArea )
                    {
                        if( lastTopArea )
                        {
                            if( typeBy.length() > 0 )
                            {
                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                typeBy, Constant.COMMON.FLAG_OUT, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                                else
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN, censorBy,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                typeBy, Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                            }
                            else
                            {
                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                Constant.COMMON.FLAG_OUT, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                                else
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN, censorBy,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                            }
                        }
                        else
                        {
                            if( typeBy.length() > 0 )
                            {

                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN, idFlag,
                                        Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                                else
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN, censorBy, idFlag,
                                        Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                            }
                            else
                            {
                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN, idFlag,
                                        Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                                else
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN, censorBy, idFlag,
                                        Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                            }

                        }

                    }
                    else
                    {
                        if( typeBy.length() > 0 )
                        {
                            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                .intValue() )
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    typeBy, Constant.COMMON.FLAG_OUT, idFlag,
                                    Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                            else
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    typeBy, Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                    Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                        }
                        else
                        {

                            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                .intValue() )
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    Constant.COMMON.FLAG_OUT, idFlag,
                                    Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                            else
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                    Constant.PAGE.PAGE_ACTION_PREV, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                        }
                    }
                }
            }
            else if( pageActionFlag == Constant.PAGE.PAGE_ACTION_NEXT
                || pageActionFlag == Constant.PAGE.PAGE_ACTION_HEAD )
            {
                if( classId == -1 )
                {
                    // resList = contentDao.queryLimitContentByModelAndFlagId(
                    // modelBean, "desc", topFlag, orderIdFlag, currentLimitSize
                    // );
                }
                else
                {
                    if( inTopArea )
                    {
                        if( lastTopArea )
                        {
                            if( typeBy.length() > 0 )
                            {
                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                typeBy, Constant.COMMON.FLAG_OUT, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                                else
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN, censorBy,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                typeBy, Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                            }
                            else
                            {
                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                Constant.COMMON.FLAG_OUT, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                                else
                                {
                                    Object lastTopContentOrderIdFlag = getMaxOrMinEndPageModeOrderIdByOrderByAndWay(
                                        orderBy, orderWay );

                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN, censorBy,
                                        lastTopContentOrderIdFlag, Constant.PAGE.PAGE_ACTION_PREV,
                                        startDateContentId, endDateContentId, topPageInfo[1]
                                            .intValue() );

                                    if( resList.size() < currentLimitSize )
                                    {
                                        idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                                            orderBy, orderWay );

                                        resList.addAll( contentDao
                                            .queryLimitContentByModelAndFlagId( showAll, modelBean,
                                                perMysqlCodebean, orderBy, orderWay, classId,
                                                Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                                Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                                endDateContentId, currentLimitSize
                                                    - topPageInfo[1].intValue() ) );
                                    }

                                }
                            }
                        }
                        else
                        {
                            if( typeBy.length() > 0 )
                            {
                                if( pageInfo.getCurrentPage() == 1 )
                                {
                                    idFlag = Double.valueOf( Constant.CONTENT.MAX_ORDER_ID_FLAG );
                                }

                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN, idFlag,
                                        Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                                else
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, typeBy, Constant.COMMON.FLAG_IN, censorBy, idFlag,
                                        Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                            }
                            else
                            {

                                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                    .intValue() )
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN, idFlag,
                                        Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                                else
                                {
                                    resList = contentDao.queryLimitContentByModelAndFlagId(
                                        showAll, modelBean, perMysqlCodebean, orderBy, orderWay,
                                        classId, Constant.COMMON.FLAG_IN, censorBy, idFlag,
                                        Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                        endDateContentId, currentLimitSize );
                                }
                            }

                        }

                    }
                    else
                    {

                        if( pageInfo.getCurrentPage() == topPageInfo[0].intValue() + 1
                            && topPageInfo[1].intValue() == pageSize )
                        {
                            idFlag = getMaxOrMinHeadPageModeOrderIdByOrderByAndWay( orderBy,
                                orderWay );
                        }

                        if( typeBy.length() > 0 )
                        {
                            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                .intValue() )
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    typeBy, Constant.COMMON.FLAG_OUT, idFlag,
                                    Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                            else
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    typeBy, Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                    Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                        }
                        else
                        {

                            if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy
                                .intValue() )
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    Constant.COMMON.FLAG_OUT, idFlag,
                                    Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                            else
                            {
                                resList = contentDao.queryLimitContentByModelAndFlagId( showAll,
                                    modelBean, perMysqlCodebean, orderBy, orderWay, classId,
                                    Constant.COMMON.FLAG_OUT, censorBy, idFlag,
                                    Constant.PAGE.PAGE_ACTION_NEXT, startDateContentId,
                                    endDateContentId, currentLimitSize );
                            }
                        }
                    }
                }
            }

            listContentCache.putEntry( key, resList );
        }

        return resList;
    }

    /**
     * limit传统分页取数据,包含top数据模式
     * 
     * @param modelBean
     * @param targetClassId
     * @param typeBy
     * @param censorBy
     * @param startDate
     * @param endDate
     * @param pagePos
     * @param pageSize
     * @param orderBy
     * @param orderWay
     * @return
     */
    public List retrieveLimitModeContentTopMode( boolean showAll, DataModelBean modelBean,
        long targetClassId, Integer censorBy, String typeBy, Timestamp startDate,
        Timestamp endDate, long pagePos, int pageSize, String orderFilter, String orderBy,
        String orderWay )
    {
        if( modelBean == null )
        {
            return new ArrayList();
        }

        String key = "retrieveLimitModeContentTopMode:" + showAll + "|"
            + modelBean.getDataModelId() + "|" + targetClassId + "|" + censorBy + "|" + typeBy
            + "|" + startDate + "|" + endDate + "|" + pagePos + "|" + pageSize + "|" + orderFilter
            + "|" + orderBy + "|" + orderWay;

        List result = ( List ) listContentCache.getEntry( key );

        if( result == null )
        {

            ModelPersistenceMySqlCodeBean perMysqlCodebean = metaDataService
                .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() );

            Long startDateContentId = null;

            Long endDateContentId = null;

            boolean haveDateFlag = false;
            if( startDate != null || endDate != null )
            {
                haveDateFlag = true;

                startDateContentId = contentDao.queryMinAddDateContentIdByDate( Long
                    .valueOf( targetClassId ), modelBean.getDataModelId(), startDate, endDate );

                endDateContentId = contentDao.queryMaxAddDateContentIdByDate( Long
                    .valueOf( targetClassId ), modelBean.getDataModelId(), startDate, endDate );

                if( startDateContentId == null || endDateContentId == null )
                {
                    result = new ArrayList( 1 );
                    listContentCache.putEntry( key, result );
                    return result;
                }
            }

            if( typeBy.length() > 0 )
            {
                if( haveDateFlag )
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, typeBy, startDateContentId,
                            endDateContentId, pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                    else
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(), typeBy,
                            startDateContentId, endDateContentId, pagePos, pageSize, orderFilter,
                            orderBy, orderWay );
                    }
                }
                else
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, typeBy, null, null, pagePos, pageSize,
                            orderFilter, orderBy, orderWay );
                    }
                    else
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(), typeBy, null,
                            null, pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                }
            }
            else
            {
                if( haveDateFlag )
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, startDateContentId, endDateContentId,
                            pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                    else
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(),
                            startDateContentId, endDateContentId, pagePos, pageSize, orderFilter,
                            orderBy, orderWay );
                    }
                }
                else
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, null, null, pagePos, pageSize,
                            orderFilter, orderBy, orderWay );
                    }
                    else
                    {
                        result = contentDao.queryLimitModeContentTopMode( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(), null, null,
                            pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                }
            }

            listContentCache.putEntry( key, result );
        }

        return result;
    }

    /**
     * limit传统分页取数据,普通数据模式
     * 
     * @param modelBean
     * @param targetClassId
     * @param typeBy
     * @param censorBy
     * @param startDate
     * @param endDate
     * @param pagePos
     * @param pageSize
     * @param orderBy
     * @param orderWay
     * @return
     */
    public List retrieveLimitModeContent( boolean showAll, DataModelBean modelBean,
        long targetClassId, Integer censorBy, String typeBy, Timestamp startDate,
        Timestamp endDate, long pagePos, int pageSize, String orderFilter, String orderBy,
        String orderWay )
    {
        if( modelBean == null )
        {
            return new ArrayList();
        }

        String key = "retrieveLimitModeContent:" + showAll + "|" + modelBean.getDataModelId() + "|"
            + targetClassId + "|" + censorBy + "|" + typeBy + "|" + startDate + "|" + endDate + "|"
            + pagePos + "|" + pageSize + "|" + orderFilter + "|" + orderBy + "|" + orderWay;

        List result = ( List ) fastListContentCache.getEntry( key );

        if( result == null )
        {

            ModelPersistenceMySqlCodeBean perMysqlCodebean = metaDataService
                .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() );

            Long startDateContentId = null;

            Long endDateContentId = null;

            boolean haveDateFlag = false;

            if( startDate != null || endDate != null )
            {
                haveDateFlag = true;

                startDateContentId = contentDao.queryMinAddDateContentIdByDate( Long
                    .valueOf( targetClassId ), modelBean.getDataModelId(), startDate, endDate );

                endDateContentId = contentDao.queryMaxAddDateContentIdByDate( Long
                    .valueOf( targetClassId ), modelBean.getDataModelId(), startDate, endDate );

                if( startDateContentId == null || endDateContentId == null )
                {
                    result = new ArrayList( 1 );
                    listContentCache.putEntry( key, result );
                    return result;
                }
            }

            if( typeBy.length() > 0 )
            {
                if( haveDateFlag )
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {
                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, typeBy, startDateContentId,
                            endDateContentId, pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                    else
                    {
                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(), typeBy,
                            startDateContentId, endDateContentId, pagePos, pageSize, orderFilter,
                            orderBy, orderWay );
                    }
                }
                else
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {
                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, typeBy, null, null, pagePos, pageSize,
                            orderFilter, orderBy, orderWay );
                    }
                    else
                    {
                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(), typeBy, null,
                            null, pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                }
            }
            else
            {
                if( haveDateFlag )
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {
                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, startDateContentId, endDateContentId,
                            pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                    else
                    {
                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(),
                            startDateContentId, endDateContentId, pagePos, pageSize, orderFilter,
                            orderBy, orderWay );
                    }
                }
                else
                {
                    if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                    {

                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, null, null, pagePos, pageSize,
                            orderFilter, orderBy, orderWay );

                    }
                    else
                    {
                        result = contentDao.queryLimitModeContent( showAll, modelBean,
                            perMysqlCodebean, targetClassId, censorBy.intValue(), null, null,
                            pagePos, pageSize, orderFilter, orderBy, orderWay );
                    }
                }
            }

            fastListContentCache.putEntry( key, result );

        }

        return result;
    }

    /**
     * limit传统分页取数据,普通数据模式,可以传入多个栏目信息,只获取已经通过审核发布的内容
     * 
     * @param modelBean
     * @param targetClassId
     * @param typeBy
     * @param censorBy
     * @param startDate
     * @param endDate
     * @param pagePos
     * @param pageSize
     * @param orderBy
     * @param orderWay
     * @return
     */
    public List retrieveLimitModeContentMainInfoByClassIds( String classIds, String typeBy,
        long startPos, int pageSize, String orderFilter, String orderBy, String orderWay )
    {

        List result = null;

        String key = "retrieveLimitModeContentMainInfoByClassIds:" + classIds + "|" + typeBy + "|"
            + startPos + "|" + pageSize + "|" + orderFilter + "|" + orderBy + "|" + orderWay;

        String order = orderBy.toLowerCase();

        boolean fastMode = false;

        if( order.indexOf( "comm" ) != -1 || order.indexOf( "click" ) != -1
            || order.indexOf( "su" ) != -1 || order.indexOf( "ag" ) != -1 )
        {
            fastMode = true;
        }

        if( fastMode )
        {
            result = ( List ) fastListContentCache.getEntry( key );
        }
        else
        {
            result = ( List ) listContentCache.getEntry( key );
        }

        if( result == null )
        {
            Integer censorBy = Constant.WORKFLOW.CENSOR_STATUS_SUCCESS;

            if( typeBy.length() > 0 )
            {

                result = contentDao
                    .queryLimitModeContentIdByClassIdsOnlyMainInfo( classIds, typeBy, censorBy
                        .intValue(), startPos, pageSize, orderFilter, orderBy, orderWay );
            }
            else
            {
                result = contentDao.queryLimitModeContentIdByClassIdsOnlyMainInfo( classIds, null,
                    censorBy.intValue(), startPos, pageSize, orderFilter, orderBy, orderWay );

            }

            if( fastMode )
            {
                fastListContentCache.putEntry( key, result );
            }
            else
            {
                listContentCache.putEntry( key, result );
            }

        }

        return result;
    }

    /**
     * limit传统分页取数据,普通数据模式,可以传入多个栏目信息,只获取已经通过审核发布的内容
     * 
     * @param modelBean
     * @param targetClassId
     * @param typeBy
     * @param censorBy
     * @param startDate
     * @param endDate
     * @param pagePos
     * @param pageSize
     * @param orderBy
     * @param orderWay
     * @return
     */
    public List retrieveLimitModeContentMainInfo( Long siteId, String typeBy, long startPos,
        int pageSize, String orderFilter, String orderBy, String orderWay )
    {

        List result = null;

        String key = "retrieveLimitModeContentMainInfo:" + siteId + "|" + typeBy + "|" + startPos
            + "|" + pageSize + "|" + orderFilter + "|" + orderBy + "|" + orderWay;

        String order = orderBy.toLowerCase();

        boolean fastMode = false;

        if( order.indexOf( "comm" ) != -1 || order.indexOf( "click" ) != -1
            || order.indexOf( "su" ) != -1 || order.indexOf( "ag" ) != -1 )
        {
            fastMode = true;
        }

        if( fastMode )
        {
            result = ( List ) fastListContentCache.getEntry( key );
        }
        else
        {
            result = ( List ) listContentCache.getEntry( key );

        }

        if( result == null )
        {

            Integer censorBy = Constant.WORKFLOW.CENSOR_STATUS_SUCCESS;

            if( typeBy.length() > 0 )
            {

                result = contentDao.queryLimitModeContentIdOnlyMainInfo( siteId, typeBy, censorBy
                    .intValue(), startPos, pageSize, orderFilter, orderBy, orderWay );

            }
            else
            {
                result = contentDao.queryLimitModeContentIdOnlyMainInfo( siteId, null, censorBy
                    .intValue(), startPos, pageSize, orderFilter, orderBy, orderWay );

            }

            if( fastMode )
            {
                fastListContentCache.putEntry( key, result );
            }
            else
            {
                listContentCache.putEntry( key, result );
            }

        }

        return result;
    }

    /**
     * 
     * @param targetClassId
     * @param modelId
     * @param filterBy
     * @param startDate
     * @param endDate
     * @return
     */
    public Map retrieveContentQueryFlagForEndPageMode( Long targetClassId, Long modelId,
        String typeBy, Integer censorBy, String orderBy, String orderWay, Timestamp startDate,
        Timestamp endDate )
    {
        if( typeBy.length() > 0 )
        {
            if( startDate != null || endDate != null )
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    return contentDao.queryFirstContentQueryFlagDateMode( typeBy, targetClassId,
                        startDate, endDate, modelId );
                }
                else
                {
                    return contentDao.queryFirstContentQueryFlagDateMode( typeBy, targetClassId,
                        startDate, endDate, modelId );
                }
            }
            else
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    return contentDao.queryFirstContentQueryFlagDateMode( typeBy, targetClassId,
                        modelId );
                }
                else
                {
                    return contentDao.queryFirstContentQueryFlag( typeBy, targetClassId, modelId,
                        censorBy );
                }
            }

        }
        else
        {
            if( startDate != null || endDate != null )
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    return contentDao.queryFirstContentQueryFlagDateMode( targetClassId, startDate,
                        endDate, modelId );
                }
                else
                {
                    return contentDao.queryFirstContentQueryFlagDateMode( targetClassId, startDate,
                        endDate, modelId );
                }
            }
            else
            {
                if( Constant.WORKFLOW.CENSOR_ALL_STATUS.intValue() == censorBy.intValue() )
                {
                    return contentDao.queryFirstContentQueryFlagDateMode( targetClassId, modelId );
                }
                else
                {
                    return contentDao.queryFirstContentQueryFlag( targetClassId, modelId, censorBy );
                }
            }

        }

    }

    public void updateWaitPublishContentSuccessStatus( Map contentMap, Timestamp currDT )
    {
        try
        {
            mysqlEngine.beginTransaction();

            Long contentId = ( Long ) contentMap.get( "contentId" );

            contentDao.updateSystemPublishIdFlagAndCensorStatusAndPubDate(
                getNextPublishOrderTrace(), Constant.WORKFLOW.CENSOR_STATUS_SUCCESS, currDT,
                contentId );

            SearchIndexContentState searchIndexState = new SearchIndexContentState();

            searchIndexState.setClassId( ( Long ) contentMap.get( "classId" ) );
            searchIndexState.setContentId( contentId );

            searchIndexState.setCensor( Constant.WORKFLOW.CENSOR_STATUS_SUCCESS );
            searchIndexState.setBoost( ( Float ) contentMap.get( "boost" ) );
            searchIndexState.setIndexDate( ( Date ) contentMap.get( "addTime" ) );
            searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_EDIT );

            searchIndexState.setModelId( ( Long ) contentMap.get( "modelId" ) );
            searchIndexState.setSiteId( ( Long ) contentMap.get( "siteId" ) );

            searchService.addIndexContentState( searchIndexState );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }
    }

    public void updateWithdrawContentSuccessStatus( Map contentMap )
    {
        try
        {
            mysqlEngine.beginTransaction();

            Long contentId = ( Long ) contentMap.get( "contentId" );

            contentDao.updateContentCensorState( contentId,
                Constant.WORKFLOW.CENSOR_STATUS_WITHDRAW );

            SearchIndexContentState searchIndexState = new SearchIndexContentState();

            searchIndexState.setClassId( ( Long ) contentMap.get( "classId" ) );
            searchIndexState.setContentId( contentId );

            searchIndexState.setCensor( Constant.WORKFLOW.CENSOR_STATUS_WITHDRAW );
            searchIndexState.setBoost( ( Float ) contentMap.get( "boost" ) );
            searchIndexState.setIndexDate( ( Date ) contentMap.get( "addTime" ) );
            searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_EDIT );

            searchIndexState.setModelId( ( Long ) contentMap.get( "modelId" ) );
            searchIndexState.setSiteId( ( Long ) contentMap.get( "siteId" ) );

            searchService.addIndexContentState( searchIndexState );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }
    }

    /**
     * 为静态发布工作批量获取模型内容,所有相关值(主数据和定义数据以及辅助数据)必须全部获取
     * 
     * @param classId
     * @param modelId
     * @param orderIdFlag
     * @param limitCount
     * @return
     */
    public List retrieveNeedPublishContentByClassIDAndModelIdAndFlag( Long classId, Long modelId,
        Double orderIdFlag, Integer limitCount, Timestamp startAddDate, Timestamp endAddDate )
    {
        List result = null;

        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );

        if( modelBean == null )
        {
            return Collections.EMPTY_LIST;
        }

        ModelPersistenceMySqlCodeBean sqlBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelId );

        if( Constant.METADATA.MODEL_RES_ARTICLE.equals( modelBean.getModelResType() ) )
        {
            if( startAddDate == null && endAddDate == null )
            {
                result = contentDao
                    .queryNeedPublishContentAndPageContentByClassIDAndModelIdAndFlag( classId,
                        modelBean, sqlBean, orderIdFlag, limitCount );
            }
            else
            {
                result = contentDao
                    .queryNeedPublishContentAndPageContentByClassIDAndModelIdAndFlag( classId,
                        modelBean, sqlBean, orderIdFlag, startAddDate, endAddDate, limitCount );
            }
        }
        else
        {
            if( startAddDate == null && endAddDate == null )
            {
                result = contentDao.queryNeedPublishContentByClassIDAndModelIdAndFlag( classId,
                    modelBean, sqlBean, orderIdFlag, limitCount );
            }
            else
            {
                result = contentDao.queryNeedPublishContentByClassIDAndModelIdAndFlag( classId,
                    modelBean, sqlBean, orderIdFlag, startAddDate, endAddDate, limitCount );
            }
        }

        return result;
    }

    public List retrieveWaitPublishContentBySiteId( Long siteId, Timestamp currTime )
    {
        return contentDao.querySiteWaitPublishContentMainInfo( siteId, currTime );
    }

    public List retrieveWithdrawContentBySiteId( Long siteId, Timestamp currTime )
    {
        return contentDao.querySiteWithdrawContentMainInfo( siteId, currTime );
    }

    public void addWaitPublishIdTemp( Long contentId, Double orderIdFlag, Long classId )
    {
        contentDao.saveWaitPublishIdTemp( contentId, orderIdFlag, classId );
    }

    public void deleteWaitPublishIdTemp()
    {
        contentDao.deleteWaitPublishIdTemp();
    }

    public List retrieveWaitPublishContentBySiteIdAndCurrentDate( Long classId, Long modelId,
        Double orderIdFlag, Integer limitCount )
    {
        List result = null;

        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );

        if( modelBean == null )
        {
            return Collections.EMPTY_LIST;
        }

        ModelPersistenceMySqlCodeBean sqlBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelId );

        if( Constant.METADATA.MODEL_RES_ARTICLE.equals( modelBean.getModelResType() ) )
        {
            result = contentDao.queryWaitPublishArticlePageContentIdByCurrentDate( classId,
                modelBean, sqlBean, orderIdFlag, limitCount );

        }
        else
        {
            result = contentDao.queryWaitPublishContentIdByCurrentDate( classId, modelBean,
                sqlBean, orderIdFlag, limitCount );
        }

        return result;

    }

    public Long retrieveNeedPublishContentCountByClassIDAndModelIdAndFlag( Long classId,
        Long modelId, Double orderIdFlag, Timestamp startAddDate, Timestamp endAddDate )
    {
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );

        if( modelBean == null )
        {
            return Long.valueOf( 0 );
        }

        if( startAddDate == null && endAddDate == null )
        {
            return contentDao.queryNeedPublishContentCountByClassIDAndModelIdAndFlag( classId,
                modelBean, orderIdFlag );
        }
        else
        {
            return contentDao.queryNeedPublishContentCountByClassIDAndModelIdAndFlagAndAddDate(
                classId, modelBean, orderIdFlag, startAddDate, endAddDate );
        }
    }

    public Map retrieveSingleUserDefineContent( DataModelBean modelBean, Long id )
    {
        if( modelBean == null )
        {
            return Collections.EMPTY_MAP;
        }

        return contentDao.querySingleUserDefineContent( metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ), modelBean
            .getRelateTableName(), id, Integer.valueOf( 1 ) );
    }

    /**
     * 获取自定义模型的内容数据,不包含内容模型的maininfo,全通用
     * 
     * @param modelBean
     * @param id
     * @return
     */
    public Map retrieveSingleUserDefineContentOnlyModelData( Long modelId, Long id, String siteFlag )
    {
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );

        if( modelBean == null )
        {
            return Collections.EMPTY_MAP;
        }

        return contentDao.querySingleUserDefineContentOnlyModelData( metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ), modelBean
            .getRelateTableName(), id, siteFlag, modelId );
    }

    /**
     * 获取单一内容
     * 
     * @param modelId
     * @param id
     * @param pos
     * @return
     */
    public Map retrieveSingleUserDefineContent( Long id, Integer pos )
    {
        String key = "retrieveSingleUserDefineContent:" + id + "|" + pos;

        Map info = ( Map ) singleContentCache.getEntry( key );

        if( info == null )
        {
            DataModelBean modelBean = metaDataDao
                .querySingleDataModelBeanById( retrieveContentMainInfoModelIdByCid( id ) );

            if( modelBean == null )
            {
                info = Collections.EMPTY_MAP;
            }
            else
            {
                if( Constant.METADATA.MODEL_RES_ARTICLE.equals( modelBean.getModelResType() ) )
                {
                    info = contentDao.querySingleUserDefineContent( metaDataService
                        .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ),
                        modelBean.getRelateTableName(), id, pos );
                }
                else
                {
                    info = contentDao.querySingleUserDefineContent( metaDataService
                        .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ),
                        modelBean.getRelateTableName(), id );
                }
            }

            singleContentCache.putEntry( key, info );
        }

        return info;

    }

    public Map retrieveSingleNextOrPrevContentById( Double orderId, Long classId, Long modelId,
        String flag )
    {
        String key = "retrieveSingleNextOrPrevContentById:" + orderId + "|" + classId + "|"
            + modelId + "|" + flag;

        Map info = ( Map ) singleContentCache.getEntry( key );

        if( info == null )
        {

            if( "n".equals( flag ) )
            {
                info = contentDao.querySingleNextContentById( orderId, classId, modelId );
            }
            else if( "p".equals( flag ) )
            {
                info = contentDao.querySinglePrevContentById( orderId, classId, modelId );
            }

            singleContentCache.putEntry( key, info );
        }

        return info;
    }

    public Map retrieveSingleUserDefineContent( Long modelId, Long id )
    {
        DataModelBean modelBean = metaDataDao.querySingleDataModelBeanById( modelId );

        if( modelBean == null )
        {
            return Collections.EMPTY_MAP;
        }

        return contentDao.querySingleUserDefineContent( metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ), modelBean
            .getRelateTableName(), id );
    }

    public Map retrieveSingleUserDefineContentManageMode( Long modelId, Long id )
    {
        DataModelBean modelBean = metaDataDao.querySingleDataModelBeanById( modelId );

        if( modelBean == null )
        {
            return Collections.EMPTY_MAP;
        }

        String key = "retrieveSingleUserDefineContentManageMode:" + modelId + "|" + id;

        Map info = ( Map ) singleContentCache.getEntry( key );

        if( info == null )
        {
            info = contentDao.querySingleUserDefineContentManageMode( metaDataService
                .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ), modelBean
                .getRelateTableName(), id );

            singleContentCache.putEntry( key, info );
        }

        return info;
    }

    public Map retrieveSingleUserDefineContentManageMode( String modelName, Long id )
    {
        DataModelBean modelBean = metaDataDao.querySingleDataModelBeanByName( modelName );

        if( modelBean == null )
        {
            return Collections.EMPTY_MAP;
        }

        return contentDao.querySingleUserDefineContentManageMode( metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ), modelBean
            .getRelateTableName(), id );
    }

    public boolean checkContentTitleExist( Long siteId, String title )
    {
        if( title == null )
        {
            return false;
        }

        Long count = contentDao.queryCountForContentTitle( siteId, title.trim() );

        if( count.longValue() > 0 )
        {
            return true;
        }

        return false;
    }

    public String retrieveTextFieldVal( DataModelBean modelBean, ModelFiledInfoBean filedInfoBean,
        Long contentId )
    {
        if( modelBean == null )
        {
            return null;
        }

        return contentDao.queryTextColumnVal( modelBean, filedInfoBean, contentId );
    }

    public Map retrieveSingleUserDefineContent( String modelName, Long id, Integer pos )
    {

        String key = "retrieveSingleUserDefineContent:" + modelName + "|" + id + "|" + pos;

        Map info = ( Map ) singleContentCache.getEntry( key );

        if( info == null )
        {
            DataModelBean modelBean = metaDataDao.querySingleDataModelBeanByName( modelName );

            if( modelBean == null )
            {
                info = Collections.EMPTY_MAP;
            }
            else
            {
                if( Constant.METADATA.MODEL_RES_ARTICLE.equals( modelBean.getModelResType() ) )
                {
                    info = contentDao.querySingleUserDefineContent( metaDataService
                        .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ),
                        modelBean.getRelateTableName(), id, pos );
                }
                else
                {
                    info = contentDao.querySingleUserDefineContent( metaDataService
                        .retrieveSingleModelPerMysqlCodeBean( modelBean.getDataModelId() ),
                        modelBean.getRelateTableName(), id, Integer.valueOf( 1 ) );
                }
            }

            singleContentCache.putEntry( key, info );
        }

        return info;
    }

    /**
     * 将指定ID的自定义模型数据删除到trash中
     * 
     * @param modelId
     * @param contentId
     */
    public void deleteSystemAndUserDefineContentToTrash( SiteGroupBean site, List idList,
        List memberList )
    {
        if( idList == null )
        {
            return;
        }

        Auth auth = SecuritySessionKeeper.getSecuritySession().getAuth();

        try
        {
            mysqlEngine.beginTransaction();

            Long contentId = null;

            Integer censor = null;

            String creator = null;

            for ( int i = 0; i < idList.size(); i++ )
            {

                if( idList.get( i ) instanceof Long )
                {
                    contentId = ( Long ) idList.get( i );
                }
                else
                {
                    contentId = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ),
                        -1 ) );
                }

                if( contentId.longValue() < 0 )
                {
                    continue;
                }

                Map mainInfo = contentDao.querySingleContentMainInfo( contentId );

                if( mainInfo.isEmpty() )
                {
                    contentDao.deleteTrashContentMainInfo( ( Long ) mainInfo.get( "contentId" ) );
                }

                creator = ( String ) mainInfo.get( "creator" );

                censor = ( Integer ) mainInfo.get( "censorState" );

                if( Constant.WORKFLOW.CENSOR_STATUS_DRAFT.equals( censor ) )
                {

                    if( auth != null && !creator.equals( auth.getApellation() ) )
                    {
                        if( !"001".equals( auth.getOrgCode() ) )
                        {
                            continue;
                        }
                    }
                }

                Map exist = contentDao.querySingleTrashContentByContentId( contentId );

                if( !exist.isEmpty() )
                {
                    continue;
                }

                Map rt = StatService.getInstance().getContentResTrace( contentId );

                if( !rt.isEmpty() )
                {

                    int pc = -1;

                    Integer ic = ( Integer ) rt.get( "imgCount" );

                    Integer iv = ( Integer ) rt.get( "videoCount" );

                    if( Constant.COMMON.OFF.equals( rt.get( "isPub" ) ) )
                    {
                        pc = 0;

                        ic = 0;

                        iv = 0;
                    }

                    StatService.getInstance().collAndAnalysisContentStat( false, null, contentId,
                        null, ( Long ) mainInfo.get( "siteId" ),
                        ( Long ) mainInfo.get( "classId" ), -9999, pc, -ic, -iv );

                }

                contentDao.transferCotentToTrash( contentId );

                contentDao.deleteContentMainInfo( contentId );

                SearchIndexContentState searchIndexState = new SearchIndexContentState();

                searchIndexState.setContentId( contentId );

                searchIndexState.setSiteId( ( Long ) mainInfo.get( "siteId" ) );

                searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_DEL );

                searchService.addIndexContentState( searchIndexState );

                if( Constant.COMMON.ON.equals( mainInfo.get( "otherFlag" ) ) )
                {
                    memberList.add( mainInfo.get( "creator" ) );

                }

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            ContentDao.releaseAllCountCache();

            releaseContentCache();
        }

    }

    public void recoverAllSystemAndUserDefineContent( Long classId )
    {

        Long prevCid = Long.valueOf( Constant.CONTENT.MAX_ID_FLAG );

        Long modelId = null;

        ContentClassBean classBean = channelDao.querySingleClassBeanInfoByClassId( classId );

        if( classId != null )
        {
            modelId = classBean.getContentType();
        }

        List needRecoverContentList = contentDao.queryTrashContentIdByClassIdAndModelId( classId,
            modelId, prevCid, DELETE_QUERY_COUNT );

        while ( !needRecoverContentList.isEmpty() )
        {
            prevCid = ( Long ) needRecoverContentList.get( needRecoverContentList.size() - 1 );

            recoverContentForTrash( needRecoverContentList );

            recoverContentForTrash( retrieveTrashLinkInfo( needRecoverContentList ) );

            needRecoverContentList = contentDao.queryTrashContentIdByClassIdAndModelId( classId,
                modelId, prevCid, DELETE_QUERY_COUNT );
        }
    }

    public void recoverContentForTrash( List idList )
    {
        if( idList == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Long contentId = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                if( idList.get( i ) instanceof String )
                {
                    contentId = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ),
                        -1 ) );
                }
                else
                {
                    contentId = ( Long ) idList.get( i );
                }

                if( contentId.longValue() < 0 )
                {
                    continue;
                }

                Map info = contentDao.querySingleTrashContentByContentId( contentId );

                if( info.isEmpty() )
                {
                    continue;
                }

                contentDao.transferTrashToCotent( contentId );

                contentDao.deleteTrashContentMainInfo( contentId );

                Map rt = StatService.getInstance().getContentResTrace( contentId );

                if( !rt.isEmpty() )
                {
                    Integer ic = ( Integer ) rt.get( "imgCount" );

                    Integer iv = ( Integer ) rt.get( "videoCount" );

                    int pc = 1;

                    if( Constant.COMMON.OFF.equals( rt.get( "isPub" ) ) )
                    {
                        pc = 0;

                        ic = 0;

                        iv = 0;
                    }

                    StatService.getInstance().collAndAnalysisContentStat( false, null, contentId,
                        null, ( Long ) info.get( "siteId" ), ( Long ) info.get( "classId" ), 9999,
                        pc, ic, iv );
                }

                SearchIndexContentState searchIndexState = new SearchIndexContentState();

                searchIndexState.setClassId( ( Long ) info.get( "classId" ) );
                searchIndexState.setContentId( contentId );

                searchIndexState.setCensor( ( Integer ) info.get( "censorState" ) );
                searchIndexState.setBoost( ( Float ) info.get( "boost" ) );
                searchIndexState.setIndexDate( ( Date ) info.get( "addTime" ) );
                searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_ADD );

                searchIndexState.setModelId( ( Long ) info.get( "modelId" ) );
                searchIndexState.setSiteId( ( Long ) info.get( "siteId" ) );

                searchService.addIndexContentState( searchIndexState );
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            ContentDao.releaseAllCountCache();
            releaseContentCache();
        }

    }

    public List retrieveAllTrashContentByclassId( Long classId )
    {
        return contentDao.queryAllTrashContentByclassId( classId );
    }

    public void deleteAllSystemAndUserDefineContentToTrash( SiteGroupBean site, Long modelId,
        Long classId, List memberList )
    {

        Long prevCid = Long.valueOf( Constant.CONTENT.MAX_ID_FLAG );

        if( modelId == null || modelId.longValue() < 0 )
        {
            ContentClassBean classBean = channelDao.querySingleClassBeanInfoByClassId( classId );

            if( classId != null )
            {
                modelId = classBean.getContentType();
            }
        }

        List needDeleteContentList = contentDao.queryMainContentIdByClassIdAndModelId( classId,
            modelId, prevCid, DELETE_QUERY_COUNT );

        while ( !needDeleteContentList.isEmpty() )
        {
            prevCid = ( Long ) needDeleteContentList.get( needDeleteContentList.size() - 1 );

            deleteSystemAndUserDefineContentToTrash( site, needDeleteContentList, memberList );

            deleteSystemAndUserDefineContentToTrash( site,
                retrieveLinkInfo( needDeleteContentList ), memberList );

            needDeleteContentList = contentDao.queryMainContentIdByClassIdAndModelId( classId,
                modelId, prevCid, DELETE_QUERY_COUNT );
        }

    }

    public void deleteAllSystemAndUserDefineContent( Long modelId, Long classId )
    {

        Long prevCid = Long.valueOf( Constant.CONTENT.MAX_ID_FLAG );

        if( modelId == null || modelId.longValue() < 0 )
        {
            ContentClassBean classBean = channelDao.querySingleClassBeanInfoByClassId( classId );

            if( classId != null )
            {
                modelId = classBean.getContentType();
            }
        }

        List needDeleteContentList = contentDao.queryTrashContentIdByClassIdAndModelId( classId,
            modelId, prevCid, DELETE_QUERY_COUNT );

        while ( !needDeleteContentList.isEmpty() )
        {
            prevCid = ( Long ) needDeleteContentList.get( needDeleteContentList.size() - 1 );

            deleteSystemAndUserDefineContent( needDeleteContentList );

            deleteSystemAndUserDefineContent( retrieveTrashLinkInfo( needDeleteContentList ) );

            needDeleteContentList = contentDao.queryTrashContentIdByClassIdAndModelId( classId,
                modelId, prevCid, DELETE_QUERY_COUNT );
        }
    }

    public void deleteAllDefFormContent( SiteGroupBean site, Long modelId )
    {

        Long prevCid = Long.valueOf( Constant.CONTENT.MAX_ID_FLAG );

        List needDeleteContentList = metaDataService.retrieveFormDataByIdTrace( modelId, Long
            .valueOf( Constant.CONTENT.MAX_ID_FLAG ), DELETE_QUERY_COUNT );

        while ( !needDeleteContentList.isEmpty() )
        {
            prevCid = ( Long ) ( ( Map ) needDeleteContentList
                .get( needDeleteContentList.size() - 1 ) ).get( "contentId" );

            deleteDefFormContent( site, needDeleteContentList, modelId );

            needDeleteContentList = metaDataService.retrieveFormDataByIdTrace( modelId, prevCid,
                DELETE_QUERY_COUNT );
        }
    }

    /**
     * (会员)删除指定ID的自定义模型数据,包括附带资源,只可删除草稿，发布后的内容由管理员维护，会员无权管理,需要检查内容是否由会员创建
     * 
     * @param site
     * @param modelId
     * @param idList
     */
    public void deleteSystemAndUserDefineContentForMember( List idList )
    {

        SecuritySession session = SecuritySessionKeeper.getSecuritySession();

        MemberBean memberUser = memberDao.querySingleMemberBeanById( ( Long ) session.getAuth()
            .getIdentity() );

        Long contentId = null;

        List memberContentIdList = new ArrayList();

        Map info = null;

        String creator = null;

        for ( int i = 0; i < idList.size(); i++ )
        {
            if( idList.get( i ) instanceof Long )
            {
                contentId = ( Long ) idList.get( i );
            }
            else
            {
                contentId = Long
                    .valueOf( StringUtil.getLongValue( ( String ) idList.get( i ), -1 ) );
            }

            if( contentId.longValue() < 0 )
            {
                continue;
            }

            info = contentDao.querySingleContentMainInfo( contentId );

            if( Constant.WORKFLOW.CENSOR_STATUS_DRAFT.equals( info.get( "censorState" ) ) )
            {
                creator = ( String ) info.get( "creator" );

                if( creator != null && creator.equals( memberUser.getMemberName() ) )
                {
                    memberContentIdList.add( contentId );
                }
            }

        }

        deleteSystemAndUserDefineContent( memberContentIdList );
    }

    /**
     * 删除指定ID的自定义模型数据,包括附带资源
     * 
     * @param modelId
     * @param contentId
     */
    public void deleteSystemAndUserDefineContent( List idList )
    {
        log.info( "[SERVICE:] deleteSystemAndUserDefineContent : 将删除ID为:" + idList + "的数据" );

        if( idList == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            ModelFiledInfoBean bean = null;

            Long contentId = null;

            Integer censor = null;

            String creator = null;

            Map main = null;

            Long modelId = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                if( idList.get( i ) instanceof Long )
                {
                    contentId = ( Long ) idList.get( i );
                }
                else
                {
                    contentId = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ),
                        -1 ) );
                }

                if( contentId.longValue() < 0 )
                {
                    continue;
                }

                main = contentDao.querySingleContentMainInfo( contentId );

                modelId = ( Long ) main.get( "modelId" );

                if( modelId == null )
                {
                    main = contentDao.querySingleTrashContentByContentId( contentId );

                    if( main.isEmpty() )
                    {
                        continue;
                    }

                    modelId = ( Long ) main.get( "modelId" );
                }

                if( main.isEmpty() )
                {
                    continue;
                }

                ModelPersistenceMySqlCodeBean sqlCodeBean = null;

                List modeFieldList = null;

                DataModelBean modelBean = null;

                if( modelId != null )
                {
                    sqlCodeBean = metaDataService.retrieveSingleModelPerMysqlCodeBean( modelId );

                    modeFieldList = metaDataService.retrieveModelFiledInfoBeanList( modelId );

                    modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );
                }

                Map mainAndDefInfo = null;

                if( modelBean == null )
                {
                    mainAndDefInfo = contentDao.querySingleTrashContentMainInfo( contentId );
                }
                else
                {
                    mainAndDefInfo = contentDao.querySingleTrashUserDefineContent( sqlCodeBean,
                        modelBean.getRelateTableName(), contentId );
                }

                creator = ( String ) mainAndDefInfo.get( "creator" );

                censor = ( Integer ) mainAndDefInfo.get( "censorState" );

                contentDao.deleteTrashContentMainInfo( contentId );

                contentDao.deleteContentMainInfo( contentId );

                StatService.getInstance().deleteContentResInfo( contentId );

                if( modelBean != null )
                {
                    contentDao.deleteUserDefineInfo( modelBean, contentId );
                }

                /**
                 * 删除主信息的引导图片信息
                 */
                resService.updateSiteResourceTraceUseStatus( Long.valueOf( StringUtil.getLongValue(
                    ( String ) mainAndDefInfo.get( "homeImageResId" ), -1 ) ), Constant.COMMON.OFF );

                resService.updateSiteResourceTraceUseStatus( Long.valueOf( StringUtil.getLongValue(
                    ( String ) mainAndDefInfo.get( "channelImageResId" ), -1 ) ),
                    Constant.COMMON.OFF );

                resService
                    .updateSiteResourceTraceUseStatus( Long.valueOf( StringUtil.getLongValue(
                        ( String ) mainAndDefInfo.get( "classImageResId" ), -1 ) ),
                        Constant.COMMON.OFF );

                resService.updateSiteResourceTraceUseStatus( Long.valueOf( StringUtil.getLongValue(
                    ( String ) mainAndDefInfo.get( "contentImageResId" ), -1 ) ),
                    Constant.COMMON.OFF );

                /**
                 * 删除资源文件以及信息 以下为资源类型字段
                 */
                if( modeFieldList != null )
                {
                    for ( int j = 0; j < modeFieldList.size(); j++ )
                    {
                        bean = ( ModelFiledInfoBean ) modeFieldList.get( j );

                        if( Constant.METADATA.UPLOAD_IMG == bean.getHtmlElementId().intValue() )
                        {
                            SiteResourceBean resBean = resService
                                .retrieveSingleResourceBeanByResId( Long.valueOf( StringUtil
                                    .getLongValue( ( String ) mainAndDefInfo.get( bean
                                        .getFieldSign()
                                        + "ResId" ), -1 ) ) );

                            if( resBean != null )
                            {
                                resService.updateSiteResourceTraceUseStatus( resBean.getResId(),
                                    Constant.COMMON.OFF );
                            }
                        }
                        else if( Constant.METADATA.UPLOAD_MEDIA == bean.getHtmlElementId()
                            .intValue() )
                        {
                            SiteResourceBean resBean = resService
                                .retrieveSingleResourceBeanByResId( Long.valueOf( StringUtil
                                    .getLongValue( ( String ) mainAndDefInfo.get( bean
                                        .getFieldSign()
                                        + "ResId" ), -1 ) ) );

                            if( resBean != null )
                            {
                                resService.updateSiteResourceTraceUseStatus( resBean.getResId(),
                                    Constant.COMMON.OFF );

                                String cover = StringUtil.isStringNull( resBean.getCover() ) ? ""
                                    : resBean.getCover();

                                if( StringUtil.isStringNotNull( resBean.getCover() ) )
                                {
                                    SiteResourceBean coverResBean = resService
                                        .retrieveSingleResourceBeanBySource( cover );

                                    if( coverResBean != null )
                                    {
                                        resService.updateSiteResourceTraceUseStatus( coverResBean
                                            .getResId(), Constant.COMMON.OFF );
                                    }
                                }
                            }
                        }
                        else if( Constant.METADATA.UPLOAD_FILE == bean.getHtmlElementId()
                            .intValue() )
                        {
                            SiteResourceBean resBean = resService
                                .retrieveSingleResourceBeanByResId( Long.valueOf( StringUtil
                                    .getLongValue( ( String ) mainAndDefInfo.get( bean
                                        .getFieldSign() ), -1 ) ) );

                            if( resBean != null )
                            {
                                resService.updateSiteResourceTraceUseStatus( resBean.getResId(),
                                    Constant.COMMON.OFF );
                            }
                        }
                        else if( Constant.METADATA.UPLOAD_IMG_GROUP == bean.getHtmlElementId()
                            .intValue() )
                        {
                            List imageGroupList = contentDao.queryGroupPhotoInfoByContentId(
                                contentId, Constant.METADATA.MODEL_TYPE_CONTENT, null, true );

                            Map imageInfo = null;

                            for ( int k = 0; k < imageGroupList.size(); k++ )
                            {
                                imageInfo = ( Map ) imageGroupList.get( k );

                                SiteResourceBean resBean = resService
                                    .retrieveSingleResourceBeanByResId( Long.valueOf( StringUtil
                                        .getLongValue( ( String ) imageInfo.get( "resId" ), -1 ) ) );

                                if( resBean != null )
                                {
                                    resService.updateSiteResourceTraceUseStatus(
                                        resBean.getResId(), Constant.COMMON.OFF );
                                }

                            }

                        }
                        else if( Constant.METADATA.EDITER == bean.getHtmlElementId().intValue() )
                        {
                            ServiceUtil.disposeTextHaveSiteResId( null, ( String ) mainAndDefInfo
                                .get( bean.getFieldSign() ), new HashSet(), contentId, true );
                        }
                    }
                }

                if( modelBean != null
                    && Constant.METADATA.MODEL_RES_ARTICLE.equals( modelBean.getModelResType() ) )
                {
                    contentDao.deleteContentAssistantPageInfoByContentId( contentId );
                }

                contentDao.deletePhotoGroupInfo( contentId, Constant.METADATA.MODEL_TYPE_CONTENT );

                SearchIndexContentState searchIndexState = new SearchIndexContentState();

                searchIndexState.setContentId( contentId );

                searchIndexState.setSiteId( ( Long ) main.get( "siteId" ) );

                searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_DEL );

                searchService.addIndexContentState( searchIndexState );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            ContentDao.releaseAllCountCache();
            releaseContentCache();
        }

    }

    /**
     * 删除指定ID的表单数据,包括附带资源
     * 
     * @param modelId
     * @param contentId
     */
    public void deleteDefFormContent( SiteGroupBean site, List idList, Long modelId )
    {
        log.info( "[SERVICE:] deleteDefFormContent : 将删除ID为:" + idList + "的数据" );

        if( idList == null )
        {
            return;
        }

        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );

        if( modelBean == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Long contentId = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                if( idList.get( i ) instanceof Long )
                {
                    contentId = ( Long ) idList.get( i );
                }
                else if( idList.get( i ) instanceof Map )
                {
                    contentId = ( Long ) ( ( Map ) idList.get( i ) ).get( "contentId" );
                }
                else
                {
                    contentId = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ),
                        -1 ) );
                }

                if( contentId.longValue() < 0 )
                {
                    continue;
                }

                /**
                 * 删除扩展数据
                 */
                metaDataService.deleteAndClearDefModelInfo( contentId, modelBean.getDataModelId(),
                    site.getSiteFlag() );

                metaDataDao.deleteFormDataMainById( contentId );

                contentDao.deleteUserDefineInfo( modelBean, contentId );

                contentDao.deletePhotoGroupInfo( contentId, Constant.METADATA.MODEL_TYPE_CONTENT );

                SearchIndexContentState searchIndexState = new SearchIndexContentState();

                searchIndexState.setContentId( contentId );

                searchIndexState.setSiteId( site.getSiteId() );

                searchIndexState.setEventFlag( Constant.JOB.SEARCH_INDEX_DEL );

                searchService.addIndexContentState( searchIndexState );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            MetaDataService.resetFormDataCache();
        }

    }

    public void sortContentAgTwo( Long targetId, Long nextId )
    {
        if( targetId == null || nextId == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Map targetContent = contentDao.querySingleContentMainInfo( targetId );
            Map nextContent = contentDao.querySingleContentMainInfo( nextId );

            Long tmpOrderId = null;

            Long targetOrderFlag = ( Long ) targetContent.get( "orderIdFlag" );
            Long nextOrderFlag = ( Long ) nextContent.get( "orderIdFlag" );

            String directFlag = null;

            if( targetContent.get( "classId" ).equals( nextContent.get( "classId" ) ) )
            {

                if( targetOrderFlag.longValue() > nextOrderFlag.longValue() )
                {
                    directFlag = "great";
                    Map lastFlagArticle = contentDao.queryBigestOrderArticleByNextId(
                        ( Long ) nextContent.get( "orderIdFlag" ), ( Long ) targetContent
                            .get( "classId" ), directFlag );

                    tmpOrderId = ( Long ) lastFlagArticle.get( "orderIdFlag" );
                }
                else
                {

                    tmpOrderId = nextOrderFlag;
                }

                List sortInfoList = contentDao.queryContentOrderInfo( targetOrderFlag,
                    nextOrderFlag, ( Long ) targetContent.get( "classId" ), directFlag );

                Map info;
                Long id;
                Long nextOrderId;

                mysqlEngine.startBatch();
                for ( int i = 0; i < sortInfoList.size(); i++ )
                {
                    if( i + 1 == sortInfoList.size() )
                    {
                        break;
                    }
                    info = ( Map ) sortInfoList.get( i );
                    id = ( Long ) info.get( "contentId" );

                    info = ( Map ) sortInfoList.get( i + 1 );
                    nextOrderId = ( Long ) info.get( "orderIdFlag" );

                    contentDao.updateContentOrderId( id, nextOrderId );
                }

                contentDao.updateContentOrderId( ( Long ) targetContent.get( "contentId" ),
                    tmpOrderId );

                mysqlEngine.executeBatch();

            }
            else
            {
                log.warn( "当前参与排序的内容不属于同一栏目!" );
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }

    }

    /**
     * 对指定的内容进行排序，与栏目和内容模型无关
     * 
     * @param targetId
     * @param nextId
     */
    public void sortContent( Long targetId, Long nextId )
    {
        if( targetId == null || nextId == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Map targetContent = contentDao.querySingleContentMainInfo( targetId );
            Map nextContent = contentDao.querySingleContentMainInfo( nextId );

            Double nextOrderFlag = ( Double ) nextContent.get( "orderIdFlag" );

            Double newTargetOrderFlag = null;

            Map bigestContent = contentDao.queryBigestOrderContentByNextId( nextOrderFlag );

            newTargetOrderFlag = Double.valueOf( MathUtil.add( nextOrderFlag.doubleValue(),
                Constant.CONTENT.OREDER_UNIT ) );

            if( !bigestContent.isEmpty()
                && !( ( ( Integer ) bigestContent.get( "isSystemOrder" ) ).intValue() == Constant.CONTENT.IS_SYS_ORDER && ( ( Integer ) nextContent
                    .get( "isSystemOrder" ) ).intValue() == Constant.CONTENT.IS_SYS_ORDER ) )
            {
                List allBigestContent = contentDao.queryAllNotSysBigestOrderArticleByNextId(
                    nextOrderFlag, Long.valueOf( nextOrderFlag.longValue() ) );

                mysqlEngine.startBatch();

                Map contentMainInfo = null;
                for ( int i = 0; i < allBigestContent.size(); i++ )
                {
                    contentMainInfo = ( Map ) allBigestContent.get( i );

                    contentDao.updateContentOrderIdAndFlag( ( Long ) contentMainInfo
                        .get( "contentId" ), Double.valueOf( MathUtil.add(
                        ( ( Double ) contentMainInfo.get( "orderIdFlag" ) ).doubleValue(),
                        Constant.CONTENT.OREDER_UNIT ) ), Integer
                        .valueOf( Constant.CONTENT.NOT_SYS_ORDER ) );
                }
                mysqlEngine.executeBatch();
            }

            contentDao.updateContentOrderIdAndFlag( ( Long ) targetContent.get( "contentId" ),
                newTargetOrderFlag, Integer.valueOf( Constant.CONTENT.NOT_SYS_ORDER ) );

            if( !nextContent.get( "topFlag" ).equals( targetContent.get( "topFlag" ) ) )
            {
                contentDao.updateContentTopFlag( ( Long ) targetContent.get( "contentId" ),
                    ( Integer ) nextContent.get( "topFlag" ) );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            ContentDao.releaseAllCountCache();
            releaseContentCache();
        }

    }

    /**
     * 批量更新模型数据静态化后的结果
     * 
     * @param endStaticURLInfoList
     */
    public void setContentStaticPageURL( List endStaticURLInfoList, SiteGroupBean site )
    {
        Object[] info = null;

        Long contentId = null;

        String endStaticClassFilePath = null;

        boolean isUpdate = false;

        try
        {
            mysqlEngine.beginTransaction();

            mysqlEngine.startBatch();

            for ( int endI = 0; endI < endStaticURLInfoList.size(); endI++ )
            {
                info = ( Object[] ) endStaticURLInfoList.get( endI );

                endStaticClassFilePath = ( String ) info[0];

                contentId = ( Long ) info[1];

                if( endStaticClassFilePath == null )
                {
                    continue;
                }

                ContentMainInfoBean mainInfo = contentDao
                    .querySingleContentMainInfoBean( contentId );

                if( mainInfo != null
                    && !endStaticClassFilePath.equals( mainInfo.getStaticPageUrl() ) )
                {
                    contentDao.updateContentStaticPageURL( endStaticClassFilePath, contentId );

                    isUpdate = true;
                }
            }

            mysqlEngine.executeBatch();

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            if( isUpdate )
            {
                ContentDao.releaseAllCountCache();

                releaseContentCache();
            }
        }

    }

    /**
     * 批量更新模型数据分页静态化后的结果
     * 
     * @param endStaticURLInfoList
     */
    public void setPageContentStaticURL( List endStaticURLInfoList )
    {

        Long contentId = null;

        String endStaticClassFilePath = null;

        Integer pagePos = null;

        boolean isUpdate = false;

        try
        {
            mysqlEngine.beginTransaction();

            mysqlEngine.startBatch();

            Object[] info = null;

            for ( int endI = 0; endI < endStaticURLInfoList.size(); endI++ )
            {
                info = ( Object[] ) endStaticURLInfoList.get( endI );

                endStaticClassFilePath = ( String ) info[0];

                contentId = ( Long ) info[1];

                pagePos = ( Integer ) info[2];

                if( endStaticClassFilePath == null )
                {
                    continue;
                }

                Map mainPageInfo = contentDao.queryContentAssistantPageInfoBeanByContentIdDataMode(
                    contentId, pagePos );

                if( mainPageInfo == null )
                {
                    continue;
                }

                if( !endStaticClassFilePath.equals( ( String ) mainPageInfo.get( "pageStaticUrl" ) ) )
                {
                    contentDao.updatePageContentStaticURL( endStaticClassFilePath, contentId,
                        pagePos );

                    isUpdate = true;
                }

            }

            mysqlEngine.executeBatch();

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            if( isUpdate )
            {
                ContentDao.releaseAllCountCache();

                releaseContentCache();
            }
        }

    }

    public Integer retrieveTrashContentCountByClassId( Long classId, Long modelId )
    {
        return contentDao.queryTrashContentCountByClassId( classId, modelId );
    }

    public List retrieveTrashContentByClassId( Long classId, Long modelId, Long startPos,
        Integer size )
    {
        return contentDao.queryTrashContentByClassId( classId, modelId, startPos, size );
    }

    public List retrieveTrashContentByTitleKey( String key, Long classId )
    {
        return contentDao.queryTrashContentByTitleKey( key, classId );
    }

    /**
     * 从web获取内容系统定义参数,普通edit模式不更新系统参数
     * 
     * @param editMode 若为edit模式,ID将放在返回List的最后位置
     * @param modelId
     * @param classBean
     * @param params
     * @return
     */
    private List getSystemModelValueFromWebParamOld( boolean editMode, Long modelId,
        ContentClassBean classBean, Map params, Map workValue )
    {
        List values = new ArrayList();

        /**
         * 必须严格按照系统字段顺序获取和合理处理值!!!
         */

        values.add( params.get( "classId" ) );
        values.add( params.get( "title" ) );
        Auth sysAuth = SecuritySessionKeeper.getSecuritySession().getAuth();
        if( sysAuth == null )
        {
            values.add( "匿名用户" );
        }
        else
        {
            values.add( ( String ) sysAuth.getApellation() );
        }

        values.add( params.get( "author" ) );

        if( !editMode )
        {
            values.add( new Timestamp( DateAndTimeUtil.clusterTimeMillis() ) );
        }

        values.add( Long.valueOf( 0 ) );

        values.add( DateAndTimeUtil.getNotNullTimestamp( null, DateAndTimeUtil.DEAULT_FORMAT_NANO )
            .toString() );

        values.add( "" );

        values.add( classBean.getContentProduceType() );

        if( !editMode )
        {
            values.add( Long.valueOf( 1 ) );
        }

        values.add( params.get( "tagKey" ) );

        String appearStartDateTime = ( String ) params.get( "appearStartDateTime" );
        Timestamp appearStartDateTS = null;
        if( StringUtil.isStringNull( appearStartDateTime ) )
        {
            values.add( Constant.CONTENT.MIN_DATE );
            appearStartDateTS = Constant.CONTENT.MIN_DATE;
        }
        else
        {
            appearStartDateTS = DateAndTimeUtil.getNotNullTimestamp( appearStartDateTime,
                DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );
            values.add( appearStartDateTS );
        }

        workValue.put( "appearStartDateTS", appearStartDateTS );

        String appearEndDateTime = ( String ) params.get( "appearEndDateTime" );
        Timestamp appearEndDateTS = null;
        if( StringUtil.isStringNull( appearEndDateTime ) )
        {
            values.add( Constant.CONTENT.MAX_DATE );
            appearEndDateTS = Constant.CONTENT.MAX_DATE;
        }
        else
        {
            appearEndDateTS = DateAndTimeUtil.getNotNullTimestamp( appearEndDateTime,
                DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );
            values.add( appearEndDateTS );
        }

        workValue.put( "appearEndDateTS", appearEndDateTS );

        values.add( pendingCensorStateByStartAndEndPublishDate( appearStartDateTS, appearEndDateTS,
            Constant.WORKFLOW.CENSOR_STATUS_SUCCESS ) );

        String topFlag = ( String ) params.get( "topFlag" );
        if( Constant.COMMON.FLAG_IN_VAL.equals( topFlag ) )
        {
            values.add( Integer.valueOf( 1 ) );
        }
        else
        {
            values.add( Integer.valueOf( 0 ) );
        }

        String commendFlag = ( String ) params.get( "commendFlag" );
        if( Constant.COMMON.FLAG_IN_VAL.equals( commendFlag ) )
        {
            values.add( Integer.valueOf( 1 ) );
        }
        else
        {
            values.add( Integer.valueOf( 0 ) );
        }

        String hotFlag = ( String ) params.get( "hotFlag" );
        if( Constant.COMMON.FLAG_IN_VAL.equals( hotFlag ) )
        {
            values.add( Integer.valueOf( 1 ) );
        }
        else
        {
            values.add( Integer.valueOf( 0 ) );
        }

        String photoArticleType = ( String ) params.get( "photoArticleType" );
        if( Constant.COMMON.FLAG_IN_VAL.equals( photoArticleType ) )
        {
            values.add( Integer.valueOf( 1 ) );
        }
        else
        {
            values.add( Integer.valueOf( 0 ) );
        }

        String videoType = ( String ) params.get( "videoType" );
        if( Constant.COMMON.FLAG_IN_VAL.equals( videoType ) )
        {
            values.add( Integer.valueOf( 1 ) );
        }
        else
        {
            values.add( Integer.valueOf( 0 ) );
        }

        String attachType = ( String ) params.get( "attachType" );
        if( Constant.COMMON.FLAG_IN_VAL.equals( attachType ) )
        {
            values.add( Integer.valueOf( 1 ) );
        }
        else
        {
            values.add( Integer.valueOf( 0 ) );
        }

        if( !editMode )
        {
            values.add( Integer.valueOf( 1 ) );
        }

        String homePage = ( String ) params.get( "homeImage" );

        if( StringUtil.isStringNotNull( homePage ) )
        {
            values.add( homePage );

            values.add( Integer.valueOf( 1 ) );
        }
        else
        {
            values.add( homePage );
            values.add( Integer.valueOf( 0 ) );
        }

        values.add( modelId );

        if( editMode )
        {
            values.add( Long.valueOf( StringUtil.getLongValue(
                ( String ) params.get( "contentId" ), -1 ) ) );
        }

        return values;

    }

    /**
     * 从web获取内容系统定义参数,普通edit模式不更新系统参数
     * 
     * @param editMode
     * @param modelId
     * @param classBean
     * @param params
     * @return
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private ContentMainInfo getSystemModelValueFromWebParam( boolean editMode,
        ContentClassBean classBean, Map params, Map workValue, String mainEditorSign,
        SiteGroupBean site )
    {
        ContentMainInfo mainInfo = new ContentMainInfo();

        mainInfo.setClassId( classBean.getClassId() );

        long refCid = StringUtil.getLongValue( ( String ) params.get( "refCid" ), -1 );

        if( refCid > 0 )
        {
            mainInfo.setRefCid( Long.valueOf( refCid ) );
        }

        mainInfo.setTitle( ( String ) params.get( "title" ) );

        mainInfo.setSimpleTitle( ( String ) params.get( "simpleTitle" ) );

        mainInfo.setShortTitle( ( String ) params.get( "shortTitle" ) );

        mainInfo.setTitleStyle( ( String ) params.get( "titleStyle" ) );

        mainInfo.setSimpleTitleStyle( ( String ) params.get( "simpleTitleStyle" ) );

        mainInfo.setAddTime( DateAndTimeUtil.getNotNullTimestamp(
            ( String ) params.get( "addTime" ), DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS ) );

        mainInfo.setBoost( StringUtil.getFloatValue( ( String ) params.get( "boost" ), 1.0f ) );

        if( !editMode )
        {
            String summaryExist = ( String ) params.get( "summary" );

            if( StringUtil.isStringNotNull( summaryExist ) )
            {
                mainInfo.setSummary( summaryExist );
            }
            else
            {
                if( StringUtil.isStringNotNull( ( String ) params.get( mainEditorSign ) ) )
                {
                    String text = Jsoup.clean( ( String ) params.get( mainEditorSign ), Whitelist
                        .none() );

                    text = StringUtil.replaceString( text, " ", "", false, false );

                    text = StringUtil.replaceString( text, "&nbsp;", "", false, false );

                    if( StringUtil.isStringNotNull( text ) )
                    {
                        if( ( text.length() + 1 <= site.getSummaryLength().intValue() )
                            && text.length() < 200 )
                        {
                            mainInfo.setSummary( text );
                        }
                        else
                        {
                            mainInfo.setSummary( StringUtil.subString( text, 0, site
                                .getSummaryLength().intValue() <= 200 ? site.getSummaryLength()
                                .intValue() : 200 ) );
                        }
                    }
                }
            }

        }
        else
        {
            mainInfo.setSummary( ( String ) params.get( "summary" ) );
        }

        mainInfo.setOutLink( ( String ) params.get( "outLink" ) );

        String creator = ( String ) params.get( "creator" );
        if( StringUtil.isStringNull( creator ) )
        {
            Auth sysAuth = SecuritySessionKeeper.getSecuritySession().getAuth();
            if( sysAuth == null )
            {
                mainInfo.setCreator( "匿名用户" );
            }
            else
            {
                mainInfo.setCreator( ( String ) sysAuth.getApellation() );
                mainInfo.setOrgCode( ( String ) sysAuth.getOrgCode() );
            }
        }
        else
        {
            mainInfo.setCreator( creator );

            if( SecuritySessionKeeper.getSecuritySession() != null
                && SecuritySessionKeeper.getSecuritySession().getAuth() != null )
            {

                mainInfo.setOrgCode( ( String ) SecuritySessionKeeper.getSecuritySession()
                    .getAuth().getOrgCode() );
            }
        }

        mainInfo.setAuthor( ( String ) params.get( "author" ) );

        mainInfo.setSystemHandleTime( DateAndTimeUtil.getNotNullTimestamp( null,
            DateAndTimeUtil.DEAULT_FORMAT_NANO ).toString() );

        if( !editMode )
        {
            mainInfo.setProduceType( classBean.getContentProduceType() );

            mainInfo.setOrderIdFlag( Double.valueOf( 1 ) );

            mainInfo.setIsSystemOrder( Integer.valueOf( 1 ) );

            Integer of = ( Integer ) params.get( "otherFlag" );

            if( Constant.COMMON.ON.equals( of ) )
            {
                mainInfo.setOtherFlag( of );
            }
            else
            {
                mainInfo.setOtherFlag( Constant.COMMON.OFF );
            }
        }

        mainInfo.setTagKey( ( String ) params.get( "tagKey" ) );

        mainInfo.setKeywords( ( String ) params.get( "keywords" ) );

        mainInfo.setRelateIds( ( String ) params.get( "relateIds" ) );

        mainInfo.setRelateSurvey( ( String ) params.get( "relateSurvey" ) );

        if( !editMode && site.getDefClickCount().intValue() > 0 )
        {
            mainInfo.setClickCount( Long.valueOf( new Random().nextInt( site.getDefClickCount()
                .intValue() ) + 1 ) );
        }

        String appearStartDateTime = ( String ) params.get( "appearStartDateTime" );

        Timestamp appearStartDateTS = null;

        String contentAddStatus = ( String ) params.get( "contentAddStatus" );

        if( Constant.WORKFLOW.DRAFT.equals( contentAddStatus ) )
        {
            appearStartDateTS = DateAndTimeUtil.getTodayTimestampDayAndTime();
        }
        else
        {

            if( StringUtil.isStringNull( appearStartDateTime ) )
            {
                if( !editMode )
                {

                    appearStartDateTS = DateAndTimeUtil.getTodayTimestampDayAndTime();

                    if( !Constant.WORKFLOW.DRAFT.equals( contentAddStatus ) )
                    {
                        mainInfo.setPubDateSysDT( getNextPublishOrderTrace() );
                    }
                }
            }
            else
            {
                appearStartDateTS = DateAndTimeUtil.getNotNullTimestamp( appearStartDateTime,
                    DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );

                Timestamp currentTime = DateAndTimeUtil.getTodayTimestampDayAndTime();

                if( appearStartDateTS.compareTo( currentTime ) < 1 )
                {
                    if( !editMode )
                    {
                        appearStartDateTS = currentTime;

                        if( !Constant.WORKFLOW.DRAFT.equals( contentAddStatus ) )
                        {
                            mainInfo.setPubDateSysDT( getNextPublishOrderTrace() );
                        }
                    }
                    else
                    {
                        Integer prevCensorState = Integer.valueOf( ( String ) params
                            .get( "censorState" ) );
                        if( Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( prevCensorState )
                            || Constant.WORKFLOW.CENSOR_STATUS_IN_EDIT.equals( prevCensorState )
                            || Constant.WORKFLOW.CENSOR_STATUS_IN_FLOW.equals( prevCensorState ) )
                        {
                            appearStartDateTS = DateAndTimeUtil.getNotNullTimestamp(
                                ( String ) params.get( "cmsSysOldPublishDateTime" ),
                                DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );

                            if( StringUtil.isStringNull( ( String ) params
                                .get( "cmsSysOldPublishDT" ) ) )
                            {
                                mainInfo.setPubDateSysDT( getNextPublishOrderTrace() );

                                if( Constant.WORKFLOW.CENSOR_STATUS_IN_FLOW
                                    .equals( prevCensorState ) )
                                {
                                    mainInfo.setAppearStartDateTime( currentTime );
                                }

                            }
                            else
                            {
                                mainInfo.setPubDateSysDT( Long.valueOf( ( String ) params
                                    .get( "cmsSysOldPublishDT" ) ) );
                            }

                        }
                        else if( Constant.WORKFLOW.CENSOR_STATUS_WAIT_PUBLISH
                            .equals( prevCensorState )
                            || Constant.WORKFLOW.CENSOR_STATUS_DRAFT.equals( prevCensorState ) )
                        {
                            appearStartDateTS = currentTime;

                            mainInfo.setPubDateSysDT( getNextPublishOrderTrace() );
                        }
                    }
                }
                else
                {
                    mainInfo.setPubDateSysDT( null );
                }
            }

        }

        mainInfo.setAppearStartDateTime( appearStartDateTS );

        workValue.put( "appearStartDateTS", appearStartDateTS );

        String appearEndDateTime = ( String ) params.get( "appearEndDateTime" );
        Timestamp appearEndDateTS = null;
        if( StringUtil.isStringNull( appearEndDateTime ) )
        {
            appearEndDateTS = Constant.CONTENT.MAX_DATE;
        }
        else
        {
            appearEndDateTS = DateAndTimeUtil.getNotNullTimestamp( appearEndDateTime,
                DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );
        }

        mainInfo.setAppearEndDateTime( appearEndDateTS );

        workValue.put( "appearEndDateTS", appearEndDateTS );

        if( Constant.WORKFLOW.DRAFT.equals( contentAddStatus ) )
        {
            mainInfo.setCensorState( Constant.WORKFLOW.CENSOR_STATUS_DRAFT );
        }
        else
        {
            if( !editMode )
            {

                if( Constant.WORKFLOW.DRAFT.equals( contentAddStatus ) )
                {
                    mainInfo.setCensorState( Constant.WORKFLOW.CENSOR_STATUS_DRAFT );
                }
                else
                {
                    mainInfo
                        .setCensorState( pendingCensorStateByStartAndEndPublishDate(
                            appearStartDateTS, appearEndDateTS,
                            Constant.WORKFLOW.CENSOR_STATUS_SUCCESS ) );
                }
            }
            else
            {

                Map currentInfo = contentDao.querySingleContentMainInfo( Long
                    .valueOf( StringUtil.getLongValue( ( String ) params
                        .get( Constant.METADATA.CONTENT_ID_NAME ), -1 ) ) );

                Integer censorState = null;

                if( params.get( "censorState" ) == null )
                {
                    censorState = ( Integer ) currentInfo.get( "censorState" );
                }
                else
                {
                    censorState = Integer.valueOf( ( String ) params.get( "censorState" ) );
                }

                if( Constant.WORKFLOW.CENSOR_STATUS_SUCCESS.equals( censorState )
                    || Constant.WORKFLOW.CENSOR_STATUS_WAIT_PUBLISH.equals( censorState )
                    || Constant.WORKFLOW.CENSOR_STATUS_WITHDRAW.equals( censorState ) )
                {
                    mainInfo
                        .setCensorState( pendingCensorStateByStartAndEndPublishDate(
                            appearStartDateTS, appearEndDateTS,
                            Constant.WORKFLOW.CENSOR_STATUS_SUCCESS ) );
                }
                else
                {
                    mainInfo.setCensorState( censorState );
                }
            }
        }

        String topFlag = ( String ) params.get( "topFlag" );
        if( Constant.COMMON.FLAG_IN_VAL.equals( topFlag ) )
        {
            mainInfo.setTopFlag( Integer.valueOf( 1 ) );
        }
        else
        {
            mainInfo.setTopFlag( Integer.valueOf( 0 ) );
        }

        mainInfo.setTypeFlag( ( String ) params.get( "typeFlag" ) );
        // allowCommend
        String allowCommend = ( String ) params.get( "allowCommend" );

        if( !Constant.COMMON.FLAG_OUT_VAL.equals( allowCommend ) )
        {
            mainInfo.setAllowCommend( Integer.valueOf( 1 ) );
        }
        else
        {
            mainInfo.setAllowCommend( Integer.valueOf( 0 ) );
        }

        mainInfo.setEspecialTemplateUrl( ( String ) params.get( "especialTemplateUrl" ) );

        String homeImage = ( String ) params.get( "homeImage" );

        Long homeResId = Long.valueOf( StringUtil.getLongValue( homeImage, -1 ) );

        ServiceUtil.disposeOldImageInfo( homeResId, "homeImage", params );

        mainInfo.setHomeImage( ServiceUtil.disposeSingleImageInfo( homeResId ) );
        if( StringUtil.isStringNotNull( homeImage ) && !"-1".equals( homeImage ) )
        {
            mainInfo.setHomeImgFlag( Integer.valueOf( 1 ) );
        }
        else
        {
            mainInfo.setHomeImgFlag( Integer.valueOf( 0 ) );
        }

        String channelImage = ( String ) params.get( "channelImage" );

        Long chResId = Long.valueOf( StringUtil.getLongValue( channelImage, -1 ) );

        ServiceUtil.disposeOldImageInfo( chResId, "channelImage", params );

        mainInfo.setChannelImage( ServiceUtil.disposeSingleImageInfo( chResId ) );
        if( StringUtil.isStringNotNull( channelImage ) && !"-1".equals( channelImage ) )
        {
            mainInfo.setChannelImgFlag( Integer.valueOf( 1 ) );
        }
        else
        {
            mainInfo.setChannelImgFlag( Integer.valueOf( 0 ) );
        }

        String classImage = ( String ) params.get( "classImage" );

        Long csResId = Long.valueOf( StringUtil.getLongValue( classImage, -1 ) );

        ServiceUtil.disposeOldImageInfo( csResId, "classImage", params );

        mainInfo.setClassImage( ServiceUtil.disposeSingleImageInfo( csResId ) );
        if( StringUtil.isStringNotNull( classImage ) && !"-1".equals( classImage ) )
        {
            mainInfo.setClassImgFlag( Integer.valueOf( 1 ) );
        }
        else
        {
            mainInfo.setClassImgFlag( Integer.valueOf( 0 ) );
        }

        String contentImage = ( String ) params.get( "contentImage" );

        Long cnResId = Long.valueOf( StringUtil.getLongValue( contentImage, -1 ) );

        ServiceUtil.disposeOldImageInfo( cnResId, "contentImage", params );

        mainInfo.setContentImage( ServiceUtil.disposeSingleImageInfo( cnResId ) );

        if( StringUtil.isStringNotNull( contentImage ) && !"-1".equals( contentImage ) )
        {
            mainInfo.setContentImgFlag( Integer.valueOf( 1 ) );
        }
        else
        {
            mainInfo.setContentImgFlag( Integer.valueOf( 0 ) );
        }

        mainInfo.setCommendFlag( Integer.valueOf( StringUtil.getIntValue( ( String ) params
            .get( "commendFlag" ), 0 ) ) );

        mainInfo.setModelId( classBean.getContentType() );

        if( editMode )
        {
            mainInfo.setContentId( StringUtil.getLongValue( ( String ) params.get( "contentId" ),
                -1 ) );
        }
        else
        {

            mainInfo.setSiteId( site.getSiteId() );
        }

        return mainInfo;

    }

    private ContentMainInfo changeDbMapToNewCopyContentMainVo( Map infpMap,
        ContentClassBean classBean )
    {

        ContentMainInfo info = new ContentMainInfo();
        info.setContentId( ( Long ) infpMap.get( "contentId" ) );
        info.setModelId( ( Long ) infpMap.get( "modelId" ) );
        info.setClassId( classBean.getClassId() );
        info.setRefCid( ( Long ) infpMap.get( "contentId" ) );
        info.setTitle( ( String ) infpMap.get( "title" ) );
        info.setSimpleTitle( ( String ) infpMap.get( "simpleTitle" ) );
        info.setShortTitle( ( String ) infpMap.get( "shortTitle" ) );
        info.setTitleStyle( ( String ) infpMap.get( "titleStyle" ) );
        info.setSimpleTitleStyle( ( String ) infpMap.get( "simpleTitleStyle" ) );
        info.setAuthor( ( String ) infpMap.get( "author" ) );
        info.setCreator( ( String ) infpMap.get( "creator" ) );
        info.setOrgCode( ( String ) SecuritySessionKeeper.getSecuritySession().getAuth()
            .getOrgCode() );
        info.setSummary( ( String ) infpMap.get( "summary" ) );
        info.setOrderIdFlag( Double.valueOf( -1 ) );
        info.setBoost( ( Float ) infpMap.get( "boost" ) );
        info.setAddTime( ( Timestamp ) infpMap.get( "addTime" ) );

        info.setHomeImgFlag( ( Integer ) infpMap.get( "homeImgFlag" ) );
        info.setClassImgFlag( ( Integer ) infpMap.get( "classImgFlag" ) );
        info.setChannelImgFlag( ( Integer ) infpMap.get( "channelImgFlag" ) );
        info.setContentImgFlag( ( Integer ) infpMap.get( "contentImgFlag" ) );

        String day = DateAndTimeUtil.getCunrrentDayAndTime( DateAndTimeUtil.DEAULT_FORMAT_YMD );

        SiteGroupBean targetSite = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupFlagInfoCache
            .getEntry( classBean.getSiteFlag() );

        if( StringUtil.isStringNotNull( ( String ) infpMap.get( "homeImage" ) ) )
        {
            String resInfo = ServiceUtil.copyImageRes( ( String ) infpMap.get( "homeImage" ),
                targetSite, day, classBean.getClassId() );

            info.setHomeImage( resInfo );
        }

        if( StringUtil.isStringNotNull( ( String ) infpMap.get( "classImage" ) ) )
        {
            String resInfo = ServiceUtil.copyImageRes( ( String ) infpMap.get( "classImage" ),
                targetSite, day, classBean.getClassId() );

            info.setClassImage( resInfo );
        }

        if( StringUtil.isStringNotNull( ( String ) infpMap.get( "channelImage" ) ) )
        {
            String resInfo = ServiceUtil.copyImageRes( ( String ) infpMap.get( "channelImage" ),
                targetSite, day, classBean.getClassId() );

            info.setChannelImage( resInfo );
        }

        if( StringUtil.isStringNotNull( ( String ) infpMap.get( "contentImage" ) ) )
        {
            String resInfo = ServiceUtil.copyImageRes( ( String ) infpMap.get( "contentImage" ),
                targetSite, day, classBean.getClassId() );

            info.setContentImage( resInfo );
        }

        info.setSystemHandleTime( DateAndTimeUtil.getNotNullTimestamp( null,
            DateAndTimeUtil.DEAULT_FORMAT_NANO ).toString() );
        info.setEspecialTemplateUrl( ( String ) infpMap.get( "especialTemplateUrl" ) );
        info.setProduceType( classBean.getContentProduceType() );
        info.setCensorState( Constant.WORKFLOW.CENSOR_STATUS_IN_FLOW );
        info.setIsPageContent( ( Integer ) infpMap.get( "isPageContent" ) );
        info.setIsSystemOrder( ( Integer ) infpMap.get( "isSystemOrder" ) );
        info.setKeywords( ( String ) infpMap.get( "keywords" ) );
        info.setTagKey( ( String ) infpMap.get( "tagKey" ) );
        info.setRelateIds( ( String ) infpMap.get( "relateIds" ) );
        info.setRelateSurvey( ( String ) infpMap.get( "relateSurvey" ) );
        info.setAppearStartDateTime( DateAndTimeUtil.getTodayTimestampDayAndTime() );
        info.setAppearEndDateTime( Constant.CONTENT.MAX_DATE );

        info.setCommendFlag( ( Integer ) infpMap.get( "commendFlag" ) );

        info.setAllowCommend( classBean.getOpenComment() );
        info.setSiteId( ( Long ) infpMap.get( "siteId" ) );

        return info;
    }

    public void setContentTopFlag( List idList, String topFlag )
    {
        try
        {
            Long contentId = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                if( idList.get( i ) instanceof Long )
                {
                    contentId = ( Long ) idList.get( i );
                }
                else
                {
                    contentId = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ),
                        -1 ) );
                }

                if( contentId.longValue() < 0 )
                {
                    continue;
                }

                if( "up".equals( topFlag ) )
                {
                    contentDao.updateContentTopFlag( contentId, Constant.COMMON.ON );
                }
                else if( "down".equals( topFlag ) )
                {
                    contentDao.updateContentTopFlag( contentId, Constant.COMMON.OFF );
                }
            }
        }
        finally
        {
            ContentDao.releaseAllCountCache();

            releaseContentCache();
        }

    }

    public Long retrieveAllInWorkflowUserDefineContentCount( Auth auth )
    {
        if( auth == null || !auth.isAuthenticated() )
        {
            return Long.valueOf( 0 );
        }

        String sqlOrCond = auth.getRoleSqlHelper().getAllRoleOrQuery( "wa", "auditManId" );

        Long result = null;

        try
        {
            mysqlEngine.beginTransaction();

            result = contentDao.queryAllInWorkflowUserDefineContentCount( sqlOrCond, ( Long ) auth
                .getIdentity(), ( Long ) auth.getOrgIdentity() );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }

        return result;
    }

    /**
     * 获取指定内容的附属图集
     * 
     * @param contentId
     * @param siteBean
     * @return
     */
    public List retrieveGroupPhotoInfoByContentId( Long contentId, String group, Integer modelType,
        SiteGroupBean siteBean, boolean serverMode )
    {

        if( siteBean == null )
        {
            return new ArrayList( 1 );
        }

        String key = "retrieveGroupPhotoInfoByContentId:" + contentId + "|" + group + "|"
            + modelType + "|" + siteBean.getSiteId() + "|" + serverMode;

        List result = ( List ) singleContentCache.getEntry( key );

        if( result == null )
        {
            if( StringUtil.isStringNull( group ) )
            {
                result = contentDao.queryGroupPhotoInfoByContentId( contentId, modelType, siteBean,
                    serverMode );
            }
            else
            {
                result = contentDao.queryGroupPhotoInfoByContentId( contentId, group, modelType,
                    siteBean, serverMode );
            }

            singleContentCache.putEntry( key, result );
        }

        return result;
    }

    public List retrieveAllContentPageByIds( String query, Long modelId )
    {
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );

        if( modelBean == null || StringUtil.isStringNull( query ) )
        {
            return Collections.EMPTY_LIST;
        }

        ModelPersistenceMySqlCodeBean sqlBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelId );

        return contentDao.queryAllContentPageByIds( query, modelBean, sqlBean );
    }

    public List retrieveAllContentAssistantPageInfoByContentId( Long contentId, Map info,
        ContentClassBean classBean, SiteGroupBean site )
    {
        String key = "retrieveAllContentAssistantPageInfoByContentId:" + contentId;

        List resList = ( List ) singleContentCache.getEntry( key );

        if( resList == null )
        {
            resList = contentDao.queryContentAssistantPageInfoBeanNotIncludeTextByContentId(
                contentId, info, classBean, site );

            singleContentCache.putEntry( key, resList );
        }

        return resList;
    }

    /**
     * 推荐内容到指定推荐位
     * 
     * @param contentIdArrayList
     * @param idArrayList
     */
    public void commendContentInfo( List contentIdArrayList, List idArrayList, boolean isSpec )
    {
        if( contentIdArrayList == null || idArrayList == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Long contentId = null;

            Long commendTypeId = null;

            ContentClassBean classBean = null;

            Map mainInfo = null;

            ContentCommendTypeBean commTypeBean = null;

            ContentCommendPushInfo pushInfo = null;

            String siteFlag = ( ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                .getCurrentLoginSiteInfo() ).getSiteFlag();

            for ( int j = 0; j < idArrayList.size(); j++ )
            {
                if( idArrayList.get( j ) instanceof String )
                {
                    commendTypeId = Long.valueOf( StringUtil.getLongValue( ( String ) idArrayList
                        .get( j ), -1 ) );
                }
                else
                {
                    commTypeBean = ( ( ContentCommendTypeBean ) idArrayList.get( j ) );

                    commendTypeId = commTypeBean.getCommendTypeId();

                }

                if( commendTypeId.longValue() < 0 )
                {
                    continue;
                }

                commTypeBean = channelDao.querySingleContentCommendTypeBeanByTypeId( commendTypeId );

                if( commTypeBean == null )
                {
                    continue;
                }

                int num = 0;

                List existCommendInfoList = contentDao.queryAllCommendContentByCommendTypeByFlag(
                    commTypeBean.getCommFlag(), siteFlag, false );

                for ( int i = 0; i < contentIdArrayList.size(); i++ )
                {
                    contentId = Long.valueOf( StringUtil.getLongValue(
                        ( String ) contentIdArrayList.get( i ), -1 ) );

                    if( contentId.longValue() < 0 )
                    {
                        continue;
                    }

                    mainInfo = contentDao.querySingleContentMainInfo( contentId );

                    if( mainInfo == null )
                    {
                        continue;
                    }

                    classBean = channelService
                        .retrieveSingleClassBeanInfoByClassId( ( Long ) mainInfo.get( "classId" ) );

                    if( classBean == null || classBean.getClassId().longValue() < 0 )
                    {
                        continue;
                    }

                    if( Constant.COMMON.ON.equals( commTypeBean.getMustCensor() ) )
                    {

                        Long count = contentDao.queryCommendPushTempCount( contentId, commTypeBean
                            .getCommendTypeId() );

                        if( count.longValue() < 1 )
                        {
                            contentDao.saveCommendPushTemp( contentId, commTypeBean
                                .getCommendTypeId(), commTypeBean.getCommFlag() );

                        }
                    }
                    else
                    {

                        num++;

                        pushInfo = new ContentCommendPushInfo();

                        pushInfo.setAddTime( DateAndTimeUtil.getTodayTimestampDayAndTime() );
                        pushInfo.setClassId( classBean.getClassId() );
                        pushInfo.setCommendFlag( commTypeBean.getCommFlag() );
                        pushInfo.setCommendTypeId( commTypeBean.getCommendTypeId() );
                        pushInfo.setContentId( contentId );

                        pushInfo.setImg( ServiceUtil.disposeSingleImageInfo( Long
                            .valueOf( StringUtil.getLongValue( ( String ) mainInfo
                                .get( "contentImageSysReUrl" ), -1 ) ) ) );

                        pushInfo.setModelId( ( Long ) mainInfo.get( "modelId" ) );
                        pushInfo.setSummary( ( String ) mainInfo.get( "summary" ) );
                        pushInfo.setTitle( ( String ) mainInfo.get( "title" ) );

                        pushInfo.setUrl( Constant.CONTENT.GEN_CONTENT_URL_PREFIX
                            + mainInfo.get( "contentId" ) );

                        Auth sysAuth = SecuritySessionKeeper.getSecuritySession().getAuth();

                        pushInfo.setCommendMan( ( String ) sysAuth.getApellation() );

                        pushInfo.setSiteFlag( siteFlag );

                        pushInfo.setRowFlag( Long.valueOf( 0 ) );

                        pushInfo.setRowOrder( Integer.valueOf( 1 ) );

                        UpdateState us = contentDao.saveCommendContent( pushInfo );

                        if( us.haveKey() )
                        {
                            contentDao.updateCommendPushContentOrderInfoByInfoId( Long
                                .valueOf( num ), Integer.valueOf( 1 ), Long.valueOf( us.getKey() ) );

                            contentDao.updateCommendFlagByContentId( contentId, Constant.COMMON.ON );
                        }

                    }
                }

                if( num > 0 )
                {
                    ContentCommendPushInfoBean commInfoBean = null;

                    List rowBeanInnerList = null;

                    for ( int y = 0; y < existCommendInfoList.size(); y++ )
                    {
                        num++;

                        commInfoBean = ( ContentCommendPushInfoBean ) existCommendInfoList.get( y );

                        contentDao.updateCommendPushContentRowFlagByInfoId( Long.valueOf( num ),
                            commInfoBean.getInfoId() );

                        rowBeanInnerList = commInfoBean.getRowInfoList();

                        for ( int x = 0; x < rowBeanInnerList.size(); x++ )
                        {
                            commInfoBean = ( ContentCommendPushInfoBean ) rowBeanInnerList.get( x );
                            contentDao.updateCommendPushContentRowFlagByInfoId(
                                Long.valueOf( num ), commInfoBean.getInfoId() );
                        }
                    }
                }

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            releaseContentCache();
            ContentDao.releaseAllCountCache();
        }
    }

    public void addSingleCommendInfo( Map params, ContentCommendPushInfo commInfo, Long rowFlag,
        Integer rowIndex, boolean inCol, String siteFlag, String commendFlag )
    {
        if( commInfo == null || rowFlag == null || rowFlag == null || rowIndex == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            ContentCommendTypeBean commTypeBean = channelDao
                .querySingleContentCommendTypeBeanByTypeId( commInfo.getCommendTypeId() );

            if( commTypeBean == null )
            {
                return;
            }

            if( inCol )
            {
                Integer maxOrder = contentDao.queryCommendContentMaxRowOrderByRowFlag( rowFlag,
                    commInfo.getCommendFlag() );

                if( maxOrder == null )
                {
                    maxOrder = Integer.valueOf( 0 );
                }

                if( rowIndex.intValue() == -1 )
                {
                    rowIndex = Integer.valueOf( maxOrder.intValue() + 1 );
                }

                Integer pos = Integer.valueOf( ( maxOrder.intValue() + 2 ) - rowIndex.intValue() );

                commInfo.setRowFlag( rowFlag );
                commInfo.setRowOrder( pos );
                commInfo.setAddTime( DateAndTimeUtil.getTodayTimestampDayAndTime() );

                commInfo.setImg( ServiceUtil.disposeSingleImageInfo( Long.valueOf( StringUtil
                    .getLongValue( commInfo.getImg(), -1 ) ) ) );

                commInfo.setCommendFlag( commTypeBean.getCommFlag() );
                commInfo.setCommendTypeId( commTypeBean.getCommendTypeId() );

                UpdateState us = contentDao.saveCommendContent( commInfo );

                if( us.haveKey() && maxOrder.intValue() > 0 )
                {
                    commInfo.setInfoId( us.getKey() );

                    contentDao.updateCommendPushContentRowOrder( commInfo.getCommendFlag(),
                        rowFlag, pos, Long.valueOf( us.getKey() ) );

                    if( commInfo.getContentId() != null )
                    {
                        contentDao.updateCommendFlagByContentId( commInfo.getContentId(),
                            Constant.COMMON.ON );
                    }
                }
            }
            else
            {
                List rowBeanAllList = contentDao.queryAllCommendContentByCommendTypeByFlag(
                    commInfo.getCommendFlag(), siteFlag, false );

                commInfo.setRowFlag( rowFlag );
                commInfo.setRowOrder( Integer.valueOf( 1 ) );
                commInfo.setAddTime( DateAndTimeUtil.getTodayTimestampDayAndTime() );

                commInfo.setImg( ServiceUtil.disposeSingleImageInfo( Long.valueOf( StringUtil
                    .getLongValue( commInfo.getImg(), -1 ) ) ) );

                commInfo.setCommendFlag( commTypeBean.getCommFlag() );
                commInfo.setCommendTypeId( commTypeBean.getCommendTypeId() );

                UpdateState us = contentDao.saveCommendContent( commInfo );

                if( us.haveKey() )
                {
                    commInfo.setInfoId( us.getKey() );

                    if( commInfo.getContentId() != null )
                    {
                        contentDao.updateCommendFlagByContentId( commInfo.getContentId(),
                            Constant.COMMON.ON );
                    }

                    ContentCommendPushInfoBean rowInfoBean = null;
                    List rowBeanInnerList = null;
                    int num = 0;

                    for ( int i = 0; i < rowBeanAllList.size(); i++ )
                    {
                        num = i + 1;

                        rowInfoBean = ( ContentCommendPushInfoBean ) rowBeanAllList.get( i );

                        if( rowInfoBean.getRowFlag().intValue() < rowFlag.intValue() )
                        {
                            continue;
                        }

                        contentDao.updateCommendPushContentRowFlagByInfoId(
                            Long.valueOf( num + 1 ), rowInfoBean.getInfoId() );

                        rowBeanInnerList = rowInfoBean.getRowInfoList();
                        for ( int j = 0; j < rowBeanInnerList.size(); j++ )
                        {
                            rowInfoBean = ( ContentCommendPushInfoBean ) rowBeanInnerList.get( j );
                            contentDao.updateCommendPushContentRowFlagByInfoId( Long
                                .valueOf( num + 1 ), rowInfoBean.getInfoId() );
                        }
                    }
                }
            }

            if( "pushMode".equals( commendFlag ) )
            {
                contentDao.deleteCommendPushTemp( commInfo.getContentId(), commTypeBean
                    .getCommendTypeId() );
            }

            /**
             * 扩展模型数据
             */

            metaDataService.addOrEditDefModelInfo( params, commTypeBean.getModelId(), commInfo
                .getInfoId(), Constant.METADATA.MODEL_TYPE_COMMEND );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            releaseContentCache();
            ContentDao.releaseAllCountCache();

        }
    }

    public void addMutiCommendInfoForContent( String commFlag, List contentIdList,
        String commendMan, String siteFlag, String commendFlag, String action )
    {
        try
        {
            mysqlEngine.beginTransaction();

            ContentCommendTypeBean commTypeBean = channelDao
                .querySingleContentCommendTypeBeanByCommFlag( commFlag );

            if( commTypeBean == null )
            {
                return;
            }

            List contentMainInfoList = new ArrayList();

            Long contentId = null;
            for ( int i = 0; i < contentIdList.size(); i++ )
            {
                contentId = Long.valueOf( StringUtil.getLongValue(
                    ( String ) contentIdList.get( i ), -1 ) );

                if( contentId.longValue() < 0 )
                {
                    continue;
                }

                contentMainInfoList.add( contentDao.querySingleContentMainInfoBean( contentId ) );

                contentDao.updateCommendFlagByContentId( contentId, Constant.COMMON.ON );
            }

            List existCommendInfoList = contentDao.queryAllCommendContentByCommendTypeByFlag(
                commFlag, siteFlag, false );

            int num = 0;

            ContentCommendPushInfo commInfo = null;

            ContentMainInfoBean contentInfoBean = null;

            int firstAddNum = 0;

            for ( int i = 0; i < contentMainInfoList.size(); i++ )
            {
                num++;

                if( "line".equals( action ) && i == 0 )
                {
                    firstAddNum = num;
                }

                contentInfoBean = ( ContentMainInfoBean ) contentMainInfoList.get( i );

                commInfo = new ContentCommendPushInfo();

                if( "line".equals( action ) )
                {
                    commInfo.setRowFlag( Long.valueOf( firstAddNum ) );

                    commInfo.setRowOrder( Integer.valueOf( num ) );
                }
                else
                {
                    commInfo.setRowFlag( Long.valueOf( num ) );
                    commInfo.setRowOrder( Integer.valueOf( 1 ) );
                }

                commInfo.setAddTime( DateAndTimeUtil.getTodayTimestampDayAndTime() );

                commInfo.setContentId( contentInfoBean.getContentId() );
                commInfo.setClassId( contentInfoBean.getClassId() );
                commInfo.setModelId( contentInfoBean.getModelId() );
                commInfo.setTitle( contentInfoBean.getTitle() );
                commInfo.setSummary( contentInfoBean.getSummary() );
                commInfo.setUrl( Constant.CONTENT.GEN_CONTENT_URL_PREFIX
                    + contentInfoBean.getContentId() );

                commInfo.setImg( ServiceUtil.disposeSingleImageInfo( Long.valueOf( StringUtil
                    .getLongValue( contentInfoBean.getContentImage(), -1 ) ) ) );

                commInfo.setCommendFlag( commFlag );
                commInfo.setCommendTypeId( commTypeBean.getCommendTypeId() );
                commInfo.setCommendMan( commendMan );
                commInfo.setSiteFlag( siteFlag );

                contentDao.saveCommendContent( commInfo );

            }

            if( num == 0 )
            {
                return;
            }

            ContentCommendPushInfoBean commInfoBean = null;

            List rowBeanInnerList = null;

            if( "line".equals( action ) )
            {
                num = firstAddNum;
            }

            for ( int j = 0; j < existCommendInfoList.size(); j++ )
            {
                num++;

                commInfoBean = ( ContentCommendPushInfoBean ) existCommendInfoList.get( j );

                contentDao.updateCommendPushContentRowFlagByInfoId( Long.valueOf( num ),
                    commInfoBean.getInfoId() );

                rowBeanInnerList = commInfoBean.getRowInfoList();

                for ( int x = 0; x < rowBeanInnerList.size(); x++ )
                {
                    commInfoBean = ( ContentCommendPushInfoBean ) rowBeanInnerList.get( x );
                    contentDao.updateCommendPushContentRowFlagByInfoId( Long.valueOf( num ),
                        commInfoBean.getInfoId() );
                }
            }

            if( "pushMode".equals( commendFlag ) )
            {
                for ( int i = 0; i < contentIdList.size(); i++ )
                {
                    contentId = Long.valueOf( StringUtil.getLongValue( ( String ) contentIdList
                        .get( i ), -1 ) );

                    if( contentId.longValue() < 0 )
                    {
                        continue;
                    }

                    contentDao.deleteCommendPushTemp( contentId, commTypeBean.getCommendTypeId() );
                }
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            releaseContentCache();
            ContentDao.releaseAllCountCache();

        }
    }

    /**
     * tag查询编辑推荐信息
     * 
     * @param commTypeId
     * @return
     */
    public List getCommendContentTempQueryTag( String commTypeId )
    {
        List result = null;

        Long ctId = Long.valueOf( StringUtil.getLongValue( commTypeId, -1 ) );

        if( ctId.longValue() < 1 )
        {
            return result;
        }

        result = contentDao.queryCommendPushTemp( ctId );

        return result;
    }

    public void editSingleCommendInfo( Map params, ContentCommendPushInfo commInfo )
    {
        try
        {
            commInfo.setImg( ServiceUtil.disposeSingleImageInfo( Long.valueOf( StringUtil
                .getLongValue( commInfo.getImg(), -1 ) ) ) );

            ContentCommendTypeBean commTypeBean = channelDao
                .querySingleContentCommendTypeBeanByTypeId( commInfo.getCommendTypeId() );

            if( commTypeBean == null )
            {
                return;
            }

            contentDao.updateCommendPushInfoByInfoId( commInfo );

            /**
             * 扩展模型数据
             */

            metaDataService.addOrEditDefModelInfo( params, commTypeBean.getModelId(), commInfo
                .getInfoId(), Constant.METADATA.MODEL_TYPE_COMMEND );

        }
        finally
        {
            releaseContentCache();
            ContentDao.releaseAllCountCache();

        }

    }

    public void deleteCommendContentColumnInfo( String commFlag, List rowFlagArrayList,
        String siteFlag )
    {
        try
        {
            mysqlEngine.beginTransaction();

            List commList = null;

            if( rowFlagArrayList == null )
            {
                commList = contentDao.queryCommendRowInfoByCommFlag( commFlag, siteFlag );
            }
            else
            {
                commList = contentDao.queryCommendRowInfoByCommFlagAndRowFlag( rowFlagArrayList,
                    commFlag, siteFlag );
            }

            long rowFlag = -1;

            Set excludeRowFlagSet = new HashSet();

            if( rowFlagArrayList != null )
            {
                for ( int i = 0; i < rowFlagArrayList.size(); i++ )
                {
                    rowFlag = StringUtil.getLongValue( ( String ) rowFlagArrayList.get( i ), -1 );

                    if( rowFlag < 0 )
                    {
                        continue;
                    }

                    excludeRowFlagSet.add( Long.valueOf( rowFlag ) );
                }
            }

            ContentCommendPushInfoBean bean = null;

            for ( int i = 0; i < commList.size(); i++ )
            {
                bean = ( ContentCommendPushInfoBean ) commList.get( i );

                ServiceUtil.deleteSiteResTraceMode( Long.valueOf( StringUtil.getLongValue( bean
                    .getImgResId(), -1 ) ) );

                ContentCommendTypeBean ctBean = channelService
                    .retrieveSingleContentCommendTypeBeanByTypeId( bean.getCommendTypeId() );

                metaDataService.deleteAndClearDefModelInfo( bean.getInfoId(), ctBean.getModelId(),
                    siteFlag, Constant.METADATA.MODEL_TYPE_COMMEND );

            }

            List excludeRowList = new ArrayList( excludeRowFlagSet );

            contentDao.updateCommendRowInfoContentStatusByCommFlag( commFlag, excludeRowList,
                Constant.COMMON.OFF, siteFlag );

            contentDao.deleteCommendRowInfoByCommFlag( commFlag, excludeRowList, siteFlag );

            List rowBeanAllList = contentDao.queryAllCommendContentByCommendTypeByFlag( commFlag,
                siteFlag, false );

            ContentCommendPushInfoBean rowInfoBean = null;
            List rowBeanInnerList = null;
            int num = 0;

            for ( int i = 0; i < rowBeanAllList.size(); i++ )
            {
                rowInfoBean = ( ContentCommendPushInfoBean ) rowBeanAllList.get( i );

                if( excludeRowFlagSet.contains( rowInfoBean.getRowFlag() ) )
                {
                    continue;
                }

                num++;

                contentDao.updateCommendPushContentRowFlagByInfoId( Long.valueOf( num ),
                    rowInfoBean.getInfoId() );

                rowBeanInnerList = rowInfoBean.getRowInfoList();
                for ( int j = 0; j < rowBeanInnerList.size(); j++ )
                {
                    rowInfoBean = ( ContentCommendPushInfoBean ) rowBeanInnerList.get( j );
                    contentDao.updateCommendPushContentRowFlagByInfoId( Long.valueOf( num ),
                        rowInfoBean.getInfoId() );
                }
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            releaseContentCache();
            ContentDao.releaseAllCountCache();

        }

    }

    public void deleteCommendContentRowInfo( String commFlag, Long rowFlag,
        List deleteInfoIdArrayList, String siteFlag )
    {
        try
        {
            mysqlEngine.beginTransaction();

            List rowInfoExcludeDeleteIdList = contentDao
                .queryCommendPushContentRowInfoByRowFlagAndExcludeId( commFlag, rowFlag,
                    deleteInfoIdArrayList );

            Long infoId = null;

            ContentCommendPushInfoBean bean = null;

            for ( int i = 0; i < deleteInfoIdArrayList.size(); i++ )
            {
                infoId = Long.valueOf( StringUtil.getLongValue( ( String ) deleteInfoIdArrayList
                    .get( i ), -1 ) );

                if( infoId.longValue() > 0 )
                {
                    bean = contentDao.querySingleCommendPushInfoByInfoId( infoId );

                    ServiceUtil.deleteSiteResTraceMode( Long.valueOf( StringUtil.getLongValue( bean
                        .getImgResId(), -1 ) ) );

                    contentDao.deleteCommendInfoByInfoId( infoId );

                    ContentCommendTypeBean ctBean = channelService
                        .retrieveSingleContentCommendTypeBeanByTypeId( bean.getCommendTypeId() );

                    metaDataService.deleteAndClearDefModelInfo( infoId, ctBean.getModelId(),
                        siteFlag, Constant.METADATA.MODEL_TYPE_COMMEND );
                }

            }

            ContentCommendPushInfoBean infoBean = null;
            int num = 0;
            for ( int i = 0; i < rowInfoExcludeDeleteIdList.size(); i++ )
            {
                num = i + 1;

                infoBean = ( ContentCommendPushInfoBean ) rowInfoExcludeDeleteIdList.get( i );

                contentDao.updateCommendPushContentOrderInfoByInfoId( infoBean.getRowFlag(),
                    Integer.valueOf( num ), infoBean.getInfoId() );
            }

            if( rowInfoExcludeDeleteIdList.size() == 0 )
            {
                List allRowInfo = contentDao.queryAllCommendContentByCommendTypeByFlag( commFlag,
                    siteFlag, false );

                List rowList = null;
                num = 0;
                for ( int i = 0; i < allRowInfo.size(); i++ )
                {

                    infoBean = ( ContentCommendPushInfoBean ) allRowInfo.get( i );

                    if( infoBean.getRowFlag().longValue() != rowFlag.longValue() )
                    {
                        num++;

                        contentDao.updateCommendPushContentOrderInfoByInfoId( Long.valueOf( num ),
                            infoBean.getRowOrder(), infoBean.getInfoId() );

                        rowList = infoBean.getRowInfoList();

                        for ( int j = 0; j < rowList.size(); j++ )
                        {
                            infoBean = ( ContentCommendPushInfoBean ) rowList.get( j );

                            contentDao.updateCommendPushContentOrderInfoByInfoId( Long
                                .valueOf( num ), infoBean.getRowOrder(), infoBean.getInfoId() );
                        }
                    }
                }

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            releaseContentCache();
            ContentDao.releaseAllCountCache();

        }
    }

    public Integer retrieveAllCommendContentByCommendCountByFlag( String commendFlag )
    {
        return contentDao.queryAllCommendContentByCommendCountByFlag( commendFlag );
    }

    public Integer retrieveAllCommendContentByCommendCountByTypeId( Long typeId )
    {
        return contentDao.queryAllCommendContentByCommendCountByTypeId( typeId );
    }

    public List retrieveAllCommendContentByCommendByFlag( String commendFlag, String siteFlag )
    {
        String key = "retrieveAllCommendContentByCommendByFlag:" + commendFlag + "|" + siteFlag;

        List resList = ( List ) listContentCache.getEntry( key );

        if( resList == null )
        {
            resList = contentDao.queryAllCommendContentByCommendTypeByFlag( commendFlag, siteFlag,
                false );

            listContentCache.putEntry( key, resList );
        }

        return resList;
    }

    public List retrieveAllCommendContentByCommendByFlag( String commendFlag, String siteFlag,
        Integer size )
    {
        String key = "retrieveAllCommendContentByCommendByFlag:" + commendFlag + "|" + siteFlag
            + "|" + size;

        List resList = ( List ) listContentCache.getEntry( key );

        if( resList == null )
        {
            resList = contentDao.queryAllCommendContentByCommendTypeByFlag( commendFlag, siteFlag,
                size, false );

            listContentCache.putEntry( key, resList );
        }

        return resList;
    }

    public List retrieveAllCommendContentByCommendByTypeId( Long typeId, String siteFlag,
        Long startPos, Integer size )
    {
        String key = "retrieveAllCommendContentByCommendByTypeId:" + typeId + "|" + siteFlag + "|"
            + startPos + "|" + size;

        List resList = ( List ) listContentCache.getEntry( key );

        if( resList == null )
        {
            resList = contentDao.queryAllCommendContentByCommendTypeByTypeId( typeId, siteFlag,
                startPos, size, false );

            listContentCache.putEntry( key, resList );
        }

        return resList;
    }

    public ContentCommendPushInfoBean retrieveSingleCommendPushInfoByInfoId( Long infoId )
    {

        String key = "retrieveSingleCommendPushInfoByInfoId:" + infoId;

        ContentCommendPushInfoBean result = ( ContentCommendPushInfoBean ) singleContentCache
            .getEntry( key );

        if( result == null )
        {
            result = contentDao.querySingleCommendPushInfoByInfoId( infoId );

            singleContentCache.putEntry( key, result );
        }

        return result;
    }

    public Integer retrieveCensorStateByContentId( Long contentId )
    {
        return contentDao.queryCensorStateByContentId( contentId );
    }

    public List retrieveCommendContentRowInfoByRowFlag( Long rowFlag, String commFlag,
        String siteFlag )
    {
        String key = "retrieveCommendContentRowInfoByRowFlag:" + rowFlag + "|" + commFlag + "|"
            + siteFlag;

        List resList = ( List ) listContentCache.getEntry( key );

        if( resList == null )
        {
            resList = contentDao.queryCommendPushContentRowInfoByRowFlag( rowFlag, commFlag,
                siteFlag );

            listContentCache.putEntry( key, resList );
        }

        return resList;
    }

    public void sortCommendContentColumnInfo( String direct, Integer count, String commFlag,
        Long rowFlag, String siteFlag )
    {
        try
        {
            mysqlEngine.beginTransaction();

            List allCommInfoList = contentDao.queryAllCommendContentByCommendTypeByFlag( commFlag,
                siteFlag, false );

            ContentCommendPushInfoBean infoBean = null;
            ContentCommendPushInfoBean directBean = null;
            int directBeanPos = 0;

            for ( int i = 0; i < allCommInfoList.size(); i++ )
            {
                infoBean = ( ContentCommendPushInfoBean ) allCommInfoList.get( i );

                if( infoBean.getRowFlag().equals( rowFlag ) )
                {
                    directBean = infoBean;
                    directBeanPos = i + 1;
                    break;
                }
            }

            allCommInfoList.remove( directBean );

            int allRowCount = allCommInfoList.size();

            int targetPos = 0;

            if( "up".equals( direct ) )
            {
                targetPos = directBeanPos - count.intValue();
            }
            else if( "down".equals( direct ) )
            {
                targetPos = directBeanPos + count.intValue();
            }

            int newPos = targetPos - 1;
            if( targetPos - 1 < 0 )
            {
                newPos = 0;
            }
            else if( ( targetPos - 1 ) > allRowCount )
            {
                newPos = allRowCount;
            }

            allCommInfoList.add( newPos, directBean );

            List innerRowBeanList = null;
            for ( int i = 0; i < allCommInfoList.size(); i++ )
            {
                targetPos = i + 1;

                infoBean = ( ContentCommendPushInfoBean ) allCommInfoList.get( i );

                contentDao.updateCommendPushContentRowFlagByInfoId( Long.valueOf( targetPos ),
                    infoBean.getInfoId() );

                innerRowBeanList = infoBean.getRowInfoList();
                for ( int j = 0; j < innerRowBeanList.size(); j++ )
                {
                    infoBean = ( ContentCommendPushInfoBean ) innerRowBeanList.get( j );

                    contentDao.updateCommendPushContentRowFlagByInfoId( Long.valueOf( targetPos ),
                        infoBean.getInfoId() );
                }

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            releaseContentCache();
        }

    }

    public void sortCommendContentRowInfo( Long infoId, String sortFlag )
    {
        try
        {
            mysqlEngine.beginTransaction();

            ContentCommendPushInfoBean currentInfoBean = contentDao
                .querySingleCommendPushInfoByInfoId( infoId );

            if( currentInfoBean == null )
            {
                return;
            }

            ContentCommendPushInfoBean targetInfoBean = null;

            Integer currentOrder = currentInfoBean.getRowOrder();

            if( Constant.CONTENT.SORT_LEFT.equals( sortFlag ) )
            {
                targetInfoBean = contentDao.querySingleCommendPushInfoByOrderInfo( currentInfoBean
                    .getCommendFlag(), currentInfoBean.getRowFlag(), Integer
                    .valueOf( currentInfoBean.getRowOrder().intValue() + 1 ) );

            }
            else if( Constant.CONTENT.SORT_RIGHT.equals( sortFlag ) )
            {
                targetInfoBean = contentDao.querySingleCommendPushInfoByOrderInfo( currentInfoBean
                    .getCommendFlag(), currentInfoBean.getRowFlag(), Integer
                    .valueOf( currentInfoBean.getRowOrder().intValue() - 1 ) );
            }

            if( targetInfoBean == null )
            {
                return;
            }

            contentDao.updateCommendPushContentRowOrder( infoId, targetInfoBean.getRowOrder() );

            contentDao.updateCommendPushContentRowOrder( targetInfoBean.getInfoId(), currentOrder );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            releaseContentCache();

        }

    }

    public void addDownloadImgRes( Long contentId, Long resId )
    {
        contentDao.saveDlImg( contentId, resId );
    }

    public List retrieveDownloadImgRes( Long contentId )
    {
        return contentDao.queryDlImgList( contentId );
    }

    public void deleteDownloadImgRes( Long contentId )
    {
        contentDao.deleteDlImgList( contentId );
    }

    /**
     * 获取当前用户的稿件
     * 
     * @param classId
     * @param pn
     * @param size
     * @return
     */
    public Object[] getDraftContentForSiteQueryTag( String censorState, String classId, String pn,
        String size )
    {
        List result = null;

        Long classIdVar = Long.valueOf( StringUtil.getLongValue( classId, -1 ) );

        int pageNum = StringUtil.getIntValue( pn, 1 );

        int pageSize = StringUtil.getIntValue( size, 12 );

        SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
            .getCurrentLoginSiteInfo();

        String managerName = ( String ) SecuritySessionKeeper.getSecuritySession().getAuth()
            .getApellation();

        Page pageInfo = null;

        Long count = null;

        Long censor = Long.valueOf( StringUtil.getLongValue( censorState, -1 ) );

        if( classIdVar.longValue() < 1 )
        {
            count = contentDao.queryDraftContentCountBySiteId( site.getSiteId(), censor,
                managerName );

            pageInfo = new Page( pageSize, count.intValue(), pageNum );

            result = contentDao
                .queryDraftContentInfoBySiteId( site.getSiteId(), censor, managerName, Long
                    .valueOf( pageInfo.getFirstResult() ), Integer.valueOf( pageSize ) );
        }
        else
        {
            count = contentDao.queryDraftContentCountBySiteId( site.getSiteId(), classIdVar,
                censor, managerName );

            pageInfo = new Page( pageSize, count.intValue(), pageNum );

            result = contentDao.queryDraftContentInfoBySiteId( site.getSiteId(), classIdVar,
                censor, managerName, Long.valueOf( pageInfo.getFirstResult() ), Integer
                    .valueOf( pageSize ) );
        }

        return new Object[] { result, pageInfo };
    }

    public void deleteRelateContent( Long contentId, List deleteRIdList )
    {
        if( contentId == null || contentId.longValue() < 1 || deleteRIdList == null )
        {
            return;
        }

        SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
            .getCurrentLoginSiteInfo();

        try
        {
            mysqlEngine.beginTransaction();

            ContentMainInfoBean mainInfo = contentDao.querySingleContentMainInfoBean( contentId );

            if( !site.getSiteId().equals( mainInfo.getSiteId() ) )
            {
                return;
            }

            String currentRelateStr = mainInfo.getRelateIds();

            if( StringUtil.isStringNull( currentRelateStr ) )
            {
                return;
            }

            String idStr = null;

            for ( int i = 0; i < deleteRIdList.size(); i++ )
            {
                idStr = ( String ) deleteRIdList.get( i );

                currentRelateStr = StringUtil.replaceString( currentRelateStr, idStr + "|", "",
                    false, false );
            }

            contentDao.updateContentRelateIdInfo( contentId, currentRelateStr );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            ContentDao.releaseAllCountCache();

            releaseContentCache();
        }

    }

    /**
     * 获取关联内容
     * 
     * @param tagId
     * @return
     */
    public List getRelateContentQueryTag( String idList )
    {
        return StringUtil.changeStringToList( idList, "\\*" );
    }

    /**
     * 获取关联调查
     * 
     * @param tagId
     * @return
     */
    public List getRelateSurveyQueryTag( String idList )
    {
        return StringUtil.changeStringToList( idList, "\\*" );
    }

    public Long retrieveTagRaleteContentByTagId( Long tagId )
    {

        Long result = ( Long ) listTagContentCountCache.getEntry( tagId );

        if( result == null )
        {
            result = contentDao.queryTagRaleteContentCountByTagId( tagId );

            listTagContentCountCache.putEntry( tagId, result );
        }

        return result;
    }

    public List retrieveTagRaleteContentByTagId( Long tagId, String order, Long start, Integer size )
    {

        String key = "retrieveTagRaleteContentByTagId:" + tagId + "|" + order + "|" + start + "|"
            + size;

        List result = ( List ) listTagContentCache.getEntry( key );

        if( result == null )
        {
            result = contentDao.queryTagRaleteContentByTagId( tagId, order, start, size );

            listTagContentCache.putEntry( tagId, result );
        }

        return result;

    }

    public void addContentOperInfo( Long contentId, String puserName, String actionId,
        String msgContent )
    {
        contentDao.saveContentOperInfo( contentId, puserName, actionId, msgContent, new Timestamp(
            DateAndTimeUtil.clusterTimeMillis() ) );
    }

    public void checkAndDisposePickTraceSourceInfo()
    {
        try
        {
            mysqlEngine.beginTransaction();

            List pickSourceList = pickDao.queryAllPickWebTraceSource();

            String source = null;

            Map csMap = null;
            for ( int i = 0; i < pickSourceList.size(); i++ )
            {
                source = ( String ) pickSourceList.get( i );

                if( StringUtil.isStringNotNull( source ) )
                {
                    csMap = contentDao.querySingleContentSource( source );

                    if( csMap.isEmpty() )
                    {
                        contentDao.saveSource( source, StringUtil.getFirstPY(
                            source.toCharArray()[0] ).toString() );
                    }
                }
            }

            mysqlEngine.commit();
        }
        catch ( Exception e )
        {
        }
        finally
        {
            mysqlEngine.endTransaction();
        }

    }

    public void addNewContentSource( String sourceName )
    {
        contentDao.saveSource( sourceName, StringUtil.getFirstPY( sourceName.toCharArray()[0] )
            .toString() );
    }

    public void editContentSource( String sourceName, Long sId )
    {
        contentDao.updateSource( sourceName, StringUtil.getFirstPY( sourceName.toCharArray()[0] )
            .toString(), sId );
    }

    public void deleteContentSource( List idList )
    {
        Long id = null;

        for ( int i = 0; i < idList.size(); i++ )
        {
            id = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ), -1 ) );

            if( id.longValue() < 1 )
            {
                continue;
            }

            contentDao.deleteSource( id );
        }
    }

    public Object getContentSourceTag( String id, String fc, String pn, String size )
    {
        List result = null;

        Long tId = Long.valueOf( StringUtil.getLongValue( id, -1 ) );

        int pageNum = StringUtil.getIntValue( pn, 1 );

        int pageSize = StringUtil.getIntValue( size, 15 );

        Page pageInfo = null;

        if( tId.longValue() > 0 )
        {
            return contentDao.querySingleContentSource( tId );
        }
        else
        {
            if( StringUtil.isStringNull( fc ) )
            {
                Long count = contentDao.queryContentSourceCount();

                pageInfo = new Page( pageSize, count.intValue(), pageNum );

                result = contentDao.queryContentSource( Long.valueOf( pageInfo.getFirstResult() ),
                    Integer.valueOf( pageSize ) );
            }
            else
            {
                Long count = contentDao.queryContentSourceCount( fc );

                pageInfo = new Page( pageSize, count.intValue(), pageNum );

                result = contentDao.queryContentSource( fc, Long
                    .valueOf( pageInfo.getFirstResult() ), Integer.valueOf( pageSize ) );
            }

            return new Object[] { result, pageInfo };
        }

    }

    public void addNewSensitiveWord( String sensitive, String replace )
    {

        if( StringUtil.isStringNotNull( sensitive ) && !haveSameFlag( sensitive.trim() ) )
        {
            contentDao.saveSensitiveWord( sensitive, replace, Constant.COMMON.ON );

            swFilter.load();
        }

    }

    public void editSensitiveWord( String sensitive, String replace, Long swId )
    {
        contentDao.updateSensitiveWord( sensitive.trim(), replace, swId );

        swFilter.load();
    }

    public void importSensitiveWord( String sword )
    {
        if( sword == null )
        {
            return;
        }

        String[] sws = StringUtil.split( sword, "\n" );

        for ( String sw : sws )
        {
            if( StringUtil.isStringNotNull( sw ) && !haveSameFlag( sw.trim() ) )
            {
                contentDao.saveSensitiveWord( sw.trim(), "", Constant.COMMON.ON );
            }
        }

        swFilter.load();
    }

    public boolean haveSameFlag( String flag )
    {
        Integer count = valiDao.querySystemTableFlagExist( "site_sensitive_word", "sensitiveStr",
            flag );

        if( count.intValue() > 0 )
        {
            return true;
        }

        return false;
    }

    public void deleteSensitiveWord( List idList )
    {
        Long id = null;

        for ( int i = 0; i < idList.size(); i++ )
        {
            id = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ), -1 ) );

            if( id.longValue() < 1 )
            {
                continue;
            }

            contentDao.deleteSensitiveWord( id );
        }

        swFilter.load();
    }

    public Object getSensitiveWordTag( String id, String pn, String size )
    {
        List result = null;

        Long tId = Long.valueOf( StringUtil.getLongValue( id, -1 ) );

        int pageNum = StringUtil.getIntValue( pn, 1 );

        int pageSize = StringUtil.getIntValue( size, 15 );

        Page pageInfo = null;

        if( tId.longValue() > 0 )
        {
            return contentDao.querySingleSensitiveWord( tId );
        }
        else
        {
            Long count = contentDao.querySensitiveWordCount();

            pageInfo = new Page( pageSize, count.intValue(), pageNum );

            result = contentDao.querySensitiveWord( Long.valueOf( pageInfo.getFirstResult() ),
                Integer.valueOf( pageSize ) );

            return new Object[] { result, pageInfo };
        }

    }

    public Object getSensitiveWordTag( String id, String fc, String pn, String size )
    {
        List result = null;

        Long tId = Long.valueOf( StringUtil.getLongValue( id, -1 ) );

        int pageNum = StringUtil.getIntValue( pn, 1 );

        int pageSize = StringUtil.getIntValue( size, 15 );

        Page pageInfo = null;

        if( tId.longValue() > 0 )
        {
            return contentDao.querySingleContentSource( tId );
        }
        else
        {
            if( StringUtil.isStringNull( fc ) )
            {
                Long count = contentDao.queryContentSourceCount();

                pageInfo = new Page( pageSize, count.intValue(), pageNum );

                result = contentDao.queryContentSource( Long.valueOf( pageInfo.getFirstResult() ),
                    Integer.valueOf( pageSize ) );
            }
            else
            {
                Long count = contentDao.queryContentSourceCount( fc );

                pageInfo = new Page( pageSize, count.intValue(), pageNum );

                result = contentDao.queryContentSource( fc, Long
                    .valueOf( pageInfo.getFirstResult() ), Integer.valueOf( pageSize ) );
            }

            return new Object[] { result, pageInfo };
        }

    }

    public void changeSensitiveWorduserStatus( List idList, Integer us )
    {
        Long id = null;

        for ( int i = 0; i < idList.size(); i++ )
        {
            id = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ), -1 ) );

            if( id.longValue() < 1 )
            {
                continue;
            }

            contentDao.updateSensitiveWordUseStatus( us, id );
        }
    }

    public Map retrieveSingleContentStatus( Long contentId )
    {
        String key = "retrieveSingleContentStatus:" + contentId;

        Map result = ( Map ) fastContentStatusCache.getEntry( key );

        if( result == null )
        {
            result = contentDao.querySingleContentStatus( contentId );

            fastContentStatusCache.putEntry( key, result );
        }
        else
        {
            StatContentVisitOrCommentDWMCount scv = ( StatContentVisitOrCommentDWMCount ) StatService.statCacheContentClickCountMap
                .get( contentId );

            if( scv != null )
            {
                long clickMonthCount = ( Long ) result.get( "clickMonthCount" );

                long clickWeekCount = ( Long ) result.get( "clickWeekCount" );

                long clickDayCount = ( Long ) result.get( "clickDayCount" );

                long clickCount = ( Long ) result.get( "clickCount" );

                Map res = new LinkedHashMap();

                res.putAll( result );

                res.put( "clickMonthCount", clickMonthCount + scv.getMonthCount() );
                res.put( "clickWeekCount", clickWeekCount + scv.getWeekCount() );
                res.put( "clickDayCount", clickDayCount + scv.getDayCount() );
                res.put( "clickCount", clickCount + scv.getNoLimitCount() );

                return res;
            }

        }

        return result;
    }

    public void addTagRelateContent( List cidList, List tagIdList )
    {
        if( cidList == null || tagIdList == null )
        {
            return;
        }

        Long tagId = null;

        Long contentId = null;

        Long existCount = null;

        Map mainInfo = null;

        String tagkey = null;

        try
        {
            mysqlEngine.beginTransaction();

            for ( int i = 0; i < tagIdList.size(); i++ )
            {
                if( tagIdList.get( i ) instanceof Long )
                {
                    tagId = ( Long ) tagIdList.get( i );
                }
                else
                {
                    tagId = Long.valueOf( StringUtil.getLongValue( ( String ) tagIdList.get( i ),
                        -1 ) );
                }

                if( tagId.longValue() < 0 )
                {
                    continue;
                }

                for ( int j = 0; j < cidList.size(); j++ )
                {
                    if( cidList.get( j ) instanceof Long )
                    {
                        contentId = ( Long ) cidList.get( j );
                    }
                    else
                    {
                        contentId = Long.valueOf( StringUtil.getLongValue( ( String ) cidList
                            .get( j ), -1 ) );
                    }

                    if( contentId.longValue() < 0 )
                    {
                        continue;
                    }

                    existCount = channelDao.queryTagWordRelateContentCount( tagId, contentId );

                    if( existCount.longValue() < 1 )
                    {
                        channelDao.saveTagWordRelateContent( tagId, contentId );

                        mainInfo = contentDao.querySingleContentMainInfo( contentId );

                        tagkey = ( String ) mainInfo.get( "tagKey" );

                        contentDao.updateContentTagKeyIdStr( contentId, tagkey + "*" + tagId );
                    }
                }

                channelDao.updateTagWordRelateContentCount( tagId );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            ContentDao.releaseAllCountCache();

            releaseContentCache();
        }

    }

    public int addOrEditDefineFormData( SiteGroupBean site, Map params, boolean editMode )
    {
        Long modelId = Long.valueOf( StringUtil.getLongValue( ( String ) params.get( "modelId" ),
            -1 ) );

        /**
         * 获取对应数据模型元数据
         */
        DataModelBean modelBean = metaDataService.retrieveSingleDataModelBeanById( modelId );
        List filedBeanList = metaDataService.retrieveModelFiledInfoBeanList( modelId );
        ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( modelId );

        if( modelBean == null )
        {
            return -1;
        }

        ModelFiledInfoBean bean = null;

        List needUploadImageGroupInfoList = new ArrayList();

        List userDefineParamList = new ArrayList();

        Object val = null;

        String reUrl = null;

        for ( int j = 0; j < filedBeanList.size(); j++ )
        {
            bean = ( ModelFiledInfoBean ) filedBeanList.get( j );

            if( Constant.METADATA.INNER_DATA == bean.getHtmlElementId().intValue() )
            {
                continue;
            }

            val = ServiceUtil.disposeDataModelFiledFromWeb( bean, params,
                needUploadImageGroupInfoList, false );

            if( val == null )
            {
                val = bean.getDefaultValue();
            }

            userDefineParamList.add( val );

            if( Constant.METADATA.UPLOAD_IMG == bean.getHtmlElementId().intValue()
                && Constant.COMMON.ON.equals( bean.getNeedMark() ) )
            {
                reUrl = ServiceUtil.getImageReUrl( ( String ) val );

                if( !Constant.COMMON.ON.equals( resService.getImageMarkStatus( reUrl ) ) )
                {
                    if( ServiceUtil.disposeImageMark( site, reUrl, Integer.valueOf( ServiceUtil
                        .getImageW( ( String ) val ) ), Integer.valueOf( ServiceUtil
                        .getImageH( ( String ) val ) ) ) )
                    {
                        resService.setImageMarkStatus( reUrl, Constant.COMMON.ON );
                    }
                }

            }

            if( Constant.METADATA.MYSQL_DATETIME.equals( bean.getPerdureType() )
                && Constant.COMMON.ON.equals( bean.getOrderFlag() ) )
            {
                if( val instanceof Timestamp )
                {
                    userDefineParamList.add( Long.valueOf( ( ( Timestamp ) val ).getTime() ) );
                }
                else
                {
                    userDefineParamList.add( DateAndTimeUtil.clusterTimeMillis() );
                }
            }
        }

        try
        {
            mysqlEngine.beginTransaction();

            Long contentId = Long.valueOf( -1 );

            if( editMode )
            {
                contentId = Long.valueOf( StringUtil.getLongValue( ( String ) params
                    .get( Constant.METADATA.CONTENT_ID_NAME ), -1 ) );

                userDefineParamList.add( contentId );

                contentDao.saveOrUpdateModelContent( sqlCodeBean.getUpdateSql(),
                    userDefineParamList.toArray() );
            }
            else
            {

                UpdateState updateState = metaDataDao.saveDefFormMainInfo( modelBean
                    .getDataModelId(), site.getSiteId(), modelBean.getMustCensor().intValue() == 1
                    ? Constant.WORKFLOW.CENSOR_STATUS_IN_FLOW
                    : Constant.WORKFLOW.CENSOR_STATUS_SUCCESS );

                if( updateState.haveKey() )
                {
                    contentId = Long.valueOf( updateState.getKey() );

                    params.put( Constant.CONTENT.ID_ORDER_VAR, contentId );

                    userDefineParamList.add( contentId );

                    contentDao.saveOrUpdateModelContent( sqlCodeBean.getInsertSql(),
                        userDefineParamList.toArray() );

                    for ( int j = 0; j < filedBeanList.size(); j++ )
                    {
                        bean = ( ModelFiledInfoBean ) filedBeanList.get( j );

                        if( Constant.METADATA.INNER_DATA == bean.getHtmlElementId().intValue() )
                        {
                            metaDataService.updateFieldMetadataDefValAndId( modelBean,
                                Constant.METADATA.PREFIX_COLUMN_NAME + bean.getFieldSign(), bean
                                    .getDefaultValue(), contentId );
                        }
                    }

                }
                else
                {
                    return -3;
                }

            }

            SearchIndexContentState searchIndexState = new SearchIndexContentState();

            searchIndexState.setClassId( Long.valueOf( -999999999 ) );
            searchIndexState.setContentId( contentId );

            searchIndexState
                .setCensor( modelBean.getMustCensor().intValue() == 1
                    ? Constant.WORKFLOW.CENSOR_STATUS_IN_FLOW
                    : Constant.WORKFLOW.CENSOR_STATUS_SUCCESS );

            searchIndexState.setIndexDate( new Date( DateAndTimeUtil.clusterTimeMillis() ) );
            searchIndexState.setEventFlag( editMode ? Constant.JOB.SEARCH_INDEX_EDIT
                : Constant.JOB.SEARCH_INDEX_ADD );

            searchIndexState.setModelId( modelBean.getDataModelId() );
            searchIndexState.setSiteId( site.getSiteId() );

            searchService.addIndexContentState( searchIndexState );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            MetaDataService.resetFormDataCache();
        }

        return 1;
    }

    /**
     * 获取内容审核日志
     * 
     * @param contentId
     * @return
     */
    public Object getContentOperInfoInfoList( String contentId, String pn, String size )
    {
        List result = null;

        Long cid = Long.valueOf( StringUtil.getLongValue( contentId, -1 ) );

        int pageNum = StringUtil.getIntValue( pn, 1 );

        int pageSize = StringUtil.getIntValue( size, 12 );

        Page pageInfo = null;

        Long count = null;

        count = contentDao.queryContentOperInfoInfoCount( cid );

        pageInfo = new Page( pageSize, count.intValue(), pageNum );

        result = contentDao.queryContentOperInfoInfoList( cid, Long.valueOf( pageInfo
            .getFirstResult() ), Integer.valueOf( pageSize ) );

        return new Object[] { result, pageInfo };
    }

    public List retrieveLinkInfo( List ids )
    {
        List allId = new ArrayList();

        for ( Object id : ids )
        {
            Long cId = StringUtil.getLongValue( id.toString(), -1 );

            allId.addAll( contentDao.queryLinkContentMainInfo( cId ) );

        }

        return allId;
    }

    public List retrieveTrashLinkInfo( List ids )
    {
        List allId = new ArrayList();

        for ( Object id : ids )
        {
            Long cId = StringUtil.getLongValue( id.toString(), -1 );

            allId.addAll( contentDao.queryTrashLinkContentMainInfo( cId ) );

        }

        return allId;
    }

    public Object getMaxOrMinHeadPageModeOrderIdByOrderByAndWay( String orderBy, String orderWay )
    {
        Object max = null;
        if( Constant.CONTENT.DEFAULT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ORDER_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ORDER_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.ID_ORDER_VAR.equals( orderBy )
            || Constant.CONTENT.ADD_DATE_ORDER_VAR.equals( orderBy ) )
        {

            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Long.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Long.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.PUB_DATE_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Long.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Long.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_DAY_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_WEEK_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_MONTH_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_DAY_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_WEEK_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_MONTH_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.SUPPORT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.AGAINST_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
        }

        return max;
    }

    public Object getMaxOrMinEndPageModeOrderIdByOrderByAndWay( String orderBy, String orderWay )
    {
        Object max = null;
        if( Constant.CONTENT.DEFAULT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ORDER_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ORDER_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.ID_ORDER_VAR.equals( orderBy )
            || Constant.CONTENT.ADD_DATE_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.PUB_DATE_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_DAY_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_WEEK_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_MONTH_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_DAY_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_WEEK_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_MONTH_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.SUPPORT_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }
        else if( Constant.CONTENT.AGAINST_ORDER_VAR.equals( orderBy ) )
        {
            if( Constant.CONTENT.UP_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MAX_ID_FLAG );
            }
            else if( Constant.CONTENT.DOWN_ORDER_WAY_VAR.equals( orderWay ) )
            {
                max = Double.valueOf( Constant.CONTENT.MIN_ID_FLAG );
            }
        }

        return max;
    }

    public String getOrderFilterByCreateBy( String orderFilterBy, String filterBy,
        String creatorBy, String orgCode, boolean childMode )
    {
        String filter = getOrderFilterByFilterBy( orderFilterBy, filterBy, orgCode, childMode );

        if( Constant.CONTENT.CREATOR_MY_FILTER.equals( creatorBy ) )
        {
            filter = filter + " and creator='"
                + SecuritySessionKeeper.getSecuritySession().getAuth().getApellation() + "'";
        }
        else if( Constant.CONTENT.CREATOR_OTHER_FILTER.equals( creatorBy ) )
        {
            filter = filter + " and otherFlag=1";
        }

        return filter;
    }

    public String getOrderFilterByFilterBy( String orderFilterBy, String filterBy, String orgCode,
        boolean childMode )
    {
        String filter = getOrderFilterByOrderFilterBy( orderFilterBy );

        filter = getOrderFilterByOrgCodeFilterBy( orgCode, childMode );

        if( Constant.CONTENT.HAME_IMG_FILTER.equals( filterBy ) )
        {
            filter = filter + " and homeImgFlag=1 ";
        }
        else if( Constant.CONTENT.CHANNEL_IMG_FILTER.equals( filterBy ) )
        {
            filter = filter + " and channelImgFlag=1 ";
        }
        else if( Constant.CONTENT.CLASS_IMG_FILTER.equals( filterBy ) )
        {
            filter = filter + " and classImgFlag=1 ";
        }
        else if( Constant.CONTENT.CONTENT_IMG_FILTER.equals( filterBy ) )
        {
            filter = filter + " and contentImgFlag=1 ";
        }

        return filter;
    }

    public String getOrderFilterByOrgCodeFilterBy( String orgCode, boolean childMode )
    {
        String filter = "";

        if( StringUtil.isStringNotNull( orgCode ) )
        {
            if( childMode )
            {
                filter = " and orgCode like '" + orgCode + "%' ";
            }
            else
            {
                filter = " and orgCode='" + orgCode + "' ";
            }
        }

        return filter;
    }

    public String getOrderFilterByOrderFilterBy( String orderFilterBy )
    {
        String filter = "";

        if( Constant.CONTENT.CLICK_COUNT_ORDER_VAR.equals( orderFilterBy ) )
        {
            filter = " and clickCount>=0 ";
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_DAY_VAR.equals( orderFilterBy ) )
        {
            filter = " and clickDayCount>=0 ";
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_WEEK_VAR.equals( orderFilterBy ) )
        {
            filter = " and clickWeekCount>=0 ";
        }
        else if( Constant.CONTENT.CLICK_COUNT_ORDER_MONTH_VAR.equals( orderFilterBy ) )
        {
            filter = " and clickMonthCount>=0 ";
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_VAR.equals( orderFilterBy ) )
        {
            filter = " and commCount>=0 ";
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_DAY_VAR.equals( orderFilterBy ) )
        {
            filter = " and commDayCount>=0 ";
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_WEEK_VAR.equals( orderFilterBy ) )
        {
            filter = " and commWeekCount>=0 ";
        }
        else if( Constant.CONTENT.COMM_COUNT_ORDER_MONTH_VAR.equals( orderFilterBy ) )
        {
            filter = " and commMonthCount>=0 ";
        }
        else if( Constant.CONTENT.SUPPORT_ORDER_VAR.equals( orderFilterBy ) )
        {
            filter = " and supportCount>=0 ";
        }
        else if( Constant.CONTENT.AGAINST_ORDER_VAR.equals( orderFilterBy ) )
        {
            filter = " and againstCount>=0 ";
        }

        return filter;
    }

    private Integer disposeWorkflowState( Map requestParams, Long currentKey,
        ContentClassBean classBean, ContentMainInfo info, boolean editMode, List wfActionList,
        Map currentInfo )
    {
        String contentAddStatus = ( String ) requestParams.get( "contentAddStatus" );

        Integer endState = null;

        if( Constant.WORKFLOW.DRAFT.equals( contentAddStatus ) )
        {
            contentDao.updateContentMainInfoCensorStatus( currentKey,
                Constant.WORKFLOW.CENSOR_STATUS_DRAFT );

            endState = Constant.WORKFLOW.CENSOR_STATUS_DRAFT;
        }

        else
        {
            endState = pendingCensorStateByStartAndEndPublishDate( new Timestamp( info
                .getAppearStartDateTime().getTime() ), new Timestamp( info.getAppearEndDateTime()
                .getTime() ), Constant.WORKFLOW.CENSOR_STATUS_SUCCESS );

            contentDao.updateContentMainInfoCensorStatus( currentKey, endState );
        }

        return endState;
    }

    /**
     * 替换敏感词
     * 
     * @param fullContent
     * @return
     */
    public String replcaeContentTextSensitive( String fullContent )
    {
        List swList = new ArrayList<String>( swFilter.getSensitiveWord( fullContent, SW_MAX ) );

        String sw = null;
        String replace = null;

        for ( int i = 0; i < swList.size(); i++ )
        {
            sw = ( String ) swList.get( i );

            replace = "";

            fullContent = StringUtil.replaceString( fullContent, sw, replace, false, false );
        }

        return fullContent;
    }

    public void reloadSensitiveWord()
    {
        this.swFilter.load();
    }

    /**
     * 获取新的发布时间排序ID,此方法必须为完全方法级同步,保证ID生成正确性
     * 
     * @return
     */
    public synchronized Long getNextPublishOrderTrace()
    {
        UpdateState us = contentDao.updateAndAddValPublishOrderId();

        if( us.getRow() < 0 )
        {
            throw new FrameworkException( "更新发布排序ID失败." );
        }

        return contentDao.queryPublishOrderId();
    }

    private Integer pendingCensorStateByStartAndEndPublishDate( Timestamp start, Timestamp end,
        Integer defaultCensor )
    {
        Date current = new Date( DateAndTimeUtil.clusterTimeMillis() );

        if( current.compareTo( end ) > 0 )
        {
            return Constant.WORKFLOW.CENSOR_STATUS_WITHDRAW;
        }
        else if( current.compareTo( start ) < 0 )
        {
            return Constant.WORKFLOW.CENSOR_STATUS_WAIT_PUBLISH;
        }
        else
        {
            return defaultCensor;
        }
    }

    public static void releaseContentCache()
    {
        singleContentCache.clearAllEntry();
        listContentCache.clearAllEntry();

        fastListContentCache.clearAllEntry();
        fastContentStatusCache.clearAllEntry();

        listTagContentCountCache.clearAllEntry();
        listTagContentCache.clearAllEntry();

    }

    public static void releaseTagContentCache()
    {
        listTagContentCountCache.clearAllEntry();
        listTagContentCache.clearAllEntry();
    }

    public static void releaseFastListContentCache()
    {
        fastListContentCache.clearAllEntry();
        fastContentStatusCache.clearAllEntry();

    }

}
