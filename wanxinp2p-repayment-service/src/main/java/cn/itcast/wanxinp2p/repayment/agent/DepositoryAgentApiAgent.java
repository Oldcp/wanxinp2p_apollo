package cn.itcast.wanxinp2p.repayment.agent;

import cn.itcast.wanxinp2p.api.depository.model.RepaymentRequest;
import cn.itcast.wanxinp2p.api.depository.model.UserAutoPreTransactionRequest;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author old money
 * @create 2022-03-01 14:18
 */
@FeignClient(value = "depository-agent-service")
public interface DepositoryAgentApiAgent {


    @PostMapping("/depository-agent/l/user-auto-pre-transaction")
    RestResponse<String> userAutoPreTransaction(UserAutoPreTransactionRequest userAutoPreTransactionRequest);


    @PostMapping("/depository-agent/l/confirm-repayment")
    RestResponse<String> confirmRepayment( RepaymentRequest repaymentRequest);

}
