package cn.itcast.wanxinp2p.repayment.service.Impl;

import cn.itcast.wanxinp2p.api.consumer.model.BorrowerDTO;
import cn.itcast.wanxinp2p.api.depository.model.RepaymentDetailRequest;
import cn.itcast.wanxinp2p.api.depository.model.RepaymentRequest;
import cn.itcast.wanxinp2p.api.depository.model.UserAutoPreTransactionRequest;
import cn.itcast.wanxinp2p.api.repayment.model.ProjectWithTendersDTO;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.api.transaction.model.TenderDTO;
import cn.itcast.wanxinp2p.common.domain.*;
import cn.itcast.wanxinp2p.common.util.CodeNoUtil;
import cn.itcast.wanxinp2p.common.util.DateUtil;
import cn.itcast.wanxinp2p.repayment.agent.ConsumerApiAgent;
import cn.itcast.wanxinp2p.repayment.agent.DepositoryAgentApiAgent;
import cn.itcast.wanxinp2p.repayment.entity.ReceivableDetail;
import cn.itcast.wanxinp2p.repayment.entity.ReceivablePlan;
import cn.itcast.wanxinp2p.repayment.entity.RepaymentDetail;
import cn.itcast.wanxinp2p.repayment.entity.RepaymentPlan;
import cn.itcast.wanxinp2p.repayment.mapper.PlanMapper;
import cn.itcast.wanxinp2p.repayment.mapper.ReceivableDetailMapper;
import cn.itcast.wanxinp2p.repayment.mapper.ReceivablePlanMapper;
import cn.itcast.wanxinp2p.repayment.mapper.RepaymentDetailMapper;
import cn.itcast.wanxinp2p.repayment.message.RepaymentProducer;
import cn.itcast.wanxinp2p.repayment.model.EqualInterestRepayment;
import cn.itcast.wanxinp2p.repayment.service.RepaymentService;
import cn.itcast.wanxinp2p.repayment.sms.SmsService;
import cn.itcast.wanxinp2p.repayment.util.RepaymentUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author old money
 * @create 2022-02-26 14:33
 */
@Service
@Slf4j
public class RepaymentServiceImpl implements RepaymentService {


    @Resource
    private PlanMapper planMapper;

    @Resource
    private ReceivablePlanMapper receivablePlanMapper;

    @Autowired
    private DepositoryAgentApiAgent depositoryAgentApiAgent;

    @Resource
    private RepaymentDetailMapper repaymentDetailMapper;

    @Resource
    private ReceivableDetailMapper receivableDetailMapper;


    @Autowired
    private RepaymentProducer repaymentProducer;


    @Autowired
    private ConsumerApiAgent consumerApiAgent;

    @Resource
    private SmsService smsService;


    /**
     * 启动还款
     * @param projectWithTendersDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public String startRepayment(ProjectWithTendersDTO projectWithTendersDTO) {
        //1.生成借款人还款计划
            //1.1获取标的信息
            ProjectDTO projectDTO = projectWithTendersDTO.getProject();

            //1.2获取投标信息
            List<TenderDTO> tenders = projectWithTendersDTO.getTenders();

            //1.3计算还款的月数
            Double ceil = Math.ceil(projectDTO.getPeriod() / 30);
            Integer month = ceil.intValue();

            //1.4还款方式：只针对等额本息
            String repaymentWay = projectDTO.getRepaymentWay();
            if (repaymentWay.equals(RepaymentWayCode.FIXED_REPAYMENT.getCode())){


                //1.5生成还款计划
                EqualInterestRepayment equalInterestRepayment = RepaymentUtil.fixedRepayment(projectDTO.getAmount(), projectDTO.getBorrowerAnnualRate(), month, projectDTO.getCommissionAnnualRate());

                //1.6保存还款计划
                List<RepaymentPlan> planList = saveRepaymentPlan(projectDTO, equalInterestRepayment);


        //2.生成投资人应收明细
                //2.1根据投标信息生成应收明细
                for (TenderDTO tender : tenders) {
                    EqualInterestRepayment fixedRepayment = RepaymentUtil.fixedRepayment(tender.getAmount(), tender.getProjectAnnualRate(), month, projectWithTendersDTO.getCommissionBorrowerAnnualRate());

                //2.2保存应收明细到数据库
                    for (RepaymentPlan repaymentPlan : planList) {
                        saveRreceivablePlan(repaymentPlan,tender,fixedRepayment);
                    }
                }
            }else{
                return "-1";
            }
        return DepositoryReturnCode.RETURN_CODE_00000.getCode();
    }


    /**
     * 查询所有到期的还款计划
     * @param date 日期格式: yyyy-MM-dd
     * @return
     */
    @Override
    public List<RepaymentPlan> selectDueRepayment(String date) {
        List<RepaymentPlan> repaymentPlans = planMapper.selectDueRepayment(date);
        return repaymentPlans;
    }




