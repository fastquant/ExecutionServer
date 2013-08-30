/*
 * Blitz Trading
 */
package executionserver.domain;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ProcessingStatus {
    
    public static final int NEW = 1;
    public static final int PROCESSED = 2;
    public static final int SENT = 3;
    public static final int NOT_SENT = 4;
}
