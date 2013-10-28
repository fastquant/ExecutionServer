/**
 * Blitz Trading
 */
package executionserver.fix;

import BE.BEOrderUpdate;
import BE.BEOrderUpdate.OrderUpdate.Builder;
import executionserver.controller.ExecutionServerController;
import executionserver.domain.ExecutionOrder;
import executionserver.domain.OrderStatus;
import executionserver.domain.RequestTypes;
import java.util.HashMap;
import java.util.Map;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.bson.BasicBSONObject;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class Fix42 extends AbstractFixConnection {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void processRequest(ExecutionOrder order) throws SessionNotFound {

        Message msg = null;

        switch (order.getReqType()) {

            case RequestTypes.REQUEST_NEW_ORDER:

                NewOrderSingle message = new NewOrderSingle(
                        new ClOrdID(order.getId()),
                        new HandlInst('1'),
                        new Symbol(order.getSecurity()),
                        new Side((char) order.getSide()),
                        new TransactTime(),
                        new OrdType((char) order.getOrderType()));

                message.set(new TimeInForce(getTimeInForce(order.getValidity())));
                message.set(new OrderQty(order.getQty()));
                message.set(new Account(order.getAccount()));

                switch (order.getOrderType()) {
                    case OrdType.ON_CLOSE:
                    case OrdType.MARKET:
                    case OrdType.MARKET_WITH_LEFTOVER_AS_LIMIT:
                        break;

                    case OrdType.STOP_LIMIT:

                        if (order.getStopPrice() > 0) {
                            message.set(new StopPx(order.getStopPrice()));
                        }

                        break;

                    default:
                        message.set(new Price(order.getPrice()));
                }

                if (order.getMinQty() > 0) {
                    message.set(new MinQty(order.getMinQty()));
                }
                else {
                    message.set(new MinQty(0));
                }

                if (order.getOpenQty() > 0) {
                    message.set(new MaxFloor(order.getOpenQty()));
                }
                else {
                    message.set(new MaxFloor(0));
                }

                msg = (Message) message;

                insertCustomFields(msg, "D", order);
                
                break;

            case RequestTypes.REQUEST_REPLACE:
                
                database.changeId(order, generateId(order.getClientId()));
                
                OrderCancelReplaceRequest replaceMessage = new OrderCancelReplaceRequest(
                        new OrigClOrdID(order.getLastId()),
                        new ClOrdID(order.getId()),
                        new HandlInst('1'),
                        new Symbol(order.getSecurity()),
                        new Side((char) order.getSide()),
                        new TransactTime(),
                        new OrdType((char) order.getOrderType())
                        );
                
                replaceMessage.set(new OrderQty(order.getQty()));
                replaceMessage.set(new Price(order.getPrice()));
                replaceMessage.set(new Account(order.getAccount()));
                replaceMessage.set(new TimeInForce(getTimeInForce(order.getValidity())));
                
                msg = (Message) replaceMessage;
                
                insertCustomFields(msg, "G", order);
                
                break;

            case RequestTypes.REQUEST_CANCEL:
                
                database.changeId(order, generateId(order.getClientId()));

                OrderCancelRequest cancelMessage = new OrderCancelRequest(
                        new OrigClOrdID(order.getLastId()),
                        new ClOrdID(order.getId()),
                        new Symbol(order.getSecurity()),
                        new Side((char) order.getSide()),
                        new TransactTime());

                cancelMessage.set(new OrderQty((long) order.getQty()));
                cancelMessage.set(new Account(order.getAccount()));

                msg = (Message) cancelMessage;
                
                insertCustomFields(msg, "F", order);

                break;
        }

        // send message to target.
        send(msg);
    }

    @Override
    public void fromApp(Message msg, SessionID sid) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

        // Retrieve transaction type
        ExecTransType execTransType = new ExecTransType();
        if (msg.isSetField(execTransType)) {
            msg.getField(execTransType);
        }

        // Retrieve order id.
        ClOrdID clOrdId = new ClOrdID();
        if (msg.isSetField(clOrdId)) {
            msg.getField(clOrdId);
        }

        // Try to find order with related retrieved id.
        ExecutionOrder order;
        
        synchronized(database) {
             order = database.find(clOrdId.getValue());
        }

        // Check retrieval
        if (order == null) {
            logger.error("Order with id [" + clOrdId.getValue() + "] could not be found at database, there is nothing to do.");
            return;
        }

        // Prepare Order update response.
        Builder response = BEOrderUpdate.OrderUpdate.newBuilder();

        response.setOrderId(order.getClientId());
        response.setQtyRemaining(0);
        response.setSymbol(order.getSecurity());
        response.setSide(order.getSide());
        response.setClientId(order.getOwner());
        response.setAccountId(order.getAccount());

        MsgType msgType = new MsgType();
        msg.getHeader().getField(msgType);

        logger.info(msgType.getValue());

        /**
         * Check if message type is and reject.
         *
         * 9 - OrderCancelReject
         */
        if (msgType.getValue().equals("9")) {

            // Retrieve reason and send to client.
            Text rejectReason = new Text();
            if (msg.isSetField(rejectReason)) {
                msg.getField(rejectReason);
                response.setRejectReason(rejectReason.getValue());
            } else {
                response.setRejectReason("[ExecutionServer] A reject was sent by market without a clear reason.");
            }

            response.setStatus(OrderStatus.REJECTED);
        }

        /**
         * Check if message type is an execution report.
         *
         * 8 - ExecutionReport
         */
        if (msgType.getValue().equals("8")) {

            ExecType execType = new ExecType();
            msg.getField(execType);

            OrderID orderId = new OrderID();
            if (msg.isSetField(orderId)) {
                msg.getField(orderId);
            }

            LeavesQty leavesQty = new LeavesQty();
            if (msg.isSetField(leavesQty)) {
                msg.getField(leavesQty);
            }

            CumQty cumQty = new CumQty();
            if (msg.isSetField(cumQty)) {
                msg.getField(cumQty);
            }

            Price price = new Price();
            if (msg.isSetField(price)) {
                msg.getField(price);
            }

            AvgPx avgPrice = new AvgPx();
            if (msg.isSetField(avgPrice)) {
                msg.getField(avgPrice);
            }

            switch (execType.getValue()) {
                case ExecType.PENDING_NEW:
                case ExecType.PENDING_CANCEL:
                case ExecType.PENDING_REPLACE:
                    return;

                case ExecType.NEW:
                    response.setMarketOrderId(orderId.getValue());
                    response.setQtyRemaining((long) leavesQty.getValue());
                    response.setInMarket(true);
                    response.setIsVisible(true);
                    response.setModifiable(true);
                    response.setCancelled(false);
                    response.setStatus(OrderStatus.NEW);

                    synchronized(database) {
                        database.updateStatus(order, OrderStatus.NEW);
                    }
                    break;

                case ExecType.REJECTED:
                    String reason = "[ExecutionServer] Order request rejected without aparent reason.";

                    Text rejectReason = new Text();
                    if (msg.isSetField(rejectReason)) {
                        msg.getField(rejectReason);
                        reason = rejectReason.getValue();
                    }

                    response.setInMarket(false);
                    response.setRejectReason(reason);
                    response.setStatus(OrderStatus.REJECTED);

                    synchronized(database) {
                        database.updateStatus(order, OrderStatus.REJECTED);
                    }
                    break;

                case ExecType.SUSPENDED:
                    response.setStatus(OrderStatus.REJECTED);
                    response.setInMarket(false);
                    response.setRejectReason("Suspended");

                    synchronized(database) {
                        database.updateStatus(order, OrderStatus.REJECTED);
                    }

                    break;

                case ExecType.PARTIAL_FILL:

                    response.setQtyRemaining((long) leavesQty.getValue());
                    response.setQtyExecuted((long) cumQty.getValue());

                    response.setPrice(price.getValue());
                    response.setAvgPrice(avgPrice.getValue());

                    response.setInMarket(true);
                    response.setIsVisible(true);
                    response.setModifiable(true);
                    response.setCancelled(false);
                    response.setStatus(OrderStatus.PARTIAL);

                    LastPx lastPx = new LastPx();
                    if (msg.isSetField(lastPx)) {
                        msg.getField(lastPx);
                        response.setLastPrice(lastPx.getValue());
                    }

                    LastShares lastShares = new LastShares();
                    if (msg.isSetField(lastShares)) {
                        msg.getField(lastShares);
                        response.setLastShares((long) lastShares.getValue());
                    }

                    ExecID execID = new ExecID();

                    if (msg.isSetField(execID)) {
                        msg.getField(execID);
                        response.setMarketOrderId(execID.getValue());
                    } else {
                        response.setMarketOrderId(orderId.getValue());
                    }

                    Symbol symbol = new Symbol();
                    if (msg.isSetField(symbol)) {
                        msg.getField(symbol);
                        response.setSymbol(symbol.toString());
                    }

                    synchronized(database) {
                        database.updateStatus(order, OrderStatus.PARTIAL);                    
                        database.updateLasts(order, lastShares.getValue(), lastPx.getValue());
                    }
                    break;

                case ExecType.FILL:

                    response.setMarketOrderId(orderId.getValue());
                    response.setQtyRemaining((long) leavesQty.getValue());
                    response.setQtyExecuted((long) cumQty.getValue());
                    response.setPrice(price.getValue());
                    response.setAvgPrice(avgPrice.getValue());
                    response.setInMarket(false);
                    response.setIsVisible(false);
                    response.setModifiable(false);
                    response.setCancelled(false);
                    response.setStatus(OrderStatus.FILLED);

                    lastPx = new LastPx();
                    if (msg.isSetField(lastPx)) {
                        msg.getField(lastPx);
                        response.setLastPrice(lastPx.getValue());
                    }

                    lastShares = new LastShares();

                    if (msg.isSetField(lastShares)) {
                        msg.getField(lastShares);
                        response.setLastShares((long) lastShares.getValue());
                    }

                    execID = new ExecID();
                    if (msg.isSetField(execID)) {
                        msg.getField(execID);
                    }

                    symbol = new Symbol();

                    if (msg.isSetField(symbol)) {
                        msg.getField(symbol);
                    }

                    synchronized(database) {
                        database.updateStatus(order, OrderStatus.FILLED);
                        database.updateLasts(order, lastShares.getValue(), lastPx.getValue());
                    }
                    break;


                case ExecType.CANCELED:

                    response.setStatus(OrderStatus.CANCELED);
                    response.setCancelled(true);
                    response.setInMarket(false);

                    synchronized(database) {
                        database.updateStatus(order, OrderStatus.CANCELED);
                    }
                    break;

                case ExecType.REPLACE:

                    response.setStatus(OrderStatus.REPLACED);
                    response.setInMarket(true);
                    response.setQtyRemaining((long) leavesQty.getValue());
                    
                    synchronized(database) {
                        database.updateStatus(order, OrderStatus.REPLACED);
                    }
                    break;

                default:
                    return;
            }

            IoSession session = ExecutionServerController.clients.get(order.getOwner());
            
            boolean isBson = session.getAttribute("Protocol").equals("BSON");
            
            synchronized(database) {
                try{                    
                    WriteFuture fut;
                    
                    if(isBson) {
                        
                        BasicBSONObject bsonOrder = new BasicBSONObject();
                        
                        bsonOrder.put("AccountId", response.getAccountId());
                        bsonOrder.put("AvgPrice", response.getAvgPrice());
                        bsonOrder.put("ClientId", response.getClientId());
                        bsonOrder.put("LastPrice", response.getLastPrice());
                        bsonOrder.put("LastShares", response.getLastShares());
                        bsonOrder.put("MarketOrderId", response.getMarketOrderId());
                        bsonOrder.put("OrderId", response.getOrderId());
                        bsonOrder.put("Price", response.getPrice());
                        bsonOrder.put("QtyExecuted", response.getQtyExecuted());
                        bsonOrder.put("QtyRemaining", response.getQtyRemaining());
                        bsonOrder.put("Qty", response.getQuantity());
                        bsonOrder.put("RejectReason", response.getRejectReason());
                        bsonOrder.put("Side", response.getSide());
                        bsonOrder.put("Status", response.getStatus());
                        bsonOrder.put("StopPrice", response.getStoppx());
                        bsonOrder.put("Symbol", response.getSymbol());
                        bsonOrder.put("Type", response.getType());
                        
                        Map<String, Object> args = new HashMap<String, Object>();
                        args.put("Order", bsonOrder);
                        
                        BasicBSONObject orderUpdate = new BasicBSONObject();                        
                        orderUpdate.put("Handler", "OrderUpdate");
                        orderUpdate.put("Args", args);
                        
                        fut = session.write(orderUpdate);                        
                    }
                    else {                    
                        fut = session.write(response.build());                                                
                    }
                        
                    fut.join();
                    
                    if(fut.isWritten()) {                                
                        database.markSent(order);                   
                    }
                    else {
                        database.markProblem(order);
                    }
                }
                catch(Exception e) {                    
                    database.markProblem(order);                    
                    logger.error("Order Id: " + order.getClientId() + " could not be send back to client:" + e.getMessage());
                }
            }
        }
    }
}