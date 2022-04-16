# Stun4J Guid
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

### 分布式ID生成器 全局唯一、极速、趋势递增、易于使用  / [English](README_en_US.md) 


| 稳定版 | 主要变动 | 发布日期 |
| ------------- | ------------- | ------------|
| 1.2.2 | 引入量身定制的spring-boot-starter | 2022/04/16 |
| 1.1.6 | 引入另一种开箱即用的GUID获取策略，可根据IP地址末段自动获取GUID | 2022/03/21 |
| 1.1.5 | 提升了核心组件的健壮性 | 2022/01/25 |


## 功能特性
* 生成全局唯一的ID，适用于分布式环境(以进程为最小工作单元，在单机/伪集群中照常工作)
* 云原生、虚拟环境友好，对IP、端口的漂移无感
* 生成速度极快，轻松达到 **单机百万级的QPS**
* ID趋势递增，基于twitter-snowflake算法，针对时钟回退能够有限自愈
* 制品为袖珍型jar包，依赖极少，易于使用和集成

## 如何获取

### 方式1：从Maven中央仓库获取
在你工程的**pom.xml**中加入如下片段，即可从maven中央仓库获取：
#### 获取**核心库**，直接使用
```xml
<dependency>
  <groupId>com.stun4j</groupId>
  <artifactId>stun4j-guid-core</artifactId>
  <version>1.2.2</version>
</dependency>
```
#### 或者
#### 获取专属的**spring-boot-starter**，便于在spring-boot工程中使用
```xml
<dependency>
  <groupId>com.stun4j.boot</groupId>
  <artifactId>stun4j-guid-spring-boot-starter</artifactId>
  <version>1.2.2</version>
</dependency>
```
### 方式2：通过源码构建
切到项目根目录，在控制台执行如下maven命令：
```shell
$ mvn clean package
```
会在各自的target目录中生成`stun4j-guid-core-<version>.jar`和`stun4j-guid-spring-boot-starter-<version>.jar`，放入你工程的classpath即可

## 如何使用
### [**核心库**如何使用](stun4j-guid-core/README.md)
### [专属的**spring-boot-starter**如何使用](stun4j-guid-spring-boot-starter/README.md)

## 注意事项
1. 本ID生成算法是时间敏感的，所以集群环境务必开启NTP服务(尽可能做到时钟前向同步)，保障总体正确性和可用性
2. 采用[Zookeeper](http://zookeeper.apache.org/)作为分布式协调者时，客户端采用[Curator](http://curator.apache.org/)和ZK进行通信，需要注意Curator和Zookeeper的**兼容性**问题
	* 目前测试下来，Curator **2.13.0** 这个版本的兼容性比较好，可兼容Zookeeper **3.4.10+(server版本)**
	* 如使用**Zookeeper 3.5+(server版本)**，那么至少应搭配Curator **3.3.0+** 版本
3. 一个集群支持的进程/节点数量的上限是1024，这是经典snowflake算法非常核心的一点，也就是说datacenterId和workerId的取值范围都是 **[0,31]**，所以有1024种组合，在本框架的实现中也充分映射了这个概念，比如对分布式协调者一个namespace下参与者的数量也做了相同的限制
4. 直接或间接通过[**核心库-方式3**](stun4j-guid-core/README.md)使用的需额外注意：
    * 虽然本框架提供了灵活的pick ip的方式，但严格来说只有类似如下方式才能确保全局唯一性：
      ```java
      LocalGuid.initWithLocalIp("192.168", 1);//表示从本机挑选出 匹配'192.168.1'这个网段的IP
      
      LocalGuid.initWithLocalIp("192.168.1");//等价如上
      ```
    * 本方式的算法其实是取 'IP**末段**' 来标识节点的，所以只适用于 **相同网段** 并且 **节点数<=256** 的场景(原因是 a.受限于节点数<=1024, b.单个ip段的范围是[0,255])，换言之，**不同网段下，IP末段可能重复**，导致GUID的全局唯一性被破坏，所以现在，我们再来阐明其它几种API用法的问题，如下：
      ```java
      /*
       * 以下这些使用方式
       * 在不确定的网络环境中(如多网卡、多网段)，都可能破坏GUID的全局唯一性
       */
      LocalGuid.initWithLocalIp();//自动挑选本机IP，过于随意(适用于开发、测试)
      LocalGuid.initWithLocalIp("192.168");//范围过大
      LocalGuid.initWithLocalIp("192.168", 1, 2);//范围过大
      ```    
    * 应注意避免一种情况，比如在某个节点上起一个(伪)集群(比如3个进程)，因为IP相同，该集群的每个进程又具有独立的本地时钟(仅确保单进程唯一)，所以该集群中如果简单使用本框架来提供逻辑上的全局GUID，是无法避免重复的
5. 承上，作一个延伸讨论，本ID算法保证全局唯一的另一个重要(且隐晦)前提是预期在**同1个JVM中维护1个单例**，但我们知道，**不同的classloader**是可以打破这个限制的（即使在同1个JVM中），目前本框架并没有刻意去处理该问题
    * 通常来说，不同的classloader往往会被用来进行业务隔离，但当你将不同的classloader联合起来，以集群视角（或者说一个逻辑的业务单元视角）来直接使用本GUID算法，那么就和上述**问题4**类似了，而且即使是进程粒度的唯一性也会面对此问题（因为classloader的粒度更细）
    * 当然，也不必过于担心，当今主流的微服务架构都是**1个进程1个业务单元**的，规避复杂的classloader带来的潜在影响，规避**问题4**产生的伪集群或这些问题的变种 都是好的实践
6. **再次重申：datacenterId和workerId结合起来被用来唯一标识一个进程or节点，这两者的组合必须是'唯一'的**

## 路线图
* 支持更多分布式协调者 如etcd等
* 尽可能克服时间敏感问题
* 支持Id语义定制
* TBD


## 参与
* 报告bugs、给到建议反馈，请提交一个[issue](https://github.com/stun4j/stun4j-guid/issues/new)
* 参与贡献 改进或新功能，请提交pull request并创建一个[issue](https://github.com/stun4j/stun4j-guid/issues/new)以便讨论与进度追踪
* 不吝赐:star2:

## 感谢
*  极速UUID算法使用了fast-uuid这个[项目](https://github.com/codahale/fast-uuid)

## 开源许可协议

本项目采用 **Apache Software License, Version 2.0**
