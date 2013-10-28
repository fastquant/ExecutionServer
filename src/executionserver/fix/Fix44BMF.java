/*
 * Blitz Trading
 */
package executionserver.fix;

import BE.BEOrderUpdate;
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
import quickfix.Message;
import quickfix.field.*;
import quickfix.fix44.*;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class Fix44BMF extends AbstractFixConnection {
    
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());    
    
    @Override
    public void processRequest(ExecutionOrder order) throws SessionNotFound {        
        
        Message msg = null;

        switch (order.getReqType()) {

            case RequestTypes.REQUEST_NEW_ORDER:

                NewOrderSingle message = new NewOrderSingle(
                        new ClOrdID(order.getId()),
                        new Side((char) order.getSide()),
                        new TransactTime(),
                        new OrdType((char) order.getOrderType()));

                message.set(new HandlInst('1'));
                message.set(new Symbol(order.getSecurity()));
                                if(super.securityList != null) {
                    
                String securityId = super.securityList.get(order.getSecurity());
                    
                    if(securityId!=null) {
                        message.set(new SecurityID(securityId));
                    }
                }
                
                message.set(new TimeInForce(getTimeInForce(order.getValidity())));
                message.set(new OrderQty(order.getQty()));
                
                NewOrderSingle.NoAllocs noAllocs = new NewOrderSingle.NoAllocs();
                noAllocs.set(new AllocAccount(order.getAccount()));
                noAllocs.set(new AllocAcctIDSource(99));
                
                message.addGroup(noAllocs);
                
                message.set(new SecurityExchange("XBMF"));
                
                // Add NoPartyIDs repeating group.
                NewOrderSingle.NoPartyIDs noPartyIDsGroup = new NewOrderSingle.NoPartyIDs();

                noPartyIDsGroup.set(new PartyID("114"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(7));

                message.addGroup(noPartyIDsGroup);

                noPartyIDsGroup.set(new PartyID("110"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(36));

                message.addGroup(noPartyIDsGroup);

                noPartyIDsGroup.set(new PartyID("DM3"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(54));
                
                message.addGroup(noPartyIDsGroup);

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
                
                if(order.getExchangeId() == null)  {                    
                    rejectRequest(order);
                }
                
                OrderCancelReplaceRequest replaceMessage = new OrderCancelReplaceRequest(
                        new OrigClOrdID(order.getId()),
                        new ClOrdID(order.getClientId()),
                        new Side((char) order.getSide()),
                        new TransactTime(),
                        new OrdType((char) order.getOrderType())
                        );
                
                replaceMessage.set(new OrderID(order.getExchangeId()));
                
                replaceMessage.set(new HandlInst('1'));
                replaceMessage.set(new Symbol(order.getSecurity()));
                
                if(super.securityList != null) {
                    
                    String securityId = super.securityList.get(order.getSecurity());
                    
                    if(securityId!=null) {
                        replaceMessage.set(new SecurityID(securityId));
                    }
                }
                
                replaceMessage.set(new OrderQty(order.getQty()));
                replaceMessage.set(new Price(order.getPrice()));                
                replaceMessage.set(new TimeInForce(getTimeInForce(order.getValidity())));
                
                noAllocs = new NewOrderSingle.NoAllocs();
                noAllocs.set(new AllocAccount(order.getAccount()));
                noAllocs.set(new AllocAcctIDSource(99));
                
                replaceMessage.addGroup(noAllocs);
                
                replaceMessage.set(new SecurityExchange("XBMF"));
                
                // Add NoPartyIDs repeating group.
                noPartyIDsGroup = new NewOrderSingle.NoPartyIDs();

                noPartyIDsGroup.set(new PartyID("114"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(7));

                replaceMessage.addGroup(noPartyIDsGroup);

                noPartyIDsGroup.set(new PartyID("110"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(36));

                replaceMessage.addGroup(noPartyIDsGroup);

                noPartyIDsGroup.set(new PartyID("DM3"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(54));
                
                replaceMessage.addGroup(noPartyIDsGroup);
                
                msg = (Message) replaceMessage;
                
                insertCustomFields(msg, "G", order);
                
                break;

            case RequestTypes.REQUEST_CANCEL:
                
                if(order.getExchangeId() == null)  {                    
                    rejectRequest(order);
                }
                
                OrderCancelRequest cancelMessage = new OrderCancelRequest(
                        new OrigClOrdID(order.getId()),
                        new ClOrdID(order.getClientId()),                        
                        new Side((char) order.getSide()),
                        new TransactTime());

                cancelMessage.set(new OrderID(order.getExchangeId()));
                
                cancelMessage.set(new Symbol(order.getSecurity()));
                
                if(super.securityList != null) {
                    
                    String securityId = super.securityList.get(order.getSecurity());
                    
                    if(securityId!=null) {
                        cancelMessage.set(new SecurityID(securityId));
                    }
                }
                
                cancelMessage.set(new OrderQty((long) order.getQty()));
                
                cancelMessage.set(new SecurityExchange("XBMF"));
                
                // Add NoPartyIDs repeating group.
                noPartyIDsGroup = new NewOrderSingle.NoPartyIDs();

                noPartyIDsGroup.set(new PartyID("114"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(7));

                cancelMessage.addGroup(noPartyIDsGroup);

                noPartyIDsGroup.set(new PartyID("110"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(36));

                cancelMessage.addGroup(noPartyIDsGroup);

                noPartyIDsGroup.set(new PartyID("DM3"));
                noPartyIDsGroup.set(new PartyIDSource('D'));
                noPartyIDsGroup.set(new PartyRole(54));
                
                cancelMessage.addGroup(noPartyIDsGroup);

                msg = (Message) cancelMessage;
                
                insertCustomFields(msg, "F", order);

                break;
        }

        // send message to target.
        send(msg);
    }

    @Override
    public void fromApp(Message msg, SessionID sid) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

        // Retrieve the message type
        MsgType msgType = new MsgType();        
        msg.getHeader().getField(msgType);
        
        // SecurityList message.
        if(msgType.getValue().equals("y")) {
            NoRelatedSym noRelatedSym = new NoRelatedSym();
            msg.getField(noRelatedSym);
            
            int entries = noRelatedSym.getValue();
            
            SecurityList.NoRelatedSym group = new SecurityList.NoRelatedSym();            
            Symbol symbol = new Symbol();
            SecurityID securityId = new SecurityID();
            
            for(int idx = 1; idx < entries; idx++) {                
                msg.getGroup(idx, group);
                group.get(symbol);
                group.get(securityId);
                
                synchronized(database) {
                    if(database!=null && database.isConnect()) {                    
                        database.insertSymbol(symbol, securityId);
                    }                                
                }
            }
            
            return;
        }
        
        if(msgType.getValue().equals("j")) {
            
            Text rejectReason = new Text();
            if (msg.isSetField(rejectReason)) {
                msg.getField(rejectReason);                
                logger.warn("A business reject reason has been received: " + rejectReason.getValue());
            }
            else {
                logger.warn("A business reject reason has been received.");
            }
            
            return;
        }
        
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
        BEOrderUpdate.OrderUpdate.Builder response = BEOrderUpdate.OrderUpdate.newBuilder();
        
        response.setOrderId(order.getId());        
        response.setQtyRemaining(0);
        response.setSymbol(order.getSecurity());
        response.setSide(order.getSide());
        response.setClientId(order.getClientId());
        response.setAccountId(order.getAccount());

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
            
            synchronized(database) {
                database.saveExchangeId(order, orderId.getValue());
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

                case ExecType.TRADE:

                    response.setMarketOrderId(orderId.getValue());
                    response.setQtyRemaining((long) leavesQty.getValue());
                    response.setQtyExecuted((long) cumQty.getValue());
                    response.setPrice(price.getValue());
                    response.setAvgPrice(avgPrice.getValue());
                    response.setInMarket(false);
                    response.setIsVisible(false);
                    response.setModifiable(false);
                    response.setCancelled(false);
                    
                    OrdStatus ordStatus = new OrdStatus();
                    msg.getField(ordStatus);
                    
                    if(ordStatus.getValue() == OrdStatus.PARTIALLY_FILLED) {
                        response.setStatus((int) OrderStatus.PARTIAL);
                    }
                    else {
                        response.setStatus((int) OrderStatus.FILLED);
                    }

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
                    }

                    Symbol symbol = new Symbol();

                    if (msg.isSetField(symbol)) {
                        msg.getField(symbol);
                    }

                    synchronized(database) {
                        
                        database.updateLasts(order, lastShares.getValue(), lastPx.getValue());

                        if(ordStatus.getValue() == OrdStatus.PARTIALLY_FILLED) {
                            database.updateStatus(order, OrderStatus.PARTIAL);
                        }
                        else {
                            database.updateStatus(order, OrderStatus.FILLED);
                        }
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
                    logger.error("Order Id: " + order.getClientId() + " could not be send back to client.");
                }
            }
        }
    }
    
    @Override
    public void loadSecurities() throws SessionNotFound {        
        
        synchronized(database) {
            database.deleteAllSecurities();
        }
        
        // Prepare SecurityListRequest message.
        SecurityListRequest msg = new SecurityListRequest();
        
        msg.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT));
        msg.set(new SecurityReqID("1"));
        msg.set(new SecurityListRequestType(SecurityListRequestType.ALL_SECURITIES));
        
        insertCustomFields(msg, "x", null);
        
        send(msg);
    }

    private void rejectRequest(ExecutionOrder order) {
        
        IoSession session = ExecutionServerController.clients.get(order.getOwner());
        
        boolean isBson = session.getAttribute("Protocol").equals("BSON");
        
        if(isBson) {
            BasicBSONObject bsonOrder = new BasicBSONObject();

            bsonOrder.put("OrderId", order.getId());
            bsonOrder.put("QtyRemaining", 0);            
            bsonOrder.put("Symbol", order.getSecurity());
            bsonOrder.put("Side", order.getSide());
            bsonOrder.put("ClientId", order.getClientId());
            bsonOrder.put("AccountId", order.getAccount());
            bsonOrder.put("RejectReason", "[ExecutionServer] Order is not in market.");
            bsonOrder.put("Status", OrderStatus.REJECTED);
            
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("Order", bsonOrder);

            BasicBSONObject orderUpdate = new BasicBSONObject();                        
            orderUpdate.put("Handler", "OrderUpdate");

            session.write(orderUpdate);
        }
        else {        
            BEOrderUpdate.OrderUpdate.Builder response = BEOrderUpdate.OrderUpdate.newBuilder();

            response.setOrderId(order.getId());        
            response.setQtyRemaining(0);
            response.setSymbol(order.getSecurity());
            response.setSide(order.getSide());
            response.setClientId(order.getClientId());
            response.setAccountId(order.getAccount());
            response.setRejectReason("[ExecutionServer] Order is not in market.");
            response.setStatus(OrderStatus.REJECTED);
            
            session.write(response.build());
        }
    }
}