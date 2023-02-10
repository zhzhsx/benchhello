package me.zehua;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

/**
 * 基于 Spring 出品的 Reactor 框架的 HTTP hello world 服务。
 */
public class ReactorMain {
    public static void main(String[] args) {
        // 压测环境实测，1+5 相对 1+4 QPS略微高
        LoopResources loop = LoopResources.create("event-loop", 1, 5, true);

        HttpServer server = HttpServer.create()
                .host("0.0.0.0")
                .port(8080)
                .accessLog(false)
                .runOn(loop, true)
                .route(routes -> routes.get("/hello",
                        (request, response) -> {
                            // System.out.println("====" + Thread.currentThread().getName());
                            return response.sendString(Mono.just("hello"));
                        }));
                // .route(routes -> routes.route(r -> true,
                //         (request, response) -> {
                //             // System.out.println("====" + Thread.currentThread().getName());
                //             return response.sendString(Mono.just("hello"));
                //         }));
        server.warmup().block();
        DisposableServer s = server.bindNow();
        System.out.println("starting server");
        s.onDispose().block();
    }
}
