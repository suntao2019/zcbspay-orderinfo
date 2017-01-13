/* 
 * RefundOrderServiceImpl.java  
 * 
 * version TODO
 *
 * 2016年11月11日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.service.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zcbspay.platform.member.coopinsti.service.CoopInstiProductService;
import com.zcbspay.platform.member.coopinsti.service.CoopInstiService;
import com.zcbspay.platform.member.merchant.bean.MerchantBean;
import com.zcbspay.platform.member.merchant.service.MerchService;
import com.zcbspay.platform.orderinfo.bean.OrderBean;
import com.zcbspay.platform.orderinfo.bean.RefundOrderBean;
import com.zcbspay.platform.orderinfo.dao.TxncodeDefDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsLogDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsOrderinfoDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsRefundDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxncodeDef;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsLog;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsOrderinfo;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsRefund;
import com.zcbspay.platform.orderinfo.enums.BusiTypeEnum;
import com.zcbspay.platform.orderinfo.enums.BusinessEnum;
import com.zcbspay.platform.orderinfo.exception.CommonException;
import com.zcbspay.platform.orderinfo.exception.RefundOrderException;
import com.zcbspay.platform.orderinfo.sequence.SerialNumberService;
import com.zcbspay.platform.orderinfo.service.CommonOrderService;
import com.zcbspay.platform.orderinfo.service.RefundOrderService;
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
import com.zcbspay.platform.support.trade.acc.service.RefundAccountingService;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年11月11日 下午1:58:55
 * @since
 */
@Deprecated
public class RefundOrderServiceImpl implements RefundOrderService {

	@Autowired
	private CommonOrderService commonOrderService;
	@Autowired
	private TxnsOrderinfoDAO txnsOrderinfoDAO;
	@Autowired
	private TxnsLogDAO txnsLogDAO;
	@Autowired
	private TxncodeDefDAO txncodeDefDAO;
	@Autowired
	private MerchService merchService;
	@Autowired
	private CoopInstiProductService coopInstiProductService;
	@Autowired
	private SerialNumberService serialNumberService;
	@Autowired
	private CoopInstiService coopInstiService;
	@Autowired
	private TxnsRefundDAO txnsRefundDAO;
	@Autowired
	private RefundAccountingService refundAccountingService;
	@Autowired
	private TradeRiskControlService tradeRiskControlService;
	@Autowired
	private TradeFeeService tradeFeeService;
	/**
	 *
	 * @param refundOrderBean
	 * @return
	 * @throws CommonException
	 */
	@Override
	public String createRefundOrder(RefundOrderBean refundOrderBean)
			throws RefundOrderException, CommonException {
		/**
		 * 1.退款检查,退款商户订单号和原受理订单号不可以同时为空 2.重复订单检查 3.业务检查 4.商户合作机构检查 5.检查商户账户状态和余额
		 * 6.校验原始交易订单
		 */
		if (StringUtils.isEmpty(refundOrderBean.getOrigOrderId())&& StringUtils.isEmpty(refundOrderBean.getOrigTN())) {
			throw new RefundOrderException("OD028");
		}
		commonOrderService.verifyRepeatRefundOrder(refundOrderBean);

		commonOrderService.verifyBusiness(BeanCopyUtil.copyBean(OrderBean.class, refundOrderBean),BusiTypeEnum.refund);

		commonOrderService.verifyMerchantAndCoopInsti(refundOrderBean.getMerId(), refundOrderBean.getCoopInstiId());

		commonOrderService.checkBusiAcctOfRefund(refundOrderBean.getMerId(),refundOrderBean.getTxnAmt());

		commonOrderService.checkOldOrder(refundOrderBean);

		
		try {
			return commonRefund(refundOrderBean);
		} catch (TradeRiskException e) {
			// TODO: handle exception
			throw new RefundOrderException("OD037");
		} catch (RefundOrderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RefundOrderException("OD036");
		}
	}

