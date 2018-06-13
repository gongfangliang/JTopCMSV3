package cn.com.mjsoft.cms.common.interceptor;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.com.mjsoft.cms.common.spring.annotation.ActionInfo;
import cn.com.mjsoft.cms.stat.behavior.DangerAcceccTraceBehavior;
import cn.com.mjsoft.framework.behavior.Behavior;
import cn.com.mjsoft.framework.util.IPSeeker;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.html.TagConstants;

/**
 * Spring MVC 辅助业务拦截器
 * 
 * @author MJSoft
 * 
 */
public class SpringMVCFlowExeTokenAndTraceInterceptor implements HandlerInterceptor
{
    private static Logger log = Logger.getLogger( SpringMVCFlowExeTokenAndTraceInterceptor.class );

    private static Behavior accessBehavior = new DangerAcceccTraceBehavior();

    public void postHandle( HttpServletRequest request, HttpServletResponse response,
        Object handler, ModelAndView modelAndView ) throws Exception
    {
 
    }

    public boolean preHandle( HttpServletRequest request, HttpServletResponse response,
        Object handler ) throws Exception
    {

        HandlerMethod hm = ( HandlerMethod ) handler;

        ActionInfo ai = hm.getMethodAnnotation( ActionInfo.class );

        Set tokenSet = null;

        if( ai != null )
        {
            request.setCharacterEncoding( ai.encoding() );

            if( ai.token() )
            {
                HttpSession httpSession = request.getSession();

                if( httpSession == null )
                {
                    responseCliect( request, "token丢失，无法确认访问来源！" );

                    response.setStatus( HttpServletResponse.SC_BAD_REQUEST );

                    return false;
                }

                tokenSet = ( Set ) httpSession.getAttribute( TagConstants.TOKEN );

                if( tokenSet == null )
                {
                    responseCliect( request, "token丢失，无法确认访问来源！" );

                    response.setStatus( HttpServletResponse.SC_BAD_REQUEST );

                    return false;
                }

                String tokenVal = request.getParameter( TagConstants.TOKEN_KEY );

                if( StringUtil.isStringNull( tokenVal ) || !tokenSet.contains( tokenVal ) )
                {
                    // 删除token
                    tokenSet.remove( tokenVal );
                    // tokenSet.clear();

                    responseCliect( request, "token丢失，无法确认访问来源！" );

                    response.setStatus( HttpServletResponse.SC_BAD_REQUEST );

                    return false;
                }

                // 删除token
                tokenSet.remove( tokenVal );

                httpSession.setAttribute( TagConstants.TOKEN, tokenSet );

            }

        }

        return true;
    }

    public void afterCompletion( HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception ex ) throws Exception
    {

    }

    private static void responseCliect( HttpServletRequest request, String target )
    {

        // 打印出错信息
        log.fatal( "IP->" + IPSeeker.getIp( ( HttpServletRequest ) request ) + ",非法动作->" + target
            + ",URL->" + ( ( HttpServletRequest ) request ).getRequestURL().toString() );

        // 记录行为
        Object[] param = new Object[] { IPSeeker.getIp( ( HttpServletRequest ) request ),
            ( ( HttpServletRequest ) request ).getRequestURL().toString(), target,
            request.getQueryString() };

        accessBehavior.operation( null, param );

    }

}
