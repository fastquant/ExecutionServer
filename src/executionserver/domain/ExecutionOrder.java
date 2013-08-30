/** 
 * Blitz Trading
 */
package executionserver.domain;

import com.mongodb.DBObject;
import java.util.Calendar;
import java.util.Date;

/**
 * Order internal server representation.
 * 
 * @author Sylvio Azevedo
 */
public class ExecutionOrder {
    
    // properties
    private String  id;
    private String  exchange;
    private String  owner;
    private String  clientId;
    private String  exchangeId;        
    private String  lastId;
    private String  rejectReason;
    private String  security;
    private String  account;
    private String  route;
    private String  broker;
    private String  portfolio;
    private String  securityId;
    private String  securityIdSource;
    private String  securityExchange;
    
    private int     side;
    private int     orderStatus;   
    private int     status;
    private int     orderType;
    private int     orderTimeInForce;
    private int     validity;
    
    private double  qty;
    private double  originalQty;
    private double  minQty;
    private double  originalMinQty;
    private double  openQty;
    private double  originalOpenQty;
    private double  cumQty;
    private double  leavesQty;
    private double  price;
    private double  originalPrice;
    private double  stopPrice;
    private double  originalStopPrice;
    private double  brokage;
    private double  transactionCost;  
    private double  lastPrice;
    private double  lastShares;
    
    private boolean removalFlag;
    
    private int     reqType;
    
    private Date    dateCreated;
    
    /**
     * Empty constructor.
     */
    public ExecutionOrder() {    
        dateCreated = Calendar.getInstance().getTime();
    }

