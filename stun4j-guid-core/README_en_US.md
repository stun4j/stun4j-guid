# How to use core library / [Chinese](README.md)
## Method 1：Manually specify (for applications with a small number of nodes that wish or are capable of maintaining "process or node identity uniqueness" by themselves)：

```java
//Step 1.Initialization (only once,usually when the application starts)
/*datacenterId and workerId are used to uniquely identify a process or node, 
and the combination of the two must be 'unique'*/
LocalGuid guid = LocalGuid.init(0/*datacenterId*/, 0/*workerId*/);

//Step 2.Get the id (snowflake algorithm)
//Method 1:
Long id1 = guid.next();
//Method 2:
Long id2 = LocalGuid.instance().next();

//In addition, the framework also integrates two excellent UUID algorithms
//Method 1 (FastUUID algorithm):
String uuid1 = LocalGuid.uuid();

//Method 2 (Improved JDK UUID):
String uuid2 = LocalGuid.uuid(true/*Whether it is separated by '-'*/, false/*Whether to use top speed mode*/);
```

## Method 2(recommend*)：Use in conjunction with distributed coordinator ("process identity uniqueness" is automatically maintained)：

```java
//Step 1.Initialization (only once,using zookeeper as Distributed-Coordinator)
LocalGuid guid = LocalZkGuid.init("localhost:2181"/*zk address*/)
//Step 2.Get the id(same as 'Step 2 of Method 1', omitted)
```

## Method 3(Applicable to the same network segment)：Use by identifying the local IP("node identity uniqueness" is automatically maintained) or by specifying the IP：

```java
//Step 1.Initialization (only once,usually when the application starts)
//Specify the local IP prefix (in the scenario of multiple network interfaces, pick the correct IP address)
LocalGuid guid
= LocalGuid.initWithLocalIp("192.168.1");//currently, only IPV4 is supported

//Or specify the local IP prefix and the IP segment(currently only 'the third segment' is supported)
= LocalGuid.initWithLocalIp("192.168", 1);//Pick from 192.168.1.*

//Step 2.Get the id(same as 'Step 2 of Method 1', omitted)
```

[< Back to index](../README_en_US.md)