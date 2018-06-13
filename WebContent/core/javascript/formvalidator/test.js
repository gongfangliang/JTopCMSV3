//====================================================================================================
// [�������] jQuery formValidator
//----------------------------------------------------------------------------------------------------
// [��    ��] jQuery formValidator������֤��������ǻ���jQuery��⣬ʵ����js�ű���ҳ��ķ��롣��һ����
//            ��������ֻ��Ҫдһ�д���Ϳ�������ʵ��20�����ϵĽű����ơ���֧��һ������Ԫ���ۼӺܶ���
//            У�鷽ʽ,����������Ϣ��˼�룬�����ǰ���Ϣд�ڱ���Ԫ���ϣ��ܱȽ�������ʵ��ajax����
//----------------------------------------------------------------------------------------------------
// [��������] è��	
// [��    ��] wzmaodong@126.com
// [���߲���] http://wzmaodong.cnblogs.com
// [�����ҳ] http://www.yhuan.com
// [QQȺ����] 72280458��74106519
// [��������] 2011-07-24
// [�� �� ��] ver4.1.3 �ڲ����԰�
// [��ԴЭ��] LGPL
//====================================================================================================

(function($) {
$.formValidator = 
{
	//ȫ������
	initConfig : function(controlOptions)
	{
		var settings = {};
		$.extend(true, settings, initConfig_setting, controlOptions || {});
		//����Ǿ���ģʽ�����������ʱ�򣬵�һ������Ŀؼ��Ͳ���ý���
		if(settings.mode == "SingleTip"){settings.errorFocus=false};
		//�����д�˱����Ͱ�ť����ע����֤�¼�
		if(settings.formID!=""){
		    $("#"+settings.formID).submit(function(){
		        if(settings.ajaxForm!=null){
		            $.formValidator.ajaxForm(settings.validatorGroup,settings.ajaxForm);
			        return false;
				}
		        else
				{
					if (settings.ajaxCountValid > 0) {
						settings.onAlert(settings.ajaxPrompt);
						return false;
					}
					return $.formValidator.pageIsValid(settings.validatorGroup);
                }
            });
		}
		$("body").data(settings.validatorGroup, settings);
		if(settings.theme==""){return}
		//��ȡ�����Ӧ�Ľű�
		var scriptSrcArray = fv_scriptSrc.split('/');
		var jsName = scriptSrcArray[scriptSrcArray.length-1];
    	var themedir = fv_scriptSrc.replace(jsName,'');
		$.ajax({async:false,type: "GET",url: themedir + "themes/"+settings.theme+"/js/theme.js",dataType: "script",
			error :function(){alert('��ǰƤ�����س�������ȷ��Ƥ����'+settings.theme+'���Ƿ����')}
		}); 
		//��ȡ�����Ӧ����ʽ
		if($.browser.msie)
		{
			var css=document.createElement("link");
			css.rel="stylesheet";
			css.type="text/css";
			css.href=themedir+"themes/"+settings.theme+"/style/style.css";
			document.getElementsByTagName("head")[0].appendChild(css);
		}
		else
		{
			var style=document.createElement('style'); 
			style.setAttribute("type", "text/css"); 
			var styCss = "@import url('"+themedir+"themes/"+settings.theme+"/style/style.css');";
			if (style.styleSheet) {style.styleSheet.cssText=styCss} else {style.appendChild(document.createTextNode(styCss))} 
			document.getElementsByTagName("head")[0].appendChild(style); 
		}
	},
	
	//����У�鷽ʽ֧�ֵĿؼ�����
	sustainType : function(elem,validateType)
	{
		var srcTag = elem.tagName;
		var stype = elem.type;
		switch(validateType)
		{
			case "formValidator":
				return true;
			case "inputValidator":
				return (srcTag == "INPUT" || srcTag == "TEXTAREA" || srcTag == "SELECT");
			case "compareValidator":
				return ((srcTag == "INPUT" || srcTag == "TEXTAREA") ? (stype != "checkbox" && stype != "radio") : false);
			case "ajaxValidator":
				return (stype == "text" || stype == "textarea" || stype == "file" || stype == "password" || stype == "select-one");
			case "regexValidator":
				return ((srcTag == "INPUT" || srcTag == "TEXTAREA") ? (stype != "checkbox" && stype != "radio") : false);
			case "functionValidator":
			    return true;
			case "passwordValidator":
			    return stype == "password";
		}
	},
    
	//���validator�����Ӧ��element�����validator����׷��Ҫ���е�У�顣
	appendValid : function(id, setting )
	{
		//����Ǹ���У�鲻֧�ֵ����ͣ��Ͳ�׷�ӵ�������-1��ʾû��׷�ӳɹ�
		var elem = $("#"+id).get(0);   
		var validateType = setting.validateType;
		if(!$.formValidator.sustainType(elem,validateType)){return -1}
		//���³�ʼ��
		if (validateType=="formValidator" || elem.settings == undefined ){elem.settings = new Array()}   
		var len = elem.settings.push( setting );
		elem.settings[len - 1].index = len - 1;
		return len - 1;
	},
	
	//������ʾ��Ϣ
	setTipState : function(elem,showclass,showmsg)
	{
		//alert('s');
		var initConfig = $("body").data(elem.validatorGroup);

		if(initConfig.mode == "SingleTip")
		{
			//��ʾ�ͱ�����ʾ��Ϣ
			$("#fv_content").html(showmsg);
			elem.Tooltip = showmsg;
			if(showclass!="onError"){tip.hide()}
		}
		else
		{
			var tip = $("#"+elem.settings[0].tipID);
			//alert(tip);
			var html = showclass == "onShow" ? onShowHtml : (showclass == "onFocus" ? onFocusHtml : (showclass == "onCorrect" ? onCorrectHtml : onErrorHtml));
			//alert(showclass);
			if(html.length = 0)
			{
				tip.hide()
			}
			else
			{
				if(elem.validatorPasswordIndex > 0 && showclass =="onCorrect"){
					var setting = elem.settings[elem.validatorPasswordIndex];
					var level = $.formValidator.passwordValid(elem);
					showmsg = "";
					if(level==-1 && setting.onErrorContinueChar!=""){
						showmsg=setting.onErrorContinueChar
					}else if(level==-2 && setting.onErrorSameChar!=""){
						showmsg=setting.onErrorSameChar
					}else if(level==-3 && setting.onErrorCompareSame!=""){
						showmsg=setting.onErrorCompareSame
					}
					if(showmsg!="")
					{
						$.formValidator.setTipState(elem,'onError',showmsg);
						return
					}
					showmsg = passwordStrengthText[level<=0?0:level - 1];
				}
				//alert(html);
				
				html = html.replace(/\$class\$/g, showclass).replace(/\$data\$/g, showmsg);

				//alert(html);
				//alert(showclass);
				if(showclass!=""){
					tip.html(html).removeClass().addClass(showclass).show();
					//alert(showclass);
				}else{
					tip.html(html).show();
				}
			}
			var stype = elem.type;
			if(stype == "password" || stype == "text" || stype == "file")
			{
				jqobj = $(elem);
				if(onShowClass!="" && showclass == "onShow"){jqobj.removeClass().addClass(onShowClass)};
				if(onFocusClass!="" && showclass == "onFocus"){jqobj.removeClass().addClass(onFocusClass)};
				if(onCorrectClass!="" && showclass == "onCorrect"){jqobj.removeClass().addClass(onCorrectClass)};
				if(onErrorClass!="" && showclass == "onError"){jqobj.removeClass().addClass(onErrorClass)};
			}
		}
			//alert('t');
	},
		
	//����ʾ�����ó�ԭʼ��ʾ(�����defaultPassed,Ӧ������ΪonCorrect)
	resetTipState : function(validatorGroup)
	{
		if(validatorGroup == undefined){validatorGroup = "1"};
		var initConfig = $("body").data(validatorGroup);
		$.each(initConfig.validObjects,function(){
			var setting = this.settings[0];
			var passed = setting.defaultPassed;
			$.formValidator.setTipState(this, passed ? "onCorrect" : "onShow", passed ? $.formValidator.getStatusText(this,setting.onCorrect) : setting.onShow );	
		});
	},
	
	//���ô������ʾ��Ϣ
	setFailState : function(tipID,showmsg)
	{
		$.formValidator.setTipState($("#"+tipID).get(0), "onError", showmsg);
	},

	//���ݵ�������,��ȷ:��ȷ��ʾ,����:������ʾ
	showMessage : function(returnObj)
	{
	    var id = returnObj.id;
		var elem = $("#"+id).get(0);
		var isValid = returnObj.isValid;
		var setting = returnObj.setting;//��ȷ:setting[0],����:��Ӧ��setting[i]
		var showmsg = "",showclass = "";
		var intiConfig = $("body").data(elem.validatorGroup);
		if (!isValid)
		{		
			showclass = "onError";
			if(setting.validateType=="ajaxValidator")
			{
				if(setting.lastValid=="")
				{
				    showclass = "onLoad";
				    showmsg = setting.onWait;
				}
				else
				{
				    showmsg = $.formValidator.getStatusText(elem,setting.onError);
				}
			}
			else
			{
				showmsg = (returnObj.errormsg==""? $.formValidator.getStatusText(elem,setting.onError) : returnObj.errormsg);
				
			}
			if(intiConfig.mode == "AlertTip")		
			{
				if(elem.validValueOld!=$(elem).val()){intiConfig.onAlert(showmsg);}   
			}
			else
			{
				$.formValidator.setTipState(elem,showclass,showmsg);
			}
		}
		else
		{		
			//��֤�ɹ���,���û�����óɹ���ʾ��Ϣ,�����Ĭ����ʾ,��������Զ�����ʾ;����Ϊ��,ֵΪ�յ���ʾ
			showmsg = $.formValidator.isEmpty(id) ? setting.onEmpty : $.formValidator.getStatusText(elem,setting.onCorrect);
			$.formValidator.setTipState(elem,"onCorrect",showmsg);
		}
		return showmsg;
	},

	showAjaxMessage : function(e)
	{
		var elem = $("#"+e.id).get(0);
		var setting = elem.settings[e.ajax];
		var validValueOld = elem.validValueOld;
		var validvalue = $(elem).val();
		e.setting = setting;
		//defaultPassed��δ����
		if(validValueOld!= validvalue || validValueOld == validvalue && elem.onceValided == undefined)
		{
			$.formValidator.ajaxValid(e);
		}
		else
		{
			if(setting.isValid!=undefined && !setting.isValid){
				elem.lastshowclass = "onError"; 
				elem.lastshowmsg = $.formValidator.getStatusText(elem,setting.onError);
			}
			$.formValidator.setTipState(elem,elem.lastshowclass,elem.lastshowmsg);
		}
	},

	//��ȡָ���ַ����ĳ���
    getLength : function(id)
    {
        var srcjo = $("#"+id);
		var elem = srcjo.get(0);
        var sType = elem.type;
        var len = 0;
        switch(sType)
		{
			case "text":
			case "hidden":
			case "password":
			case "textarea":
			case "file":
		        var val = srcjo.val();
				var setting = elem.settings[0];
				//�������ʾ��ʾ���ݵģ�Ҫ���Ե�
				if(elem.isInputControl && elem.value == setting.onShowText){val=""}
				var initConfig = $("body").data(elem.validatorGroup);
				if (initConfig.wideWord)
				{
					for (var i = 0; i < val.length; i++) 
					{
						len = len + ((val.charCodeAt(i) >= 0x4e00 && val.charCodeAt(i) <= 0x9fa5) ? 2 : 1); 
					}
				}
				else{
					len = val.length;
				}
		        break;
			case "checkbox":
			case "radio": 
				len = $("input[type='"+sType+"'][name='"+srcjo.attr("name")+"']:checked").length;
				break;
		    case "select-one":
		        len = elem.options ? elem.options.selectedIndex : -1;
				break;
			case "select-multiple":
				len = $("select[name="+elem.name+"] option:selected").length;
				break;
	    }
		return len;
    },
    
	//���empty������ԣ��жϽ����Ƿ�Ϊ�յ�У�������
    isEmpty : function(id)
    {
        return ($("#"+id).get(0).settings[0].empty && $.formValidator.getLength(id)==0);
    },
    
	//������ã��жϵ�������Ԫ���Ƿ���֤ͨ���������ص�����
    isOneValid : function(id)
    {
	    return $.formValidator.oneIsValid(id).isValid;
    },
    
	//��֤�����Ƿ���֤ͨ��,��ȷ����settings[0],���󷵻ض�Ӧ��settings[i]
	oneIsValid : function (id)
	{
		var e = {};
		e.id = id;
		e.dom = $("#"+id).get(0);
		e.initConfig = $("body").data(e.dom.validatorGroup);
		e.ajax = -1;
		e.errormsg = "";       //�Զ��������Ϣ
	    e.settings = e.dom.settings;
	    var settingslen = e.settings.length;
		var validateType;
		//ֻ��һ��formValidator��ʱ�򲻼���
		if (settingslen==1){e.settings[0].bind=false;}
		if(!e.settings[0].bind){return null;}
		$.formValidator.resetInputValue(true,e.initConfig,id);
		for ( var i = 0 ; i < settingslen ; i ++ )
		{   
			if(i==0){
				//���Ϊ�գ�ֱ�ӷ�����ȷ
				if($.formValidator.isEmpty(id)){
					e.isValid = true;
					e.setting = e.settings[0];
					break;
				}
				continue;
			}
			e.setting = e.settings[i];
			validateType = e.settings[i].validateType;
			//�������ʹ���У��
			switch(validateType)
			{
				case "inputValidator":
					$.formValidator.inputValid(e);
					break;
				case "compareValidator":
					$.formValidator.compareValid(e);
					break;
				case "regexValidator":
					$.formValidator.regexValid(e);
					break;
				case "functionValidator":
					$.formValidator.functionValid(e);
					break;
				case "ajaxValidator":
					//�����ajaxУ�飬����ֱ��ȡ�ϴεĽ��ֵ
					e.ajax = i;
					break;
				case "passwordValidator":
					break;
			}
			if(!e.settings[i].isValid) {
				e.isValid = false;
				e.setting = e.settings[i];
				break;
			}else{
				e.isValid = true;
				e.setting = e.settings[0];
				if (e.settings[i].validateType == "ajaxValidator"){break};
			}
		}
		$.formValidator.resetInputValue(false,e.initConfig,id);
		return e;
	},

	//��֤������Ҫ��֤�Ķ��󣬲������Ƿ���֤�ɹ����������������ajaxValidator���ύ��ʱ��Ͳ�����У�飬ֱ�Ӷ�ȡ�����
	pageIsValid : function (validatorGroup)
	{
	    if(validatorGroup == undefined){validatorGroup = "1"};
		var isValid = true,returnObj,firstErrorMessage="",errorMessage;
		var error_tip = "^",thefirstid,name,name_list="^"; 	
		var errorlist = new Array();
		//�����ύ״̬��ajax�Ƿ�����������б�
		var initConfig = $("body").data(validatorGroup);
		initConfig.status = "sumbiting";
		initConfig.ajaxCountSubmit = 0;
		//��������ҪУ��Ŀؼ�,�������ajaxValidator����ֱ�Ӵ���
		$.each(initConfig.validObjects,function()
		{
			if($(this).length==0){return true};
			if (this.settings[0].bind && this.validatorAjaxIndex!=undefined && this.onceValided == undefined) {
				returnObj = $.formValidator.oneIsValid(this.id);
				if (returnObj.ajax == this.validatorAjaxIndex) {
					initConfig.status = "sumbitingWithAjax";
					$.formValidator.ajaxValid(returnObj);
				}
			}
		});
		//������ύ��ʱ���д���ajaxValidator�����еĴ���������ajax�ﴦ��
		if(initConfig.ajaxCountSubmit > 0){return false}
		//��������ҪУ��Ŀؼ�
		$.each(initConfig.validObjects,function()
		{
			//ֻУ��󶨵Ŀؼ�
			if($(this).length==0){return true};
			if(this.settings[0].bind){
				name = this.name;
				//��ͬnameֻУ��һ��
				if (name_list.indexOf("^"+name+"^") == -1) {
					onceValided = this.onceValided == undefined ? false : this.onceValided;
					if(name){name_list = name_list + name + "^"};
					returnObj = $.formValidator.oneIsValid(this.id);
					if (returnObj) {
						//У��ʧ��,��ȡ��һ�������������Ϣ��ID
						if (!returnObj.isValid) {
							//��¼����ajaxValidatorУ�麯����У����
							isValid = false;
							errorMessage = returnObj.errormsg == "" ? returnObj.setting.onError : returnObj.errormsg;
							errorlist[errorlist.length] = errorMessage;
							if (thefirstid == null) {thefirstid = returnObj.id};
							if(firstErrorMessage==""){firstErrorMessage=errorMessage};
						}
						//Ϊ�˽��ʹ��ͬ��TIP��ʾ����:����ĳɹ���ʧ�ܶ�������ǰ���ʧ��
						if (initConfig.mode != "AlertTip") {
							var tipID = this.settings[0].tipID;
							if (error_tip.indexOf("^" + tipID + "^") == -1) {
								if (!returnObj.isValid) {error_tip = error_tip + tipID + "^"};
								$.formValidator.showMessage(returnObj);
							}
						}
						//��������У��ģ��ҵ�ǰУ��ʧ�ܣ���ֱ�ӷ���
						if(initConfig.oneByOneVerify && !returnObj.isValid){return false}
					}
				}
			}
		});
		
		//�ɹ���ʧ�ܽ��лص������Ĵ������Լ��ɹ���Ļҵ��ύ��ť�Ĺ���
		if(isValid)
		{
            if(!initConfig.onSuccess()){return false};
			if(initConfig.submitOnce){$(":submit,:button,:reset").attr("disabled",true);}
		}
		else
		{
			initConfig.onError(firstErrorMessage, $("#" + thefirstid).get(0), errorlist);
			if (thefirstid && initConfig.errorFocus) {$("#" + thefirstid).focus()};
		}
		initConfig.status="init";
		if(isValid && initConfig.debug){alert("���������ڵ���ģʽ(debug:true)�������ύ");}
		return !initConfig.debug && isValid;
	},
	
	//��������
	ajaxForm : function(validatorGroup,option,formid)
	{
		if(validatorGroup == undefined){validatorGroup = "1"};
		var setting = {};
		$.extend(true, setting, ajaxForm_setting, option || {});
		var initConfig = $("body").data(validatorGroup);	
		if(formid==undefined){
			formid = initConfig.formID;
			if(formid==""){alert('����IDδ����');return false;};
		};
		if(!$.formValidator.pageIsValid(validatorGroup)){return false};
		var ls_url = setting.url;
		var ls_data = setting.data;
		var parm = $.formValidator.serialize('#'+formid);
		if(setting.type=="POST"){
			ls_data = ls_data + (ls_data != "" ? ("&" + parm) : parm)
		}else{
			ls_url = ls_url + (ls_url.indexOf("?") > -1 ? ("&" + parm) : ("?" + parm))
		}
        $.ajax(
		{	
			type : setting.type, 
			url : ls_url, 
			cache : setting.cache,
			data : ls_data, 
			async : setting.async, 
			timeout : setting.timeout, 
			dataType : setting.dataType, 
			beforeSend : function(jqXHR, configs){//�ٷ�����û�з�������֮ǰ���Ȼص��ύ��ť
				if(setting.buttons && setting.buttons.length > 0){setting.buttons.attr({"disabled":true})};
				return setting.beforeSend(jqXHR, configs);
			},
			success : function(data, textStatus, jqXHR){setting.success(data, textStatus, jqXHR);},
			complete : function(jqXHR, textStatus){
				if(setting.buttons && setting.buttons.length > 0){setting.buttons.attr({"disabled":false})};
				setting.complete(jqXHR, textStatus);
			},
			error : function(jqXHR, textStatus, errorThrown){setting.error(jqXHR, textStatus, errorThrown);}
		});
	},
	
	//�ѱ���decodeURIComponent��ת֮����escape
	serialize : function(objs,initConfig)
	{
		if(initConfig!=undefined){$.formValidator.resetInputValue(true,initConfig)};
		var parmString = $(objs).serialize();
		if(initConfig!=undefined){$.formValidator.resetInputValue(false,initConfig)};
		var parmArray = parmString.split("&");
		var parmStringNew="";
		$.each(parmArray,function(index,data){
			var li_pos = data.indexOf("=");	
			if(li_pos >0){
				var name = data.substring(0,li_pos);
				var value = escape(decodeURIComponent(data.substr(li_pos+1)));
				var parm = name+"="+value;
				parmStringNew = parmStringNew=="" ? parm : parmStringNew + '&' + parm;
			}
		});
		return parmStringNew;
	},

	//ajaxУ��
	ajaxValid : function(e)
	{
		var id = e.id;
	    var srcjo = $("#"+id);
		var elem = srcjo.get(0);
		var initConfig = e.initConfig;
		var settings = elem.settings;
		var setting = settings[e.ajax];
		var ls_url = setting.url;
		var ls_data = setting.data;
		//��ȡҪ���ݵĲ���
		var validatorGroup = elem.validatorGroup;
		var initConfig = $("body").data(validatorGroup);
		var parm = $.formValidator.serialize(initConfig.ajaxObjects);
		//���Ӵ����Ŀؼ���������������ݵĲ���
		parm = "clientid=" + id + "&" +(setting.randNumberName ? setting.randNumberName+"="+((new Date().getTime())+Math.round(Math.random() * 10000)) : "") + (parm.length > 0 ? "&" + parm : "");
		if(setting.type=="POST"){
			ls_data = ls_data + (ls_data != "" ? ("&" + parm) : parm)
		}else{
			ls_url = ls_url + (ls_url.indexOf("?") > -1 ? ("&" + parm) : ("?" + parm))
		}
		//����ajax����
		$.ajax(
		{	
			type : setting.type, 
			url : ls_url, 
			cache : setting.cache,
			data : ls_data, 
			async : setting.async, 
			timeout : setting.timeout, 
			dataType : setting.dataType, 
			success : function(data, textStatus, jqXHR){
				var lb_ret,ls_status,ls_msg,lb_isValid = false;
				$.formValidator.dealAjaxRequestCount(validatorGroup,-1);
				e.dom.onceValided = true;
				//����ҵ���ж�������ʾ��Ϣ
				lb_ret = setting.success(data, textStatus, jqXHR);
				if((typeof lb_ret)=="string")
				{
					ls_status = "onError";
					ls_msg = lb_ret;
				}
				else if(lb_ret){
					lb_isValid = true;
					ls_status = "onCorrect";
					ls_msg = settings[0].onCorrect;
				}else{
					ls_status = "onError";
					ls_msg = $.formValidator.getStatusText(elem,setting.onError);
				}
				setting.isValid = lb_isValid;
				$.formValidator.setTipState(elem,ls_status,ls_msg);
				//�ύ��ʱ�򴥷���ajaxУ�飬��ajaxУ����ɣ�����������У��
				if(e.initConfig.status=="sumbitingWithAjax" && e.initConfig.ajaxCountSubmit == 0)
				{
					if (initConfig.formID != "") {
						$('#' + initConfig.formID).trigger('submit');
					}
				}
			},
			complete : function(jqXHR, textStatus){
				if(setting.buttons && setting.buttons.length > 0){setting.buttons.attr({"disabled":false})};
				setting.complete(jqXHR, textStatus);
			}, 
			beforeSend : function(jqXHR, configs){
				//���ؼ��������У�飬���ж��ϴ�
				if (this.lastXMLHttpRequest) {this.lastXMLHttpRequest.abort()};
				this.lastXMLHttpRequest = jqXHR;
				//�ٷ�����û�з�������֮ǰ���Ȼص��ύ��ť
				if(setting.buttons && setting.buttons.length > 0){setting.buttons.attr({"disabled":true})};
				var lb_ret = setting.beforeSend(jqXHR,configs);
				var isValid = false;
				//�����Ƿ�ɹ�������ʧ�ܴ���
				setting.isValid = false;
				if((typeof lb_ret)=="boolean" && lb_ret)
				{
					isValid = true;
					$.formValidator.setTipState(elem,"onLoad",settings[e.ajax].onWait);
				}else
				{
					isValid = false;
					$.formValidator.setTipState(elem,"onError",lb_ret);
				}
				setting.lastValid = "-1";
				if(isValid){$.formValidator.dealAjaxRequestCount(validatorGroup,1);}
				return isValid;
			}, 
			error : function(jqXHR, textStatus, errorThrown){
				$.formValidator.dealAjaxRequestCount(validatorGroup,-1);
			    $.formValidator.setTipState(elem,"onError",$.formValidator.getStatusText(elem,setting.onError));
			    setting.isValid = false;
				e.dom.onceValided = true;
				setting.error(jqXHR, textStatus, errorThrown);
			},
			processData : setting.processData 
		});
	},
	
	//����ajax���������
	dealAjaxRequestCount : function(validatorGroup,val)
	{
		var initConfig = $("body").data(validatorGroup);
		initConfig.ajaxCountValid = initConfig.ajaxCountValid + val;
		if (initConfig.status == "sumbitingWithAjax") {
			initConfig.ajaxCountSubmit = initConfig.ajaxCountSubmit + val;
		}
	},

	//���������ʽ����У�飨Ŀǰֻ���input��textarea��
	regexValid : function(returnObj)
	{
		var id = returnObj.id;
		var setting = returnObj.setting;
		var srcTag = $("#"+id).get(0).tagName;
		var elem = $("#"+id).get(0);
		var isValid;
		//����������������ʽ���ͽ��б���ʽУ��
		if(elem.settings[0].empty && elem.value==""){
			setting.isValid = true;
		}
		else 
		{
			var regexArray = setting.regExp;
			setting.isValid = false;
			if((typeof regexArray)=="string") regexArray = [regexArray];
			$.each(regexArray, function() {
			    var r = this;
			    if(setting.dataType=="enum"){r = eval("regexEnum."+r);}			
			    if(r==undefined || r=="") 
			    {
			        return false;
			    }
			    isValid = (new RegExp(r, setting.param)).test($(elem).val());
			    
			    if(setting.compareType=="||" && isValid)
			    {
			        setting.isValid = true;
			        return false;
			    }
			    if(setting.compareType=="&&" && !isValid) 
			    {
			        return false
			    }
            });
            if(!setting.isValid) setting.isValid = isValid;
		}
	},
	
	//����У�顣����true/false��ʾУ���Ƿ�ɹ�;�����ַ�����ʾ������Ϣ��У��ʧ��;���û�з���ֵ��ʾ����������У��ɹ�
	functionValid : function(returnObj)
	{
		var id = returnObj.id;
		var setting = returnObj.setting;
	    var srcjo = $("#"+id);
		var lb_ret = setting.fun(srcjo.val(),srcjo.get(0));
		if(lb_ret != undefined) 
		{
			if((typeof lb_ret) === "string"){
				setting.isValid = false;
				returnObj.errormsg = lb_ret;
			}else{
				setting.isValid = lb_ret;
			}
		}else{
		    setting.isValid = true;
		}
	},
	
	//��input��select���Ϳؼ�����У��
	inputValid : function(returnObj)
	{
		var id = returnObj.id;
		var setting = returnObj.setting;
		var srcjo = $("#"+id);
		var elem = srcjo.get(0);
		var val = srcjo.val();
		var sType = elem.type;
		var len = $.formValidator.getLength(id);
		var empty = setting.empty,emptyError = false;
		switch(sType)
		{
			case "text":
			case "hidden":
			case "password":
			case "textarea":
			case "file":
				if (setting.type == "size") {
					empty = setting.empty;
					if(!empty.leftEmpty){
						emptyError = (val.replace(/^[ \s]+/, '').length != val.length);
					}
					if(!emptyError && !empty.rightEmpty){
						emptyError = (val.replace(/[ \s]+$/, '').length != val.length);
					}
					if(emptyError && empty.emptyError){returnObj.errormsg= empty.emptyError}
				}
			case "checkbox":
			case "select-one":
			case "select-multiple":
			case "radio":
				var lb_go_on = false;
				if(sType=="select-one" || sType=="select-multiple"){setting.type = "size";}
				var type = setting.type;
				if (type == "size") {		//���������ַ����ȣ�������У��
					if(!emptyError){lb_go_on = true}
					if(lb_go_on){val = len}
				}
				else if (type =="date" || type =="datetime")
				{
					var isok = false;
					if(type=="date"){lb_go_on = isDate(val)};
					if(type=="datetime"){lb_go_on = isDate(val)};
					if(lb_go_on){val = new Date(val);setting.min=new Date(setting.min);setting.max=new Date(setting.max);};
				}else{
					stype = (typeof setting.min);
					if(stype =="number")
					{
						val = (new Number(val)).valueOf();
						if(!isNaN(val)){lb_go_on = true;}
					}
					if(stype =="string"){lb_go_on = true;}
				}
				setting.isValid = false;
				if(lb_go_on)
				{
					if(val < setting.min || val > setting.max){
						if(val < setting.min && setting.onErrorMin){
							returnObj.errormsg= setting.onErrorMin;
						}
						if(val > setting.min && setting.onErrorMax){
							returnObj.errormsg= setting.onErrorMax;
						}
					}
					else{
						setting.isValid = true;
					}
				}
				break;
		}
	},
	
	//�������ؼ����бȽ�У��
	compareValid : function(returnObj)
	{
		var id = returnObj.id;
		var setting = returnObj.setting;
		var srcjo = $("#"+id);
	    var desjo = $("#"+setting.desID );
		var ls_dataType = setting.dataType;
		
		curvalue = srcjo.val();
		ls_data = desjo.val();
		if(ls_dataType=="number")
        {
            if(!isNaN(curvalue) && !isNaN(ls_data)){
				curvalue = parseFloat(curvalue);
                ls_data = parseFloat(ls_data);
			}
			else{
			    return;
			}
        }
		if(ls_dataType=="date" || ls_dataType=="datetime")
		{
			var isok = false;
			if(ls_dataType=="date"){isok = (isDate(curvalue) && isDate(ls_data))};
			if(ls_dataType=="datetime"){isok = (isDateTime(curvalue) && isDateTime(ls_data))};
			if(isok){
				curvalue = new Date(curvalue);
				ls_data = new Date(ls_data)
			}
			else{
				return;
			}
		}
		
	    switch(setting.operateor)
	    {
	        case "=":
	            setting.isValid = (curvalue == ls_data);
	            break;
	        case "!=":
	            setting.isValid = (curvalue != ls_data);
	            break;
	        case ">":
	            setting.isValid = (curvalue > ls_data);
	            break;
	        case ">=":
	            setting.isValid = (curvalue >= ls_data);
	            break;
	        case "<": 
	            setting.isValid = (curvalue < ls_data);
	            break;
	        case "<=":
	            setting.isValid = (curvalue <= ls_data);
	            break;
			default :
				setting.isValid = false;
				break; 
	    }
	},
	
	//��ȡ����У��ȼ�
	passwordValid : function(elem)
	{
	    var setting = elem.settings[elem.validatorPasswordIndex];
	    var pwd = elem.value;
	    //�Ƿ�Ϊ�����ַ�
	    function isContinuousChar(str){
		    var str = str.toLowerCase();
		    var flag = 0;
		    for(var i=0;i<str.length;i++){
			    if(str.charCodeAt(i) != flag+1 && flag!=0)
				    return false;
			    else
				    flag = str.charCodeAt(i);
		    }
		    return true;
	    }
	    //�Ƿ��ַ�����ͬ
	    function isSameChar(str){
		    var str = str.toLowerCase();
		    var flag = 0;
		    for(var i=0;i<str.length;i++){
			    if(str.charCodeAt(i) != flag && flag!=0)
				    return false;
			    else
				    flag = str.charCodeAt(i);
		    }
		    return true;
	    }
	    //��ȡ������ڵ�λ�ã���1��ʾ
	    function getFlag(val,sum,index)
	    {
		    if(sum==undefined){sum=[0,0,0,0]}
		    if(index==undefined){index=-1};
		    index ++;
		    sum[index] = val % 2;
		    val = Math.floor(val / 2);
		    if(val==1 || val==0){sum[index+1] = val;return sum}
		    sum = getFlag(val,sum,index);
		    return sum;
	    }
    	
	    //�ж��������λ�õ�������
        if(pwd==""){return 0};
	    if(setting.onErrorContinueChar!="" && isContinuousChar(pwd)){return -1};
	    if(setting.onErrorSameChar!="" && isSameChar(pwd)){return -2};
		if(setting.compareID!="" && $("#"+setting.compareID).val()==pwd){return -3};
	    var sum1 = [0, 0, 0, 0];
	    var specicalChars = "!,@,#,$,%,\^,&,*,?,_,~";
	    var len = pwd.length;
	    for (var i=0; i< len; i++) {
		    var c = pwd.charCodeAt(i);
		    if (c >=48 && c <=57){  //����
			    sum1[0] += 1;
		    }else if (c >=97 && c <=122){ //Сд��ĸ
			    sum1[1] += 1;
		    }else if (c >=65 && c <=90){ //��д��ĸ
			    sum1[2] += 1;
		    }else if(specicalChars.indexOf(pwd.substr(i,1))>=0){ //�����ַ�
			    sum1[3] += 1; 
		    }
	    }
	    
	    //����json������ȡ�ַ���
	    var returnLevel = 0;
	    var hasFind = true;
	    $.each(passwordStrengthRule,function(j,n){
		    var level = n.level;
		    var rules = n.rule;
		    $.each(rules,function(i,rule){
			    var index = 0;
			    //���α������еġ�������(��λ��==1)�Ľڵ�
			    hasFind = true;
			    $.each(getFlag(rule.flag),function(k,value){
				    if(value==1){
					    val = rule.value[index++];
					    var val = val == 0 ? len : (val > len ? len : val);
					    if(sum1[k] < val){hasFind = false;return false;}
				    }
			    });
			    if(hasFind){returnLevel = level;return false;}
		    });
		    if(hasFind){returnLevel = level;}
	    });
	    return returnLevel;
	},
	
	//��λƯ����
	localTooltip : function(e)
	{
		e = e || window.event;
		var mouseX = e.pageX || (e.clientX ? e.clientX + document.body.scrollLeft : 0);
		var mouseY = e.pageY || (e.clientY ? e.clientY + document.body.scrollTop : 0);
		$("#fvtt").css({"top":(mouseY+2)+"px","left":(mouseX-40)+"px"});
	},
	
	reloadAutoTip : function(validatorGroup)
	{
		alert('ssss');
		if(validatorGroup == undefined) validatorGroup = "1";
		var initConfig = $("body").data(validatorGroup);
		$.each(initConfig.validObjects,function()
		{
			if(initConfig.mode == "AutoTip")
			{
				//��ȡ���ID����Զ�λ�ؼ���ID������
				var setting = this.settings[0];
				var relativeID = "#"+setting.relativeID;
				var offset = $(relativeID ).offset();
				var y = offset.top;
				var x = $(relativeID ).width() + offset.left;
				$("#"+setting.tipID).parent().show().css({left: x+"px", top: y+"px"});			
			}
		});
	},
	
	getStatusText : function(elem,obj)
	{
		return ($.isFunction(obj) ? obj($(elem).val()) : obj);
	},
	
	resetInputValue : function(real,initConfig,id)
	{
		var showTextObjects;
		if(id){
			showTextObjects = $("#"+id);
		}else{
			showTextObjects = $(initConfig.showTextObjects);
		}
		showTextObjects.each(function(index,elem){
			if(elem.isInputControl){
				var showText = elem.settings[0].onShowText;
				if(real && showText==elem.value){elem.value = ""};
				if(!real && showText!="" && elem.value == ""){elem.value = showText};
			}
		});
	}
};

//ÿ��У��ؼ������ʼ����
$.fn.formValidator = function(cs) 
{
	cs = cs || {};
	var setting = {};

	//��ȡ��У�����ȫ��������Ϣ
	if(cs.validatorGroup == undefined){cs.validatorGroup = "1"};
	
	//�Ⱥϲ���������(��ȿ���)
	$.extend(true,setting, formValidator_setting);
	
	var initConfig = $("body").data(cs.validatorGroup);
	
	//У�������ţ����ܼ�¼��
	initConfig.validCount += 1;
	
	//���Ϊ����ģʽ��tipCssҪ�������ó�ʼֵ
	if(initConfig.mode == "SingleTip"){setting.tipCss = {left:10,top:1,width:22,height:22,display:"none"}};
	
	//������Ϣ��ʾģʽ���Զ��޸�����
	if(initConfig.mode == "AlertTip"){setting.autoModify=true};
	
	//�Ⱥϲ���������(��ȿ���)
	$.extend(true,setting, cs || {});
	return this.each(function(e)
	{
		//��¼�ÿؼ���У��˳��ź�У�����
		this.validatorIndex = initConfig.validCount - 1;
		this.validatorGroup = cs.validatorGroup;
		var jqobj = $(this);
		//�Զ��γ�TIP
		var setting_temp = {};
		$.extend(true,setting_temp, setting);
		//�ж��Ƿ���ID
		var id = jqobj.attr('id');
		if(!id)
		{ 
			id = Math.ceil(Math.random()*50000000); 
			jqobj.attr('id', id);
		}
		var tip = setting_temp.tipID ? setting_temp.tipID : id+"Tip";
        
		if(initConfig.mode == "AutoTip" || initConfig.mode == "FixTip")
		{			
			var tipDiv = $("#"+tip);
			//��ȡ���ID����Զ�λ�ؼ���ID������
			if(initConfig.mode == "AutoTip" && tipDiv.length==0)
			{		
				var relativeID = setting_temp.relativeID ? setting_temp.relativeID : id;
				var offset = $("#"+relativeID ).offset();
				setting_temp.tipCss.top = offset.top + setting_temp.tipCss.top;
				setting_temp.tipCss.left = $("#"+relativeID ).width() + offset.left + setting_temp.tipCss.left;
				tipDiv = $("<div style='position:absolute;' id='"+tip+"'></div>").appendTo($("body"));
				tipDiv.css(setting_temp.tipCss);
				setting.relativeID = relativeID ;
			}
			tipDiv.css("margin","0px").css("padding","0px").css("background","transparent");
		}else if(initConfig.mode == "SingleTip"){
			jqobj.showTooltips();
		}
		
		//ÿ���ؼ���Ҫ�������������Ϣ��Ϊ��ȡ�����㣬����һ�ݿؼ��������õ��ؼ���
		setting.tipID = tip;
		setting.pwdTipID = setting_temp.pwdTipID ? setting_temp.pwdTipID : setting.tipID;
		setting.fixTipID = setting_temp.fixTipID ? setting_temp.fixTipID : id+"FixTip";
		$.formValidator.appendValid(id,setting);

		//����ؼ�ID
		var validIndex = $.inArray(jqobj,initConfig.validObjects);
		if(validIndex == -1)
		{
			if (setting_temp.ajax) {initConfig.ajaxObjects.push(this)}
			initConfig.validObjects.push(this);
		}else{
			initConfig.validObjects[validIndex] = this;
		}

		//��ʼ����ʾ��Ϣ
		if(initConfig.mode != "AlertTip"){
			$.formValidator.setTipState(this,"onShow",setting.onShow);
		}

		var srcTag = this.tagName.toLowerCase();
		var stype = this.type;
		var defaultval = setting.defaultValue;
		var isInputControl = stype == "password" || stype == "text" || stype == "textarea";
		this.isInputControl = isInputControl;
		//����Ĭ��ֵ
		if(defaultval){
			jqobj.val(defaultval);
		}
		//�̶���ʾ����
		var fixTip = $("#"+setting.fixTipID);
		var showFixText = setting.onShowFixText;
		if(fixTip.length==1 && onMouseOutFixTextHtml!="" && onMouseOnFixTextHtml!="" && showFixText != "")
		{
			jqobj.hover(
				function () {
					fixTip.html(onMouseOnFixTextHtml.replace(/\$data\$/g, showFixText));
				},
				function () {
					fixTip.html(onMouseOutFixTextHtml.replace(/\$data\$/g, showFixText));
				}
			);
			fixTip.css("padding","0px 0px 0px 0px").css("margin","0px 0px 0px 0px").html(onMouseOutFixTextHtml.replace(/\$data\$/g, setting.onShowFixText)); 
		}
		//��ȡ������ڵ���ʾ����
        var showText = setting.onShowText;
		if(srcTag == "input" || srcTag=="textarea")
		{
			if (isInputControl) {
				if(showText !="" && jqobj.val() == ""){
					showObjs = initConfig.showTextObjects;
					initConfig.showTextObjects = showObjs + (showObjs != "" ? ",#" : "#") + id;
					jqobj.val(showText);
					jqobj.css("color",setting.onShowTextColor.mouseOutColor);
				}
			}
			//ע���ý�����¼����ı���ʾ��������ֺ���ʽ������ԭֵ
			jqobj.focus(function()
			{	
				if (isInputControl) {
					var val = jqobj.val();
					this.validValueOld = val;
					if(showText==val){
						this.value = "";
						jqobj.css("color",setting.onShowTextColor.mouseOnColor);
					}
				};
				if(initConfig.mode != "AlertTip"){
					//����ԭ����״̬
					var tipjq = $("#"+tip);
					this.lastshowclass = tipjq.attr("class");
					this.lastshowmsg = tipjq.text();
					$.formValidator.setTipState(this,"onFocus",setting.onFocus);
				};
				if(this.validatorPasswordIndex > 0){$("#"+setting.pwdTipID).show();jqobj.trigger('keyup');}
			});
			//����ʱ�򴥷�У��
		    jqobj.bind("keyup",function(){
		        if(this.validatorPasswordIndex > 0)
		        {
					try{
						var returnObj = $.formValidator.oneIsValid(id);
						var level = $.formValidator.passwordValid(this);
						if(level < 0){level=0};
						if(!returnObj.isValid){level = 0};
						$("#"+setting.pwdTipID).show();
						$("#"+setting.pwdTipID).html(passwordStrengthStatusHtml[level]);
					}catch(e)
					{
						alert("����ǿ��У��ʧ��,����ԭ��:����passwordStrengthStatusHtml�﷨�������Ϊ����)");
					}
	            }
		    });
			//ע��ʧȥ������¼�������У�飬�ı���ʾ��������ֺ���ʽ����������ʾ����
			jqobj.bind(setting.triggerEvent, function(){
				var settings = this.settings;
				//�������ýص����ҵĿո�
				if(settings[0].leftTrim){this.value = this.value.replace(/^\s*/g, "")}
				if(settings[0].rightTrim){this.value = this.value.replace(/\s*$/g, "")}
				//�ָ�Ĭ��ֵ
				if(isInputControl){
					if(this.value == "" && showText != ""){this.value = showText}
					if(this.value == showText){jqobj.css("color",setting.onShowTextColor.mouseOutColor)}
				}
				//������Ч��У��
				var returnObj = $.formValidator.oneIsValid(id);
				if(returnObj==null){return}
				if(returnObj.ajax >= 0) 
				{
					$.formValidator.showAjaxMessage(returnObj);
				}
				else
				{
					var showmsg = $.formValidator.showMessage(returnObj);
					if(!returnObj.isValid)
					{
						//�Զ���������
						var auto = setting.autoModify && isInputControl;
						if(auto)
						{
							$(this).val(this.validValueOld);
							if(initConfig.mode != "AlertTip"){$.formValidator.setTipState(this,"onShow",$.formValidator.getStatusText(this,setting.onCorrect))};
						}
						else
						{
							if(initConfig.forceValid || setting.forceValid){
								intiConfig.onAlert(showmsg);this.focus();
							}
						}
					}
				}
			});
		} 
		else if (srcTag == "select")
		{
			jqobj.bind({
				//��ý���
				focus: function(){	
					if (initConfig.mode != "AlertTip") {
						$.formValidator.setTipState(this, "onFocus", setting.onFocus)
					};
				},
				//ʧȥ����
				blur: function(){
					if(this.validValueOld==undefined || this.validValueOld==jqobj.val()){$(this).trigger("change")}
				},
				//ѡ����Ŀ�󴥷�
				change: function(){
					var returnObj = $.formValidator.oneIsValid(id);	
					if(returnObj==null){return;}
					if ( returnObj.ajax >= 0){
						$.formValidator.showAjaxMessage(returnObj);
					}else{
						$.formValidator.showMessage(returnObj); 
					}
				}
			});
		}
	});
}; 

$.fn.inputValidator = function(controlOptions)
{
	var settings = {};
	$.extend(true, settings, inputValidator_setting, controlOptions || {});
	return this.each(function(){
		$.formValidator.appendValid(this.id,settings);
	});
};

$.fn.compareValidator = function(controlOptions)
{
	var settings = {};
	$.extend(true, settings, compareValidator_setting, controlOptions || {});
	return this.each(function(){
		$.formValidator.appendValid(this.id,settings);
	});
};

$.fn.regexValidator = function(controlOptions)
{
	var settings = {};
	$.extend(true, settings, regexValidator_setting, controlOptions || {});
	return this.each(function(){
		$.formValidator.appendValid(this.id,settings);
	});
};

$.fn.functionValidator = function(controlOptions)
{
	var settings = {};
	$.extend(true, settings, functionValidator_setting, controlOptions || {});
	return this.each(function(){
		$.formValidator.appendValid(this.id,settings);
	});
};

$.fn.ajaxValidator = function(controlOptions)
{
	var settings = {};
	$.extend(true, settings, ajaxValidator_setting, controlOptions || {});
	return this.each(function()
	{
		var initConfig = $("body").data(this.validatorGroup);
		if($.inArray(this,initConfig.ajaxObjects) == -1)
		{
			initConfig.ajaxObjects.push(this);
		}
		this.validatorAjaxIndex = $.formValidator.appendValid(this.id,settings);
	});
};

$.fn.passwordValidator = function(controlOptions)
{
	//Ĭ������
	var settings = {};
	$.extend(true, settings, passwordValidator_setting, controlOptions || {});
	return this.each(function()
	{
	    this.validatorPasswordIndex = $.formValidator.appendValid(this.id,settings);
	});
};

//ָ���ؼ���ʾͨ����ͨ����ʽ
$.fn.defaultPassed = function(onShow)
{
	return this.each(function()
	{
		var settings = this.settings;
		settings[0].defaultPassed = true;
		this.onceValided = true;
		for ( var i = 1 ; i < settings.length ; i ++ )
		{   
			settings[i].isValid = true;
			if(!($("body").data(settings[0].validatorGroup).mode == "AlertTip")){
				this.lastshowclass = onShow ? "onShow" : "onCorrect";
				this.lastshowmsg = onShow ? settings[0].onShow : settings[0].onCorrect;
				$.formValidator.setTipState(this,this.lastshowclass, this.lastshowmsg);
			}
		}
	});
};

//ָ���ؼ����μ�У��
$.fn.unFormValidator = function(unbind)
{
	return this.each(function()
	{
	    if(this.settings)
	    {
		    this.settings[0].bind = !unbind;
		    if(unbind){
			    $("#"+this.settings[0].tipID).hide();
		    }else{
			    $("#"+this.settings[0].tipID).show();
		    }
		}
	});
};

//��ʾƯ����ʾ��
$.fn.showTooltips = function()
{
	if($("body [id=fvtt]").length==0){
		fvtt = $("<div id='fvtt' style='position:absolute;z-index:56002'></div>");
		$("body").append(fvtt);
		fvtt.before("<iframe index=0 src='about:blank' class='fv_iframe' scrolling='no' frameborder='0'></iframe>");
		
	}
	return this.each(function()
	{
		jqobj = $(this);
		s = $("<span class='top' id=fv_content style='display:block'></span>");
		b = $("<b class='bottom' style='display:block' />");
		this.tooltip = $("<span class='fv_tooltip' style='display:block'></span>").append(s).append(b).css({"filter":"alpha(opacity:95)","KHTMLOpacity":"0.95","MozOpacity":"0.95","opacity":"0.95"});
		//ע���¼�
		jqobj.bind({
			mouseover : function(e){
				$("#fvtt").empty().append(this.tooltip).show();
				$("#fv_content").html(this.Tooltip);
				$.formValidator.localTooltip(e);
			},
			mouseout : function(){
				$("#fvtt").hide();
			},
			mousemove: function(e){
				$.formValidator.localTooltip(e);
			}
		});
	});
}
})(jQuery);

