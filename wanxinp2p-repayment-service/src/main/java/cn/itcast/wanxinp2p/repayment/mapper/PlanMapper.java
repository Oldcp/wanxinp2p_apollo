package cn.itcast.wanxinp2p.repayment.mapper;

import cn.itcast.wanxinp2p.repayment.entity.RepaymentPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 *
 * 借款人还款计划 Mapper接口
 * @author old money
 * @create 2022-02-26 14:21
 */
public interface PlanMapper extends BaseMapper<RepaymentPlan> {


    /**
     * 查询所有到期的还款计划
     * @param date
     * @return
     */
    @Select("SELECT * FROM repayment_plan WHERE " +
            "DATE_FORMAT(SHOULD_REPAYMENT_DATE,'%Y-%m-%d') = #{date}"
            + " AND REPAYMENT_STATUS = '0'")
    List<RepaymentPlan> selectDueRepayment(String date);


    @Select("SELECT * FROM repayment_plan WHERE DATE_FORMAT(SHOULD_REPAYMENT_DATE,'%Y-%m-%d') = #{date} AND REPAYMENT_STATUS = '0' AND MOD(NUMBER_OF_PERIODS,#{shardingCount}) = #{shardingItem}")
    List<RepaymentPlan> selectDueRepaymentlist(@Param("date") String date,@Param("shardingCount") int count,@Param("shardingItem") int item);

}
