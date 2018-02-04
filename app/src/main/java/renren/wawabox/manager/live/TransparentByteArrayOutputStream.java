package renren.wawabox.manager.live;

import java.io.ByteArrayOutputStream;

/**
 * Created by xunwang on 2017/9/19.
 */

public class TransparentByteArrayOutputStream extends ByteArrayOutputStream {
    public TransparentByteArrayOutputStream(int size) {
        super(size);
    }
    public byte[] getBuffer(){
        return buf;
    }
    public int getBufferSize(){
        return count;
    }
}
