package cn.itcast.wanxinp2p.api.search;

import cn.itcast.wanxinp2p.api.search.model.ProjectQueryParamsDTO;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.common.domain.PageVO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;

/**
 *
 * 内部检索服务接口
 * @author old money
 * @create 2022-02-23 14:47
 */
public interface ContentSearchApi {


    /**
     * 从ES 检索标的
     * @param projectQueryParamsDTO
     * @param pageNo
     * @param pageSize
     * @param sortBy
     * @param order
     * @return
     */
    RestResponse<PageVO<ProjectDTO>> queryProjectIndex(ProjectQueryParamsDTO projectQueryParamsDTO,
                                                       Integer pageNo, Integer pageSize, String sortBy, String order);

}
