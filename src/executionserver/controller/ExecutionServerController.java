/**
 * Blitz Trading
 */
package executionserver.controller;

import executionserver.domain.Connection;
import executionserver.domain.Settings;
import executionserver.fix.FixConnection;
import executionserver.mina.BsonHandler;
import executionserver.mina.codecs.BsonCodecFactory;
import executionserver.mina.codecs.ProtobufCodecFactory;
import executionserver.mina.codecs.StringCodecFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import javax.xml.bind.JAXBException;
import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server control class.
 * 
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ExecutionServerController implements Runnable {

    // constants
    public static final String SERVER_VERSION = "1.0";
    public static final String LOGGER_CONFIG_FILE = "log4j.properties";
    public static final int BSON_DEFAULT_PORT = 60100;
    public static final int PROTOBUFF_DEFAULT_PORT = 60000;
    public static final int ADMIN_DEFAULT_PORT = 60777;      
    
    // logger
    private final Logger logger = LoggerFactory.getLogger(ExecutionServerController.class);    
    
    // properties    
    private boolean running;
    private Thread server;    
    private IoAcceptor bsonAcceptor;
    private IoAcceptor protobuffAcceptor;
    private IoAcceptor adminAcceptor;
    
    public static int bsonPort = BSON_DEFAULT_PORT;
    public static int protobuffPort = PROTOBUFF_DEFAULT_PORT;
    public static int adminPort = ADMIN_DEFAULT_PORT;    
    public static Settings settings;    
    public static String configPath;    
    public static HashMap<String, FixConnection> connections;
    public static HashMap<String, IoSession> clients;
    
    public ExecutionServerController() { 
        
        // initialize list and maps.        
        connections = new HashMap<String, FixConnection>();
        clients = new HashMap<String, IoSession>();
    }
    
    public void start(String configFilePath) throws JAXBException, FileNotFoundException, IOException, InterruptedException {
        
        // Keep config file path reference.
        configPath = configFilePath;
        
        // Load settings from file
        settings = SettingsController.load(configFilePath);

        // Start execution server thread.
        server = new Thread(this);
        server.start();
        
        // Start administration console acceptor.
        if (settings.adminAcceptor != null && settings.adminAcceptor.port != null) {
            adminPort = Integer.parseInt(settings.adminAcceptor.port);
        }        
        startAdmin();

        running = true;
    }

    public void startProtobuff() throws IOException, InterruptedException {

        protobuffAcceptor = new SocketAcceptor();
        protobuffAcceptor.getFilterChain().addLast("logging", new LoggingFilter());
        protobuffAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtobufCodecFactory()));

        protobuffAcceptor.bind(new InetSocketAddress(protobuffPort), new ProtobufHandler());

        System.out.println("Protobuff accpetor listening on port " + protobuffPort);
    }
    
    private void stopProtobuff() {
        protobuffAcceptor.unbindAll();
    }

    public void startBsonAcceptor() throws IOException, InterruptedException {

        bsonAcceptor = new SocketAcceptor();
        bsonAcceptor.getFilterChain().addLast("logging", new LoggingFilter());
        bsonAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new BsonCodecFactory()));

        bsonAcceptor.bind(new InetSocketAddress(bsonPort), new BsonHandler());

        System.out.println("BSON accpetor listening on port " + bsonPort);
    }
    
    private void stopBsonAcceptor() {
        bsonAcceptor.unbindAll();
    }
    
    private void addAndStart(Connection conn) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        // check if exists a connection with the same name.
        if(connections.containsKey(conn.name)) {
            
            // ingore connection.
            logger.warn("Connection with name [" + conn.name + "] already exists and started. please review the server settings.");
            return;
        }
        
        // start fix connection and put it in control.
        FixConnection fixConn = (FixConnection) Class.forName(conn.impl).newInstance();
        fixConn.start(conn);
        connections.put(conn.name, fixConn);
    }

    public static void orderNotify() {

        for (FixConnection conn : connections.values()) {
            conn.orderNotify();
        }
    }

    private void startAdmin() throws IOException {

        adminAcceptor = new SocketAcceptor();

        adminAcceptor.getFilterChain().addLast("logging", new LoggingFilter());
        adminAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new StringCodecFactory()));

        adminAcceptor.bind(new InetSocketAddress(adminPort), new TextLineHandler(this));

        System.out.println("Administration service accpetor listening on port " + adminPort);
    }
    
    private void stopAdmin() {
        adminAcceptor.unbindAll();
    }

    @Override
    public void run() {
        
        // set log4j configuration file.
        PropertyConfigurator.configure("etc" + File.separator + LOGGER_CONFIG_FILE);
        
        // Start fix connections
        for (Connection conn : settings.connections) {
            try {
                addAndStart(conn);
            }            
            catch (ClassNotFoundException cnfe) {
                logger.error("Could not find class implementation [" + conn.impl + "] :" + cnfe.getMessage());
            } 
            catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }

        // Start Protobuff commands acceptor.
        if (settings.protobuffAcceptor != null && settings.protobuffAcceptor.port != null) {
            protobuffPort = Integer.parseInt(settings.protobuffAcceptor.port);
        }
        try {            
            //startProtobuff();

            // Start Bson commands acceptor.
            startBsonAcceptor();
        } 
        catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }
    
    private void stopFixConnections() {
        
        for (FixConnection conn : connections.values()) {
            conn.stop();
        }
    }
    
    public void minStop() {
        
        stopFixConnections();
        stopProtobuff();        
    }
    
    public void minStart(String configFilePath) {        

        // Start execution server thread.
        server = new Thread(this);
        server.start();        
    }
    
    public void stop() {
        
        stopFixConnections();

        //stopProtobuff();
        
        stopBsonAcceptor();

        stopAdmin();

        running = false;
    
        synchronized(this) {

            this.notify();
        }
        
        System.exit(0);
    }
    
    public static String getServerPath() {
        return System.getProperty("user.dir");
    }
}
