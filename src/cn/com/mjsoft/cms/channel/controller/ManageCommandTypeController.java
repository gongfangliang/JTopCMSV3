package cn.com.mjsoft.cms.channel.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.com.mjsoft.cms.channel.bean.ContentCommendTypeBean;
import cn.com.mjsoft.cms.channel.dao.vo.ContentCommendType;
import cn.com.mjsoft.cms.channel.service.ChannelService;
import cn.com.mjsoft.cms.common.Constant;
import cn.com.mjsoft.cms.common.spring.annotation.ActionInfo;
 
 
import cn.com.mjsoft.cms.security.controller.ManageRoleController;
import cn.com.mjsoft.cms.site.bean.SiteGroupBean;
import cn.com.mjsoft.framework.security.Auth;
import cn.com.mjsoft.framework.security.Role;
import cn.com.mjsoft.framework.security.session.SecuritySessionKeeper;
import cn.com.mjsoft.framework.util.StringUtil;
import cn.com.mjsoft.framework.web.wing.ServletUtil;

@SuppressWarnings( { "rawtypes", "unchecked" } )
@Controller
@RequestMapping( "/channel" )
public class ManageCommandTypeController
{
    private static ChannelService channelService = ChannelService.getInstance();

    @ResponseBody
    @RequestMapping( value = "/createCommendType.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "添加内容集合类型", token = true )
    public Object createCommendType( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        ContentCommendType commendType = ( ContentCommendType ) ServletUtil.getValueObject(
            request, ContentCommendType.class );

        boolean isSpec = false;

        if( Constant.COMMON.ON.equals( commendType.getIsSpec() ) )
        {
            isSpec = true;
        }

        Auth auth = SecuritySessionKeeper.getSecuritySession().getAuth();

        if( auth != null )
        {
            commendType.setCreator( auth.getApellation().toString() );
        }

        if( isSpec )
        {
            commendType.decode();
        }

        SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
            .getCurrentLoginSiteInfo();

        commendType.setSiteFlag( site.getSiteFlag() );

        Long typeId = channelService.addCommendTypeInfo( commendType );

      

        Map param = new HashMap();

        Long orgId = ( Long ) auth.getOrgIdentity();

        SiteGroupBean siteBean = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
            .getCurrentLoginSiteInfo();

        param.put( "orgId", orgId.toString() );
        param.put( "targetSiteId", siteBean.getSiteId().toString() );
        param.put( "manage-commend-accredit", typeId + "-" + typeId );
        param.put( "maintain-content-accredit", typeId + "-" + typeId );

    
       
        Role role = auth.getUserRole()[0];

        if( role != null )
        {
            param.put( "roleId", role.getRoleID().toString() );

            ManageRoleController.disposeNewAccResRole( param );
        }

        if( isSpec )
        {
            return "success";
        }

        Map paramMap = new HashMap();

        paramMap.put( "fromFlow", Boolean.TRUE );

        return ServletUtil.redirect( "/core/channel/CreateCommendType.jsp", paramMap );
    }

    @ResponseBody
    @RequestMapping( value = "/editCommendType.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "编辑内容集合类型", token = true )
    public Object editCommendType( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {

        ContentCommendType commendType = ( ContentCommendType ) ServletUtil.getValueObject(
            request, ContentCommendType.class );

        boolean isSpec = false;

        if( Constant.COMMON.ON.equals( commendType.getIsSpec() ) )
        {
            isSpec = true;
        }

        if( isSpec )
        {
            commendType.decode();
        }

        channelService.editCommendTypeInfo( commendType );

        if( isSpec )
        {
           
            SiteGroupBean site = ( SiteGroupBean ) SecuritySessionKeeper.getSecuritySession()
                .getCurrentLoginSiteInfo();

      
            List commTypeList = channelService.retrieveContentCommendTypeBean( site.getSiteFlag(),
                commendType.getClassId(), false, true, false );

            ContentCommendTypeBean commTypeBean = null;

            boolean havaStatic = false;
            for ( int icl = 0; icl < commTypeList.size(); icl++ )
            {
                commTypeBean = ( ContentCommendTypeBean ) commTypeList.get( icl );

                if( Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE.equals( commTypeBean
                    .getListProduceType() ) )
                {
                    havaStatic = true;
                    break;
                }

               

            }

            if( havaStatic )
            {
                channelService.updateContentClassListProduceType( commendType.getClassId(),
                    Constant.SITE_CHANNEL.PAGE_PRODUCE_H_TYPE );

            }
            else
            {
                channelService.updateContentClassListProduceType( commendType.getClassId(),
                    Constant.SITE_CHANNEL.PAGE_PRODUCE_D_TYPE );
            }

            return "success";
        }

        Map paramMap = new HashMap();

        paramMap.put( "fromFlow", Boolean.TRUE );

        return ServletUtil.redirect( "/core/channel/EditCommendType.jsp", paramMap );
    }

    @ResponseBody
    @RequestMapping( value = "/deleteCommendType.do", method = { RequestMethod.POST } )
    @ActionInfo( traceName = "删除内容集合类型", token = true )
    public Object deleteCommendType( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        Map params = ServletUtil.getRequestInfo( request );

        List typeIdList = StringUtil.changeStringToList( ( String ) params.get( "typeIds" ), "," );

        channelService.deleteCommendTypeAllInfo( typeIdList );

        return "success";
    }

}
