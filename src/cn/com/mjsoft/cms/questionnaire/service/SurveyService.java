package cn.com.mjsoft.cms.questionnaire.service;

import static cn.com.mjsoft.cms.common.ServiceUtil.cleanBasicHtmlByWhiteRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import cn.com.mjsoft.cms.cluster.adapter.ClusterListAdapter;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.ServiceUtil;
import cn.com.mjsoft.cms.common.datasource.MySqlDataSource;
import cn.com.mjsoft.cms.questionnaire.bean.SurveyBaseInfoBean;
import cn.com.mjsoft.cms.questionnaire.bean.SurveyGroupBean;
import cn.com.mjsoft.cms.questionnaire.bean.SurveyOptionInfoBean;
import cn.com.mjsoft.cms.questionnaire.dao.SurveyDao;
import cn.com.mjsoft.cms.questionnaire.dao.vo.SurveyBaseInfo;
import cn.com.mjsoft.cms.questionnaire.dao.vo.SurveyGroup;
import cn.com.mjsoft.cms.questionnaire.dao.vo.SurveyOptionInfo;
import cn.com.mjsoft.cms.questionnaire.dao.vo.SurveyVoteInfo;
import cn.com.mjsoft.cms.resources.service.ResourcesService;
import cn.com.mjsoft.cms.schedule.service.ScheduleService;
import cn.com.mjsoft.framework.persistence.core.PersistenceEngine;
import cn.com.mjsoft.framework.persistence.core.support.UpdateState;
import cn.com.mjsoft.framework.security.Auth;
import cn.com.mjsoft.framework.security.headstream.IUser;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.DateAndTimeUtil;
import cn.com.mjsoft.framework.util.MathUtil;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.wing.ServletUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class SurveyService
{
    private static Logger log = Logger.getLogger( SurveyService.class );

    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365 * 50;

    private ScheduleService scheduleService = ScheduleService.getInstance();

    private ResourcesService resService = ResourcesService.getInstance();

    public static final ClusterListAdapter voteDelayList = new ClusterListAdapter(
        "surveyService.statDelayList", SurveyVoteInfo.class );

    private static final int MAX_DELAY = 5;

    private static SurveyService service = null;

    public PersistenceEngine mysqlEngine = new PersistenceEngine( new MySqlDataSource() );

    private SurveyDao surveyDao;

    private SurveyService()
    {
        surveyDao = new SurveyDao( mysqlEngine );
    }

    private static synchronized void init()
    {
        if( null == service )
        {
            service = new SurveyService();
        }
    }

    public static SurveyService getInstance()
    {
        if( null == service )
        {
            init();
        }
        return service;
    }

    public void addNewSurveyGroup( SurveyGroup quest )
    {
        surveyDao.saveSurveyGroup( quest );

        SurveyDao.clearCache();
    }

    public Long retrieveAllSurveyGroupBeanCount( Long siteId )
    {
        return surveyDao.queryAllQuestBaseInfoBeanCount( siteId );
    }

    public List retrieveAllSurveyGroupBeanList( Long siteId, Long start, Integer size )
    {
        return surveyDao.queryAllQuestBaseInfoBeanList( siteId, start, size );
    }

    public SurveyGroupBean retrieveSingleSurveyGroupBeanByQuestId( Long questId )
    {
        return surveyDao.querySingleQuestBaseInfoBeanByQuestId( questId );
    }

    public SurveyGroupBean retrieveSingleQuestBaseInfoBeanByQuestFlag( String flag )
    {
        return surveyDao.querySingleQuestBaseInfoBeanByQuestFlag( flag );
    }

    public void editSurveyGroup( SurveyGroup quest )
    {
        surveyDao.updateSurveyGroup( quest );

        SurveyDao.clearCache();
    }

    public void addNewSurveyAndOption( SurveyBaseInfo survey, List optionList )
    {
        if( survey == null || optionList == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            Integer maxOrderFlag = surveyDao.queryMaxSurveyOrderFlag( survey.getGroupId() );

            if( maxOrderFlag != null )
            {
                survey.setOrderFlag( Integer.valueOf( maxOrderFlag.intValue() + 1 ) );
            }

            UpdateState updateState = surveyDao.saveSurveyBaseInfo( survey );

            Long surveyId = null;

            if( updateState.haveKey() )
            {
                surveyId = Long.valueOf( updateState.getKey() );

                SurveyOptionInfo option = null;

                for ( int i = 0; i < optionList.size(); i++ )
                {
                    option = ( SurveyOptionInfo ) optionList.get( i );
                    option.setSurveyId( surveyId );

                    surveyDao.saveSurveyOptionInfo( option );
                }
            }
            else
            {
                log.info( "[SurveyService] addNewSurveyAndOption : 增加survey失败,survey:" + survey );
                return;
            }

            updateVoteOptionPercent( surveyId );

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            SurveyDao.clearCache();
        }

    }

    public void editSurveyAndOption( SurveyBaseInfo survey, List optionList )
    {
        if( survey == null || optionList == null )
        {
            return;
        }

        try
        {
            mysqlEngine.beginTransaction();

            SurveyBaseInfoBean surveyBean = surveyDao
                .querySingleSurveyBaseInfoBeanBySurveyId( survey.getSurveyId() );

            if( surveyBean == null )
            {
                return;
            }

            surveyDao.updateSurveyBaseInfo( survey );

            surveyDao.deletesSurveyOptionBySurveyId( survey.getSurveyId() );

            SurveyOptionInfo option = null;
            for ( int i = 0; i < optionList.size(); i++ )
            {
                option = ( SurveyOptionInfo ) optionList.get( i );
                option.setSurveyId( survey.getSurveyId() );

                surveyDao.saveSurveyOptionInfo( option );
            }

            updateVoteOptionPercent( surveyBean.getSurveyId() );

            mysqlEngine.commit();

        }
        finally
        {
            mysqlEngine.endTransaction();

            SurveyDao.clearCache();
        }
    }

    public List retrieveSurveyBaseInfoBeanListByGroupId( Long groupId )
    {
        return surveyDao.querySurveyBaseInfoBeanListByGroupId( groupId );
    }

    public List retrieveSurveyBaseInfoBeanListByGroupFlag( String groupFlag )
    {
        return surveyDao.querySurveyBaseInfoBeanListByGroupFlag( groupFlag );
    }

    public SurveyBaseInfoBean retrieveSingleSurveyBaseInfoBeanBySurveyId( Long surveyId )
    {
        return surveyDao.querySingleSurveyBaseInfoBeanBySurveyId( surveyId );
    }

    public List retrieveSurveyOptionBeanListBySurveyId( Long surveyId )
    {
        return surveyDao.querySurveyOptionBeanListBySurveyId( surveyId );
    }

    public Long retrieveSurveyVoteInfoCountBySurveyId( Long surveyId )
    {
        return surveyDao.querySurveyVoteInfoCountBySurveyId( surveyId );
    }

    public List retrieveSurveyVoteInfoBySurveyId( Long surveyId, Long start, Integer size )
    {
        return surveyDao.querySurveyVoteInfoBySurveyId( surveyId, start, size );
    }

    public void addNewClientUserVoteInfo( List voteInfoList )
    {
        if( voteInfoList == null )
        {
            return;
        }

        for ( int i = 0; i < voteInfoList.size(); i++ )
        {
            voteDelayList.add( voteInfoList.get( i ) );
        }

        if( voteDelayList.size() >= MAX_DELAY )
        {
            scheduleService.startCollectUserVoteInfoJob();
        }
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void transferUserVoteInfoCacheToPe()
    {
        List tempInfoList = null;

        synchronized ( voteDelayList )
        {
            tempInfoList = new ArrayList( voteDelayList.getList() );
            voteDelayList.clear();
            log.info( "[SurveyService] transferUserVoteInfoCacheToPe : DelayList-isEmpty:"
                + voteDelayList.size() );
        }

        log.info( "[StatService] transferVisitorStatInfoCacheToPe : DelayList " + voteDelayList );

        Map voteCount = new HashMap();
        Integer count = null;
        Long optId = null;

        try
        {
            mysqlEngine.beginTransaction();

            mysqlEngine.startBatch();

            SurveyVoteInfo vote = null;

            Set<Long> sidSet = new HashSet<Long>();

            for ( int j = 0; j < tempInfoList.size(); j++ )
            {
                vote = ( SurveyVoteInfo ) tempInfoList.get( j );

                optId = vote.getOptId();

                sidSet.add( vote.getSurveyId() );

                if( optId.longValue() > 0 && StringUtil.isStringNull( vote.getVoteText() ) )
                {

                    count = ( Integer ) voteCount.get( optId );

                    if( count == null )
                    {
                        voteCount.put( optId, Integer.valueOf( 1 ) );
                    }
                    else
                    {
                        voteCount.put( optId, Integer.valueOf( count.intValue() + 1 ) );
                    }
                }
                else
                {
                    vote.setVoteText( cleanBasicHtmlByWhiteRule( vote.getVoteText() ) );

                    surveyDao.saveVoteInfo( vote );
                }

            }

            Iterator voteCountIter = voteCount.entrySet().iterator();

            Entry entry = null;
            while ( voteCountIter.hasNext() )
            {
                entry = ( Entry ) voteCountIter.next();
                surveyDao.updateVoteCount( ( Long ) entry.getKey(), ( Integer ) entry.getValue() );
            }

            mysqlEngine.executeBatch();

            List<Long> sidList = new ArrayList<Long>( sidSet );

            for ( Long sid : sidList )
            {
                updateVoteOptionPercent( sid );
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();
        }

        SurveyDao.clearCache();
    }

    public void swapSurveyOrder( Long surveyId, String act, String groupFlag )
    {
        try
        {
            mysqlEngine.beginTransaction();

            SurveyBaseInfoBean surveyBean = surveyDao
                .querySingleSurveyBaseInfoBeanBySurveyId( surveyId );

            if( surveyBean == null )
            {
                return;
            }

            if( "up".equals( act ) )
            {
                SurveyBaseInfoBean lessObj = surveyDao.queryFirstLessOrderFlagBySurveyId( surveyId,
                    groupFlag );

                if( lessObj != null )
                {
                    Integer currentFlag = surveyBean.getOrderFlag();

                    surveyDao.updateSurveyOrderFlag( surveyId, lessObj.getOrderFlag() );

                    surveyDao.updateSurveyOrderFlag( lessObj.getSurveyId(), currentFlag );
                }
            }
            else if( "down".equals( act ) )
            {
                SurveyBaseInfoBean bigObj = surveyDao.queryFirstBigOrderFlagBySurveyId( surveyId,
                    groupFlag );

                if( bigObj != null )
                {
                    Integer currentFlag = surveyBean.getOrderFlag();

                    surveyDao.updateSurveyOrderFlag( surveyId, bigObj.getOrderFlag() );

                    surveyDao.updateSurveyOrderFlag( bigObj.getSurveyId(), currentFlag );
                }
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            SurveyDao.clearCache();
        }
    }

    public Long retrieveSurveyGroupBeanCountByClassId( String classId, Long siteId )
    {
        return surveyDao.querySurveyGroupBeanCountByClassId( classId, siteId );
    }

    public List retrieveSurveyGroupBeanListByClassId( Long siteId, String classId, Long start,
        Integer size )
    {
        return surveyDao.querySurveyGroupBeanListByClassId( siteId, classId, start, size );
    }

    /**
     * 投票验证码
     * 
     * @param groupFlag
     * @param ip
     * @param flowContext
     * @return
     */
    public int judgeUserCaptcha( String groupFlag, HttpServletRequest request, Map params )
    {

        SurveyGroupBean groupBean = surveyDao.querySingleQuestBaseInfoBeanByQuestFlag( groupFlag );

        if( groupBean.getGroupId().longValue() < 0 )
        {
            return -2;
        }

        Integer capStatus = groupBean.getNeedCaptcha();

        if( capStatus.intValue() == 0 )
        {
            // 什么都不做
        }
        else if( capStatus.intValue() == 1 )
        {
            return ServiceUtil.checkCode( request, ( String ) params.get( "jtopcms-vote-captcha-"
                + groupFlag ) );
        }
        else if( capStatus.intValue() == 2 )
        {
            Auth auth = SecuritySessionKeeper.getSecuritySession().getAuth();

            IUser memberUser = ( IUser ) SecuritySessionKeeper.getSecuritySession().getMember();

            boolean notLoginMember = false;
            if( auth == null || !auth.isAuthenticated() || memberUser == null )
            {
                notLoginMember = true;
            }

            if( notLoginMember )
            {
                return ServiceUtil.checkCode( request, ( String ) params
                    .get( "jtopcms-vote-captcha-" + groupFlag ) );
            }
        }

        return 1;
    }

    /**
     * 根据限制投票的方式以及用户信息判断是否允许投票
     * 
     * @param groupFlag
     * @param remoteHost
     * @param flowContext
     * @return
     */
    public boolean judgeUserVote( String groupFlag, HttpServletRequest req, HttpServletResponse rep )
    {

        String ip = req.getRemoteAddr();

        SurveyGroupBean groupBean = surveyDao.querySingleQuestBaseInfoBeanByQuestFlag( groupFlag );

        if( groupBean.getGroupId().longValue() < 0 )
        {
            return false;
        }

        String currentTime = DateAndTimeUtil
            .getCunrrentDayAndTime( DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );

        Cookie cookie = ServletUtil.getCookie( req, "JTOPCMS-VOTE-INFO" );

        String voteInfo = null;

        if( Constant.SURVEY.RESTR_COOKIE_MODE.equals( groupBean.getRestriction() ) )
        {
            if( cookie == null )
            {

                ServletUtil.addCookie( rep, "JTOPCMS-VOTE-INFO", groupFlag + "="
                    + DateAndTimeUtil.getCunrrentDayAndTime( DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS )
                    + ",", COOKIE_MAX_AGE );

            }
            else
            {
                voteInfo = ( String ) cookie.getValue();

                String oldVoteInfo = null;
                if( voteInfo.indexOf( groupFlag + "=" ) != -1 )
                {
                    int keyPos = voteInfo.indexOf( groupFlag + "=" );
                    oldVoteInfo = StringUtil.subString( voteInfo, keyPos, voteInfo.indexOf( ",",
                        keyPos ) );

                    String[] flagAndTime = StringUtil.split( oldVoteInfo, "=" );

                    int hourInterval = DateAndTimeUtil.getHourInterval( currentTime,
                        flagAndTime[1], DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );

                    if( hourInterval < groupBean.getRestInterval().intValue() )
                    {
                        return false;
                    }
                    else
                    {

                        String newVoteInfo = StringUtil.replaceString( voteInfo, oldVoteInfo,
                            groupFlag + "=" + currentTime );

                        ServletUtil.addCookie( rep, "JTOPCMS-VOTE-INFO", newVoteInfo,
                            COOKIE_MAX_AGE );

                    }
                }
                else
                {
                    String newInfo = groupFlag
                        + "="
                        + DateAndTimeUtil
                            .getCunrrentDayAndTime( DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS ) + ",";

                    ServletUtil.addCookie( rep, "JTOPCMS-VOTE-INFO", voteInfo + newInfo,
                        COOKIE_MAX_AGE );
                }
            }
        }
        else if( Constant.SURVEY.RESTR_IP_MODE.equals( groupBean.getRestriction() ) )
        {
            String lastVoteDateTime = surveyDao.queryIpVoteTraceDateTime( ip, groupBean
                .getGroupId() );

            if( lastVoteDateTime == null )
            {
                surveyDao.saveIpVoteTrace( ip, groupBean.getGroupId(), currentTime );
            }
            else
            {
                int hourInterval = DateAndTimeUtil.getHourInterval( currentTime, lastVoteDateTime,
                    DateAndTimeUtil.DEAULT_FORMAT_YMD_HMS );

                if( hourInterval < groupBean.getRestInterval().intValue() )
                {
                    return false;
                }
                else
                {
                    surveyDao.updateIpVoteTrace( ip, groupBean.getGroupId(), currentTime );
                }
            }
        }

        return true;
    }

    public void deleteSurveyGroupInfo( List idList )
    {
        if( idList == null )
        {
            return;
        }

        Long id = null;

        List surveyList = null;

        SurveyBaseInfoBean surveyBean = null;

        try
        {
            mysqlEngine.beginTransaction();

            List optList = null;

            SurveyOptionInfoBean optBean = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                if( idList.get( i ) instanceof String )
                {
                    id = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ), -1 ) );
                }
                else
                {
                    id = ( Long ) idList.get( i );
                }

                if( id.longValue() < 0 )
                {
                    continue;
                }

                surveyDao.deleteSurveyGroupByGroupId( id );

                surveyList = surveyDao.querySurveyBaseInfoBeanListByGroupId( id );

                for ( int j = 0; j < surveyList.size(); j++ )
                {
                    surveyBean = ( SurveyBaseInfoBean ) surveyList.get( j );

                    optList = surveyDao.querySurveyOptionBeanListBySurveyIdNoCache( surveyBean
                        .getSurveyId() );

                    for ( int op = 0; op < optList.size(); op++ )
                    {
                        optBean = ( SurveyOptionInfoBean ) optList.get( op );

                        ServiceUtil.deleteSiteResTraceMode( Long.valueOf( StringUtil.getLongValue(
                            optBean.getOptionImageResId(), -1 ) ) );
                    }

                    surveyDao.deletesSurveyOptionBySurveyId( surveyBean.getSurveyId() );

                    surveyDao.deletesSurveyBaseInfoBySurveyId( surveyBean.getSurveyId() );

                    surveyDao.deleteSurveyVoteInfoBySurveyId( surveyBean.getSurveyId() );
                }
            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            SurveyDao.clearCache();
        }
    }

    public void deleteSurveyInfo( List idList )
    {
        Long id = null;

        try
        {
            mysqlEngine.beginTransaction();

            List optList = null;

            SurveyOptionInfoBean optBean = null;

            for ( int i = 0; i < idList.size(); i++ )
            {
                id = Long.valueOf( StringUtil.getLongValue( ( String ) idList.get( i ), -1 ) );

                if( id.longValue() < 0 )
                {
                    continue;
                }

                optList = surveyDao.querySurveyOptionBeanListBySurveyId( id );

                for ( int j = 0; j < optList.size(); j++ )
                {
                    optBean = ( SurveyOptionInfoBean ) optList.get( j );

                    resService.updateSiteResourceTraceUseStatus( Long.valueOf( StringUtil
                        .getLongValue( optBean.getOptionImageResId(), -1 ) ), Constant.COMMON.OFF );
                }

                surveyDao.deletesSurveyOptionBySurveyId( id );

                surveyDao.deletesSurveyBaseInfoBySurveyId( id );

                surveyDao.deleteSurveyVoteInfoBySurveyId( id );

            }

            mysqlEngine.commit();
        }
        finally
        {
            mysqlEngine.endTransaction();

            SurveyDao.clearCache();
        }
    }

    private void updateVoteOptionPercent( Long surveyId )
    {
        List optList = surveyDao.querySurveyOptionBeanListBySurveyIdNoCache( surveyId );

        if( optList == null )
        {
            return;
        }

        SurveyOptionInfoBean bean = null;

        int allVoteCount = 0;

        for ( int i = 0; i < optList.size(); i++ )
        {
            bean = ( SurveyOptionInfoBean ) optList.get( i );

            allVoteCount += bean.getVote().intValue();
        }

        if( allVoteCount > 0 )
        {
            for ( int i = 0; i < optList.size(); i++ )
            {
                bean = ( SurveyOptionInfoBean ) optList.get( i );

                bean.setVotePer( Double
                    .valueOf(
                        MathUtil.mul( MathUtil.div( bean.getVote().intValue(), allVoteCount, 3 ),
                            100 ) ).intValue() );

                surveyDao.updateVotePercent( bean.getOptionId(), Integer.valueOf( Double
                    .valueOf(
                        MathUtil.mul( MathUtil.div( bean.getVote().intValue(), allVoteCount, 3 ),
                            100 ) ).intValue() ) );
            }
        }
    }

}
