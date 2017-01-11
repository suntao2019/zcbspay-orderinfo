/* 
 * InsteadPayOrderServiceImpl.java  
 * 
 * version TODO
 *
 * 2016年10月20日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.service.impl;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zcbspay.platform.member.coopinsti.service.CoopInstiProductService;
import com.zcbspay.platform.member.coopinsti.service.CoopInstiService;
import com.zcbspay.platform.member.individual.service.MemberAccountService;
import com.zcbspay.platform.member.individual.service.MemberService;
import com.zcbspay.platform.member.merchant.bean.EnterpriseBean;
import com.zcbspay.platform.member.merchant.bean.MerchantBean;
import com.zcbspay.platform.member.merchant.service.EnterpriseService;
import com.zcbspay.platform.member.merchant.service.MerchService;
import com.zcbspay.platform.orderinfo.bean.CardBin;
import com.zcbspay.platform.orderinfo.bean.InsteadPayOrderBean;
import com.zcbspay.platform.orderinfo.bean.OrderBean;
import com.zcbspay.platform.orderinfo.common.bean.ResultBean;
import com.zcbspay.platform.orderinfo.consumer.enums.TradeStatFlagEnum;
import com.zcbspay.platform.orderinfo.dao.InsteadPayRealtimeDAO;
import com.zcbspay.platform.orderinfo.dao.TxncodeDefDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsLogDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoInsteadPayRealtime;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxncodeDef;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsLog;
import com.zcbspay.platform.orderinfo.enums.AccountTypeEnum;
import com.zcbspay.platform.orderinfo.enums.BusiTypeEnum;
import com.zcbspay.platform.orderinfo.enums.CertifTypeEnmu;
import com.zcbspay.platform.orderinfo.enums.CurrencyEnum;
import com.zcbspay.platform.orderinfo.exception.CommonException;
import com.zcbspay.platform.orderinfo.exception.InsteadPayOrderException;
import com.zcbspay.platform.orderinfo.sequence.SerialNumberService;
import com.zcbspay.platform.orderinfo.service.CommonOrderService;
import com.zcbspay.platform.orderinfo.service.InsteadPayOrderService;
import com.zcbspay.platform.orderinfo.utils.BeanCopyUtil;
import com.zcbspay.platform.orderinfo.utils.Constant;
import com.zcbspay.platform.orderinfo.utils.DateUtil;
import com.zcbspay.platform.orderinfo.utils.ValidateLocator;
import com.zcbspay.platform.support.fee.bean.FeeBean;
import com.zcbspay.platform.support.fee.exception.TradeFeeException;
import com.zcbspay.platform.support.fee.service.TradeFeeService;
import com.zcbspay.platform.support.trade.acc.service.InsteadPayAccountingService;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年10月20日 上午9:25:11
 * @since 
 */
@Service("insteadPayOrderService")
public class InsteadPayOrderServiceImpl implements InsteadPayOrderService {

	//private static final Logger log = LoggerFactory.getLogger(InsteadPayOrderServiceImpl.class);
	@Autowired
	private CommonOrderService commonOrderService;
	@Autowired
	private TxncodeDefDAO txncodeDefDAO;
	@Reference(version="1.0")
	private CoopInstiService coopInstiService ;
	@Reference(version="1.0")
	private MemberService memberService;
	@Reference(version="1.0")
	private MemberAccountService memberAccountService;
	
