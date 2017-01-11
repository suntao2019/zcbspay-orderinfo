/* 
 * TxnsLogDAOImpl.java  
 * 
 * version TODO
 *
 * 2016年9月13日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.query.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zcbspay.platform.orderinfo.common.dao.impl.HibernateBaseDAOImpl;
import com.zcbspay.platform.orderinfo.query.dao.TxnsLogDAO;
import com.zcbspay.platform.orderinfo.query.pojo.PojoTxnsLog;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月13日 下午5:33:02
 * @since
 */
@Repository
public class TxnsLogDAOImpl extends HibernateBaseDAOImpl<PojoTxnsLog> implements
		TxnsLogDAO {

	

	/**
	 *
	 * @param txnseqno
	 * @return
	 */
	@Override
	@Transactional(readOnly=true)
	public PojoTxnsLog getTxnsLogByTxnseqno(String txnseqno) {
		Criteria criteria = getSession().createCriteria(PojoTxnsLog.class);
		criteria.add(Restrictions.eq("txnseqno", txnseqno));
		return (PojoTxnsLog) criteria.uniqueResult();
	}

	
}
