/*
 * Blitz Trading
 */
package executionserver.controller;

import BE.BEConnectRequest;
import BE.BEOrderRequest;
import executionserver.domain.ExecutionOrder;
import executionserver.domain.Market;
import executionserver.domain.ProcessingStatus;
import java.util.Calendar;
import java.util.UUID;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ProtobufHandler extends IoHandlerAdapter{
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private String clientName = null;
    
    public ProtobufHandler() {                
    }
   
    @Override
    public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
    {
        logger.error(cause.getMessage());
    }

    @Override
    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        if(message instanceof BEConnectRequest.ConnectRequest)
        {   
            BEConnectRequest.ConnectRequest request = (BEConnectRequest.ConnectRequest) message;
            
            clientName = request.getClient();
            
            logger.info("Adding a session for client:" + clientName);
            
            ExecutionController execCtr = new ExecutionController(clientName, session);
            
            session.setAttribute("ExecutionController", execCtr);
            
            ExecutionServerController.clients.put(clientName, session);            
            
            logger.info("[ConnectRequest] received, preparing response.");
            
            execCtr.sendAvailableConns();
            
            execCtr.sendOrderList();
            
            execCtr.startQueueWatcher();
        }
        else
            
        if(message instanceof BEOrderRequest.OrderRequest) {
            
            if(clientName == null) {
                // Not connected with a valid client.
                return;
            }
            
            BEOrderRequest.OrderRequest request = (BEOrderRequest.OrderRequest) message;
            
            logger.info("Order [" + request.getOrderId() + "] received.");            
                    
            ExecutionOrder order = new ExecutionOrder();

            order.setId(request.getOrderId());
            order.setAccount(request.getAccountId());
            order.setClientId(request.getOrderId());
            order.setBroker(request.getBroker());
            order.setExchange(request.getExchange());
            order.setMinQty(request.getMinqty());
            order.setOpenQty(request.getOpenqty());
            order.setPrice(request.getPrice());
            order.setQty(request.getQuantity());
            order.setSide(request.getSide());
            order.setStopPrice(request.getStoppx());
            order.setSecurity(request.getSymbol());
            order.setOrderType(request.getType());
            order.setValidity(request.getValidity());            
            order.setOwner(request.getClientId());
            order.setReqType(request.getReqType());
            order.setStatus(ProcessingStatus.NEW);            
            order.setDateCreated(Calendar.getInstance().getTime());
            
            if(request.hasSecurityId()) {
                order.setSecurityId(request.getSecurityId());
            }
            
            if(request.hasSecurityIdSource()) {
                order.setSecurityIdSource(request.getSecurityIdSource());
            }
            
            if(request.hasSecurityExchange()){
                order.setSecurityExchange(request.getSecurityExchange());
            }
            
            if(request.hasLastPrice()) {
                order.setLastPrice(request.getLastPrice());
            }
            
            if(request.hasPortfolio()) {
                order.setPortfolio(request.getPortfolio());
            }
            
            if(request.getRoute().isEmpty()) {
                
                /**
                 * Check the market settings and try to select the default
                 * route.
                 */
                for(Market market : ExecutionServerController.settings.markets)
                {
                    if(market.name.trim().equals(order.getExchange().trim())) {
                        order.setRoute(market.conn);
                    }
                }
            }
            else {
                order.setRoute(request.getRoute());
            }

            ExecutionController controller = (ExecutionController) session.getAttribute("ExecutionController");

            controller.addOrder(order);
        }
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
        session.setAttribute("id", sessionId);
        session.write("Execution Server - Version (" + ExecutionServerController.SERVER_VERSION + ") Id: " + sessionId);
    }
    
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        logger.info("Session created, setting buffer size.");
        
        ((SocketSessionConfig) session.getConfig()).setReceiveBufferSize(1024);
        session.setIdleTime( IdleStatus.BOTH_IDLE, 60 );
    }
}
