# 核心库使用说明 / [English](README_en_US.md)
## 方式1：手动指定(适用于节点数少，希望或有能力自行维护"进程或节点标识唯一性"的应用)：
```java
//步骤1.初始化(仅需一次，一般即应用启动时)
//datacenterId和workerId被用来唯一标识一个进程or节点，这两者的组合必须是'唯一'的
LocalGuid guid = LocalGuid.init(0/*datacenterId*/, 0/*workerId*/);

//步骤2.获取id (snowflake算法)
//方式1:
Long id1 = guid.next();

//方式2:
Long id2 = LocalGuid.instance().next();

//此外，框架也集成了两种性能优越的UUID算法
//方式1 (FastUUID算法):
String uuid1 = LocalGuid.uuid();

//方式2 (改良过的JDK UUID):
String uuid2 = LocalGuid.uuid(true/*是否以-区隔*/, false/*是否采用极速模式*/);
```

## 方式2(**推荐\***)：结合分布式协调者使用("进程标识唯一性"自动得到维护)：
```java
//步骤1.初始化(仅需一次，采用zookeeper作为分布式协调者)
LocalGuid guid = LocalZkGuid.init("localhost:2181"/*zk地址*/)
//步骤2.获取id(同 '方式1的步骤2'，略)
```

## 方式3(**适用于同网段**)：通过识别本机IP("节点标识唯一性"自动得到维护)或指定IP 来使用：
```java
//步骤1.初始化(仅需一次，一般即应用启动时)
//指定本机IP前缀(多网卡场景，辅助挑选出正确的ip，目前仅支持IPV4)
LocalGuid guid
= LocalGuid.initWithLocalIp("192.168.1");
//或 指定本机IP前缀、同时指定IP段(目前仅支持'第3段')
= LocalGuid.initWithLocalIp("192.168", 1);//在 192.168.1.* 中挑选

//步骤2.获取id(同 '方式1的步骤2'，略)
```

[< 回索引](../README.md)