package org.springframework.data.jpa.condition;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * 极简JPA动态查询工具类
 *
 * @author TianGanLin
 * @version [版本号, 2017/8/24]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class JpaConditionUtils {

  /**
   * 实例化Jpa条件查询
   *
   * @param root Root
   * @param query CriteriaQuery
   * @param cb CriteriaBuilder
   * @param model 实体类
   * @param <T> 实体类类型
   * @return JpaCondition<T>
   */
  public static <T> JpaCondition<T> condition(
      Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb, T model) {
    return new JpaCondition<>(root, query, cb).setModel(model);
  }

  /**
   * 生成JPA查询明细
   *
   * @param model 实体类
   * @param specifications ConditionSpecification
   * @param <S> 实体类类型
   * @return Specification
   */
  public static <S> Specification<S> specification(
      S model, ConditionSpecification<S>... specifications) {
    return (root, query, cb) -> {
      JpaCondition<S> condition = JpaConditionUtils.condition(root, query, cb, model);
      for (ConditionSpecification<S> specification : specifications) {
        specification.apply(root, query, cb, condition);
      }
      return condition.toPredicate();
    };
  }

  /**
   * 生成JPA查询明细
   *
   * @param specification ParallelSpecification
   * @param <T> 实体类类型
   * @return Specification
   */
  public static <T> Specification<T> specification(
      ParallelSpecification<T> specification) {
    return (root, query, cb) -> {
      List<javax.persistence.criteria.Predicate> predicates =
          new ArrayList<>();
      specification.apply(root, query, cb, predicates);
      return specification.mergePredicate(cb, predicates);
    };
  }

  /* Property Filter */

  /**
   * 断言-包含
   *
   * @return Predicate<PropertyDescriptor>
   */
  public static Predicate<PropertyDescriptor> includePredicate(
      String... includes) {
    return descriptor -> Stream.of(includes)
        .anyMatch(s -> Objects.equals(s, descriptor.getName()));
  }

  /**
   * 断言-排除
   *
   * @return Predicate<PropertyDescriptor>
   */
  public static Predicate<PropertyDescriptor> excludePredicate(
      String... excludes) {
    return descriptor -> !Stream.of(excludes)
        .anyMatch(s -> Objects.equals(s, descriptor.getName()));
  }

  /**
   * 获取实体类的属性值
   *
   * @param model 实体类
   * @param descriptor spring-beans属性
   * @param <T> 实体类类型
   * @return 属性值
   */
  public static <T> Object getPropertyValue(T model,
      PropertyDescriptor descriptor) {
    Method reader = descriptor.getReadMethod();
    try {
      return reader.invoke(model);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 判断属性是否瞬态
   *
   * @param root 实体类映射
   * @param name 属性名
   * @param <T> 实体类类型
   * @return 是否瞬态
   */
  public static <T> boolean isTransient(Root<T> root, String name) {
    try {
      root.get(name);
    } catch (Exception e) {
      return true;
    }
    return false;
  }
}
