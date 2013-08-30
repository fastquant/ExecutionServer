/*
 * Blitz Trading
 * 
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
@XmlRootElement(name="Customisation")
public class Customisation {

    @XmlElementWrapper(name = "Commands")
    @XmlElement(name="Command")
    public List<Command> commands;
}
