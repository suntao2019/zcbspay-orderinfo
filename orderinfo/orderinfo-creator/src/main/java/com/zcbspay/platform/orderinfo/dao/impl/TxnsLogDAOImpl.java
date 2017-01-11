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
package com.zcbspay.platform.orderinfo.dao.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zcbspay.platform.orderinfo.bean.CardBin;
import com.zcbspay.platform.orderinfo.common.dao.impl.HibernateBaseDAOImpl;
import com.zcbspay.platform.orderinfo.dao.TxnsLogDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsLog;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月13日 下午5:33:02
 * @since 
 */
@Repository
public class TxnsLogDAOImpl extends HibernateBaseDAOImpl<PojoTxnsLog> implements TxnsLogDAO {

	private static final Logger log = LoggerFactory.getLogger(TxnsLogDAOImpl.class);
	@Transactional(propagation=Propagation.REQUIRED,rollbackFor=Throwable.class)
	public void saveTxnsLog(PojoTxnsLog txnsLog){
		super.saveEntity(txnsLog);
	}
	
	@Override
	@Transactional(readOnly=true)
	public CardBin getCard(String cardNo) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("SELECT t.CARDBIN as cardBin,t.CARDLEN as cardLen,t.BINLEN as BINLEN   ");
        sqlBuffer.append(",t.CARDNAME as cardName, t.TYPE as Type,t.BANKCODE as bankCode ,  b.bankname as bankName  ");
        sqlBuffer.append("FROM t_card_bin t  inner join  T_BANK_INSTI b on t.BANKCODE =b.BANKCODE ");
        sqlBuffer.append("WHERE ? LIKE t.cardbin || '%'  ");
        sqlBuffer.append("AND t.cardlen = ?  ");
        sqlBuffer.append("ORDER BY t.cardbin DESC  ");
        Session session=this.getSession();
        SQLQuery sqlquery=session.createSQLQuery(sqlBuffer.toString());
        sqlquery.setParameter(1, cardNo.intern().length());
        sqlquery.setParameter(0, cardNo);
//        sqlquery.setResultTransformer(new SQLColumnToBean(
//                CardBin.class));
        // @SuppressWarnings("unchecked")
        //List<CardBin> li=   sqlquery.list();
        sqlquery .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> li=sqlquery.list();
        if(li.isEmpty())
            return null;
        CardBin cardBin=new CardBin();
        Map< String, Object> carBin= li.get(0);
        cardBin.setBinLen( carBin.get("BINLEN")!=null?String.valueOf(carBin.get("BINLEN")):null);
        cardBin.setCardBin( carBin.get("CARDBIN")!=null?String.valueOf(carBin.get("CARDBIN")):null);
        cardBin.setCardLen(carBin.get("CARDLEN")!=null?String.valueOf(carBin.get("CARDLEN")):null);
        cardBin.setCardName(carBin.get("CARDNAME")!=null?String.valueOf(carBin.get("CARDNAME")):null);
        cardBin.setType(   carBin.get("TYPE")!=null?String.valueOf(carBin.get("TYPE")):null);
        cardBin.setBankCode(carBin.get("BANKCODE")!=null?String.valueOf(carBin.get("BANKCODE")):null);
        cardBin.setBankName(carBin.get("BANKNAME")!=null?String.valueOf(carBin.get("BANKNAME")):null);
        return    cardBin;       
                
    //    return li.get(0);
        
    }

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

	/**
	 *
	 * @param txnseqno
	 * @param fee
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED,rollbackFor=Throwable.class)
	public void updateTradeFee(String txnseqno, long fee) {
		// TODO Auto-generated method stub
		String hql = "update PojoTxnsLog set txnfee=? where txnseqno = ?  ";
		Session session = getSession();
		Query query = session.createQuery(hql);
		query.setParameter(0, fee);
		query.setParameter(1, txnseqno);
		int rows = query.executeUpdate();
		log.info("updateTradeFee sql :{},effect rows:{}", hql, rows);
	}

}
