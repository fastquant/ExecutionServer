/*
 * Blitz Trading
 */
package executionserver.domain;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class  MessageTypes {     
    
    public static final int NONE               = -1;
    public static final int CONNECT            =  1;
    public static final int CONNECT_RESPONSE   =  2;
    public static final int HEARTBEAT          =  3;
    public static final int ORDER_REQUEST      =  4;
    public static final int ORDER_UPDATE       =  5;
    public static final int ORDER_LIST         =  6;
    public static final int TRADE_LIST         =  7;

}
