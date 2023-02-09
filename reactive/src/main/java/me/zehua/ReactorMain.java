package me.zehua;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

public class ReactorMain {
    public static void main(String[] args) {
        // java http 链接越少，qps越高 (30 vs 100)
        LoopResources loop = LoopResources.create("event-loop", 1, 4, true);

        HttpServer server = HttpServer.create()
                .host("0.0.0.0")
                .port(8080)
                .accessLog(false)
                .runOn(loop)
                .route(routes -> routes.get("/hello",
                        (request, response) -> {
                            // System.out.println("====" + Thread.currentThread().getName());
                            return response.sendString(Mono.just("hello"));
                        }));
        server.warmup().block();
        DisposableServer s = server.bindNow();
        System.out.println("starting server");
        s.onDispose().block();
    }
}
