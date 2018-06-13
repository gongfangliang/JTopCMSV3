package cn.com.mjsoft.cms.schedule.service;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import cn.com.mjsoft.cms.behavior.InitSiteGroupInfoBehavior;
import cn.com.mjsoft.cms.block.bean.BlockInfoBean;
import cn.com.mjsoft.cms.block.dao.BlockDao;
import cn.com.mjsoft.cms.block.dao.vo.BlockInfo;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.datasource.MySqlDataSource;
import cn.com.mjsoft.cms.pick.bean.PickContentTaskBean;
import cn.com.mjsoft.cms.pick.controller.ManagePickTaskController;
import cn.com.mjsoft.cms.pick.dao.PickDao;
import cn.com.mjsoft.cms.schedule.bean.ScheduleJobDetailBean;
import cn.com.mjsoft.cms.schedule.dao.ScheduleDao;
import cn.com.mjsoft.cms.schedule.dao.vo.ScheduleJobDetail;
import cn.com.mjsoft.cms.search.dao.SearchDao;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.cms.site.dao.SiteGroupDao;
import cn.com.mjsoft.cms.site.service.FileMonitorListener;
import cn.com.mjsoft.framework.config.SystemRuntimeConfig;
import cn.com.mjsoft.framework.config.impl.SystemConfiguration;
import cn.com.mjsoft.framework.exception.FrameworkException;
import cn.com.mjsoft.framework.persistence.core.PersistenceEngine;
import cn.com.mjsoft.framework.persistence.core.support.UpdateState;
import cn.com.mjsoft.framework.util.FileMonitorUtil;
import cn.com.mjsoft.framework.util.ScheduleJobUtil;
import cn.com.mjsoft.framework.util.StringUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class ScheduleService
{
    private static Logger log = Logger.getLogger( ScheduleService.class );

    public static final String FILE_TRANSFER_DATA_COLLECT_CLASS_NAME = "cn.com.mjsoft.cms.site.job.FileTransferDataCollectJob";

    public static final String FILE_TRANSFER_FILE_CLASS_NAME = "cn.com.mjsoft.cms.site.job.FileTransferDataJob";

    public static final String CREATE_INDEX_CLASS_NAME = "cn.com.mjsoft.cms.search.job.DisposeSearchIndexDataJob";

    public static final String COLLECT_VISITOR_INFO_CLASS_NAME = "cn.com.mjsoft.cms.stat.job.CollectVisitorInfoAndAnalyseJob";

    public static final String COLLECT_COLL_TFLOW_RACE_INFO_CLASS_NAME = "cn.com.mjsoft.cms.stat.job.CollectFlowTraceJob";

    public static final String COLLECT_USER_VOTE_INFO_CLASS_NAME = "cn.com.mjsoft.cms.questionnaire.job.CollectUserVoteJob";

    public static final String P_D_CONTENT_CLASS_NAME = "cn.com.mjsoft.cms.content.job.PublishAndDisposeContentJob";

    public static final String PUBLISH_BLOCK_CLASS_NAME = "cn.com.mjsoft.cms.block.job.PublishBlockJob";

    public static final String PICK_WEB_CLASS_NAME = "cn.com.mjsoft.cms.pick.job.PickWebContentJob";

    public static final String CLUSTER_MASTER_EXE_CMD_CLASS_NAME = "cn.com.mjsoft.cms.cluster.job.ClusterMasterExeCmdJob";

    public PersistenceEngine mysqlEngine = new PersistenceEngine( new MySqlDataSource() );

    private static Scheduler scheduler;

    static
    {
        try
        {
            // Properties properties = new Properties();
            // properties.put("org.quartz.scheduler.skipUpdateCheck","true");
            //
            // SchedulerFactory schedulerFactory = new
            // StdSchedulerFactory(properties);

            scheduler = StdSchedulerFactory.getDefaultScheduler();

        }
        catch ( SchedulerException e )
        {
            e.printStackTrace();
        }

    }

    public static Scheduler getScheduler()
    {
        return scheduler;
    }

    private static ScheduleService service = null;

    private ScheduleDao scheduleDao = null;

    private SiteGroupDao siteGroupDao = null;

    private SearchDao searchDao = null;

    private BlockDao blockDao = null;

    private PickDao pickDao = null;

    private ScheduleService()
    {
        scheduleDao = new ScheduleDao( mysqlEngine );
        siteGroupDao = new SiteGroupDao( mysqlEngine );
        searchDao = new SearchDao( mysqlEngine );
        blockDao = new BlockDao( mysqlEngine );
        pickDao = new PickDao( mysqlEngine );
    }

    private static synchronized void init()
    {
        if( null == service )
        {
            service = new ScheduleService();
        }
    }

    public static ScheduleService getInstance()
    {
        if( null == service )
        {
            init();
        }
        return service;
    }

    public void updateJobExecuteDT( Long jobId, Timestamp dt )
    {
        scheduleDao.updateJobExecuteDate( jobId, dt );
    }

    public void addPickWebContentScheduleJobDetail( Long taskId, Long siteId, Integer pType,
        Integer pVlaue )
    {

        // 持久化保存任务执行计划
        ScheduleJobDetail pickBlockJobDetail = new ScheduleJobDetail();
        pickBlockJobDetail.setJobDesc( "站点采集:" + siteId + ", task id:" + taskId );
        pickBlockJobDetail.setJobExecuteClass( PICK_WEB_CLASS_NAME );
        pickBlockJobDetail.setJobName( "PickWebContent" );
        pickBlockJobDetail.setPeriodSegment( pType );
        pickBlockJobDetail.setPeriodVar( pVlaue );
        pickBlockJobDetail.setTriggerType( Constant.JOB.TRIGGER_PERIOD );
        pickBlockJobDetail.setSystemJob( Constant.COMMON.OFF );
        pickBlockJobDetail.setUseState( Constant.COMMON.ON );
        pickBlockJobDetail.setSiteId( siteId );

        UpdateState dbState = scheduleDao.saveScheduleJobDetail( pickBlockJobDetail );

        if( !dbState.haveKey() )
        {
            throw new FrameworkException( "持久层增加任务信息失败! jobDetail:" + pickBlockJobDetail );
        }
        else
        {
            pickDao.updateTaskJobId( taskId, Long.valueOf( dbState.getKey() ) );

            // 启动任务

            JobKey publishJobKey = new JobKey( ManagePickTaskController.JOB_NAME + ": JobKey :"
                + Long.valueOf( dbState.getKey() ) );

            TriggerKey publishTriggerKey = new TriggerKey( ManagePickTaskController.JOB_NAME
                + ": TrriggerKey" + Long.valueOf( dbState.getKey() ) );

            buildPickWebScheduleAndStart( publishJobKey, publishTriggerKey, scheduler, pVlaue,
                pType, siteId, taskId, Long.valueOf( dbState.getKey() ) );
        }

    }

    // ///////////////////////////////以下为任务API交互///////////////////////////////////

    public void addNewPublishBlockJob( SiteGroupBean currentSiteBean, BlockInfo block )
    {
        // publish Key信息
        JobKey publishJobKey = new JobKey( "PublishBlock: JobKey :" + block.getBlockId() );

        TriggerKey publishTriggerKey = new TriggerKey( "PublishBlock: TrriggerKey"
            + block.getBlockId() );

        // 启动任务
        buildPublishBlockScheduleAndStart( publishJobKey, publishTriggerKey, scheduler, block
            .getPeriod(), block.getPeriodType(), block.getFlag(), currentSiteBean.getSiteId() );

        // 持久化保存任务执行计划
        ScheduleJobDetail publishBlockJobDetail = new ScheduleJobDetail();
        publishBlockJobDetail.setJobDesc( "站点:" + currentSiteBean.getSiteName() + ", 目标区块ID:"
            + block.getBlockId() );
        publishBlockJobDetail.setJobExecuteClass( PUBLISH_BLOCK_CLASS_NAME );
        publishBlockJobDetail.setJobName( "PublishBlock" );
        publishBlockJobDetail.setPeriodSegment( block.getPeriodType() );
        publishBlockJobDetail.setPeriodVar( block.getPeriod() );
        publishBlockJobDetail.setTriggerType( Constant.JOB.TRIGGER_PERIOD );
        publishBlockJobDetail.setSystemJob( Constant.COMMON.OFF );
        publishBlockJobDetail.setUseState( Constant.COMMON.ON );
        publishBlockJobDetail.setSiteId( currentSiteBean.getSiteId() );

        UpdateState dbState = scheduleDao.saveScheduleJobDetail( publishBlockJobDetail );

        if( !dbState.haveKey() )
        {
            try
            {
                ScheduleJobUtil.stopScheduleJob( scheduler, publishTriggerKey );
                ScheduleJobUtil.removeJob( scheduler, publishJobKey );
                ScheduleJobUtil.shutdown( scheduler );
            }
            catch ( SchedulerException e )
            {
                e.printStackTrace();
            }

            throw new FrameworkException( "持久层增加任务信息失败! jobDetail:" + publishBlockJobDetail );
        }
        else
        {
            blockDao.updateBlockJobId( block.getBlockId(), Long.valueOf( dbState.getKey() ) );
        }
    }

    public void updatePublishBlockJob( SiteGroupBean currentSiteBean, BlockInfo block )
    {
        // publish Key信息
        JobKey publishJobKey = new JobKey( "PublishBlock: JobKey :" + block.getBlockId() );

        TriggerKey publishTriggerKey = new TriggerKey( "PublishBlock: TrriggerKey"
            + block.getBlockId() );

        // 启动任务
        buildPublishBlockScheduleAndStart( publishJobKey, publishTriggerKey, scheduler, block
            .getPeriod(), block.getPeriodType(), block.getFlag(), currentSiteBean.getSiteId() );

        // 更新执行计划

        scheduleDao.updateScheduleJobDetail( block.getJobId(), block.getPeriodType(), block
            .getPeriod() );

    }

    public void updatePickWebJob( PickContentTaskBean taskBean )
    {
        // Key信息
        JobKey pickJobKey = new JobKey( ManagePickTaskController.JOB_NAME + ": JobKey :"
            + taskBean.getSelfJobId() );

        TriggerKey pickTriggerKey = new TriggerKey( ManagePickTaskController.JOB_NAME
            + ": TrriggerKey" + taskBean.getSelfJobId() );

        // 启动任务
        buildPickWebScheduleAndStart( pickJobKey, pickTriggerKey, scheduler, taskBean.getPeriod(),
            taskBean.getPeriodType(), taskBean.getSiteId(), taskBean.getPickTaskId(), taskBean
                .getSelfJobId() );

        // 更新执行计划

        scheduleDao.updateScheduleJobDetail( taskBean.getSelfJobId(), taskBean.getPeriodType(),
            taskBean.getPeriod() );

    }

    public void stopPublishBlockScheduleJob( Long blockId )
    {
        // publish Key信息
        JobKey publishJobKey = new JobKey( "PublishBlock: JobKey :" + blockId );

        TriggerKey publishTriggerKey = new TriggerKey( "PublishBlock: TrriggerKey" + blockId );

        try
        {
            ScheduleJobUtil.stopScheduleJob( scheduler, publishTriggerKey );
            ScheduleJobUtil.removeJob( scheduler, publishJobKey );
        }
        catch ( SchedulerException e )
        {
            e.printStackTrace();
        }

        log.info( "[PublishBlockJob] ...stoping..." + publishJobKey );
    }

    public void stopPickWebScheduleJob( Long jobId )
    {
        // Key信息
        JobKey pickJobKey = new JobKey( ManagePickTaskController.JOB_NAME + ": JobKey :" + jobId );

        TriggerKey pickTriggerKey = new TriggerKey( ManagePickTaskController.JOB_NAME
            + ": TrriggerKey" + jobId );

        try
        {
            ScheduleJobUtil.stopScheduleJob( scheduler, pickTriggerKey );
            ScheduleJobUtil.removeJob( scheduler, pickJobKey );
        }
        catch ( SchedulerException e )
        {
            e.printStackTrace();
        }

        log.info( "[stopPickWebScheduleJob] ...stoping..." + pickJobKey );
    }

    /**
     * 建立并执行文件监视数据收集任务.
     * 
     * @param jobKey
     * @param triggerKey
     * @param scheduler
     * @param siteId
     * @param siteRoot
     * @param gatewayId
     * @param fileListener
     */
    private void buildFileMonitorCollectEventDataScheduleAndStart( JobKey jobKey,
        TriggerKey triggerKey, Scheduler scheduler, String siteRoot, Long gatewayId, String path )
    {
        // 注册文件监听
        SystemRuntimeConfig config = SystemConfiguration.getInstance().getSystemConfig();

        File targetDir = new File( config.getSystemRealPath() + siteRoot + File.separator + path );

        if( !targetDir.exists() )
        {
            // throw new FrameworkException( "监听的目标文件夹不存在！targetDir:"
            // + targetDir.getPath() );
            log.error( "监听的目标文件夹不存在！targetDir:" + targetDir.getPath() );
            return;
        }

        FileMonitorListener fileListener = new FileMonitorListener();

        FileMonitorUtil.regNewFileMonitor( FileMonitorUtil.ALL_EVENT_MASK, true, fileListener,
            config.getSystemRealPath() + siteRoot + File.separator + path );

        // 放入job执行所需数据
        fileListener.setGatewayId( gatewayId );
        Map jobDataMap = new HashMap();
        jobDataMap.put( "fmListener", fileListener );
        // jobDataMap.put( "watchId", Integer.valueOf( watchId ) );

        buildSystemScheduleAndStart( FILE_TRANSFER_DATA_COLLECT_CLASS_NAME, jobKey, triggerKey,
            "SiteDirectoryMonitor", scheduler, jobDataMap, Integer.valueOf( 20 ),
            Constant.JOB.PERROID_SEC, false, false, null );

    }

    /**
     * 内容索引创建任务
     * 
     * @param jobKey
     * @param triggerKey
     * @param scheduler
     * @param siteId
     * @param transferPeriod
     * @param transferPeriodType
     */
    private void buildCreateContentIndexScheduleAndStart( JobKey jobKey, TriggerKey triggerKey,
        Scheduler scheduler, Long siteId, Integer transferPeriod, Integer transferPeriodType )
    {
        // 放入job执行所需数据

        Map jobDataMap = new HashMap();

        jobDataMap.put( "site", ( SiteGroupBean ) InitSiteGroupInfoBehavior.siteGroupIdInfoCache
            .getEntry( siteId ) );

        buildSystemScheduleAndStart( CREATE_INDEX_CLASS_NAME, jobKey, triggerKey,
            "CreateSearchIndex", scheduler, jobDataMap, transferPeriod, transferPeriodType, false,
            false, null );
    }

    /**
     * 收集用户信息并分析访问数据系统任务
     * 
     * @param jobKey
     * @param triggerKey
     * @param scheduler
     * @param siteId
     * @param transferPeriod
     * @param transferPeriodType
     */
    private void buildCollectVisitorInfoAndAnalyseScheduleAndStart( JobKey jobKey,
        TriggerKey triggerKey, Scheduler scheduler, String cron )
    {
        // 放入job执行所需数据
        Map paramMap = new HashMap();
        paramMap.put( "prevHour", Boolean.TRUE );
        paramMap.put( "analyse", Boolean.TRUE );
        buildSystemScheduleAndStart( COLLECT_VISITOR_INFO_CLASS_NAME, jobKey, triggerKey,
            "CollectVisitorInfoAndAnalyse", scheduler, paramMap, null, null, false, true, cron );
    }

    private void buildCollectVisitorInfoAndAnalyseScheduleAndStartAtOnce( JobKey jobKey,
        TriggerKey triggerKey, Scheduler scheduler )
    {
        // 放入job执行所需数据
        Map paramMap = new HashMap();
        paramMap.put( "prevHour", Boolean.FALSE );
        paramMap.put( "analyse", Boolean.FALSE );
        buildSystemScheduleAndStart( COLLECT_VISITOR_INFO_CLASS_NAME, jobKey, triggerKey,
            "CollectVisitorInfoAndAnalyse", scheduler, paramMap, null, null, true, false, null );
    }

    /**
     * 收集用户投票调查信息
     * 
     * @param jobKey
     * @param triggerKey
     * @param scheduler
     * @param siteId
     * @param transferPeriod
     * @param transferPeriodType
     */
    private void buildCollectUserVoteInfoScheduleAndStart( JobKey jobKey, TriggerKey triggerKey,
        Scheduler scheduler, String cron )
    {
        // 放入job执行所需数据
        Map paramMap = new HashMap();
        // paramMap.put( "prevHour", Boolean.TRUE );
        // paramMap.put( "analyse", Boolean.TRUE );
        buildSystemScheduleAndStart( COLLECT_USER_VOTE_INFO_CLASS_NAME, jobKey, triggerKey,
            "CollectUserVoteInfo", scheduler, paramMap, null, null, false, true, cron );
    }

    private void buildCollectUserVoteInfoScheduleAndStartAtOnce( JobKey jobKey,
        TriggerKey triggerKey, Scheduler scheduler )
    {
        // 放入job执行所需数据
        Map paramMap = new HashMap();
        // paramMap.put( "prevHour", Boolean.FALSE );
        // paramMap.put( "analyse", Boolean.FALSE );
        buildSystemScheduleAndStart( COLLECT_USER_VOTE_INFO_CLASS_NAME, jobKey, triggerKey,
            "CollectUserVoteInfo", scheduler, paramMap, null, null, true, false, null );
    }

    private void buildExeClusterMasterCMDScheduleAndStartAtOnce( JobKey jobKey,
        TriggerKey triggerKey, Scheduler scheduler, Map paramMap )
    {

        buildSystemScheduleAndStart( CLUSTER_MASTER_EXE_CMD_CLASS_NAME, jobKey, triggerKey,
            "ExeMasterClusterCMD", scheduler, paramMap, null, null, true, false, null );
    }

    /**
     * 发布等待发布的内容,并处理过期内容
     * 
     * @param jobKey
     * @param triggerKey
     * @param scheduler
     * @param siteId
     * @param transferPeriod
     * @param transferPeriodType
     */
    private void buildPublishAndDisposeContentScheduleAndStart( JobKey jobKey,
        TriggerKey triggerKey, Scheduler scheduler, Integer period, Integer periodType )
    {
        // 放入job执行所需数据
        Map paramMap = new HashMap();
        // paramMap.put( "prevHour", Boolean.TRUE );
        // paramMap.put( "analyse", Boolean.TRUE );
        buildSystemScheduleAndStart( P_D_CONTENT_CLASS_NAME, jobKey, triggerKey,
            "PublishAndDisposeContent", scheduler, paramMap, period, periodType, false, false, null );
    }

    /**
     * 区块发布任务
     * 
     * @param publishJobKey
     * @param publishTriggerKey
     * @param scheduler2
     * @param siteRoot
     * @param block
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private void buildPublishBlockScheduleAndStart( JobKey publishJobKey,
        TriggerKey publishTriggerKey, Scheduler scheduler, Integer period, Integer periodType,
        String blockFlag, Long siteId )
    {
        // 放入job执行所需数据
        Map paramMap = new HashMap();

        paramMap.put( "blockFlag", blockFlag );

        paramMap.put( "siteId", siteId );

        buildSystemScheduleAndStart( PUBLISH_BLOCK_CLASS_NAME, publishJobKey, publishTriggerKey,
            "PublishBlock", scheduler, paramMap, period, periodType, false, false, null );

    }

    /**
     * 采集任务
     * 
     * @param pickJobKey
     * @param publishTriggerKey
     * @param scheduler2
     * @param siteRoot
     * @param block
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private void buildPickWebScheduleAndStart( JobKey pickJobKey, TriggerKey pickTriggerKey,
        Scheduler scheduler, Integer period, Integer periodType, Long siteId, Long taskId,
        Long jobId )
    {
        // 放入job执行所需数据
        Map paramMap = new HashMap();

        paramMap.put( "jobId", jobId );

        paramMap.put( "taskId", taskId );

        paramMap.put( "siteId", siteId );

        buildSystemScheduleAndStart( PICK_WEB_CLASS_NAME, pickJobKey, pickTriggerKey,
            "PickWebContent", scheduler, paramMap, period, periodType, false, false, null );

    }

    public void resumeCreateContentIndexJob()
    {
        List siteList = siteGroupDao.queryAllSiteBean();
        SiteGroupBean bean = null;

        ScheduleJobDetailBean jobDetailBean = null;

        for ( int i = 0; i < siteList.size(); i++ )
        {
            // 每一个site都会配置一个索引任务
            bean = ( SiteGroupBean ) siteList.get( i );

            // Key信息
            JobKey jobKey = new JobKey( "DisposeSearchIndex: JobKey :" + bean.getSiteId() );

            TriggerKey triggerKey = new TriggerKey( "DisposeSearchIndex: TrriggerKey"
                + bean.getSiteId() );

            jobDetailBean = scheduleDao
                .querySingleSystemScheduleJobDetailBean( CREATE_INDEX_CLASS_NAME );

            buildCreateContentIndexScheduleAndStart( jobKey, triggerKey, scheduler, bean
                .getSiteId(), jobDetailBean.getPeriodVar(), jobDetailBean.getPeriodSegment() );
        }
    }

    public void startNewCreateContentIndexJob( Long siteId )
    {

        ScheduleJobDetailBean jobDetailBean = null;

        // 每一个site都会配置一个索引任务

        // Key信息
        JobKey jobKey = new JobKey( "DisposeSearchIndex: JobKey :" + siteId );

        TriggerKey triggerKey = new TriggerKey( "DisposeSearchIndex: TrriggerKey" + siteId );

        jobDetailBean = scheduleDao
            .querySingleSystemScheduleJobDetailBean( CREATE_INDEX_CLASS_NAME );

        buildCreateContentIndexScheduleAndStart( jobKey, triggerKey, scheduler, siteId,
            jobDetailBean.getPeriodVar(), jobDetailBean.getPeriodSegment() );

    }

    public void stopCreateContentIndexJob( Long siteId )
    {

        // 每一个site都会配置一个索引任务

        // Key信息
        JobKey jobKey = new JobKey( "DisposeSearchIndex: JobKey :" + siteId );

        TriggerKey triggerKey = new TriggerKey( "DisposeSearchIndex: TrriggerKey" + siteId );

        try
        {
            ScheduleJobUtil.stopScheduleJob( scheduler, triggerKey );
            ScheduleJobUtil.removeJob( scheduler, jobKey );
        }
        catch ( SchedulerException e )
        {
            log.error( e );
            e.printStackTrace();
        }

        log.info( "[CreateContentIndexJob] ...stoping..." + jobKey );
    }

    public void resumeCollectVisitorInfoAndAnalyseJob()
    {
        JobKey jobKey = new JobKey( "CollectVisitorInfoAndAnalyse: JobKey : system" );

        TriggerKey triggerKey = new TriggerKey(
            "CollectVisitorInfoAndAnalyse: TrriggerKey : system" );

        ScheduleJobDetailBean jobDetailBean = scheduleDao
            .querySingleSystemScheduleJobDetailBean( COLLECT_VISITOR_INFO_CLASS_NAME );

        buildCollectVisitorInfoAndAnalyseScheduleAndStart( jobKey, triggerKey, scheduler,
            jobDetailBean.getCronExpression() );
    }

    public void resumeCollectUserVoteInfoJob()
    {
        JobKey jobKey = new JobKey( "CollectUserVoteInfo: JobKey : system" );

        TriggerKey triggerKey = new TriggerKey( "CollectUserVoteInfo: TrriggerKey : system" );

        ScheduleJobDetailBean jobDetailBean = scheduleDao
            .querySingleSystemScheduleJobDetailBean( COLLECT_USER_VOTE_INFO_CLASS_NAME );

        buildCollectUserVoteInfoScheduleAndStart( jobKey, triggerKey, scheduler, jobDetailBean
            .getCronExpression() );
    }

    public void resumePublishAndDisposeContentJob()
    {
        JobKey jobKey = new JobKey( "PublishAndDisposeContent: JobKey : system" );

        TriggerKey triggerKey = new TriggerKey( "PublishAndDisposeContent: TrriggerKey : system" );

        ScheduleJobDetailBean jobDetailBean = scheduleDao
            .querySingleSystemScheduleJobDetailBean( P_D_CONTENT_CLASS_NAME );

        buildPublishAndDisposeContentScheduleAndStart( jobKey, triggerKey, scheduler, jobDetailBean
            .getPeriodVar(), jobDetailBean.getPeriodSegment() );
    }

    public void resumePublishBlockJob()
    {
        // 系统全部block
        List blockInfoBeanList = blockDao.queryAllBlockInfoBean();

        BlockInfoBean blockBean = null;
        Long jobId = null;
        ScheduleJobDetailBean jobDetailBean = null;
        for ( int i = 0; i < blockInfoBeanList.size(); i++ )
        {
            blockBean = ( BlockInfoBean ) blockInfoBeanList.get( i );

            jobId = blockBean.getJobId();

            if( jobId == null || jobId.longValue() <= 0 )
            {
                continue;
            }

            jobDetailBean = scheduleDao.querySingleScheduleJobDetailBean( jobId );

            if( jobDetailBean != null )
            {
                for ( int j = 0; j < 1; j++ )
                {
                    // publish Key信息
                    JobKey publishJobKey = new JobKey( "PublishBlock: JobKey :"
                        + blockBean.getBlockId() );

                    TriggerKey publishTriggerKey = new TriggerKey( "PublishBlock: TrriggerKey"
                        + blockBean.getBlockId() );

                    // 启动任务
                    buildPublishBlockScheduleAndStart( publishJobKey, publishTriggerKey, scheduler,
                        jobDetailBean.getPeriodVar(), jobDetailBean.getPeriodSegment(), blockBean
                            .getFlag(), blockBean.getSiteId() );
                }
            }
        }
    }

    public void resumePickWebHtmlJob()
    {
        List jobBeanList = scheduleDao
            .queryScheduleJobDetailBeanByName( ManagePickTaskController.JOB_NAME );

        ScheduleJobDetailBean jobDetailBean = null;

        for ( int i = 0; i < jobBeanList.size(); i++ )
        {
            // 停止1秒
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

            jobDetailBean = ( ScheduleJobDetailBean ) jobBeanList.get( i );

            startNewPickHtmlJob( jobDetailBean );
        }
    }

    /**
     * 启动新的采集任务
     * 
     * @param jobDetailBean
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void startNewPickHtmlJob( ScheduleJobDetailBean jobDetailBean )
    {
        if( jobDetailBean == null )
        {
            log.error( "[startNewPickHtmlJob] 无法获取job元信息,无法启动发布任务" );
            return;
        }

        // Key信息
        JobKey pickJobKey = new JobKey( ManagePickTaskController.JOB_NAME + ": JobKey :"
            + jobDetailBean.getJobId() );

        TriggerKey pickTriggerKey = new TriggerKey( ManagePickTaskController.JOB_NAME
            + ": TrriggerKey" + jobDetailBean.getJobId() );

        // 启动任务

        Map paramMap = new HashMap();

        paramMap.put( "jobId", jobDetailBean.getJobId() );

        Long taskId = pickDao.queryTaskIdByJobId( jobDetailBean.getJobId() );

        paramMap.put( "taskId", taskId );

        paramMap.put( "siteId", jobDetailBean.getSiteId() );

        buildSystemScheduleAndStart( jobDetailBean.getJobExecuteClass(), pickJobKey,
            pickTriggerKey, ManagePickTaskController.JOB_NAME, scheduler, paramMap, jobDetailBean
                .getPeriodVar(), jobDetailBean.getPeriodSegment(), false, false, null );

    }

    // ////////////////////////////以下为强制执行一次的方法///////////////////////////////////
    public void startCollectVisitorInfoAndAnalyseJob()
    {

        JobKey jobKey = new JobKey( "CollectVisitorInfoAndAnalyse: JobKey : system : atonce"
            + StringUtil.getUUIDString() );

        TriggerKey triggerKey = new TriggerKey(
            "CollectVisitorInfoAndAnalyse: TrriggerKey : system : atonce"
                + StringUtil.getUUIDString() );

        buildCollectVisitorInfoAndAnalyseScheduleAndStartAtOnce( jobKey, triggerKey, scheduler );
    }

    public void startCollectUserVoteInfoJob()
    {
        JobKey jobKey = new JobKey( "CollectUserVoteInfo: JobKey : system : atonce"
            + StringUtil.getUUIDString() );

        TriggerKey triggerKey = new TriggerKey(
            "CollectUserVoteInfo: TrriggerKey : system : atonce" + StringUtil.getUUIDString() );

        buildCollectUserVoteInfoScheduleAndStartAtOnce( jobKey, triggerKey, scheduler );
    }

    public void startExeClusterMasterCMDJob( Map paramMap )
    {
        JobKey jobKey = new JobKey( "ExeClusterMasterCMD: JobKey : system : atonce"
            + StringUtil.getUUIDString() );

        TriggerKey triggerKey = new TriggerKey(
            "ExeClusterMasterCMD: TrriggerKey : system : atonce" + StringUtil.getUUIDString() );

        buildExeClusterMasterCMDScheduleAndStartAtOnce( jobKey, triggerKey, scheduler, paramMap );
    }

    // ////////////////////////////以下为公用的方法///////////////////////////////////
    /**
     * 构造并启动一个任务
     * 
     * @param className
     * @param jobKey
     * @param triggerKey
     * @param triggerInfo
     * @param scheduler
     * @param jobDataMap
     * @param period
     * @param periodType
     * @param atOnce 是否是立即执行
     * @param cronMode cron表达式执行模式
     * @param cron cron表达式,需配合cronMode参数
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private void buildSystemScheduleAndStart( String className, JobKey jobKey,
        TriggerKey triggerKey, String triggerInfo, Scheduler scheduler, Map jobDataMap,
        Integer period, Integer periodType, boolean atOnce, boolean cronMode, String cron )
    {
        Class targetJobClass = null;

        try
        {
            targetJobClass = Class.forName( className );
        }
        catch ( ClassNotFoundException e )
        {
            targetJobClass = null;
            e.printStackTrace();
        }

        if( targetJobClass == null )
        {
            log.error( "[Service] buildSystemScheduleAndStart " + className + " : 无法获取Class信息" );
            return;
        }

        JobDetail transferJobDetail = ScheduleJobUtil.buildNewJobDetail( targetJobClass, jobKey,
            triggerInfo );

        // 放入job执行所需数据

        transferJobDetail.getJobDataMap().putAll( jobDataMap );

        // Trigger信息
        Trigger trigger = null;

        if( atOnce )
        {
            trigger = ScheduleJobUtil.buildNewAtOnceTrigger( triggerKey );
        }
        else if( cronMode )
        {
            trigger = ScheduleJobUtil.buildNewCronTrigger( triggerKey, cron );
        }
        else
        {
            trigger = ScheduleJobUtil.buildNewPeriodTrigger( triggerKey, periodType, period
                .intValue(), true );
        }

        try
        {
            ScheduleJobUtil.regScheduleJob( scheduler, transferJobDetail, trigger );

            ScheduleJobUtil.start( scheduler );
        }
        catch ( SchedulerException e )
        {
            e.printStackTrace();

        }

    }

}
