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

#### 相关API ####
```java
JpaSpecificationExecutor.findOne(Specification<T> spec);

Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
```

#### 前置条件 ####
```java
public interface YourRepository<T, ID extends Serializable>
    extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>
```

#### Example ####
```java
// Test Entity
// 用于演示的实体类
public class YourEntity
{
    private Long id;
    private String category;
    private String desc;
    private String code;
    private Integer value;
}
```
Use JPA Condition
```java
// 使用 JPA Condition，极简的代码，并保持良好的可读性
Specification specification =
    JpaConditionUtils.specification(yourEntity, (root, query, cb, jc) -> {
        // where category = :category
        jc.clauseAnd(jc.equal("category"))
            // and desc = :desc or code = :code or value = :value
            .clauseAnd(jc.orEqualInclude("desc", "code", "value"));
    });
YourEntity one = yourRepository.findOne(specification);

// Also you can use Native API like this
// 同时也支持 Spring Data JPA 原生API
// jc.clauseAnd(cb.equal(root.get("id"), yourEntity.getId()));
```
Use Spring Data JPA
```java
// 使用 Spring Data JPA，冗长的代码，无法保持良好的可读性
Specification<YourEntity> specification = (root, query, cb) -> {
    // 1. category = :category
    Predicate category = cb.equal(root.get("category"), yourEntity.getId());
    // 2. desc = :desc
    Predicate desc = cb.equal(root.get("desc"), yourEntity.getDesc());
    // 3. code = :code
    Predicate code = cb.equal(root.get("code"), yourEntity.getCode());
    // 4. value = :value
    Predicate value = cb.equal(root.get("value"), yourEntity.getValue());
    // 5. (2 or 3 or 4)
    Predicate or = cb.or(desc, code, value); // use mergeOr() of JPA Condition
    // where 1 and 5
    return cb.and(category, or); // use clauseAnd() of JPA Condition
};
YourEntity one = yourRepository.findOne(specification);
```
### merge & clause ###
在JPA Condition中，and/or被拆分成两种类型的操作，以明确它们的语义
#### merge ####
> mergeAnd & mergeOr

merge操作仅合并查询条件，而不会将查询条件注入到CriteriaQuery

#### clause ####
> clauseAnd & clauseOr

clause操作合并条件，并将查询条件注入到CriteriaQuery

### JPA Condition API ###
```java
/**
 * Equal条件
 *
 * @return List<Predicate>
 * @apiNote range:Entity.attributes
 */
public Predicate[] equals();

/**
 * Equal条件
 *
 * @param names 属性名数组
 * @return Predicate
 * @apiNote range:Entity.attributes in names
 */
public Predicate[] equalsInclude(@NotNull String... names);

/**
 * Equal条件
 *
 * @param names 属性名数组
 * @return Predicate
 * @apiNote range:Entity.attributes not in names
 */
public Predicate[] equalsExclude(@NotNull String... names);

public Predicate[] likes();

public Predicate[] likesInclude(@NotNull String... names);

public Predicate[] likesExclude(@NotNull String... names);

/**
 * OrEqual条件
 *
 * @param names 属性名数组
 * @return Predicate
 * @apiNote range:Entity.attributes in names
 * @apiNote builder.or(equalsInclude(names))
 */
public Predicate orEqualInclude(@NotNull String... names);
```

## Core ##
### JpaConditionUtils ###
```java
/**
 * 生成JPA查询明细
 *
 * @param model         实体类
 * @param specification ConditionSpecification
 * @param <T>           实体类类型
 * @return Specification
 */
@SuppressWarnings("unchecked")
public static <T> Specification specification(T model,
    ConditionSpecification<T> specification)
    
/**
 * 实例化Jpa条件查询
 *
 * @param root
 * @param query
 * @param cb
 * @param model 实体类
 * @param <T>   实体类类型
 * @return JpaCondition<T>
 */
public static <T> JpaCondition<T> condition(Root<T> root,
    CriteriaQuery<?> query, CriteriaBuilder cb, T model)
```
### JpaCondition ###
### ConditionSpecification ###
