package cn.itcast.wanxinp2p.transaction.message;

import cn.itcast.wanxinp2p.api.repayment.model.ProjectWithTendersDTO;
import cn.itcast.wanxinp2p.transaction.entity.Project;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 实现分布式事务的消息发送方
 *
 * @author old money
 * @create 2022-02-27 15:15
 */
@Component
public class P2pTransactionProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;


    public void updateProjectStatusAndStartrepayment(Project project,
                                                     ProjectWithTendersDTO projectWithTendersDTO){

        //1.构造消息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("project",project);
        jsonObject.put("projectWithTendersDTO",projectWithTendersDTO);

        Message<String> message = MessageBuilder.withPayload(jsonObject.toJSONString()).build();

        //2.发送消息
        rocketMQTemplate.sendMessageInTransaction("PID_START_REPAYMENT",
                                        "TP_START_REPAYMENT",message,null);


    }

}
