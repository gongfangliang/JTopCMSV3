package cn.com.mjsoft.cms.guestbook.controller;

import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.com.mjsoft.cms.common.spring.annotation.ActionInfo;
import cn.com.mjsoft.cms.guestbook.service.GuestbookService;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.framework.security.Auth;
import cn.com.mjsoft.framework.security.session.SecuritySession;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.util.SystemSafeCharUtil;
import cn.com.mjsoft.framework.web.wing.ServletUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
@Controller
@RequestMapping( "/guestbook" )
public class ReplyAndManageGuestbookInfoController
{
    private static GuestbookService gbService = GuestbookService.getInstance();

    @ResponseBody
    @RequestMapping( value = "/replayGbInfo.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "回复留言", token = true )
    public String replayGbInfo( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        Map params = ServletUtil.getRequestInfo( request );

        SecuritySession session = SecuritySessionKeeper.getSecuritySession();

        Auth auth = session.getAuth();

        Long gbId = Long.valueOf( StringUtil.getLongValue( ( String ) params.get( "gbId" ), -1 ) );

        String replyText = SystemSafeCharUtil.filterHTMLNotApos( URLDecoder.decode(
            ( String ) params.get( "replyText" ), "UTF-8" ) );

        gbService.replyGuestbook( gbId, replyText, ( String ) auth.getApellation() );

        return "success";
    }

    @ResponseBody
    @RequestMapping( value = "/changeStatus.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "切换留言状态", token = true )
    public String changeStatus( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        Map params = ServletUtil.getRequestInfo( request );

        String ids = ( String ) params.get( "id" );

        Integer flag = Integer
            .valueOf( StringUtil.getIntValue( ( String ) params.get( "flag" ), 0 ) );

        String action = ( String ) params.get( "action" );

        gbService.editGuestbookStatus( StringUtil.changeStringToList( ids, "," ), action, flag );

        return "success";
    }

    @ResponseBody
    @RequestMapping( value = "/sendReplyMsg.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "发送邀请留言信件" )
    public String sendReplyMsg( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        Map params = ServletUtil.getRequestInfo( request );

        SecuritySession session = SecuritySessionKeeper.getSecuritySession();

        Auth auth = session.getAuth();

        Long userId = StringUtil.getLongValue( ( String ) params.get( "userId" ), 0 );

        Long gbId = StringUtil.getLongValue( ( String ) params.get( "gbId" ), -1 );

        gbService.sendNeedReplyMessage( ( Long ) auth.getIdentity(), userId, gbId );

        return "success";

    }

    @ResponseBody
    @RequestMapping( value = "/deleteGb.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "删除留言", token = true )
    public String deleteGb( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        Map params = ServletUtil.getRequestInfo( request );

        String ids = ( String ) params.get( "id" );

        String cfgFlag = ( String ) params.get( "cfgFlag" );

        SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
            .getCurrentLoginSiteInfo();

        gbService.deleteGuestbookAllInfoByIds( StringUtil.changeStringToList( ids, "," ), gbService
            .retrieveSingleGuestbookConfigBeanByConfigFlag( cfgFlag ), site );

        return "success";
    }

}
