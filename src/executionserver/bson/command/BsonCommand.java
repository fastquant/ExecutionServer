/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package executionserver.bson.command;

import executionserver.mina.BsonHandler;
import java.util.Map;
import org.apache.mina.common.IoSession;

/**
 *
 * @author spquant
 */
public interface BsonCommand {
    public void setHandler(BsonHandler handler);
    public void execute(Map<String, Object> args, IoSession session) throws Exception;
}
