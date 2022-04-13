package cn.itcast.wanxinp2p.account.service;

import cn.itcast.wanxinp2p.account.common.AccountErrorCode;
import cn.itcast.wanxinp2p.common.domain.BusinessException;
import cn.itcast.wanxinp2p.common.domain.CommonErrorCode;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import cn.itcast.wanxinp2p.common.util.OkHttpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author old money
 * @create 2022-02-10 16:53
 */
@Service
public class SmsService {


    /**
     * 配置中获取短信验证码微服务的URL地址
     */
    @Value("${sms.url}")
    private String smsURL;


    /**
     * 配置中是否通过短信获取验证码的属性
     */
    @Value("${sms.enable}")
    private Boolean smsEnable;


    /**
     * 发送并获取短信验证码
     * @param mobile
     * @return
     */
    public RestResponse getSMSCode(String mobile){
        if (smsEnable){

            /**
             * 通过 OkHttpUtil 工具类向验证码服务请求验证码
             */
            return OkHttpUtil.post(smsURL+"/ generate?effectiveTime=300&name=sms","{\"mobile\":"+mobile+"}");
        }
      return RestResponse.success();
    }



    /**
     * 校验短信验证码
     * @param key 校验标识 redis中的key
     * @param code 短信验证码 redis中的值
     */
    public void verifySmsCode(String key,String code){
        if (smsEnable){
//          StringBuilder params = new StringBuilder("/verify?name=sms");
//          params.append("&verificationKey").append(key).append("&verificationCode=").append(code);
//
//          OkHttpUtil.post(smsURL+params,"");

            RestResponse restResponse = OkHttpUtil.post(smsURL + "/ verify?name=sms & " + "verificationkey=" + key + " & verificationCode= " + code, "");

            if (restResponse.getCode() != CommonErrorCode.SUCCESS.getCode() || restResponse.getResult().toString().equalsIgnoreCase("false")){
                throw  new BusinessException(AccountErrorCode.E_140152);
            }
        }
    }



}
