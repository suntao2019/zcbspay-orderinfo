/* 
 * AbstractRechargeOrderService.java  
 * 
 * version TODO
 *
 * 2016年11月22日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.service.recharge;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zcbspay.platform.member.coopinsti.bean.CoopInsti;
import com.zcbspay.platform.member.coopinsti.service.CoopInstiService;
import com.zcbspay.platform.member.individual.bean.MemberAccountBean;
import com.zcbspay.platform.member.individual.bean.MemberBean;
import com.zcbspay.platform.member.individual.bean.PoMemberBean;
import com.zcbspay.platform.member.individual.bean.enums.MemberType;
import com.zcbspay.platform.member.individual.service.MemberAccountService;
import com.zcbspay.platform.member.individual.service.MemberService;
import com.zcbspay.platform.member.merchant.bean.MerchantBean;
import com.zcbspay.platform.member.merchant.service.MerchService;
import com.zcbspay.platform.orderinfo.bean.BaseOrderBean;
import com.zcbspay.platform.orderinfo.common.bean.ResultBean;
import com.zcbspay.platform.orderinfo.dao.ProdCaseDAO;
import com.zcbspay.platform.orderinfo.dao.TxncodeDefDAO;
import com.zcbspay.platform.orderinfo.dao.TxnsOrderinfoDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoProdCase;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxncodeDef;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsOrderinfo;
import com.zcbspay.platform.orderinfo.enums.AcctStatusType;
import com.zcbspay.platform.orderinfo.enums.BusiTypeEnum;
import com.zcbspay.platform.orderinfo.exception.OrderException;
import com.zcbspay.platform.orderinfo.recharge.bean.RechargeOrderBean;
import com.zcbspay.platform.orderinfo.service.CheckOfServcie;
import com.zcbspay.platform.orderinfo.service.OrderService;
import com.zcbspay.platform.orderinfo.utils.ValidateLocator;
import com.zlebank.zplatform.acc.bean.enums.Usage;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年11月22日 下午2:52:44
 * @since 
 */
@Component
public abstract class AbstractRechargeOrderService implements OrderService,CheckOfServcie<RechargeOrderBean>{

	private static final Logger logger = LoggerFactory.getLogger(AbstractRechargeOrderService.class); 
	private static final String ANONMEMBERID="999999999999999";
	@Autowired
	private TxnsOrderinfoDAO txnsOrderinfoDAO;
	@Autowired
	private TxncodeDefDAO txncodeDefDAO;
	@Reference(version="1.0")
	private MerchService merchService;
	@Autowired
	private ProdCaseDAO prodCaseDAO;
	@Reference(version="1.0")
	private MemberService memberService;
	@Reference(version="1.0")
	private CoopInstiService coopInstiService;
	@Reference(version="1.0")
	private MemberAccountService memberAccountService;
	
	/**
	 * 订单非空有效性检查
	 * @param baseOrderBean
	 * @throws OrderException
	 */
	@Override
	public void checkOfOrder(BaseOrderBean baseOrderBean) throws OrderException{
		ResultBean resultBean = null;
		resultBean = ValidateLocator.validateBeans((RechargeOrderBean)baseOrderBean);
		if(!resultBean.isResultBool()){
			throw new OrderException("OD049", resultBean.getErrMsg());
		}
	}
	/**
	 * 检查订单二次支付
	 * @param baseOrderBean
	 * @return 受理订单号 tn
	 * @throws OrderException
	 */
	@Override
	public String checkOfSecondPay(RechargeOrderBean orderBean) throws OrderException{
		PojoTxnsOrderinfo orderinfo = txnsOrderinfoDAO.getOrderinfoByOrderNoAndMerchNo(orderBean.getOrderId(), orderBean.getMerId());
		if(orderinfo==null){
			return null;
		}
		if(orderinfo.getOrderamt().longValue()!=Long.valueOf(orderBean.getTxnAmt()).longValue()){
			logger.info("订单金额:{};数据库订单金额:{}", orderBean.getTxnAmt(),orderinfo.getOrderamt());
			throw new OrderException("OD015");
		}
		
		if(!orderinfo.getOrdercommitime().equals(orderBean.getTxnTime())){
			logger.info("订单时间:{};数据库订单时间:{}", orderBean.getTxnTime(),orderinfo.getOrdercommitime());
			throw new OrderException("OD016");
		}
		return orderinfo.getTn();
	}
	
	/**
	 * 检查订单是否为二次提交
	 * @param orderBean
	 * @throws OrderException
	 */
	@Override
	public void checkOfRepeatSubmit(RechargeOrderBean orderBean) throws OrderException{
		PojoTxnsOrderinfo orderInfo = txnsOrderinfoDAO.getOrderinfoByOrderNoAndMerchNo(orderBean.getOrderId(), orderBean.getMerId());
		if (orderInfo != null) {
			if ("00".equals(orderInfo.getStatus())) {// 交易成功订单不可二次支付
				throw new OrderException("OD001","订单交易成功，请不要重复支付");
			}
			if ("02".equals(orderInfo.getStatus())) {
				throw new OrderException("OD002","订单正在支付中，请不要重复支付");
			}
			if ("04".equals(orderInfo.getStatus())) {
				throw new OrderException("OD003","订单失效");
			}
			
		}
	}
	
