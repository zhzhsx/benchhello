package me.zehua;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.incubator.channel.uring.IOUringChannelOption;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;

/**
 * Thread per core
 */
public class NettyTpcUring {

    public static void main(String[] args) throws Exception {
        int tc = 4;
        ExecutorService executor = Executors.newFixedThreadPool(tc - 1);
        for (int i = 0; i < tc - 1; i++) {
            executor.submit(() -> {
                singleThreaded();
                return null;
            });
        }
        System.out.println("starting server");
        singleThreaded();
    }

    static void singleThreaded() throws InterruptedException {
        EventLoopGroup sharedGroup = new IOUringEventLoopGroup(1);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024)
                    .option(IOUringChannelOption.SO_REUSEPORT, true)
                    .option(IOUringChannelOption.SO_REUSEADDR, true)
                    .group(sharedGroup)
                    .channel(IOUringServerSocketChannel.class)
                    .childHandler(new HttpHelloWorldServerInitializer());
            Channel ch = b.bind("0.0.0.0", 8080).sync().channel();
            ch.closeFuture().sync();
        } finally {
            sharedGroup.shutdownGracefully();
        }
        System.err.println("server stopped");
    }
}