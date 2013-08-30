/*
 * Blitz Trading
 */
package executionserver.domain;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class OrderStatus {
    
    public static final int REJECTED = -1;
    public static final int RECEIVED = 0;
    public static final int ACCEPTED = 1;
    public static final int NEW = 2;
    public static final int PARTIAL = 3;
    public static final int FILLED = 4;
    public static final int PENDING_REPLACE = 5;
    public static final int PENDING_CANCEL = 6;
    public static final int CANCELED = 7;
    public static final int REPLACED = 8;
}
