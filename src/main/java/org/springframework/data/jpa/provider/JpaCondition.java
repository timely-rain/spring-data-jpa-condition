package org.springframework.data.jpa.provider;

import com.sun.istack.internal.NotNull;
import org.springframework.beans.BeanUtils;

import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Jpa条件查询
 * @author TianGanLin
 * @version [1.0.0, 2017/8/25]
 * @see  [相关类/方法]
 * @since [产品/模块版本]
 */
public class JpaCondition<T>
{
    /**
     * 反射时排除Object类自带的Get方法
     */
    public static final String OBJECT_CLASS = "class";

    private T model;

    private Root<T> root;

    private CriteriaQuery<?> query;

    private CriteriaBuilder builder;

    /* Cache */
    private Class<? extends T> javaType;

    private PropertyDescriptor[] propertyDescriptors;

    private Set<Attribute<? super T, ?>> attributes;

    /* Constractor */
    public JpaCondition(Root<T> root, CriteriaQuery<?> query,
        CriteriaBuilder builder)
    {
        this.root = root;
        this.query = query;
        this.builder = builder;
        this.javaType = root.getJavaType();
    }

    /* Method */

    /**
     * 创建属性条件
     * @param filter 过滤器
     * @param predicate 创建条件
     * @return 条件
     */
    public Predicate propertyPredicate(
        Function<Stream<PropertyDescriptor>, Stream<PropertyDescriptor>> filter,
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate)
    {
        //        attributeStream().map(
        //            attribute -> BeanUtils.getPropertyDescriptor(javaType(),
        //                attribute.getName())).forEach(System.out::println);
        attributeStream().forEach(
            attribute -> System.out.println(attribute.getName()));
        Stream<PropertyDescriptor> stream = propertyStream()
            // 排除Object.getClass()
            .filter(descriptor -> !Objects.equals(descriptor.getName(),
                OBJECT_CLASS))
            // 排除瞬态属性
            .filter(descriptor -> !JpaConditionUtils.isTransient(root,
                descriptor.getName()));
        if (Objects.nonNull(filter)) // 执行过滤Lambda
            stream = filter.apply(stream); // 保留引用，否则会导致流“耗尽”
        Predicate[] predicates = predicate.apply(stream) // 执行条件Lambda
            .filter(p -> p != null) // 过滤空条件
            .toArray(Predicate[]::new); // 转为数组
        return builder.and(predicates);
    }

    public Predicate propertyPredicate(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate)
    {
        return propertyPredicate(null, predicate);
    }

    public Predicate propertyPredicateInclude(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate,
        String... include)
    {
        return propertyPredicate(stream -> stream.filter(
            JpaConditionUtils.includePredicate(include)), predicate);
    }

    public Predicate propertyPredicateExclude(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate,
        String... exclude)
    {
        return propertyPredicate(stream -> stream.filter(
            JpaConditionUtils.excludePredicate(exclude)), predicate);
    }

    /**
     * Equal条件
     * @return List<Predicate>
     * @author TianGanLin
     * @version [版本号, 2017/8/25]
     */
    public Predicate equals()
    {
        return propertyPredicate(stream -> stream.map(this::equal));
    }

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

    /* Tool */

    /**
     * Equal条件
     * @param descriptor 属性反射
     * @return Predicate
     */
    private Predicate equal(PropertyDescriptor descriptor)
    {
        return equal(true, descriptor);
    }

    /**
     * Equal条件
     * @param ignoreNull 忽略空值
     * @param descriptor 属性反射
     * @return Predicate
     */
    private Predicate equal(boolean ignoreNull, PropertyDescriptor descriptor)
    {
        String name = descriptor.getName();
        //        Object value = descriptor.getValue(name);
        //        if (ignoreNull && value == null)
        //            return null;
        //        return builder.equal(root.get(name), value);
        Method reader = descriptor.getReadMethod();
        try
        {
            Object value = reader.invoke(model);
            if (ignoreNull && value == null)
                return null;
            return builder.equal(root.get(name), value);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /* Reader */

    public Class<? extends T> javaType()
    {
        if (javaType == null)
        {
            javaType = root.getJavaType();
        }
        return javaType;
    }

    public PropertyDescriptor[] propertyDescriptors()
    {
        if (propertyDescriptors == null)
        {
            propertyDescriptors = BeanUtils.getPropertyDescriptors(javaType());
        }
        return propertyDescriptors;
    }

    public Set<Attribute<? super T, ?>> attributes()
    {
        if (attributes == null)
        {
            attributes = root.getModel().getAttributes();
        }
        return attributes;
    }

    public Stream<PropertyDescriptor> propertyStream()
    {
        return Arrays.stream(propertyDescriptors());
    }

    public Stream<Attribute<? super T, ?>> attributeStream()
    {
        return attributes.stream();
    }

    /* Getter And Setter */

    public T getModel()
    {
        return model;
    }

    public JpaCondition<T> setModel(T model)
    {
        this.model = model;
        return this;
    }

    public Root<T> getRoot()
    {
        return root;
    }

    public void setRoot(Root<T> root)
    {
        this.root = root;
    }

    public CriteriaQuery<?> getQuery()
    {
        return query;
    }

    public void setQuery(CriteriaQuery<?> query)
    {
        this.query = query;
    }

    public CriteriaBuilder getBuilder()
    {
        return builder;
    }

    public void setBuilder(CriteriaBuilder builder)
    {
        this.builder = builder;
    }

    public Class<? extends T> getJavaType()
    {
        return javaType;
    }

    public void setJavaType(Class<? extends T> javaType)
    {
        this.javaType = javaType;
    }
}
