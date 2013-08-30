/*
 * Blitz Trading
 */
package executionserver.domain;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ConnectionInfo {
    
    // properties
    public int port;
    public int heartBitInterval;
    
    public boolean dictionary;
    public boolean resetOnLogon;
    public boolean socketNodelay;
    
    public String name;
    public String hostname;
    public String fixVersion;
    public String senderCompId;
    public String targetCompId;
    public String logPath;
    public String storePath;
}
