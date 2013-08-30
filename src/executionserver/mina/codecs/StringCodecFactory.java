/*
 * Blitz Trading
 */
package executionserver.mina.codecs;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class StringCodecFactory implements ProtocolCodecFactory {

    private StringEncoder encoder;
    private StringDecoder decoder;
    
    public StringCodecFactory() {
        encoder = new StringEncoder();
        decoder = new StringDecoder();
    }
    
    @Override
    public ProtocolEncoder getEncoder() throws Exception {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder() throws Exception {
        return decoder;
    }
    
}
