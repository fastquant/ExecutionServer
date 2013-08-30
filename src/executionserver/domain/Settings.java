/**
 * Blitz Trading
 */
package executionserver.domain;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
@XmlRootElement(name="ExecutionServer")
public class Settings {
    
    @XmlElementWrapper(name = "Connections")
    @XmlElement(name="Connection")
    public List<Connection> connections;    
    
    @XmlElementWrapper(name = "MarketDefaultConnections")
    @XmlElement(name="Market")
    public List<Market> markets;    
    
    @XmlElement(name="Database")
    public Database srvDb;
    
    @XmlElement(name="ProtobuffAcceptor")
    public ProtobuffAcceptor protobuffAcceptor;
    
    @XmlElement(name="AdminAcceptor")
    public AdminAcceptor adminAcceptor;
}
