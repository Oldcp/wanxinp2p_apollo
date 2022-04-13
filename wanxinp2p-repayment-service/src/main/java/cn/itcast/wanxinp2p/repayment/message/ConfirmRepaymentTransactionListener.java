package cn.itcast.wanxinp2p.repayment.message;

import cn.itcast.wanxinp2p.api.depository.model.RepaymentRequest;
import cn.itcast.wanxinp2p.repayment.entity.RepaymentPlan;
import cn.itcast.wanxinp2p.repayment.mapper.PlanMapper;
import cn.itcast.wanxinp2p.repayment.service.RepaymentService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 监听本地事务，执行本地事务，回查本地事务
 *
 * @author old money
 * @create 2022-03-01 15:58
 */
@Component
@RocketMQTransactionListener(txProducerGroup = "PID_CONFIRM_REPAYMENT")
public class ConfirmRepaymentTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private RepaymentService repaymentService;


    @Autowired
    private PlanMapper planMapper;

    /**
     * 执行本地事务
     * @param message
     * @param o
     * @return
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
        //1.解析消息
        JSONObject jsonObject = JSON.parseObject(new String((byte[]) message.getPayload()));
        RepaymentPlan repaymentPlan = JSONObject.parseObject(jsonObject.getString("repaymentPlan"),RepaymentPlan.class);
        RepaymentRequest repaymentRequest = JSONObject.parseObject(jsonObject.getString("repaymentRequest"),RepaymentRequest.class);

        //2.执行本地事务
        Boolean aBoolean = repaymentService.confirmRepayment(repaymentPlan, repaymentRequest);

        //3.返回结果
        if (aBoolean){
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }


    /**
     * 事务状态回查
     * @param message
     * @return
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        //1.解析消息
        JSONObject jsonObject = JSON.parseObject(new String((byte[]) message.getPayload()));
        RepaymentPlan repaymentPlan = JSONObject.parseObject(jsonObject.getString("repaymentPlan"),RepaymentPlan.class);
        //2.执行查询
        RepaymentPlan selectById = planMapper.selectById(repaymentPlan.getId());
        //3.返回状态
        if (selectById != null && selectById.getRepaymentStatus().equals("1")){
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