    /**
     * 查询所有到期还款计划
     * @param date 日期格式: yyyy-MM-dd
     * @return
     */
    @Override
    public List<RepaymentPlan> selectDueRepayment(String date, int count, int item) {

        List<RepaymentPlan> repaymentPlans = planMapper.selectDueRepaymentlist(date,count,item);

        return repaymentPlans;
    }






    /**
     * 根据还款计划生成还款明细并保存
     * @param repaymentplan
     * @return
     */
    @Override
    public RepaymentDetail saveRepaymentDetail(RepaymentPlan repaymentplan) {
        //1.进行查询
        QueryWrapper<RepaymentDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("repaymentPlanId",repaymentplan.getId());

        RepaymentDetail repaymentDetail = repaymentDetailMapper.selectOne(wrapper);

        //2.查不到数据才进行保存
        if (repaymentDetail == null){
            repaymentDetail = new RepaymentDetail();
            //还款计划项标识
            repaymentDetail.setRepaymentPlanId(repaymentplan.getId());
            //实还本息
            repaymentDetail.setAmount(repaymentplan.getAmount());
            //实际还款时间
            repaymentDetail.setRepaymentDate(LocalDateTime.now());
            //请求流水号
            repaymentDetail.setRequestNo(CodeNoUtil.getNo(CodePrefixCode.CODE_REQUEST_PREFIX));
            //未同步
            repaymentDetail.setStatus(StatusCode.STATUS_OUT.getCode());
            //保存数据
            repaymentDetailMapper.insert(repaymentDetail);
        }

        return repaymentDetail;
    }






    /**
     * 执行用户还款
     * @param date
     */
    @Override
    public void executeRepayment(String date,int count,int item) {
        //查询到期的还款计划
        List<RepaymentPlan> repaymentPlans = selectDueRepayment(date,count,item);

        //生成还款明细
        RepaymentDetail repaymentDetail = new RepaymentDetail();
        for (RepaymentPlan repaymentPlan : repaymentPlans) {
            repaymentDetail = saveRepaymentDetail(repaymentPlan);

            //还款预处理，冻结还款人金额
            Boolean preRepayment = preRepayment(repaymentPlan, repaymentDetail.getRequestNo());

            if (preRepayment){

                RepaymentRequest repaymentRequest = generateRepaymentRequest(repaymentPlan, repaymentDetail.getRequestNo());
                repaymentProducer.confirmRepayment(repaymentPlan,repaymentRequest);
            }
        }
    }





    /**
     * 构造还款消息请求数据 （RepaymentRequest）
     * @param repaymentPlan
     * @param preRequestNo
     * @return
     */
    private RepaymentRequest generateRepaymentRequest(RepaymentPlan repaymentPlan,String preRequestNo){

        RepaymentRequest repaymentRequest = new RepaymentRequest();
        //还款总额
        repaymentRequest.setAmount(repaymentPlan.getAmount());
        //业务实体id
        repaymentRequest.setId(repaymentPlan.getId());
        //向借款人收取的佣金
        repaymentRequest.setCommission(repaymentPlan.getCommission());
        //标的编码
        repaymentRequest.setProjectNo(repaymentPlan.getProjectNo());
        //请求流水号
        repaymentRequest.setRequestNo(CodeNoUtil.getNo(CodePrefixCode.CODE_REQUEST_PREFIX));
        //预处理业务流水号
        repaymentRequest.setPreRequestNo(preRequestNo);


        //根据还款计划id，查询应收计划
        QueryWrapper<ReceivablePlan> wrapper = new QueryWrapper<>();
        wrapper.eq("repaymentId",repaymentPlan.getId());
        List<ReceivablePlan> receivablePlans = receivablePlanMapper.selectList(wrapper);

        ArrayList<RepaymentDetailRequest> detailRequests = new ArrayList<>();

        for (ReceivablePlan receivablePlan : receivablePlans) {
            RepaymentDetailRequest detailRequest = new RepaymentDetailRequest();
            detailRequest.setAmount(receivablePlan.getAmount());
            detailRequest.setCommission(receivablePlan.getCommission());
            detailRequest.setInterest(receivablePlan.getInterest());
            detailRequest.setUserNo(receivablePlan.getUserNo());

            //添加到集合
            detailRequests.add(detailRequest);
        }
        //放款明细
        repaymentRequest.setDetails(detailRequests);

        return repaymentRequest;
    }











