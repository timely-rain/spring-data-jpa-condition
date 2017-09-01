package org.springframework.data.jpa.domain;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * @author TianGanLin
 * @version [版本号, 2017/9/1]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@FunctionalInterface
public interface ConditionSpecification<T> extends ParallelSpecificationSupport
{
    /**
     * Creates a WHERE clause for a query of the referenced entity in form of a {@link Predicate} for the given
     * {@link Root} and {@link CriteriaQuery}.
     *
     * @param root       Root
     * @param query      CriteriaQuery
     * @param cb         CriteriaBuilder
     * @param predicates List<Predicate>
     * @return a {@link Predicate}, must not be {@literal null}.
     */
    void toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb,
        List<Predicate> predicates, JpaCondition<T> condition);
}
