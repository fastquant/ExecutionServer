/*
 * Blitz Trading
 */
package executionserver.domain;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
@XmlRootElement(name="Group")
public class Group {
    
    @XmlAttribute(name="Number")
    public int number;
    
    @XmlElementWrapper(name = "Fields")
    @XmlElement(name="Field")
    public List<Field> fields;
}