var initConfig_setting = 
{
	theme:"Default",
	validatorGroup : "1",						//�����
	formID:"",					//����ID
	submitOnce:false,							//ҳ���Ƿ��ύһ�Σ�����ͣ��
	ajaxForm : null,       //�����Ϊnull����ʾ��������ajax�ύ
	mode : "FixTip",			//��ʾģʽ
	errorFocus:true,			//��һ������Ŀؼ���ý���
	wideWord:true,				//һ�����ֵ���2����
	forceValid:false,							//�ؼ�������ȷ֮�󣬲�����ʧȥ��
	debug:false,								//����ģʽ��
	inIframe:false,
	onSuccess: function() {return true},		//�ύ�ɹ���Ļص�����
	onError: $.noop,						//�ύʧ�ܵĻص�������
	onAlert: function() {alert(arguments[0])},
	status:"",					//�ύ��״̬��submited��sumbiting��sumbitingWithAjax
	ajaxPrompt : "��ǰ���������ڽ��з�������У�飬���Ժ�",	//�ؼ�ʧȥ����󣬴���ajaxУ�飬û�з��ؽ��ǰ�Ĵ�����ʾ
	validCount:0,			//��ajaxValidator�Ŀؼ�����
	ajaxCountSubmit:0,		//�ύ��ʱ�򴥷���ajax��֤����
	ajaxCountValid:0,		//ʧȥ����ʱ�򴥷���ajax��֤����
	validObjects:[],							//�μ�У��Ŀؼ�����
	ajaxObjects:[],							//�����������Ŀؼ��б�
	showTextObjects:"",
	validateType : "initConfig",
	offsetChrome : {left:42,top:0},
	oneByOneVerify : false
};

