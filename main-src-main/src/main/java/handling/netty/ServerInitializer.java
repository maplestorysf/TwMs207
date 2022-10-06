/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.netty;

/**
 *
 * @author o黯淡o
 */
import constants.ServerConstants.ServerType;
import handling.MapleServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private final int world;
    private final int channels;
    private final ServerType serverType;
    private MaplePacketDecoder decoder;
    private MaplePacketEncoder encoder;
    private MapleServerHandler handler;

    public ServerInitializer(ServerType serverType, int world, int channels) {
        this.serverType = serverType;
        this.world = world;
        this.channels = channels;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipe = channel.pipeline();
        decoder = new MaplePacketDecoder();
        encoder = new MaplePacketEncoder();
        handler = new MapleServerHandler(serverType, world, channels);
        pipe.addLast("decoder", decoder); // decodes the packet
        pipe.addLast("encoder", encoder); // encodes the packet
        pipe.addLast("handler", handler);
    }

}
