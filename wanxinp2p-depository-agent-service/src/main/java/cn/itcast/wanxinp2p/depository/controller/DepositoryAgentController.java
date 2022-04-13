package cn.itcast.wanxinp2p.depository.controller;

import cn.itcast.wanxinp2p.api.consumer.model.ConsumerRequest;
import cn.itcast.wanxinp2p.api.depository.DepositoryAgentApi;
import cn.itcast.wanxinp2p.api.depository.model.*;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import cn.itcast.wanxinp2p.depository.service.DepositoryRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存管代理微服务
 * @author old money
 * @create 2022-02-16 15:55
 */
@Api(value = "存管代理微服务",tags = "depository-agent")
@RestController
public class DepositoryAgentController implements DepositoryAgentApi {

    @Autowired
    private DepositoryRecordService depositoryRecordService;


    @ApiOperation("生成开户请求数据")
    @ApiImplicitParam(name = "consumerRequest",value = "开户信息",required = true,
                        dataType = "ConsumerRequest",paramType = "body")
    @PostMapping(value = "/l/consumers")
    @Override
    public RestResponse<GatewayRequest> createConsumer(@RequestBody ConsumerRequest consumerRequest) {
        return RestResponse.success(depositoryRecordService.createConsumer(consumerRequest));
    }



    @ApiOperation("向银行存管系统发送标的信息")
    @ApiImplicitParam(name = "projectDTO",value = "向存管系统发送标的信息",
                    required = true,dataType = "ProjectDTO",paramType = "body")
    @PostMapping("/l/createProject")
    @Override
    public RestResponse<String> createProject(@RequestBody ProjectDTO projectDTO) {
        DepositoryResponseDTO<DepositoryBaseResponse> project = depositoryRecordService.createProject(projectDTO);

        RestResponse<String> response = new RestResponse<>();
        response.setResult(project.getRespData().getRespCode());
        response.setMsg(project.getRespData().getRespMsg());

        return response;
    }




    @ApiOperation("投标预授权处理")
    @ApiImplicitParam(name = "userAutoPreTransactionRequest",value = "平台向银行存管系统发送标的信息",
            required = true,dataType = "UserAutoPreTransactionRequest",paramType = "body")
    @PostMapping("/l/user-auto-pre-transaction")
    @Override
    public RestResponse<String> userAutoPreTransaction(@RequestBody UserAutoPreTransactionRequest userAutoPreTransactionRequest) {
        DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = depositoryRecordService.userAutoPreTransaction(userAutoPreTransactionRequest);

        return getRestResponse(responseDTO);
    }






    @Override
    @ApiOperation(value = "审核标的满标放款")
    @ApiImplicitParam(name = "loanRequest", value = "标的满标放款信息", required =
            true, dataType = "LoanRequest", paramType = "body")
    @PostMapping("l/confirm-loan")
    public RestResponse<String> confirmLoan(@RequestBody LoanRequest loanRequest) {

        DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = depositoryRecordService.confirmLoan(loanRequest);

        return getRestResponse(responseDTO);
    }





    @Override
    @ApiOperation(value = "修改标的状态")
    @ApiImplicitParam(name = "modifyProjectStatusDTO", value = "修改标的状态DTO",
            required = true, dataType = "ModifyProjectStatusDTO",
            paramType = "body")
    @PostMapping("l/modify-project-status")
    public RestResponse<String> modifyprojectStatus(@RequestBody ModifyProjectStatusDTO modifyprojectStatusDTO) {

        DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = depositoryRecordService.modifyProjectStatus(modifyprojectStatusDTO);

        return getRestResponse(responseDTO);
    }






    @ApiOperation("还款确认")
    @ApiImplicitParam(name = "repaymentRequest",value = "还款信息",required = true,
                    dataType = "RepaymentRequest",paramType = "body")
    @PostMapping("l/confirm-repayment")
    @Override
    public RestResponse<String> confirmRepayment(@RequestBody RepaymentRequest repaymentRequest) {

        DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = depositoryRecordService.confirmRepayment(repaymentRequest);

        RestResponse<String> restResponse = getRestResponse(responseDTO);
        return restResponse;
    }




    /**
     * 统一处理响应信息
     * @param depositoryResponseDTO
     * @return
     */
    private RestResponse<String> getRestResponse(DepositoryResponseDTO<DepositoryBaseResponse> depositoryResponseDTO){
        RestResponse<String> response = new RestResponse<>();

        response.setResult(depositoryResponseDTO.getRespData().getRespCode());
        response.setMsg(depositoryResponseDTO.getRespData().getRespMsg());
        return response;
    }


}
