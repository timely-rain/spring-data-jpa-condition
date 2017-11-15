package org.springframework.data.jpa.condition;

import com.sun.istack.internal.NotNull;
import org.springframework.beans.BeanUtils;

import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import java.beans.PropertyDescriptor;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * 极简JPA动态查询
 *
 * @author TianGanLin
 * @version [1.0.0, 2017/8/25]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class JpaCondition<T> {
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
                        CriteriaBuilder builder) {
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
    protected Stream<Attribute<? super T, ?>> attributeStream() {
        return attributes().stream();
    }

    /**
     * 获得实体类的spring-beans属性流
     *
     * @return Stream<PropertyDescriptor>
     */
    protected Stream<PropertyDescriptor> propertyStream() {
        return attributeStream().map(this::propertyDescriptor);
    }

    /**
     * 获得实体类的spring-beans属性流
     * include by names
     *
     * @return Stream<PropertyDescriptor>
     */
    protected Stream<PropertyDescriptor> propertyStreamInclude(String... names) {
        return attributeStream().map(this::propertyDescriptor)
                .filter(Objects::nonNull)
                .filter(JpaConditionUtils.includePredicate(names));
    }

    /**
     * 获得实体类的spring-beans属性流
     * exclude by names
     *
     * @return Stream<PropertyDescriptor>
     */
    protected Stream<PropertyDescriptor> propertyStreamExclude(String... names) {
        return attributeStream().map(this::propertyDescriptor)
                .filter(Objects::nonNull)
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
    public JpaCondition clauseAnd(Predicate... restrictions) {
        Predicate and = mergeAnd(restrictions);
        if (Objects.isNull(and)) return this;
        clausePredicate = Objects.isNull(clausePredicate) ? and : builder.and(clausePredicate, and);
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
    public JpaCondition clauseOr(Predicate... restrictions) {
        Predicate or = mergeOr(restrictions);
        if (Objects.isNull(or)) return this;
        clausePredicate = Objects.isNull(clausePredicate) ? or : builder.or(clausePredicate, or);
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
    public Predicate mergeAnd(Predicate... restrictions) {
        // 过滤空值
        Predicate[] predicates = Stream.of(restrictions).filter(Objects::nonNull).toArray(Predicate[]::new);
        if (predicates.length == 0) return null;
        return builder.and(predicates);
    }

    /**
     * 拼接Or条件
     * Only merge restrictions
     *
     * @param restrictions 查询条件
     * @return Predicate
     * @see CriteriaBuilder
     */
    public Predicate mergeOr(Predicate... restrictions) {
        // 过滤空值
        Predicate[] predicates = Stream.of(restrictions).filter(Objects::nonNull).toArray(Predicate[]::new);
        if (predicates.length == 0) return null;
        return builder.or(predicates);
    }

    /* Predicate Factory */

    /**
     * 生成条件断言
     *
     * @return 条件断言
     */
    public Predicate toPredicate() {
        return clausePredicate;
    }

    /* Properties Predicate */

    /**
     * Equal条件
     *
     * @return List<Predicate>
     * @apiNote range:Entity.attributes
     */
    public Predicate[] equals() {
        return streamToArray(propertyStream().map(this::equal));
    }

    /**
     * Equal条件
     *
     * @param names 属性名数组
     * @return Predicate
     * @apiNote range:Entity.attributes in names
     */
    public Predicate[] equalsInclude(@NotNull String... names) {
        return streamToArray(propertyStreamInclude(names).map(this::equal));
    }

    /**
     * Equal条件
     *
     * @param names 属性名数组
     * @return Predicate
     * @apiNote range:Entity.attributes not in names
     */
    public Predicate[] equalsExclude(@NotNull String... names) {
        return streamToArray(propertyStreamExclude(names).map(this::equal));
    }

    /**
     * Like条件
     *
     * @return Predicate
     */
    public Predicate[] likes() {
        return streamToArray(propertyStream().map(this::like));
    }

    /**
     * Like条件, 包含所有names
     *
     * @return Predicate
     */
    public Predicate[] likesInclude(@NotNull String... names) {
        return streamToArray(propertyStreamInclude(names).map(this::like));
    }

    /**
     * Like条件, 排除所有names
     *
     * @return Predicate
     */
    public Predicate[] likesExclude(@NotNull String... names) {
        return streamToArray(propertyStreamExclude(names).map(this::like));
    }

    /**
     * OrEqual条件
     *
     * @param names 属性名数组
     * @return Predicate
     * @apiNote Entity.attributes in names
     * @apiNote builder.or(equalsInclude(names))
     */
    public Predicate orEqualInclude(@NotNull String... names) {
        Predicate[] predicates = equalsInclude(names);
        return builder.or(predicates);
    }

    /* Property Predicate */

    /**
     * Equal条件
     *
     * @param name 属性名
     * @return Predicate
     */
    public Predicate equal(String name) {
        return propertyPredicate(true, propertyDescriptor(name), builder::equal);
    }

    /**
     * 大于
     *
     * @param name 属性名
     * @return Predicate
     */
    public Predicate greaterThan(String name) {
        return greaterThan(name, name);
    }

    /**
     * 大于
     *
     * @param name      属性名
     * @param valueName 取值属性名
     * @return Predicate
     */
    public Predicate greaterThan(String name, String valueName) {
        return propertyComparablePredicate(name, propertyDescriptor(valueName), builder::greaterThan);
    }


    /**
     * 小于或等于
     *
     * @param name 属性名
     * @return Predicate
     */
    public Predicate greaterThanOrEqualTo(String name) {
        return greaterThanOrEqualTo(name, name);
    }

    /**
     * 小于或等于
     *
     * @param name      属性名
     * @param valueName 取值属性名
     * @return Predicate
     */
    public Predicate greaterThanOrEqualTo(String name, String valueName) {
        return propertyComparablePredicate(name, propertyDescriptor(valueName), builder::greaterThanOrEqualTo);
    }


    /**
     * 小于
     *
     * @param name 属性名
     * @return Predicate
     */
    public Predicate lessThan(String name) {
        return lessThan(name, name);
    }

    /**
     * 小于
     *
     * @param name      属性名
     * @param valueName 取值属性名
     * @return Predicate
     */
    public Predicate lessThan(String name, String valueName) {
        return propertyComparablePredicate(name, propertyDescriptor(valueName), builder::lessThan);
    }

    /**
     * 小于或等于
     *
     * @param name 属性名
     * @return Predicate
     */
    public Predicate lessThanOrEqualTo(String name) {
        return lessThanOrEqualTo(name, name);
    }

    /**
     * 小于或等于
     *
     * @param name      属性名
     * @param valueName 取值属性名
     * @return Predicate
     */
    public Predicate lessThanOrEqualTo(String name, String valueName) {
        return propertyComparablePredicate(name, propertyDescriptor(valueName), builder::lessThanOrEqualTo);
    }

    /**
     * Between条件
     *
     * @param name 属性名, Entity中必须有[name+"Start"]和[name+"End"]属性
     * @return Predicate
     * @apiNote startValue <= root.get(name) < endValue
     */
    public <T extends Comparable<? super T>> Predicate between(String name) {
        return this.between(true, name);
    }

    /**
     * Between条件
     *
     * @param ignoreNull 忽略空值
     * @param name       属性名, Entity中必须有[name+"Start"]和[name+"End"]属性
     * @return Predicate
     * @apiNote startValue <= root.get(name) < endValue
     */
    public <T extends Comparable<? super T>> Predicate between(boolean ignoreNull, String name) {
        PropertyDescriptor start = propertyDescriptor(name + "Start");
        PropertyDescriptor end = propertyDescriptor(name + "End");
        @SuppressWarnings("unchecked") T startValue = (T) JpaConditionUtils.getPropertyValue(model, start);
        @SuppressWarnings("unchecked") T endValue = (T) JpaConditionUtils.getPropertyValue(model, end);
        return this.between(name, startValue, endValue);
    }

    /**
     * Between条件
     *
     * @param name       属性名
     * @param startValue 起始值
     * @param endValue   结束值
     * @param <T>        值类型
     * @return Predicate
     * @apiNote startValue <= root.get(name) < endValue
     */
    public <T extends Comparable<? super T>> Predicate between(String name, T startValue, T endValue) {
        if (Objects.isNull(startValue) && Objects.isNull(endValue))
            return null;
        if (Objects.isNull(startValue))
            return builder.lessThan(root.get(name), endValue);
        if (Objects.isNull(endValue))
            return builder.greaterThanOrEqualTo(root.get(name), startValue);
        Predicate s = builder.greaterThanOrEqualTo(root.get(name), startValue);
        Predicate e = builder.lessThan(root.get(name), endValue);
        return this.mergeAnd(s, e);
    }

    /* Custom Property Predicate */

    /**
     * 属性条件断言, 对实体类属性尝试生成条件断言
     * 支持 Spring Data JPA 方法引用
     *
     * @param name     属性名
     * @param function BiFunction<属性表达式, 属性值, 条件断言>
     * @param <P>      属性表达式类型
     * @return 条件断言
     */
    public <P, V> Predicate propertyPredicate(String name,
                                              BiFunction<Expression<P>, V, Predicate> function) {
        return propertyPredicate(true, name, function);
    }

    /**
     * 属性条件断言, 对实体类属性尝试生成条件断言
     * 支持 Spring Data JPA 方法引用
     *
     * @param ignoreNull 忽略空值
     * @param name       属性名
     * @param function   BiFunction<属性表达式, 属性值, 条件断言>
     * @param <P>        属性表达式类型
     * @return 条件断言
     */
    public <P, V> Predicate propertyPredicate(boolean ignoreNull, String name,
                                              BiFunction<Expression<P>, V, Predicate> function) {
        return propertyPredicate(ignoreNull, propertyDescriptor(name), function);
    }

    /* Custom Properties Predicate */

    /**
     * 属性条件断言, 批量对实体类属性尝试生成条件断言
     *
     * @param function BiFunction<属性表达式, 属性值, 条件断言>
     * @param <P>      属性表达式类型
     * @param <V>      属性值类型
     * @return 条件断言
     */
    public <P, V> Predicate[] propertiesPredicate(
            BiFunction<Expression<P>, V, Predicate> function) {
        return propertiesPredicate(true, function);
    }

//    public <P extends Number> Predicate[] propertiesPredicate(
//        BiFunction<Expression<P>, P, Predicate> function)
//    {
//        return propertiesPredicate(true, function);
//    }


    /**
     * 属性条件断言, 批量对实体类属性尝试生成条件断言
     *
     * @param ignoreNull 忽略空值
     * @param function   BiFunction<属性表达式, 属性值, 条件断言>
     * @param <P>        属性表达式类型
     * @param <V>        属性值类型
     * @return 条件断言
     */
    public <P, V> Predicate[] propertiesPredicate(boolean ignoreNull,
                                                  BiFunction<Expression<P>, V, Predicate> function) {
        Stream<Predicate> predicateStream = propertyStream().map(
                // 对实体类属性尝试生成条件断言
                propertyDescriptor -> propertyPredicate(ignoreNull,
                        propertyDescriptor, function));
        return streamToArray(predicateStream);
    }

    /* Property Predicate Support */

    /**
     * Equal条件
     *
     * @param descriptor 属性反射
     * @return Predicate
     */
    protected Predicate equal(PropertyDescriptor descriptor) {
        return propertyPredicate(true, descriptor, builder::equal);
    }

    /**
     * 包含该属性值
     *
     * @param descriptor
     * @return
     */
    protected Predicate like(PropertyDescriptor descriptor) {
        return propertyPredicate(descriptor,
                (Expression<String> name, Object value) -> builder
                        .like(name, "%" + value + "%"));
    }

    /**
     * 以属性值开头
     *
     * @param descriptor
     * @return
     */
    protected Predicate likeStart(PropertyDescriptor descriptor) {
        return propertyPredicate(true, descriptor,
                (Expression<String> name, Object string) -> builder
                        .like(name, string + "%"));
    }

    /**
     * 以属性值结尾
     *
     * @param descriptor
     * @return
     */
    protected Predicate likeEnd(PropertyDescriptor descriptor) {
        return propertyPredicate(true, descriptor,
                (Expression<String> property, String value) -> builder
                        .like(property, "%" + value));
    }

    /**
     * 属性条件断言, 对实体类属性尝试生成条件断言
     *
     * @param descriptor 属性反射
     * @param function   BiFunction<属性表达式, 属性值, 条件断言>
     * @param <P>        属性表达式类型
     * @return 条件断言
     */
    protected <P, V> Predicate propertyPredicate(PropertyDescriptor descriptor,
                                                 BiFunction<Expression<P>, V, Predicate> function) {
        return propertyPredicate(true, descriptor, function);
    }

    /**
     * 属性匹配条件断言, 对实体类属性尝试生成条件断言
     *
     * @param ignoreNull 忽略空值
     * @param descriptor 属性反射
     * @param function   BiFunction<属性表达式, 属性值, 条件断言>
     * @param <P>        属性表达式类型
     * @param <V>        属性zhi类型
     * @return 条件断言
     */
    @SuppressWarnings("unchecked")
    protected <P, V> Predicate propertyPredicate(
            boolean ignoreNull,
            PropertyDescriptor descriptor,
            BiFunction<Expression<P>, V, Predicate> function) {
        String name = descriptor.getName();
        Object value = JpaConditionUtils.getPropertyValue(model, descriptor);
        if (ignoreNull && value == null) return null;
        return function.apply(root.get(name), (V) value);
    }

    /**
     * 属性比较条件, 对实体类属性尝试生成条件断言
     *
     * @param descriptor 属性反射
     * @param function   BiFunction<属性表达式, 属性值, 条件断言>
     * @param <P>        属性表达式类型
     * @param <V>        属性zhi类型
     * @return 条件断言
     */
    @SuppressWarnings("unchecked")
    private <P extends Comparable<? super V>, V extends Comparable> Predicate propertyComparablePredicate(
            String name,
            PropertyDescriptor descriptor,
            BiFunction<Expression<P>, V, Predicate> function) {
        Object value = JpaConditionUtils.getPropertyValue(model, descriptor);
        if (Objects.isNull(value)) return null;
        return function.apply(root.get(name), (V) value);
    }
    /* Reader */

    protected Class<? extends T> javaType() {
        if (javaType == null) {
            javaType = root.getJavaType();
        }
        return javaType;
    }


    protected Set<Attribute<? super T, ?>> attributes() {
        if (attributes == null) {
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
            Attribute<? super T, ?> attribute) {
        String name = attribute.getName();
        // 解决 PropertyDescriptor 无法正确获取 isFoo 属性的问题
        if (name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2)))
            name = Character.toLowerCase(name.charAt(2)) + name.substring(3);
        return propertyDescriptor(name);
    }

    /**
     * 获得一个实体类属性 spring-bean
     *
     * @param name 属性名
     * @return 实体类属性 spring-bean
     */
    protected PropertyDescriptor propertyDescriptor(String name) {
        return BeanUtils.getPropertyDescriptor(javaType(), name);
    }

    // 将流转为数组
    protected Predicate[] streamToArray(Stream<Predicate> stream) {
        return stream.filter(Objects::nonNull).toArray(Predicate[]::new);
    }

    /* Getter And Setter */

    public T getModel() {
        return model;
    }

    public JpaCondition<T> setModel(T model) {
        this.model = model;
        return this;
    }

    public Root<T> getRoot() {
        return root;
    }

    public void setRoot(Root<T> root) {
        this.root = root;
    }

    public CriteriaQuery<?> getQuery() {
        return query;
    }

    public void setQuery(CriteriaQuery<?> query) {
        this.query = query;
    }

    public CriteriaBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(CriteriaBuilder builder) {
        this.builder = builder;
    }

    public Class<? extends T> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class<? extends T> javaType) {
        this.javaType = javaType;
    }
}