    /**
     * Mongo object initialized constructor.
     * 
     * @param doc 
     */
    public ExecutionOrder(DBObject doc) {
        
        // properties
        id                  = (String) doc.get("id");
        exchange            = (String) doc.get("exchange");
        owner               = (String) doc.get("owner");
        clientId            = (String) doc.get("clientId");        
        exchangeId          = (String) doc.get("exchangeId");        
        lastId              = (String) doc.get("lastId");    
        rejectReason        = (String) doc.get("rejectReason");
        security            = (String) doc.get("security");
        account             = (String) doc.get("account");
        route               = (String) doc.get("route");
        broker              = (String) doc.get("broker");
        portfolio           = (String) doc.get("portfolio");
        securityId          = (String) doc.get("securityId");
        securityIdSource    = (String) doc.get("securityIdSource");
        securityExchange    = (String) doc.get("securityExchange");

        side                = (Integer) doc.get("side");
        orderStatus         = (Integer) doc.get("orderStatus");   
        status              = (Integer) doc.get("status");
        reqType             = (Integer) doc.get("reqType");
        orderType           = (Integer) doc.get("orderType");
        orderTimeInForce    = (Integer) doc.get("orderTimeInForce");
        validity            = (Integer) doc.get("validity");

        qty                 = (Double) doc.get("qty");
        originalQty         = (Double) doc.get("originalQty");
        minQty              = (Double) doc.get("minQty");
        originalMinQty      = (Double) doc.get("originalMinQty");
        openQty             = (Double) doc.get("openQty");
        originalOpenQty     = (Double) doc.get("originalOpenQty");
        cumQty              = (Double) doc.get("cumQty");
        leavesQty           = (Double) doc.get("leavesQty");
        price               = (Double) doc.get("price");
        originalPrice       = (Double) doc.get("originalPrice");
        stopPrice           = (Double) doc.get("stopPrice");
        originalStopPrice   = (Double) doc.get("originalStopPrice");
        brokage             = (Double) doc.get("brokage");
        transactionCost     = (Double) doc.get("transactionCost");
        lastPrice           = (Double) doc.get("lastPrice");
        lastShares          = (Double) doc.get("lastShares");

        removalFlag = (Boolean) doc.get("removalFlag");
        
        dateCreated = Calendar.getInstance().getTime();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the exchange
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * @param exchange the exchange to set
     */
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the exchangeId
     */
    public String getExchangeId() {
        return exchangeId;
    }

    /**
     * @param exchangeId the exchangeId to set
     */
    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    /**
     * @return the lastId
     */
    public String getLastId() {
        return lastId;
    }

    /**
     * @param lastId the lastId to set
     */
    public void setLastId(String lastId) {
        this.lastId = lastId;
    }

    /**
     * @return the rejectReason
     */
    public String getRejectReason() {
        return rejectReason;
    }

    /**
     * @param rejectReason the rejectReason to set
     */
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    /**
     * @return the security
     */
    public String getSecurity() {
        return security;
    }

    /**
     * @param security the security to set
     */
    public void setSecurity(String security) {
        this.security = security;
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    /**
     * @param account the account to set
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * @return the route
     */
    public String getRoute() {
        return route;
    }

    /**
     * @param route the route to set
     */
    public void setRoute(String route) {
        this.route = route;
    }

    /**
     * @return the broker
     */
    public String getBroker() {
        return broker;
    }

    /**
     * @param broker the broker to set
     */
    public void setBroker(String broker) {
        this.broker = broker;
    }

    /**
     * @return the portfolio
     */
    public String getPortfolio() {
        return portfolio;
    }

    /**
     * @param portfolio the portfolio to set
     */
    public void setPortfolio(String portfolio) {
        this.portfolio = portfolio;
    }

    /**
     * @return the side
     */
    public int getSide() {
        return side;
    }

    /**
     * @param side the side to set
     */
    public void setSide(int side) {
        this.side = side;
    }

    /**
     * @return the orderStatus
     */
    public int getOrderStatus() {
        return orderStatus;
    }

    /**
     * @param orderStatus the orderStatus to set
     */
    public void setOrderStatus(int orderStatus) {
        this.orderStatus = orderStatus;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the orderType
     */
    public int getOrderType() {
        return orderType;
    }

    /**
     * @param orderType the orderType to set
     */
    public void setOrderType(int orderType) {
        this.orderType = orderType;
    }

    /**
     * @return the orderTimeInForce
     */
    public int getOrderTimeInForce() {
        return orderTimeInForce;
    }

    /**
     * @param orderTimeInForce the orderTimeInForce to set
     */
    public void setOrderTimeInForce(int orderTimeInForce) {
        this.orderTimeInForce = orderTimeInForce;
    }

    /**
     * @return the validity
     */
    public int getValidity() {
        return validity;
    }

    /**
     * @param validity the validity to set
     */
    public void setValidity(int validity) {
        this.validity = validity;
    }

    /**
     * @return the qty
     */
    public double getQty() {
        return qty;
    }

    /**
     * @param qty the qty to set
     */
    public void setQty(double qty) {
        this.qty = qty;
    }

    /**
     * @return the originalQty
     */
    public double getOriginalQty() {
        return originalQty;
    }

    /**
     * @param originalQty the originalQty to set
     */
    public void setOriginalQty(double originalQty) {
        this.originalQty = originalQty;
    }

    /**
     * @return the minQty
     */
    public double getMinQty() {
        return minQty;
    }

    /**
     * @param minQty the minQty to set
     */
    public void setMinQty(double minQty) {
        this.minQty = minQty;
    }

    /**
     * @return the originalMinQty
     */
    public double getOriginalMinQty() {
        return originalMinQty;
    }

    /**
     * @param originalMinQty the originalMinQty to set
     */
    public void setOriginalMinQty(double originalMinQty) {
        this.originalMinQty = originalMinQty;
    }

    /**
     * @return the openQty
     */
    public double getOpenQty() {
        return openQty;
    }

    /**
     * @param openQty the openQty to set
     */
    public void setOpenQty(double openQty) {
        this.openQty = openQty;
    }

    /**
     * @return the originalOpenQty
     */
    public double getOriginalOpenQty() {
        return originalOpenQty;
    }

    /**
     * @param originalOpenQty the originalOpenQty to set
     */
    public void setOriginalOpenQty(double originalOpenQty) {
        this.originalOpenQty = originalOpenQty;
    }

    /**
     * @return the cumQty
     */
    public double getCumQty() {
        return cumQty;
    }

    /**
     * @param cumQty the cumQty to set
     */
    public void setCumQty(double cumQty) {
        this.cumQty = cumQty;
    }

    /**
     * @return the leavesQty
     */
    public double getLeavesQty() {
        return leavesQty;
    }

    /**
     * @param leavesQty the leavesQty to set
     */
    public void setLeavesQty(double leavesQty) {
        this.leavesQty = leavesQty;
    }

    /**
     * @return the price
     */
    public double getPrice() {
        return price;
    }

    /**
     * @param price the price to set
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * @return the originalPrice
     */
    public double getOriginalPrice() {
        return originalPrice;
    }

    /**
     * @param originalPrice the originalPrice to set
     */
    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    /**
     * @return the stopPrice
     */
    public double getStopPrice() {
        return stopPrice;
    }

    /**
     * @param stopPrice the stopPrice to set
     */
    public void setStopPrice(double stopPrice) {
        this.stopPrice = stopPrice;
    }

    /**
     * @return the originalStopPrice
     */
    public double getOriginalStopPrice() {
        return originalStopPrice;
    }

    /**
     * @param originalStopPrice the originalStopPrice to set
     */
    public void setOriginalStopPrice(double originalStopPrice) {
        this.originalStopPrice = originalStopPrice;
    }

    /**
     * @return the brokage
     */
    public double getBrokage() {
        return brokage;
    }

    /**
     * @param brokage the brokage to set
     */
    public void setBrokage(double brokage) {
        this.brokage = brokage;
    }

    /**
     * @return the transactionCost
     */
    public double getTransactionCost() {
        return transactionCost;
    }

    /**
     * @param transactionCost the transactionCost to set
     */
    public void setTransactionCost(double transactionCost) {
        this.transactionCost = transactionCost;
    }

    /**
     * @return the lastPrice
     */
    public double getLastPrice() {
        return lastPrice;
    }

    /**
     * @param lastPrice the lastPrice to set
     */
    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    /**
     * @return the lastShares
     */
    public double getLastShares() {
        return lastShares;
    }

    /**
     * @param lastShares the lastShares to set
     */
    public void setLastShares(double lastShares) {
        this.lastShares = lastShares;
    }

    /**
     * @return the removalFlag
     */
    public boolean isRemovalFlag() {
        return removalFlag;
    }

    /**
     * @param removalFlag the removalFlag to set
     */
    public void setRemovalFlag(boolean removalFlag) {
        this.removalFlag = removalFlag;
    }

    /**
     * @return the reqType
     */
    public int getReqType() {
        return reqType;
    }

    /**
     * @param reqType the reqType to set
     */
    public void setReqType(int reqType) {
        this.reqType = reqType;
    }

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the securityId
     */
    public String getSecurityId() {
        return securityId;
    }

    /**
     * @param securityId the securityId to set
     */
    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    /**
     * @return the securityIdSource
     */
    public String getSecurityIdSource() {
        return securityIdSource;
    }

    /**
     * @param securityIdSource the securityIdSource to set
     */
    public void setSecurityIdSource(String securityIdSource) {
        this.securityIdSource = securityIdSource;
    }

    /**
     * @return the securityExchange
     */
    public String getSecurityExchange() {
        return securityExchange;
    }

    /**
     * @param securityExchange the securityExchange to set
     */
    public void setSecurityExchange(String securityExchange) {
        this.securityExchange = securityExchange;
    }
}