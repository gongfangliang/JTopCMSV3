package cn.com.mjsoft.cms.content.html;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

import cn.com.mjsoft.cms.channel.bean.ContentClassBean;
import cn.com.mjsoft.cms.channel.service.ChannelService;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.framework.util.SystemSafeCharUtil;
import cn.com.mjsoft.cms.common.page.Page;
import cn.com.mjsoft.cms.content.service.ContentService;
import cn.com.mjsoft.cms.metadata.bean.DataModelBean;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.cms.search.service.SearchService;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.framework.security.session.SecuritySession;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.DateAndTimeUtil;
import cn.com.mjsoft.framework.util.StringUtil;

public class SystemManageContentListTag extends TagSupport
{
    private static final long serialVersionUID = 5994924559655778847L;

    private static Logger log = Logger.getLogger( SystemManageContentListTag.class );

    private static final long NO_ID_FLAG = -1;

    private static DecimalFormat decimalFormat = new DecimalFormat( Constant.CONTENT.ORDER_FORMAT );

    private static ContentService contentService = ContentService.getInstance();

    private static ChannelService channelService = ChannelService.getInstance();

    private static MetaDataService metaDataService = MetaDataService.getInstance();

    private static SearchService searchService = SearchService.getInstance();

    private String key = "";

    private String field = "";

    private String form = "false";

    private String relateSearch = "false";

    // 模型名称
    private String modelName = "";

    // 栏目ID
    private String classId = "";

    // 获取指定数量的内容,目前不支持x,y这样的选取,使用此属性后,分页将无效
    private String limit;

    // 分页
    private String page;

    // 大小默认20
    private String pageSize = "16";

    // 自定义模型ID
    private String modelId = "-1";

    // 筛选分类属性
    private String filter = "";

    // 内容分类标识
    private String type = "";

    // 内容发布属性
    private String censorBy = "";

    // 内容创建者
    private String createBy = "";

    // 排序属性
    private String order = "";

    // 排序属性
    // private String orderBy = "";

    // 排序标准，默认降序
    // private String orderWay = "desc";

    private String startDate = "";

    private String endDate = "";

