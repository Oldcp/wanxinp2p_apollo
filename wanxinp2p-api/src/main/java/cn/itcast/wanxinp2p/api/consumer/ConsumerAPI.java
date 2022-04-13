package cn.itcast.wanxinp2p.api.consumer;

/**
 * @author old money
 * @create 2022-02-12 10:13
 */

import cn.itcast.wanxinp2p.api.consumer.model.*;
import cn.itcast.wanxinp2p.api.depository.model.GatewayRequest;
import cn.itcast.wanxinp2p.common.domain.RestResponse;

/**
 * 用户中心接口API
 */
public interface ConsumerAPI {

    /**
     * 保存用户信息
     * @param consumerRegisterDTO
     * @return
     */
    RestResponse register(ConsumerRegisterDTO consumerRegisterDTO);


    /**
     *生成开户请求数据
     * @param consumerRequest 开户信息
     * @return
     */
    RestResponse<GatewayRequest> createConsumer(ConsumerRequest consumerRequest);


    /**
     * 后端远程调用使用
     * 获取当前登录用户
     * @return
     */
    RestResponse<ConsumerDTO> getCurrConsumer(String mobile);



    /**
     * 获取当前登录用户信息
     * @return
     */
    RestResponse<ConsumerDTO> getMyConsumer();


    /**
     * 获取借款人用户信息
     * @param id
     * @return
     */
    RestResponse<BorrowerDTO> getBorrower(Long id);


    /**
     * 获取当前登录用户余额信息
     * 后端调用
     * @param userNo
     * @return
     */
    RestResponse<BalanceDetailsDTO> getBalance(String userNo);



    /**
     * 获取当前登录用户余额信息
     * 前端调用
     * @return
     */
    RestResponse<BalanceDetailsDTO> getMyBalance();



    /**
     * 获取借款人用户信息,供微服务访问
     * @param id
     * @return
     */
    RestResponse<BorrowerDTO> getBorrowerMobile(Long id);

}
