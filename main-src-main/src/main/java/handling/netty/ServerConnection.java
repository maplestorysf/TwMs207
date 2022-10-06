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
import constants.ServerConfig;
import constants.ServerConstants.ServerType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ServerConnection {

    private final int port;
    private int world = -1;
    private int channels = -1;
    private final ServerType serverType;
    private ServerBootstrap boot;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1); // The initial connection thread where all the
    // new connections go to
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(); // Once the connection thread has finished it
    // will be moved over to this group where the
    // thread will be managed
    private Channel channel;
    private ServerInitializer serverinitializer;

    public ServerConnection(ServerType serverType, int port) {
        this.serverType = serverType;
        this.port = port;
    }

    public ServerConnection(ServerType serverType, int port, int world, int channels) {
        this.serverType = serverType;
        this.port = port;
        this.world = world;
        this.channels = channels;
    }

    public void run() {
        try {
            serverinitializer = new ServerInitializer(this.serverType, world, channels);
            boot = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, ServerConfig.USER_LIMIT)
                    .childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(serverinitializer);
            try {
                channel = boot.bind(ServerConfig.IP, port).sync().channel().closeFuture().channel();
            } catch (InterruptedException e) {
                System.out.println("綁定" + channel.remoteAddress() + "出錯 : " + e);
            } finally {
                // System.out.println("Listening to port: " + port);
            }
        } catch (Exception e) {
            System.out.println("Connection to failed:" + e);
            // Shut down all event loops to terminate all threads.
            close();
        }
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
