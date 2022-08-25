# 语义定制-核心库如何使用 / [English](README_en_US.md)
## 方式1：手动指定(适用于节点数少，希望或有能力自行维护"进程或节点标识唯一性"的应用)：
```java
//步骤1.初始化(仅需一次，一般即应用启动时)
/*
 * datacenterId和workerId 被用来唯一标识一个进程or节点，这两者的组合必须是'唯一'的
 * digits 表示所生成id的数值位数，比如id=456789，那么digits即为6(主要影响'id长度')
 * dcIdBits 表示id中datacenterId因子所占据的二进制比特位数(主要影响'datacenterId的取值') 
 * wkIdBits 表示id中workerId因子所占据的二进制比特位数(主要影响'workerId的取值')
 * seqBits 表示id中sequence因子所占据的二进制比特位数(主要影响'id生成的tps')
 * fixedDigitsEnabled 表示id长度是否固定
 *   比如digits=15时
 *   如fixedDigitsEnabled=true，那么id'固定'为15位
 *   如fixedDigitsEnabled=false，那么id'最长'为15位
 */
LocalGuid guid = LocalGuid.init(0/*datacenterId*/, 0/*workerId*/, 
15/*digits*/, 
4/*dcIdBits*/, 4/*wkIdBits*/, 
3/*seqBits*/, 
true/*fixedDigitsEnabled*/);

//步骤2.获取id (snowflake算法)
//方式1:
Long id1 = guid.next();

//方式2:
Long id2 = LocalGuid.instance().next();

//此外，框架也集成了两种性能优越的UUID算法
//略...
```

## 方式2(**推荐\***)：结合分布式协调者使用("进程标识唯一性"自动得到维护)：
```java
//步骤1.初始化(仅需一次，采用zookeeper作为分布式协调者)
//语义定制说明 见'方式1'
LocalGuid guid = LocalZkGuid.init("localhost:2181"/*zk地址*/,
15/*digits*/, 
4/*dcIdBits*/, 4/*wkIdBits*/, 
3/*seqBits*/, 
true/*fixedDigitsEnabled*/)

//步骤2.获取id(同 '方式1的步骤2'，略)
```

## 方式3(**适用于同网段**)：通过识别本机IP("节点标识唯一性"自动得到维护)或指定IP 来使用：
```java
//步骤1.初始化(仅需一次，一般即应用启动时)
//指定本机IP前缀(多网卡场景，辅助挑选出正确的ip，目前仅支持IPV4)
/*
 * shortDcWkIdBitsEnabled 表示dcIdBits和wkIdBits是否启用精简模式
 *   如shortDcWkIdBitsEnabled=true，那么dcIdBits和wkIdBits均为4
 *   如shortDcWkIdBitsEnabled=false，那么dcIdBits和wkIdBits均为5
 *   
 * 其它语义定制说明 见'方式1'
 */
LocalGuid guid
= LocalGuid.initWithLocalIp(15/*digits*/, 
true/*shortDcWkIdBitsEnabled*/,
3/*seqBits*/, 
true/*fixedDigitsEnabled*/, "192.168.1");

//或 指定本机IP前缀、同时指定IP段(目前仅支持'第3段')
= LocalGuid.initWithLocalIp(15/*digits*/, 
true/*shortDcWkIdBitsEnabled*/, 
3/*seqBits*/, 
true/*fixedDigitsEnabled*/, "192.168", 1);//在 192.168.1.* 中挑选

//步骤2.获取id(同 '方式1的步骤2'，略)
```
# 
[< 回索引](../../README.md)