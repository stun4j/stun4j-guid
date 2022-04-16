# How to use stun4j-guid-spring-boot-starter / [Chinese](README.md)

## 1. Quick start
### Use the `@EnableGuid` Annotation
Where the `@SpringBootApplication` annotation is used, the `@EnableGuid` annotation is used as follows:
```java
@SpringBootApplication
@EnableGuid
//omitted...
public class SampleApplication {
  public static void main(String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }
}
```
### Get guid through injected core-api in user(business) code
```java
@Service
public class BizService {
  @Autowired
  LocalGuid guid;
  //omitted...
  public void doBiz() {
    //omitted...
    Long id = guid.next();
    //omitted...
  }
}
```

## 2. Configuration detail of `application.yml`
### 2.1 Configuration corresponding to [**Core library-Method3**](../stun4j-guid-core/README.md)(**Default\***)
```yml
#omitted...

stun4j:
  guid: #Optional(If this does not appear, the default configuration is used)
    strategy: local-ip #Strategy:Identify local ip(Optional, default: local-ip)
    ip-start-with: <your-local-ip-prefix,e.g. 192.168.28> #Specify the local ip prefix(Optional,if not specified,the local ip is automatically selected)
    
#omitted...
```
### 2.2 Configuration corresponding to [**Core library-Method1**](../stun4j-guid-core/README.md)
```yml
#omitted...

stun4j:
  guid: #Optional(If this does not appear, the default configuration is used)
    strategy: manual #Strategy:manually specify(Optional, default: local-ip)
    datacenter-id: <your-datacenter-id,e.g. 0> #Optional,default:0, value range:[0,31]
    worker-id: <your-worker-id,e.g. 0> #Optional,default:0,value range:[0,31]
    
#omitted...
```
### 2.3 Configuration corresponding to [**Core library-Method2**](../stun4j-guid-core/README.md)
```yml
#omitted...

stun4j:
  guid: #Optional(If this does not appear, the default configuration is used)
    strategy: zk #Strategy:collaborate with zooKeeper(Optional, default: local-ip)
    zk-conn-addr: <your-zk-address,e.g. 192.168.28.161:2181> #The zookeeper address(Optional, default: localhost:2181)
    zk-namespace: <your-zk-namespace> #The zk namespace of the guid(Optional, default: stun4j-guid)
    ip-start-with: <your-local-ip-prefix,如192.168.28> #Specify the local ip prefix(Optional,if not specified,the local ip is automatically selected)
    
#omitted...
```
[< Back to index](../README_en_US.md)