	@Autowired
	private InsteadPayRealtimeDAO insteadPayRealtimeDAO;
	@Autowired
	private SerialNumberService serialNumberService;
	@Reference(version="1.0")
	private MerchService merchService;
	@Reference(version="1.0")
	private CoopInstiProductService coopInstiProductService;
	@Reference(version="1.0")
	private EnterpriseService enterpriseService;
	@Autowired
	private TxnsLogDAO txnsLogDAO;
	@Reference(version="1.0")
	private InsteadPayAccountingService insteadPayAccountingService;
	@Reference(version="1.0")
	private TradeFeeService tradeFeeService;
	/**
	 *
	 * @param insteadPayOrderBean
	 * @return
	 * @throws InsteadPayOrderException 
	 * @throws CommonException 
	 */
	@Override
	public ResultBean createRealTimeOrder(InsteadPayOrderBean insteadPayOrderBean) throws InsteadPayOrderException, CommonException {
		
		/**
		 * 实时代付订单校验流程
		 * 0。二次代付校验,校验订单是否为二次代付，校验准则：商户号，订单号，收款账户，收款账号，订单时间,金额必须完全一致
		 * 1.检查代付时间
		 * 2.校验业务类型
		 * 3.校验商户和合作机构
		 * 4.检查代付业务资金账户状态
		 * 5.代付扣款账务处理
		 * 6.保存订单信息
		 */
		ResultBean resultBean = null;
		String tn = commonOrderService.verifySecondInsteadPay(insteadPayOrderBean);
		if(StringUtils.isNotEmpty(tn)){
			resultBean = new ResultBean(tn);
			return resultBean;
		}
		checkOrderInfo(insteadPayOrderBean);
		
		
		String txnseqno = serialNumberService.generateTxnseqno();
		String TN = serialNumberService.generateTN(insteadPayOrderBean.getMerId());
		PojoInsteadPayRealtime insteadPayRealtime = generateInsteadPayOrder(insteadPayOrderBean);
		insteadPayRealtime.setTxnseqno(txnseqno);
		insteadPayRealtime.setTn(TN);
		PojoTxnsLog txnsLog = generateTxnsLog(insteadPayOrderBean);
		txnsLog.setTxnseqno(txnseqno);
		insteadPayRealtimeDAO.saveInsteadTrade(insteadPayRealtime);
		//计算交易手续费
		try {
			FeeBean feeBean = new FeeBean();
			feeBean.setBusiCode(txnsLog.getBusicode());
			feeBean.setFeeVer(txnsLog.getFeever());
			feeBean.setTxnAmt(txnsLog.getAmount()+"");
			feeBean.setMerchNo(txnsLog.getAccsecmerno());
			feeBean.setCardType(insteadPayOrderBean.getCardType());
			feeBean.setTxnseqnoOg("");
			feeBean.setTxnseqno(txnseqno);
			long fee = tradeFeeService.getCommonFee(feeBean);
			txnsLog.setTxnfee(fee);
		} catch (TradeFeeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InsteadPayOrderException("OD048");
		}
		txnsLogDAO.saveTxnsLog(txnsLog);
		com.zcbspay.platform.support.trade.acc.bean.ResultBean paymentAccountingResult = insteadPayAccountingService.advancePaymentAccounting(txnseqno);
		if(!paymentAccountingResult.isResultBool()){
			throw new InsteadPayOrderException();
		}
		return new ResultBean(TN);
	}
	