	public String commonRefund(RefundOrderBean orderBean)
			throws RefundOrderException, TradeRiskException {
		MerchantBean member = null;
		PojoTxnsLog txnsLog = null;
		PojoTxnsOrderinfo old_orderInfo = null;
		PojoTxnsLog old_txnsLog = null;

		old_orderInfo = txnsOrderinfoDAO.getOrderinfoByTN(orderBean
				.getOrigTN());
		old_txnsLog = txnsLogDAO.getTxnsLogByTxnseqno(old_orderInfo
				.getRelatetradetxn());
		PojoTxncodeDef busiModel = txncodeDefDAO.getBusiCode(
				orderBean.getTxnType(), orderBean.getTxnSubType(),
				orderBean.getBizType());
		if (busiModel == null) {
			throw new RefundOrderException("");
		}
		
		txnsLog = new PojoTxnsLog();
		if (StringUtils.isNotEmpty(orderBean.getMerId())) {// 商户为空时，取商户的各个版本信息
			member = merchService.getMerchBymemberId(orderBean.getMerId());
			txnsLog.setRiskver(member.getRiskVer());
			txnsLog.setSplitver(member.getSpiltVer());
			txnsLog.setFeever(member.getFeeVer());
			txnsLog.setPrdtver(member.getPrdtVer());
			// txnsLog.setCheckstandver(member.getCashver());
			txnsLog.setRoutver(member.getRoutVer());
			//txnsLog.setAccordinst(member.getParent() + "");
			txnsLog.setAccsettledate(DateUtil.getSettleDate(Integer
					.valueOf(member.getSetlCycle().toString())));
		} else {
			txnsLog.setRiskver(coopInstiProductService.getDefaultVerInfo(
					orderBean.getCoopInstiId(), busiModel.getBusicode(), 13));
			txnsLog.setSplitver(coopInstiProductService.getDefaultVerInfo(
					orderBean.getCoopInstiId(), busiModel.getBusicode(), 12));
			txnsLog.setFeever(coopInstiProductService.getDefaultVerInfo(
					orderBean.getCoopInstiId(), busiModel.getBusicode(), 11));
			txnsLog.setPrdtver(coopInstiProductService.getDefaultVerInfo(
					orderBean.getCoopInstiId(), busiModel.getBusicode(), 10));
			txnsLog.setRoutver(coopInstiProductService.getDefaultVerInfo(
					orderBean.getCoopInstiId(), busiModel.getBusicode(), 20));
			txnsLog.setAccsettledate(DateUtil.getSettleDate(1));
		}

		txnsLog.setTxndate(DateUtil.getCurrentDate());
		txnsLog.setTxntime(DateUtil.getCurrentTime());
		txnsLog.setBusicode(busiModel.getBusicode());
		txnsLog.setBusitype(busiModel.getBusitype());
		// 核心交易流水号，交易时间（yymmdd）+业务代码+6位流水号（每日从0开始）
		txnsLog.setTxnseqno(serialNumberService.generateTxnseqno());
		txnsLog.setAmount(Long.valueOf(orderBean.getTxnAmt()));
		txnsLog.setAccordno(orderBean.getOrderId());
		txnsLog.setAccfirmerno(orderBean.getCoopInstiId());
		txnsLog.setAccsecmerno(orderBean.getMerId());
		txnsLog.setAcccoopinstino(Constant.getInstance()
				.getZlebank_coopinsti_code());
		txnsLog.setTxnseqnoOg(old_txnsLog.getTxnseqno());
		txnsLog.setAccordcommitime(DateUtil.getCurrentDateTime());
		txnsLog.setTradestatflag("00000000");// 交易初始状态
		txnsLog.setAccsettledate(DateUtil.getSettleDate(Integer.valueOf(member
				.getSetlCycle().toString())));
		txnsLog.setAccmemberid(orderBean.getMemberId());

		// 匿名判断
		/*
		 * String payMember = old_txnsLog.getAccmemberid(); boolean anonFlag =
		 * false; if ("999999999999999".equals(payMember)) { anonFlag = true; }
		 */
		if (old_txnsLog.getPayinst().equals("99999999")) {
			txnsLog.setBusicode(BusinessEnum.REFUND_ACCOUNT.getBusiCode());
		} else {
			txnsLog.setBusicode(BusinessEnum.REFUND_BANK.getBusiCode());
		}

		// 原交易渠道号
		// String payChannelCode = old_txnsLog.getPayinst();
		// 原交易类型 1000002为账户余额支付
		// String accbusicode = old_txnsLog.getAccbusicode();
		// 退款路由选择退款渠道或者退款的方式
		/*
		 * ResultBean refundRoutResultBean = refundRouteConfigService
		 * .getTransRout(DateUtil.getCurrentDateTime(), txnsLog.getAmount() +
		 * "", "", accbusicode, txnsLog .getPan(), payChannelCode, anonFlag ?
		 * "1" : "0"); if (refundRoutResultBean.isResultBool()) {
		 * 
		 * String refundRout = refundRoutResultBean.getResultObj() .toString();
		 * if ("99999999".equals(refundRout)) {
		 * txnsLog.setBusicode(BusinessEnum.REFUND_ACCOUNT .getBusiCode()); }
		 * else { txnsLog.setBusicode(BusinessEnum.REFUND_BANK.getBusiCode()); }
		 * }
		 */
		txnsLog.setTradcomm(0L);
		
		
		long fee = 0;
		try {
			FeeBean feeBean = new FeeBean();
			feeBean.setBusiCode(BusinessEnum.REFUND_BANK.getBusiCode());
			feeBean.setFeeVer(txnsLog.getFeever());
			feeBean.setTxnAmt(txnsLog.getAmount()+"");
			feeBean.setMerchNo(txnsLog.getAccsecmerno());
			feeBean.setCardType("");
			feeBean.setTxnseqnoOg(txnsLog.getTxnseqnoOg());
			feeBean.setTxnseqno(txnsLog.getTxnseqno());
			fee = tradeFeeService.getRefundFee(feeBean);
		} catch (TradeFeeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RefundOrderException("OD042");
		}
		txnsLog.setTxnfee(fee);
		txnsLogDAO.saveTxnsLog(txnsLog);
		ResultBean resultBean = refundAccountingService
				.refundApply(txnsLog.getTxnseqno());
		if (!resultBean.isResultBool()) {
			throw new RefundOrderException("OD041");
		}

		String tn = "";
		PojoTxnsOrderinfo orderinfo = null;

		// 保存订单信息
		orderinfo = new PojoTxnsOrderinfo();
		orderinfo.setId(Long.valueOf(RandomUtils.nextInt()));
		// orderinfo.setInstitution(member.getMerchinsti());
		orderinfo.setOrderno(orderBean.getOrderId());// 商户提交的订单号
		orderinfo.setOrderamt(Long.valueOf(orderBean.getTxnAmt()));
		orderinfo.setOrdercommitime(orderBean.getTxnTime());
		orderinfo.setRelatetradetxn(txnsLog.getTxnseqno());// 关联的交易流水表中的交易序列号
		orderinfo.setFirmemberno(orderBean.getCoopInstiId());
		orderinfo.setFirmembername(coopInstiService.getInstiByInstiCode(
				orderBean.getCoopInstiId()).getInstiName());
		orderinfo.setSecmemberno(orderBean.getMerId());
		orderinfo.setSecmembername(member == null ? "" : member.getAccName());
		orderinfo.setBackurl("");
		orderinfo.setTxntype(orderBean.getTxnType());
		orderinfo.setTxnsubtype(orderBean.getTxnSubType());
		orderinfo.setBiztype(orderBean.getBizType());
		// orderinfo.setReqreserved(orderBean.getReqReserved());
		orderinfo.setOrderdesc(orderBean.getOrderDesc());
		// orderinfo.setAccesstype(orderBean.getAccessType());
		orderinfo.setTn(serialNumberService.generateTN(orderBean.getMerId()));
		orderinfo.setStatus("02");
		orderinfo.setMemberid(orderBean.getMemberId());
		orderinfo.setCurrencycode("156");
		// txnsLogDAO.tradeRiskControl(txnsLog.getTxnseqno(),txnsLog.getAccfirmerno(),txnsLog.getAccsecmerno(),txnsLog.getAccmemberid(),txnsLog.getBusicode(),txnsLog.getAmount()+"","1","");
		
		RiskBean riskBean = new RiskBean();
		riskBean.setBusiCode(txnsLog.getBusicode());
		riskBean.setCardNo("");
		riskBean.setCardType("1");
		riskBean.setCoopInstId(txnsLog.getAccfirmerno());
		riskBean.setMemberId(txnsLog.getAccmemberid());
		riskBean.setMerchId(txnsLog.getAccsecmerno());
		riskBean.setTxnAmt(txnsLog.getAmount()+"");
		riskBean.setTxnseqno(txnsLog.getTxnseqno());
		tradeRiskControlService.realTimeTradeRiskControl(riskBean);
		txnsOrderinfoDAO.saveOrderInfo(orderinfo);
		tn = orderinfo.getTn();

		// 无异常时保存退款交易流水表，以便于以后退款审核操作
		PojoTxnsRefund refundOrder = new PojoTxnsRefund();
		refundOrder.setRefundorderno(serialNumberService.generateRefundNo());
		refundOrder.setOldorderno(orderBean.getOrigOrderId());
		refundOrder.setOldtxnseqno(old_txnsLog.getTxnseqno());
		refundOrder.setMerchno(orderBean.getCoopInstiId());
		refundOrder.setSubmerchno(orderBean.getMerId());
		refundOrder.setMemberid(orderBean.getMemberId());
		refundOrder.setAmount(Long.valueOf(orderBean.getTxnAmt()));
		refundOrder.setOldamount(old_orderInfo.getOrderamt());
		refundOrder.setRefundtype(orderBean.getRefundType());
		refundOrder.setRefunddesc(orderBean.getOrderDesc());
		refundOrder.setReltxnseqno(txnsLog.getTxnseqno());
		refundOrder.setTxntime(DateUtil.getCurrentDateTime());
		refundOrder.setStatus("01");
		refundOrder.setRelorderno(orderBean.getOrderId());
		txnsRefundDAO.saveRefundOrder(refundOrder);
		return tn;

	}

}
