package cn.itcast.wanxinp2p.consumer.service.Impl;

import cn.itcast.wanxinp2p.api.consumer.model.BankCardDTO;
import cn.itcast.wanxinp2p.consumer.entity.BankCard;
import cn.itcast.wanxinp2p.consumer.mapper.BankCardMapper;
import cn.itcast.wanxinp2p.consumer.service.BankCardService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * @author old money
 * @create 2022-02-16 16:15
 */
@Service
public class BankCardServiceImpl extends ServiceImpl<BankCardMapper, BankCard> implements BankCardService {


    /**
     * 根据用户 id 获取银行卡信息
     * @param consumerId 用户id
     * @return
     */
    @Override
    public BankCardDTO getByConsumerId(Long consumerId) {

        QueryWrapper<BankCard> wrapper = new QueryWrapper<>();
        wrapper.eq("consumerId",consumerId);
        BankCard bankCard = getOne(wrapper);

        BankCardDTO bankCardDTO = converBankCardEntityToDTO(bankCard);

        return bankCardDTO;
    }


    /**
     * 根据用户 卡号 获取银行卡信息
     * @param cardNumber 卡号
     * @return
     */
    @Override
    public BankCardDTO getByCardNumber(String cardNumber) {

        QueryWrapper<BankCard> wrapper = new QueryWrapper<>();
        wrapper.eq("cardNumber",cardNumber);
        BankCard bankCard = getOne(wrapper);

        BankCardDTO bankCardDTO = converBankCardEntityToDTO(bankCard);
        return bankCardDTO;
    }


    /**
     * BankCard 转换为 BankCarDTO 的方法
     * @param bankCard
     * @return
     */
    private BankCardDTO converBankCardEntityToDTO(BankCard bankCard){
        if (bankCard == null){
            return null;
        }
        BankCardDTO bankCardDTO = new BankCardDTO();
        BeanUtils.copyProperties(bankCard,bankCardDTO);
        return bankCardDTO;
    }
}
