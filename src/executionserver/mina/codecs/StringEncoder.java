/*
 * Blitz Trading
 */
package executionserver.mina.codecs;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class StringEncoder implements ProtocolEncoder {

    @Override
    public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) throws Exception {
        
        ByteBuffer buffer;        
        
        String toSend = (String) msg;                    
        buffer = ByteBuffer.allocate(toSend.length());
        buffer.put(toSend.getBytes());
        buffer.flip();
            
        if(buffer!=null) {
            out.write(buffer);
            out.flush();
        }
    }

    @Override
    public void dispose(IoSession is) throws Exception {
        // nothing to do.
    }
}
