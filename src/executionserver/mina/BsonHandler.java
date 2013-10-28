/*
 * Itaú Asset Management - Quantitative Research Team
 * 
 * @project 
 * @date 
 */

package executionserver.mina;

import executionserver.bson.command.BsonCommand;
import executionserver.controller.ExecutionServerController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo
 */
public class BsonHandler extends IoHandlerAdapter {
    
    // constants
    public static final String BSON_CMD_PACKAGE = "executionserver.bson.command.";
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public String clientName = null;    
    
    @Override
    public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
    {
        logger.error(cause.getMessage());
    }

    @Override
    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        if(!(message instanceof BasicBSONObject)) {        
        
            List args = new ArrayList();
            args.add("-100");
            args.add("Command is not valid bson object.");            
            
            BasicBSONObject bson = new BasicBSONObject();
            bson.put("Handler", "Message");        
            bson.put("Args", args);
            
            return;
        }
     
        // retrieve bson object.
        BasicBSONObject bsonObj = (BasicBSONObject) message;
        
        // retrieve bson command name
        String cmdName = bsonObj.getString("Handler");
        
        // retrieve command arguments
        Map<String, Object> args = (Map<String, Object>) bsonObj.get("Args");
        
        logger.info("Command received [" + cmdName + "]. Processing...");
        
        // instance and execute bson command by reflection.
        BsonCommand cmd = (BsonCommand) Class.forName(BSON_CMD_PACKAGE + cmdName).newInstance();        
        cmd.setHandler(this);        
        cmd.execute(args, session);
    }

    @Override
    public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
    {
        System.out.println( "IDLE " + session.getIdleCount( status ));
    }
    
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        
        logger.info("Session open. Sending server information.");
        
        String sessionId = UUID.randomUUID().toString();        
        
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("ServerInfo", "Execution Server - Version (" + ExecutionServerController.SERVER_VERSION + ") Id: " + sessionId) ;
        
        BasicBSONObject response = new BasicBSONObject();
        response.put("Handler", "SessionOpen");
        response.put("Args", args);
        
        session.setAttribute("id", sessionId);
        session.write(response);
    }
    
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        logger.info("Session created, setting buffer size.");
        
        ((SocketSessionConfig) session.getConfig()).setReceiveBufferSize(1024);
        session.setIdleTime( IdleStatus.BOTH_IDLE, 60 );
    }
}
