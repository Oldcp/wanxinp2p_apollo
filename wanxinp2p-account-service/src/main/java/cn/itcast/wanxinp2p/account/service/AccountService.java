package cn.itcast.wanxinp2p.account.service;

import cn.itcast.wanxinp2p.account.entity.Account;
import cn.itcast.wanxinp2p.api.account.model.AccountDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountLoginDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountRegisterDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author old money
 * @create 2022-02-10 16:49
 */
// 业务层接口继承 MP的接口
public interface AccountService extends IService<Account> {


    /**
     * 获取手机验证码
     * @param mobile 手机号
     * @return
     */
    RestResponse getSMSCode(String mobile);


    /**
     * 验证手机号及验证码
     * @param mobile
     * @param key
     * @param code
     * @return
     */
    Integer checkMobile(String mobile,String key,String code);



    /**
     * 注册保存用户信息
     * @param accountRegisterDTO
     * @return
     */
     AccountDTO register(AccountRegisterDTO accountRegisterDTO);



    /**
     * 实现用户登录功能
     * @param accountLoginDTO
     * @return
     */
     AccountDTO login(AccountLoginDTO accountLoginDTO);

}
