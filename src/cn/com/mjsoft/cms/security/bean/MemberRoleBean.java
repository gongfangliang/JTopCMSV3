package cn.com.mjsoft.cms.security.bean;


public class MemberRoleBean
{
    
    private Long roleId = Long.valueOf( -1 );

    private Long siteId = Long.valueOf( -1 );

    private String roleName;

    private Integer useState;

    private String roleDesc;

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId( Long roleId )
    {
        this.roleId = roleId;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName( String roleName )
    {
        this.roleName = roleName;
    }

    

    public Integer getUseState()
    {
        return useState;
    }

    public void setUseState( Integer useState )
    {
        this.useState = useState;
    }

    public String getRoleDesc()
    {
        return roleDesc;
    }

    public void setRoleDesc( String roleDesc )
    {
        this.roleDesc = roleDesc;
    }

    public Long getSiteId()
    {
        return siteId;
    }

    public void setSiteId( Long siteId )
    {
        this.siteId = siteId;
    }

}
