# 多例-spring-boot-starter使用说明 / [English](README_en_US.md)

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
## 2. `application.yml`配置详解
```yml
#略...

stun4j:
  guid: #可选(如不出现，则采用默认配置)
    multiton: #可选(如不出现，则采用默认配置)
      enabled: true #是否启用多例(可选,默认值:false)
      auto-register-enabled: true #是否启用自动注册(可选,默认值:true)
    
#略...
```
## 3. 单例和多例的共存使用
### 通过注入的LocalGuid使用默认单例，通过`LocalGuidMultiton#instance`使用多例
```java
@Service
public class BizService {
  @Autowired
  LocalGuid dftSolo;//这个是默认单例(即你在application.yml中通过'非multiton节点'配置的guid)
  //略...
  public void doBiz() {
    //略...
    Long id = dftSolo.next();
    //略...
    
    /*
     * 下面就是多例的展示了，假设默认dftSolo生成的是18位以上的整数
     * 而下面两个solo的生成模式完全一致，都是(16,4,4,5,false)
     * 所以fooSolo和anotherFooSolo是同一个实例(引用相同)，即fooSolo == anotherFooSolo
     * 但fooSolo != dftSolo
     */
    LocalGuid fooSolo = LocalGuidMultiton.instance(16, 4, 4, 5, false);
    LocalGuid anotherFooSolo = LocalGuidMultiton.instance(16, 4, 4, 5, false);
    
    /*
     * 下面这个barSolo的生成模式略微不同，是(16,4,4,5,true)
     * 所以barSolo != fooSolo，假设dftSolo的内置模式恰好
     * 也是(16,4,4,5,true)，那么dftSolo == barSolo,
     * 否则，barSolo就又是一个新的实例了
     */ 
    LocalGuid barSolo = LocalGuidMultiton.instance(16, 4, 4, 5, true);
    
    //这样，在系统中，dftSolo、fooSolo和barSolo都按各自的模式生成不同的id，略...
  }
}
```

# 
[< 回索引](../../README.md)
