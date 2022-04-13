package cn.itcast.wanxinp2p.transaction.agent;

import cn.itcast.wanxinp2p.api.search.model.ProjectQueryParamsDTO;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectQueryDTO;
import cn.itcast.wanxinp2p.common.domain.PageVO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 远程调用内容检索微服务接口
 *
 * @author old money
 * @create 2022-02-23 14:55
 */
@FeignClient(value = "content-serch-service")
public interface ContentearchApiAgent {


    @PostMapping(value = "/content-search/l/projects/indexes/q")
    public RestResponse<PageVO<ProjectDTO>> queryProjectIndex(
            @RequestBody ProjectQueryDTO projectQueryParamsDTO,
            @RequestParam String pageNo,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) Integer sortBy,
            @RequestParam(required = false) String order);
}
