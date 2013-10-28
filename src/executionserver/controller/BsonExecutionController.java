/**
 * Blitz Trading
 */
package executionserver.controller;

import executionserver.domain.ExecutionOrder;
import executionserver.domain.OrderStatus;
import executionserver.domain.RequestTypes;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.mina.common.IoSession;
import org.bson.BasicBSONObject;
import org.slf4j.LoggerFactory;

/**
 * ExecutionServer connection object, controls the server operations with FIX
 * initiator (client).
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class BsonExecutionController {

    // properties
    private String clientName;
    private IoSession session;
    private final DatabaseController database;
    private final Object ordersToProcessMutex = new Object();
    private List<ExecutionOrder> ordersToProcess;
    private boolean running = true;
    private QueueWatcher watcher;
    private Thread watcherThread;
    // logger
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Constructor storing the client name and the connection session.
     *
     * @param clientName Connection client name
     * @param session Connection session reference.
     *
     * @throws UnknownHostException
     */
    public BsonExecutionController(String clientName, IoSession session) throws UnknownHostException {

        this.clientName = clientName;
        this.session = session;

        // instance temporary queue for orders to process.
        ordersToProcess = new ArrayList<ExecutionOrder>();

        // start a connection to mongo database
        database = new DatabaseController();
        database.connect(ExecutionServerController.settings);
    }

    public void startQueueWatcher() {

        // start queue watcher
        watcher = new QueueWatcher();
        watcherThread = new Thread(watcher);
        watcherThread.start();
    }

    public void stopQueueWatcher() {

        running = false;

        synchronized (ordersToProcessMutex) {
            ordersToProcessMutex.notify();
        }
    }

    /**
     * Add a new order in the processing queue
     *
     * @param order Order about be enqueued.
     *
     * @throws InterruptedException
     */
    public void addOrder(ExecutionOrder order) throws InterruptedException {

        // put order in the temporary queue            
        ordersToProcess.add(order);
        
        BasicBSONObject orderUpdate = new BasicBSONObject();
        
        orderUpdate.put("OrderId", order.getId());
        orderUpdate.put("Status", OrderStatus.RECEIVED);
        orderUpdate.put("QtyRemaining", order.getQty());
        
        // creating and sending the order update message.
        Map<String, Object> args = new HashMap<String, Object>();
        
        args.put("Order", orderUpdate);

        // Send message "received" to client.
        BasicBSONObject response = new BasicBSONObject();
        
        response.put("Handler", "OrderUpdate");
        response.put("Args", args);

        session.write(response);

        synchronized (ordersToProcessMutex) {            
            ordersToProcessMutex.notify();
        }
    }

    /**
     * Order rejection method.
     *
     * @param order Rejected order reference.
     * @param message Rejection message.
     */
    private void reject(ExecutionOrder order, String message) {

        logger.warn("Order [" + order.getId() + "] has been rejected with message: " + message);
        
        Map<String, Object> args = new HashMap<String, Object>();
        
        args.put("OrderId", order.getId());
        args.put("Status", OrderStatus.REJECTED);
        args.put("QtyRemaining", order.getQty());
        args.put("RejectReason", message);
        

        // Send message "received" to client.
        BasicBSONObject response = new BasicBSONObject();
        
        response.put("Handler", "OrderUpdate");
        response.put("Args", args);

        session.write(response);
    }

    /**
     * Send all available connections (routings) to the client part.
     */
    public void sendAvailableConns() {
        
        List<String> routes = new ArrayList<String>();
        
        for (String conn : ExecutionServerController.connections.keySet()) {
            routes.add(conn);
        }
        
        Map<String, Object> args = new HashMap<String, Object>();
        
        args.put("Routes", routes);

        // Send message "received" to client.
        BasicBSONObject response = new BasicBSONObject();
        
        response.put("Handler", "ConnectResponse");
        response.put("Args", args);

        session.write(response);
    }

    /**
     * Send a list of orders from a particular client.
     */
    public void sendOrderList() {

        List<ExecutionOrder> orderList = database.findOrdersByOwner(clientName);

        if (orderList.isEmpty()) {
            return;
        }
        
        List<BasicBSONObject> ordersListToSend = new ArrayList<BasicBSONObject>();
        
        for (ExecutionOrder order : orderList) {
            
            BasicBSONObject newOrder = new BasicBSONObject();
            
            newOrder.put("OrderId", order.getClientId());
            newOrder.put("ClientId", order.getClientId());
            newOrder.put("Symbol", order.getSecurity());
            newOrder.put("Side", order.getSide());
            newOrder.put("Type", order.getOrderType());
            newOrder.put("Exchange", order.getExchange());
            newOrder.put("Account", order.getAccount());
            newOrder.put("Validity", order.getValidity());
            newOrder.put("Status", order.getStatus());
            newOrder.put("Price", order.getPrice());
            newOrder.put("Quantity", order.getQty());
            newOrder.put("MinQty", order.getMinQty());
            newOrder.put("OpenQty", order.getOpenQty());
            newOrder.put("EntryTime", 0);
            newOrder.put("StopPx", order.getStopPrice());
            newOrder.put("LastPrice", order.getLastPrice());
            newOrder.put("LastShares", order.getLastShares());
            newOrder.put("Route", order.getRoute());
            newOrder.put("Broker", order.getBroker());
            
            if(order.getRejectReason() != null && order.getRejectReason().isEmpty()) {
                newOrder.put("RejectReason", order.getRejectReason());
            }
            
            ordersListToSend.add(newOrder);
        }
        
        Map<String, Object> args = new HashMap<String, Object>();        
        
        args.put("OrderList", ordersListToSend);

        // Send message "received" to client.
        BasicBSONObject response = new BasicBSONObject();
        
        response.put("Handler", "OrderList");
        response.put("Args", args);

        try {
            // send orders to client.
            session.write(response);
        }
        catch (Exception e) {
            logger.error("Error sending client list order: " + e.getMessage());
        }
    }

    private class QueueWatcher implements Runnable {

        @Override
        public void run() {

            while (running) {

                synchronized (ordersToProcessMutex) {
                    try {
                        ordersToProcessMutex.wait(50);
                    }
                    catch (InterruptedException ex) {
                        logger.warn("Queue watcher has been interrupted. All order will be stucked in the queue: " + ex.getMessage());
                    }
                }

                while (!ordersToProcess.isEmpty()) {

                    // remove the head of the queue
                    ExecutionOrder order = ordersToProcess.remove(0);

                    switch (order.getReqType()) {

                        case RequestTypes.REQUEST_NEW_ORDER:

                            // Check if the appointed fix connection exists.
                            if (ExecutionServerController.connections.get(order.getRoute()) == null) {

                                // Send reject message.
                                reject(order, "[ExecutionServer] Invalid routing name: " + order.getRoute());
                                return;
                            } {
                            // Check if order already exists.
                            if (database.exists(order)) {

                                // Send reject message.
                                reject(order, "[ExecutionServer] Duplicated order id.");
                                continue;
                            }

                            // Send message "accepted" to client.
                            orderUpdate(order.getId(), order.getQty(), OrderStatus.ACCEPTED);                            

                            // Change order status.
                            order.setOrderStatus(OrderStatus.ACCEPTED);

                            database.insert(order);
                        }
                        break;

                        case RequestTypes.REQUEST_REPLACE: {
                            // Retrieve order from database.
                            ExecutionOrder dbOrder = database.find(order.getId());

                            // Check if order exists.
                            if (dbOrder == null) {

                                // Send reject message.
                                reject(order, "[ExecutionServer] Order id not found.");
                                continue;
                            }

                            // Sending status of pending replace.
                            orderUpdate(order.getId(), order.getQty(), OrderStatus.PENDING_REPLACE);
                          
                            // Update order in the database for cancel                    
                            dbOrder.setPrice(order.getPrice());
                            dbOrder.setQty(order.getQty());
                            dbOrder.setReqType(order.getReqType());
                            dbOrder.setStatus(order.getStatus());
                            dbOrder.setOrderStatus(OrderStatus.PENDING_REPLACE);

                            database.update(dbOrder);
                        }
                        break;

                        case RequestTypes.REQUEST_CANCEL: {
                            ExecutionOrder dbOrder = database.find(order.getId());

                            // Check if order exists.
                            if (dbOrder == null) {

                                // Send accepted for bidding message.
                                reject(order, "[ExecutionServer] Order id not found.");
                                continue;
                            }

                            // Check if order is already filled.
                            if (dbOrder.getOrderStatus() == OrderStatus.FILLED) {

                                // Send accepted for bidding message.
                                reject(order, "[ExecutionServer] Order [ " + order.getId() + "] is filled can't be canceled.");
                                continue;
                            }

                            // Check if order is already filled.
                            if (dbOrder.getOrderStatus() == OrderStatus.CANCELED) {

                                // Send accepted for bidding message.
                                reject(order, "[ExecutionServer] Order [ " + order.getId() + "] is already canceled.");

                                continue;
                            }

                            // Sending status of pending cancel.
                            orderUpdate(order.getId(), order.getQty(), OrderStatus.PENDING_CANCEL);                            

                            // Update order in the database for cancel                    
                            dbOrder.setReqType(order.getReqType());
                            dbOrder.setStatus(order.getStatus());
                            dbOrder.setOrderStatus(OrderStatus.PENDING_CANCEL);

                            database.update(dbOrder);
                        }
                        break;
                    }
                }
                ExecutionServerController.orderNotify();
            }
        }
    }
    
    private void orderUpdate(String id, double qtyRemaining, int orderStatus) {
        
        BasicBSONObject order = new BasicBSONObject();
        
        order.put("OrderId", id);
        order.put("Status", orderStatus);
        order.put("QtyRemaining", qtyRemaining);
        
        // creating and sending the order update message.
        Map<String, Object> args = new HashMap<String, Object>();
        
        args.put("Order", order);
        
        BasicBSONObject response = new BasicBSONObject();
        
        response.put("Handler", "OrderUpdate");
        response.put("Args", args);

        session.write(response);
    }
}