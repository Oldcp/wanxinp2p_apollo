package cn.itcast.wanxinp2p.transaction.mapper;

import cn.itcast.wanxinp2p.transaction.entity.Tender;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 投标信息的mapper接口
 * @author old money
 * @create 2022-02-23 18:23
 */
public interface TenderMapper extends BaseMapper<Tender> {


    /**
     * 根据标的 id , 获取标的已投金额，如果未投返回 0.0
     * @param id
     * @return
     */
    @Select("SELECT IFNULL(SUM(AMOUNT),0.0) FROM tender WHERE PROJECT_ID = #{ID} STATUS = 1")
    List<BigDecimal> selectAmountInvestendByProjectId(Long id);

}
