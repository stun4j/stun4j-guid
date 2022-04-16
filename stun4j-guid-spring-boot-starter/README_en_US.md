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

## 2. Configuration detail of `application.yml`(TBD)

[< Back to index](../README_en_US.md)