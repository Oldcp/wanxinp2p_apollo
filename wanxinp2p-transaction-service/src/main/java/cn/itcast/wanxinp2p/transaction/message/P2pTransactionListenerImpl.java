package cn.itcast.wanxinp2p.transaction.message;

import cn.itcast.wanxinp2p.common.domain.ProjectCode;
import cn.itcast.wanxinp2p.transaction.entity.Project;
import cn.itcast.wanxinp2p.transaction.mapper.ProjectMapper;
import cn.itcast.wanxinp2p.transaction.service.ProjectService;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author old money
 * @create 2022-02-27 15:55
 */
@Component
@RocketMQTransactionListener(txProducerGroup = "PID_START_REPAYMENT")
public class P2pTransactionListenerImpl implements RocketMQLocalTransactionListener {


    @Autowired
    private ProjectService projectService;

    @Resource
    private ProjectMapper projectMapper;


    /**
     * 执行本地事务
     * @param message
     * @param o
     * @return
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o){
        //1.解析消息
        JSONObject jsonObject = JSON.parseObject(new String((byte[])
                                            message.getPayload()));
        Project project = JSONObject.parseObject(jsonObject.getString("project"), Project.class);

        //2.执行本地事务
        Boolean aBoolean = projectService.updateProjectStatusAndStartRepayment(project);

        //3.返回执行结果
        if (aBoolean){
         return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }



    /**
     * 执行事务回查
     * @param message
     * @return
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        //1.解析消息
        JSONObject jsonObject = JSON.parseObject(new String((byte[])
                message.getPayload()));
        Project project = JSONObject.parseObject(jsonObject.getString("project"), Project.class);

        //2.查询标的状态
        Project pro = projectMapper.selectById(project.getId());

        //3.返回执行结果
        if (pro.getProjectStatus().equals(ProjectCode.REPAYING.getCode())){
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