    public int doStartTag() throws JspException
    {
        log.info( "[ContentListTag] {classId}:" + classId + " ,{contentType}:" + modelName );

        String filterBy = filter;

        long targetClassId = -1;// -1默认行文为此模型的全表模式
        // 注意,标签中使用classId属性会比URL传递参数级别高
        if( StringUtil.isStringNotNull( classId ) )
        {
            targetClassId = StringUtil.getLongValue( classId, -1 );
        }

        ContentClassBean classBean = channelService.retrieveSingleClassBeanInfoByClassId( Long
            .valueOf( targetClassId ) );

        log.info( " targetClassId:" + targetClassId );

        DataModelBean modelBean = null;

        if( classBean.getClassId().longValue() > 0 )
        {
            modelBean = metaDataService
                .retrieveSingleDataModelBeanById( classBean.getContentType() );
        }
        else if( !"-1".equals( modelId ) )// 传入模型ID参数
        {
            modelBean = metaDataService.retrieveSingleDataModelBeanById( Long.valueOf( StringUtil
                .getLongValue( modelId, -1 ) ) );
        }
        else
        {
            modelBean = metaDataService.retrieveSingleDataModelBeanByName( modelName );
        }

        List contentList = Collections.EMPTY_LIST;

        if( StringUtil.isStringNotNull( key ) || StringUtil.isStringNotNull( field ) )
        {
            SecuritySession securitySession = SecuritySessionKeeper.getSecuritySession();

            SiteGroupBean siteBean = ( SiteGroupBean ) securitySession.getCurrentLoginSiteInfo();

            String keyVal = SystemSafeCharUtil.decodeFromWeb( key );

            int size = StringUtil.getIntValue( pageSize, 25 );

            int possibleCurrentPage = StringUtil.getIntValue( pageContext.getRequest()
                .getParameter( "currentPage" ), 1 );

            Long[] closeClassIds = null;

            if( "true".equals( relateSearch ) )
            {
                closeClassIds = channelService.retrieveSiteNotUseRelateFunClassId( siteBean
                    .getSiteFlag() );
            }

            String[] searchFieldArray = null;

            if( StringUtil.isStringNotNull( field ) )
            {
                searchFieldArray = ( String[] ) StringUtil.changeStringToList( field, "," )
                    .toArray( new String[] {} );
            }

            long closeId = StringUtil.getLongValue( this.pageContext.getRequest().getParameter(
                "closeCId" ), -1 );

            Object[] result = searchService
                .searchContentByKey( siteBean, searchFieldArray, modelBean != null ? modelBean
                    .getDataModelId() : Long.valueOf( StringUtil.getLongValue( pageContext
                    .getRequest().getParameter( "searchModelId" ), -1 ) ), targetClassId < 0 ? null
                    : Long.valueOf( targetClassId ), closeClassIds, ( closeId < 1 ) ? null : Long
                    .valueOf( closeId ), new String[] {}, keyVal, size, possibleCurrentPage, false,
                    true );

            if( "true".equals( form ) && modelBean != null )
            {
                contentList = metaDataService.retrieveFormDataByIds( modelBean.getDataModelId(),
                    ( List ) result[0] );
            }
            else
            {
                contentList = contentService
                    .retrieveSingleContentMainInfoBeanByIds( ( List ) result[0] );
            }

            if( contentList == null )
            {
                contentList = new ArrayList();
            }

            Set currIdSet = new HashSet();

            Map infoMap = null;

            for ( int si = 0; si < contentList.size(); si++ )
            {
                infoMap = ( Map ) contentList.get( si );

                Long id = ( Long ) infoMap.get( "contentId" );

                currIdSet.add( id.toString() );
            }

            List deleteIds = new ArrayList();

            for ( int si = 0; si < ( ( List ) result[0] ).size(); si++ )
            {
                String idStr = ( String ) ( ( List ) result[0] ).get( si );

                if( !currIdSet.contains( idStr ) )
                {
                    deleteIds.add( idStr );
                }
            }

            List deleteInfo = contentService
                .retrieveSingleTrashContentMainInfoBeanByIds( deleteIds );

            for ( int si = 0; si < deleteInfo.size(); si++ )
            {
                infoMap = ( Map ) deleteInfo.get( si );

                infoMap.put( "____CMS_DELETE_FOR_LUCENE____", "true" );
            }

            contentList.addAll( deleteInfo );

            this.pageContext.setAttribute( "___system_dispose_page_object___", result[1] );

            pageContext.setAttribute( "allContent", contentList );

            // 模型名
            pageContext.setAttribute( "contentType", modelName );

        }
        else if( modelBean != null || filterBy.length() > 0 )
        {
            log.info( "模型名称：" + ( ( modelBean != null ) ? modelBean.getModelName() : "" ) );

            pageContext.setAttribute( "currentModelBean", modelBean );

            Integer censorByVar = Integer.valueOf( StringUtil.getIntValue( censorBy, -9999 ) );

            if( "".equals( createBy ) )
            {
                createBy = "0";
            }

            if( "".equals( order ) )
            {
                order = "default-down";
            }

            String[] orderFlag = StringUtil.split( order, "-" );

            String orderByFlag = null;
            String orderWayFlag = null;

            String orderBy = null;// 时间,自然序列,点击等
            String orderWay = null;// desc asc
            String orderFilterFlag = null;// 数据过滤条件

            Object headOrderIdFlag = null;
            Object lastOrderIdFlag = null;

            boolean pageLimitMode = false;
            boolean noTopMode = false;

            if( orderFlag != null && orderFlag.length > 1 )
            {
                orderByFlag = orderFlag[0];
                orderWayFlag = orderFlag[1];

                if( Constant.CONTENT.UP_ORDER_WAY.equals( orderWayFlag ) )
                {
                    orderWay = "asc";
                }
                else if( Constant.CONTENT.DOWN_ORDER_WAY.equals( orderWayFlag ) )
                {
                    orderWay = "desc";
                }

                if( Constant.CONTENT.DEFAULT_ORDER.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.DEFAULT_ORDER_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );
                    pageLimitMode = true;

                }
                else if( Constant.CONTENT.ID_ORDER.equals( orderByFlag ) )
                {
                    orderBy = "contentId";

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );
                    pageLimitMode = true;
                }
                else if( Constant.CONTENT.ADD_DATE_ORDER.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.ADD_DATE_ORDER_VAR;
                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "hof" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lof" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.PUB_DATE_ORDER.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.PUB_DATE_ORDER_VAR;

                    headOrderIdFlag = pageContext.getRequest().getParameter( "headOrderFlag" );

                    lastOrderIdFlag = pageContext.getRequest().getParameter( "lastOrderFlag" );

                    pageLimitMode = true;

                }
                else if( Constant.CONTENT.CLICK_COUNT_ORDER.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.CLICK_COUNT_ORDER_VAR;
                    orderFilterFlag = Constant.CONTENT.CLICK_COUNT_ORDER_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.CLICK_COUNT_ORDER_DAY.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.CLICK_COUNT_ORDER_DAY_VAR;
                    orderFilterFlag = Constant.CONTENT.CLICK_COUNT_ORDER_DAY_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.CLICK_COUNT_ORDER_WEEK.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.CLICK_COUNT_ORDER_WEEK_VAR;
                    orderFilterFlag = Constant.CONTENT.CLICK_COUNT_ORDER_WEEK_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.CLICK_COUNT_ORDER_MONTH.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.CLICK_COUNT_ORDER_MONTH_VAR;
                    orderFilterFlag = Constant.CONTENT.CLICK_COUNT_ORDER_MONTH_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.COMM_COUNT_ORDER.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.COMM_COUNT_ORDER_VAR;
                    orderFilterFlag = Constant.CONTENT.COMM_COUNT_ORDER_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.COMM_COUNT_ORDER_DAY.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.COMM_COUNT_ORDER_DAY_VAR;
                    orderFilterFlag = Constant.CONTENT.COMM_COUNT_ORDER_DAY_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.COMM_COUNT_ORDER_WEEK.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.COMM_COUNT_ORDER_WEEK_VAR;
                    orderFilterFlag = Constant.CONTENT.COMM_COUNT_ORDER_WEEK_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.COMM_COUNT_ORDER_MONTH.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.COMM_COUNT_ORDER_MONTH_VAR;
                    orderFilterFlag = Constant.CONTENT.COMM_COUNT_ORDER_MONTH_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.SUPPORT_ORDER.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.SUPPORT_ORDER_VAR;
                    orderFilterFlag = Constant.CONTENT.SUPPORT_ORDER_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }
                else if( Constant.CONTENT.AGAINST_ORDER.equals( orderByFlag ) )
                {
                    orderBy = Constant.CONTENT.AGAINST_ORDER_VAR;
                    orderFilterFlag = Constant.CONTENT.AGAINST_ORDER_VAR;

                    headOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "headOrderFlag" ), NO_ID_FLAG ) );

                    lastOrderIdFlag = Double.valueOf( StringUtil.getDoubleValue( pageContext
                        .getRequest().getParameter( "lastOrderFlag" ), NO_ID_FLAG ) );

                    pageLimitMode = true;
                    noTopMode = true;
                }

                if( Constant.CONTENT.HAME_IMG_FILTER.equals( filterBy ) )
                {

                    pageLimitMode = true;
                }
                else if( Constant.CONTENT.CHANNEL_IMG_FILTER.equals( filterBy ) )
                {

                    pageLimitMode = true;
                }
                else if( Constant.CONTENT.CLASS_IMG_FILTER.equals( filterBy ) )
                {

                    pageLimitMode = true;
                }
                else if( Constant.CONTENT.CONTENT_IMG_FILTER.equals( filterBy ) )
                {

                    pageLimitMode = true;
                }

                if( Constant.CONTENT.CREATOR_MY_FILTER.equals( createBy )
                    || Constant.CONTENT.CREATOR_OTHER_FILTER.equals( createBy ) )
                {
                    pageLimitMode = true;
                }
            }

            Timestamp startAddTS = null;
            Timestamp endAddTS = null;
            if( "" != startDate || "" != endDate )
            {
                startAddTS = DateAndTimeUtil.getTimestamp( startDate + " 00:00:00",
                    DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );

                endAddTS = DateAndTimeUtil.getTimestamp( endDate + " 23:59:59",
                    DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );

                if( startAddTS == null )
                {
                    startAddTS = Constant.CONTENT.MIN_DATE;
                }

                if( endAddTS == null )
                {
                    endAddTS = Constant.CONTENT.MAX_DATE;
                }

                if( endAddTS != null && startAddTS != null )
                {
                    if( endAddTS.getTime() - startAddTS.getTime() < 0 )
                    {
                    }
                }
            }

            log.info( " typeFalg:" + type );

            if( "true".equals( page ) && orderBy != null && orderWay != null )// 进入分页模式
            {
                log.info( "ContentListTag进入分页模式 orderBy:" + orderBy + ", orderWay:" + orderWay );

                int size = StringUtil.getIntValue( pageSize, 25 );
                log.info( "pagesize:" + size );

                Integer count = Integer.valueOf( 0 );

                String orgCode = "";

                boolean childMode = false;

                if( Constant.CONTENT.CREATOR_ORG.equals( createBy )
                    || Constant.CONTENT.CREATOR_ORG_CHILD.equals( createBy ) )
                {
                    orgCode = ( String ) SecuritySessionKeeper.getSecuritySession().getAuth()
                        .getOrgCode();
                }

                if( Constant.CONTENT.CREATOR_ORG_CHILD.equals( createBy ) )
                {
                    childMode = true;
                }

                String countOrderFilter = contentService.getOrderFilterByCreateBy( orderFilterFlag,
                    filterBy, createBy, orgCode, childMode );

                String orderFilter = countOrderFilter;

                if( StringUtil.isStringNotNull( countOrderFilter ) )
                {
                    count = contentService.getUserDefineContentAllCountOrderFilterMode( Long
                        .valueOf( targetClassId ), modelBean, type, censorByVar, startAddTS,
                        endAddTS, countOrderFilter );
                }
                else
                {
                    count = contentService.getUserDefineContentAllCount( Long
                        .valueOf( targetClassId ), modelBean, type, censorByVar, startAddTS,
                        endAddTS );
                }

                int possibleCurrentPage = StringUtil.getIntValue( pageContext.getRequest()
                    .getParameter( "currentPage" ), 1 );

                int pageAction = StringUtil.getIntValue( pageContext.getRequest().getParameter(
                    "pageAction" ), Constant.PAGE.PAGE_ACTION_HEAD );

                log.info( "[Page] currentPage:" + possibleCurrentPage );
                log.info( "[Page] pageAction:" + pageAction );

                if( pageAction == Constant.PAGE.PAGE_ACTION_NEXT )
                {
                    possibleCurrentPage += 1;
                }
                else if( pageAction == Constant.PAGE.PAGE_ACTION_PREV )
                {
                    possibleCurrentPage -= 1;
                }

                Page pageInfo = new Page( ( size == 0 ) ? Page.DEFAULT_PAGE_SIZE : size, count
                    .intValue(), possibleCurrentPage );

                if( pageAction == Constant.PAGE.PAGE_ACTION_NEXT )
                {
                    log.info( "[Page] 进入下一页动作,使用Last Flag" );

                    if( pageInfo.getCurrentPage() == pageInfo.getPageCount()
                        || pageInfo.getPageCount() == 0 )
                    {
                        log.info( "[Page] 已经是最后一页,强制进入尾页" );

                        if( pageInfo.getPageCount() == 0 )
                        {
                            pageInfo.setCurrentPage( 1 );
                        }

                        int endContentSize = pageInfo.getLastPageSize();

                        log.info( "[Page] 最后页内容的个数:" + endContentSize );

                        Object firstOrderIdFlag = null;

                        firstOrderIdFlag = contentService
                            .getMaxOrMinEndPageModeOrderIdByOrderByAndWay( orderBy, orderWay );

                        if( !pageLimitMode )
                        {
                            contentList = contentService
                                .retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag( true,
                                    modelBean, targetClassId, startAddTS, endAddTS,
                                    firstOrderIdFlag, type, censorByVar, orderBy, orderWay,
                                    pageInfo.getPageSize(), endContentSize,
                                    Constant.PAGE.PAGE_ACTION_END, pageInfo );
                        }
                        else
                        {
                            if( noTopMode )
                            {
                                contentList = contentService.retrieveLimitModeContent( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                            else
                            {
                                contentList = contentService.retrieveLimitModeContentTopMode( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                        }
                    }
                    else
                    {
                        if( !pageLimitMode )
                        {
                            contentList = contentService
                                .retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag( true,
                                    modelBean, targetClassId, startAddTS, endAddTS,
                                    lastOrderIdFlag, type, censorByVar, orderBy, orderWay, pageInfo
                                        .getPageSize(), pageInfo.getPageSize(),
                                    Constant.PAGE.PAGE_ACTION_NEXT, pageInfo );
                        }
                        else
                        {
                            if( noTopMode )
                            {
                                contentList = contentService.retrieveLimitModeContent( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                            else
                            {
                                contentList = contentService.retrieveLimitModeContentTopMode( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                        }
                    }
                }
                else if( pageAction == Constant.PAGE.PAGE_ACTION_PREV )// 上一页
                {
                    log.info( "[Page] 进入上一页模式,使用Head Flag:" );

                    if( pageInfo.getCurrentPage() == 1 )// 已经是第一页,强制进入首页
                    {

                        log.info( "[Page] 已经是第一页,强制进入首页" );

                        Object max = contentService.getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                            orderBy, orderWay );

                        if( !pageLimitMode )
                        {
                            contentList = contentService
                                .retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag( true,
                                    modelBean, targetClassId, startAddTS, endAddTS, max, type,
                                    censorByVar, orderBy, orderWay, pageInfo.getPageSize(),
                                    pageInfo.getPageSize(), Constant.PAGE.PAGE_ACTION_HEAD,
                                    pageInfo );
                        }
                        else
                        {

                            if( noTopMode )
                            {
                                contentList = contentService.retrieveLimitModeContent( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                            else
                            {
                                contentList = contentService.retrieveLimitModeContentTopMode( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                        }
                    }
                    else
                    {

                        if( !pageLimitMode )
                        {
                            contentList = contentService
                                .retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag( true,
                                    modelBean, targetClassId, startAddTS, endAddTS,
                                    headOrderIdFlag, type, censorByVar, orderBy, orderWay, pageInfo
                                        .getPageSize(), pageInfo.getPageSize(),
                                    Constant.PAGE.PAGE_ACTION_PREV, pageInfo );
                        }
                        else
                        {

                            if( noTopMode )
                            {
                                contentList = contentService.retrieveLimitModeContent( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                            else
                            {
                                contentList = contentService.retrieveLimitModeContentTopMode( true,
                                    modelBean, targetClassId, censorByVar, type, startAddTS,
                                    endAddTS, pageInfo.getFirstResult(), pageInfo.getPageSize(),
                                    orderFilter, orderBy, orderWay );
                            }
                        }
                    }
                }
                else if( pageAction == Constant.PAGE.PAGE_ACTION_HEAD )

                {
                    log.info( "[Page] 进入首页模式 [contentType]:" + modelName );

                    pageInfo.setCurrentPage( 1 );

                    Object max = contentService.getMaxOrMinHeadPageModeOrderIdByOrderByAndWay(
                        orderBy, orderWay );

                    if( !pageLimitMode )
                    {
                        contentList = contentService
                            .retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag( true,
                                modelBean, targetClassId, startAddTS, endAddTS, max, type,
                                censorByVar, orderBy, orderWay, pageInfo.getPageSize(), pageInfo
                                    .getPageSize(), Constant.PAGE.PAGE_ACTION_HEAD, pageInfo );
                    }
                    else
                    {

                        if( noTopMode )
                        {
                            contentList = contentService.retrieveLimitModeContent( true, modelBean,
                                targetClassId, censorByVar, type, startAddTS, endAddTS, pageInfo
                                    .getFirstResult(), pageInfo.getPageSize(), orderFilter,
                                orderBy, orderWay );
                        }
                        else
                        {
                            contentList = contentService.retrieveLimitModeContentTopMode( true,
                                modelBean, targetClassId, censorByVar, type, startAddTS, endAddTS,
                                pageInfo.getFirstResult(), pageInfo.getPageSize(), orderFilter,
                                orderBy, orderWay );
                        }
                    }
                }
                else if( pageAction == Constant.PAGE.PAGE_ACTION_END )
                {
                    log.info( "[Page] 进入尾页模式 [contentType]:" + modelName );

                    int endContentSize = pageInfo.getLastPageSize();

                    log.info( "[Page] 最后页内容的个数:" + endContentSize );

                    pageInfo.setCurrentPage( pageInfo.getPageCount() );

                    Object firstOrderIdFlag = null;

                    if( targetClassId == -1 )
                    {
                        if( Constant.CONTENT.USER_DEFINE.equals( modelName ) )
                        {

                        }

                    }
                    else
                    {

                        {
                            firstOrderIdFlag = contentService
                                .getMaxOrMinEndPageModeOrderIdByOrderByAndWay( orderBy, orderWay );
                        }

                    }

                    if( !pageLimitMode )
                    {
                        contentList = contentService
                            .retrieveLimitUserDefineContentByClassIDAndModelIdAndFlag( true,
                                modelBean, targetClassId, startAddTS, endAddTS, firstOrderIdFlag,
                                type, censorByVar, orderBy, orderWay, pageInfo.getPageSize(),
                                endContentSize, Constant.PAGE.PAGE_ACTION_END, pageInfo );
                    }
                    else
                    {

                        if( noTopMode )
                        {
                            contentList = contentService.retrieveLimitModeContent( true, modelBean,
                                targetClassId, censorByVar, type, startAddTS, endAddTS, pageInfo
                                    .getFirstResult(), pageInfo.getPageSize(), orderFilter,
                                orderBy, orderWay );
                        }
                        else
                        {
                            contentList = contentService.retrieveLimitModeContentTopMode( true,
                                modelBean, targetClassId, censorByVar, type, startAddTS, endAddTS,
                                pageInfo.getFirstResult(), pageInfo.getPageSize(), orderFilter,
                                orderBy, orderWay );
                        }
                    }
                }
                else if( pageAction == Constant.PAGE.PAGE_ACTION_JUMP )
                {
                    if( pageInfo.getPageCount() == 0 )
                    {
                        pageInfo.setCurrentPage( 1 );
                    }

                    if( noTopMode )
                    {
                        contentList = contentService.retrieveLimitModeContent( true, modelBean,
                            targetClassId, censorByVar, type, startAddTS, endAddTS, pageInfo
                                .getFirstResult(), pageInfo.getPageSize(), orderFilter, orderBy,
                            orderWay );
                    }
                    else
                    {
                        contentList = contentService.retrieveLimitModeContentTopMode( true,
                            modelBean, targetClassId, censorByVar, type, startAddTS, endAddTS,
                            pageInfo.getFirstResult(), pageInfo.getPageSize(), orderFilter,
                            orderBy, orderWay );
                    }

                }
                else
                {
                    throw new JspException( "错误的分页标记" );
                }

                if( contentList == null )
                {
                    contentList = new ArrayList( 1 );
                }

                log.info( "最终结果:" + contentList );

                String nextQuery = null;
                String prevQuery = null;

                String headQuery = null;
                String endQuery = null;
                String jumpQuery = null;

                {

                    String currentHeadOrderFlag = "";

                    String currentLastOrderFlag = "";

                    if( !contentList.isEmpty()
                        && !Constant.CONTENT.ADD_DATE_ORDER.equals( orderByFlag ) )

                    {
                        currentHeadOrderFlag = decimalFormat
                            .format( ( ( Map ) contentList.get( 0 ) ).get( orderBy ) );

                        currentLastOrderFlag = decimalFormat.format( ( ( Map ) contentList
                            .get( contentList.size() - 1 ) ).get( orderBy ) );
                    }

                    StringBuffer url = ( ( HttpServletRequest ) this.pageContext.getRequest() )
                        .getRequestURL();

                    nextQuery = new StringBuffer( url.toString() )
                        .append( "?classId=" + targetClassId )
                        // .append( "&modelId=" + modelId )
                        .append( "&typeBy=" + type ).append( "&orderBy=" + order ).append(
                            "&filterBy=" + filterBy ).append( "&createBy=" + createBy ).append(
                            "&censorBy=" + censorByVar ).append( "&filterStartDate=" + startDate )
                        .append( "&filterEndDate=" + endDate ).append(
                            "&currentPage=" + pageInfo.getCurrentPage() ).append( "&pageAction=1" )
                        .append( "&pageCount=" + pageInfo.getPageCount() ).append(
                            "&headOrderFlag=" + currentHeadOrderFlag ).append(
                            "&lastOrderFlag=" + currentLastOrderFlag ).toString();

                    prevQuery = new StringBuffer( url.toString() )
                        .append( "?classId=" + targetClassId )
                        // .append( "&modelId=" + modelId )
                        .append( "&typeBy=" + type ).append( "&orderBy=" + order ).append(
                            "&filterBy=" + filterBy ).append( "&createBy=" + createBy ).append(
                            "&censorBy=" + censorByVar ).append( "&filterStartDate=" + startDate )
                        .append( "&filterEndDate=" + endDate ).append(
                            "&currentPage=" + pageInfo.getCurrentPage() ).append( "&pageAction=-1" )
                        .append( "&pageCount=" + pageInfo.getPageCount() ).append(
                            "&headOrderFlag=" + currentHeadOrderFlag ).append(
                            "&lastOrderFlag=" + currentLastOrderFlag ).toString();

                    headQuery = new StringBuffer( url.toString() )
                        .append( "?classId=" + targetClassId )
                        // .append( "&modelId=" + modelId )
                        .append( "&typeBy=" + type ).append( "&orderBy=" + order ).append(
                            "&filterBy=" + filterBy ).append( "&createBy=" + createBy ).append(
                            "&censorBy=" + censorByVar ).append( "&filterStartDate=" + startDate )
                        .append( "&filterEndDate=" + endDate ).append(
                            "&currentPage=" + pageInfo.getCurrentPage() ).append( "&pageAction=2" )
                        .append( "&pageCount=" + pageInfo.getPageCount() ).append(
                            "&headOrderFlag=" + currentHeadOrderFlag ).append(
                            "&lastOrderFlag=" + currentLastOrderFlag ).toString();

                    endQuery = new StringBuffer( url.toString() )
                        .append( "?classId=" + targetClassId )
                        // .append( "&modelId=" + modelId )
                        .append( "&typeBy=" + type ).append( "&orderBy=" + order ).append(
                            "&filterBy=" + filterBy ).append( "&createBy=" + createBy ).append(
                            "&censorBy=" + censorByVar ).append( "&filterStartDate=" + startDate )
                        .append( "&filterEndDate=" + endDate ).append(
                            "&currentPage=" + pageInfo.getCurrentPage() ).append( "&pageAction=-2" )
                        .append( "&pageCount=" + pageInfo.getPageCount() ).append(
                            "&headOrderFlag=" + currentHeadOrderFlag ).append(
                            "&lastOrderFlag=" + currentLastOrderFlag ).toString();

                    jumpQuery = new StringBuffer( url.toString() ).append(
                        "?classId=" + targetClassId )

                    .append( "&typeBy=" + type ).append( "&orderBy=" + order ).append(
                        "&filterBy=" + filterBy ).append( "&createBy=" + createBy ).append(
                        "&censorBy=" + censorByVar ).append( "&filterStartDate=" + startDate )
                        .append( "&filterEndDate=" + endDate ).append( "&pageAction=3" ).toString();

                }

                pageInfo.setNextQuery( nextQuery );
                pageInfo.setPrevQuery( prevQuery );
                pageInfo.setHeadQuery( headQuery );
                pageInfo.setEndQuery( endQuery );
                pageInfo.setJumpQuery( jumpQuery );

                log.info( "[Page] 分页对象为:" + pageInfo );

                pageContext.getRequest().setAttribute( "page", pageInfo );

                pageContext.setAttribute( "allContent", contentList );

                pageContext.setAttribute( "contentType", modelName );

            }
            else
            {

                pageContext.setAttribute( "allContent", contentList );

                // 模型名
                pageContext.setAttribute( "contentType", modelName );

            }

        }

        return EVAL_BODY_INCLUDE;

    }

    public void setModelName( String modelName )
    {
        this.modelName = modelName;
    }

    public String getPage()
    {
        return page;
    }

    public void setPage( String page )
    {
        this.page = page;
    }

    public String getPageSize()
    {
        return pageSize;
    }

    public void setPageSize( String pageSize )
    {
        this.pageSize = pageSize;
    }

    public String getClassId()
    {
        return classId;
    }

    public void setClassId( String classId )
    {
        this.classId = classId;
    }

    public String getLimit()
    {
        return limit;
    }

    public void setLimit( String limit )
    {
        this.limit = limit;
    }

    public void setModelId( String modelId )
    {
        this.modelId = modelId;
    }

    public void setFilter( String filter )
    {
        this.filter = filter;
    }

    public void setStartDate( String startDate )
    {
        this.startDate = startDate;
    }

    public void setEndDate( String endDate )
    {
        this.endDate = endDate;
    }

    public void setCensorBy( String censorBy )
    {
        this.censorBy = censorBy;
    }

    public void setOrder( String order )
    {
        this.order = order;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public void setCreateBy( String createBy )
    {
        this.createBy = createBy;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public void setRelateSearch( String relateSearch )
    {
        this.relateSearch = relateSearch;
    }

    public void setField( String field )
    {
        this.field = field;
    }

    public void setForm( String form )
    {
        this.form = form;
    }

    public int doEndTag() throws JspException
    {
        pageContext.removeAttribute( "allContent" );
        pageContext.removeAttribute( "contentType" );
        return EVAL_PAGE;
    }

}
