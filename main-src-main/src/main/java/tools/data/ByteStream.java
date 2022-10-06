package tools.data;

import java.io.IOException;

public interface ByteStream {

    public long getPosition();

    public void seek(final long offset) throws IOException;

    public int readByte();

    public String toString();

    public String toString(final boolean b);

    public long available();

}
