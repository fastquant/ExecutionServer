/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package executionserver.bson.command;

import executionserver.mina.BsonHandler;
import org.slf4j.LoggerFactory;

/**
 *
 * @author spquant
 */
public abstract class BasicCommand implements BsonCommand {
    
    // logger
    protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
    
    protected BsonHandler   handler;    
    
    @Override
    public void setHandler(BsonHandler handler) {
        this.handler = handler;
    }
}
