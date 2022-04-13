package cn.itcast.wanxinp2p.api.account;

import cn.itcast.wanxinp2p.api.account.model.AccountDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountLoginDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountRegisterDTO;
import cn.itcast.wanxinp2p.common.domain.RestResponse;

/**
 * 统一账户接口API
 *
 * @author old money
 * @create 2022-02-10 16:37
 */
public interface AccountAPI {

    /**
     * 获取手机验证码
     * @param mobile 手机号
     * @return
     */
    RestResponse getSMSCode(String mobile);


    /**
     * 校验手机号和验证码
     * @param mobile 手机号
     * @param key 校验标识
     * @param code 验证码
     * @return
     */
    RestResponse<Integer> checkMobile(String mobile, String key, String code);


    /**
     * 注册保存用户信息
     * @param accountRegisterDTO
     * @return
     */
    RestResponse<AccountDTO> register(AccountRegisterDTO accountRegisterDTO);



    /**
     * 实现用户登录功能
     * @param accountLoginDTO 封装用户登录信息
     * @return
     */
    RestResponse<AccountDTO> login(AccountLoginDTO accountLoginDTO);


}
