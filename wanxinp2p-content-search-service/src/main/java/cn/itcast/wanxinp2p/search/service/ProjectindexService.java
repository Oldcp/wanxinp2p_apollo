package cn.itcast.wanxinp2p.search.service;

import cn.itcast.wanxinp2p.api.search.model.ProjectQueryParamsDTO;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.common.domain.PageVO;

/**
 * @author old money
 * @create 2022-02-23 15:11
 */
public interface ProjectindexService {


    /**
     * 从ES索引库检索标的信息
     * @param projectQueryParamsDTO
     * @param pageNo
     * @param pageSize
     * @param sortBy
     * @param order
     * @return
     */
    PageVO<ProjectDTO> queryProjectIndex(ProjectQueryParamsDTO projectQueryParamsDTO,
                                         Integer pageNo,Integer pageSize,
                                         String sortBy,String order);


}
