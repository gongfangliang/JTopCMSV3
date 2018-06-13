package cn.com.mjsoft.cms.stat.dao;

import java.util.List;
import java.util.Map;

import cn.com.mjsoft.cms.channel.bean.ContentClassBean;
import cn.com.mjsoft.cms.channel.service.ChannelService;
import cn.com.mjsoft.framework.persistence.core.support.MapValueCallback;

public class ClassOrgTreeResultCallBack implements MapValueCallback
{
    private static ChannelService channelService = ChannelService.getInstance();

    
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void transformVlaue( Map result )
    {
        Long classId = ( Long ) result.get( "classId" );

         
        ContentClassBean classBean = channelService
            .retrieveSingleClassBeanInfoByClassId( classId );

        StringBuilder buf = new StringBuilder();

        List resultList = null;

        if( classBean != null && classBean.getClassId().longValue() > 0 )
        {
            resultList = channelService
                .retrieveContentClassBeanByCurrentPath( classBean
                    .getChannelPath() );

            for ( int i = 0; i < resultList.size(); i++ )
            {
                classBean = ( ContentClassBean ) resultList.get( i );

                if( i + 1 != resultList.size() )
                {
                    buf.append( classBean.getClassName() + " > " );
                }
                else
                {
                    buf.append( classBean.getClassName() );
                }
            }
        }

        result.put( "classTree", buf.toString() );

         

    }
}
