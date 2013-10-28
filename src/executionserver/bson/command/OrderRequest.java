/*
 * Itaú Asset Management - Quantitative Research Team
 * 
 * @project ExecutionServer
 * @date 15/10/2013
 */

package executionserver.bson.command;

import executionserver.controller.BsonExecutionController;
import executionserver.controller.ExecutionServerController;
import executionserver.domain.ExecutionOrder;
import executionserver.domain.Market;
import executionserver.domain.ProcessingStatus;
import java.util.Calendar;
import java.util.Map;
import org.apache.mina.common.IoSession;
import org.bson.BasicBSONObject;

/**
 *
 * @author Sylvio Azevedo
 */
public class OrderRequest extends BasicCommand {

    @Override
    public void execute(Map<String, Object> args, IoSession session) throws Exception {

        if (this.handler.clientName == null) {
            // Not connected with a valid client.
            return;
        }

        BasicBSONObject order = (BasicBSONObject) args.get("Order");

        logger.info("Order [" + order.getString("OrderId") + "] received.");

        ExecutionOrder newOrder = new ExecutionOrder();

        newOrder.setId(order.getString("OrderId"));
        newOrder.setAccount(order.getString("Account"));
        newOrder.setClientId(order.getString("OrderId"));
        newOrder.setBroker(order.getString("Broker"));
        newOrder.setExchange(order.getString("Exchange"));
        newOrder.setMinQty(order.getDouble("MinQty"));
        newOrder.setOpenQty(order.getDouble("OpenQty"));
        newOrder.setPrice(order.getDouble("Price"));
        newOrder.setQty(order.getDouble("Qty"));
        newOrder.setSide(order.getInt("Side"));
        newOrder.setStopPrice(order.getDouble("StopPrice"));
        newOrder.setSecurity(order.getString("Security"));
        newOrder.setOrderType(order.getInt("OrderType"));
        newOrder.setValidity(order.getInt("Validity"));
        newOrder.setOwner(this.handler.clientName);
        newOrder.setReqType(order.getInt("ReqType"));
        newOrder.setStatus(ProcessingStatus.NEW);
        newOrder.setDateCreated(Calendar.getInstance().getTime());        

        if (order.keySet().contains("SecurityId")) {
            newOrder.setSecurityId(order.getString("SecurityId"));
        }

        if (order.keySet().contains("SecurityIdSource")) {
            newOrder.setSecurityIdSource(order.getString("SecurityIdSource"));
        }

        if (order.keySet().contains("SecurityExchange")) {
            newOrder.setSecurityExchange(order.getString("SecurityExchange"));
        }

        if (order.keySet().contains("LastPrice")) {
            newOrder.setLastPrice(order.getDouble("LastPrice"));
        }

        if (order.keySet().contains("Portfolio")) {
            newOrder.setPortfolio(order.getString("Portfolio"));
        }

        if (order.getString("Route").isEmpty()) {

            /**
             * Check the market settings and try to select the default route.
             */
            for (Market market : ExecutionServerController.settings.markets) {
                if (market.name.trim().equals(newOrder.getExchange().trim())) {
                    newOrder.setRoute(market.conn);
                }
            }
        } 
        else {
            newOrder.setRoute(order.getString("Route"));
        }

        BsonExecutionController controller = (BsonExecutionController) session.getAttribute("ExecutionController");

        controller.addOrder(newOrder);
    }
}