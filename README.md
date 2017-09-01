# Spring Data JPA Condition

Spring Data JPA 动态查询工具类，用于快速生成动态查询的查询条件

## Features ##
* 快速生成Predicate条件
* 极简代码，良好可读性
* 基于实体类的属性进行自动遍历，自动处理空属性
* 支持自定义扩展

## Getting Help ##

## Quick Start ##

Download the jar through Maven:

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-jpa-condition</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

相关API
```java
JpaSpecificationExecutor.findOne(Specification<T> spec);

Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
```

前置条件
```java
public interface YourRepository<T, ID extends Serializable>
    extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>
```

Example
```java
// Use JPA Condition
// 使用 JPA Condition，极简的代码，并保持良好的可读性
Specification specification =
    JpaConditionUtils.specification(yourEntity, (root, query, cb, jc) -> {
        // where category = :category
        jc.and(jc.equal("category"))
            // and desc = :desc or code = :code or value = :value
            .and(jc.orInclude("desc", "code", "value"));
    });
YourEntity one = yourRepository.findOne(specification);
```
```java
// Use Spring Data JPA
// 使用 Spring Data JPA，冗长的代码，无法保持良好的可读性
Specification<Dictionary> specification = (root, query, cb) -> {
    // 1. category = :category
    Predicate category = cb.equal(root.get("category"), dict.getId());
    // 2. desc = :desc
    Predicate desc = cb.equal(root.get("desc"), dict.getDesc());
    // 3. code = :code
    Predicate code = cb.equal(root.get("code"), dict.getCode());
    // 4. value = :value
    Predicate value = cb.equal(root.get("value"), dict.getValue());
    // 5. (2 or 3 or 4)
    Predicate or = cb.or(desc, code, value);
    // where 1 or 5
    return cb.and(category, or);
};
YourEntity one = yourRepository.findOne(specification);
```

JpaCondition API
```java
/**
 * Equal条件, 包含所有names
 * @param names 属性名数组
 * @return Predicate
 */
public Predicate equalsInclude(@NotNull String... names);

/**
 * Equal条件, 排除所有names
 * @param names 属性名数组
 * @return Predicate
 */
public Predicate equalsExclude(@NotNull String... names);

/**
 * Like条件
 * @author TianGanLin
 * @version [1.0.0, 2017-08-28]
 * @return Predicate
 */
public Predicate likes()

/**
 * Like条件, 包含所有names
 * @author TianGanLin
 * @version [1.0.0, 2017-08-28]
 * @return Predicate
 */
public Predicate likesInclude(@NotNull String... names);

/**
 * Like条件, 排除所有names
 * @author TianGanLin
 * @version [1.0.0, 2017-08-28]
 * @return Predicate
 */
public Predicate likesExclude(@NotNull String... names);

/**
 * Or条件, 包含names
 *
 * @param names 属性名数组
 * @return Predicate
 */
public Predicate orInclude(@NotNull String... names);
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
