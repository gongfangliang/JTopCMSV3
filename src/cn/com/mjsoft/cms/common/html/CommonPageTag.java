package cn.com.mjsoft.cms.common.html;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import cn.com.mjsoft.cms.common.page.Page;

public class CommonPageTag extends TagSupport
{
    private static final long serialVersionUID = 645085394370109893L;

    protected void initTag()
    {

    }

    public int doStartTag() throws JspException
    {

      
        Page pageInfo = ( Page ) pageContext
            .getAttribute( "___system_dispose_page_object___" );

        pageContext.setAttribute( "Page", pageInfo );

        

        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws JspException
    {
        pageContext.removeAttribute( "___system_dispose_page_object___" );
        pageContext.removeAttribute( "Page" );
        return EVAL_PAGE;
    }

}
