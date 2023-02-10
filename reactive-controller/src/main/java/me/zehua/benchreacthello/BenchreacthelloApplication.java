package me.zehua.benchreacthello;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class BenchreacthelloApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchreacthelloApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> monoRouterFunction() {
        return route()
                .GET(BenchreacthelloApplication::hello)
                .build();
    }

    // hello by handler
    public static Mono<ServerResponse> hello(ServerRequest request) {
        return ServerResponse.ok().body(Mono.just("hello"), String.class);
    }
}

/**
 * hello by annotation
 */
@RestController
class HelloController {
	@GetMapping("hello-anno")
	public String hello() {
		return "hello";
	}
}