/**
 *Blitz Trading
 */
package executionserver.fix;

import executionserver.controller.CustomisationController;
import executionserver.controller.DatabaseController;
import executionserver.controller.ExecutionServerController;
import executionserver.domain.Command;
import executionserver.domain.Connection;
import executionserver.domain.ConnectionInfo;
import executionserver.domain.Customisation;
import executionserver.domain.ExecutionOrder;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.ExecTransType;
import quickfix.field.MsgType;
import quickfix.field.RefTagID;
import quickfix.field.Text;
import quickfix.field.TimeInForce;

/**
 * Abstract class with default fix connection and processing operations.
 * 
 * @author Sylvio Azevedo
 */
public abstract class AbstractFixConnection implements FixConnection, Application {
    
    // class logger.
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());    
    
    // configuration object of fix customisation fields.
    protected Customisation customisation = null;        
    
    // fix security list ids.
    protected Map<String, String> securityList = null;
    
    // configuration object of fix connection.
    protected Connection conn = null;
    
    // Fix connection information
    protected ConnectionInfo info;
    
    // mina protobuf session id.
    private SessionID sessionId;
    
    // mina protobuf session settings.
    private SessionSettings settings;
    
    // process requests operation handlers.
    private ProcessRequests processMessages;
    private Thread prt;    
    
    // mongo database handler, where order status machine is persisted.
    public final DatabaseController database = new DatabaseController();
    
    // fix initiator handler.
    private Initiator initiator;
    
    // variable for sequence number id generation.
    private int nextId = 1;
    
    
    /**
     * Start - FixConnection override.
     * 
     * Connect to mongo database, load customisation fix fields, establish 
     * initiator connection into the broker side and start order processing
     * engine.
     * 
     * @param conn Connection settings.
     */
    @Override
    public void start(Connection conn) {
        
        // keep connection settings inside.
        this.conn = conn;
        
        try {                
            // Open a database connection.
            database.connect(ExecutionServerController.settings);
        } 
        catch (UnknownHostException ex) {
            logger.error("Could not connect into database: " + ex.getMessage());            
        }   
        
        try {
            // check if a customisation file was set.
            if(conn.customFile!=null) {
                
                try {
                    // load customisation fields for each command.
                    customisation = CustomisationController.load(conn.customFile);
                } 
                catch (JAXBException ex) {
                    logger.error("Error parsing file [" + conn.customFile + "]: " + ex.getMessage());
                } 
                catch (FileNotFoundException ex) {
                    logger.error("Customisation file could not be found: " + ex.getLocalizedMessage());
                }
            }
            
            // initialise connection to the broker side.
            settings = new SessionSettings(conn.configFile);
            
            MessageStoreFactory stf = new FileStoreFactory(settings);
            LogFactory lf = new FileLogFactory(settings);
            MessageFactory mf = new DefaultMessageFactory();
            
            initiator = new SocketInitiator(this, stf, settings, lf, mf);            
            initiator.start();
            
            Properties defaultProps = settings.getDefaultProperties();
            
            Iterator it = settings.sectionIterator();            
            
            // keep sessionid reference inside.
            if(it.hasNext()) {
                sessionId = (SessionID) it.next();
            }
            
            // Start messages processing thread
            processMessages = new ProcessRequests(this, conn);
            prt = new Thread(processMessages);
            prt.start();
            
            // instance and set connection information object
            info = new ConnectionInfo();            
            info.name = conn.name;            
            info.fixVersion = sessionId.getBeginString();
            info.dictionary = defaultProps.getProperty("UseDataDictionary").equalsIgnoreCase("Y") ? true: false;
            info.heartBitInterval = Integer.valueOf(defaultProps.getProperty("HeartBtInt"));
            info.hostname = defaultProps.getProperty("SocketConnectHost");
            info.port = Integer.valueOf(defaultProps.getProperty("SocketConnectPort")); 
            info.resetOnLogon = defaultProps.getProperty("ResetOnLogon").equalsIgnoreCase("Y") ? true: false;
            info.senderCompId = sessionId.getSenderCompID();
            info.targetCompId = sessionId.getTargetCompID();
        } 
        catch (ConfigError ex) {
            logger.error("Could not initialise and start the fix connection [" + conn.name + "] :" + ex.getMessage());            
            //ex.printStackTrace();
        }
        
        // try to load security list from database
        securityList = database.findAllSecurities();
    }

    /**
     * Stop - FixConnection override.
     * 
     * Stop initiator connection and the order processing engine.
     */   
    @Override
    public void stop() {
        
        /**
         * Stop initiator connection.
         */
        if(initiator != null) {
            initiator.stop();
            initiator = null;
        }
        
        // signalize thread to stop.
        processMessages.running = false;        
        this.orderNotify();
        
        // Wait a sec to thread finalize.
        try {
            prt.join(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(AbstractFixConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // If the thread is still alive.
        if(prt.isAlive()) {
            
            // force interruption.
            prt.interrupt();
        }
    }

    /**
     * processRequest - FixConnection override
     * 
     * Order processing operation. It must be implemented by each FIX version
     * concrete class.
     * 
     * @param order Order about to be processed.
     * @throws SessionNotFound 
     */
    @Override
    public void processRequest(ExecutionOrder order) throws SessionNotFound {
        logger.error("No implementation to handle this order execution");
    }

    /**
     * onCreated - Application override
     * 
     * On new fix session creation.
     * 
     * @param sid FIX session id.
     */
    @Override
    public void onCreate(SessionID sid) {        
    }
    
    /**
     * onLogon - Application override
     * 
     * On logon to broker side fix acceptor.
     * 
     * @param sid FIX session id.
     */
    @Override
    public void onLogon(SessionID sid) {
        
        synchronized(database) {
         
            // try to send old stuck messages.
            List<ExecutionOrder> stuckOrders = database.findStuck(conn);
        
            // check if there are any stuck messages.
            if(stuckOrders == null) {
                return;
            }
            
            // process each one.
            for(ExecutionOrder curr: stuckOrders) {

                try {
                    this.processRequest(curr);
                } 
                catch (SessionNotFound ex) {
                    logger.error("Error processing order: " + curr.getId() + ", error: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * onLogoff - Application override
     * 
     * On logoff from broker side fix acceptor.
     * 
     * @param sid FIX session id.
     */
    @Override
    public void onLogout(SessionID sid) {
        
    }

    /**
     * toAdmin - Application override.
     * 
     * Intercepts administration messages that will be sent to broker side.
     * 
     * @param msg Message handler
     * @param sid Fix session id.
     */
    @Override
    public void toAdmin(Message msg, SessionID sid) {        
        
        try {
            // retrieve message type.
            MsgType msgType = new MsgType();
            msg.getHeader().getField(msgType);
            
            // Check if it is a logon type [A]
            if(msgType.getValue().equals("A")) {
                
                // Insert custom fields related to logon message, if they exist.
                insertCustomFields(msg, "A", null);
            }
        }
        catch (FieldNotFound ex) {
            logger.error(ex.getMessage());            
        }
    }

    /**
     * fromAdmin - Application override
     * 
     * Receive fix administration messages from broker side.
     * 
     * @param msg Fix message handler
     * @param sid Fix session id
     * 
     * @throws FieldNotFound
     * @throws IncorrectDataFormat
     * @throws IncorrectTagValue
     * @throws RejectLogon 
     */
    @Override
    public void fromAdmin(Message msg, SessionID sid) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {                
        
        // Retrieve the message type
        MsgType msgType = new MsgType();        
        msg.getHeader().getField(msgType);        
        
        // Retrieve transaction type
        ExecTransType execTransType = new ExecTransType();
        if (msg.isSetField(execTransType)) {
            msg.getField(execTransType);
        }
        
        /**
         * Check if message type is and reject.
         *
         * 3 - Reject
         */
        if (msgType.getValue().equals("3")) {

            // Retrieve reason and send to client.
            Text rejectReason = new Text();
            if (msg.isSetField(rejectReason)) {
                msg.getField(rejectReason);
                
                RefTagID refTagId = new RefTagID();
                msg.getField(refTagId);
                
                // Retrieve related tag
                if(msg.isSetField(refTagId)) {
                    logger.error("An order has been reject by server: " + rejectReason + " Tag ID [" + refTagId.getValue() + "].");
                }
                else {
                   logger.error("An order has been reject by server: " + rejectReason);
                }
                
            } else {
                logger.error("[ExecutionServer] A reject was sent by market without a clear reason.");
            }
        }
    }

    /**
     * toAdmin - Application override.
     * 
     * Intercepts application messages that will be sent to broker side.
     * 
     * @param msg Message handler
     * @param sid Fix session id.
     */
    @Override
    public void toApp(Message msg, SessionID sid) throws DoNotSend {
        
    }

    /**
     * fromAdmin - Application override
     * 
     * Receive fix application messages from broker side.
     * 
     * @param msg Fix message handler
     * @param sid Fix session id
     * 
     * @throws FieldNotFound
     * @throws IncorrectDataFormat
     * @throws IncorrectTagValue
     * @throws RejectLogon 
     */
    @Override
    public void fromApp(Message msg, SessionID sid) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    }
    
    /**
     * send
     * 
     * Send a message to class referenced session id.
     * 
     * @param msg Message about to be sent.
     * 
     * @throws SessionNotFound 
     */
    protected void send(Message msg) throws SessionNotFound {        
        
        Session.sendToTarget(msg, sessionId);
    }

    /**
     * orderNotify - FixConnection override.
     * 
     * Notify that a new order has been received and is ready to be processed, 
     * or that this Fix connection is about to be stopped.     
     */
    @Override
    public void orderNotify() {
        
        synchronized (database) {
            database.notify();
        }
    }

    /**
     * start - FixConnection override.
     * 
     * Start object with stored settings references.
     */
    @Override
    public void start() {
        
        if(this.conn != null && initiator==null) {
            this.start(this.conn);
        }
    }

    /**
     * loadSecurities - FixConnection override.
     * 
     * Send a security list message to load the symbols of BMF market.
     * 
     * @throws SessionNotFound 
     */
    @Override
    public void loadSecurities() throws SessionNotFound {
        logger.error("No implementation to handle this.");
    }

    /**
     * getHostPort - FixConnection
     * 
     * Retrieve server host name and protobuf connection port.
     * 
     * @return hostname:port
     */
    @Override
    public String getHostPort() {
        
        try {
            return settings.getString("SocketConnectHost") + ":" + settings.getString("SocketConnectPort");
        }
        catch (Exception ex) {
            Logger.getLogger(AbstractFixConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return "Error loading connection settints.";
    }

    /**
     * Retrieve connection information.
     * 
     * @return Object ConnectInfo
     */
    @Override
    public ConnectionInfo getInfo() {
        
        return info;
    }
    
    /**
     * Order requests processing engine.
     */
    private class ProcessRequests implements Runnable {
        
        // running flag.
        private boolean running = true;
        
        // Connection points handlers.
        Connection connCfg;
        FixConnection conn;
        
        /**
         * Constructor. 
         * 
         * Keep connection references.
         * @param conn
         * @param connCfg 
         */
        public ProcessRequests(FixConnection conn, Connection connCfg) {            
            
            this.conn = conn;
            this.connCfg = connCfg;
        }        
        
        /**
         * Thread entry-point
         */
        @Override
        public void run() {
            
            // check database connection, leaves, if it not established.
            if(!database.isConnect()) {
                return;
            }
            
            try {
                
                // main loop
                while(true) {

                    synchronized(database) {

                        // waits until a new order comes.
                        while(database.noNewOrders(connCfg)) {                            
                            
                            database.wait();
                            
                            // check if notify is a termination command.
                            if(!running) {
                                logger.info("Shutdown command invoked. Exiting...");
                                return;
                            }
                        }
                        
                        // If Fix connection is no longer logged, ignore order.
                        if(!isLoggedOn()) {
                            continue;
                        }

                        // Pull one order from database
                        ExecutionOrder order = database.getOne(connCfg);
                        
                        // Process order.                        
                        conn.processRequest(order);
                        
                        // mark as processed
                        database.mark(order);
                    }             
                }
            }
            catch(Exception e){
                logger.error("The requests processing engine has been forcely interrupted: " + e.getMessage());
            }
        }
    }
    
    /**
     * insertCustomFields
     * 
     * Insert customization fields within a Fix message.
     * 
     * @param msg Fix message reference
     * @param cmdType Fix message type
     */
    protected void insertCustomFields(Message msg, String cmdType, ExecutionOrder order) {
    
        // check if there is any customisation for this communication
        if (customisation == null) {
            return;
        }
        
        for (Command cmd : customisation.commands) {                        

            if(!cmd.type.equals(cmdType)) {
                continue;                  
            }
            
            if(cmd.fields != null) {
                for(executionserver.domain.Field field : cmd.fields) {
                    
                    if(order !=null && field.value.startsWith("[") && field.value.endsWith("]")) {
                        
                        String value;
                        String property = field.value.replaceAll("\\[", "");
                        property = property.replaceAll("\\]", "");
                        
                        try {
                            value = BeanUtils.getProperty(order, property);                        
                        } 
                        catch (Exception ex) {                         
                            logger.error("Can't retrieve property [" + property + "] from order: " + ex.getMessage());
                            continue;
                        }
                        
                        if(value == null) {
                            logger.error("Can't retrieve property [" + property + "] from order.");
                            continue;
                        }
                            
                        msg.setField(new StringField(field.number, value));                        
                    }
                    else {                    
                        msg.setField(new StringField(field.number, field.value));
                    }
                }
            }
            
            
            if(cmd.groups != null) {
            
                for(executionserver.domain.Group group: cmd.groups) {

                    Group newGroup = new Group(group.number, group.fields.get(0).number);

                    if(group.fields != null) {
                    
                        for(executionserver.domain.Field field : group.fields) {
                            
                             if(order !=null && field.value.startsWith("[") && field.value.endsWith("]")) {
                        
                                String value;
                                String property = field.value.replaceAll("\\[", "");
                                property = property.replaceAll("\\]", "");

                                try {
                                    value = BeanUtils.getProperty(order, property);                        
                                } 
                                catch (Exception ex) {                         
                                    logger.error("Can't retrieve property [" + property + "] from order: " + ex.getMessage());
                                    continue;
                                }
                                
                                if(value == null) {
                                    logger.error("Can't retrieve property [" + property + "] from order.");
                                    continue;
                                }

                                newGroup.setField(new StringField(field.number, value));
                            }
                            else {                    
                                newGroup.setField(new StringField(field.number, field.value));
                            }
                        }

                        msg.addGroup(newGroup);
                    }
                }
            }
        }
    }
    
    /**
     * isLoggedOn - FixConnection override
     * 
     * @return if a Fix session is logged on.
     */
    @Override
    public boolean isLoggedOn() {     
        return initiator !=null && initiator.isLoggedOn();
    }
    
    /**
     * Generate a serial client order id.
     * 
     * @param clientId
     * @return brand new client order id.
     */
    protected String generateId(String clientId) {
        
        return clientId + ":" + nextId++;
    }
    
    
    protected char getTimeInForce(int value) {
        
        switch(value) {
            case 0:
                return TimeInForce.DAY;
                
            case 1:
                return TimeInForce.GOOD_TILL_CANCEL;
                
            case 2:
                return TimeInForce.AT_THE_OPENING;
                
            case 3:
                return TimeInForce.IMMEDIATE_OR_CANCEL;
                
            case 4:
                return TimeInForce.FILL_OR_KILL;
                
            case 5:
                return TimeInForce.GOOD_TILL_CROSSING;
                
            case 6:
               return TimeInForce.GOOD_TILL_DATE;
                       
            case 7:
                return TimeInForce.AT_THE_CLOSE;
                
            default:
                return TimeInForce.DAY;
        }
    }
}