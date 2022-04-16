# Stun4J Guid
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

### Global unique id generator, distributed, ultra fast, easy to use / [中文版](README.md) 


| Stable Release Version | Major change | Release Date |
| ------------- | ------------- | ------------|
| 1.2.2 | Introduced customized spring-boot-starter | 04/16/2022 |
| 1.1.6 | Another out-of-box GUID acquisition strategy is introduced, which can automatically obtain the GUID based on the end of the IP address | 03/21/2022 |
| 1.1.5 | Improved robustness of core components | 01/25/2022 |

## Feature
* Global unique id-generating,fully distributed(treat system-process as minimal working unit,hence,the id-gen is fully workable,even in the pseudo-cluster environment)
* Clound-native friendly,fully workable on virtualization environment with floating ip/port.
* Ultra fast on id-generating, **over million QPS per single process/node**
* Monotonic increasing mechanism, based on twitter-snowflake algorithm, clock-backwards awarness,self-healable
* The artifact is a very-small jar,with minimal dependencies, easy to use

## How to get
### Method 1: Maven Repository
Stun4J-Guid is deployed at sonatypes open source maven repository. You can get it from the central maven repository, just add these to your pom.xml file:
#### Get the **core library**, use it directly
```xml
<dependency>
  <groupId>com.stun4j</groupId>
  <artifactId>stun4j-guid-core</artifactId>
  <version>1.2.2</version>
</dependency>
```
#### Or
#### Get the customized **spring-boot-starter**, it is easy to use in spring-boot projects
```xml
<dependency>
  <groupId>com.stun4j.boot</groupId>
  <artifactId>stun4j-guid-spring-boot-starter</artifactId>
  <version>1.2.2</version>
</dependency>
```
### Method 2: Building from the sources
As it is maven project, buidling is just a matter of executing the following in your console:
```shell
$ mvn clean package
```
`stun4j-guid-core-<version>.jar` and `stun4J-guid-spring-boot-starter-<version>.jar` will be generated in their respective target directories and placed in your project's classpath.

## How to use
### [How to use the **core library**](stun4j-guid-core/README_en_US.md)
### [How to use the customized **spring-boot-starter**](stun4j-guid-spring-boot-starter/README_en_US.md)

## Notes
1. This ID generation algorithm is time sensitive, so the cluster environment must turn on the NTP service (do as much clock forward synchronization as possible) to ensure overall correctness and availability
2. When [Zookeeper](http://zookeeper.apache.org/) is adopted as the distributed coordinator, the client uses [Curator](http://curator.apache.org/) to communicate with ZK. Therefore, it is necessary to pay attention to the **compatibility** between Curator and Zookeeper
	* Tests so far shows that Curator **2.13.0** is compatible with **Zookeeper 3.4.10+(server version)**
	* If you are using **Zookeeper 3.5+(server version)**, you should at least use it with Curator **3.3.0+**
3. The upper limit of a cluster supporting the number of process/nodes is 1024, that's the way classic snowflake-algorithm works, that is to say, both of datacenterId and workerId scope is [0, 31], so there are 1024 kinds of combination, in the implementation of this framework is fully the concept mapping, e.g. the same restriction is made on the number of participants under a namespace for the distributed coordinator
4. Extra attention should be paid to those directly or indirectly using [**Core library-Method3**](stun4j-guid-core/README_en_US.md)：
    * Although the framework provides a flexible way to pick IP, strictly speaking, only something like the following can ensure global uniqueness：
      ```java
      LocalGuid.initWithLocalIp("192.168", 1);//indicates that IP addresses matching the network segment '192.168.1' are selected from the host
      
      LocalGuid.initWithLocalIp("192.168.1");//equivalent as above
      ```
    * This method is used to identify nodes in **the end** of IP address segment. Therefore, this method is only applicable to scenarios where **in the same network segment** and the **number of nodes <= 256**.(The reason is **a.** The number of nodes <= 1024. **b.** A single IP address segment range is [0,255]).In other words, the end of the IP may be repeated in different network segments, resulting in the destruction of the global uniqueness of the GUID. Therefore, we will clarify the other API usage problems as follows:
      ```java
      /*
       * The following uses may break the global uniqueness of GUID
       * in uncertain network environments(such as multiple network
       * interfaces or multiple network segments)
       */
      LocalGuid.initWithLocalIp();//automatic selection of local IP, too arbitrary(for development, testing only)
      LocalGuid.initWithLocalIp("192.168");//range is too large
      LocalGuid.initWithLocalIp("192.168", 1, 2);//range is too large
      ```
    * It is important to avoid situations such as having a (pseudo) cluster (e.g. 3 processes) on a node, each process in the cluster has the same IP and has an independent local clock(Only ensure single process uniqueness), so if you simply use this framework to provide a logical global GUID in the cluster, it is impossible to avoid duplication
5. As an extension, an important requirement for this ID algorithm to be globally unique is to expect to **maintain a singleton in the same JVM**, but we know that **different classloaders** can break this limitation (even in the same JVM), and this algorithm does not address this issue intentionally
    * Normally, different classloaders are used for business isolation, but when you combine the different classloaders to use the GUID algorithm directly from the cluster perspective (or a logical business unit perspective), it is similar to **Question 4** above. And even the uniqueness of process granularity is a problem (because classLoaders are more granular)
    * Of course, there is no need to worry too much. Today's mainstream microservice architectures are all **1 process 1 business unit**, and it is good practice to avoid the potential impact of complex Classloaders, and to avoid the pseudo-clustering or variations of these problems caused by **Question 4**
6. **Again, the combination of datacenterId and workerId is used to uniquely identify a process or node, and the combination of the two must be 'unique'**

## Roadmap
* To support more kinds of distributed-coordinator e.g. etcd
* Try the best to solve the time-sensitive problem
* To support ID semantic customization
* TBD

## Contributions
To help Stun4J-Guid development you are encouraged to

* For reporting bugs, provide suggestion/feedback, please open an [issue](https://github.com/stun4j/stun4j-guid/issues/new)
* For contributing improvements or new features, please send in the pull request and open an [issue](https://github.com/stun4j/stun4j-guid/issues/new) for discussion and progress tracking
* Star :star2: the project

## Thanks
*  The FastUUID algorithm uses the Fast-UUID [project](https://github.com/codahale/fast-uuid)

## License

This project is licensed under **Apache Software License, Version 2.0**
