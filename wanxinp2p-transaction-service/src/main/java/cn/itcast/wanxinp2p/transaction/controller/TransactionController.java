package cn.itcast.wanxinp2p.transaction.controller;

import cn.itcast.wanxinp2p.api.transaction.TransactionApi;
import cn.itcast.wanxinp2p.api.transaction.model.*;
import cn.itcast.wanxinp2p.common.domain.PageVO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import cn.itcast.wanxinp2p.transaction.service.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author old money
 * @create 2022-02-19 15:09
 */
@RestController
@Api(value = "交易中心服务",tags = "transaction")
public class TransactionController implements TransactionApi {

    @Autowired
    private ProjectService projectService;


    @ApiOperation("借款人发标")
    @ApiImplicitParam(name = "projectDTO",value = "标的信息",required = true,
                        dataType = "ProjectDTO",paramType = "body")
    @PostMapping("/my/projects")
    @Override
    public RestResponse<ProjectDTO> createProject(@RequestBody ProjectDTO projectDTO) {
        return RestResponse.success(projectService.createProject(projectDTO));
    }



    @Override
    @ApiOperation("分页检索标的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectQueryDTO", value = "标的信息查询对象",
                    required = true, dataType = "ProjectQueryDTO", paramType = "body"),
            @ApiImplicitParam(name = "order", value = "顺序", required = false,
                    dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "pageNo", value = "页码", required = true,
                    dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页记录数", required =
                    true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "sortBy", value = "排序字段", required = true,
                    dataType = "string", paramType =   "query")})
    @PostMapping("/projects/q")
    public RestResponse<PageVO<ProjectDTO>> queryProjects(@RequestBody ProjectQueryDTO projectQueryDTO, String order,
                                                          Integer pageNo, Integer pageSize, String sortBy) {
       return RestResponse.success(projectService.queryProjectsByQueryDTO(projectQueryDTO,order,pageNo,pageSize,sortBy));
    }




    @ApiOperation("管理员审核标的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "标的id",required = true,
                            dataType = "Long",paramType = "path"),
            @ApiImplicitParam(name = "approveStatus",value = "审批状态",
                            required = true,dataType = "String",paramType = "path")
    })
    @PutMapping("/m/prjects/{id}/projecttatus/{approveStatus}")
    @Override
    public RestResponse<String> projectsApprovalStatus(@PathVariable("id") Long id,
                                                       @PathVariable("approveStatus") String approveStatus) {
        return RestResponse.success(projectService.projectsApprovalStatus(id,approveStatus));
    }





    @ApiOperation("从ES检索标的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectQueryDTO",value = "标的信息检索条件",
            required = true,dataType = "ProjectQueryDTO",paramType = "body"),
            @ApiImplicitParam(name = "pageNo",value = "分页页码",required = true,
            dataType = "int",paramType = "query"),
            @ApiImplicitParam(name = "pageSize",value = "每页记录数",required = true,
            dataType = "int",paramType = "query"),
            @ApiImplicitParam(name = "order",value = "排序顺序",required = true,
            dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sortBy",value = "排序字段",required = true,
            dataType = "String",paramType = "query")
    })
    @PostMapping("/projects/indexes/q")
    @Override
    public RestResponse<PageVO<ProjectDTO>> queryProjects(@RequestBody ProjectQueryDTO projectQueryDTO, Integer pageNo, Integer pageSize, String sortBy, String order) {
        PageVO<ProjectDTO> projectDTOS = projectService.queryProjects(projectQueryDTO, order, pageNo, pageSize, sortBy);

        return RestResponse.success(projectDTOS);
    }




    @ApiOperation("通过ids获取多个标的")
    @ApiImplicitParam(name = "ids",value = "多个标的的id",required = true,dataType = "string",
    paramType = "query")
    @GetMapping("/projects/{ids}")
    @Override
    public RestResponse<List<ProjectDTO>> queryProjectsIds(@PathVariable("ids") String ids) {
        return RestResponse.success(projectService.queryProjectsIds(ids));
    }





    @ApiOperation("根据标的 id 查询投标记录")
    @ApiImplicitParam(name = "id",value = "标的id",required = true,
                    dataType = "Long",paramType = "query")
    @GetMapping("/tenders/projects/{id}")
    @Override
    public RestResponse<List<TenderOverviewDTO>> queryTendersbyProjectId(@PathVariable("id") Long id) {

        return RestResponse.success(projectService.queryTenderByProjectId(id));
    }




    @ApiOperation("用户投标")
    @ApiImplicitParam(name = "projectInvestDTO",value = "投标信息",
                    required = true,dataType = "ProjectInvestDTO",paramType = "body")
    @PostMapping("/my/tenders")
    @Override
    public RestResponse<TenderDTO> createTender(@RequestBody ProjectInvestDTO projectInvestDTO) {

        return RestResponse.success(projectService.createTender(projectInvestDTO));
    }








    @Override
    @ApiOperation("审核标的满标放款")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标的id", required = true,
                    dataType = "long", paramType = "path"),
            @ApiImplicitParam(name = "approveStatus", value = "标的审核状态", required =
                    true,
                    dataType = "string", paramType = "path"),
            @ApiImplicitParam(name = "commission", value = "平台佣金", required = true,
                    dataType = "string", paramType = "query")
    })
    @PutMapping("/m/loans/{id}/projectStatus/{approveStatus}")
    public RestResponse<String> loansApprovalStatus(@PathVariable("id") Long id, @PathVariable("approveStatus") String approveStatus, String commission) {

        return RestResponse.success(projectService.LoansApprovalStatus(id,approveStatus,commission));
    }


}
