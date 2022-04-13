package cn.itcast.wanxinp2p.consumer.service.Impl;

import cn.itcast.wanxinp2p.api.account.model.AccountDTO;
import cn.itcast.wanxinp2p.api.account.model.AccountRegisterDTO;
import cn.itcast.wanxinp2p.api.consumer.model.*;
import cn.itcast.wanxinp2p.api.depository.DepositoryAgentApi;
import cn.itcast.wanxinp2p.api.depository.model.DepositoryConsumerResponse;
import cn.itcast.wanxinp2p.api.depository.model.GatewayRequest;
import cn.itcast.wanxinp2p.common.domain.BusinessException;
import cn.itcast.wanxinp2p.common.domain.CodePrefixCode;
import cn.itcast.wanxinp2p.common.domain.CommonErrorCode;
import cn.itcast.wanxinp2p.common.domain.RestResponse;
import cn.itcast.wanxinp2p.common.util.CodeNoUtil;
import cn.itcast.wanxinp2p.common.util.IDCardUtil;
import cn.itcast.wanxinp2p.consumer.agent.AccountAPIAgent;
import cn.itcast.wanxinp2p.consumer.common.ConsumerErrorCode;
import cn.itcast.wanxinp2p.consumer.entity.BankCard;
import cn.itcast.wanxinp2p.consumer.entity.Consumer;
import cn.itcast.wanxinp2p.consumer.mapper.ConsumerMapper;
import cn.itcast.wanxinp2p.consumer.service.ConsumerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hmily.annotation.Hmily;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author old money
 * @create 2022-02-13 14:10
 */
@Slf4j
@Service
public class ConsumerServiceImpl extends ServiceImpl<ConsumerMapper, Consumer> implements ConsumerService {

    @Autowired
    private AccountAPIAgent accountAPIAgent;

    @Autowired
    private BankCardServiceImpl bankCardService;

    @Resource
    private DepositoryAgentApi depositoryAgentApi;


    /**
     * 检测用户是否存在，查看数据库中是否存在该手机号
     * @param mobile
     * @return
     */
    @Override
    public Integer checkMobile(String mobile) {

        ConsumerDTO byMobile = getByMobile(mobile);

        if (byMobile != null){
            return 1;
        }
        return 0;
    }


    /**
     * 注册保存用户相关信息
     * @param consumerRegisterDTO
     */
    @Override
    @Hmily(confirmMethod = "confirmRegister",cancelMethod = "cancelRegister") //添加分布式事务Hmily框架注解，实现登录的TCC分布式事务
    public void register(ConsumerRegisterDTO consumerRegisterDTO) {

        //首先判断是否是新用户
        if (checkMobile(consumerRegisterDTO.getMobile()) == 1){
            //存在即抛出公共的通知异常
            throw new BusinessException(ConsumerErrorCode.E_140107);
        }

        //不存在即保存到数据库中
        Consumer consumer = new Consumer();
        BeanUtils.copyProperties(consumerRegisterDTO,consumer);

        //一些consumerRegisterDTO中没有的值进行人工赋值
        consumer.setUsername(CodeNoUtil.getNo(CodePrefixCode.CODE_NO_PREFIX));
        consumer.setUserNo(CodeNoUtil.getNo(CodePrefixCode.CODE_REQUEST_PREFIX));
        consumer.setIsBindCard(0);

        save(consumer);

        //远程调用 Account 微服务的保存用户信息服务
        AccountRegisterDTO accountRegisterDTO = new AccountRegisterDTO();
        consumerRegisterDTO.setUsername(consumer.getUsername());

        BeanUtils.copyProperties(consumerRegisterDTO,accountRegisterDTO);

        RestResponse<AccountDTO> register = accountAPIAgent.register(accountRegisterDTO);

        //判断 Account微服务是否成功保存用户信息
        if (register.getCode() != CommonErrorCode.SUCCESS.getCode()){
            throw new BusinessException(ConsumerErrorCode.E_140106);
        }
    }


    /**
     * 生成开户数据
     * @param consumerRequest
     * @return
     */
    @Override
    @Transactional  //添加本地事务注解
    public RestResponse<GatewayRequest> createConsumer(ConsumerRequest consumerRequest) {

        //1.判断当前用户是否已经开户
        ConsumerDTO consumerDTO = getByMobile(consumerRequest.getMobile());
        Integer isBindCard = consumerDTO.getIsBindCard();
        if (isBindCard == 1){
            throw new BusinessException(ConsumerErrorCode.E_140105);
        }


        //2.判断提交过来的银行卡是否已被绑定
        BankCardDTO byCardNumber = bankCardService.getByCardNumber(consumerRequest.getCardNumber());
        if (byCardNumber != null){
            throw new BusinessException(ConsumerErrorCode.E_140151);
        }


        //3.更新用户的信息
        consumerRequest.setId(consumerDTO.getId());
            //产生请求流水号和用户编号
            consumerRequest.setUserNo(CodeNoUtil.getNo(CodePrefixCode.CODE_CONSUMER_PREFIX));
            consumerRequest.setRequestNo(CodeNoUtil.getNo(CodePrefixCode.CODE_REQUEST_PREFIX));

                //设置查询条件和需要更新的数据
                UpdateWrapper<Consumer> wrapper = new UpdateWrapper<>();
                wrapper.eq("username",consumerDTO.getUsername());
                wrapper.set("userNo",consumerRequest.getUserNo());
                wrapper.set("requestNo",consumerRequest.getRequestNo());
                wrapper.set("fullname",consumerRequest.getFullname());
                wrapper.set("idNumber",consumerRequest.getIdNumber());
                wrapper.set("authList","ALL");
                update(wrapper);



        //4.保存银行卡的信息
        BankCard bankCard = new BankCard();
        bankCard.setConsumerId(consumerDTO.getId());
        bankCard.setBankCode(consumerRequest.getBankCode());
        bankCard.setCardNumber(consumerRequest.getCardNumber());
        bankCard.setMobile(consumerDTO.getMobile());
        //bankCard.setStatus();
                //判断之前该用户的银行卡信息是否有残留
                BankCardDTO existBankCard = bankCardService.getByConsumerId(bankCard.getConsumerId());
                if (existBankCard != null){
                    bankCard.setId(existBankCard.getId());
                }
                bankCardService.saveOrUpdate(bankCard);



        //5.准备数据，发起远程调用，把数据发送到存管代理服务
        return depositoryAgentApi.createConsumer(consumerRequest);
    }






