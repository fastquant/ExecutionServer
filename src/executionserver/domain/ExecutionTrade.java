/*
 * Blitz Trading
 */
package executionserver.domain;

/**
 * Trade internal server representation.
 * 
 * @author Sylvio Azevedo
 */
public class ExecutionTrade {
    
    // properties
    public String exchange;
    public String security;
    public String orderid;
    public String tradeId;
    
    public int    side;
    
    public double price;
    public double qty;
}
