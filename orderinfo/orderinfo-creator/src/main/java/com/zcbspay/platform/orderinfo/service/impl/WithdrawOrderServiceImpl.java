/* 
 * WithdrawOrderServiceImpl.java  
 * 
 * version TODO
 *
 * 2016年11月14日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.service.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.zcbspay.platform.member.coopinsti.service.CoopInstiProductService;
import com.zcbspay.platform.member.coopinsti.service.CoopInstiService;
import com.zcbspay.platform.member.individual.bean.QuickpayCustBean;
import com.zcbspay.platform.member.individual.service.MemberBankCardService;
import com.zcbspay.platform.member.merchant.bean.MerchantBean;
import com.zcbspay.platform.member.merchant.service.MerchService;
import com.zcbspay.platform.orderinfo.bean.OrderBean;
import com.zcbspay.platform.orderinfo.bean.WithdrawAccBean;
import com.zcbspay.platform.orderinfo.bean.WithdrawBean;
import com.zcbspay.platform.orderinfo.dao.TxncodeDefDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsLogDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsOrderinfoDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsWithdrawDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxncodeDef;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsLog;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsOrderinfo;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsWithdraw;
import com.zcbspay.platform.orderinfo.enums.BusiTypeEnum;
import com.zcbspay.platform.orderinfo.exception.CommonException;
import com.zcbspay.platform.orderinfo.exception.WithdrawOrderException;
import com.zcbspay.platform.orderinfo.sequence.SerialNumberService;
import com.zcbspay.platform.orderinfo.service.CommonOrderService;
import com.zcbspay.platform.orderinfo.service.WithdrawOrderService;
import com.zcbspay.platform.orderinfo.utils.BeanCopyUtil;
import com.zcbspay.platform.orderinfo.utils.Constant;
import com.zcbspay.platform.orderinfo.utils.DateUtil;
import com.zcbspay.platform.support.fee.bean.FeeBean;
import com.zcbspay.platform.support.fee.exception.TradeFeeException;
import com.zcbspay.platform.support.fee.service.TradeFeeService;
import com.zcbspay.platform.support.risk.bean.RiskBean;
import com.zcbspay.platform.support.risk.exception.TradeRiskException;
import com.zcbspay.platform.support.risk.service.TradeRiskControlService;
import com.zcbspay.platform.support.trade.acc.bean.ResultBean;
import com.zcbspay.platform.support.trade.acc.service.WithdrawAccountingService;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年11月14日 下午3:28:55
 * @since 
 */
@Deprecated
public class WithdrawOrderServiceImpl implements WithdrawOrderService {
	
	@Autowired
	private MemberBankCardService memberBankCardService;
	@Autowired
	private CommonOrderService commonOrderService;
	@Autowired
	private TxncodeDefDAO txncodeDefDAO;
	@Reference(version="1.0")
	private MerchService merchService;
	@Reference(version="1.0")
	private CoopInstiProductService coopInstiProductService;
	@Autowired
	private SerialNumberService serialNumberService;
	@Autowired
	private TxnsLogDAO txnsLogDAO;
	@Reference(version="1.0")
	private CoopInstiService coopInstiService;
	@Autowired
	private TxnsWithdrawDAO txnsWithdrawDAO;
	@Reference(version="1.0")
	private WithdrawAccountingService withdrawAccountingService;
	@Reference(version="1.0")
	private TradeRiskControlService tradeRiskControlService;
	@Reference(version="1.0")
	private TradeFeeService tradeFeeService;
	@Autowired
	private TxnsOrderinfoDAO txnsOrderinfoDAO;
	/**
	 *
	 * @param withdrawBean
	 * @return
	 * @throws CommonException 
	 */
	@Override
	public String createIndividualWithdrawOrder(WithdrawBean withdrawBean) throws WithdrawOrderException, CommonException {
		WithdrawAccBean accBean = null;
		if (StringUtils.isNotEmpty(withdrawBean.getBindId())) {// 使用已绑定的卡进行提现
			QuickpayCustBean custCard = memberBankCardService.getMemberBankCardById(Long.valueOf(withdrawBean.getBindId()));
			if (custCard == null) {
				throw new WithdrawOrderException("OD039");
			}
			accBean = new WithdrawAccBean(custCard);
		} else {
			accBean = JSON.parseObject(withdrawBean.getCardData(),WithdrawAccBean.class);
		}
		if (accBean == null) {
			throw new WithdrawOrderException("OD040");
		}
		
		commonOrderService.verifyRepeatWithdrawOrder(withdrawBean);
		
		commonOrderService.verifyBusiness(BeanCopyUtil.copyBean(OrderBean.class, withdrawBean),BusiTypeEnum.withdrawal);
		
		//commonOrderService.verifyMerchantAndCoopInsti(withdrawBean.getMerId(), withdrawBean.getCoopInstiId());
		
		commonOrderService.checkBusiAcctOfWithdraw(withdrawBean.getMemberId(),withdrawBean.getAmount());
		
		try {
			return saveWithdrawOrder(withdrawBean,accBean);
		}catch(TradeRiskException e){
			throw new WithdrawOrderException("OD037");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new WithdrawOrderException("OD038");
		}
	}

