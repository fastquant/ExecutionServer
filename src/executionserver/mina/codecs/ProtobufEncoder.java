/*
 * Blitz Trading
 */
package executionserver.mina.codecs;

import BE.BEConnectResponse;
import BE.BEOrderList;
import BE.BEOrderUpdate;
import com.google.protobuf.Message;
import executionserver.domain.MessageHeader;
import executionserver.domain.MessageTypes;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ProtobufEncoder implements ProtocolEncoder {

    @Override
    public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) throws Exception {
        
        ByteBuffer buffer;
        
        if(msg instanceof String) {
            String toSend = (String) msg;                    
            buffer = ByteBuffer.allocate(toSend.length());
            buffer.put(toSend.getBytes());
            buffer.flip();
            
            if(buffer!=null)
            {
                out.write(buffer);
                out.flush();
            }
        }
        else {            
            MessageHeader header = new MessageHeader();            
            Message response = (Message) msg;
            
            header.type = MessageTypes.NONE;
            
            if(msg instanceof BEConnectResponse.ConnectResponse) {
                header.type = MessageTypes.CONNECT_RESPONSE;                
            }
            
            if(msg instanceof BEOrderUpdate.OrderUpdate) {
                header.type = MessageTypes.ORDER_UPDATE;
            }
            
            if(msg instanceof BEOrderList.DataOrderList) {
                header.type = MessageTypes.ORDER_LIST;
            }
            
            if(header.type == MessageTypes.NONE) {                
                return;
            }
                
            header.bodySize = response.toByteArray().length;

            buffer = ByteBuffer.allocate(header.size() + response.toByteArray().length);
            buffer.put(header.toByteArray());
            buffer.put(response.toByteArray());
            buffer.flip();                

            out.write(buffer);
            out.flush();            
        }
    }

    @Override
    public void dispose(IoSession is) throws Exception {
        // nothing to do.
    }    
}