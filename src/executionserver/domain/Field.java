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
@XmlRootElement(name="Field")
public class Field {

    @XmlAttribute(name="Number")
    public int number;
    
    @XmlAttribute(name="Value")
    public String value;
}
