/*
 * Blitz Trading
 */
package executionserver;

import executionserver.controller.ExecutionServerController;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tanukisoftware.wrapper.WrapperListener;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class Launcher implements WrapperListener {

    private static ExecutionServerController esc;    
    private static final String CONFIG_FILE = "etc"  + File.separator + "ExecutionServer.xml";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        
        try {            
            esc = new ExecutionServerController();
            esc.start(CONFIG_FILE);
        }
        catch (Exception ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }                
    }
    
    @Override
    public Integer start(String[] strings) {
        esc = new ExecutionServerController();
        try {
            esc.start(CONFIG_FILE);
        } 
        catch (Exception ex) {
            java.util.logging.Logger.getLogger(ExecutionServerController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return 0;
    }

    @Override
    public int stop(int exitCode) {
        esc.stop();        
        return 0;
    }

    @Override
    public void controlEvent(int i) {
        // do nothing
    }
}
