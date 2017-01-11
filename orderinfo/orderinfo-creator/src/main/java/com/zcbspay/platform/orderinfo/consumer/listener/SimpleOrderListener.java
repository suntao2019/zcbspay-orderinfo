/* 
 * SimpleOrderListener.java  
 * 
 * version TODO
 *
 * 2016年9月8日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.consumer.listener;

import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.google.common.base.Charsets;
import com.zcbspay.platform.orderinfo.bean.OrderBean;
import com.zcbspay.platform.orderinfo.common.bean.ResultBean;
import com.zcbspay.platform.orderinfo.consume.bean.ConsumeOrderBean;
import com.zcbspay.platform.orderinfo.consumer.enums.OrderTagsEnum;
import com.zcbspay.platform.orderinfo.exception.OrderException;
import com.zcbspay.platform.orderinfo.recharge.bean.RechargeOrderBean;
import com.zcbspay.platform.orderinfo.refund.bean.RefundOrderBean;
import com.zcbspay.platform.orderinfo.service.OrderCacheResultService;
import com.zcbspay.platform.orderinfo.service.OrderService;
import com.zcbspay.platform.orderinfo.withdraw.bean.WithdrawOrderBean;
/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月8日 下午3:00:14
 * @since
 */
@Service("simpleOrderListener")
public class SimpleOrderListener implements MessageListenerConcurrently {
	private static final Logger log = LoggerFactory.getLogger(SimpleOrderListener.class);
	private static final ResourceBundle RESOURCE = ResourceBundle.getBundle("consumer_order");
	private static final String KEY = "SIMPLEORDER:";

	//@Autowired
	//private ConsumeOrderService consumeOrderService;
	@Autowired
	private OrderCacheResultService orderCacheResultService;
	/*@Autowired
	private RefundOrderService refundOrderService;*/
	/*@Autowired
	private WithdrawOrderService withdrawOrderService;*/
	@Autowired
	@Qualifier("consumeOrderService")
	private OrderService consumeOrderService;
	@Autowired
	@Qualifier("rechargeOrderService")
	private OrderService rechargeOrderService;
	@Autowired
	@Qualifier("refundOrderService")
	private OrderService refundOrderService;
	@Autowired
	@Qualifier("withdrawOrderService")
	private OrderService withdrawOrderService;
	
