package cn.itcast.wanxinp2p.api.repayment;

import cn.itcast.wanxinp2p.api.repayment.model.ProjectWithTendersDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;

/**
 *
 * 还款微服务接口
 *
 * @author old money
 * @create 2022-02-25 15:51
 */
public interface RepaymentApi {


    /**
     * 启动还款
     * @param projectwithTendersDTO
     * @return
     */
    public RestResponse<String> startRepayment(ProjectWithTendersDTO projectwithTendersDTO);

}
