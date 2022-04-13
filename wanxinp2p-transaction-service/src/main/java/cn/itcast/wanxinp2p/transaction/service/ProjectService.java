package cn.itcast.wanxinp2p.transaction.service;

import cn.itcast.wanxinp2p.api.transaction.model.*;
import cn.itcast.wanxinp2p.common.domain.PageVO;
import cn.itcast.wanxinp2p.transaction.entity.Project;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 交易中心 Service 接口
 * @author old money
 * @create 2022-02-19 15:15
 */
public interface ProjectService extends IService<Project> {

    /**
     * 创建标的
     * @param projectDTO
     * @return
     */
    ProjectDTO createProject(ProjectDTO projectDTO);


    /**
     * 根据分页条件检索标的信息
     * @param projectQueryDTO
     * @param order
     * @param pageNo
     * @param pageSize
     * @param sortBy
     * @return
     */
    PageVO<ProjectDTO> queryProjectsByQueryDTO(ProjectQueryDTO projectQueryDTO,
                                               String order, Integer pageNo, Integer pageSize,String sortBy);


    /**
     * 管理员审核标的信息
     * @param id
     * @param approveStatus
     * @return
     */
    String projectsApprovalStatus(Long id,String approveStatus);


    /**
     * 检索标的信息
     * @param projectQueryDTO
     * @param order
     * @param pageNo
     * @param pageSize
     * @param sortBy
     * @return
     */
    PageVO<ProjectDTO> queryProjects(ProjectQueryDTO projectQueryDTO,String order,
                                     Integer pageNo,Integer pageSize,String sortBy);


    /**
     * 通过ids 获取多个标的
     * @param ids
     * @return
     */
    List<ProjectDTO>  queryProjectsIds(String ids);



    /**
     * 根据标的 id 查询投标记录
     * @param id
     * @return
     */
    List<TenderOverviewDTO> queryTenderByProjectId(Long id);


    /**
     * 用户投标
     * @param projectInvestDTO
     * @return
     */
    TenderDTO createTender(ProjectInvestDTO projectInvestDTO);


    /**
     * 审核标的满标放款
     * @param id
     * @param approveStatus
     * @param commission
     * @return
     */
    String LoansApprovalStatus(Long id,String approveStatus,String commission);



    /**
     * 修改标的状态为还款中
     * @param project
     * @return
     */
    Boolean updateProjectStatusAndStartRepayment(Project project);

}
