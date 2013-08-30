/** 
 Blitz Trading
 */
package executionserver.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Fix connection settings class.
 * 
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
@XmlRootElement(name="Connection")
public class Connection {    
    
    // Connection/routing name.
    @XmlAttribute(name="Name")              
    public String name;        
    
    @XmlAttribute(name="Configuration")
    public String configFile;
    
    // User defined field to be inserted in the Fix messages.
    @XmlAttribute(name="Customisation")     
    public String customFile;
    
    // Fix class implementation
    @XmlAttribute(name="Implementation")    
    public String impl;
    
    // Activate or deactivates connection
    @XmlAttribute(name="Active")
    public boolean active;
}