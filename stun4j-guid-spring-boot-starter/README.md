# stun4j-guid-spring-boot-starter使用说明 / [English](README_en_US.md)

## 1. 快速开始
### 使用`@EnableStf`注解
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
### 通过注入的core-api，在用户（业务）代码中获取guid
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

## 2. `application.yml`配置详解(TBD)