package cn.com.mjsoft.cms.cluster.adapter;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据集群和单机部署环境部署切换Map存储源,序列化对象方式
 * 
 * @author MJ-Soft
 * 
 */

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class ClusterSeriMapAdapter
{

    private Map innerMap = new HashMap();

    private String redisMapName;

    private String mode = "inner";

    public ClusterSeriMapAdapter( String redisMapName )
    {
        this.redisMapName = redisMapName;
    }

    public void put( Object key, Object val )
    {
        innerMap.put( key, val );
    }

    public Object get( Object key )
    {
        return innerMap.get( key );
    }

    public boolean containsKey( Object key )
    {
        return innerMap.containsKey( key );
    }

    public void remove( Object key )
    {
        innerMap.remove( key );
    }

    public Map getMap()
    {
        return innerMap;
    }

    public long size()
    {
        return innerMap.size();
    }

    public void clear()
    {
        innerMap.clear();
    }

}
