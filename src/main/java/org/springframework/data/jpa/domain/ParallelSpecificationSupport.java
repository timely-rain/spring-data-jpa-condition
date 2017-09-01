package org.springframework.data.jpa.domain;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.Collection;

/**
 * @author TianGanLin
 * @version [版本号, 2017/9/1]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public interface ParallelSpecificationSupport
{
    /**
     * 合并条件集合
     *
     * @param cb         CriteriaBuilder
     * @param predicates 条件集合
     * @return 条件
     */
    default Predicate mergePredicate(CriteriaBuilder cb,
        Collection<Predicate> predicates)
    {
        return cb.and(predicates.toArray(new Predicate[predicates.size()]));
    }
}
