# Stun4J Guid
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

### 分布式ID生成器 全局唯一、极速、趋势递增、易于使用  / [English](README_en_US.md) 


| 稳定版 | JDK版本兼容性 | 发布日期 |
| ------------- | ------------- | ------------|
| 1.1.1  | 1.8+ | 04/15/2021 |
| 1.1.0  | 1.8+ | 04/12/2021 |
| 1.0.4  | 1.8+ | 10/30/2020 |


## 功能特性
* 生成全局唯一的ID，适用于分布式环境(以进程为最小工作单元，在单机/伪集群中照常工作)
* 云原生、虚拟环境友好，对IP、端口的漂移无感
* 生成速度极快，轻松达到 **单机百万级的QPS**
* ID趋势递增，基于twitter-snowflake算法，针对时钟回退能够有限自愈
* 制品为袖珍型jar包，依赖极少，易于使用和集成

## 如何获取

### 方式1：从Maven中央仓库获取
在你工程的**pom.xml**中加入如下片段，即可从maven的中央仓库拉取：

```xml
<dependency>
  <groupId>com.stun4j</groupId>
  <artifactId>stun4j-guid</artifactId>
  <version>1.1.1</version>
</dependency>
```


### 方式2：通过源码构建
切到项目根目录，在控制台执行如下maven命令：

	$ mvn clean package

会在target目录中生成 stun4j-guid-VERSION.jar，放入你工程的classpath即可

## 如何使用
### 方式1：直接使用(适用于节点数少，希望或有能力自行维护\"进程标识唯一性\"的应用)：

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

### 方式2(推荐\*)：结合分布式协调者使用(\"进程标识唯一性\"自动得到维护)：

```
//步骤1.初始化(仅需一次，采用zookeeper作为分布式协调者)
LocalGuid guid = LocalZkGuid.init("localhost:2181"/*zk地址*/)
//步骤2.获取id(同上，略)
```

## 注意事项
* 本ID生成算法是时间敏感的，所以集群环境务必开启NTP服务(尽可能做到时钟前向同步)，保障总体正确性和可用性
* 采用[Zookeeper](http://zookeeper.apache.org/)作为分布式协调者时，客户端采用[Curator](http://curator.apache.org/)和ZK进行通信，需要注意Curator和Zookeeper的**兼容性**问题
	* 目前测试下来，Curator **2.13.0** 这个版本的兼容性比较好，可兼容Zookeeper **3.4.10+(server版本)**
	* 如使用**Zookeeper 3.5+(server版本)**，那么至少应搭配Curator **3.3.0+**版本
* 一个集群支持的进程/节点数量的上限是1024，这是经典snowflake算法非常核心的一点，也就是说datacenterId和workerId的取值范围都是 **[**0,31**]**，所以有1024种组合，在本框架的实现中也充分映射了这个概念，比如对分布式协调者一个namespace下参与者的数量也做了相同的限制
* **再次重申：datacenterId和workerId结合起来被用来唯一标识一个进程or节点，这两者的组合必须是'唯一'的**

## 路线图
* 支持更多分布式协调者 如etcd等
* 尽可能克服时间敏感问题
* 支持Id语义定制
* TBD


## 参与
* 报告bugs、给到建议反馈，请提交一个[issue](https://github.com/stun4j/stun4j-guid/issues/new)
* 参与贡献 改进或新功能，请提交pull request并创建一个[issue](https://github.com/stun4j/stun4j-guid/issues/new)以便讨论与进度追踪
* 不吝赐:star2: 

## 开源许可协议

本项目采用 **Apache Software License, Version 2.0**