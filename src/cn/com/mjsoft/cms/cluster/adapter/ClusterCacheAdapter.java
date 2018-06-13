package cn.com.mjsoft.cms.cluster.adapter;

import cn.com.mjsoft.framework.cache.impl.LRUCache;

/**
 * 根据集群和单机部署环境处理Cache业务
 * 
 * @author MJ-Soft
 * 
 */
@SuppressWarnings( { "rawtypes", "unchecked" } )
public class ClusterCacheAdapter extends LRUCache
{
    private String entryName;

    private String mode = "inner";

    public ClusterCacheAdapter( int lruc, String entryName )
    {
        super( lruc );

        this.entryName = entryName;
    }

    public Object getEntry( Object key )
    {
        return super.getEntry( key );
    }

    public Object getEntry( Object key, boolean showLog )
    {
        return super.getEntry( key, showLog );
    }

    public Object putEntry( Object key, Object value )
    {
        return super.putEntry( key, value );
    }

    public boolean containsEntryKey( Object key )
    {
        return super.containsEntryKey( key );
    }

    public Object removeEntry( Object key )
    {
        return super.removeEntry( key );
    }

    public int cacheCurrentSize()
    {
        return super.cacheCurrentSize();
    }

    public void clearAllEntry()
    {
        super.clearAllEntry();
    }

}
