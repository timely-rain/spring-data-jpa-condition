# Spring Data JPA Condition

Spring Data JPA 动态查询工具类，用于快速生成动态查询的查询条件

## Features ##

## Getting Help ##

## Quick Start ##

Download the jar through Maven:

```xml
<dependency>
  <groupId>org.springframework.data.jpa.provider</groupId>
  <artifactId>spring-data-jpa-condition</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

相关API
```java
JpaSpecificationExecutor.findAll(Specification<T> spec);

Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
```

前置条件
```java
public interface YourRepository<T, ID extends Serializable>
    extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>
```

Example
```java
yourRepository.findAll(((root, query, cb) -> {
  // 实例化JpaCondition
  JpaCondition<YourEntity> condition =
      JpaConditionUtils.instance(root, query, cb, yourEntity);
  // 匹配所有属性
  Predicate equals = condition.equals();
  // 返回条件断言
  return cb.and(equals);
}));
```

JpaCondition API
```java
/**
 * Equal条件, 包含所有names
 * @param names 属性名数组
 * @return Predicate
 */
public Predicate equalsInclude(@NotNull String... names)
{
    return propertyPredicateInclude(stream -> stream.map(this::equal),
        names);
}

/**
 * Equal条件, 排除所有names
 * @param names 属性名数组
 * @return Predicate
 */
public Predicate equalsExclude(@NotNull String... names)
{
    return propertyPredicateExclude(stream -> stream.map(this::equal),
        names);
}

/**
 * Equal条件, 排除所有names
 * @param names 属性名数组
 * @return Predicate
 */
public Predicate equalsExclude(@NotNull String... names)
{
    return propertyPredicateExclude(stream -> stream.map(this::equal),
        names);
}

/**
 * Like条件
 * @author TianGanLin
 * @version [1.0.0, 2017-08-28]
 * @return Predicate
 */
public Predicate likes()
{
    return propertyPredicate(stream -> stream.map(this::like));
}

/**
 * Like条件, 包含所有names
 * @author TianGanLin
 * @version [1.0.0, 2017-08-28]
 * @return Predicate
 */
public Predicate likesInclude(@NotNull String... names)
{
    return propertyPredicateInclude(stream -> stream.map(this::like),
        names);
}

/**
 * Like条件, 排除所有names
 * @author TianGanLin
 * @version [1.0.0, 2017-08-28]
 * @return Predicate
 */
public Predicate likesExclude(@NotNull String... names)
{
    return propertyPredicateExclude(stream -> stream.map(this::like),
        names);
}
```

核心(用于自定义扩展)
```java
/**
 * 获得实体类的Managed属性流
 * @return Stream<Attribute>
 */
public Stream<Attribute<? super T, ?>> attributeStream();

/**
 * 获得实体类的Managed属性流的spring-beans版
 * @return Stream<PropertyDescriptor>
 */
public Stream<PropertyDescriptor> propertyStream();

/**
 * 为所有属性生成条件断言
 * @param predicate 条件Lambda
 * @return 条件断言
 */
public Predicate propertyPredicate();

/**
 * 值条件断言, 对实体类每个属性的值尝试生成条件断言
 * @param ignoreNull 忽略空值
 * @param descriptor 属性
 * @param function BiFunction<属性名, 属性值, 条件断言>
 * @param <T> 属性值类型
 * @return 条件断言
 */
private <T> Predicate valuePredicate(boolean ignoreNull,
        PropertyDescriptor descriptor,
        BiFunction<String, T, Predicate> function);
```