	private PojoInsteadPayRealtime generateInsteadPayOrder(InsteadPayOrderBean insteadPayOrderBean){
		PojoInsteadPayRealtime insteadBean = new PojoInsteadPayRealtime();
		 //授理订单
		
		 insteadBean.setOrderno(insteadPayOrderBean.getOrderId());
		 insteadBean.setOrderCommiTime(insteadPayOrderBean.getTxnTime());
		 insteadBean.setOrderDesc("");
		 //付款人信息
		 insteadBean.setPayAccNo(insteadPayOrderBean.getAccNo());
		 insteadBean.setPayAccName(insteadPayOrderBean.getAccName());
		 insteadBean.setPayBankType(insteadPayOrderBean.getBankCode());
		 insteadBean.setPayBankName("");
		 //收款人信息
		 insteadBean.setAccType(insteadPayOrderBean.getAccType());
		 insteadBean.setAccName(insteadPayOrderBean.getAccName());
		 insteadBean.setAccNo(insteadPayOrderBean.getAccNo());
		 insteadBean.setCertifyType(insteadPayOrderBean.getCertifTp());
		 insteadBean.setCertifyNo(insteadPayOrderBean.getCertifId());
		 insteadBean.setMobile(insteadPayOrderBean.getPhoneNo());
		 insteadBean.setBankType(insteadPayOrderBean.getBankCode());
		 insteadBean.setBankName(insteadPayOrderBean.getIssInsName());
		 insteadBean.setProvince(insteadPayOrderBean.getIssInsProvince());
		 insteadBean.setCity(insteadPayOrderBean.getIssInsCity());
		 insteadBean.setTransAmt(Long.valueOf(insteadPayOrderBean.getTxnAmt()));
		 insteadBean.setCurrencyCode(insteadPayOrderBean.getCurrencyCode());
		 insteadBean.setRemark(insteadPayOrderBean.getNotes());
		 //其它
		 insteadBean.setTxnType(insteadPayOrderBean.getTxnType());
		 insteadBean.setTxnSubType(insteadPayOrderBean.getTxnSubType());
		 insteadBean.setBizType(insteadPayOrderBean.getBizType());
		 insteadBean.setBackUrl(insteadPayOrderBean.getBackUrl());
		 insteadBean.setFrontUrl("");
		 //insteadBean.setTxnFee(txnsLog.getTxnfee());
		 //insteadBean.setTxnseqno(txnsLog.getTxnseqno());
		 //商户
		 insteadBean.setMerId(insteadPayOrderBean.getMerId());
		 insteadBean.setCoopInstCode(insteadPayOrderBean.getCoopInstiId());
		 insteadBean.setCreateTime(new Date());
		 String enterpriseName="";
		if(StringUtils.isNotEmpty(insteadPayOrderBean.getMerId())){
			 EnterpriseBean enter= enterpriseService.getEnterpriseByMemberId(insteadPayOrderBean.getMerId());
			 enterpriseName=enter.getEnterpriseName();
		}
		 insteadBean.setMerName(enterpriseName);
		 insteadBean.setMerNameAbbr(null);
		 //合作机构
		 insteadBean.setCoopInstCode(insteadPayOrderBean.getCoopInstiId());
		 insteadBean.setStatus("01");
		 //状态
		 return insteadBean;
	}
	
	private PojoTxnsLog generateTxnsLog(InsteadPayOrderBean insteadPayOrderBean) throws InsteadPayOrderException{
		MerchantBean member = merchService.getMerchBymemberId(insteadPayOrderBean.getMerId());
		PojoTxnsLog txnsLog = new PojoTxnsLog();
		PojoTxncodeDef busiModel = txncodeDefDAO.getBusiCode(
				insteadPayOrderBean.getTxnType(), insteadPayOrderBean.getTxnSubType(),
				insteadPayOrderBean.getBizType());
		if(member!=null){
			txnsLog.setRiskver(member.getRiskVer());
			txnsLog.setSplitver(member.getSpiltVer());
			txnsLog.setFeever(member.getFeeVer());
			txnsLog.setPrdtver(member.getPrdtVer());
			txnsLog.setRoutver(member.getRoutVer());
			txnsLog.setAccsettledate(DateUtil.getSettleDate(Integer
					.valueOf(member.getSetlCycle().toString())));
		}else{
			// 10-产品版本,11-扣率版本,12-分润版本,13-风控版本,20-路由版本
			txnsLog.setRiskver(coopInstiProductService.getDefaultVerInfo(
					insteadPayOrderBean.getCoopInstiId(), busiModel.getBusicode(), 13));
			txnsLog.setSplitver(coopInstiProductService.getDefaultVerInfo(
					insteadPayOrderBean.getCoopInstiId(), busiModel.getBusicode(), 12));
			txnsLog.setFeever(coopInstiProductService.getDefaultVerInfo(
					insteadPayOrderBean.getCoopInstiId(), busiModel.getBusicode(), 11));
			txnsLog.setPrdtver(coopInstiProductService.getDefaultVerInfo(
					insteadPayOrderBean.getCoopInstiId(), busiModel.getBusicode(), 10));
			txnsLog.setRoutver(coopInstiProductService.getDefaultVerInfo(
					insteadPayOrderBean.getCoopInstiId(), busiModel.getBusicode(), 20));
			txnsLog.setAccsettledate(DateUtil.getSettleDate(1));
		}
		
		txnsLog.setAccsettledate(DateUtil.getSettleDate(1));
		txnsLog.setTxndate(DateUtil.getCurrentDate());
		txnsLog.setTxntime(DateUtil.getCurrentTime());
		txnsLog.setBusicode(busiModel.getBusicode());
		//代付
		txnsLog.setBusitype(busiModel.getBusitype());
		// 核心交易流水号，交易时间（yymmdd）+业务代码+6位流水号（每日从0开始）
		
		txnsLog.setAmount(Long.valueOf(insteadPayOrderBean.getTxnAmt()));
		txnsLog.setAccordno(insteadPayOrderBean.getOrderId());
		txnsLog.setAccfirmerno(insteadPayOrderBean.getCoopInstiId());
		txnsLog.setAcccoopinstino(Constant.getInstance().getZlebank_coopinsti_code());
		txnsLog.setAccsecmerno(insteadPayOrderBean.getMerId());
		txnsLog.setAccordcommitime(DateUtil.getCurrentDateTime());
		txnsLog.setTradestatflag(TradeStatFlagEnum.INITIAL.getStatus());// 交易初始状态
		txnsLog.setAccmemberid("999999999999999");
		//收款人户名
		txnsLog.setPanName(insteadPayOrderBean.getAccName());
		//收款人账号
		txnsLog.setPan(insteadPayOrderBean.getAccNo());
		//收款人账户联行号
		txnsLog.setCardinstino(insteadPayOrderBean.getBankCode_DB());
		//卡类型
		txnsLog.setCardtype(insteadPayOrderBean.getCardType());
		txnsLog.setTradcomm(0L);
		return txnsLog;
		
		
	}
	
	
	
