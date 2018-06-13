package cn.com.mjsoft.cms.behavior;

import cn.com.mjsoft.cms.stat.service.StatService;
import cn.com.mjsoft.framework.behavior.Behavior;
import java.util.Map;

public class testAccessBehavior implements Behavior
{
    private static StatService statService = StatService.getInstance();

    public Object operation( Object target, Object[] param )
    {
        Map params = ( Map ) target;

        Object obj = param[0];

        return new String[] { "true" };
    }
}
