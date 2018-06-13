package cn.com.mjsoft.cms.member.behavior;

import javax.servlet.http.HttpServletRequest;

import cn.com.mjsoft.framework.behavior.Behavior;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.StringUtil;

public class SecTokenModeCheckBehavior implements Behavior
{

    public Object operation( Object target, Object[] param )
    {
        HttpServletRequest request = ( HttpServletRequest ) target;

        String tet = request.getParameter( "sysSign" );

        if( tet == null )
        {
            tet = request.getHeader( "sysSign" );
        }

        String testETokenSign = tet;

        String suid = request.getParameter( "sysUid" );

        if( suid == null )
        {
            suid = request.getHeader( "sysUid" );
        }

        Long sysUserId = StringUtil.getLongValue( suid, 0 );

        String innerEToken = SecuritySessionKeeper
            .getETokenByUserId( sysUserId );

          if( StringUtil.isStringNotNull( testETokenSign )
            && testETokenSign.equals( innerEToken ) )
        {
            return sysUserId;
        }

        return null;
    }
}
