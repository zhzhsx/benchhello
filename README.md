# benchhello

local vs network
相同规格服务器的差异，似乎有，之后有空再看一下
内存是16g，因为阿里云这款cpu没有4c8g的型号，考虑hello world不怎么耗内存，后续再考虑这个点。
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