	/**
	 * 检查订单业务有效性
	 * @param orderBean
	 * @throws OrderException
	 */
	@Override
	public void checkOfBusiness(RechargeOrderBean orderBean) throws OrderException {
		PojoTxncodeDef busiModel = txncodeDefDAO.getBusiCode(orderBean.getTxnType(), orderBean.getTxnSubType(), orderBean.getBizType());
        if(busiModel==null){
        	throw new OrderException("OD045");
        }
        BusiTypeEnum busiTypeEnum = BusiTypeEnum.fromValue(busiModel.getBusitype());
        if(busiTypeEnum==BusiTypeEnum.charge){//充值
        	//个人充值
        	if (StringUtils.isEmpty(orderBean.getMemberId()) || "999999999999999".equals(orderBean.getMemberId())) {
				throw new OrderException("OD008");
			}
        	//商户充值
        	if (StringUtils.isNotEmpty(orderBean.getMerId())){
        		MerchantBean member = merchService.getMerchBymemberId(orderBean.getMerId());
        		if(member==null){
            		throw new OrderException("OD009");
            	}
            	PojoProdCase prodCase= prodCaseDAO.getMerchProd(member.getPrdtVer(),busiModel.getBusicode());
            	if(prodCase==null){
                    throw new OrderException("OD005");
                }
        	}
        }else{
            throw new OrderException("OD045");
        }
	}
	
	/**
	 * 检查商户和合作机构有效性
	 * @param orderBean
	 * @throws OrderException
	 */
	@Override
	public void checkOfMerchantAndCoopInsti(RechargeOrderBean orderBean) throws OrderException{
        MerchantBean subMember = merchService.getMerchBymemberId(orderBean.getMerId());
        if (subMember == null) {
        	throw new OrderException("OD009");
        }
        PoMemberBean pojoMember = memberService.getMbmberByMemberId(orderBean.getMerId(), null);
        //校验商户会员信息 
        if (pojoMember.getMemberType()==MemberType.ENTERPRISE) {// 对于企业会员需要进行检查
        	CoopInsti pojoCoopInsti = coopInstiService.getInstiByInstiID(pojoMember.getInstiId());
            if (!orderBean.getCoopInstiId().equals(pojoCoopInsti.getInstiCode())) {
            	throw new OrderException("OD010");
            }
        }
	}
	
	/**
	 * 检查商户和会员的账户状态
	 *
	 * @param orderBean
	 * @throws OrderException
	 */
	@Override
	public void checkOfBusiAcct(RechargeOrderBean orderBean) throws OrderException{
		if (!ANONMEMBERID.equals(orderBean.getMemberId())&&StringUtils.isNotEmpty(orderBean.getMemberId())) {
			MemberBean member = new MemberBean();
			member.setMemberId(orderBean.getMemberId());
			MemberAccountBean memberAccountBean = null;
			try {
				memberAccountBean = memberAccountService.queryBalance(MemberType.INDIVIDUAL, member, Usage.BASICPAY);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				throw new OrderException("OD049", "账户查询异常:"+e.getMessage());
			}
			if (AcctStatusType.fromValue(memberAccountBean.getStatus()) == AcctStatusType.FREEZE||AcctStatusType.fromValue(memberAccountBean.getStatus())== AcctStatusType.STOP_IN) {
				throw new OrderException("OD049", "会员账户状态异常");
			}
		}
		if(StringUtils.isEmpty(orderBean.getMerId())){
			return ;
		}
		MemberBean member = new MemberBean();
		member.setMemberId(orderBean.getMerId());
		MemberAccountBean memberAccountBean = null;
		try {
			memberAccountBean = memberAccountService.queryBalance(MemberType.ENTERPRISE, member, Usage.BASICPAY);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new OrderException("OD049", "账户查询异常:"+e.getMessage());
		}
		if (AcctStatusType.fromValue(memberAccountBean.getStatus()) == AcctStatusType.FREEZE||AcctStatusType.fromValue(memberAccountBean.getStatus()) == AcctStatusType.STOP_IN) {
			//throw new TradeException("GW05");
			throw new OrderException("OD049", "商户账户状态异常");
		}
	}
	
	/**
	 * 检查消费订单特殊性要求检查，如果没有可以为空
	 * @param orderBean
	 * @throws OrderException 
	 */
	@Override
	public void checkOfSpecialBusiness(RechargeOrderBean orderBean) throws OrderException{
		
	}
	
	/**
	 * 检查所有订单有效性检查项
	 * @param baseOrderBean
	 * @throws OrderException 
	 */
	public abstract void checkOfAll(BaseOrderBean baseOrderBean) throws OrderException;
	
	/**
	 * 保存订单信息
	 * @param orderBean
	 * @throws OrderException 
	 */
	public abstract String saveRechargeOrder(BaseOrderBean baseOrderBean) throws OrderException;

	
}
