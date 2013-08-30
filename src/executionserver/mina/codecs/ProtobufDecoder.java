/*
 * Blitz Trading
 */
package executionserver.mina.codecs;

import BE.BEConnectRequest;
import BE.BEOrderRequest;
import com.google.protobuf.Message;
import executionserver.domain.MessageHeader;
import executionserver.domain.MessageTypes;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class ProtobufDecoder extends CumulativeProtocolDecoder {    
    
    // logger
    private final Logger logger = LoggerFactory.getLogger(ProtobufDecoder.class);
    
    // properties
    private boolean newMessage = true;
    private MessageHeader header;
    private ByteArrayOutputStream baos;
    private int totalRead = 0;
    
    @Override
    protected boolean doDecode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {        
               
        InputStream is = in.asInputStream();
            
        byte[] buffer = new byte[1024];

        if (baos == null) {
            baos = new ByteArrayOutputStream();
        }

        if (newMessage) {

            try {
                int read = is.read(buffer, 0, 12 - totalRead);
                baos.write(buffer, 0, read);
                totalRead += read;
            } 
            catch (Exception e) {
                logger.error("Error reading protobuf stream: " + e.getMessage());
                return false;
            }

            // check if message is complete
            if (totalRead != 12) {
                return false;
            }

            header = new MessageHeader();
            header.load(baos.toByteArray());

            totalRead = 0;
            baos.reset();
        }

        buffer = new byte[1024];

        try {
            int read = is.read(buffer, 0, header.bodySize - totalRead);
            baos.write(buffer, 0, read);
            totalRead += read;
        } catch (Exception e) {
            logger.error("Error reading protobuf stream: " + e.getMessage());
            return false;
        }

        // check if message is complete
        if (totalRead != header.bodySize) {
            newMessage = false;
            return false;
        }
        Message msg;

        try {
            switch (header.type) {

                case MessageTypes.CONNECT:
                    msg = BEConnectRequest.ConnectRequest.parseFrom(baos.toByteArray());
                    break;

                case MessageTypes.ORDER_REQUEST:
                    msg = BEOrderRequest.OrderRequest.parseFrom(baos.toByteArray());
                    break;

                default:
                    return false;
            }
        }
        catch (Exception e) {
            logger.error("FORMAT EXCEPTION: " + e.getMessage());
            return false;
        }

        if (msg != null) {
            out.write(msg);
        }

        // set flag for a new message
        newMessage = true;
        totalRead = 0;
        baos = new ByteArrayOutputStream();
        
        return true;
    }
}