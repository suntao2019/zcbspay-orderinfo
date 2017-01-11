/* 
 * QueryServiceImpl.java  
 * 
 * version TODO
 *
 * 2016年10月17日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.query.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zcbspay.platform.orderinfo.exception.QueryOrderException;
import com.zcbspay.platform.orderinfo.query.bean.OrderResultBean;
import com.zcbspay.platform.orderinfo.query.dao.InsteadPayRealtimeDAO;
import com.zcbspay.platform.orderinfo.query.dao.TxnsLogDAO;
import com.zcbspay.platform.orderinfo.query.dao.TxnsOrderinfoDAO;
import com.zcbspay.platform.orderinfo.query.enums.BusiTypeEnum;
import com.zcbspay.platform.orderinfo.query.enums.OrderType;
import com.zcbspay.platform.orderinfo.query.pojo.PojoInsteadPayRealtime;
import com.zcbspay.platform.orderinfo.query.pojo.PojoTxnsLog;
import com.zcbspay.platform.orderinfo.query.pojo.PojoTxnsOrderinfo;
import com.zcbspay.platform.orderinfo.query.service.QueryOrderService;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年10月17日 上午10:15:49
 * @since 
 */
@Service("queryOrderService")
public class QueryOrderServiceImpl implements QueryOrderService{

	@Autowired
	private TxnsOrderinfoDAO txnsOrderinfoDAO;
	@Autowired
	private TxnsLogDAO txnsLogDAO;
	@Autowired
	private InsteadPayRealtimeDAO insteadPayRealtimeDAO;
	/**
	 *
	 * @param merchNo
	 * @param orderId
	 * @return
	 * @throws PaymentOrderException 
	 */
	@Override
	public OrderResultBean queryOrder(String merchNo, String orderId) throws QueryOrderException {
		PojoTxnsOrderinfo orderinfo = txnsOrderinfoDAO.getOrderinfoByOrderNoAndMerchNo(orderId, merchNo);
		if(orderinfo==null){
			throw new QueryOrderException("PC004");
		}
		PojoTxnsLog txnsLog = txnsLogDAO.getTxnsLogByTxnseqno(orderinfo.getRelatetradetxn());
		OrderResultBean order = new OrderResultBean();
		order.setMerId(orderinfo.getFirmemberno());
		order.setMerName(orderinfo.getFirmembername());
		order.setMerAbbr(orderinfo.getFirmembershortname());
		order.setOrderId(orderinfo.getOrderno());
		order.setTxnAmt(orderinfo.getOrderamt()+"");
		order.setTxnTime(orderinfo.getOrdercommitime());
		order.setOrderStatus(orderinfo.getStatus());
		order.setOrderDesc(orderinfo.getOrderdesc());
		order.setCurrencyCode(orderinfo.getCurrencycode());
		order.setTn(orderinfo.getTn());
		BusiTypeEnum busitype = BusiTypeEnum.fromValue(txnsLog.getBusitype());
		String code=OrderType.UNKNOW.getCode();
		if(busitype.equals(BusiTypeEnum.consumption)){
			code=OrderType.CONSUME.getCode();
		}else if(busitype.equals(BusiTypeEnum.refund)){
			code=OrderType.REFUND.getCode();
		}else if(busitype.equals(BusiTypeEnum.charge)){
			code=OrderType.RECHARGE.getCode();
		}else if(busitype.equals(BusiTypeEnum.withdrawal)){
			code=OrderType.WITHDRAW.getCode();
		}
		order.setOrderType(code);
		return order;
	}
	/**
	 *
	 * @param merchNo
	 * @param orderId
	 * @throws PaymentOrderException 
	 */
	@Override
	public OrderResultBean queryInsteadPayOrder(String merchNo, String orderId) throws QueryOrderException {
		PojoInsteadPayRealtime orderinfo = insteadPayRealtimeDAO.getOrderinfoByOrderNoAndMerchNo(merchNo, orderId);//getOrderinfoByOrderNoAndMerchNo(orderId, merchNo);
		if(orderinfo==null){
			throw new QueryOrderException("PC004");
		}
		PojoTxnsLog txnsLog = txnsLogDAO.getTxnsLogByTxnseqno(orderinfo.getTxnseqno());
		OrderResultBean order = new OrderResultBean();
		order.setMerId(orderinfo.getMerId());
		order.setMerName(orderinfo.getMerName());
		order.setMerAbbr(orderinfo.getMerNameAbbr());
		order.setOrderId(orderinfo.getOrderno());
		order.setTxnAmt(orderinfo.getTransAmt()+"");
		order.setTxnTime(orderinfo.getOrderCommiTime());
		order.setOrderStatus(orderinfo.getStatus());
		order.setOrderDesc(orderinfo.getNotes());
		order.setCurrencyCode(orderinfo.getCurrencyCode());
		order.setTn(orderinfo.getTn());
		BusiTypeEnum busitype = BusiTypeEnum.fromValue(txnsLog.getBusitype());
		String code=OrderType.UNKNOW.getCode();
		if(busitype.equals(BusiTypeEnum.consumption)){
			code=OrderType.CONSUME.getCode();
		}else if(busitype.equals(BusiTypeEnum.refund)){
			code=OrderType.REFUND.getCode();
		}else if(busitype.equals(BusiTypeEnum.charge)){
			code=OrderType.RECHARGE.getCode();
		}else if(busitype.equals(BusiTypeEnum.withdrawal)){
			code=OrderType.WITHDRAW.getCode();
		}
		order.setOrderType(code);
		return order;
		
	}
	/**
	 *
	 * @param tn
	 * @return
	 * @throws PaymentOrderException 
	 */
	@Override
	public OrderResultBean queryOrderByTN(String tn) throws QueryOrderException {
		PojoTxnsOrderinfo orderinfo = txnsOrderinfoDAO.getOrderinfoByTN(tn);
		if(orderinfo==null){
			throw new QueryOrderException("PC004");
		}
		PojoTxnsLog txnsLog = txnsLogDAO.getTxnsLogByTxnseqno(orderinfo.getRelatetradetxn());
		OrderResultBean order = new OrderResultBean();
		order.setMerId(orderinfo.getFirmemberno());
		order.setMerName(orderinfo.getFirmembername());
		order.setMerAbbr(orderinfo.getFirmembershortname());
		order.setOrderId(orderinfo.getOrderno());
		order.setTxnAmt(orderinfo.getOrderamt()+"");
		order.setTxnTime(orderinfo.getOrdercommitime());
		order.setOrderStatus(orderinfo.getStatus());
		order.setOrderDesc(orderinfo.getOrderdesc());
		order.setCurrencyCode(orderinfo.getCurrencycode());
		order.setTn(orderinfo.getTn());
		BusiTypeEnum busitype = BusiTypeEnum.fromValue(txnsLog.getBusitype());
		String code=OrderType.UNKNOW.getCode();
		if(busitype.equals(BusiTypeEnum.consumption)){
			code=OrderType.CONSUME.getCode();
		}else if(busitype.equals(BusiTypeEnum.refund)){
			code=OrderType.REFUND.getCode();
		}else if(busitype.equals(BusiTypeEnum.charge)){
			code=OrderType.RECHARGE.getCode();
		}else if(busitype.equals(BusiTypeEnum.withdrawal)){
			code=OrderType.WITHDRAW.getCode();
		}
		order.setOrderType(code);
		return order;
	}

}