var formValidator_setting = 
{
	validatorGroup : "1",
	onShowText : "",
	//�޸ģ����ı�ҳ���κ�Ԫ��
	//onShowTextColor:{mouseOnColor:"#000000",mouseOutColor:"#999999"},
	onShowTextColor:{},
	onShowFixText:"",
	onShow :"����������",
	onFocus: "����������",
	onCorrect: "������ȷ",
	onEmpty: "��������Ϊ��",
	empty :false,
	autoModify : false,
	defaultValue : null,
	bind : true,
	ajax : false,
	validateType : "formValidator",
	tipCss : 
	{
		//�޸ģ�λ����Ϣ
		left:18,
		top:-9,
		height:20,
		width:280
	},
	triggerEvent:"blur",
	forceValid : false,
	tipID : null,
	pwdTipID : null,
	fixTipID : null,
	relativeID : null,
	index : 0,
	leftTrim : false,
	rightTrim : false
};

var inputValidator_setting = 
{
	isValid : false,
	type : "size",
	min : 0,
	max : 99999,
	onError:"�������",
	validateType:"inputValidator",
	empty:{leftEmpty:true,rightEmpty:true,leftEmptyError:null,rightEmptyError:null}
};

var compareValidator_setting = 
{
	isValid : false,
	desID : "",
	operateor :"=",
	onError:"�������",
	validateType:"compareValidator"
};

