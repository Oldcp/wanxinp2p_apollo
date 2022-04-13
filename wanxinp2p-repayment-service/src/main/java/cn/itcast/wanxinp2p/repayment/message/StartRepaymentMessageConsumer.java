package cn.itcast.wanxinp2p.repayment.message;

import cn.itcast.wanxinp2p.api.repayment.model.ProjectWithTendersDTO;
import cn.itcast.wanxinp2p.repayment.service.RepaymentService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 接收 交易中心发送的RocketMQ 可靠消息
 * @author old money
 * @create 2022-02-27 16:19
 */
@Component
@RocketMQMessageListener(topic = "TP_START_REPAYMENT",consumerGroup = "CID_START_REPAYMENT")
public class StartRepaymentMessageConsumer implements RocketMQListener<String> {


    @Autowired
    private RepaymentService repaymentService;


    /**
     * 执行本地事务
     * @param
     */
    @Override
    public void onMessage(String message) {
        //1.解析消息
        JSONObject jsonObject = JSON.parseObject(message);
        ProjectWithTendersDTO projectWithTendersDTO = JSONObject.parseObject(jsonObject.getString("projectWithTendersDTO"), ProjectWithTendersDTO.class);

        //2.执行本地事务
        repaymentService.startRepayment(projectWithTendersDTO);
    }
}
