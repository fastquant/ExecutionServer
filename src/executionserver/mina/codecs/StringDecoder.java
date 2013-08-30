/*
 * Blitz Trading
 */
package executionserver.mina.codecs;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class StringDecoder extends CumulativeProtocolDecoder {

    ByteArrayOutputStream baos;
    
    public StringDecoder() {        
        baos = new ByteArrayOutputStream();
    }
    
    @Override
    protected boolean doDecode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
        
        InputStream is = in.asInputStream();        

        byte[] buffer = new byte[4];
        
        int read = is.read(buffer);
        
        if(buffer[0] == '\r' || buffer[0] == '\n') {
            out.write(baos.toString());
            baos.reset();
            return true;
        }
        
        baos.write(buffer, 0, read);
        
        return false;        
    }
}
