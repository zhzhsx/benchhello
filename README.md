本工程测试不同语言、框架情况下 hello world HTTP 接口的极限 QPS。

先说结论：Rust 服务器需要客户端 100 并发才能消耗所有 CPU，此时 QPS 是 50 万。也注意到 30 并发 Java 服务器确实比较低。原因可能包括 HTTP Pipeling 实现上的影响、语言 GC 的影响、HTTP 解析效率的影响、Spring 额外功能代码的影响。

# 压测环境和配置

压测时，最好避免本地压本地，这是因为本地 loopback 网卡 kernel 处理不一样，压测程序也和服务器竞争 CPU 资源。[之前踩过坑](https://github.com/tokio-rs/tokio/issues/5010)。

在阿里云北京地域创建两台服务器，作为客户端和服务端。
服务端型号为 ecs.g7.xlarge，配置 4C16G，CPU型号 CPU Model Intel(R) Xeon(R) Platinum 8369B CPU @ 2.70GHz，注意到内存是 16G，因为没 8G 的规格，测试中看到 Java 进程内存消耗在 300 MiB 左右，内存实际不影响 QPS。

客户端型号为 ecs.g7.2xlarge，配置 8C32G，注意到规格较高，避免客户端压测时遇到瓶颈。

服务器间网络带宽为 10Gbps，用 ping 测试时延为 0.08 ms。

服务端系统为 Ubuntu 20.04，内核为 5.15.0，内核参数未调整。压测用 wrk 工具，连接数最好是线程数的倍数，wrk最好一核一线程。

也注意到同型号服务器压测，数据也不一样，可能是底层物理机调度和网络抖动导致。所以这里的值仅为参考。

所有测试的 wrk 输出保存在 `wrk-results.txt` 中。

# Rust 50万 QPS 复现

代码在 rust 目录中。
启用了 HTTP Pipeling，使用了 thread per core 线程模型，用了 jemalloc 内存分配器，hyper 也调优了。

| 配置  | 客户端  |  QPS | 平均时延  | 服务器CPU利用率  |
|---|---|---|---|---|
| Rust   | -t8 -c104   | 563743   | 185us  | ~99%  |
| Rust  | -t8 -c32  | 289072  |  110us |  ~47% |

Rust 情况下，32 并发无法消耗服务端所有 CPU。之前的想法是，HTTP Pipeling 从原理上讲，如果每个请求响应时间差异不大，且 HTTP 并发超过 CPU 核数，就可以压满 CPU，但看来实际不是这样。

![HTTP Pipeling](https://upload.wikimedia.org/wikipedia/commons/1/19/HTTP_pipelining2.svg)

# Java

代码在 reactive 、 reactive-controller  和 mvc 中。

Java 内存消耗都在 300 MiB 左右，远小于服务器内存。
Java 版本 17。Java 需要预热。
平均时延不太准确，但工具没提供99%时延。
压测客户端6线程是因为够用了。

| 配置  | 客户端  |  QPS | 平均时延  | 服务器CPU利用率  |
|---|---|---|---|---|
| undertow  | -t8 -c104   |  474021| 232us  | ~99%  |
| undertow  | -t6 -c30  | 265927  |  112us |  ~55% |
| Netty  | -t8 -c104   |  391129| 269us  | ~99%  |
| Netty  | -t6 -c30  | 260736  |  114us |  ~47% |
| reactor  | -t8 -c104   |  274250| 1.39ms  | ~99%  |
| reactor  | -t6 -c30  | 220045  |  137us |  ~81% |
| webflux  | -t8 -c104   |  104200| 1.08ms  | ~99%  |
| webflux  | -t6 -c30  | 106116  |  313us |  ~99% |
| mvc  | -t8 -c104   |  88172| 1.18ms  | ~93%  |
| mvc  | -t6 -c30  | 87243  |  352us |  ~93% |

undertow 结果不错。

netty 已经明显下来了，基于 netty 的 reactor 竟然比 netty 差很多，基于 reactor 的 webflux 又差许多。和之前的预估有较大的差异。目前未分析原因细节，可能需要基于 CPU Profiling 和几个项目的代码分析。

mvc 线程模型和webflux不一样，可以想象到结果较差，但差的有点多。可能也有 MVC 代码复杂上的影响。

# 备注

可能的影响因素：
* 有的服务器支持 HTTP Pipeling，有的不支持，wrk 偏好 HTTP Pipeling
* GC的影响
* HTTP解析的影响
* Spring 带来的额外功能的影响
* 线程模型的影响

