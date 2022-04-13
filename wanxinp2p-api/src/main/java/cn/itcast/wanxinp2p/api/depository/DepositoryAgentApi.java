package cn.itcast.wanxinp2p.api.depository;

import cn.itcast.wanxinp2p.api.consumer.model.ConsumerRequest;
import cn.itcast.wanxinp2p.api.depository.model.*;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;

/**
 * 存管代理服务API
 *
 * @author old money
 * @create 2022-02-16 15:52
 */
public interface DepositoryAgentApi {


    /**
     * 开通存管账户
     * @param consumerRequest 开户信息
     * @return
     */
    RestResponse<GatewayRequest> createConsumer(ConsumerRequest consumerRequest);


    /**
     * 向银行存管系统发送标的信息
     * @param projectDTO
     * @return
     */
    RestResponse<String> createProject(ProjectDTO projectDTO);



    /**
     * 用户投标预授权处理
     * @param userAutoPreTransactionRequest
     * @return
     */
    RestResponse<String> userAutoPreTransaction(UserAutoPreTransactionRequest userAutoPreTransactionRequest);


    /**
     * 审核标的的满标放款
     * @param loanRequest
     * @return
     */
    RestResponse<String> confirmLoan(LoanRequest loanRequest);



    /**
     * 修改标的状态为还款中
     * @param modifyprojectStatusDTO
     * @return
     */
    RestResponse<String> modifyprojectStatus(ModifyProjectStatusDTO modifyprojectStatusDTO);





    /**
     *还款确认
     * @param repaymentRequest 还款信息
     * @return
     */
    RestResponse<String> confirmRepayment(RepaymentRequest repaymentRequest);


}
