# 多例-核心库如何使用 / [English](README_en_US.md)
## 1. 通过全局开关启用多例特性
```java
LocalGuidMultiton._enabled = true;
```
## 2. 单例和多例的共存使用
### 通过`LocalGuid`使用单例，通过`LocalGuidMultiton#instance`使用多例
```java
//这个是全局单例的初始化和使用
//LocalGuid.init(0,0)等价于LocalGuid.init(0,0, 19, 5, 5, 12, false)
LocalGuid dftSolo = LocalGuid.init(0,0);
Long id = dftSolo.next();

/*
 * 下面就是多例的展示了，假设全局dftSolo生成的是默认的18位以上的整数
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
    
//这样，在系统中，dftSolo、fooSolo和barSolo都按各自的模式生成着不同的id，略...
```

# 
[< 回索引](../../README.md)
