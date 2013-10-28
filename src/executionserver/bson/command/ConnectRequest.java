/*
 * Itaú Asset Management - Quantitative Research Team
 * 
 * @project 
 * @date 
 */

package executionserver.bson.command;

import executionserver.controller.BsonExecutionController;
import executionserver.controller.ExecutionServerController;
import java.util.Map;
import org.apache.mina.common.IoSession;

/**
 *
 * @author Sylvio Azevedo
 */
public class ConnectRequest extends BasicCommand {
    
    @Override
    public void execute(Map<String, Object> args, IoSession session) throws Exception {
        
        this.handler.clientName = (String) args.get("ClientId");
            
        logger.info("Adding a session for client:" + this.handler.clientName);

        BsonExecutionController execCtr = new BsonExecutionController(this.handler.clientName, session);

        session.setAttribute("ExecutionController", execCtr);
        session.setAttribute("Protocol", "BSON");

        ExecutionServerController.clients.put(this.handler.clientName, session);            

        logger.info("[ConnectRequest] received, preparing response.");

        execCtr.sendAvailableConns();
        execCtr.sendOrderList();
        execCtr.startQueueWatcher();
    }
}
