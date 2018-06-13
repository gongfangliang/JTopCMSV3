package cn.com.mjsoft.cms.content.job;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import cn.com.mjsoft.cms.behavior.InitSiteGroupInfoBehavior;
import cn.com.mjsoft.cms.behavior.JtRuntime;
import cn.com.mjsoft.cms.channel.service.ChannelService;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.content.controller.MultipleUploadController;
import cn.com.mjsoft.cms.content.dao.ContentDao;
import cn.com.mjsoft.cms.content.service.ContentService;
import cn.com.mjsoft.cms.resources.service.ResourcesService;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.cms.site.service.SiteGroupService;
import cn.com.mjsoft.cms.stat.controller.ClientQuerySiteVisStatController;
import cn.com.mjsoft.cms.stat.service.StatService;
import cn.com.mjsoft.framework.cache.jsr14.ReadWriteLockHashMap;
import cn.com.mjsoft.framework.security.authorization.AuthorizationHandler;
import cn.com.mjsoft.framework.util.DateAndTimeUtil;
import cn.com.mjsoft.framework.util.MailAndSmsUtil;
import cn.com.mjsoft.framework.util.StringUtil;

public class PublishAndDisposeContentJob implements Job
{
    private static Logger log = Logger.getLogger( PublishAndDisposeContentJob.class );

    private static ChannelService channelService = ChannelService.getInstance();

    private static ContentService contentService = ContentService.getInstance();

    private static SiteGroupService siteService = SiteGroupService.getInstance();

    private static ResourcesService resService = ResourcesService.getInstance();

    private static StatService statService = StatService.getInstance();

    private static Map excuteJob = new ReadWriteLockHashMap();

