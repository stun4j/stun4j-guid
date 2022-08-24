# stun4j-guid-spring-boot-starter使用说明 / [English](README_en_US.md)

## 1. 快速开始
### 使用`@EnableGuid`注解
在使用`@SpringBootApplication`注解的地方，使用`@EnableGuid`注解，如下：
```java
@SpringBootApplication
@EnableGuid
public class SampleApplication {
  public static void main(String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }
}
```
### 通过注入的core-api，在业务代码中获取guid
```java
@Service
public class BizService {
  @Autowired
  LocalGuid guid;
  //略...
  public void doBiz() {
    //略...
    Long id = guid.next();
    //略...
  }
}
```

## 2. `application.yml`配置详解
### 2.1 对应于[**核心库使用-方式3**](../stun4j-guid-core/README.md)的配置(**默认\***)
```yml
#略...

stun4j:
  guid: #可选(如不出现，则采用默认配置)
    strategy: local-ip #策略:识别本机IP(可选,默认值:local-ip)
    ip-start-with: <your-local-ip-prefix,如192.168.28> #指定本机ip前缀(可选,如不指定,将自动挑选本机IP)
    
#略...
```
### 2.2 对应于[**核心库使用-方式1**](../stun4j-guid-core/README.md)的配置
```yml
#略...

stun4j:
  guid: #可选(如不出现，则采用默认配置)
    strategy: manual #策略:手动指定(可选,默认值:local-ip)
    datacenter-id: <your-datacenter-id,如0> #可选,默认值:0, 取值范围[0,31]
    worker-id: <your-worker-id,如0> #可选,默认值:0, 取值范围[0,31]
    
#略...
```
### 2.3 对应于[**核心库使用-方式2**](../stun4j-guid-core/README.md)的配置
```yml
#略...

stun4j:
  guid: #可选(如不出现，则采用默认配置)
    strategy: zk #策略:结合zookeeper使用(可选,默认值:local-ip)
    zk-conn-addr: <your-zk-address,如192.168.28.161:2181> #zookeeper地址(可选,默认值:localhost:2181)
    zk-namespace: <your-zk-namespace> #guid的zk命名空间(可选,默认值:stun4j-guid)
    ip-start-with: <your-local-ip-prefix,如192.168.28> #指定本机ip前缀(可选,如不指定,将自动挑选本机IP)
    
#略...
```
# 
[< 回索引](../README.md)
