/*
 * Blitz Trading
 * 
 */
package executionserver.controller;

import executionserver.domain.Customisation;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class CustomisationController {
    
    public static Customisation load(String configFilePath) throws JAXBException, FileNotFoundException {
        
        JAXBContext ctx = JAXBContext.newInstance(Customisation.class);
        Unmarshaller u = ctx.createUnmarshaller();
    
        return (Customisation) u.unmarshal(new FileInputStream(configFilePath));
    }
    
    public static void save(Customisation customisation, String configFilePath) throws JAXBException, FileNotFoundException {
        
        JAXBContext ctx = JAXBContext.newInstance(Customisation.class);        
        Marshaller m = ctx.createMarshaller();
        
        m.marshal(m, new FileOutputStream(configFilePath));        
    }
    
}
