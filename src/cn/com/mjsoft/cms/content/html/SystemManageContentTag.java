package cn.com.mjsoft.cms.content.html;

import java.util.List;
import java.util.Map;

import cn.com.mjsoft.cms.content.service.ContentService;
import cn.com.mjsoft.cms.metadata.service.MetaDataService;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.html.common.AbstractIteratorTag;

public class SystemManageContentTag extends AbstractIteratorTag
{
    private static final long serialVersionUID = -4014128235298996756L;

    private static ContentService contentService = ContentService.getInstance();

    private static MetaDataService metaDataService = MetaDataService.getInstance();

    private String modelName = "";// 模型名称

    private String modelId = "-1";// 模型Id

    private String formMode = "false";

    private String objName = "Info";

    protected String returnPutValueName()
    {
        return objName;
    }

    protected String returnRequestAndPageListAttName()
    {
        return "allContent";
    }

    protected String returnValueRange()
    {
        return "pageRange";
    }

    protected Object returnSingleObject()
    {
        Map info = ( Map ) pageContext.getRequest().getAttribute( "Info" );

        if( info != null )
        {
            return info;
        }

        Long id = Long.valueOf( StringUtil.getLongValue( this.getId(), -1 ) );

        if( "true".equals( formMode ) )
        {
            info = metaDataService.retrieveSingleFormDataById( id );
        }
        else
        {
            Long modelId = Long.valueOf( StringUtil.getLongValue( this.modelId, -1 ) );

            if( "".equals( modelName ) )
            {
                info = contentService.retrieveSingleUserDefineContentManageMode( modelId, id );
            }
            else
            {
                info = contentService.retrieveSingleUserDefineContentManageMode( modelName, id );
            }
        }

        return info;
    }

    protected List returnObjectList()
    {
        return null;
    }

    protected void initTag()
    {
    }

    public void setModelName( String modelName )
    {
        this.modelName = modelName;
    }

    public void setModelId( String modelId )
    {
        this.modelId = modelId;
    }

    public void setFormMode( String formMode )
    {
        this.formMode = formMode;
    }

    public void setObjName( String objName )
    {
        this.objName = objName;
    }

}
