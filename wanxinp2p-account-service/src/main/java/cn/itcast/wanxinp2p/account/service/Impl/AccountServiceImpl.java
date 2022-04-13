package cn.itcast.wanxinp2p.account.service.Impl;

import cn.itcast.wanxinp2p.account.common.AccountErrorCode;
import cn.itcast.wanxinp2p.account.entity.Account;
import cn.itcast.wanxinp2p.account.mapper.AccountMapper;
import cn.itcast.wanxinp2p.account.service.AccountService;
import cn.itcast.wanxinp2p.account.service.SmsService;
import cn.itcast.wanxinp2p.api.account.model.AccountDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountLoginDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountRegisterDTO;
import cn.itcast.wanxinp2p.common.domain.BusinessException;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import cn.itcast.wanxinp2p.common.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hmily.annotation.Hmily;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author old money
 * @create 2022-02-10 16:52
 */
//业务层实现类 继承MP的类
@Service
@Slf4j
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Autowired
    private SmsService smsService;


    /**
     * 配置中是否通过短信获取验证码的属性
     */
    @Value("${sms.enable}")
    private Boolean smsEnable;


    /**
     * 发送并获取短信验证码
     * @param mobile 手机号
     * @return
     */
    @Override
    public RestResponse getSMSCode(String mobile) {

       return smsService.getSMSCode(mobile);
    }


    /**
     * 验证手机号及验证码
     * @param mobile
     * @param key
     * @param code
     * @return
     */
    @Override
    public Integer checkMobile(String mobile, String key, String code) {

        //校验 验证码是否正确
        smsService.verifySmsCode(key,code);

        QueryWrapper<Account> wrapper = new QueryWrapper<>();

        //查询手机号是否存在
        wrapper.eq("mobile",mobile);

        int count = count(wrapper);
        return count>0?1:0;
    }



    /**
     * 注册保存用户信息
     * @param accountRegisterDTO
     * @return
     */
    @Override
    @Hmily(confirmMethod = "confirmRegister",cancelMethod = "cancelRegister") //分布式事务框架Hmily注解，以实现注册保存用户信息的TCC 分布式事务
    public AccountDTO register(AccountRegisterDTO accountRegisterDTO) {

        //将accountRegisterDTO 中的属性转换为 Account 对象 通过mp封装的方法保存
        Account account = new Account();
        account.setUsername(accountRegisterDTO.getUsername());
        account.setMobile(accountRegisterDTO.getMobile());

        //将密码加密保存
        account.setPassword(PasswordUtil.generate(accountRegisterDTO.getPassword()));

        //判断是否开启使用短信验证码登录
        if (smsEnable){ // 配置中配置的该属性如果为 true 表示使用手机号作为密码
            account.setPassword(PasswordUtil.generate(accountRegisterDTO.getMobile()));
        }
        account.setDomain("c");

        //mp 封装的保存方法 参数需要Account对象
        save(account);

        //将 account 中的属性 转换为AccountDTO对象返回
        return convertAccountEntityToDTO(account);
    }


    /**
     * 分布式事务框架Hmily 的confirm方法 ，实现注册保存用户信息功能的TCC分布式事务
     * 提交
     * @param registerDTO
     */
    public void confirmRegister(AccountRegisterDTO registerDTO) {
        log.info("execute confirmRegister");
    }


    /**
     * 分布式事务框架 Hmily 的 cancel方法，实现注册保存用户信息功能的 TCC 分布式事务
     * 回滚
     * @param registerDTO
     */
    public void cancelRegister(AccountRegisterDTO registerDTO) {
        log.info("execute cancelRegister");
        //删除账号
        remove(Wrappers.<Account>lambdaQuery().eq(Account::getUsername,
                registerDTO.getUsername()));
    }






    /**
         * 实现用户登录功能
         * @param accountLoginDTO
         * @return
         */
    @Override
    public AccountDTO login(AccountLoginDTO accountLoginDTO) {
        //先根据用户名进行查询，然后再比对密码

        Account account = null;
        if (accountLoginDTO.getDomain().equalsIgnoreCase("c")){
            //如果是C端用户，用户名就是手机号
           account = getAccountByMobile(accountLoginDTO.getMobile());

        }else {
            //如果是b端用户登录，用户名就是账户
           account = getAccountByUserName(accountLoginDTO.getUsername());
        }

        if (account == null){
            throw new BusinessException(AccountErrorCode.E_130104);
        }

        AccountDTO accountDTO = convertAccountEntityToDTO(account);

        // 判断是否开启短信验证码登录
        if (smsEnable){ //如果为true，表示采用短信验证码登录，无需比较密码
            return accountDTO;
        }

        //通过加密解密密码的工具类比对密码
        if (PasswordUtil.verify(accountLoginDTO.getPassword(),account.getPassword())){

            return accountDTO;
        }
        throw new BusinessException(AccountErrorCode.E_130105);
    }


    /**
     * 根据手机号查询
     * @param mobile
     * @return
     */
    private Account getAccountByMobile(String mobile){

        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.eq("mobile",mobile);

        Account account = getOne(wrapper);
        return account;
    }


    /**
     * 根据账号查询
     * @param username
     * @return
     */
    private Account getAccountByUserName(String username){
        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.eq("username",username);

        Account account = getOne(wrapper);

        return account;
    }



    /**
     * entity转换为dto
     * @param entity
     * @return
     */
    private AccountDTO convertAccountEntityToDTO(Account entity){
        if (entity == null){
            return null;
        }
        AccountDTO dto = new AccountDTO();

        BeanUtils.copyProperties(entity,dto);

        return dto;
    }

}
