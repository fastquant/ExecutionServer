/*
 * Blitz Trading
 * 
 */
package executionserver.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
@XmlRootElement(name="Database")
public class Database {
    
    @XmlAttribute(name="ConnectionString")
    public String connString;
    
    @XmlAttribute(name="Database")
    public String database;
}
