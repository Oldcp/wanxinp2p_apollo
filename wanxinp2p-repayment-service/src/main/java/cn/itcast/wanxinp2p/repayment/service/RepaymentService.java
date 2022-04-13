package cn.itcast.wanxinp2p.repayment.service;

import cn.itcast.wanxinp2p.api.depository.model.RepaymentRequest;
import cn.itcast.wanxinp2p.api.repayment.model.ProjectWithTendersDTO;
import cn.itcast.wanxinp2p.repayment.entity.RepaymentDetail;
import cn.itcast.wanxinp2p.repayment.entity.RepaymentPlan;

import java.util.List;

/**
 * @author old money
 * @create 2022-02-26 14:29
 */
public interface RepaymentService {



    /**
     * 启动还款
     * @param projectWithTendersDTO
     * @return
     */
    String startRepayment(ProjectWithTendersDTO projectWithTendersDTO);



    /**
     * 查询到期的还款计划
     * @param date 日期格式: yyyy-MM-dd
     * @return
     */
    List<RepaymentPlan> selectDueRepayment(String date);
    List<RepaymentPlan> selectDueRepayment(String date,int count,int item);




    /**
     * 根据还款计划生成还款明细并保存
     * @param repaymentplan
     * @return
     */
    RepaymentDetail saveRepaymentDetail(RepaymentPlan repaymentplan);





    /**
     * 执行用户还款
     * @param date
     */
    void executeRepayment(String date,int count,int item);


    /**
     * 还款预处理，冻结借款人应还金额
     * @param repaymentPlan 还款计划
     * @param preRequestNo 还款请求流水号
     * @return
     */
    Boolean preRepayment(RepaymentPlan repaymentPlan,String preRequestNo);





    /**
     * 执行本地事务
     * 确认还款处理
     * @param repaymentPlan
     * @param repaymentRequest
     * @return
     */
    Boolean confirmRepayment(RepaymentPlan repaymentPlan, RepaymentRequest repaymentRequest);





    /**
     *远程调用确认还款接口
     * @param repaymentPlan
     * @param repaymentRequest
     */
    void invokeConfirmRepayment(RepaymentPlan repaymentPlan,RepaymentRequest repaymentRequest);



    /**
     * 查询还款人相关信息，并调用发送短信接口发送还款提醒
     * @param date 还款日期
     */
    void sendRepaymentNotify(String date);

}
