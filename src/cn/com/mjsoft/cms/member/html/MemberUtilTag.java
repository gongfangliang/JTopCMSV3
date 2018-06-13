package cn.com.mjsoft.cms.member.html;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import cn.com.mjsoft.framework.security.crypto.util.EncodeOne;

public class MemberUtilTag extends TagSupport
{
    private static final long serialVersionUID = 2743300770165412187L;

    private static final Map sMap = new HashMap();

    static
    {
        sMap.put( "A", "34534r3r34r34rr" );
        sMap.put( "B", "etertywererr324" );
        sMap.put( "C", "werthjtyhyt76776" );
        sMap.put( "D", "ertertgrtgrt55436" );
        sMap.put( "E", "123tr453t65yghh" );
        sMap.put( "F", "fghtry54645345345" );
    }

    private String target = "";

    private String mode = "push";

    private String use = "C";

    public int doStartTag() throws JspException
    {
        String end = null;

        try
        {
            if( "p".equals( mode ) )
            {
                byte[] encryptResult = EncodeOne.encryptAES( target,
                    ( String ) sMap.get( use ) );

                end = EncodeOne.encode16( encryptResult ).toLowerCase();

            }
            else if( "d".equals( mode ) )
            {
                byte[] decryptResult = EncodeOne.decryptAES( EncodeOne
                    .parseHexStr2Byte( target ), ( String ) sMap.get( use ) );

                end = new String( decryptResult );
            }
        }
        catch ( Exception e )
        {

        }

        this.pageContext.setAttribute( "SysDecVal", end );

        return EVAL_BODY_INCLUDE;
    }

    public void setTarget( String target )
    {
        this.target = target;
    }

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setUse( String use )
    {
        this.use = use;
    }

    public static String getSalt( String key )
    {
        return ( String ) sMap.get( key );
    }

}
