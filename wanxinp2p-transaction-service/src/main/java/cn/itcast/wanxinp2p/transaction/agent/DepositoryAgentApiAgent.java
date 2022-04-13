package cn.itcast.wanxinp2p.transaction.agent;

import cn.itcast.wanxinp2p.api.depository.model.LoanRequest;
import cn.itcast.wanxinp2p.api.depository.model.ModifyProjectStatusDTO;
import cn.itcast.wanxinp2p.api.depository.model.UserAutoPreTransactionRequest;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 远程调用存管代理微服务的Feign接口
 * @author old money
 * @create 2022-02-21 15:30
 */
@FeignClient(value = "depository-agent-service")
public interface DepositoryAgentApiAgent {


    @PostMapping("/depository-agent/l/createProject")
    public RestResponse<String> createProject(@RequestBody ProjectDTO projectDTO);



    @PostMapping("/depository-agent/l/user-auto-pre-transaction")
    RestResponse<String> userAutoPreTransaction(UserAutoPreTransactionRequest userAutoPreTransactionRequest);




    @PostMapping("/depository-agent/l/confirm-loan")
    RestResponse<String> confirmLoan(@RequestBody LoanRequest loanRequest);


    @PostMapping("/depository-agent/l/modify-project-status")
    RestResponse<String> modifyProjectStatus(@RequestBody ModifyProjectStatusDTO modifyProjectStatusDTO);
}
