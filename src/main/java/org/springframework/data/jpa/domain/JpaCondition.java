package org.springframework.data.jpa.domain;

import com.sun.istack.internal.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.util.JpaConditionUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import java.beans.PropertyDescriptor;
import java.util.Set;
import java.util.function.BiFunction;
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

    @Deprecated
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
     * @param stream 属性流
     * @param predicate 创建条件
     * @return 条件
     */
    public Predicate propertyPredicate(Stream<PropertyDescriptor> stream,
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate)
    {
        Stream<Predicate> predicateStream = predicate.apply(stream);
        return toPredicate(predicateStream);
    }

    /**
     * 生成条件断言
     * @param predicate 条件断言流
     * @return 条件断言
     */
    public Predicate toPredicate(Stream<Predicate> predicate)
    {
        Predicate[] predicates = predicate.filter(p -> p != null) // 过滤空条件
            .toArray(Predicate[]::new); // 转为数组
        return builder.and(predicates);
    }

    public Predicate propertyPredicate(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate)
    {
        Stream<Predicate> stream = predicate.apply(propertyStream());
        return toPredicate(stream);
    }

    public Predicate propertyPredicateInclude(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate,
        String... include)
    {
        return propertyPredicate(propertyStream().filter(
            JpaConditionUtils.includePredicate(include)), predicate);
    }

    public Predicate propertyPredicateExclude(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate,
        String... exclude)
    {
        return propertyPredicate(propertyStream().filter(
            JpaConditionUtils.excludePredicate(exclude)), predicate);
    }

    /* Properties Predicate */

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


    /* Property Predicate */

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
        return valuePredicate(ignoreNull, descriptor,
            (name, value) -> builder.equal(root.get(name), value));
    }

    /**
     * 包含该属性值
     * @param descriptor
     * @return
     */
    private Predicate like(PropertyDescriptor descriptor)
    {
        return valuePredicate(true, descriptor,
            (name, value) -> builder.like(root.get(name), "%" + value + "%"));
    }

    /**
     * 以属性值开头
     * @param descriptor
     * @return
     */
    public Predicate likeStart(PropertyDescriptor descriptor)
    {
        return valuePredicate(true, descriptor,
            (name, value) -> builder.like(root.get(name), value + "%"));
    }

    /**
     * 以属性值结尾
     * @param descriptor
     * @return
     */
    public Predicate likeEnd(PropertyDescriptor descriptor)
    {
        return valuePredicate(true, descriptor,
            (name, value) -> builder.like(root.get(name), "%" + value));
    }

    /**
     * 值条件断言
     * @param ignoreNull 忽略空值
     * @param descriptor 属性
     * @param function BiFunction<属性名, 属性值, 条件断言>
     * @param <T> 属性值类型
     * @return 条件断言
     */
    private <T> Predicate valuePredicate(boolean ignoreNull,
        PropertyDescriptor descriptor,
        BiFunction<String, T, Predicate> function)
    {
        String name = descriptor.getName();
        Object value = JpaConditionUtils.getPropertyValue(model, descriptor);
        if (ignoreNull && value == null)
            return null;
        return function.apply(name, (T)value);
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

    @Deprecated
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
        //        return Arrays.stream(propertyDescriptors());
        return attributeStream().map(
            attribute -> BeanUtils.getPropertyDescriptor(javaType(),
                attribute.getName()));
    }

    public Stream<Attribute<? super T, ?>> attributeStream()
    {
        return attributes().stream();
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
