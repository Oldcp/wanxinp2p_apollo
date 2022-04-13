package cn.itcast.wanxinp2p.consumer.message;

import cn.itcast.wanxinp2p.api.depository.model.DepositoryConsumerResponse;
import cn.itcast.wanxinp2p.consumer.service.ConsumerService;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 使用 RocketMQ 接收存管代理服务异步发送的消息
 *消费者
 * @author old money
 * @create 2022-02-17 16:58
 */
@Component
public class GatewayNotifyConsumer {


    @Resource
    private ConsumerService consumerService;


    public GatewayNotifyConsumer( @Value("${rocketmq.consumer.group}") String consumerGroup,@Value("${rocketmq.name-server}") String nameServer) throws MQClientException{
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.subscribe("TP_GATEWAY_NOTIFY_AGENT","*");


        //注册监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                try {
                    Message message = msgs.get(0);
                    String topic = message.getTopic();
                    String tags = message.getTags();
                    String body = new String(message.getBody(), StandardCharsets.UTF_8); //发送过来的数据

                    if (tags.equals("PERSONAL_REGISTER")){
                        DepositoryConsumerResponse response = JSON.parseObject(body, DepositoryConsumerResponse.class);

                        consumerService.modifyResult(response);
                    }

                } catch (Exception e) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
    }
}