	/**
	 *
	 * @param msgs
	 * @param context
	 * @return
	 */
	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
			ConsumeConcurrentlyContext context) {
		for (MessageExt msg : msgs) {
			if (msg.getTopic().equals(RESOURCE.getString("simple.order.subscribe"))) {
				OrderTagsEnum orderTagsEnum = OrderTagsEnum.fromValue(msg.getTags());
				if(orderTagsEnum==OrderTagsEnum.COMMONCONSUME_SIMPLIFIED){
					consumeOrder(msg);
				}else if(orderTagsEnum==OrderTagsEnum.REFUND_SIMPLIFIED){
					refundOrder(msg);
				}else if(orderTagsEnum==OrderTagsEnum.WITHDRAW_SIMPLIFIED) {
					withdrawOrder(msg);
				}else if(orderTagsEnum==OrderTagsEnum.RECHARGE_SIMPLIFIED){
					rechargeOrder(msg);
				}
			}
			log.info(Thread.currentThread().getName()+ " Receive New Messages: " + msgs);
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
	
	public void consumeOrder(MessageExt msg){
		String json = new String(msg.getBody(), Charsets.UTF_8);
		log.info("接收到的MSG:{}", json);
		log.info("接收到的MSGID:{}", msg.getMsgId());
		OrderBean orderBean = JSON.parseObject(json,
				OrderBean.class);
		if (orderBean == null) {
			log.warn("MSGID:{}JSON转换后为NULL,无法生成订单数据,原始消息数据为{}",msg.getMsgId(), json);
			return ;
		}
		String tn = "";
		ResultBean resultBean = null;
		try {
			tn = consumeOrderService.create(JSON.parseObject(json,ConsumeOrderBean.class));
			//tn = consumeOrderService.createConsumeOrder(orderBean);
			if (StringUtils.isNotEmpty(tn)) {
				resultBean = new ResultBean(tn);
			}else{
				resultBean = new ResultBean("09","创建订单失败");
			}
		} catch (OrderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			resultBean = new ResultBean(e.getCode(),e.getMessage());
		}catch (Throwable e) {
			e.printStackTrace();
			resultBean = new ResultBean("T000",e.getMessage());
		}
		orderCacheResultService.saveConsumeOrderOfTN(KEY+ msg.getMsgId(), JSON.toJSONString(resultBean));
	}

	public void refundOrder(MessageExt msg){
		String json = new String(msg.getBody(), Charsets.UTF_8);
		log.info("接收到的MSG:{}", json);
		log.info("接收到的MSGID:{}", msg.getMsgId());
		RefundOrderBean orderBean = JSON.parseObject(json,
				RefundOrderBean.class);
		if (orderBean == null) {
			log.warn("MSGID:{}JSON转换后为NULL,无法生成订单数据,原始消息数据为{}",
					msg.getMsgId(), json);
			return;
		}
		
		String tn = "";
		ResultBean resultBean = null;
		try {
			tn = refundOrderService.create(orderBean);
			if (StringUtils.isNotEmpty(tn)) {
				resultBean = new ResultBean(tn);
			}else{
				resultBean = new ResultBean("09","创建订单失败");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			resultBean = new ResultBean("",e.getMessage());
		}catch (Throwable e) {
			e.printStackTrace();
			resultBean = new ResultBean("T000",e.getMessage());
		}
		orderCacheResultService.saveConsumeOrderOfTN(KEY
				+ msg.getMsgId(), JSON.toJSONString(resultBean));
	}
	
	public void withdrawOrder(MessageExt msg){
		String json = new String(msg.getBody(), Charsets.UTF_8);
		log.info("接收到的MSG:{}", json);
		log.info("接收到的MSGID:{}", msg.getMsgId());
		WithdrawOrderBean orderBean = JSON.parseObject(json,
				WithdrawOrderBean.class);
		if (orderBean == null) {
			log.warn("MSGID:{}JSON转换后为NULL,无法生成订单数据,原始消息数据为{}",
					msg.getMsgId(), json);
			return;
		}
		String tn = "";
		ResultBean resultBean = null;
		try {
			tn = withdrawOrderService.create(orderBean);
			if (StringUtils.isNotEmpty(tn)) {
				resultBean = new ResultBean(tn);
			}else{
				resultBean = new ResultBean("09","创建订单失败");
			}
		} catch (OrderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			resultBean = new ResultBean(e.getCode(),e.getMessage());
		}catch (Throwable e) {
			e.printStackTrace();
			resultBean = new ResultBean("T000",e.getMessage());
		}
		orderCacheResultService.saveConsumeOrderOfTN(KEY
				+ msg.getMsgId(), JSON.toJSONString(resultBean));
	}
	
	public void rechargeOrder(MessageExt msg){
		String json = new String(msg.getBody(), Charsets.UTF_8);
		log.info("接收到的MSG:{}", json);
		log.info("接收到的MSGID:{}", msg.getMsgId());
		RechargeOrderBean orderBean = JSON.parseObject(json,
				RechargeOrderBean.class);
		if (orderBean == null) {
			log.warn("MSGID:{}JSON转换后为NULL,无法生成订单数据,原始消息数据为{}",
					msg.getMsgId(), json);
			return;
		}
		String tn = "";
		ResultBean resultBean = null;
		try {
			tn = rechargeOrderService.create(orderBean);
			//tn = consumeOrderService.createConsumeOrder(orderBean);
			if (StringUtils.isNotEmpty(tn)) {
				resultBean = new ResultBean(tn);
			}else{
				resultBean = new ResultBean("09","创建订单失败");
			}
		} catch (OrderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			resultBean = new ResultBean(e.getCode(),e.getMessage());
		}catch (Throwable e) {
			e.printStackTrace();
			resultBean = new ResultBean("T000",e.getMessage());
		}
		orderCacheResultService.saveConsumeOrderOfTN(KEY
				+ msg.getMsgId(), JSON.toJSONString(resultBean));
	}
}
