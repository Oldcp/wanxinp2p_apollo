package cn.itcast.wanxinp2p.repayment.sms;

import java.math.BigDecimal;

/**
 * 发送还款提醒短信接口
 * @author old money
 * @create 2022-03-07 16:13
 */
public interface SmsService {

    /**
     * 发送还款短信通知
     * @param mobile 还款人手机号
     * @param date 日期
     * @param amount 应还金额
     */
    void sendRepaymentNotify(String mobile, String date, BigDecimal amount);

}
