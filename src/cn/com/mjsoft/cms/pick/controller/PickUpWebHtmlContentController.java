package cn.com.mjsoft.cms.pick.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.com.mjsoft.cms.common.spring.annotation.ActionInfo;
import cn.com.mjsoft.cms.pick.service.PickService;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.wing.ServletUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
@Controller
@RequestMapping( "/pick" )
public class PickUpWebHtmlContentController
{

    private static PickService pickService = PickService.getInstance();

    @ResponseBody
    @RequestMapping( value = "/pickWeb.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "进行采集", token = false )
    public String deletePickTrace( HttpServletRequest request, HttpServletResponse response )
    {

        Map params = ServletUtil.getRequestInfo( request );

        boolean testMode = StringUtil.getBooleanValue( ( String ) params.get( "testMode" ), false );

        boolean singleMode = StringUtil.getBooleanValue( ( String ) params.get( "singleMode" ),
            false );

        if( testMode )
        {
            Map testValMap = pickService.pickUpWebContentForTestConfig( Long.valueOf( StringUtil
                .getLongValue( ( String ) params.get( "ruleId" ), -1 ) ) );

            if( testValMap.isEmpty() )
            {
                testValMap.put( "empty", "true" );
            }

            return StringUtil.changeMapToJSON( testValMap );
        }
        else if( singleMode )
        {
            Long classId = Long.valueOf( StringUtil.getLongValue(
                ( String ) params.get( "classId" ), -1 ) );

            boolean isSucc = pickService.pickUpSingleWebContent( ( String ) params
                .get( "targetUrl" ), classId, Long.valueOf( StringUtil.getLongValue(
                ( String ) params.get( "ruleId" ), -1 ) ) );

            if( isSucc )
            {
                return "success";
            }
            else
            {
                return "fail";
            }

        }

        return "success";
    }

}
