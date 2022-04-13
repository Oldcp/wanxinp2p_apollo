package cn.itcast.wanxinp2p.transaction.service.Impl;

import cn.itcast.wanxinp2p.api.consumer.model.BalanceDetailsDTO;
import cn.itcast.wanxinp2p.api.consumer.model.ConsumerDTO;
import cn.itcast.wanxinp2p.api.depository.model.LoanDetailRequest;
import cn.itcast.wanxinp2p.api.depository.model.LoanRequest;
import cn.itcast.wanxinp2p.api.depository.model.ModifyProjectStatusDTO;
import cn.itcast.wanxinp2p.api.depository.model.UserAutoPreTransactionRequest;
import cn.itcast.wanxinp2p.api.repayment.model.ProjectWithTendersDTO;
import cn.itcast.wanxinp2p.api.transaction.model.*;
import cn.itcast.wanxinp2p.common.domain.*;
import cn.itcast.wanxinp2p.common.util.CodeNoUtil;
import cn.itcast.wanxinp2p.common.util.CommonUtil;
import cn.itcast.wanxinp2p.transaction.agent.ConsumerApiAgent;
import cn.itcast.wanxinp2p.transaction.agent.ContentearchApiAgent;
import cn.itcast.wanxinp2p.transaction.agent.DepositoryAgentApiAgent;
import cn.itcast.wanxinp2p.transaction.common.constant.TradingCode;
import cn.itcast.wanxinp2p.transaction.common.constant.TransactionErrorCode;
import cn.itcast.wanxinp2p.transaction.common.utils.IncomeCalcUtil;
import cn.itcast.wanxinp2p.transaction.common.utils.SecurityUtil;
import cn.itcast.wanxinp2p.transaction.entity.Project;
import cn.itcast.wanxinp2p.transaction.entity.Tender;
import cn.itcast.wanxinp2p.transaction.mapper.ProjectMapper;
import cn.itcast.wanxinp2p.transaction.mapper.TenderMapper;
import cn.itcast.wanxinp2p.transaction.message.P2pTransactionProducer;
import cn.itcast.wanxinp2p.transaction.model.LoginUser;
import cn.itcast.wanxinp2p.transaction.service.ConfigService;
import cn.itcast.wanxinp2p.transaction.service.ProjectService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author old money
 * @create 2022-02-19 15:16
 */
