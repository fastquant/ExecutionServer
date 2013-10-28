/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package executionserver.mina.codecs;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 *
 * @author spquant
 */
public class BsonCodecFactory implements ProtocolCodecFactory{
    
    // properties
    private BsonEncoder encoder;
    private BsonDecoder decoder;    
    
    public BsonCodecFactory() {
        encoder = new BsonEncoder();
        decoder = new BsonDecoder();
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