	/**
	 * @param withdrawBean
	 * @param accBean
	 * @throws WithdrawOrderException 
	 * @throws TradeRiskException 
	 */
	public String saveWithdrawOrder(WithdrawBean withdrawBean,
			WithdrawAccBean accBean) throws WithdrawOrderException, TradeRiskException {
		// TODO Auto-generated method stub
		// 记录订单信息
		PojoTxnsOrderinfo orderinfo = null;
		PojoTxnsLog txnsLog = null;
		
		PojoTxncodeDef busiModel = txncodeDefDAO.getBusiCode(
				withdrawBean.getTxnType(), withdrawBean.getTxnSubType(),
				withdrawBean.getBizType());
		// member = memberService.get(withdrawBean.getCoopInstiId());
		txnsLog = new PojoTxnsLog();
		if (StringUtils.isNotEmpty(withdrawBean.getMerId())) {// 商户为空时，取商户的各个版本信息
			MerchantBean member = merchService.getMerchBymemberId(withdrawBean
					.getMerId());
			txnsLog.setRiskver(member.getRiskVer());
			txnsLog.setSplitver(member.getSpiltVer());
			txnsLog.setFeever(member.getFeeVer());
			txnsLog.setPrdtver(member.getPrdtVer());
			txnsLog.setRoutver(member.getRoutVer());
			txnsLog.setAccsettledate(DateUtil.getSettleDate(Integer
					.valueOf(member.getSetlCycle().toString())));
		} else {
			txnsLog.setRiskver(coopInstiProductService.getDefaultVerInfo(
					withdrawBean.getCoopInstiId(), busiModel.getBusicode(),
					13));
			txnsLog.setSplitver(coopInstiProductService.getDefaultVerInfo(
					withdrawBean.getCoopInstiId(), busiModel.getBusicode(),
					12));
			txnsLog.setFeever(coopInstiProductService.getDefaultVerInfo(
					withdrawBean.getCoopInstiId(), busiModel.getBusicode(),
					11));
			txnsLog.setPrdtver(coopInstiProductService.getDefaultVerInfo(
					withdrawBean.getCoopInstiId(), busiModel.getBusicode(),
					10));
			txnsLog.setRoutver(coopInstiProductService.getDefaultVerInfo(
					withdrawBean.getCoopInstiId(), busiModel.getBusicode(),
					20));
			txnsLog.setAccsettledate(DateUtil.getSettleDate(1));
		}

		txnsLog.setTxndate(DateUtil.getCurrentDate());
		txnsLog.setTxntime(DateUtil.getCurrentTime());
		txnsLog.setBusicode(busiModel.getBusicode());
		txnsLog.setBusitype(busiModel.getBusitype());
		txnsLog.setTxnseqno(serialNumberService.generateTxnseqno());
		txnsLog.setAmount(Long.valueOf(withdrawBean.getAmount()));
		txnsLog.setAccordno(withdrawBean.getOrderId());
		txnsLog.setAccfirmerno(withdrawBean.getCoopInstiId());
		// 提现订单不记录商户号，记录在订单表中
		if ("3000".equals(txnsLog.getBusitype())) {
			txnsLog.setAccsecmerno("");
		} else {
			txnsLog.setAccsecmerno(withdrawBean.getMerId());
		}
		txnsLog.setAcccoopinstino(Constant.getInstance().getZlebank_coopinsti_code());
		txnsLog.setAccordcommitime(withdrawBean.getTxnTime());
		txnsLog.setTradestatflag("00000000");// 交易初始状态
		txnsLog.setAccmemberid(withdrawBean.getMemberId());
		txnsLog.setPan(accBean.getAccNo());
		txnsLog.setPanName(accBean.getAccName());
		txnsLog.setCardtype("1");
		txnsLog.setCardinstino(accBean.getBankCode());
		//txnsLog.setTxnfee(txnsLogService.getTxnFee(txnsLog));
		long fee = 0;
		try {
			FeeBean feeBean = new FeeBean();
			feeBean.setBusiCode(txnsLog.getBusicode());
			feeBean.setFeeVer(txnsLog.getFeever());
			feeBean.setTxnAmt(txnsLog.getAmount()+"");
			feeBean.setMerchNo(txnsLog.getAccsecmerno());
			feeBean.setCardType("1");
			feeBean.setTxnseqnoOg("");
			feeBean.setTxnseqno(txnsLog.getTxnseqno());
			fee = tradeFeeService.getCommonFee(feeBean);
		} catch (TradeFeeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new WithdrawOrderException("OD043");
		}
		txnsLog.setTxnfee(fee);
		txnsLog.setTradcomm(0L);
		txnsLogDAO.saveTxnsLog(txnsLog);

		

		
		orderinfo = new PojoTxnsOrderinfo();
		orderinfo.setId(Long.valueOf(RandomUtils.nextInt()));
		orderinfo.setOrderno(withdrawBean.getOrderId());// 商户提交的订单号
		orderinfo.setOrderamt(Long.valueOf(withdrawBean.getAmount()));
		orderinfo.setOrderfee(txnsLog.getTxnfee());
		orderinfo.setOrdercommitime(withdrawBean.getTxnTime());
		orderinfo.setRelatetradetxn(txnsLog.getTxnseqno());// 关联的交易流水表中的交易序列号
		orderinfo.setFirmemberno(withdrawBean.getCoopInstiId());
		orderinfo.setFirmembername(coopInstiService.getInstiByInstiCode(
				withdrawBean.getCoopInstiId()).getInstiName());

		//orderinfo.setBackurl(withdrawBean.getBackUrl());
		orderinfo.setTxntype(withdrawBean.getTxnType());
		orderinfo.setTxnsubtype(withdrawBean.getTxnSubType());
		orderinfo.setBiztype(withdrawBean.getBizType());
		orderinfo.setAccesstype(withdrawBean.getAccessType());
		orderinfo.setTn(serialNumberService.generateTN(withdrawBean.getMemberId()));
		orderinfo.setMemberid(withdrawBean.getMemberId());
		orderinfo.setCurrencycode("156");
		orderinfo.setStatus("02");
		txnsOrderinfoDAO.saveOrderInfo(orderinfo);
	
	
		PojoTxnsWithdraw withdraw = new PojoTxnsWithdraw(withdrawBean,accBean);
		withdraw.setWithdraworderno(serialNumberService.generateWithdrawNo());
		withdraw.setTexnseqno(txnsLog.getTxnseqno());
		withdraw.setFee(txnsLog.getTxnfee());
		txnsWithdrawDAO.saveTxnsWithdraw(withdraw);
		// 风控
		RiskBean riskBean = new RiskBean();
		riskBean.setBusiCode(txnsLog.getBusicode());
		riskBean.setCardNo(accBean.getAccNo());
		riskBean.setCardType("1");
		riskBean.setCoopInstId(txnsLog.getAccfirmerno());
		riskBean.setMemberId(txnsLog.getAccmemberid());
		riskBean.setMerchId(txnsLog.getAccsecmerno());
		riskBean.setTxnAmt(txnsLog.getAmount()+"");
		riskBean.setTxnseqno(txnsLog.getTxnseqno());
		tradeRiskControlService.realTimeTradeRiskControl(riskBean);
		ResultBean resultBean = withdrawAccountingService.withdrawApply(txnsLog.getTxnseqno());
		
		
		if(resultBean.isResultBool()){
			return orderinfo.getTn();
		}else{
			throw new WithdrawOrderException("OD044");
		}
		
		
		/*txnsLogService.tradeRiskControl(txnsLog.getTxnseqno(),
				txnsLog.getAccfirmerno(), txnsLog.getAccsecmerno(),
				txnsLog.getAccmemberid(), txnsLog.getBusicode(),
				txnsLog.getAmount() + "", "1", withdraw.getAcctno());
		txnsOrderinfoDAO.saveOrderInfo(orderinfo);*/
			
		/*// 提现账务处理
		TradeInfo tradeInfo = new TradeInfo();
		tradeInfo.setPayMemberId(txnsLog.getAccmemberid());
		tradeInfo.setPayToMemberId(txnsLog.getAccmemberid());
		tradeInfo.setAmount(new BigDecimal(txnsLog.getAmount()));
		tradeInfo
				.setCharge(new BigDecimal(
						txnsLog.getTxnfee() == null ? 0L : txnsLog
								.getTxnfee()));
		tradeInfo.setTxnseqno(txnsLog.getTxnseqno());
		tradeInfo.setBusiCode(BusinessCodeEnum.WITHDRAWALS
				.getBusiCode());
		tradeInfo.setAccess_coopInstCode(txnsLog.getAccfirmerno());
		tradeInfo.setCoopInstCode(txnsLog.getAcccoopinstino());
		// 记录分录流水
		accEntryService.accEntryProcess(tradeInfo,
				EntryEvent.AUDIT_APPLY);*/
			

		
		
	}

}