    /**
     * 还款预处理，冻结借款人应还金额
     * @param repaymentPlan 还款计划
     * @param preRequestNo 还款请求流水号
     * @return
     */
    @Override
    public Boolean preRepayment(RepaymentPlan repaymentPlan, String preRequestNo) {
        //1.构造请求数据
        UserAutoPreTransactionRequest userAutoPreTransactionRequest =
                generateUserAutoPreTransactionRequest(repaymentPlan, preRequestNo);
        //2.请求存管代理服务
        RestResponse<String> restResponse = depositoryAgentApiAgent.userAutoPreTransaction(userAutoPreTransactionRequest);

        //3.返回结果
        return restResponse.getResult().equals(DepositoryReturnCode.RETURN_CODE_00000.getCode());
    }











    /**
     * 执行本地事务
     * 确认还款处理
     * @param repaymentPlan
     * @param repaymentRequest
     * @return
     */
    @Override
    @Transactional
    public Boolean confirmRepayment(RepaymentPlan repaymentPlan, RepaymentRequest repaymentRequest) {
        //1.更新还款明细：已同步
        UpdateWrapper<RepaymentDetail> wrapper = new UpdateWrapper<>();
        wrapper.eq("requestNo",repaymentRequest.getPreRequestNo());
        wrapper.set("status",StatusCode.STATUS_IN.getCode());
        repaymentDetailMapper.update(null,wrapper);

        //2.1更新receivable_plan表为：已收
            //根据还款计划id，查询应收计划
            QueryWrapper<ReceivablePlan> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("repaymentId",repaymentPlan.getId());
            List<ReceivablePlan> receivablePlans = receivablePlanMapper.selectList(queryWrapper);
        for (ReceivablePlan receivablePlan : receivablePlans) {
            receivablePlan.setReceivableStatus(1);
            receivablePlanMapper.updateById(receivablePlan);

            //2.2保存数据到receivable_detail（应收明细）
            ReceivableDetail receivableDetail = new ReceivableDetail();
            //应收项标识
            receivableDetail.setReceivableId(receivablePlan.getId());
            //应收本息
            receivableDetail.setAmount(receivablePlan.getAmount());
            //应收时间
            receivableDetail.setReceivableDate(DateUtil.now());
            //保存
            receivableDetailMapper.insert(receivableDetail);
        }

        //3.更新还款计划：已收款
        repaymentPlan.setRepaymentStatus("1");
        int i = planMapper.updateById(repaymentPlan);

        return i>0;
    }






    /**
     * 远程调用确认还款接口
     * @param repaymentPlan
     * @param repaymentRequest
     */
    @Override
    public void invokeConfirmRepayment(RepaymentPlan repaymentPlan, RepaymentRequest repaymentRequest) {
        RestResponse<String> restResponse = depositoryAgentApiAgent.confirmRepayment(repaymentRequest);
        if ( ! restResponse.getResult().equals(DepositoryReturnCode.RETURN_CODE_00000.getCode())){

            throw new RuntimeException("还款失败");
        }

    }


    /**
     * 查询还款人相关信息，并调用发送短信接口发送还款提醒
     * @param date 还款日期
     */
    @Override
    public void sendRepaymentNotify(String date) {
        //1.查询到期的还款计划
        List<RepaymentPlan> repaymentPlanList = selectDueRepayment(date);
        //2.遍历还款计划
        for (RepaymentPlan repaymentPlan : repaymentPlanList) {

            RestResponse<BorrowerDTO> consumerResponse = consumerApiAgent.getBorrowerMobile(repaymentPlan.getConsumerId());
            //3.得到还款人的信息
            BorrowerDTO borrowerDTO = consumerResponse.getResult();
            //4.得到还款人的手机号
            String mobile = borrowerDTO.getMobile();
            //5.发送还款短信
            smsService.sendRepaymentNotify(mobile,date,repaymentPlan.getAmount());
        }

    }





