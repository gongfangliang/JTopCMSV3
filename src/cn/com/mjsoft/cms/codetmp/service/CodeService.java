package cn.com.mjsoft.cms.codetmp.service;

import org.apache.log4j.Logger;

import cn.com.mjsoft.cms.common.datasource.MySqlDataSource;
import cn.com.mjsoft.framework.persistence.core.PersistenceEngine;

public class CodeService
{
    private static Logger log = Logger.getLogger( CodeService.class );

    private static CodeService service = null;

    public PersistenceEngine mysqlEngine = new PersistenceEngine(
        new MySqlDataSource() );

    // private AdvertDao advertDao;

    private CodeService()
    {
        // advertDao = new AdvertDao( mysqlEngine );
    }

    private static synchronized void init()
    {
        if( null == service )
        {
            service = new CodeService();
        }
    }

    public static CodeService getInstance()
    {
        if( null == service )
        {
            init();
        }
        return service;
    }
}
