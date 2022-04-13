package cn.itcast.wanxinp2p.repayment.message;

import cn.itcast.wanxinp2p.api.depository.model.RepaymentRequest;
import cn.itcast.wanxinp2p.repayment.entity.RepaymentPlan;
import cn.itcast.wanxinp2p.repayment.service.RepaymentService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * 接收 还款 微服务 发送的 RocketMQ 可靠消息
 * @author old money
 * @create 2022-03-01 17:54
 */
@Component
@RocketMQMessageListener(topic = "TP_CONFIRM_REPAYMENT",consumerGroup = "CID_CONFIRM_REPAYMENT")
public class ConfirmRepaymentConsumer implements RocketMQListener<String> {

    @Autowired
    private RepaymentService repaymentService;


    @Override
    public void onMessage(String msg) {
        //1.解析消息
        JSONObject jsonObject = JSON.parseObject(msg);

        RepaymentPlan repaymentPlan = JSONObject.parseObject(jsonObject.getString("repaymentPlan"),RepaymentPlan.class);
        RepaymentRequest repaymentRequest = JSONObject.parseObject(jsonObject.getString("repaymentRequest"),RepaymentRequest.class);

        //2.执行本地业务
        repaymentService.invokeConfirmRepayment(repaymentPlan,repaymentRequest);
    }

}