@Service
@Slf4j
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Autowired
    private ConsumerApiAgent consumerApiAgent;

    @Autowired
    private ConfigService configService;


    @Autowired
    private DepositoryAgentApiAgent depositoryAgentApiAgent;


    @Autowired
    private ContentearchApiAgent contentearchApiAgent;


    @Autowired
    private P2pTransactionProducer p2pTransactionProducer;


    @Resource
    private TenderMapper tenderMapper;

    /**
     * 借款人发标 保存标的信息 功能
     * @param projectDTO
     * @return
     */
    @Override
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        //从令牌上下文参数中获取用户手机号查询获取当前登录用户
        RestResponse<ConsumerDTO> restResponse = consumerApiAgent.getCurrConsumer(SecurityUtil.getUser().getMobile());

        //从当前登录用户中将信息保存
        projectDTO.setConsumerId(restResponse.getResult().getId());
        projectDTO.setUserNo(restResponse.getResult().getUserNo());

        //生成标的编码
        projectDTO.setProjectNo(CodeNoUtil.getNo(CodePrefixCode.CODE_PROJECT_PREFIX));
        //标的状态修改
        projectDTO.setProjectStatus(ProjectCode.COLLECTING.getCode());
        //标的可用状态修改，未同步
        projectDTO.setStatus(StatusCode.STATUS_OUT.getCode());
        //设置标的创建时间
        projectDTO.setCreateDate(LocalDateTime.now());
        //设置还款方式
        projectDTO.setRepaymentWay(RepaymentWayCode.FIXED_REPAYMENT.getCode());
        //设置标的类型
        projectDTO.setRepaymentWay("NEW");

        Project project = converProjectDTOToEntity(projectDTO);
        //使用ConfigService工具类 将Apollo配置中心中配置的属性值，取出 填写到 project对象中
        project.setBorrowerAnnualRate(configService.getBorrowerAnnualRate());
        project.setAnnualRate(configService.getAnnualRate());
        project.setCommissionAnnualRate(configService.getCommissionAnnualRate());
        //债券转让
        project.setIsAssignment(0);

        //设置标的名称，姓名+性别+第n次借款
            //通过身份证号码判断男女
             String sex = Integer.parseInt(restResponse.getResult().getIdNumber()
                     .substring(16,17)) % 2 == 0 ? "女士":"先生";
            //构造借款测试查询条件
            QueryWrapper<Project> wrapper = new QueryWrapper<>();
            wrapper.eq("consumerId",restResponse.getResult().getId());
            project.setName(restResponse.getResult().getFullname() + sex +
                    "第" + (count(wrapper) + 1 ) + "次借款");

        save(project);

        projectDTO.setId(project.getId());
        projectDTO.setName(project.getName());

        return projectDTO;
    }



    /**
     * 根据分页条件 检索标的信息
     * @param projectQueryDTO
     * @param order
     * @param pageNo
     * @param pageSize
     * @param sortBy
     * @return
     */
    @Override
    public PageVO<ProjectDTO> queryProjectsByQueryDTO(ProjectQueryDTO projectQueryDTO, String order, Integer pageNo, Integer pageSize, String sortBy) {

        //带条件
            QueryWrapper<Project> wrapper = new QueryWrapper<>();
            //标的类型,StringUtils.isNotBlank apache封装的工具类方法，判断字符串是否为空
            if (StringUtils.isNotBlank(projectQueryDTO.getType())){
                wrapper.eq("type",projectQueryDTO.getType());
            }
            // 起止年化利率(投资人)根据区间
            if (null != projectQueryDTO.getEndAnnualRate()){
                wrapper.ge("annualRate",projectQueryDTO.getStartAnnualRate());
            }
            if (null != projectQueryDTO.getEndAnnualRate()){
                wrapper.le("annualRate",projectQueryDTO.getStartAnnualRate());
            }
            //借款期限 根据区间
            if (null != projectQueryDTO.getStartPeriod()){
                wrapper.ge("period",projectQueryDTO.getStartPeriod());
            }
            if (null != projectQueryDTO.getEndPeriod()){
                wrapper.le("period",projectQueryDTO.getEndPeriod());
            }
            //标的状态
            if (StringUtils.isNotBlank(projectQueryDTO.getProjectStatus())){
                wrapper.eq("projectStatus",projectQueryDTO.getProjectStatus());
            }
        //分页
        Page<Project> page = new Page<>(pageNo, pageSize);

        //排序
        if (StringUtils.isNotBlank(order) && StringUtils.isNotBlank(sortBy)){

            if (order.toLowerCase().equals("asc")){
                wrapper.orderByAsc(sortBy);
            }
            if (order.toLowerCase().equals("desc")){
                wrapper.orderByDesc(sortBy);
            }
        }else {
            wrapper.orderByDesc("createDate");
        }

        //执行查询
        IPage<Project> iPage = page(page, wrapper);


        //封装结果返回
        List<ProjectDTO> projectDTOList = EntityListToDTOList(iPage.getRecords());

        PageVO<ProjectDTO> dtos = new PageVO<>(projectDTOList, iPage.getTotal(), pageNo, pageSize);

        return dtos;
    }



    /**
     * 管理员审核标的信息
     * @param id
     * @param approveStatus
     * @return
     */
    @Override
    public String projectsApprovalStatus(Long id, String approveStatus) {
        //1.根据id查询标的信息并转换为DTO对象
        Project project = getById(id);
        ProjectDTO projectDTO = converProjectToEntity(project);

        if (StringUtils.isNotBlank(project.getRequestNo())){
            //2.因为涉及到交易，所以需要生成流水号
            projectDTO.setRequestNo(CodeNoUtil.getNo(CodePrefixCode.CODE_REQUEST_PREFIX));

            UpdateWrapper<Project> wrapper = new UpdateWrapper<>();
            wrapper.eq("id",id);
            wrapper.set("requestNo",project.getRequestNo());
            update(wrapper);
        }
        //3.通过feign远程调用存管代理服务，把标的信息传输过去
        RestResponse<String> restResponse = depositoryAgentApiAgent.createProject(projectDTO);

        if (DepositoryReturnCode.RETURN_CODE_00000.equals(restResponse.getResult())){
            //4.根据结果修改状态
            UpdateWrapper<Project> wrapper = new UpdateWrapper<>();
            wrapper.eq("id",id);
            wrapper.set("projectStatus",approveStatus);
            update(wrapper);

            return "success";
        }

        //5.如果失败就抛异常
        throw new BusinessException(TransactionErrorCode.E_150107);
    }




    /**
     * 从索引库中检索标的信息
     * @param projectQueryDTO
     * @param order
     * @param pageNo
     * @param pageSize
     * @param sortBy
     * @return
     */
    @Override
    public PageVO<ProjectDTO> queryProjects(ProjectQueryDTO projectQueryDTO, String order, Integer pageNo, Integer pageSize, String sortBy) {

        RestResponse<PageVO<ProjectDTO>> esResponse = contentearchApiAgent.queryProjectIndex(projectQueryDTO, order, pageNo, pageSize, sortBy);

        if (esResponse.isSuccessful()){
            throw new BusinessException(CommonErrorCode.UNKOWN);
        }
        return esResponse.getResult();
    }


    /**
     * 通过ids 查询多个标的信息
     * @param ids
     * @return
     */
    @Override
    public List<ProjectDTO> queryProjectsIds(String ids) {

        ArrayList<Long> list = new ArrayList<>();
        Arrays.asList(ids.split(",")).forEach(str->{
            list.add(Long.parseLong(str));
        });

        //1.查询标的信息
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.in("id",list);
        List<Project> projectList = list(wrapper);

        //2.转换为DTO对象
        List<ProjectDTO> list1 = new ArrayList<>();
        for (Project project : projectList) {
            ProjectDTO projectDTO = converProjectToEntity(project);

            //3.获取剩余额度
            projectDTO.setRemainingAmount(getProjectremainingAmount(project));

            //4.查询出借人数
            QueryWrapper<Tender> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("projectId",project.getId());
            projectDTO.setTenderCount(tenderMapper.selectCount(queryWrapper));

            list1.add(projectDTO);
        }

        return list1;
    }


    /**
     * 根据标的id 查询投标记录
     * @param id
     * @return
     */
    @Override
    public List<TenderOverviewDTO> queryTenderByProjectId(Long id) {

        QueryWrapper<Tender> wrapper = new QueryWrapper<>();
        wrapper.eq("projectId",id);
        List<Tender> list = tenderMapper.selectList(wrapper);

        ArrayList<TenderOverviewDTO> tenderOverviewDTOS = new ArrayList<>();
        for (Tender tender : list) {
            TenderOverviewDTO tenderOverviewDTO = new TenderOverviewDTO();
            BeanUtils.copyProperties(tender,tenderOverviewDTO);
            //隐藏用户手机号
            tenderOverviewDTO.setConsumerUsername(CommonUtil.hiddenMobile(tender.getConsumerUsername()));

        tenderOverviewDTOS.add(tenderOverviewDTO);
        }

        return tenderOverviewDTOS;
    }


    /**
     * 用户投标
     * @param projectInvestDTO
     * @return
     */
    @Override
    public TenderDTO createTender(ProjectInvestDTO projectInvestDTO) {
        //1.前置条件判断

            //1.1判断投标金额是否大于最小投标金额
            BigDecimal amount = new BigDecimal(projectInvestDTO.getAmount());
            BigDecimal miniInvestmentAmount = configService.getMiniInvestmentAmount(); //从配置中心中读取配置的平台最小投标金额
            if (amount.compareTo(miniInvestmentAmount) < 0){
                throw new BusinessException(TransactionErrorCode.E_150109);
            }

            //1.2判断用户账户余额是否足够
            LoginUser loginUser = SecurityUtil.getUser();
            RestResponse<ConsumerDTO> restResponse = consumerApiAgent.getCurrConsumer(loginUser.getMobile());
            RestResponse<BalanceDetailsDTO> balanceDetailsDTORestResponse = consumerApiAgent.getBalance(restResponse.getResult().getUserNo());

            BigDecimal balance = balanceDetailsDTORestResponse.getResult().getBalance();

            if (amount.compareTo(balance) < 0){
                throw new BusinessException(TransactionErrorCode.E_150112);
            }

            //1.3 判断标的是否满标，标的状态为FULLY就表示满标
            Project project = getById(projectInvestDTO.getId());
            if (project.getProjectStatus().equalsIgnoreCase(ProjectCode.FULLY.getCode())){
                throw new BusinessException(TransactionErrorCode.E_150114);
            }

            //1.4 判断投标金额是否超过剩余未投金额
            BigDecimal remainingAmount = getProjectremainingAmount(project);
            if (amount.compareTo(remainingAmount) < 1){

                //1.5 判断此次投标后的剩余未投金额是否满足最小投标金额
                    //剩余未投金额 = 目前剩余未投金额减去本次投标金额
                    BigDecimal subtract = remainingAmount.subtract(amount);

                    int result = subtract.compareTo(miniInvestmentAmount);

                    if (result < 0){
                        if (subtract.compareTo(new BigDecimal(0.0)) != 0){

                            throw new BusinessException(TransactionErrorCode.E_150111);
                        }
                    }

            }else{
                throw new BusinessException(TransactionErrorCode.E_150110);
            }


        //2. 保存投标信息并发送给存管代理服务
            //2.1 保存投标信息, 数据状态为: 未发布
            // 封装投标信息
            final Tender tender = new Tender();
            // 投资人投标金额( 投标冻结金额 )
            tender.setAmount(amount);
            // 投标人用户标识
            tender.setConsumerId(restResponse.getResult().getId());
            tender.setConsumerUsername(restResponse.getResult().getUsername());
            // 投标人用户编码
            tender.setUserNo(restResponse.getResult().getUserNo());
            // 标的标识
            tender.setProjectId(projectInvestDTO.getId());
            // 标的编码
            tender.setProjectNo(project.getProjectNo());
            // 投标状态
            tender.setTenderStatus(TradingCode.FROZEN.getCode());
            // 创建时间
            tender.setCreateDate(LocalDateTime.now());
            // 请求流水号
            tender.setRequestNo(CodeNoUtil.getNo(CodePrefixCode.CODE_REQUEST_PREFIX));
            // 可用状态
            tender.setStatus(0);
            tender.setProjectName(project.getName());
            // 标的期限(单位:天)
            tender.setProjectPeriod(project.getPeriod());
            // 年化利率(投资人视图)
            tender.setProjectAnnualRate(project.getAnnualRate());
            // 保存到数据库
            tenderMapper.insert(tender);

            //2.2 发送数据给存管代理服务
                // 构造请求数据
                 UserAutoPreTransactionRequest userAutoPreTransactionRequest = new UserAutoPreTransactionRequest();
                // 冻结金额
                userAutoPreTransactionRequest.setAmount(amount);
                // 预处理业务类型
                userAutoPreTransactionRequest.setBizType(PreprocessBusinessTypeCode.TENDER.getCode());
                // 标的号
                userAutoPreTransactionRequest.setProjectNo(project.getProjectNo());
                // 请求流水号
                userAutoPreTransactionRequest.setRequestNo(tender.getRequestNo());
                // 投资人用户编码
                userAutoPreTransactionRequest.setUserNo(restResponse.getResult().getUserNo());
                // 设置 关联业务实体标识
                userAutoPreTransactionRequest.setId(tender.getId());

                //远程调用存管代理服务
        RestResponse<String> response = depositoryAgentApiAgent.userAutoPreTransaction(userAutoPreTransactionRequest);

        //3. 根据结果更新投标状态
            //3.1 判断结果
            if (response.getResult().equals(DepositoryReturnCode.RETURN_CODE_00000)){

            //3.2修改状态为：已同步
                tender.setStatus(1);
                tenderMapper.updateById(tender);

            //3.3 判断当前标的是否满标，如果满标，更新标的状态
                BigDecimal projectremainingAmount = getProjectremainingAmount(project);
                if (projectremainingAmount.compareTo(new BigDecimal(0)) == 0){
                    project.setProjectStatus(ProjectCode.FULLY.getCode());
                    updateById(project);
                }

                //3.4 转换为DTO对象，并封装相关数据
                TenderDTO tenderDTO = convertTenderEntityToDTO(tender);
                    //封装标的信息
                    project.setRepaymentWay(RepaymentWayCode.FIXED_REPAYMENT.getCode());
                    tenderDTO.setProject(converProjectToEntity(project));

                    //封装预期收益
                        //根据标的期限计算还款月数
                        final Double ceil = Math.ceil(project.getPeriod() / 30.0);
                        int month = ceil.intValue();
                tenderDTO.setExpectedIncome(IncomeCalcUtil.getIncomeTotalInterest(amount, configService.getAnnualRate(), month));

                return tenderDTO;
            }else{
                throw new BusinessException(TransactionErrorCode.E_150113);
            }


    }




    /**
     * 审核标的满标放款
     * @param id
     * @param approveStatus
     * @param commission
     * @return
     */
    @Override
    public String LoansApprovalStatus(Long id, String approveStatus, String commission) {
        //第一阶段1.生成放款明细
            //标的信息
            Project project = getById(id);
            //投标信息
            QueryWrapper<Tender> wrapper = new QueryWrapper<>();
            wrapper.eq("projectId",id);
            List<Tender> tenderList = tenderMapper.selectList(wrapper);
            //生成放款明细
            LoanRequest loanRequest = generateLoanRequest(project, tenderList, commission);

        //第二阶段2.放款
        RestResponse<String> restResponse = depositoryAgentApiAgent.confirmLoan(loanRequest);
        if (restResponse.getResult().equals(DepositoryReturnCode.RETURN_CODE_00000.getCode())){

            //修改投标信息为：已放款
            updateTenderStatusAlreadyLoan(tenderList);


        //第三阶段3.修改状态
            //创建请求参数对象
            ModifyProjectStatusDTO modifyProjectStatusDTO = new ModifyProjectStatusDTO();
            modifyProjectStatusDTO.setId(project.getId());
            modifyProjectStatusDTO.setProjectStatus(ProjectCode.REPAYING.getCode());
            modifyProjectStatusDTO.setRequestNo(loanRequest.getRequestNo());
            modifyProjectStatusDTO.setProjectNo(project.getProjectNo());

            //向存管代理服务发起请求
            RestResponse<String> modifyProjectStatus = depositoryAgentApiAgent.modifyProjectStatus(modifyProjectStatusDTO);
            if (modifyProjectStatus.getResult().equals(DepositoryReturnCode.RETURN_CODE_00000.getCode())){




        //第四阶段4.启动还款
            //准备数据
                ProjectWithTendersDTO projectWithTendersDTO = new ProjectWithTendersDTO();
                // 1.标的信息
                ProjectDTO projectDTO = converProjectToEntity(project);
                projectWithTendersDTO.setProject(projectDTO);

                //2.投标信息
                ArrayList<TenderDTO> tenderDTOS = new ArrayList<>();
                for (Tender tender : tenderList) {
                    TenderDTO tenderDTO = convertTenderEntityToDTO(tender);
                    tenderDTOS.add(tenderDTO);
                }
                projectWithTendersDTO.setTenders(tenderDTOS);

                //3.投资人让利
                projectWithTendersDTO.setCommissionInvestorAnnualRate(configService.getCommissionAnnualRate());

                //4.借款人让利
                projectWithTendersDTO.setCommissionBorrowerAnnualRate(configService.getBorrowerAnnualRate());

            //涉及到分布式事务，通过RocketMQ
            p2pTransactionProducer.updateProjectStatusAndStartrepayment(project,projectWithTendersDTO);

        return "审核成功";

            }else{
                throw new BusinessException(TransactionErrorCode.E_150116);
            }

        }else{
            throw new BusinessException(TransactionErrorCode.E_150117);
        }

    }


    /**
     * 修改标的状态为还款中
     * @param project
     * @return
     */
    @Transactional(rollbackFor = BusinessException.class)
    public Boolean updateProjectStatusAndStartRepayment(Project project){
        //如果处理成功，就修改标的状态为还款中
        project.setProjectStatus(ProjectCode.REPAYING.getCode());
        return updateById(project);
    }



    /**
     * 修改投标信息的状态为：已放款
     * @param tenderList
     */
    private void updateTenderStatusAlreadyLoan(List<Tender> tenderList){

        for (Tender tender : tenderList) {
            tender.setTenderStatus(TradingCode.LOAN.getCode());
            tenderMapper.updateById(tender);
        }
    }




    /**
     * 根据标的和投标信息生成放款明细
     * @param project
     * @param tenderList
     * @param commission
     * @return
     */
    public LoanRequest generateLoanRequest(Project project,List<Tender> tenderList,String commission){
        LoanRequest loanRequest = new LoanRequest();

        //封装标的id
        loanRequest.setId(project.getId());

        //封装平台佣金
        if (StringUtils.isNotBlank(commission)){
            loanRequest.setCommission(new BigDecimal(commission));
        }

        //封装标的编码
        loanRequest.setProjectNo(project.getProjectNo());

        //封装请求流水号
        loanRequest.setRequestNo(CodeNoUtil.getNo(CodePrefixCode.CODE_REQUEST_PREFIX));


        //封装放款明细
        List<LoanDetailRequest> details= new ArrayList<>();
        for (Tender tender : tenderList) {
            LoanDetailRequest loanDetailRequest = new LoanDetailRequest();
            loanDetailRequest.setAmount(tender.getAmount());
            loanDetailRequest.setPreRequestNo(tender.getRequestNo());
            details.add(loanDetailRequest);
        }
        loanRequest.setDetails(details);


        return loanRequest;
    }




    /**
     * Tender 对象转换为 TenderDTO 对象
     * @param tender
     * @return
     */
    private TenderDTO convertTenderEntityToDTO(Tender tender){
        if (tender == null){
            return null;
        }
        TenderDTO tenderDTO = new TenderDTO();
        BeanUtils.copyProperties(tender,tenderDTO);

        return tenderDTO;
    }




    /**
     * 获取剩余额度
     * @param project
     * @return
     */
    private BigDecimal getProjectremainingAmount(Project project){
        //根据标的id在投标表查询已投金额
        List<BigDecimal> decimalList = tenderMapper
                .selectAmountInvestendByProjectId(project.getId());

        //求和结果集
        BigDecimal decimal = new BigDecimal("0.0");
        for (BigDecimal d : decimalList) {
            decimal = decimal.add(d);
        }

        //得到结果数据
        return project.getAmount().subtract(decimal);
    }





    /**
     * 将 ProjectDTO 对象转换为 Project对象
     * @param projectDTO
     * @return
     */
    private Project converProjectDTOToEntity(ProjectDTO projectDTO){
        if (projectDTO == null){
            return null;
        }
        Project project = new Project();
        BeanUtils.copyProperties(projectDTO,project);

        return project;
    }



    /**
     * 将Project对象 转换为 ProjectDTO对象
     * @param project
     * @return
     */
    private ProjectDTO converProjectToEntity(Project project){
        if (project == null){
            return null;
        }
        ProjectDTO projectDTO = new ProjectDTO();
        BeanUtils.copyProperties(project,projectDTO);

        return projectDTO;
    }



    /**
     * 把Project泛型的List集合 转换为 ProjectDTO泛型的List集合
     * @param projectList
     * @return
     */
    private List<ProjectDTO> EntityListToDTOList(List<Project> projectList){
        if (projectList == null){
            return null;
        }
        ArrayList<ProjectDTO> dtoList = new ArrayList<>();
        for (Project project : projectList) {
            ProjectDTO projectDTO = new ProjectDTO();
            BeanUtils.copyProperties(project,projectDTO);
            dtoList.add(projectDTO);
        }
        return dtoList;
    }

}
