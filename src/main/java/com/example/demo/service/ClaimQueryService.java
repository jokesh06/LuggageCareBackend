package com.example.demo.service;

import com.example.demo.catalyst.CatalystContext;
import com.zc.component.object.ZCRowObject;
import com.zc.component.zcql.ZCQL;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClaimQueryService {

    private static final String TABLE_NAME = "customerClaims";

    // 1. Get Open Claims (Raw types avoid angle-bracket copy-paste errors)
    @SuppressWarnings("rawtypes")
    public List getOpenClaimsForCustomer(Long customerId) throws Exception {
        String zcql = "SELECT * FROM " + TABLE_NAME + 
                      " WHERE customerId = " + customerId + 
                      " AND stage < 7 ORDER BY CREATEDTIME DESC";
        return executeZcqlQuery(zcql);
    }

    // 2. Get Closed Claims
    @SuppressWarnings("rawtypes")
    public List getClosedClaimsForCustomer(Long customerId) throws Exception {
        String zcql = "SELECT * FROM " + TABLE_NAME + 
                      " WHERE customerId = " + customerId + 
                      " AND stage = 7 ORDER BY CREATEDTIME DESC";
        return executeZcqlQuery(zcql);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List executeZcqlQuery(String zcql) throws Exception {
        ArrayList rows = ZCQL.getInstance(CatalystContext.admin()).executeQuery(zcql);
        List results = new ArrayList();
        
        if (rows != null) {
            for (Object obj : rows) {
                ZCRowObject row = (ZCRowObject) obj;
                Map all = (Map) row.getRowObject();
                if (all.containsKey(TABLE_NAME) && all.get(TABLE_NAME) instanceof Map) {
                    results.add(all.get(TABLE_NAME));
                } else {
                    results.add(all);
                }
            }
        }
        return results;
    }
}