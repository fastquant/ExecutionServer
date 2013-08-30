/*
 * Blitz Trading
 */
package executionserver.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
@XmlRootElement(name="Protobuff")
public class ProtobuffAcceptor {
    
    @XmlAttribute(name="Port")
    public String port;
}
