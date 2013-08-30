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
@XmlRootElement(name="Command")
public class Command {

    @XmlAttribute(name="Type")
    public String type;
    
    @XmlElementWrapper(name = "Fields")
    @XmlElement(name="Field")
    public List<Field> fields;
    
    @XmlElementWrapper(name = "Groups")
    @XmlElement(name="Group")
    public List<Group> groups;
}
