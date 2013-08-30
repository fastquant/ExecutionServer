/*
 * Blitz Trading
 */
package executionserver.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo
 */
@XmlRootElement(name="Market")
public class Market {
    
    @XmlAttribute(name="Name")
    public String name;
    
    @XmlAttribute(name="Connection")
    public String conn;
}