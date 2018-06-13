package cn.com.mjsoft.cms.guestbook.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.guestbook.bean.GuestbookConfigBean;
import cn.com.mjsoft.cms.guestbook.dao.vo.GuestbookMainInfo;
import cn.com.mjsoft.cms.guestbook.service.GuestbookService;
import cn.com.mjsoft.cms.member.bean.MemberBean;
import cn.com.mjsoft.cms.metadata.bean.DataModelBean;
import cn.com.mjsoft.cms.metadata.bean.ModelPersistenceMySqlCodeBean;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.cms.site.service.SiteGroupService;
import cn.com.mjsoft.framework.security.Auth;
import cn.com.mjsoft.framework.security.session.SecuritySession;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.IPSeeker;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.wing.ServletUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
@Controller
@RequestMapping( "/guestbook" )
public class ClientAddGuestbookController
{
    private static GuestbookService gbService = GuestbookService.getInstance();

    private static MetaDataService metaDataService = MetaDataService.getInstance();

    @ResponseBody
    @RequestMapping( value = "/clientAddGb.do", method = { RequestMethod.POST } )
    public String clientAddGb( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestDecodeInfo( request );

        Map sessionMap = ServletUtil.sessionMap( request );

        GuestbookMainInfo gbInfo = ( GuestbookMainInfo ) ServletUtil.getValueObjectDecode( request,
            GuestbookMainInfo.class );

        // 审核和回复状态必须强制设定为否
        gbInfo.setIsReply( Constant.COMMON.OFF );
        gbInfo.setIsCensor( Constant.COMMON.OFF );

        GuestbookConfigBean configBean = gbService
            .retrieveSingleGuestbookConfigBeanByConfigFlag( ( String ) params.get( "sysConfigFlag" ) );

        if( configBean == null )
        {
            // 缺少配置
            return "-1";
        }

        if( Constant.COMMON.OFF.equals( configBean.getUseState() ) )
        {
            // 关闭留言
            return "-6";
        }

        // 强制输入验证码
        if( Constant.COMMON.ON.equals( configBean.getNeedVerifyCode() ) )
        {
            String randCode = ( String ) params.get( "sysCheckCode" );

            String code = ( String ) sessionMap
                .get( Constant.SITE_CHANNEL.RANDOM_INPUT_RAND_CODE_KEY );

            if( !randCode.equalsIgnoreCase( code ) )
            {
                // 验证码错误
                return "-3";
            }
        }

        // 留言人姓名
        SecuritySession session = SecuritySessionKeeper.getSecuritySession();

        if( session.getMember() != null )
        {
            MemberBean member = ( MemberBean ) session.getMember();

            gbInfo.setGbMan( member.getMemberName() );

            gbInfo.setMemberId( member.getMemberId() );
        }
        else if( StringUtil.isStringNull( gbInfo.getGbMan() ) )
        {
            // 姓名为空
            return "-2";
        }

        // 强制输入标题
        if( Constant.COMMON.ON.equals( configBean.getMustHaveTitle() ) )
        {
            if( StringUtil.isStringNull( gbInfo.getGbTitle() ) )
            {
                // 标题为空
                return "-4";
            }
        }

        // 必须是会员登陆
        if( Constant.COMMON.ON.equals( configBean.getMustLogin() ) )
        {
            Auth auth = SecuritySessionKeeper.getSecuritySession().getAuth();

            Object memberUser = SecuritySessionKeeper.getSecuritySession().getMember();

            if( auth == null || !auth.isAuthenticated() || memberUser == null )
            {
                // 非登陆会员
                return "-5";
            }
        }

        try
        {

            String ip = IPSeeker.getIp( request );

            SiteGroupBean site = SiteGroupService.getCurrentSiteInfoFromWebRequest( request );

            gbInfo.setIp( ip );

            gbInfo.setSiteId( site.getSiteId() );

            gbInfo.setConfigId( configBean.getConfigId() );

            // 扩展模型信息
            List filedBeanList = metaDataService.retrieveModelFiledInfoBeanList( configBean
                .getInfoModelId() );

            ModelPersistenceMySqlCodeBean sqlCodeBean = metaDataService
                .retrieveSingleModelPerMysqlCodeBean( configBean.getInfoModelId() );

            DataModelBean model = metaDataService.retrieveSingleDataModelBeanById( configBean
                .getInfoModelId() );

            gbService.addNewGuestbookInfo( gbInfo, model, filedBeanList, sqlCodeBean, params );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // 系统错误
            return "-1";
        }

        // 成功
        // 清空验证码表
        if( sessionMap != null )
        {
            sessionMap.put( Constant.SITE_CHANNEL.RANDOM_INPUT_RAND_CODE_KEY, "" );
        }

        // 设置成功标志
        request.setAttribute( "successFlag", Boolean.TRUE );

        return "1";

    }

}
