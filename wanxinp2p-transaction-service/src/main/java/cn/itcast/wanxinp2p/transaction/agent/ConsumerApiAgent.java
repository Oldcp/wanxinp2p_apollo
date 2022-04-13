package cn.itcast.wanxinp2p.transaction.agent;

import cn.itcast.wanxinp2p.api.consumer.model.BalanceDetailsDTO;
import cn.itcast.wanxinp2p.api.consumer.model.ConsumerDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 交易中心远程调用用户中心
 *
 * @author old money
 * @create 2022-02-19 15:31
 */
@FeignClient(value = "consumer-service")
public interface ConsumerApiAgent {


    /**
     * 通过手机号 查询当前登录用户信息
     * @return
     */
    @GetMapping("/consumer/l/currConsumer/{mobile}")
    public RestResponse<ConsumerDTO> getCurrConsumer(@PathVariable("mobile") String mobile);


    /**
     * 获取当前登录用户账户余额
     * @return
     */
    @GetMapping("/consumer/l/balances/{userNo}")
    public RestResponse<BalanceDetailsDTO> getBalance(@PathVariable("userNo")String userNo);


}
