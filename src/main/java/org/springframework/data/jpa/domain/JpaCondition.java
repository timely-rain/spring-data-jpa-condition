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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 极简JPA动态查询
 *
 * @author TianGanLin
 * @version [1.0.0, 2017/8/25]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class JpaCondition<T>
{
    /* Cache */

    private T model;

    private Root<T> root;

    private CriteriaQuery<?> query;

    private CriteriaBuilder builder;

    private Class<? extends T> javaType;

    private Set<Attribute<? super T, ?>> attributes;

    private Predicate clausePredicate;

    /* Constractor */
    public JpaCondition(Root<T> root, CriteriaQuery<?> query,
        CriteriaBuilder builder)
    {
        this.root = root;
        this.query = query;
        this.builder = builder;
        this.javaType = root.getJavaType();
    }

    /* Entity Attributes */

    /**
     * 获得实体类的Managed属性流
     *
     * @return Stream<Attribute>
     */
    protected Stream<Attribute<? super T, ?>> attributeStream()
    {
        return attributes().stream();
    }

    /**
     * 获得实体类的spring-beans属性流
     *
     * @return Stream<PropertyDescriptor>
     */
    protected Stream<PropertyDescriptor> propertyStream()
    {
        return attributeStream().map(this::propertyDescriptor);
    }

    /**
     * 获得实体类的spring-beans属性流
     * include by names
     *
     * @return Stream<PropertyDescriptor>
     */
    protected Stream<PropertyDescriptor> propertyStreamInclude(String... names)
    {
        return attributeStream().map(this::propertyDescriptor)
            .filter(JpaConditionUtils.includePredicate(names));
    }

    /**
     * 获得实体类的spring-beans属性流
     * exclude by names
     *
     * @return Stream<PropertyDescriptor>
     */
    protected Stream<PropertyDescriptor> propertyStreamExclude(String... names)
    {
        return attributeStream().map(this::propertyDescriptor)
            .filter(JpaConditionUtils.excludePredicate(names));
    }

    /* Conjunction */

    /**
     * 拼接And条件至WHERE语句
     * merge the And restrictions to WHERE clause
     *
     * @param restrictions 查询条件
     * @return JPA Condition
     * @see CriteriaBuilder
     */
    public JpaCondition clauseAnd(Predicate... restrictions)
    {
        Predicate and = builder.and(restrictions);
        if (Objects.isNull(clausePredicate))
            clausePredicate = and;
        else
            clausePredicate = builder.and(clausePredicate, and);
        return this;
    }

    /**
     * 拼接Or条件至WHERE语句
     * merge the Or restrictions to WHERE clause
     *
     * @param restrictions 查询条件
     * @return JPA Condition
     * @see CriteriaBuilder
     */
    public JpaCondition clauseOr(Predicate... restrictions)
    {
        Predicate or = builder.or(restrictions);
        if (Objects.isNull(clausePredicate))
            clausePredicate = or;
        else
            clausePredicate = builder.or(clausePredicate, or);
        return this;
    }

    /**
     * 拼接And条件
     * Only merge restrictions
     *
     * @param restrictions 查询条件
     * @return Predicate
     * @see CriteriaBuilder
     */
    public Predicate mergeAnd(Predicate... restrictions)
    {
        return builder.and(restrictions);
    }

    /**
     * 拼接Or条件
     * Only merge restrictions
     *
     * @param restrictions 查询条件
     * @return Predicate
     * @see CriteriaBuilder
     */
    public Predicate mergeOr(Predicate... restrictions)
    {
        return builder.or(restrictions);
    }

    /* Predicate Factory */

    /**
     * 生成条件断言
     *
     * @return 条件断言
     */
    public Predicate toPredicate()
    {
        return clausePredicate;
    }

    /**
     * 创建属性条件
     *
     * @param stream    属性流
     * @param predicate 创建条件
     * @return 条件
     */
    @Deprecated
    protected Predicate propertyPredicate(Stream<PropertyDescriptor> stream,
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate)
    {
        Predicate[] predicates =
            predicate.apply(stream).filter(Objects::nonNull) // 过滤空条件
                .toArray(Predicate[]::new);// 转为数组;
        return builder.and(predicates); // 默认and
    }

    /**
     * 为所有属性生成条件断言
     *
     * @param predicate 条件Lambda
     * @return 条件断言
     */
    @Deprecated
    protected Predicate propertyPredicate(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate)
    {
        return propertyPredicate(propertyStream(), predicate);
    }

    /**
     * 为包含的属性生成条件断言
     *
     * @param predicate 条件Lambda
     * @return 条件断言
     */
    @Deprecated
    protected Predicate propertyPredicateInclude(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate,
        String... include)
    {
        return propertyPredicate(propertyStream().filter(
            JpaConditionUtils.includePredicate(include)), predicate);
    }

    /**
     * 为过滤的属性生成条件断言
     *
     * @param predicate 条件Lambda
     * @return 条件断言
     */
    @Deprecated
    protected Predicate propertyPredicateExclude(
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate,
        String... exclude)
    {
        return propertyPredicate(propertyStream().filter(
            JpaConditionUtils.excludePredicate(exclude)), predicate);
    }

    /* Properties Predicate */

    /**
     * Equal条件
     *
     * @return List<Predicate>
     * @author TianGanLin
     * @version [版本号, 2017/8/25]
     */
    public Predicate[] equals()
    {
        return streamToArray(propertyStream().map(this::equal));
    }

    /**
     * Equal条件, 包含所有names
     *
     * @param names 属性名数组
     * @return Predicate
     */
    public Predicate[] equalsInclude(@NotNull String... names)
    {
        return streamToArray(propertyStreamInclude(names).map(this::equal));
    }

    /**
     * Equal条件, 排除所有names
     *
     * @param names 属性名数组
     * @return Predicate
     */
    public Predicate[] equalsExclude(@NotNull String... names)
    {
        return streamToArray(propertyStreamExclude(names).map(this::equal));
    }

    /**
     * Like条件
     *
     * @return Predicate
     * @author TianGanLin
     * @version [1.0.0, 2017-08-28]
     */
    public Predicate[] likes()
    {
        return streamToArray(propertyStream().map(this::like));
    }

    /**
     * Like条件, 包含所有names
     *
     * @return Predicate
     * @author TianGanLin
     * @version [1.0.0, 2017-08-28]
     */
    public Predicate[] likesInclude(@NotNull String... names)
    {
        return streamToArray(propertyStreamInclude(names).map(this::like));
    }

    /**
     * Like条件, 排除所有names
     *
     * @return Predicate
     * @author TianGanLin
     * @version [1.0.0, 2017-08-28]
     */
    public Predicate[] likesExclude(@NotNull String... names)
    {
        return streamToArray(propertyStreamExclude(names).map(this::like));
    }

    /**
     * OrEqual条件, 包含names
     *
     * @param names 属性名数组
     * @return Predicate
     */
    public Predicate orEqualInclude(@NotNull String... names)
    {
        Predicate[] predicates =
            propertyStream().filter(JpaConditionUtils.includePredicate(names))
                .map(this::equal)
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new);
        return builder.or(predicates);
    }


    /* Property Predicate */

    /**
     * Equal条件
     *
     * @param name 属性名
     * @return Predicate
     */
    public Predicate equal(String name)
    {
        return equal(true, propertyDescriptor(name));
    }

    /**
     * Equal条件
     *
     * @param descriptor 属性反射
     * @return Predicate
     */
    protected Predicate equal(PropertyDescriptor descriptor)
    {
        return equal(true, descriptor);
    }

    /**
     * Equal条件
     *
     * @param ignoreNull 忽略空值
     * @param descriptor 属性反射
     * @return Predicate
     */
    protected Predicate equal(boolean ignoreNull, PropertyDescriptor descriptor)
    {
        return valuePredicate(ignoreNull, descriptor,
            (name, value) -> builder.equal(root.get(name), value));
    }

    /**
     * 包含该属性值
     *
     * @param descriptor
     * @return
     */
    protected Predicate like(PropertyDescriptor descriptor)
    {
        return valuePredicate(true, descriptor,
            (name, value) -> builder.like(root.get(name), "%" + value + "%"));
    }

    /**
     * 以属性值开头
     *
     * @param descriptor
     * @return
     */
    protected Predicate likeStart(PropertyDescriptor descriptor)
    {
        return valuePredicate(true, descriptor,
            (name, value) -> builder.like(root.get(name), value + "%"));
    }

    /**
     * 以属性值结尾
     *
     * @param descriptor
     * @return
     */
    protected Predicate likeEnd(PropertyDescriptor descriptor)
    {
        return valuePredicate(true, descriptor,
            (name, value) -> builder.like(root.get(name), "%" + value));
    }

    /**
     * 值条件断言, 对实体类每个属性的值尝试生成条件断言
     *
     * @param ignoreNull 忽略空值
     * @param descriptor 属性
     * @param function   BiFunction<属性名, 属性值, 条件断言>
     * @param <V>        属性值类型
     * @return 条件断言
     */
    protected <V> Predicate valuePredicate(boolean ignoreNull,
        PropertyDescriptor descriptor,
        BiFunction<String, V, Predicate> function)
    {
        String name = descriptor.getName();
        Object value = JpaConditionUtils.getPropertyValue(model, descriptor);
        if (ignoreNull && value == null)
            return null;
        return function.apply(name, (V)value);
    }

    /* Reader */

    protected Class<? extends T> javaType()
    {
        if (javaType == null)
        {
            javaType = root.getJavaType();
        }
        return javaType;
    }

    protected Set<Attribute<? super T, ?>> attributes()
    {
        if (attributes == null)
        {
            attributes = root.getModel().getAttributes();
        }
        return attributes;
    }

    /* Method */

    /**
     * 获得一个实体类属性 spring-bean
     *
     * @param attribute 实体类属性 JPA
     * @return 实体类属性 spring-bean
     */
    protected PropertyDescriptor propertyDescriptor(
        Attribute<? super T, ?> attribute)
    {
        return BeanUtils.getPropertyDescriptor(javaType(), attribute.getName());
    }

    /**
     * 获得一个实体类属性 spring-bean
     *
     * @param name 属性名
     * @return 实体类属性 spring-bean
     */
    protected PropertyDescriptor propertyDescriptor(String name)
    {
        Attribute<? super T, ?> attribute = root.getModel().getAttribute(name);
        return propertyDescriptor(attribute);
    }

    // 将流转为数组
    protected Predicate[] streamToArray(Stream<Predicate> stream)
    {
        return stream.filter(Objects::nonNull).toArray(Predicate[]::new);
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
