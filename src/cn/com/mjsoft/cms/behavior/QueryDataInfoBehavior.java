package cn.com.mjsoft.cms.behavior;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import cn.com.mjsoft.cms.cluster.adapter.ClusterMapAdapter;
import cn.com.mjsoft.framework.behavior.Behavior;
import cn.com.mjsoft.framework.config.impl.SystemConfiguration;

@SuppressWarnings( { "rawtypes", "unchecked" } )
public class QueryDataInfoBehavior implements Behavior
{
     
    private static final String SYSTEM_QUERY_XSQL = "WEB-INF" + File.separator + "config"
        + File.separator + "query.xml";

    
    private static boolean debugMode = false;

    private static ClusterMapAdapter QUERY_SQL = new ClusterMapAdapter(
        "queryDataInfoBehavior.QUERY_SQL", String.class, String.class );

  
    public Object operation( Object target, Object[] param )
    {
       
        // xml
        SAXReader reader = new SAXReader();

        String basePath = SystemConfiguration.getInstance().getSystemConfig().getSystemRealPath();

        String filePath = basePath + SYSTEM_QUERY_XSQL;

        try
        {
            Document doc = reader.read( new FileInputStream( filePath ) );

            Element root = doc.getRootElement();

            List moduleNodes = root.elements();

            for ( Iterator it = moduleNodes.iterator(); it.hasNext(); )
            {
                Element queryNode = ( Element ) it.next();

                if( "query".equals( queryNode.getName() ) )
                {
                    String key = queryNode.attributeValue( "key" );

                    QUERY_SQL.put( key.trim().toLowerCase(), queryNode.getTextTrim() );

                }

            }
        }
        catch ( FileNotFoundException e )
        {

            e.printStackTrace();
        }
        catch ( DocumentException e )
        {

            e.printStackTrace();
        }

        return null;
    }

    public static ClusterMapAdapter getSystemQueryDataSQl()
    {
        if( debugMode )
        {
            SAXReader reader = new SAXReader();

            String basePath = SystemConfiguration.getInstance().getSystemConfig()
                .getSystemRealPath();

            String filePath = basePath + SYSTEM_QUERY_XSQL;

            try
            {
                Document doc = reader.read( new FileInputStream( filePath ) );

                Element root = doc.getRootElement();

                List moduleNodes = root.elements();

                for ( Iterator it = moduleNodes.iterator(); it.hasNext(); )
                {
                    Element queryNode = ( Element ) it.next();

                    if( "query".equals( queryNode.getName() ) )
                    {
                        String key = queryNode.attributeValue( "key" );

                        QUERY_SQL.put( key.trim().toLowerCase(), queryNode.getTextTrim() );
                    }

                }
            }
            catch ( FileNotFoundException e )
            {

                e.printStackTrace();
            }
            catch ( DocumentException e )
            {

                e.printStackTrace();
            }

            return QUERY_SQL;

        }

        return QUERY_SQL;
    }

   
}
