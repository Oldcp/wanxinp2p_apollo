package cn.itcast.wanxinp2p.uaa.agent;

import cn.itcast.wanxinp2p.api.account.model.AccountDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountLoginDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 *
 * UAA 远程调用 Account 微服务的接口
 * @author old money
 * @create 2022-02-14 15:46
 */
@FeignClient(value = "account-service")
public interface AccountApiAgent {


    @PostMapping(value = "/account/l/accounts/session")
    public RestResponse<AccountDTO> login(@RequestBody AccountLoginDTO accountLoginDTO);



}
