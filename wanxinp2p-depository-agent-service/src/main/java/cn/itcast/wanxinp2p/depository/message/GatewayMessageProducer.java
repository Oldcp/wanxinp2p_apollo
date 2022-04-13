package cn.itcast.wanxinp2p.depository.message;

import cn.itcast.wanxinp2p.api.depository.model.DepositoryConsumerResponse;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 *
 * 存管代理服务的异步通知消息生产者
 * @author old money
 * @create 2022-02-17 16:10
 */
@Component
public class GatewayMessageProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void personalRegister(DepositoryConsumerResponse response){
        rocketMQTemplate.convertAndSend("TP_GATEWAY_NOTIFY_AGENT:PERSONAL_REGISTER",
                response);
    }




}