var regexValidator_setting = 
{
	isValid : false,
	regExp : "",
	param : "i",
	dataType : "string",
	compareType : "||",
	onError:"����ĸ�ʽ����ȷ",
	validateType:"regexValidator"
};

var ajaxForm_setting = 
{
	type : "GET",
	url : window.location.href,
	dataType : "html",
	timeout : 100000,
	data : null,
	type : "GET",
	async : true,
	cache : false,
	buttons : null,
	beforeSend : function(){return true;},
	success : function(){return true;},
	complete : $.noop,
	processData : true,
	error : $.noop
};

var ajaxValidator_setting = 
{
	isValid : false,
	lastValid : "",
	oneceValid : false,
	randNumberName : "rand",
	onError:"������У��û��ͨ��",
	onWait:"���ڵȴ���������������",
	validateType:"ajaxValidator"
};
$.extend(true,ajaxValidator_setting,ajaxForm_setting);

var functionValidator_setting = 
{
	isValid : true,
	fun : function(){this.isValid = true;},
	validateType:"functionValidator",
	onError:"�������"
};

var passwordValidator_setting = {
	isValid : true,
	compareID : "",
	validateType:"passwordValidator",
	onErrorContinueChar:"�����ַ�Ϊ�����ַ���������",
	onErrorSameChar:"�����ַ�����ͬ��������",
	onErrorCompareSame:"�������û�����ͬ��������"
};

var fv_scriptSrc = document.getElementsByTagName('script')[document.getElementsByTagName('script').length - 1].src;