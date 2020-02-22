# Stun4J Guid

### 分布式ID生成器 全局唯一、极速、趋势递增、易于使用  / [English](README.md) 


| 稳定版 | JDK版本兼容性 | 发布日期 |
| ------------- | ------------- | ------------|
| 1.0.0  | 1.8+ | 02/?/2020 |


## 功能特性
* 生成全局唯一的ID，适用于分布式环境
* 云原生环境友好，对IP、端口的漂移无感
* 生成速度极快，达到百万级TPS+
* ID趋势递增，基于twitter-snowflake算法，针对时钟回退能够有限自愈
* 定位为迷你型工具类jar包，依赖极少，易于使用和集成

## 快速上手
### 1.直接使用(适用于节点数少，希望或有能力自行维护\"节点标识唯一性\"的应用)：

```
//1.初始化
//0,0分别代表(自定义的)datacenterId和workerId(需自行确保这两者的组合是'唯一'的)
LocalGuid guid = LocalGuid.init(0,0);

//2.获取id
//方式1:
guid.next();
//方式2:
LocalGuid.instance().next();

```

### 2.结合分布式协调者(如Zookeeper)使用(\*推荐\*，\"节点标识唯一性\"会得到自动维护)：

```
//1.初始化
Pair<Integer, Integer> node = ZkGuidNode.start("localhost:2181"/*zk地址*/);
LocalGuid guid = LocalGuid.init(node);
//2.获取id(略、同上)
```

## 注意事项
TBD

## 路线图
* 支持更多分布式协调者 如etcd
* TBD

## 开源许可协议

本项目采用 **Apache Software License, Version 2.0**