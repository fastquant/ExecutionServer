/*
 * Blitz Trading
 */
package executionserver.fix;

import executionserver.domain.Connection;
import executionserver.domain.ConnectionInfo;
import executionserver.domain.ExecutionOrder;
import quickfix.SessionNotFound;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public interface FixConnection {
    
    public void start(Connection conn);
    
    public void start();
    
    public void stop();
    
    public void processRequest(ExecutionOrder order) throws SessionNotFound;

    public void orderNotify();
    
    public boolean isLoggedOn();

    public void loadSecurities() throws SessionNotFound;
    
    public String getHostPort();
    
    public ConnectionInfo getInfo();
}
