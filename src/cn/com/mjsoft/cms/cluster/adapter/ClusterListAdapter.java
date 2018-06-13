package cn.com.mjsoft.cms.cluster.adapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据集群和单机部署环境部署切换List存储源
 * 
 * @author MJ-Soft
 * 
 */
@SuppressWarnings( { "rawtypes", "unchecked" } )
public class ClusterListAdapter
{
    private List<Object> innerList = new ArrayList<Object>();

    private String redisListName;

    private Class objClass;

    private String mode = "inner";

    public ClusterListAdapter( String redisListName, Class objClass )
    {
        this.redisListName = redisListName;

        this.objClass = objClass;
    }

    public void add( Object val )
    {
        innerList.add( val );
    }

    public Object get( int index )
    {
        return innerList.get( index );
    }

    public List getList()
    {
        return innerList;
    }

    public long size()
    {

        return innerList.size();
    }

    public void clear()
    {
        innerList.clear();
    }

}
