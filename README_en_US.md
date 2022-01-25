# Stun4J Guid
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

### Global unique id generator, distributed, ultra fast, easy to use / [中文版](README.md) 


| Stable Release Version | Major change | Release Date |
| ------------- | ------------- | ------------|
| 1.1.5 | Improved robustness of core components | 01/25/2022 |
| 1.1.3 | Optimize internal algorithm performance | 07/22/2021 |
| 1.1.2 | Optimize startup performance | 04/27/2021 |

## Feature
* Global unique id-generating,fully distributed(treat system-process as minimal working unit,hence,the id-gen is fully workable,even in the pseudo-cluster environment)
* Clound-native friendly,fully workable on virtualization environment with floating ip/port.
* Ultra fast on id-generating, **over million QPS per single process/node**
* Monotonic increasing mechanism, based on twitter-snowflake algorithm, clock-backwards awarness,self-healable
* The artifact is a very-small jar,with minimal dependencies, easy to use

## How to get
### Method 1: Maven Repository

Stun4J-Guid is deployed at sonatypes open source maven repository. You can pull stun4j-guid from the central maven repository, just add these to your pom.xml file:

```xml
<dependency>
  <groupId>com.stun4j</groupId>
  <artifactId>stun4j-guid</artifactId>
  <version>1.1.5</version>
</dependency>
```

### Method 2: Building from the sources

As it is maven project, buidling is just a matter of executing the following in your console:

	$ mvn clean package


This will produce the stun4j-guid-VERSION.jar file under the target directory.

## How to use
### Method 1：Direct use (for applications with a small number of nodes that wish or are capable of maintaining \"process identity uniqueness\" by themselves)：

```java
//Step 1.Initialization (only once,usually when the application starts)
/*datacenterId and workerId are used to uniquely identify a process or node, 
and the combination of the two must be 'unique'*/
LocalGuid guid = LocalGuid.init(0/*datacenterId*/, 0/*workerId*/);

//Step 2.Get the id (snowflake algorithm)
//Method 1:
long id1 = guid.next();
//Method 2:
long id2 = LocalGuid.instance().next();

//In addition, the framework also integrates two excellent UUID algorithms
//Method 1 (FastUUID algorithm):
String uuid1 = LocalGuid.uuid();

//Method 2 (Improved JDK UUID):
String uuid2 = LocalGuid.uuid(true/*Whether it is separated by '-'*/, false/*Whether to use top speed mode*/);
```

### Method 2(recommend\*)：Use in conjunction with distributed coordinator (\"process identity uniqueness\" automatically maintained)：

```java
//Step 1.Initialization (only once,using zookeeper as Distributed-Coordinator)
LocalGuid guid = LocalZkGuid.init("localhost:2181"/*zk address*/)
//Step 2.Get the id(same as 'Step 2 of Method 1', omitted)
```


## Notes
* This ID generation algorithm is time sensitive, so the cluster environment must turn on the NTP service (do as much clock forward synchronization as possible) to ensure overall correctness and availability
* When [Zookeeper](http://zookeeper.apache.org/) is adopted as the distributed coordinator, the client uses [Curator](http://curator.apache.org/) to communicate with ZK. Therefore, it is necessary to pay attention to the **compatibility** between Curator and Zookeeper
	* Tests so far shows that Curator **2.13.0** is compatible with **Zookeeper 3.4.10+(server version)**
	* If you are using **Zookeeper 3.5+(server version)**, you should at least use it with Curator **3.3.0+**
* The upper limit of a cluster supporting the number of process/nodes is 1024, that's the way classic snowflake-algorithm works, that is to say, both of datacenterId and workerId scope is [0, 31], so there are 1024 kinds of combination, in the implementation of this framework is fully the concept mapping, e.g. the same restriction is made on the number of participants under a namespace for the distributed coordinator
* **Again, the combination of datacenterId and workerId is used to uniquely identify a process or node, and the combination of the two must be 'unique'**

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
