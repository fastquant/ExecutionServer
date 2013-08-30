/*
 * Blitz Trading
 */
package executionserver.mina.codecs;

import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ProtobufCodecFactory implements ProtocolCodecFactory {

    private ProtocolEncoder encoder;
    private ProtocolDecoder decoder;
    
    public ProtobufCodecFactory() {
        this.encoder = new ProtobufEncoder();
        this.decoder = new ProtobufDecoder();
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
