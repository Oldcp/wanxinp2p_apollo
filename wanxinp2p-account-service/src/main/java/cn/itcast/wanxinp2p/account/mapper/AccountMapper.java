package cn.itcast.wanxinp2p.account.mapper;

import cn.itcast.wanxinp2p.account.entity.Account;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author old money
 * @create 2022-02-11 17:01
 */
// 数据访问层mapper继承 mp的 BaseMapper接口并指定实体类的泛型
public interface AccountMapper extends BaseMapper<Account> {

}