    public void execute( JobExecutionContext jobContent ) throws JobExecutionException
    {
        if( excuteJob.containsKey( jobContent.getJobDetail().getKey() ) )
        {
            log.info( "[PublishAndDisposeContentJob] ...waiting..."
                + jobContent.getJobDetail().getKey() );
            return;
        }

        String uuidKey = StringUtil.getUUIDString();

        try
        {
            excuteJob.put( jobContent.getJobDetail().getKey(), Boolean.TRUE );

            log.info( "[PublishAndDisposeContentJob] ...execute start..."
                + jobContent.getJobDetail().getKey() );

            String cmsPath = JtRuntime.cmsServer.getDomainFullPath();

            AuthorizationHandler.setInnerAccessFlag( uuidKey );

            List siteBeanList = InitSiteGroupInfoBehavior.siteGroupListCache;

            Timestamp currTime = DateAndTimeUtil.getTodayTimestampDayAndTime();

            SiteGroupBean site = null;

            URL targetUrl = null;

            List contentInfoList = null;

            for ( int i = 0; i < siteBeanList.size(); i++ )
            {
                site = ( SiteGroupBean ) siteBeanList.get( i );

                contentInfoList = contentService.retrieveWaitPublishContentBySiteId( site
                    .getSiteId(), currTime );

                Map contentInfo = null;

                for ( int j = 0; j < contentInfoList.size(); j++ )
                {
                    contentInfo = ( Map ) contentInfoList.get( j );

                    contentService.addWaitPublishIdTemp( ( Long ) contentInfo.get( "contentId" ),
                        ( Double ) contentInfo.get( "orderIdFlag" ), ( Long ) contentInfo
                            .get( "classId" ) );
                    contentService.updateWaitPublishContentSuccessStatus( contentInfo, currTime );

                    List tagIdList = StringUtil.changeStringToList( ( String ) contentInfo
                        .get( "tagKey" ), "\\*" );

                    Long tagId = null;

                    channelService.deleteTagRelateContentByContentId( ( Long ) contentInfo
                        .get( "contentId" ) );

                    for ( int x = 0; x < tagIdList.size(); x++ )
                    {
                        if( tagIdList.get( x ) instanceof Long )
                        {
                            tagId = ( Long ) tagIdList.get( x );
                        }
                        else
                        {
                            tagId = Long.valueOf( StringUtil.getLongValue( ( String ) tagIdList
                                .get( x ), -1 ) );
                        }

                        if( tagId.longValue() < 0 )
                        {
                            continue;
                        }

                        channelService.addTagWordRelateContent( tagId, ( Long ) contentInfo
                            .get( "contentId" ) );

                        channelService.updateTagWordRelateContentCount( tagId );
                    }
                }

                if( !contentInfoList.isEmpty() )
                {
                    log.info( "[PublishAndDisposeContentJob] 目标站存在待发内容,site:" + site.getSiteName()
                        + ", count:" + contentInfoList.size() );

                    // 默认端
                    targetUrl = new URL( cmsPath
                        + "/publish/generateContent.do?staticType=3&job=true&censor=wait&siteId="
                        + site.getSiteId() + "&currTime=" + currTime.getTime()
                        + "&innerAccessJtopSysFlag=" + uuidKey );

                    URLConnection URLconnection = targetUrl.openConnection();
                    HttpURLConnection httpConnection = ( HttpURLConnection ) URLconnection;

                    httpConnection.getInputStream();

                    // mob
                    targetUrl = new URL(
                        cmsPath
                            + "/publish/generateContent.do?mob=true&staticType=3&job=true&censor=wait&siteId="
                            + site.getSiteId() + "&currTime=" + currTime.getTime()
                            + "&innerAccessJtopSysFlag=" + uuidKey );

                    URLconnection = targetUrl.openConnection();
                    httpConnection = ( HttpURLConnection ) URLconnection;

                    httpConnection.getInputStream();

                    // pad
                    targetUrl = new URL(
                        cmsPath
                            + "/publish/generateContent.do?pad=true&staticType=3&job=true&censor=wait&siteId="
                            + site.getSiteId() + "&currTime=" + currTime.getTime()
                            + "&innerAccessJtopSysFlag=" + uuidKey );

                    URLconnection = targetUrl.openConnection();
                    httpConnection = ( HttpURLConnection ) URLconnection;

                    httpConnection.getInputStream();
                }
                else
                {
                    log.info( "[PublishAndDisposeContentJob] 目标站无待发内容,site:" + site.getSiteName() );
                }

                if( !contentInfoList.isEmpty() )
                {
                    ContentDao.releaseAllCountCache();
                    ContentService.releaseContentCache();
                }

                contentInfoList = contentService.retrieveWithdrawContentBySiteId( site.getSiteId(),
                    currTime );

                for ( int j = 0; j < contentInfoList.size(); j++ )
                {
                    contentInfo = ( Map ) contentInfoList.get( j );
                    contentService.updateWithdrawContentSuccessStatus( contentInfo );
                }

                if( !contentInfoList.isEmpty() )
                {
                    ContentDao.releaseAllCountCache();
                    ContentService.releaseContentCache();
                }

            }
            contentService.deleteWaitPublishIdTemp();

            Map mailInfo = siteService.retrieveSingleSiteEmailSendInfo();

            if( !mailInfo.isEmpty() )
            {
                String mailId = ( String ) mailInfo.get( "mailId" );

                try
                {
                    Long siteId = ( Long ) mailInfo.get( "siteId" );

                    site = ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupIdInfoCache
                        .getEntry( siteId );

                    if( site != null )
                    {
                        String mailHost = site.getSendMailHost();

                        String mail = site.getMail();

                        String mailUserName = site.getMailUserName();

                        String pw = site.getMailUserPW();

                        Integer sslFlag = site.getMailSSL();

                        boolean sslMode = false;
                        if( sslFlag != null )
                        {
                            sslMode = site.getMailSSL().intValue() == 1 ? true : false;
                        }

                        String sendToStr = ( String ) mailInfo.get( "sendTo" );

                        String subject = ( String ) mailInfo.get( "subject" );

                        String msg = ( String ) mailInfo.get( "mailContent" );

                        String[] sendTo = ( String[] ) StringUtil.changeStringToList( sendToStr,
                            "," ).toArray( new String[] {} );

                        MailAndSmsUtil.sendEmail( mailHost, null, sslMode,
                            Constant.SITE_CHANNEL.DEF_PAGE_CODE, sendTo, mail, mailUserName, mail,
                            pw, subject, msg );
                    }
                }
                finally
                {
                    siteService.deleteSiteEmailSendInfo( mailId );
                }
            }

            Calendar currentTime = Calendar.getInstance();

            int min = currentTime.get( Calendar.MINUTE );

            if( min % 5 == 0 )
            {

                resService.clearUselessResource();
            }

            MultipleUploadController.checkIpUpload();

            MultipleUploadController.clearBlackUploadIP();

            if( min == 5 )
            {
                ClientQuerySiteVisStatController.SITE_VIS_STAT.clear();
            }

            statService.transferVisitorStatInfoCacheToPe();

            log.info( "[PublishAndDisposeContentJob] ...execute over..."
                + jobContent.getJobDetail().getKey() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            excuteJob.remove( jobContent.getJobDetail().getKey() );
            AuthorizationHandler.romoveInnerAccessFlag( uuidKey );
        }

    }
}
