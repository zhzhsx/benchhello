use std::convert::Infallible;

use hyper::server::conn::Http;
use hyper::service::service_fn;
use hyper::{Body, Request, Response, StatusCode};
use tikv_jemallocator::Jemalloc;
use tokio::net::TcpSocket;
use tokio::runtime::Builder;
#[global_allocator]
static GLOBAL: Jemalloc = Jemalloc;

async fn hello(req: Request<Body>) -> Result<Response<Body>, Infallible> {
    let path = req.uri().path();
    match path {
        "/hello" => Ok(Response::new(Body::from("hello"))),
        _ => {
            let mut not_found = Response::default();
            *not_found.status_mut() = StatusCode::NOT_FOUND;
            Ok(not_found)
        }
    }
}

fn main() {
    let cpu_core = 4;
    for _ in 0..cpu_core - 1 {
        std::thread::spawn(move || {
            single_threaded_server();
        });
    }
    single_threaded_server();
}

fn single_threaded_server() {
    let rt = Builder::new_current_thread().enable_all().build().unwrap();
    rt.block_on(async {
        let addr = "0.0.0.0:8080".parse().unwrap();
        let socket = TcpSocket::new_v4().unwrap();
        socket.set_reuseport(true).unwrap();
        socket.set_reuseaddr(true).unwrap();
        socket.bind(addr).unwrap();
        let listener = socket.listen(1024).unwrap();
        loop {
            let (tcp_stream, _) = listener.accept().await.unwrap();
            tokio::task::spawn(async move {
                if let Err(http_err) = Http::new()
                    .pipeline_flush(true)
                    .serve_connection(tcp_stream, service_fn(hello))
                    .await
                {
                    println!("Error while serving HTTP connection: {}", http_err);
                }
            });
        }
    });
}

// multi threaded version
