package cn.com.mjsoft.cms.content.html;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cn.com.mjsoft.cms.behavior.QueryDataInfoBehavior;
import cn.com.mjsoft.framework.util.SystemSafeCharUtil;
import cn.com.mjsoft.cms.channel.bean.ContentClassBean;
import cn.com.mjsoft.cms.cluster.adapter.ClusterMapAdapter;
import cn.com.mjsoft.cms.common.page.Page;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.cms.publish.service.PublishService;
import cn.com.mjsoft.framework.exception.FrameworkException;
import cn.com.mjsoft.framework.util.ObjectUtility;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.html.TagConstants;
import cn.com.mjsoft.framework.web.html.common.AbstractIteratorTag;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateModelException;

public class SystemIteratorTag extends AbstractIteratorTag
{
    private static final long serialVersionUID = 8989827541588231939L;

    private MetaDataService mdService = MetaDataService.getInstance();

    private static PublishService publishService = PublishService.getInstance();

    // 以下为方法级别调用
    private String service = "";

    private String method = "";

    private String reObj = "";

    private String list = "false";

    private String common = "false";

    // 以下为sql级别调用
    private String querySign = "";

    private String countSign = "";

    private String countVar = "";

    private String page = "false";

    private String size = "";

    // 公用参数
    private String var = "";

    private String objName = "";

    private String single = "false";

