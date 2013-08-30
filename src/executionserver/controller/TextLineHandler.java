/*
 * Blitz Trading
 */
package executionserver.controller;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class TextLineHandler extends IoHandlerAdapter {
    
    private ExecutionServerController server;
    
    public TextLineHandler(ExecutionServerController server) {
        this.server = server;
    }
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public void exceptionCaught( IoSession session, Throwable cause ) throws Exception {
        logger.error(cause.getMessage());
    }
    
    @Override
    public void messageReceived( IoSession session, Object message ) throws Exception {
        
        new AdminController(server).parse((String) message, session);
    }
    
    @Override
    public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
    {
        System.out.println( "IDLE " + session.getIdleCount( status ));
    }
    
    @Override
    public void sessionOpened(IoSession session) throws Exception {        
        
        session.write("Execution Server - Version (" + ExecutionServerController.SERVER_VERSION + "): \r\n");
        session.write("------------------------------------------------------------------------------ \r\n");
        session.write(" -- Protobuffer acceptor listen on port: " + ExecutionServerController.protobuffPort + "\r\n");
        session.write(" -- Administration acceptor listen on port: " + ExecutionServerController.adminPort + "\r\n");
        session.write(" -- Server path: " + ExecutionServerController.getServerPath() + "\r\n");
        session.write(" -- Server configuration file path: " + ExecutionServerController.configPath + "\r\n");
        session.write("------------------------------------------------------------------------------ \r\n");
        session.write("Enter [help] to see all available commands: \r\n");        
        session.write("\r\n\\> ");
    }
}