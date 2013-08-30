/*
 * Blitz Trading
 */
package executionserver.controller;

import executionserver.domain.ConnectionInfo;
import executionserver.fix.FixConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.mina.common.IoSession;
import quickfix.SessionNotFound;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class AdminController {

    private ExecutionServerController server;
    
    public AdminController(ExecutionServerController server) {
        this.server = server;
    }
    
    public void parse(String message, IoSession session) {
        
        String[] command = message.split(" ");        
        
        session.write("\r\n -- Command [" + command[0] + "] ");
        
        if(command[0].equalsIgnoreCase("start")) {           

            session.write(" accepted. Processing...");
            
            // Start protobuff acceptor and the fix connections.
            server.minStart("ExecutionServer.xml");           
            
            session.write("\r\n -- Server successfully started.");
            session.write("\r\n\r\n\\> ");
            return;
        }
        
        if(command[0].equalsIgnoreCase("stop")) {
            
            session.write(" accepted. Processing...");
            
            // Stop protobuff acceptor and the fix connections.
            server.minStop();
            
            session.write("\r\n -- Server successfully stopped.");
            session.write("\r\n\r\n\\> ");
            return;
        }
        
        if(command[0].equalsIgnoreCase("restart")) {            
            
            session.write(" accepted. Processing...");
            
            // Stop protobuff acceptor and the fix connections.
            server.minStop();                
            
            // Start protobuff acceptor and the fix connections.
            server.minStart("ExecutionServer.xml");
            
            session.write("\r\n -- Server successfully restarted.");
            session.write("\r\n\r\n\\> ");
            return;
        }
        
        if(command[0].equalsIgnoreCase("fix")) {
            
            session.write("accepted. Checking parameter... \r\n");
            
            if(command.length < 2) {
                session.write("\r\n -- At least one more parameter is necessary, ignoring...");
                session.write("\r\n\r\n\\> ");
                return;
            }
            
            session.write(" -- Subcommand [" + command[1] + "] ");
            
            if(command[1].equalsIgnoreCase("list")) {                
                
                session.write("accepted. Processing...");
                session.write("\r\n\r\n -- Fix initiators: \r\n");

                // list available fix connections.
                for(String connName : ExecutionServerController.connections.keySet()) {                
                    FixConnection conn = ExecutionServerController.connections.get(connName);                
                    
                    ConnectionInfo info = conn.getInfo();
                    session.write("\r\n\t[" + connName + "] Fix version: " + info.fixVersion + " - Host: " + conn.getHostPort() + " - Logged " + (conn.isLoggedOn()? "on" : "off"));
                }

                session.write("\r\n\r\n\\> ");
                return;
            }
            
            if(command[1].equalsIgnoreCase("start")) {                
                
                session.write("accepted. Processing... \r\n");

                if(command.length < 3) {
                    session.write("\r\n -- A connection name must be informed. Ignoring command...");
                    session.write("\r\n\r\n\\> ");
                    return;
                }
                
                if(command[2].equalsIgnoreCase("all")) {

                    // list available fix connections.
                    for(String connName : ExecutionServerController.connections.keySet()) {                
                        FixConnection conn = ExecutionServerController.connections.get(connName);
                        conn.start();
                        session.write("\r\n\t" + connName + " - started.");
                    }

                    session.write("\r\n\r\n\\> ");
                    return;
                }
                
                ExecutionServerController.connections.get(command[2]).start();
                session.write("\r\n\t" + command[2] + " - started.");
                session.write("\r\n\r\n\\> ");
                return;
            }
            
            if(command[1].equalsIgnoreCase("stop")) {
                
                session.write("accepted. Processing... \r\n");

                if(command.length < 3) {
                    session.write("\r\n -- A connection name must be informed. Ignoring command...");
                    session.write("\r\n\r\n\\> ");
                    return;
                }
                
                if(command[2].equalsIgnoreCase("all")) {

                    // list available fix connections.
                    for(String connName : ExecutionServerController.connections.keySet()) {                
                        FixConnection conn = ExecutionServerController.connections.get(connName);                        
                        conn.stop();
                        session.write("\r\n\t" + connName + " - stopped.");
                    }
                    
                    session.write("\r\n\r\n\\> ");
                    return;
                }
                
                ExecutionServerController.connections.get(command[2]).stop();
                session.write("\r\n\t" + command[2] + " - stopped.");
                session.write("\r\n\r\n\\> ");
                return;
            }
            
            if(command[1].equalsIgnoreCase("restart")) {
                
                session.write("accepted. Processing... \r\n");

                if(command.length < 3) {
                    session.write("\r\n -- A connection name must be informed. Ignoring command...");
                    session.write("\r\n\r\n\\> ");
                    return;
                }
                
                if(command[2].equalsIgnoreCase("all")) {

                    // list available fix connections.
                    for(String connName : ExecutionServerController.connections.keySet()) {                
                        FixConnection conn = ExecutionServerController.connections.get(connName);                        
                        conn.stop();
                        conn.start();
                        session.write("\r\n\t" + connName + " - restarted.");
                    }
                    
                    session.write("\r\n\r\n\\> ");
                    return;
                }
                
                ExecutionServerController.connections.get(command[2]).stop();
                ExecutionServerController.connections.get(command[2]).start();
                session.write("\r\n\t" + command[2] + " - restarted.");
                session.write("\r\n\r\n\\> ");
                return;
            }
            
            if(command[1].equalsIgnoreCase("symbols")) {
                
                session.write("accepted. Processing... \r\n");

                if(command.length < 3) {
                    session.write("\r\n -- A connection name must be informed. Ignoring command...");
                    session.write("\r\n\r\n\\> ");
                    return;
                }
                try {
                    ExecutionServerController.connections.get(command[2]).loadSecurities();
                }
                catch (SessionNotFound ex) {
                    Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                session.write("\r\n\tMessage SecurityListRequest sent to: " + command[2] + ".");
                session.write("\r\n\r\n\\> ");
                return;
            }
            
            // If command could not be recognized.
            session.write("not recognized. Ignoring...");
            session.write("\r\n\r\n\\> ");
            return;
        }
        
        if(command[0].equalsIgnoreCase("help")) {
            session.write("accepted. Processing...");
            session.write("\r\n\r\n -- Server commands: \r\n");
            session.write("\t start     - Start protobuff acceptor and fix connections. \r\n");
            session.write("\t stop      - Stop protobuff acceptor and fix connections. \r\n");
            session.write("\t restart   - Stop and start protobuff acceptor and fix connections. \r\n");
            session.write("\t terminate - Terminate server, including this console acceptor. \r\n");
            session.write("\t quit      - Close this administration connection. \r\n");
            session.write("\r\n -- Connection commands: \r\n");
            session.write("\t fix list            - List all fix connections available. \r\n");
            session.write("\t fix stop [name]     - Stop fix connection of an specific id. \r\n");
            session.write("\t fix start [name]    - Start fix connection of an specific id. \r\n");
            session.write("\t fix restart [name]  - Retart fix connection of an specific id. \r\n");
            session.write("\t fix symbols [name]  - Load securities symbols from an specific connection. \r\n");
            session.write("\r\n\\> ");
            return;
        }
        
        if(command[0].equalsIgnoreCase("quit")) {
            session.write("accepted. Processing...");
            
            // close this session connection.
            session.close();
            return;
        }
        
        if(command[0].equalsIgnoreCase("terminate")) {
            session.write("accepted. Processing...");
            
            // terminate server
            server.stop();
            return;
        }
        
        // If command could not be recognized.
        session.write("not recognized. Ignoring...");
        session.write("\r\n\r\n\\> ");
    }
}