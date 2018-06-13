package cn.com.mjsoft.cms.guestbook.service;

import static cn.com.mjsoft.cms.common.ServiceUtil.cleanBasicHtmlByWhiteRule;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.com.mjsoft.cms.cluster.adapter.ClusterCacheAdapter;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.ServiceUtil;
import cn.com.mjsoft.cms.common.datasource.MySqlDataSource;
import cn.com.mjsoft.cms.content.dao.ContentDao;
import cn.com.mjsoft.cms.content.service.ContentService;
import cn.com.mjsoft.cms.guestbook.bean.GuestbookConfigBean;
import cn.com.mjsoft.cms.guestbook.bean.GuestbookMainInfoBean;
import cn.com.mjsoft.cms.guestbook.dao.GuestbookDao;
import cn.com.mjsoft.cms.guestbook.dao.vo.GuestbookConfig;
import cn.com.mjsoft.cms.guestbook.dao.vo.GuestbookMainInfo;
import cn.com.mjsoft.cms.message.service.MessageService;
import cn.com.mjsoft.cms.metadata.bean.DataModelBean;
import cn.com.mjsoft.cms.metadata.bean.ModelFiledInfoBean;
import cn.com.mjsoft.cms.metadata.bean.ModelPersistenceMySqlCodeBean;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.cms.resources.service.ResourcesService;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.framework.cache.Cache;
import cn.com.mjsoft.framework.persistence.core.PersistenceEngine;
import cn.com.mjsoft.framework.persistence.core.support.UpdateState;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.DateAndTimeUtil;
import cn.com.mjsoft.framework.util.StringUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class GuestbookService
{
    
    private static Cache countCache = new ClusterCacheAdapter( 500,
        "contentService.listContentCache" );

    private static Cache gbCache = new ClusterCacheAdapter( 500, "contentService.listContentCache" );

    private static Cache gbCfgCache = new ClusterCacheAdapter( 120,
        "contentService.listContentCache" );

    private static GuestbookService service = null;

    private MetaDataService metaDataService = MetaDataService.getInstance();

    private MessageService messageService = MessageService.getInstance();

    public PersistenceEngine mysqlEngine = new PersistenceEngine( new MySqlDataSource() );

    private static ResourcesService resService = ResourcesService.getInstance();

    private GuestbookDao gbDao;

    private ContentDao contentDao;

    private GuestbookService()
    {
        gbDao = new GuestbookDao( mysqlEngine );
        contentDao = new ContentDao( mysqlEngine );
    }

    private static synchronized void init()
    {
        if( null == service )
        {
            service = new GuestbookService();
        }
    }

    public static GuestbookService getInstance()
    {
        if( null == service )
        {
            init();
        }
        return service;
    }

    public List retrieveAllGuestbookConfigBeanListBySite( Long siteId )
    {
        String key = "retrieveAllGuestbookConfigBeanListBySite" + siteId;

        List result = ( List ) gbCfgCache.getEntry( key );

        if( result == null )
        {
            result = gbDao.queryAllGuestbookConfigBeanList( siteId );

            gbCfgCache.putEntry( key, result );
        }

        return result;
    }

    public Long retrieveGuestbookMainInfoCount( String configFlag, String isReply, String isCensor,
        String isOpen )
    {
        GuestbookConfigBean configBean = retrieveSingleGuestbookConfigBeanByConfigFlag( configFlag );

        if( configBean == null )
        {
            // 缺少配置
            return Long.valueOf( 0 );
        }

        Long cfgId = configBean.getConfigId();

        Integer isOp = Integer.valueOf( StringUtil.getIntValue( isOpen, -1 ) );

        Integer isCe = Integer.valueOf( StringUtil.getIntValue( isCensor, -1 ) );

        Integer isRe = Integer.valueOf( StringUtil.getIntValue( isReply, -1 ) );

        String key = "retrieveGuestbookMainInfoCount:" + cfgId + "|" + isReply + "|" + isCensor
            + "|" + isOpen;

        Long result = ( Long ) countCache.getEntry( key );

        if( result == null )
        {
            if( "".equals( isReply ) )
            {
                if( "".equals( isCensor ) )
                {
                    if( "".equals( isOpen ) )
                    {
                        result = gbDao.queryGuestbookMainInfoCount( cfgId );
                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoCountIsOpen( isOp, cfgId );
                    }
                }
                else
                {
                    if( "".equals( isOpen ) )
                    {
                        result = gbDao.queryGuestbookMainInfoCountIsCensor( isCe, cfgId );
                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoCountIsOpenAndIsCensor( isCe, isOp,
                            cfgId );
                    }
                }
            }
            else
            {
                if( "".equals( isCensor ) )
                {
                    if( "".equals( isOpen ) )
                    {
                        result = gbDao.queryGuestbookMainInfoCountIsReply( isRe, cfgId );
                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoCountIsOpenAndIsReply( isOp, isRe,
                            cfgId );
                    }
                }
                else
                {
                    if( "".equals( isOpen ) )
                    {
                        result = gbDao.queryGuestbookMainInfoCountIsCensorAndIsReply( isCe, isRe,
                            cfgId );
                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoCountIsOpenAndIsCensorAndIsReply(
                            isCe, isOp, isRe, cfgId );
                    }
                }
            }

            countCache.putEntry( key, result );
        }

        return result;
    }

    public List retrieveGuestbookMainInfoMapList( String configFlag, String isReply,
        String isCensor, String isOpen, Long startPos, Integer pageSize )
    {
      
        Integer isOp = Integer.valueOf( StringUtil.getIntValue( isOpen, -1 ) );

        Integer isCe = Integer.valueOf( StringUtil.getIntValue( isCensor, -1 ) );

        Integer isRe = Integer.valueOf( StringUtil.getIntValue( isReply, -1 ) );

        String key = "retrieveGuestbookMainInfoMapList:" + configFlag + "|" + isReply + "|"
            + isCensor + "|" + isOpen + "|" + startPos + "|" + pageSize;

        List result = ( List ) gbCache.getEntry( key );

        if( result == null )
        {

            GuestbookConfigBean configBean = retrieveSingleGuestbookConfigBeanByConfigFlag( configFlag );

            if( configBean == null )
            {
                // 缺少配置
                return new ArrayList( 1 );
            }

            // 扩展模型信息

            ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
                .retrieveSingleModelPerMysqlCodeBean( configBean.getInfoModelId() );

            DataModelBean model = metaDataService.retrieveSingleDataModelBeanById( configBean
                .getInfoModelId() );

            if( "".equals( isReply ) )
            {
                if( "".equals( isCensor ) )
                {
                    if( "".equals( isOpen ) )
                    {
                        result = gbDao.queryGuestbookMainInfoMapList( configBean.getConfigId(),
                            model, sqlCodeBean, pageSize, startPos );
                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoMapIsOpenList( configBean
                            .getConfigId(), isOp, model, sqlCodeBean, pageSize, startPos );
                    }
                }
                else
                {
                    if( "".equals( isOpen ) )
                    {
                        result = gbDao.queryGuestbookMainInfoMapIsCensorList( configBean
                            .getConfigId(), isCe, model, sqlCodeBean, pageSize, startPos );
                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoMapIsOpenAndIsCensorList( configBean
                            .getConfigId(), isOp, isCe, model, sqlCodeBean, pageSize, startPos );
                    }
                }
            }
            else
            {
                if( "".equals( isCensor ) )
                {
                    if( "".equals( isOpen ) )
                    {
                        result = gbDao.queryGuestbookMainInfoMapIsReplyList( configBean
                            .getConfigId(), isRe, model, sqlCodeBean, pageSize, startPos );

                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoMapIsReplyAndIsOpenList( configBean
                            .getConfigId(), isRe, isOp, model, sqlCodeBean, pageSize, startPos );
                    }
                }
                else
                {
                    if( "".equals( isOpen ) )
                    {

                        result = gbDao.queryGuestbookMainInfoMapIsReplyAndIsCensorList( configBean
                            .getConfigId(), isCe, isRe, model, sqlCodeBean, pageSize, startPos );
                    }
                    else
                    {
                        result = gbDao.queryGuestbookMainInfoMapIsOpenAndIsCensorAndIsReplyList(
                            configBean.getConfigId(), isOp, isRe, isCe, model, sqlCodeBean,
                            pageSize, startPos );
                    }
                }
            }

            gbCache.putEntry( key, result );
        }

        return result;
    }

    public GuestbookConfigBean retrieveSingleGuestbookConfigBeanByConfigId( Long configId )
    {
        String key = "retrieveSingleGuestbookConfigBeanByConfigId" + configId;

        GuestbookConfigBean result = ( GuestbookConfigBean ) gbCfgCache.getEntry( key );

        if( result == null )
        {
            result = gbDao.querySingleGuestbookConfigBeanByConfigId( configId );

            gbCfgCache.putEntry( key, result );
        }
        return result;
    }

    public GuestbookConfigBean retrieveSingleGuestbookConfigBeanByConfigFlag( String configFlag )
    {
        return gbDao.querySingleGuestbookConfigBeanByConfigFlag( configFlag );
    }

    public Map retrieveSingleGuestbookInfoMapByGbId( Long gbId )
    {

        GuestbookMainInfoBean gbMain = gbDao.querySingleGuestbookMainInfoByGbid( gbId );

        if( gbMain == null )
        {
            return new HashMap();
        }

        GuestbookConfigBean configBean = gbDao.querySingleGuestbookConfigBeanByConfigId( gbMain
            .getConfigId() );

        if( configBean == null )
        {
            // 缺少配置
            return new HashMap( 1 );
        }

        // 扩展模型信息

        ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
            .retrieveSingleModelPerMysqlCodeBean( configBean.getInfoModelId() );

        DataModelBean model = metaDataService.retrieveSingleDataModelBeanById( configBean
            .getInfoModelId() );

        return gbDao.querySingleGuestbookInfoMapByGbId( gbId, model, sqlCodeBean );
    }

    public void addNewGuestbookConfig( GuestbookConfig gbCfg )
    {
        try
        {
            gbDao.save( gbCfg );
        }
        finally
        {
            GuestbookDao.clearConfigBeanCache();

            clear();

            clearCfg();
        }
    }

    public void editGuestbookConfig( GuestbookConfig gbCfg )
    {
        try
        {
            gbDao.update( gbCfg );
        }
        finally
        {
            GuestbookDao.clearConfigBeanCache();

            clear();

            clearCfg();
        }
    }

    public void addNewGuestbookInfo( GuestbookMainInfo gbInfo, DataModelBean model,
        List filedBeanList, ModelPersistenceMySqlCodeBean sqlCodeBean, Map requestParams )
    {

        if( gbInfo == null )
        {
            return;
        }

        // html白名单
        if(StringUtil.isStringNotNull( gbInfo.getGbTitle() ))
        {
            gbInfo.setGbTitle( ContentService.getInstance().replcaeContentTextSensitive(gbInfo.getGbTitle() ));
        }
        
        gbInfo.setGbText( ContentService.getInstance().replcaeContentTextSensitive(
                cleanBasicHtmlByWhiteRule( gbInfo.getGbText() )) );

        try
        {
            mysqlEngine.beginTransaction();

            UpdateState us = gbDao.save( gbInfo );

            if( us.haveKey() && model != null && filedBeanList != null && sqlCodeBean != null )
            {
                ModelFiledInfoBean bean = null;

                List userDefineParamList = new ArrayList();

                String reUrl = null;

                Object val = null;

                for ( int j = 0; j < filedBeanList.size(); j++ )
                {
                    bean = ( ModelFiledInfoBean ) filedBeanList.get( j );
                    
                    val = ServiceUtil
                        .disposeDataModelFiledFromWeb( bean, requestParams, null, true );

                      if( Constant.METADATA.EDITER == bean.getHtmlElementId().intValue() )
                    {
                        val = ServiceUtil.cleanEditorHtmlByWhiteRule( ( String ) val );
                    }

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

                   userDefineParamList.add( Long.valueOf( us.getKey() ) );

                contentDao.saveOrUpdateModelContent( sqlCodeBean.getInsertSql(),
                    userDefineParamList.toArray() );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            clear();
        }

    }

    public void replyGuestbook( Long gbId, String replyText, String replyMan )
    {
        try
        {
            gbDao.updateGuestbookReplyInfo( gbId, replyText, replyMan, new Date( DateAndTimeUtil
                .clusterTimeMillis() ) );
        }
        finally
        {
            clear();
        }
    }

    public void editGuestbookStatus( List idList, String action, Integer flag )
    {
        try
        {
            mysqlEngine.beginTransaction();

            long id = -1;
            for ( int i = 0; i < idList.size(); i++ )
            {
                id = StringUtil.getLongValue( ( String ) idList.get( i ), -1 );

                if( id > 0 )
                {
                    if( "censor".equals( action ) )
                    {
                        gbDao.updateGuestbookCensorStatus( Long.valueOf( id ), flag );
                    }
                    else if( "open".equals( action ) )
                    {
                        gbDao.updateGuestbookOpenStatus( Long.valueOf( id ), flag );
                    }
                }
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
            clear();
        }
    }

    /**
     * 删除给定ID的留言信息
     * 
     * @param idList
     */
    public void deleteGuestbookAllInfoByIds( List idList, GuestbookConfigBean cfgBean,
        SiteGroupBean site )
    {
        if( cfgBean == null || idList == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            deleteGuestbookInfoByIdsNoTran( idList, cfgBean, site );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            clear();
        }
    }

    /**
     * 根据configId删除留言配置和其所属留言信息
     * 
     * @param idList
     */
    public void deleteGuestbookConfigAllInfoByIds( List idList, SiteGroupBean site )
    {
        try
        {
            mysqlEngine.beginTransaction();

            long configId = -1;

            GuestbookConfigBean cfgBean = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                if( idList.get( i ) instanceof String )
                {
                    configId = StringUtil.getLongValue( ( String ) idList.get( i ), -1 );
                }
                else
                {
                    configId = ( ( Long ) idList.get( i ) ).longValue();
                }

                cfgBean = gbDao.querySingleGuestbookConfigBeanByConfigId( Long.valueOf( configId ) );

                if( configId > 0 )
                {
                    // 删除留言所有信息
                    deleteGuestbookInfoByIdsNoTran( gbDao.queryAllGuestbookIdList( Long
                        .valueOf( configId ) ), cfgBean, site );

                    // 删除配置信息
                    gbDao.deleteGuestbookConfigById( Long.valueOf( configId ) );

                }
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            clear();

            clearCfg();
        }
    }

    public void sendNeedReplyMessage( Long senderId, Long userId, Long gbId )
    {
        // 发送系统消息

        List uidList = new ArrayList();

        uidList.add( userId );

        GuestbookMainInfoBean gb = gbDao.querySingleGuestbookMainInfoByGbid( gbId );

        if( gb != null )
        {
            SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                .getCurrentLoginSiteInfo();

            String info = "来自站点 '" + site.getSiteName() + "' 标题为 '" + gb.getGbTitle()
                + "' 的留言需要您来处理。";

            messageService.sendManagerMessage( senderId, "指定回复留言事件消息", site.getSiteName()
                + "的留言回复请求 留言ID : " + gb.getGbId(), info, uidList );
        }

    }

   
    private void deleteGuestbookInfoByIdsNoTran( List idList, GuestbookConfigBean cfgBean,
        SiteGroupBean site )
    {
        long id = -1;

        for ( int i = 0; i < idList.size(); i++ )
        {
            if( idList.get( i ) instanceof String )
            {
                id = StringUtil.getLongValue( ( String ) idList.get( i ), -1 );
            }
            else
            {
                id = ( ( Long ) idList.get( i ) ).longValue();
            }

            if( id > 0 )
            {
                metaDataService.deleteAndClearDefModelInfo( id, cfgBean.getInfoModelId(), site
                    .getSiteFlag() );

                gbDao.deleteGuestbookInfoById( Long.valueOf( id ) );

            }
        }
    }

    public static void clear()
    {
        countCache.clearAllEntry();
        gbCache.clearAllEntry();
 
    }

    
    public static void clearCfg()
    {
        gbCfgCache.clearAllEntry();
    }

   
}
