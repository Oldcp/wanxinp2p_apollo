package cn.itcast.wanxinp2p.depository.service.Impl;

import cn.itcast.wanxinp2p.api.consumer.model.ConsumerRequest;
import cn.itcast.wanxinp2p.api.depository.model.*;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.common.domain.BusinessException;
import cn.itcast.wanxinp2p.common.domain.PreprocessBusinessTypeCode;
import cn.itcast.wanxinp2p.common.domain.StatusCode;
import cn.itcast.wanxinp2p.common.util.EncryptUtil;
import cn.itcast.wanxinp2p.common.util.RSAUtil;
import cn.itcast.wanxinp2p.depository.common.cache.RedisCache;
import cn.itcast.wanxinp2p.depository.common.constant.DepositoryErrorCode;
import cn.itcast.wanxinp2p.depository.common.constant.DepositoryRequestTypeCode;
import cn.itcast.wanxinp2p.depository.entity.DepositoryRecord;
import cn.itcast.wanxinp2p.depository.mapper.DepositoryRecordMapper;
import cn.itcast.wanxinp2p.depository.service.ConfigService;
import cn.itcast.wanxinp2p.depository.service.OkHttpService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @author old money
 * @create 2022-02-16 18:05
 */
@Service
public class DepositoryRecordService extends ServiceImpl<DepositoryRecordMapper, DepositoryRecord> implements cn.itcast.wanxinp2p.depository.service.DepositoryRecordService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private OkHttpService okHttpService;

    @Resource
    private RedisCache redisCache;


    @Override
    public GatewayRequest createConsumer(ConsumerRequest consumerRequest) {
        //1.保存交易记录
        saveDepositoryRecord(consumerRequest);

        //2.签名（加密）数据并返回
            //首先将数据转换为JSON串
            String reqData = JSON.toJSONString(consumerRequest);
            //通过工具类获取数据签名
            String sign = RSAUtil.sign(reqData, configService.getP2pPrivateKey(), "utf-8");

        GatewayRequest gatewayRequest=new GatewayRequest();

        gatewayRequest.setServiceName("PERSONAL_REGISTER");
        gatewayRequest.setPlatformNo(configService.getP2pCode());
        gatewayRequest.setReqData(EncryptUtil.encodeURL(EncryptUtil
                .encodeUTF8StringBase64(reqData)));
        gatewayRequest.setSignature(EncryptUtil.encodeURL(sign));
        gatewayRequest.setDepositoryUrl(configService.getDepositoryUrl() +
                "/gateway");
        return gatewayRequest;
    }


    /**
     * 根据请求流水号更新请求状态
     * @param requestNo 请求流水号
     * @param requestsStatus 请求状态码
     * @return
     */
    @Override
    public Boolean modifyRequestStatus(String requestNo, Integer requestsStatus) {
        UpdateWrapper<DepositoryRecord> wrapper = new UpdateWrapper<>();
        wrapper.eq("requestNo",requestNo);
        wrapper.set("requestStatus",requestsStatus);
        wrapper.set("confirmDate", LocalDateTime.now());

        return update(wrapper);

    }


    /**
     * 保存标的信息，并向银行存管服务发送标的信息
     * @param projectDTO
     * @return
     */
    @Override
    public DepositoryResponseDTO<DepositoryBaseResponse> createProject(ProjectDTO projectDTO) {
        //1.保存交易记录
        DepositoryRecord depositoryRecord = new DepositoryRecord(projectDTO.getRequestNo()
                , DepositoryRequestTypeCode.CREATE.getCode()
                , "Project", projectDTO.getId());
            //实现幂等性
            DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = handleIdempotent(depositoryRecord);
            if (responseDTO != null){
                return responseDTO;
            }

            //使用最新的数据
            depositoryRecord = getEntityByRequestNo(projectDTO.getRequestNo());


        //2.签名数据
            //ProjectDTO 转换为 ProjectRequestDataDTO
        ProjectRequestDataDTO projectRequestDataDTO = concertProjectDTOToProjectRequestDataDTO(projectDTO,
                                                                depositoryRecord.getRequestNo());
            //转换为JSON数据
            String toJSONString = JSON.toJSONString(projectRequestDataDTO);

             //base64加密
            String reqDate = EncryptUtil.encodeUTF8StringBase64(toJSONString);

        //3.往银行存管系统发送数据(标的信息),根据结果修改状态并返回结果
            // url地址
            String url = configService.getDepositoryUrl() + "/service";
       return sendHttpGet("CREATE_PROJECT",url,reqDate,depositoryRecord);
    }


    /**
     * 投标预处理
     * @param userAutoPreTransactionRequest
     * @return
     */
    @Override
    public DepositoryResponseDTO<DepositoryBaseResponse> userAutoPreTransaction(UserAutoPreTransactionRequest userAutoPreTransactionRequest) {
        //1.保存交易记录（实现幂等性）
        DepositoryRecord depositoryRecord = new DepositoryRecord(userAutoPreTransactionRequest.getRequestNo(),userAutoPreTransactionRequest.getBizType(),"UserAutoPreTransactionRequest",userAutoPreTransactionRequest.getId());
            //实现幂等性
            DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = handleIdempotent(depositoryRecord);
            if (responseDTO != null){
                return responseDTO;
            }

        depositoryRecord = getEntityByRequestNo(userAutoPreTransactionRequest.getRequestNo());

        //2.签名
        String jsonString = JSON.toJSONString(depositoryRecord);
        String reqData = EncryptUtil.encodeUTF8StringBase64(jsonString);

        //3.发送数据到银行存管系统
        String url = configService.getDepositoryUrl() + "/service";
        return sendHttpGet("USER_AUTO_PRE_TRANSACTION", url, reqData, depositoryRecord);
    }







    /**
     * 审核满标放款
     * @param loanRequest
     * @return
     */
    @Override
    public DepositoryResponseDTO<DepositoryBaseResponse> confirmLoan(LoanRequest loanRequest) {
        DepositoryRecord depositoryRecord = new DepositoryRecord(loanRequest.getRequestNo(),DepositoryRequestTypeCode.FULL_LOAN.getCode(),"loanRequest",loanRequest.getId());
        //幂等性实现
        DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = handleIdempotent(depositoryRecord);
        if (responseDTO != null){
            return responseDTO;
        }

        //获取最新记录
        depositoryRecord = getEntityByRequestNo(loanRequest.getRequestNo());

        //准备签名
        String jsonString = JSON.toJSONString(loanRequest);
        String reqData = EncryptUtil.encodeUTF8StringBase64(jsonString);


        //发送数据到银行存管系统
        String url = configService.getDepositoryUrl() + "/service";
        return sendHttpGet("CONF IRM_LOAN", url, reqData, depositoryRecord);
    }





    /**
     * 修改标的状态为：还款中
     * @param modifyProjectStatusDTO
     * @return
     */
    @Override
    public DepositoryResponseDTO<DepositoryBaseResponse> modifyProjectStatus(ModifyProjectStatusDTO modifyProjectStatusDTO) {
        DepositoryRecord depositoryRecord = new DepositoryRecord(modifyProjectStatusDTO.getRequestNo(),DepositoryRequestTypeCode.MODIFY_STATUS.getCode(),"Project",modifyProjectStatusDTO.getId());
        //实现幂等性
        DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = handleIdempotent(depositoryRecord);
        if (responseDTO != null){
            return responseDTO;
        }
        depositoryRecord = getEntityByRequestNo(modifyProjectStatusDTO.getRequestNo()); //获取最新数据

        //准备签名
        //转为Json进行数据签名
        String jsonString = JSON.toJSONString(modifyProjectStatusDTO);

        //业务数据报文
        String reqData = EncryptUtil.encodeUTF8StringBase64(jsonString);

        //发送数据到银行存管系统
        String url = configService.getDepositoryUrl() + "/service";
        return sendHttpGet("MODIFY_PRO JECT", url, reqData, depositoryRecord);
    }







    /**
     * 还款确认
     * @param repaymentRequest
     * @return
     */
    @Override
    public DepositoryResponseDTO<DepositoryBaseResponse> confirmRepayment(RepaymentRequest repaymentRequest) {
        //1.构造交易记录
        DepositoryRecord depositoryRecord = new DepositoryRecord(repaymentRequest.getRequestNo(), PreprocessBusinessTypeCode.REPAYMENT.getCode(),"Repayment",repaymentRequest.getId());

        //2.实现幂等性
        DepositoryResponseDTO<DepositoryBaseResponse> responseDTO = handleIdempotent(depositoryRecord);
        if (responseDTO != null){
            return responseDTO;
        }

        //3.查询最新交易记录
        depositoryRecord = getEntityByRequestNo(repaymentRequest.getRequestNo());

        //4.请求银行存管系统进行还款
        String jsonString = JSON.toJSONString(repaymentRequest);
        //业务数据报文 base64处理
        String reqData = EncryptUtil.encodeUTF8StringBase64(jsonString);
        //拼接银行存管系统请求地址
        String url = configService.getDepositoryUrl() + "/service";
        //封装通用方法，请求银行存管系统
        return sendHttpGet("CONF IRM_REPAYMENT",url,reqData,depositoryRecord);

    }













    /**
     * 往银行存管服务发送 url 请求，并通过拦截器进行数据签名
     * @param serviceName
     * @param url
     * @param reqData
     * @param depositoryRecord
     * @return
     */
    private DepositoryResponseDTO<DepositoryBaseResponse> sendHttpGet(
            String serviceName, String url, String reqData,
            DepositoryRecord depositoryRecord){
        // 银行存管系统接收的4大参数: serviceName, platformNo, reqData, signature
        // signature会在okHttp拦截器(SignatureInterceptor)中处理
        // 平台编号
        String platformNo = configService.getP2pCode();
        // redData签名
        // 发送请求, 获取结果, 如果检验签名失败, 拦截器会在结果中放入: "signature", "false"
        String responseBody = okHttpService
                .doSyncGet(url + "?serviceName=" + serviceName + "&platformNo=" +
                        platformNo + "&reqData=" + reqData);
        DepositoryResponseDTO<DepositoryBaseResponse> depositoryResponse = JSON
                .parseObject(responseBody,
                        new TypeReference<DepositoryResponseDTO<DepositoryBaseResponse>>(){});

        //封装返回的处理结果
        depositoryRecord.setResponseData(responseBody);

        // 响应后, 根据结果更新数据库( 进行签名判断 )
        // 判断签名(signature)是为 false, 如果是说明验签失败!
        if (!"false".equals(depositoryResponse.getSignature())) {
            // 成功 - 设置数据同步状态
            depositoryRecord.setRequestStatus(StatusCode.STATUS_IN.getCode());
            // 设置消息确认时间
            depositoryRecord.setConfirmDate(LocalDateTime.now());
            // 更新数据库
            updateById(depositoryRecord);
        } else {
            // 失败 - 设置数据同步状态
            depositoryRecord.setRequestStatus(StatusCode.STATUS_FAIL.getCode());
            // 设置消息确认时间
            depositoryRecord.setConfirmDate(LocalDateTime.now());
            // 更新数据库
            updateById(depositoryRecord);
            // 抛业务异常
            throw new BusinessException(DepositoryErrorCode.E_160101);
        }
        return depositoryResponse;
    }






    /**
     * 保存交易记录
     * @param consumerRequest
     */
    private void saveDepositoryRecord(ConsumerRequest consumerRequest){
        DepositoryRecord record = new DepositoryRecord();

        record.setRequestNo(consumerRequest.getRequestNo());
        record.setRequestType(DepositoryRequestTypeCode.CONSUMER_CREATE.getCode());
        record.setObjectType("Consumer");
        record.setObjectId(consumerRequest.getId());
        record.setCreateDate(LocalDateTime.now());
       // record.setRequestStatus(StatusCode.STATUS_OUT.getCode());

         save(record);
    }





    /**
     * 保存交易记录
     * @param requestNo
     * @param requestType
     * @param objectType
     * @param objectId
     * @return
     */
    private DepositoryRecord saveDepositoryRecord(String requestNo,
                                                  String requestType,
                                                  String objectType,
                                                  Long objectId){
        DepositoryRecord depositoryRecord = new DepositoryRecord();
        //设置请求流水号
        depositoryRecord.setRequestNo(requestNo);

        //设置请求类型
        depositoryRecord.setObjectType(requestType);

        //设置关联业务实体类型
        depositoryRecord.setObjectId(objectId);

        //设置关联业务实体标识
        depositoryRecord.setObjectType(objectType);

        //设置请求时间
        depositoryRecord.setCreateDate(LocalDateTime.now());

        //设置数据同步状态
        depositoryRecord.setRequestStatus(StatusCode.STATUS_OUT.getCode());

        //保存数据
        save(depositoryRecord);
        return depositoryRecord;
    }





    /**
     * 保存交易记录
     * @return
     */
    private DepositoryRecord saveDepositoryRecord(DepositoryRecord depositoryRecord){

        //设置请求时间
        depositoryRecord.setCreateDate(LocalDateTime.now());

        //设置数据同步状态
        depositoryRecord.setRequestStatus(StatusCode.STATUS_OUT.getCode());

        //保存数据
        save(depositoryRecord);
        return depositoryRecord;
    }





    /**
     * ProjectDTO 对象 转换为 ProjectRequestDataDTO 对象
     * @param projectDTO
     * @param requestNo
     * @return
     */
    private ProjectRequestDataDTO concertProjectDTOToProjectRequestDataDTO(ProjectDTO projectDTO,String requestNo){
        if (projectDTO == null){
            return null;
        }
        ProjectRequestDataDTO requestDataDTO = new ProjectRequestDataDTO();

        BeanUtils.copyProperties(projectDTO,requestDataDTO);
        return requestDataDTO;
    }






    /**
     * 实现幂等性
     * @param depositoryRecord
     * @return
     */
    private DepositoryResponseDTO<DepositoryBaseResponse> handleIdempotent(DepositoryRecord depositoryRecord){
        //根据requestNo进行查询
        String requestNo = depositoryRecord.getRequestNo();
        DepositoryRecordDTO depositoryRecordDTO = getByRequestNo(requestNo);

        //1.交易记录不存在,保存交易记录
        if (depositoryRecordDTO == null){
            saveDepositoryRecord(depositoryRecord);
            return null;
        }

        //2.重复点击，重复请求，利用redis的原子性，争夺执行权
        if (StatusCode.STATUS_OUT.getCode() == depositoryRecord.getRequestStatus()){
            //如果requestNo不存在则返回1，如果已经存在，则会返回 (requestNo已存在的个数 + 1)
            Long count = redisCache.incrBy(requestNo, 1L);
            if (count == 1){
                redisCache.expire(requestNo,5); //设置requestNo 有效期为5秒
                return null;
            }
            //若 count 大于1，说明已经有线程在执行该操作，直接返回"正在处理"异常
            if (count > 1){
                throw  new BusinessException(DepositoryErrorCode.E_160103);
            }
        }


        //3. 交易记录已经存在，并且状态是“已同步”
        return JSON.parseObject(depositoryRecordDTO.getResponseData(),
                new TypeReference<DepositoryResponseDTO<DepositoryBaseResponse>>(){
                });
    }





    private DepositoryRecordDTO getByRequestNo(String requestNo) {
        DepositoryRecord depositoryRecord = getEntityByRequestNo(requestNo);
        if (depositoryRecord == null) {
            return null;
        }
        DepositoryRecordDTO depositoryRecordDTO = new DepositoryRecordDTO();
        BeanUtils.copyProperties(depositoryRecord, depositoryRecordDTO);
        return depositoryRecordDTO;
    }
    private DepositoryRecord getEntityByRequestNo(String requestNo) {
        return getOne(new QueryWrapper<DepositoryRecord>().lambda()
                .eq(DepositoryRecord::getRequestNo, requestNo));
    }


}
