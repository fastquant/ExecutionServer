/*
 * Blitz Trading
 * 
 */
package executionserver.controller;

import com.mongodb.*;
import executionserver.domain.Connection;
import executionserver.domain.ExecutionOrder;
import executionserver.domain.ProcessingStatus;
import executionserver.domain.Settings;
import java.net.UnknownHostException;
import java.util.*;
import quickfix.field.SecurityID;
import quickfix.field.Symbol;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class DatabaseController {
    
    // mongo database representation
    private Mongo mongo;
    private DB db;
    
    public void connect(Settings settings) throws UnknownHostException {        
        
        mongo = new Mongo(new MongoURI(settings.srvDb.connString));
        db = mongo.getDB(settings.srvDb.database);
    }
    
    public boolean isConnect() {
        
        return mongo != null;
    }
    
    public void close(){
        
        mongo.close();        
        mongo = null;
        db = null;
    }
    
    public void insert(ExecutionOrder order) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        BasicDBObject doc = new BasicDBObject();
        
        doc.put("id", order.getId());        
        doc.put("account", order.getAccount());
        doc.put("brokage", order.getBrokage());
        doc.put("clientId", order.getClientId());
        doc.put("cumQty", order.getCumQty());
        doc.put("exchange", order.getExchange());
        doc.put("exchangeId", order.getExchangeId());        
        doc.put("lastId", order.getLastId());
        doc.put("leavesQty", order.getLeavesQty());        
        doc.put("minQty", order.getMinQty());
        doc.put("openQty", order.getOpenQty());
        doc.put("orderStatus", order.getOrderStatus());        
        doc.put("orderTimeInForce", order.getOrderTimeInForce());        
        doc.put("orderType", order.getOrderType());
        doc.put("originalMinQty", order.getOriginalMinQty());
        doc.put("originalOpenQty", order.getOriginalOpenQty());
        doc.put("originalPrice", order.getOriginalPrice());
        doc.put("originalQty", order.getOriginalQty());
        doc.put("originalStopPrice", order.getOriginalStopPrice());
        doc.put("owner", order.getOwner());
        doc.put("price", order.getPrice());
        doc.put("qty", order.getQty());
        doc.put("rejectReason", order.getRejectReason());
        doc.put("removalFlag", order.isRemovalFlag());
        doc.put("reqType", order.getReqType());
        doc.put("security", order.getSecurity());
        doc.put("side", order.getSide());
        doc.put("status", order.getStatus());
        doc.put("stopPrice", order.getStopPrice());
        doc.put("transactionCost", order.getTransactionCost());
        doc.put("validity", order.getValidity());        
        doc.put("dateCreated", order.getDateCreated());
        doc.put("lastPrice", order.getLastPrice());
        doc.put("lastShares", order.getLastShares());
        doc.put("route", order.getRoute());
        doc.put("broker", order.getBroker());
        doc.put("portfolio", order.getPortfolio());
        doc.put("securityId", order.getSecurityId());
        doc.put("securityIdSource", order.getSecurityIdSource());
        doc.put("securityExchange", order.getSecurityExchange());
        
        collection.insert(doc);                
        db.requestDone();
    }

    public boolean noNewOrders(Connection conn) {
        
        Calendar now = getToday();
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        BasicDBObject doc = new BasicDBObject();
        
        doc.put("status", ProcessingStatus.NEW);        
        doc.put("route", conn.name);
        doc.put("dateCreated", new BasicDBObject("$gte", now.getTime()));
        
        return collection.count(doc) == 0;
    }

    public ExecutionOrder getOne(Connection conn) {
        
        Calendar now = getToday();
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        DBObject doc = new BasicDBObject();
        
        doc.put("status", ProcessingStatus.NEW);        
        doc.put("route", conn.name);
        doc.put("dateCreated", new BasicDBObject("$gte", now.getTime()));
        
        DBCursor cursor = collection.find(doc);
        
        if(!cursor.hasNext()) {
            return null;
        }
        
        doc = cursor.next();
        
        return new ExecutionOrder(doc);
    }

    public boolean exists(ExecutionOrder order) {
        
        Calendar now = getToday();
        
        now.clear(Calendar.HOUR);
        now.clear(Calendar.MINUTE);
        now.clear(Calendar.SECOND);
        now.clear(Calendar.MILLISECOND);
        
        DBCollection collection = db.getCollection("ExecutionOrder");        
        DBObject doc = new BasicDBObject();
        
        doc.put("id", order.getId());
        doc.put("dateCreated", new BasicDBObject("$gte", now.getTime()));
        
        DBCursor cursor = collection.find(doc);
        
        return cursor.hasNext();
    }
    
    public void updateStatus(ExecutionOrder order, int orderStatus) {        
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        BasicDBObject carrier = new BasicDBObject();
                
        carrier.put("orderStatus", orderStatus);        
                
        BasicDBObject newDoc = new BasicDBObject().append("$set", carrier);            
        
        collection.update(new BasicDBObject().append("id", order.getId()), newDoc);        
        db.requestDone();
    }
    
    public void update(ExecutionOrder order) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        BasicDBObject doc = new BasicDBObject();
        
        doc.put("id", order.getId());
        doc.put("account", order.getAccount());
        doc.put("brokage", order.getBrokage());
        doc.put("clientId", order.getClientId());
        doc.put("cumQty", order.getCumQty());
        doc.put("exchange", order.getExchange());
        doc.put("exchangeId", order.getExchangeId());        
        doc.put("lastId", order.getLastId());
        doc.put("leavesQty", order.getLeavesQty());        
        doc.put("minQty", order.getMinQty());
        doc.put("openQty", order.getOpenQty());
        doc.put("orderStatus", order.getOrderStatus());         // Fix order status            
        doc.put("orderTimeInForce", order.getOrderTimeInForce());        
        doc.put("orderType", order.getOrderType());
        doc.put("originalMinQty", order.getOriginalMinQty());
        doc.put("originalOpenQty", order.getOriginalOpenQty());
        doc.put("originalPrice", order.getOriginalPrice());
        doc.put("originalQty", order.getOriginalQty());
        doc.put("originalStopPrice", order.getOriginalStopPrice());
        doc.put("owner", order.getOwner());
        doc.put("price", order.getPrice());
        doc.put("qty", order.getQty());
        doc.put("rejectReason", order.getRejectReason());
        doc.put("removalFlag", order.isRemovalFlag());
        doc.put("reqType", order.getReqType());
        doc.put("security", order.getSecurity());
        doc.put("side", order.getSide());
        doc.put("status", order.getStatus());              // processing status
        doc.put("stopPrice", order.getStopPrice());
        doc.put("transactionCost", order.getTransactionCost());
        doc.put("validity", order.getValidity());
        doc.put("dateCreated", order.getDateCreated());
        doc.put("lastPrice", order.getLastPrice());
        doc.put("lastShares", order.getLastShares());
        doc.put("route", order.getRoute());
        doc.put("broker", order.getBroker());
        doc.put("portfolio", order.getPortfolio());
        doc.put("securityId", order.getSecurityId());
        doc.put("securityIdSource", order.getSecurityIdSource());
        doc.put("securityExchange", order.getSecurityExchange());
        
        collection.update(new BasicDBObject().append("id", order.getId()), doc);
        db.requestDone();
    }

    public void mark(ExecutionOrder order) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");        
        BasicDBObject newDoc = new BasicDBObject().append("$set", new BasicDBObject().append("status", ProcessingStatus.PROCESSED));
        
        collection.update(new BasicDBObject().append("id", order.getId()), newDoc);
        db.requestDone();
    }
    
    public void markProblem(ExecutionOrder order) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");        
        BasicDBObject newDoc = new BasicDBObject().append("$set", new BasicDBObject().append("status", ProcessingStatus.NOT_SENT));
        
        collection.update(new BasicDBObject().append("id", order.getId()), newDoc);
        db.requestDone();
    }
    
    public void markSent(ExecutionOrder order) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");        
        BasicDBObject newDoc = new BasicDBObject().append("$set", new BasicDBObject().append("status", ProcessingStatus.SENT));
        
        collection.update(new BasicDBObject().append("id", order.getId()), newDoc);
        db.requestDone();
    }    

    public ExecutionOrder find(String id) {        
                
        DBCollection collection = db.getCollection("ExecutionOrder");        
        DBObject doc = new BasicDBObject();
        
        doc.put("clientId", id);            
        
        DBCursor cursor = collection.find(doc);
        
        if(cursor.hasNext()) {    
            doc = cursor.next();
            return new ExecutionOrder(doc);
        }
        
        doc = new BasicDBObject();

        doc.put("id", id);            

        cursor = collection.find(doc);
        
        if(cursor.hasNext()) {
            doc = cursor.next();
            return new ExecutionOrder(doc);
        }
        
        doc = new BasicDBObject();

        doc.put("lastId", id);            

        cursor = collection.find(doc);
        
        if(cursor.hasNext()) {
            doc = cursor.next();
            return new ExecutionOrder(doc);
        }
            
        return null;
    }

    public List<ExecutionOrder> findOrdersByOwner(String clientName) {
        
        Calendar now = getToday();
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        DBObject doc = new BasicDBObject();
        
        doc.put("owner", clientName);
        doc.put("dateCreated", new BasicDBObject("$gte", now.getTime()));
        doc.put("status", ProcessingStatus.NOT_SENT);
        
        DBCursor cursor = collection.find(doc);
        
        List<ExecutionOrder> orderList = new ArrayList<ExecutionOrder>();
        
        while(cursor.hasNext()) {            
            orderList.add(new ExecutionOrder(cursor.next()));
        }        
        
        return orderList;
    }
    
    private Calendar getToday() {
        
        Calendar now = Calendar.getInstance();       
        
        now.clear(Calendar.HOUR);
        now.add(Calendar.HOUR, -19);
        now.clear(Calendar.MINUTE);
        now.clear(Calendar.SECOND);
        now.clear(Calendar.MILLISECOND);
        
        return now;
    }

    public ExecutionOrder changeId(ExecutionOrder order, String newId) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        BasicDBObject carrier = new BasicDBObject();
        
        String lastId = order.getId();
                
        carrier.put("id", newId);
        carrier.put("lastId", lastId);
                
        BasicDBObject newDoc = new BasicDBObject().append("$set", carrier);            
        
        collection.update(new BasicDBObject().append("id", order.getId()), newDoc);        
        db.requestDone();
        
        order.setId(newId);
        order.setLastId(lastId);
        
        return order;
    }

    public void saveExchangeId(ExecutionOrder order, String exchangeId) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");        
        BasicDBObject newDoc = new BasicDBObject().append("$set", new BasicDBObject().append("exchangeId", exchangeId));
        
        collection.update(new BasicDBObject().append("id", order.getId()), newDoc);
        db.requestDone();
    }

    public void insertSymbol(Symbol symbol, SecurityID securityId) {
        
        DBCollection collection = db.getCollection("Symbol");
        
        DBObject doc = new BasicDBObject();        
        doc.put("symbol", symbol.getValue());
        doc.put("securityId", securityId.getValue());
        
        collection.insert(doc);
        db.requestDone();
    }

    public Map<String, String> findAllSecurities() {
        
        DBCollection collection = db.getCollection("Symbol");        
        
        DBCursor cursor = collection.find();
        
        if(!cursor.hasNext()) {
            return null;
        }
        
        Map<String, String> result = new HashMap<String, String>();
        
        while(cursor.hasNext()) {            
            DBObject curr = cursor.next();
            result.put(curr.get("symbol").toString(), curr.get("securityId").toString());
        }         
        return result;
    }

    public List<ExecutionOrder> findStuck(Connection conn) {
        
        Calendar now = getToday();
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        DBObject doc = new BasicDBObject();
        
        doc.put("status", ProcessingStatus.NEW);        
        doc.put("route", conn.name);
        doc.put("dateCreated", new BasicDBObject("$gte", now.getTime()));
        
        DBCursor cursor = collection.find(doc);
        
        if(!cursor.hasNext()) {
            return null;
        }
        
        List<ExecutionOrder> stuckList = new ArrayList<ExecutionOrder>();
        
        while(cursor.hasNext()) {            
            stuckList.add(new ExecutionOrder(cursor.next()));
        }
        
        return stuckList;
    }

    public void deleteAllSecurities() {
        DBCollection collection = db.getCollection("Symbol");
        collection.drop();
        db.requestDone();
    }

    public void updateLasts(ExecutionOrder order, double qty, double price) {
        
        DBCollection collection = db.getCollection("ExecutionOrder");
        
        BasicDBObject carrier = new BasicDBObject();
                
        carrier.put("lastPrice",  price);        
        carrier.put("lastShares", qty);        
                
        BasicDBObject newDoc = new BasicDBObject().append("$set", carrier);            
        
        collection.update(new BasicDBObject().append("id", order.getId()), newDoc);        
        db.requestDone();
    }
}