    /**
     * 还款计划保存到数据库
     * @param projectDTO
     * @param equalInterestRepayment
     * @return
     */
    public List<RepaymentPlan> saveRepaymentPlan(ProjectDTO projectDTO,EqualInterestRepayment equalInterestRepayment){

        List<RepaymentPlan> repaymentPlans = new ArrayList<>();

        //获取每期利息
        Map<Integer, BigDecimal> interestMap = equalInterestRepayment.getInterestMap();

        //平台收取的利息
        Map<Integer, BigDecimal> commissionMap = equalInterestRepayment.getCommissionMap();

        //获取每期本金
        equalInterestRepayment.getPrincipalMap().forEach((k,v)->{
            RepaymentPlan repaymentPlan = new RepaymentPlan();

            // 标的id
            repaymentPlan.setProjectId(projectDTO.getId());
            // 发标人用户标识
            repaymentPlan.setConsumerId(projectDTO.getConsumerId());
            // 发标人用户编码
            repaymentPlan.setUserNo(projectDTO.getUserNo());
            // 标的编码
            repaymentPlan.setProjectNo(projectDTO.getProjectNo());
            // 期数
            repaymentPlan.setNumberOfPeriods(k);
            // 当期还款利息
            repaymentPlan.setInterest(interestMap.get(k));
            // 还款本金
            repaymentPlan.setPrincipal(v);
            // 本息 = 本金 + 利息
            repaymentPlan.setAmount(repaymentPlan.getPrincipal().add(repaymentPlan.getInterest()));
            // 应还时间 = 当前时间 + 期数( 单位月 )
            repaymentPlan.setShouldRepaymentDate(DateUtil
                    .localDateTimeAddMonth(DateUtil.now(), k));
            // 应还状态, 当前业务为待还
            repaymentPlan.setRepaymentStatus("0");
            // 计划创建时间
            repaymentPlan.setCreateDate(DateUtil.now());
            // 设置平台佣金( 借款人让利 ) 注意这个地方是 具体佣金
            repaymentPlan.setCommission(commissionMap.get(k));
            // 保存到数据库
            planMapper.insert(repaymentPlan);
            repaymentPlans.add(repaymentPlan);
        });
        return repaymentPlans;
    }




    /**
     * 保存应收明细到数据库
     * @param repaymentPlan
     * @param tender
     * @param receipt
     */
    private void saveRreceivablePlan(RepaymentPlan repaymentPlan,
                                     TenderDTO tender,
                                     EqualInterestRepayment receipt) {
        // 应收本金
        final Map<Integer, BigDecimal> principalMap = receipt.getPrincipalMap();
        // 应收利息
        final Map<Integer, BigDecimal> interestMap = receipt.getInterestMap();
        // 平台收取利息
        final Map<Integer, BigDecimal> commissionMap =
                receipt.getCommissionMap();
        // 封装投资人应收明细
        ReceivablePlan receivablePlan = new ReceivablePlan();
        // 投标信息标识
        receivablePlan.setTenderId(tender.getId());
        // 设置期数
        receivablePlan.setNumberOfPeriods(repaymentPlan.getNumberOfPeriods());
        // 投标人用户标识
        receivablePlan.setConsumerId(tender.getConsumerId());
        // 投标人用户编码
        receivablePlan.setUserNo(tender.getUserNo());
        // 还款计划项标识
        receivablePlan.setRepaymentId(repaymentPlan.getId());
        // 应收利息
        receivablePlan.setInterest(interestMap.get(repaymentPlan
                .getNumberOfPeriods()));
        // 应收本金
        receivablePlan.setPrincipal(principalMap.get(repaymentPlan
                .getNumberOfPeriods()));
        // 应收本息 = 应收本金 + 应收利息
        receivablePlan.setAmount(receivablePlan.getInterest()
                .add(receivablePlan.getPrincipal()));
        // 应收时间
        receivablePlan.setShouldReceivableDate(repaymentPlan
                .getShouldRepaymentDate());
        // 应收状态, 当前业务为未收
        receivablePlan.setReceivableStatus(0);
        // 创建时间
        receivablePlan.setCreateDate(DateUtil.now());
        // 设置投资人让利, 注意这个地方是具体: 佣金
        receivablePlan.setCommission(commissionMap
                .get(repaymentPlan.getNumberOfPeriods()));
        // 保存到数据库
        receivablePlanMapper.insert(receivablePlan);
    }


    /**
     * 构造存管代理服务预处理请求数据
     * @param repaymentPlan
     * @param preRequestNo
     * @return
     */
    private UserAutoPreTransactionRequest generateUserAutoPreTransactionRequest(RepaymentPlan repaymentPlan,String preRequestNo){
        //构造请求数据
        UserAutoPreTransactionRequest userAutoPreTransactionRequest = new UserAutoPreTransactionRequest();

        //冻结金额
        userAutoPreTransactionRequest.setAmount(repaymentPlan.getAmount());
        //预处理业务类型
        userAutoPreTransactionRequest.setBizType(PreprocessBusinessTypeCode.REPAYMENT.getCode());
        //标的号
        userAutoPreTransactionRequest.setProjectNo(repaymentPlan.getProjectNo());
        //请求流水号
        userAutoPreTransactionRequest.setRequestNo(preRequestNo);
        //标的用户编码
        userAutoPreTransactionRequest.setUserNo(repaymentPlan.getUserNo());
        //关联业务实体标识
        userAutoPreTransactionRequest.setId(repaymentPlan.getId());
        //返回结果
        return userAutoPreTransactionRequest;
    }



}

