/**
 * Blitz Trading
 */
package executionserver.controller;

import BE.BEConnectResponse;
import BE.BEOrderList;
import BE.BEOrderUpdate;
import executionserver.domain.ExecutionOrder;
import executionserver.domain.OrderStatus;
import executionserver.domain.RequestTypes;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.apache.mina.common.IoSession;
import org.slf4j.LoggerFactory;

/**
 * ExecutionServer connection object, controls the server operations with FIX
 * initiator (client).
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ExecutionController {

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
    public ExecutionController(String clientName, IoSession session) throws UnknownHostException {

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

        // Send message "received" to client.
        BEOrderUpdate.OrderUpdate response = BEOrderUpdate.OrderUpdate.newBuilder()
                .setQtyRemaining((long) order.getQty())
                .setStatus(OrderStatus.RECEIVED)
                .setOrderId(order.getId())
                .build();

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

        BEOrderUpdate.OrderUpdate response = BEOrderUpdate.OrderUpdate.newBuilder()
                .setQtyRemaining((long) order.getQty())
                .setStatus(OrderStatus.REJECTED)
                .setOrderId(order.getId())
                .setRejectReason(message)
                .build();

        session.write(response);
    }

    /**
     * Send all available connections (routings) to the client part.
     */
    public void sendAvailableConns() {

        // prepare connect response object.
        BEConnectResponse.ConnectResponse.Builder response = BEConnectResponse.ConnectResponse.newBuilder();

        for (String conn : ExecutionServerController.connections.keySet()) {
            response.addRouteName(conn);
        }

        session.write(response.build());
    }

    /**
     * Send a list of orders from a particular client.
     */
    public void sendOrderList() {

        List<ExecutionOrder> orderList = database.findOrdersByOwner(clientName);

        if (orderList.isEmpty()) {
            return;
        }

        BEOrderList.DataOrderList.Builder response = BEOrderList.DataOrderList.newBuilder();

        for (ExecutionOrder order : orderList) {

            BEOrderList.DataOrder.Builder newOrder = BEOrderList.DataOrder.newBuilder();

            newOrder.setOrderId(order.getClientId());
            newOrder.setClientId(order.getClientId());
            newOrder.setSymbol(order.getSecurity());
            newOrder.setSide(order.getSide());
            newOrder.setType(order.getOrderType());
            newOrder.setExchange(order.getExchange());
            newOrder.setAccountId(order.getAccount());
            newOrder.setValidity(order.getValidity());
            newOrder.setStatus(order.getOrderStatus());
            newOrder.setPrice(order.getPrice());
            newOrder.setQuantity((long) order.getQty());
            newOrder.setMinqty((long) order.getMinQty());
            newOrder.setOpenqty((long) order.getOpenQty());
            newOrder.setEntrytime(0);
            newOrder.setStoppx(order.getStopPrice());
            newOrder.setLastPrice(order.getLastPrice());
            newOrder.setLastShares(order.getLastShares());
            newOrder.setRoute(order.getRoute());
            newOrder.setBroker(order.getBroker());

            if (newOrder.hasRejectReason()) {
                newOrder.setRejectReason(order.getRejectReason());
            }

            response.addOrder(newOrder);
        }

        try {
            // send orders to client.
            session.write(response.build());
        } catch (Exception e) {
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

                            // Send message "received" to client.
                            BEOrderUpdate.OrderUpdate response = BEOrderUpdate.OrderUpdate.newBuilder()
                                    .setQtyRemaining((long) order.getQty())
                                    .setStatus(OrderStatus.ACCEPTED)
                                    .setOrderId(order.getId())
                                    .build();

                            session.write(response);

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

                            // Send accepted for bidding message.
                            BEOrderUpdate.OrderUpdate response = BEOrderUpdate.OrderUpdate.newBuilder()
                                    .setQtyRemaining((long) order.getQty())
                                    .setStatus(OrderStatus.PENDING_REPLACE)
                                    .setOrderId(order.getId())
                                    .build();

                            session.write(response);

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

                            // Send accepted for bidding message.
                            BEOrderUpdate.OrderUpdate response = BEOrderUpdate.OrderUpdate.newBuilder()
                                    .setQtyRemaining((long) order.getQty())
                                    .setStatus(OrderStatus.PENDING_CANCEL)
                                    .setOrderId(order.getId())
                                    .build();

                            session.write(response);

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
}