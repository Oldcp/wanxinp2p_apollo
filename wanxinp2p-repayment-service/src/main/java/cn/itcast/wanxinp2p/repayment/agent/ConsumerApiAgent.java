package cn.itcast.wanxinp2p.repayment.agent;

import cn.itcast.wanxinp2p.api.consumer.model.BorrowerDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author old money
 * @create 2022-03-07 16:28
 */
@FeignClient(value = "consumer-service")
public interface ConsumerApiAgent {

    @GetMapping("/consumer/l/borrowers/{id}")
    RestResponse<BorrowerDTO> getBorrowerMobile(@PathVariable("id") Long id);
}
