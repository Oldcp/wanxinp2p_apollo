package cn.itcast.wanxinp2p.consumer.controller;

import cn.itcast.wanxinp2p.api.consumer.ConsumerAPI;
import cn.itcast.wanxinp2p.api.consumer.model.*;
import cn.itcast.wanxinp2p.api.depository.model.GatewayRequest;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import cn.itcast.wanxinp2p.consumer.common.SecurityUtil;
import cn.itcast.wanxinp2p.consumer.service.ConsumerService;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Api(value = "用户服务的Controller", tags = "Consumer", description = "用户服务API")
@Slf4j
    public class ConsumerController implements ConsumerAPI {

    @Value("${depository.url}")
    private String depositoryURL;


    private OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();

    @Autowired
    private ConsumerService consumerService;


        @ApiOperation("测试hello")
        @GetMapping(path = "/hello")
        public String hello(){
            return "hello";
        }

        @ApiOperation("测试hi")
        @PostMapping(path="/hi")
        @ApiImplicitParam(name="name",value = "姓名",required = true,dataType = "String")
        public String hi(String name){
            return "hi,"+name;
        }



    @Override
    @ApiOperation("用户注册，保存信息")
    @ApiImplicitParam(name = "consumerRegisterDTO",value = "注册信息",required = true,
                        dataType = "ConsumerRegisterDTO",paramType = "body")
    @PostMapping("/consumers")
    public RestResponse register(@RequestBody ConsumerRegisterDTO consumerRegisterDTO) {
         consumerService.register(consumerRegisterDTO);

         return RestResponse.success();
    }



    @ApiOperation("生成开户请求数据")
    @ApiImplicitParam(name = "consumerRequest",value = "开户信息",required = true,
                    dataType = "ConsumerRequest",paramType = "body")
    @PostMapping("/my/consumers")
    @Override
    public RestResponse<GatewayRequest> createConsumer(@RequestBody ConsumerRequest consumerRequest) {

            //调用该工具类从上下文参数中获取用户的手机号
        String mobile = SecurityUtil.getUser().getMobile();
        consumerRequest.setMobile(mobile);

        return consumerService.createConsumer(consumerRequest);
    }


    @ApiOperation("获取当前登录用户信息")
    @GetMapping("l/currConsumer/{mobile}")
    @Override
    public RestResponse<ConsumerDTO> getCurrConsumer(@PathVariable("mobile") String mobile) {

            // 通过手机号查询当前登录用户
        ConsumerDTO consumerDTO = consumerService.getByMobile(mobile);

        return RestResponse.success(consumerDTO);
    }



    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/my/consumers")
    @Override
    public RestResponse<ConsumerDTO> getMyConsumer() {
            //从令牌的上下文参数中获取当前用户手机号，查询当前登录用户
        ConsumerDTO consumerDTO = consumerService.getByMobile(SecurityUtil.getUser().getMobile());

        return RestResponse.success(consumerDTO);
    }





    @ApiOperation("获取借款人用户信息")
    @ApiImplicitParam(name = "id",value = "借款人id",required = true,
                    dataType = "Long",paramType = "query")
    @GetMapping("/my/borrowers/{id}")
    @Override
    public RestResponse<BorrowerDTO> getBorrower(@PathVariable("id") Long id) {

        return RestResponse.success(consumerService.getBorrower(id));
    }




    @ApiOperation("获取当前登录用户余额信息")
    @GetMapping("/my/balances")
    @Override
    public RestResponse<BalanceDetailsDTO> getMyBalance() {

        ConsumerDTO consumerDTO = consumerService.getByMobile(SecurityUtil.getUser().getMobile());

        return getBalanceFromDepository(consumerDTO.getUserNo());
    }



    @ApiOperation("获取借款人用户信息-供微服务访问")
    @ApiImplicitParam(name = "id",value = "用户id",required = true,
                    dataType = "Long",paramType = "path")
    @GetMapping("/l/borrowers/{id}")
    @Override
    public RestResponse<BorrowerDTO> getBorrowerMobile(@PathVariable("id") Long id) {
        return RestResponse.success(consumerService.getBorrower(id));
    }




    @ApiOperation("获取当前登录用户余额信息")
    @ApiImplicitParam(name = "userNo",value = "用户id",required = true,
                        dataType = "String",paramType = "query")
    @GetMapping("/l/balances/{userNo}")
    @Override
    public RestResponse<BalanceDetailsDTO> getBalance(@PathVariable("userNO") String userNo) {
        return getBalanceFromDepository(userNo);
    }





    /**
     远程调用银行存管系统获取用户余额信息
     @param userNo 用户编码
     @return
     */
    //不用大家编码实现，直接复制使用即可
    private RestResponse<BalanceDetailsDTO> getBalanceFromDepository(String userNo)
    {
        String url = depositoryURL + "/balance-details/" + userNo;
        BalanceDetailsDTO balanceDetailsDTO;
        Request request = new Request.Builder().url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                balanceDetailsDTO = JSON.parseObject(responseBody,
                        BalanceDetailsDTO.class);
                return RestResponse.success(balanceDetailsDTO);
            }
        } catch (IOException e) {
            log.warn("调用存管系统{}获取余额失败 ", url, e);
        }
        return RestResponse.validfail("获取失败");
    }


}
