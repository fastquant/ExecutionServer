/*
 * Blitz Trading
 */
package executionserver.domain;

import executionserver.util.ArrayConv;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class MessageHeader {
    
    public int type = -1;
    public int reply = -1;
    public int bodySize = -1;


    public byte[] toByteArray() throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        baos.write(ArrayConv.intToByteArray(this.type));
        baos.write(ArrayConv.intToByteArray(this.reply));
        baos.write(ArrayConv.intToByteArray(this.bodySize));

        return baos.toByteArray();
    }
    
    public int size() {
        return 12;
    }
    
    public void load(byte[] buffer) {
        
        type = ArrayConv.byteArrayToInt(Arrays.copyOfRange(buffer, 0, 4));        
        
        reply = ArrayConv.byteArrayToInt(Arrays.copyOfRange(buffer, 4, 8));        
        
        bodySize = ArrayConv.byteArrayToInt(Arrays.copyOfRange(buffer, 8, 12));
    }
}

