/* 
 * TxnsOrderinfoDAOImpl.java  
 * 
 * version TODO
 *
 * 2016年9月9日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zcbspay.platform.orderinfo.common.dao.impl.HibernateBaseDAOImpl;
import com.zcbspay.platform.orderinfo.dao.TxnsOrderinfoDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsOrderinfo;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月9日 下午5:08:47
 * @since 
 */
@Repository
public class TxnsOrderinfoDAOImpl extends HibernateBaseDAOImpl<PojoTxnsOrderinfo> implements TxnsOrderinfoDAO{

	/**
	 *
	 * @param orderinfo
	 */
	@Transactional(propagation=Propagation.REQUIRED,rollbackFor=Throwable.class)
	public void saveOrderInfo(PojoTxnsOrderinfo orderinfo) {
		// TODO Auto-generated method stub
		super.saveEntity(orderinfo);
	}

	/**
	 *
	 * @param txnseqno
	 * @return
	 */
	@Transactional(readOnly=true)
	public PojoTxnsOrderinfo getOrderinfoByTxnseqno(String txnseqno) {
		Criteria criteria = getSession().createCriteria(PojoTxnsOrderinfo.class);
		criteria.add(Restrictions.eq("relatetradetxn", txnseqno));
		return (PojoTxnsOrderinfo) criteria.uniqueResult();
	}

	/**
	 *
	 * @param orderNo
	 * @param merchNo
	 * @return
	 */
	@Transactional(readOnly=true)
	public PojoTxnsOrderinfo getOrderinfoByOrderNoAndMerchNo(String orderNo,
			String merchNo) {
		Criteria criteria = getSession().createCriteria(PojoTxnsOrderinfo.class);
		criteria.add(Restrictions.eq("orderno", orderNo));
		criteria.add(Restrictions.eq("secmemberno", merchNo));
		return (PojoTxnsOrderinfo) criteria.uniqueResult();
	}

	/**
	 *
	 * @param tn
	 * @return
	 */
	@Transactional(readOnly=true)
	public PojoTxnsOrderinfo getOrderinfoByTN(String tn) {
		Criteria criteria = getSession().createCriteria(PojoTxnsOrderinfo.class);
		criteria.add(Restrictions.eq("tn", tn));
		return (PojoTxnsOrderinfo) criteria.uniqueResult();
	}

	

}