	private ResultBean checkOrderInfo(InsteadPayOrderBean insteadPayOrderBean) throws InsteadPayOrderException, CommonException{
		ResultBean resultBean = validateInsteadPayOrder(insteadPayOrderBean);
		if(!resultBean.isResultBool()){
			return resultBean;
		}
		//检查
		commonOrderService.checkInsteadPayTime();
		commonOrderService.verifyBusiness(BeanCopyUtil.copyBean(OrderBean.class, insteadPayOrderBean), BusiTypeEnum.insteadPay);
		commonOrderService.validateBusiness(BeanCopyUtil.copyBean(OrderBean.class, insteadPayOrderBean));
		commonOrderService.verifyMerchantAndCoopInsti(insteadPayOrderBean.getMerId(), insteadPayOrderBean.getCoopInstiId());
		commonOrderService.checkBusiAcctOfInsteadPay(insteadPayOrderBean.getMerId(),insteadPayOrderBean.getTxnAmt());
		//校验账号
        if(insteadPayOrderBean.getAccType().equals(AccountTypeEnum.PRIVATE.getCode())){
        	CardBin cardBin = commonOrderService.checkInsteadPayCard(insteadPayOrderBean.getAccNo());
        	insteadPayOrderBean.setCardType(cardBin.getType());
        	insteadPayOrderBean.setBankCode_DB(cardBin.getBankCode());
        }
		return new ResultBean("success");
	}
	
	
	private ResultBean validateInsteadPayOrder(InsteadPayOrderBean insteadPayOrderBean) throws InsteadPayOrderException{
		ResultBean resultBean = ValidateLocator.validateBeans(insteadPayOrderBean);
		CurrencyEnum  rmb = CurrencyEnum.fromValue(insteadPayOrderBean.getCurrencyCode());
        if(rmb==null || rmb.equals(CurrencyEnum.UNKNOW)){
       	 	throw  new InsteadPayOrderException("OD025");//代付货币类型错误
        }
        AccountTypeEnum  accType = AccountTypeEnum.fromValue(insteadPayOrderBean.getAccType());
        if(accType==null || accType.equals(AccountTypeEnum.UNKNOW)){
        	throw  new InsteadPayOrderException("OD047");//代付账户类型错误
        }
        CertifTypeEnmu certype = CertifTypeEnmu.fromValue(insteadPayOrderBean.getCertifTp());
        if(certype==null || certype.equals(AccountTypeEnum.UNKNOW)){
        	throw  new InsteadPayOrderException("OD027");//代付证件类型错误
        }
		return resultBean;
	}

}