    /**
     * Hmily分布式事务框的 confirm方法，实现登录功能的 TCC 分布式事务
     * 提交
     * @param consumerRegisterDTO
     */
    public void confirmRegister(ConsumerRegisterDTO consumerRegisterDTO) {
        log.info("execute confirmRegister");
    }


    /**
     * Hmily分布式事务框架的 cancel方法，实现登录功能的 TCC 分布式事务
     * 回滚
     * @param consumerRegisterDTO
     */
    public void cancelRegister(ConsumerRegisterDTO consumerRegisterDTO) {
        log.info("execute cancelRegister");
        remove(Wrappers.<Consumer>lambdaQuery().eq(Consumer::getMobile,
                consumerRegisterDTO.getMobile()));
    }









    /**
     * 接收存管代理服务异步发送的消息，更新开户结果
     * @param response
     * @return
     */
    @Override
    @Transactional //本地事务的注解
    public Boolean modifyResult(DepositoryConsumerResponse response) {
        //1.获取数据（状态）
        int status = response.getRespCode().equals(00000) ? 1 : 2;

        //2.更新用户信息
            Consumer byRequestNo = getByRequestNo(response.getRequestNo());
        UpdateWrapper<Consumer> wrapper = new UpdateWrapper<>();
        wrapper.eq("id",byRequestNo.getId());
        wrapper.set("isBindCard",status);
        wrapper.set("status",byRequestNo.getStatus());
        update(wrapper);

        //3.更新银行卡信息
        UpdateWrapper<BankCard> bankWrapper = new UpdateWrapper<>();
        bankWrapper.eq("consumerId",byRequestNo.getId());
        bankWrapper.set("status",status);
        bankWrapper.set("bankCode",response.getBankCode());
        bankWrapper.set("bankName",response.getBankName());

        return bankCardService.update(bankWrapper);
    }





    /**
     * 根据手机号查询
     * @param mobile
     * @return
     */
    public ConsumerDTO getByMobile(String mobile){

        QueryWrapper<Consumer> wrapper = new QueryWrapper<>();

        wrapper.eq("mobile",mobile);

        Consumer consumer = getOne(wrapper);

        ConsumerDTO consumerDTO = convertConsumerEntityToDTO(consumer);

        return consumerDTO;
    }


    /**
     * 获取借款人基本信息
     * @param id
     * @return
     */
    @Override
    public BorrowerDTO getBorrower(Long id) {
        ConsumerDTO consumerDTO = get(id);

        BorrowerDTO borrowerDTO = new BorrowerDTO();
        BeanUtils.copyProperties(consumerDTO,borrowerDTO);


        Map<String, String> map = IDCardUtil.getInfo(borrowerDTO.getIdNumber()); //通过身份证号码获取出生日期、性别、年龄
        borrowerDTO.setAge(new Integer(map.get("age")));
        borrowerDTO.setBirthday(map.get("birthday"));
        borrowerDTO.setGender(map.get("gender"));

        return borrowerDTO;
    }


    /**
     * 查询借款人基本信息
     * @param id
     * @return
     */
    private ConsumerDTO get(Long id){
        Consumer entity = getById(id);

        if (entity == null){
            log.info("id为{}的用户信息不存在");
            throw new BusinessException(ConsumerErrorCode.E_140101);
        }
        return convertConsumerEntityToDTO(entity);
    }



    /**
     * consumer 转换为 consumerDTO 对象
     * @param consumer
     * @return
     */
    private ConsumerDTO convertConsumerEntityToDTO(Consumer consumer){
        if (consumer == null){
            return null;
        }

        ConsumerDTO consumerDTO = new ConsumerDTO();

        BeanUtils.copyProperties(consumer,consumerDTO);

        return consumerDTO;
    }


    /**
     * 根据请求流水号(requestNo)查询用户
     * @param requestNo
     * @return
     */
    private Consumer getByRequestNo(String requestNo){
        QueryWrapper<Consumer> wrapper = new QueryWrapper<>();
        wrapper.eq("requestNo",requestNo);
        Consumer consumer = getOne(wrapper);
        return consumer;
    }


}
