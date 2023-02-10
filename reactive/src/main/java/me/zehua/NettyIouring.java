package me.zehua;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;

public class NettyIouring {
    public static void main(String[] args) throws Exception {
        // boss & woker 使用同一个 group
        EventLoopGroup sharedGroup = new IOUringEventLoopGroup(4);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024)
                    .group(sharedGroup)
                    .channel(IOUringServerSocketChannel.class)
                    .childHandler(new HttpHelloWorldServerInitializer());
            Channel ch = b.bind("0.0.0.0", 8080).sync().channel();

            System.out.println("starting server");

            ch.closeFuture().sync();
        } finally {
            sharedGroup.shutdownGracefully();
        }
    }
}
