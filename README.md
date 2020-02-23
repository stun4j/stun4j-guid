# Stun4J Guid

### 分布式ID生成器 全局唯一、极速、趋势递增、易于使用  / [English](README_en.md) 


| 稳定版 | JDK版本兼容性 | 发布日期 |
| ------------- | ------------- | ------------|
| 1.0.0  | 1.8+ | 02/?/2020 |


## 功能特性
* 生成全局唯一的ID，适用于分布式环境(以进程为最小节点粒度，在单机/伪集群中照常工作)
* 云原生环境友好，对IP、端口的漂移无感
* 生成速度极快，达到百万级TPS+
* ID趋势递增，基于twitter-snowflake算法，针对时钟回退能够有限自愈
* 制品为袖珍型jar包，依赖极少，易于使用和集成

## 快速上手
### 1.直接使用(适用于节点数少，希望或有能力自行维护\"进程or节点的标识唯一性\"的应用)：

```
//步骤1.初始化(仅需一次，一般即应用启动时)
//datacenterId和workerId被用来唯一标识一个进程or节点，这两者的组合必须是'唯一'的
LocalGuid guid = LocalGuid.init(0/*datacenterId*/, 0/*workerId*/);

//步骤2.获取id
//方式1:
long id1 = guid.next();
//方式2:
long id2 = LocalGuid.instance().next();

```

### 2.结合分布式协调者使用(\*推荐\*，\"进程or节点的标识唯一性\"会自动得到维护)：

```
//步骤1.初始化(仅需一次，采用zookeeper作为分布式协调者)
Pair<Integer, Integer> node = ZkGuidNode.start("localhost:2181"/*zk地址*/);
LocalGuid guid = LocalGuid.init(node);
//步骤2.获取id(略、同上)
```

## 注意事项
* 本ID生成算法是时间敏感的，所以集群环境务必开启NTP服务(时钟同步)，保障总体正确性和可用性
* 采用[Zookeeper](http://zookeeper.apache.org/)作为分布式协调者时，客户端采用[Curator](http://curator.apache.org/)和ZK进行通信，需要注意Curator和Zookeeper的[兼容性](http://curator.apache.org/zk-compatibility.html)问题
* TBD

## 路线图
* 支持spring boot
* 支持更多分布式协调者 如etcd
* TBD


## 参与
* 报告bugs、给到建议反馈，请提交一个[issue](https://github.com/stun4j/stun4j-guid/issues/new)
* 参与贡献 改进或新功能，请提交pull request并创建一个[issue](https://github.com/stun4j/stun4j-guid/issues/new)以便讨论与进度追踪
* 喜欢本项目，不吝赐:star2: 

## 开源许可协议

本项目采用 **Apache Software License, Version 2.0**