    protected void initTag()
    {

    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    protected List returnObjectList()
    {
        List result = null;

        Object returnObj = null;

        if( "true".equals( single ) )
        {
            this.setToListSingleMode();
        }

        boolean commMode = false;

        if( "true".equals( common ) )
        {
            commMode = true;
        }

        if( "true".equals( list ) )
        {
            Object objColl = null;

            returnObj = this.pageContext.getAttribute( reObj );

            if( returnObj == null || !( returnObj instanceof List ) )
            {
                int accPo = reObj.indexOf( "." );

                if( accPo > 0 )
                {
                    objColl = this.pageContext
                        .getAttribute( StringUtil.subString( reObj, 0, accPo ) );

                    String objName = StringUtil.subString( reObj, accPo + 1 );

                    if( StringUtil.isStringNotNull( objName ) )
                    {
                        if( objColl instanceof Map )
                        {
                            Map reMap = ( Map ) objColl;

                            returnObj = reMap.get( objName );
                        }
                        else
                        {
                            returnObj = ObjectUtility.getPrivateFieldValue( objName, objColl );
                        }
                    }
                }
            }

            if( returnObj instanceof List )
            {
                return ( List ) returnObj;
            }
            else
            {
                result = new ArrayList( 1 );
                result.add( returnObj );
                return result;
            }
        }

        if( StringUtil.isStringNotNull( service ) )
        {

            String className = service;

            String methodName = method;

            if( StringUtil.isStringNull( className ) || StringUtil.isStringNull( methodName ) )
            {
                if( commMode )
                {
                    this.pageContext.setAttribute( "jtopcms:sys:query:list:" + objName, result );
                }

                return result;
            }

            Class serviceClass = null;

            Method instanceMethod = null;

            Method targetMethod = null;

            Object serviceInstance = null;

            // 参数处理
            Class[] paramFlag = null;

            Object[] param = null;

            if( StringUtil.isStringNotNull( var ) )
            {
                int paramSize = StringUtil.getRepeatCharLength( var, "," ) + 1;

                List paramList = StringUtil.changeStringToList( var, "," );

                paramFlag = new Class[paramSize];
                param = new Object[paramSize];

                String val = null;
                for ( int i = 0; i < paramSize; i++ )
                {
                    paramFlag[i] = String.class;

                    if( i < paramList.size() )
                    {
                        val = ( String ) paramList.get( i );
                    }
                    else
                    {
                        val = "";
                    }

                    param[i] = val;

                }
            }

            try
            {
                serviceClass = ObjectUtility.getClassInstance( className );

                instanceMethod = serviceClass.getMethod( "getInstance", null );

                if( paramFlag == null )
                {
                    targetMethod = serviceClass.getMethod( methodName, null );
                }
                else
                {
                    targetMethod = serviceClass.getMethod( methodName, paramFlag );
                }

                serviceInstance = instanceMethod.invoke( null, null );

                if( param == null )
                {
                    returnObj = targetMethod.invoke( serviceInstance, null );
                }
                else
                {
                    returnObj = targetMethod.invoke( serviceInstance, param );
                }

            }
            catch ( Exception e )
            {
                if( param == null )
                {
                    try
                    {
                        targetMethod = serviceClass.getMethod( methodName,
                            new Class[] { String.class } );

                        serviceInstance = instanceMethod.invoke( null, null );

                        returnObj = targetMethod.invoke( serviceInstance, new Object[] { "" } );
                    }
                    catch ( IllegalArgumentException e1 )
                    {

                        e1.printStackTrace();
                    }
                    catch ( IllegalAccessException e1 )
                    {

                        e1.printStackTrace();
                    }
                    catch ( InvocationTargetException e1 )
                    {

                        e1.printStackTrace();
                    }
                    catch ( SecurityException e2 )
                    {

                        e.printStackTrace();
                    }
                    catch ( NoSuchMethodException e3 )
                    {

                        e.printStackTrace();
                    }
                }
                else
                {
                    e.printStackTrace();
                }
            }

            if( returnObj instanceof List )
            {
                if( commMode )
                {
                    this.pageContext.setAttribute( "jtopcms:sys:query:list:" + objName, returnObj );
                }

                return ( List ) returnObj;
            }
            else if( returnObj instanceof Object[] )// 分页
            {
                Object[] res = ( Object[] ) returnObj;

                if( res.length < 2 )
                {
                    if( commMode )
                    {
                        this.pageContext.setAttribute( "jtopcms:sys:query:list:" + objName, result );
                    }

                    return result;
                }

                result = ( List ) res[0];

                Page pageInfo = ( Page ) res[1];

                this.pageContext.setAttribute( "___system_dispose_page_object___", pageInfo );

                if( commMode )
                {
                    this.pageContext.setAttribute( "jtopcms:sys:query:list:" + objName, result );
                }

                ContentClassBean classBean = ( ContentClassBean ) this.pageContext
                    .getAttribute( "Class" );

                if( classBean != null )
                {
                    publishService.htmlTagPage( this.pageContext, null, classBean.getClassId(),
                        classBean, classBean.getClassId(), pageInfo, page, "" );
                }

                return result;
            }
            else
            {
                result = new ArrayList( 1 );

                if( returnObj != null )
                {
                    result.add( returnObj );
                }

                if( commMode )
                {
                    this.pageContext.setAttribute( "jtopcms:sys:query:list:" + objName, result );
                }

                return result;
            }

        }

        if( StringUtil.isStringNotNull( reObj ) )
        {
            Object obj = this.pageContext.getAttribute( "jtopcms:sys:query:list:" + reObj );

            if( obj == null )
            {
                obj = this.pageContext.getRequest().getAttribute( reObj );
            }

            if( obj instanceof List )
            {
                return ( List ) obj;
            }
            // freemarker
            else if( obj instanceof SimpleSequence )
            {
                try
                {
                    return ( ( SimpleSequence ) obj ).toList();
                }
                catch ( TemplateModelException e )
                {

                }
            }
            else
            {
                result = new ArrayList( 1 );
                result.add( obj );

                return result;
            }
        }

        ClusterMapAdapter QUERY_SQL = QueryDataInfoBehavior.getSystemQueryDataSQl();

        String targetQuerySql = ( String ) QUERY_SQL.get( querySign.trim().toLowerCase() );

        if( StringUtil.isStringNull( targetQuerySql ) )
        {
            return new ArrayList( 1 );
        }

        int paramCount = StringUtil.getRepeatCharLength( targetQuerySql, "?" );

        int nextPage = StringUtil.getIntValue( this.pageContext.getRequest().getParameter( "pn" ),
            1 );

        if( !"true".equals( page ) )
        {
            nextPage = 1;
        }

        int pageSize = StringUtil.getIntValue( size, 15 );

        String[] countVarArray = null;
        Object[] varObj = new Object[paramCount];
        String[] typeInfo = null;

        if( StringUtil.isStringNotNull( countVar ) )
        {

            countVarArray = StringUtil.split( countVar, "," );

            for ( int i = 0; i < countVarArray.length; i++ )
            {
                if( SystemSafeCharUtil.hasSQLDChars( countVarArray[i] ) )
                {
                    throw new FrameworkException( "包含非法字符,本次操作强制中止执行" );
                }

                varObj[i] = countVarArray[i];
            }
        }

        String targetCountSql = ( String ) QUERY_SQL.get( countSign );

        boolean isPage = StringUtil.isStringNotNull( targetCountSql ) ? true : false;
        Page pageInfo = null;

        if( isPage )
        {
            Long count = mdService.retrieveSystemTableByQueryFlagAndPageInfoCount( targetCountSql,
                varObj, false );

            pageInfo = new Page( pageSize, count.intValue(), nextPage );

            this.pageContext.setAttribute( "___system_dispose_page_object___", pageInfo );

        }

        String[] varArray = null;

        typeInfo = null;

        varObj = new Object[paramCount];

        if( StringUtil.isStringNotNull( var ) )
        {
            varArray = StringUtil.split( var, "," );

            for ( int i = 0; i < varArray.length; i++ )
            {
                if( SystemSafeCharUtil.hasSQLDChars( varArray[i] ) )
                {
                    throw new FrameworkException( "包含非法字符,本次操作强制中止执行" );
                }

                if( "{start}".equals( varArray[i] ) )
                {
                    if( pageInfo != null )
                    {
                        varObj[i] = Long.valueOf( pageInfo.getFirstResult() );
                    }
                    else
                    {
                        varObj[i] = null;
                    }
                }
                else if( "{size}".equals( varArray[i] ) )
                {
                    varObj[i] = Integer.valueOf( pageSize );
                }
                else
                {
                    varObj[i] = varArray[i];
                }

            }
        }

        List resultMap = mdService.retrieveSystemTableByQueryFlagAndPageInfo( targetQuerySql,
            varObj );

        return resultMap;
    }

    protected String returnPutValueName()
    {
        if( StringUtil.isStringNotNull( objName ) )
        {
            return objName;
        }
        else
        {
            return "SysObj";
        }
    }

    protected String returnRequestAndPageListAttName()
    {
        return null;
    }

    protected Object returnSingleObject()
    {
        return null;
    }

    protected String returnValueRange()
    {
        return TagConstants.SELF_RANFE;
    }

    public void setCountSign( String countSign )
    {
        this.countSign = countSign;
    }

    public void setQuerySign( String querySign )
    {
        this.querySign = querySign;
    }

    public void setCountVar( String countVar )
    {
        this.countVar = countVar;
    }

    public void setSize( String size )
    {
        this.size = size;
    }

    public void setVar( String var )
    {
        this.var = var;
    }

    public void setObjName( String objName )
    {
        this.objName = objName;
    }

    public void setService( String service )
    {
        this.service = service;
    }

    public void setMethod( String method )
    {
        this.method = method;
    }

    public void setSingle( String single )
    {
        this.single = single;
    }

    public void setList( String list )
    {
        this.list = list;
    }

    public void setReObj( String reObj )
    {
        this.reObj = reObj;
    }

    public void setCommon( String common )
    {
        this.common = common;
    }

}
