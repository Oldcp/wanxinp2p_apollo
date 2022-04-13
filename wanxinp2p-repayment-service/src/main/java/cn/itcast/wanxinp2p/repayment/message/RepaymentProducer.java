package cn.itcast.wanxinp2p.repayment.message;

import cn.itcast.wanxinp2p.api.depository.model.RepaymentRequest;
import cn.itcast.wanxinp2p.repayment.entity.RepaymentPlan;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 *
 * 还款服务发送RocketMQ 可靠消息
 *
 * @author old money
 * @create 2022-03-01 14:49
 */
@Component
public class RepaymentProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void confirmRepayment(RepaymentPlan repaymentPlan, RepaymentRequest repaymentRequest){
        //1.构造消息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("repaymentPlan",repaymentPlan);
        jsonObject.put("repaymentRequest",repaymentRequest);

        String jsonString = jsonObject.toJSONString();

        Message<String> msg = MessageBuilder.withPayload(jsonString).build();

        //2.发送消息
        rocketMQTemplate.sendMessageInTransaction("PID_CONFIRM_REPAYMENT",
                "TP_CONFIRM_REPAYMENT",msg,null);

    }





}
