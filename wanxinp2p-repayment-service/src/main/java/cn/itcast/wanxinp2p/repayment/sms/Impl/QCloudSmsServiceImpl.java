package cn.itcast.wanxinp2p.repayment.sms.Impl;

import cn.itcast.wanxinp2p.repayment.sms.SmsService;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 使用腾讯云实现发送还款提醒短信
 *
 * @author old money
 * @create 2022-03-07 16:56
 */
@Service
@Slf4j
public class QCloudSmsServiceImpl implements SmsService {

    @Value("${sms.qcloud.appId}")
    private int appId;

    @Value("${sms.qcloud.qppKey}")
    private String appKey;

    @Value("${sms.qcloud.templateId}")
    private int templateId;

    @Value("${sms.qcloud.sign}")
    private String sign;




    @Override
    public void sendRepaymentNotify(String mobile, String date, BigDecimal amount) {
        SmsSingleSender ssender = new SmsSingleSender(appId, appKey);
        try {
            SmsSingleSenderResult result = ssender.sendWithParam("86", mobile, templateId, new String[]{date, amount.toString()}, sign, "", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
