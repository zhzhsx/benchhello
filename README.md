本工程测试不同语言、框架情况下 hello world HTTP 接口的极限 QPS。

先说结论：Rust 服务器需要客户端 100 并发才能消耗所有 CPU，此时 QPS 是 50 万。也注意到 30 并发 Java 服务器确实比较低，下面一步步分析。

# 压测环境和配置

压测时，最好避免本地压本地，这是因为本地 loopback 网卡 kernel 处理不一样，压测程序也和服务器竞争 CPU 资源。[之前踩过坑](https://github.com/tokio-rs/tokio/issues/5010)。

阿里云北京地域创建两台服务器，作为客户端和服务端。
服务端型号为 ecs.g7.xlarge，配置 4C16G，CPU型号 CPU Model Intel(R) Xeon(R) Platinum 8369B CPU @ 2.70GHz，注意到内存是 16G，因为没 8G 的规格，下面测试时注意程序的内存使用。
客户端型号为 ecs.g7.2xlarge，配置 8C32G，注意到规格较高，避免客户端压测时遇到瓶颈。
服务器间网络带宽为 10Gbps，用 ping 测试时延为 0.08 ms。

服务端系统为 Ubuntu 20.04，内核为 5.15.0，内核参数未调整。压测用 wrk 工具，连接数最好是线程数的倍数。

也注意到同型号服务器压测，数据也不一样，可能是底层物理机调度和网络抖动导致。所以这里的值仅为参考。

# Rust 50万 QPS 复现

| 配置  | 客户端  |  QPS | 平均时延  | 服务器CPU利用率  |
|---|---|---|---|---|
| Rust   | -t8 -c104   | 563743   | 185us  | ~99%  |
| Rust  | -t8 -c32  | 289072  |  110us |  ~47% |
|   |   |   |   |   |

```
wrk -t8 -c104 -d20 http://172.28.172.48:8080/hello
Running 20s test @ http://172.28.172.48:8080/hello
  8 threads and 104 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   185.99us  113.76us   5.46ms   88.85%
    Req/Sec    70.83k     6.55k   88.69k    65.92%
  11331179 requests in 20.10s, 464.67MB read
Requests/sec: 563743.67
Transfer/sec:     23.12MB
#####
wrk -t8 -c32 -d20 http://172.28.172.48:8080/
hello
Running 20s test @ http://172.28.172.48:8080/hello
  8 threads and 32 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   110.23us   58.81us   6.60ms   99.64%
    Req/Sec    36.32k   804.43    39.32k    68.16%
  5810233 requests in 20.10s, 238.27MB read
Requests/sec: 289072.73
Transfer/sec:     11.85MB
```


# Java Netty & Undertow

| 配置  | 客户端  |  QPS | 平均时延  | 服务器CPU利用率  |
|---|---|---|---|---|
| Netty+Epoll   | -t8 -c104   | 563743   | 185us  | ~99%  |
| Netty+Epoll+Thread per core  | -t8 -c32  | 289072  |  110us |  ~47% |
| Netty+Iouring+  |   |   |   |   |
# Java Reactor Http

# Spring webflux

# Spring MVC



客户端 -c需要是核数

```
wrk -d20 -t4 -c100 http://172.16.56.122:8080/hello
Running 20s test @ http://172.16.56.122:8080/hello
  4 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   213.68us  242.48us  15.93ms   98.04%
    Req/Sec   113.61k    26.25k  183.42k    52.00%
  9059329 requests in 20.10s, 371.50MB read
Requests/sec: 450729.84
Transfer/sec:     18.48MB
```
```
wrk -d20 -t4 -c100 http://172.16.56.120:8080/hello
Running 20s test @ http://172.16.56.120:8080/hello
  4 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   183.58us  160.61us   9.79ms   97.07%
    Req/Sec   127.48k    18.89k  190.05k    76.09%
  10181183 requests in 20.10s, 417.51MB read
Requests/sec: 506542.62
Transfer/sec:     20.77MB
```

## about 500k

no pipelining & 30
```
wrk -d20 -t3 -c30 http://172.16.56.122:8080/hello
Running 20s test @ http://172.16.56.122:8080/hello
  3 threads and 30 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   118.97us   29.77us   3.99ms   90.06%
    Req/Sec    82.46k     2.95k   88.74k    66.67%
  4947443 requests in 20.10s, 202.88MB read
Requests/sec: 246150.47
Transfer/sec:     10.09MB
```
cpu idle: ~ 40%

no pipelining & 100
```
wrk -d20 -t4 -c100 http://172.16.56.122:8080/hello
Running 20s test @ http://172.16.56.122:8080/hello
  4 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   213.79us  119.75us   7.33ms   85.72%
    Req/Sec   112.56k    27.31k  159.06k    57.46%
  8995369 requests in 20.10s, 368.88MB read
Requests/sec: 447537.85
Transfer/sec:     18.35MB
```
cpu idle: ~ 7%, sys 44%, user 18%

pipelining &30
```
wrk -d20 -t3 -c30 http://172.16.56.123:8080/hello
Running 20s test @ http://172.16.56.123:8080/hello
  3 threads and 30 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   117.76us   21.39us   2.44ms   74.22%
    Req/Sec    83.08k     3.30k   87.68k    66.50%
  4983711 requests in 20.10s, 204.37MB read
Requests/sec: 247948.49
Transfer/sec:     10.17MB
```
pipelining & 100
```
wrk -d20 -t4 -c100 http://172.16.56.122:8080/hello
Running 20s test @ http://172.16.56.122:8080/hello
  4 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   213.68us  242.48us  15.93ms   98.04%
    Req/Sec   113.61k    26.25k  183.42k    52.00%
  9059329 requests in 20.10s, 371.50MB read
Requests/sec: 450729.84
Transfer/sec:     18.48MB
```


java reactor 1+4 threads; jvm需要预热
30
```
wrk -d20 -t3 -c30 http://172.16.56.123:8080/hello
Running 20s test @ http://172.16.56.123:8080/hello
  3 threads and 30 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   133.48us   50.85us   4.40ms   97.51%
    Req/Sec    74.78k     1.87k   78.39k    62.02%
  4485998 requests in 20.10s, 183.96MB read
Requests/sec: 223183.41
Transfer/sec:      9.15MB
```
idle: ~18
100
```
wrk -d20 -t4 -c100 http://172.16.56.123:8080/hello
Running 20s test @ http://172.16.56.123:8080/hello
  4 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   357.86us  149.96us   9.77ms   85.27%
    Req/Sec    70.06k     2.82k   78.28k    65.80%
  5603902 requests in 20.10s, 229.80MB read
Requests/sec: 278806.21
Transfer/sec:     11.43MB
```
idle:0.4


似乎1+5 qps最高， 287开头


reactor controller, 30比100还好一些
30
```
wrk -d20 -t3 -c30 http://172.16.56.123:8080/hello
Running 20s test @ http://172.16.56.123:8080/hello
  3 threads and 30 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   323.32us   83.66us   3.89ms   85.27%
    Req/Sec    31.15k     1.11k   33.22k    60.36%
  1868837 requests in 20.10s, 147.93MB read
Requests/sec:  92976.92
Transfer/sec:      7.36MB
```
100
```
wrk -d20 -t4 -c100 http://172.16.56.123:8080/hello
Running 20s test @ http://172.16.56.123:8080/hello
  4 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.17ms    0.95ms  26.17ms   98.46%
    Req/Sec    22.94k     2.19k   23.96k    96.02%
  1835360 requests in 20.10s, 145.28MB read
Requests/sec:  91313.24
Transfer/sec:      7.23MB
```
user cpu 占用比较高，可能是MessageConverter这类的耗时？ idle 0.3, 
