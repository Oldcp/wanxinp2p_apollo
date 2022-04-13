package cn.itcast.wanxinp2p.consumer.agent;

import cn.itcast.wanxinp2p.api.account.model.AccountDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountRegisterDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import org.dromara.hmily.annotation.Hmily;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author old money
 * @create 2022-02-13 14:51
 */
@FeignClient(value = "account-service") //远程调用的是account的微服务
public interface AccountAPIAgent {


    /**
     * 远程调用 Account 微服务的注册保存用户信息服务
     * @param accountRegisterDTO
     * @return
     */
    @PostMapping(value = "/account/l/accounts")
    @Hmily //添加分布式事务框架Hmily注解，实现登录的分布式事务
    RestResponse<AccountDTO> register(@RequestBody AccountRegisterDTO accountRegisterDTO);

}
