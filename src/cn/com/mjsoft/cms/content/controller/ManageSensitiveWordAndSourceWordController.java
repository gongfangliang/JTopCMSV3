package cn.com.mjsoft.cms.content.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

 
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.spring.annotation.ActionInfo;
import cn.com.mjsoft.cms.content.service.ContentService;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.wing.ServletUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
@Controller
@RequestMapping( "/content" )
public class ManageSensitiveWordAndSourceWordController
{

    private static ContentService contentService = ContentService.getInstance();

    @ResponseBody
    @RequestMapping( value = "/importSW.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "批量导入敏感词汇", token = true )
    public String importSensitiveWord( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        String sword = ( String ) params.get( "sword" );

        contentService.importSensitiveWord( sword );

        return "success";

    }

    @RequestMapping( value = "/addSw.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "添加敏感词汇", token = true )
    public Object addSw( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        String sensitive = ( String ) params.get( "sensitiveStr" );

        String replace = ( String ) params.get( "replaceStr" );

        contentService.addNewSensitiveWord( sensitive, replace );

        Map returnParams = new HashMap();

        returnParams.put( "fromFlow", Boolean.TRUE );
        returnParams.put( "id", params.get( "sId" ) );

        return ServletUtil.redirect( "/core/words/CreateSensitiveWord.jsp", returnParams );

    }

    @RequestMapping( value = "/editSw.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "编辑敏感词汇", token = true )
    public Object editSw( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        String sensitive = ( String ) params.get( "sensitiveStr" );

        String replace = ( String ) params.get( "replaceStr" );

        Long sId = StringUtil.getLongValue( ( String ) params.get( "swId" ), -1 );

        contentService.editSensitiveWord( sensitive, replace, sId );

        Map returnParams = new HashMap();

        returnParams.put( "fromFlow", Boolean.TRUE );
        returnParams.put( "id", params.get( "sId" ) );

        return ServletUtil.redirect( "/core/words/EditSensitiveWord.jsp", returnParams );

    }

    @ResponseBody
    @RequestMapping( value = "/changeUs.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "改变敏感词汇状态", token = true )
    public String changeUs( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List idList = StringUtil.changeStringToList( ( String ) params.get( "ids" ), "," );

        String status = ( String ) params.get( "status" );

        Integer us = null;

        if( "on".equals( status ) )
        {
            us = Constant.COMMON.ON;
        }
        else
        {
            us = Constant.COMMON.OFF;
        }

        contentService.changeSensitiveWorduserStatus( idList, us );

        return "success";

    }

    @ResponseBody
    @RequestMapping( value = "/deleteSw.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "删除敏感词", token = true )
    public String deleteSw( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List idList = StringUtil.changeStringToList( ( String ) params.get( "ids" ), "," );

        contentService.deleteSensitiveWord( idList );

        return "success";

    }

    @RequestMapping( value = "/addCs.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "添加来源", token = true )
    public Object addCs( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        String sourceName = ( String ) params.get( "sourceName" );

        contentService.addNewContentSource( sourceName );

        Map returnParams = new HashMap();

        returnParams.put( "fromFlow", Boolean.TRUE );

        returnParams.put( "id", params.get( "sId" ) );

        return ServletUtil.redirect( "/core/words/CreateContentSource.jsp", returnParams );

    }

    @RequestMapping( value = "/editCs.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "编辑来源", token = true )
    public Object editCs( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        String sourceName = ( String ) params.get( "sourceName" );

        Long sId = Long.valueOf( StringUtil.getLongValue( ( String ) params.get( "sId" ), -1 ) );

        contentService.editContentSource( sourceName, sId );

        Map returnParams = new HashMap();

        returnParams.put( "fromFlow", Boolean.TRUE );

        returnParams.put( "id", params.get( "sId" ) );

        return ServletUtil.redirect( "/core/words/EditContentSource.jsp", returnParams );

    }

    @ResponseBody
    @RequestMapping( value = "/deleteCs.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "删除来源", token = true )
    public String deleteCs( HttpServletRequest request, HttpServletResponse response )
    {
        Map params = ServletUtil.getRequestInfo( request );

        List idList = StringUtil.changeStringToList( ( String ) params.get( "ids" ), "," );

        contentService.deleteContentSource( idList );

        return "success";

    }

    @ResponseBody
    @RequestMapping( value = "/disposeCs.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "从采集结果获取来源" )
    public String disposeCs( HttpServletRequest request, HttpServletResponse response )
    {
        contentService.checkAndDisposePickTraceSourceInfo();

        return "success";
    }

}
