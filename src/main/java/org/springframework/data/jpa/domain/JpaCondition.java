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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private T model;

    private Root<T> root;

    private CriteriaQuery<?> query;

    private CriteriaBuilder builder;

    /* Cache */
    private Class<? extends T> javaType;

    private Set<Attribute<? super T, ?>> attributes;

    private List<Predicate> predicates;

    /* Constractor */
    public JpaCondition(Root<T> root, CriteriaQuery<?> query,
        CriteriaBuilder builder)
    {
        this.root = root;
        this.query = query;
        this.builder = builder;
        this.javaType = root.getJavaType();
        this.predicates = new ArrayList<>();
    }


    /* Conjunction */

    public JpaCondition and(Predicate... restrictions)
    {
        Predicate and = builder.or(restrictions);
        this.predicates.add(and);
        return this;
    }

    public JpaCondition or(Predicate... restrictions)
    {
        Predicate or = builder.or(restrictions);
        this.predicates.add(or);
        return this;
    }

    /* Predicate Factory */

    /**
     * 创建属性条件
     *
     * @param stream    属性流
     * @param predicate 创建条件
     * @return 条件
     */
    protected Predicate propertyPredicate(Stream<PropertyDescriptor> stream,
        Function<Stream<PropertyDescriptor>, Stream<Predicate>> predicate)
    {
        Predicate[] predicates =
            predicate.apply(stream).filter(p -> p != null) // 过滤空条件
                .toArray(Predicate[]::new);// 转为数组;
        return builder.and(predicates); // 默认and
    }

    /**
     * 生成条件断言
     *
     * @return 条件断言
     */
    public Predicate toPredicate()
    {
        Predicate[] predicates =
            this.predicates.toArray(new Predicate[this.predicates.size()]);
        return builder.and(predicates);
    }

    /**
     * 为所有属性生成条件断言
     *
     * @param predicate 条件Lambda
     * @return 条件断言
     */
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
    public Predicate equals()
    {
        return propertyPredicate(stream -> stream.map(this::equal));
    }

    /**
     * Equal条件, 包含所有names
     *
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
     *
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
     *
     * @return Predicate
     * @author TianGanLin
     * @version [1.0.0, 2017-08-28]
     */
    public Predicate likes()
    {
        return propertyPredicate(stream -> stream.map(this::like));
    }

    /**
     * Like条件, 包含所有names
     *
     * @return Predicate
     * @author TianGanLin
     * @version [1.0.0, 2017-08-28]
     */
    public Predicate likesInclude(@NotNull String... names)
    {
        return propertyPredicateInclude(stream -> stream.map(this::like),
            names);
    }

    /**
     * Like条件, 排除所有names
     *
     * @return Predicate
     * @author TianGanLin
     * @version [1.0.0, 2017-08-28]
     */
    public Predicate likesExclude(@NotNull String... names)
    {
        return propertyPredicateExclude(stream -> stream.map(this::like),
            names);
    }

    /**
     * Or条件, 包含names
     *
     * @param names 属性名数组
     * @return Predicate
     */
    public Predicate orInclude(@NotNull String... names)
    {
        Optional<Predicate> optional =
            propertyStream().filter(JpaConditionUtils.includePredicate(names))
                .map(this::equal)
                .reduce((left, right) -> builder.or(left, right));
        return optional.get();
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
     * @param <T>        属性值类型
     * @return 条件断言
     */
    protected <T> Predicate valuePredicate(boolean ignoreNull,
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

    protected Class<? extends T> javaType()
    {
        if (javaType == null)
        {
            javaType = root.getJavaType();
        }
        return javaType;
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

    protected Set<Attribute<? super T, ?>> attributes()
    {
        if (attributes == null)
        {
            attributes = root.getModel().getAttributes();
        }
        return attributes;
    }

    /**
     * 获得实体类的Managed属性流的spring-beans版
     *
     * @return Stream<PropertyDescriptor>
     */
    protected Stream<PropertyDescriptor> propertyStream()
    {
        return attributeStream().map(this::propertyDescriptor);
    }

    /**
     * 获得实体类的Managed属性流
     *
     * @return Stream<Attribute>
     */
    protected Stream<Attribute<? super T, ?>> attributeStream()
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
