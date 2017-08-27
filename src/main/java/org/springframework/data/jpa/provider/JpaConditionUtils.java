package org.springframework.data.jpa.provider;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author TianGanLin
 * @version [版本号, 2017/8/24]
 * @see  [相关类/方法]
 * @since [产品/模块版本]
 */
public class JpaConditionUtils
{
    /**
     * 实例化Jpa条件查询
     * @param root
     * @param query
     * @param cb
     * @param model 实体类
     * @param <T> 实体类类型
     * @return JpaCondition<T>
     */
    public static <T> JpaCondition<T> instance(Root<T> root,
        CriteriaQuery<?> query, CriteriaBuilder cb, T model)
    {
        return new JpaCondition<>(root, query, cb).setModel(model);
    }

    /* Property Filter */

    /**
     * 过滤器-非空
     * @param stream Stream<PropertyDescriptor>
     * @return Stream<PropertyDescriptor>
     */
    public static Stream<PropertyDescriptor> filterNotNull(
        Stream<PropertyDescriptor> stream)
    {
        return stream.filter(JpaConditionUtils.notNullPredicate());
    }

    /**
     * 断言-非空
     * @return Predicate<PropertyDescriptor>
     */
    public static <T> Predicate<T> notNullPredicate()
    {
        return t -> t != null;
    }

    /**
     * 断言-包含
     * @return Predicate<PropertyDescriptor>
     */
    public static Predicate<PropertyDescriptor> includePredicate(
        String... includes)
    {

        return descriptor -> {
            Arrays.sort(includes);
            String name = descriptor.getName();
            int i = Arrays.binarySearch(includes, name);
            return i >= 0;
        };
    }

    /**
     * 断言-排除
     * @return Predicate<PropertyDescriptor>
     */
    public static Predicate<PropertyDescriptor> excludePredicate(
        String... excludes)
    {
        return descriptor -> Arrays.binarySearch(excludes, descriptor.getName())
            < 0;
    }

    public static <T> Object getPropertyValue(T model,
        PropertyDescriptor descriptor)
    {
        Method reader = descriptor.getReadMethod();
        try
        {
            return reader.invoke(model);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断属性是否瞬态
     * @param root 实体类映射
     * @param name 属性名
     * @param <T> 实体类类型
     * @return 是否瞬态
     */
    public static <T> boolean isTransient(Root<T> root, String name)
    {
        try
        {
            root.get(name);
        }
        catch (Exception e)
        {
            return true;
        }
        return false;
    }
}
