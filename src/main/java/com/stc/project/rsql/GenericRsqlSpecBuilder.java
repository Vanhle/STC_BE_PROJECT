package com.stc.project.rsql;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.LogicalOperator;
import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huyvv
 * Date: 4/6/2025
 * Time: 2:37 PM
 * for all issues, contact me: huyvv@vnpt-technology.vn
 **/
public class GenericRsqlSpecBuilder<T> {

    public Specification<T> createSpecification(Node node) {
        if (node instanceof LogicalNode) {
            return createSpecification((LogicalNode) node);
        }
        if (node instanceof ComparisonNode) {
            return createSpecification((ComparisonNode) node);
        }
        return null;
    }

    public Specification<T> createSpecification(LogicalNode logicalNode) {
        List<Specification<T>> specs = new ArrayList<>();

        for (Node child : logicalNode.getChildren()) {
            Specification<T> spec = createSpecification(child);
            if (spec != null) {
                specs.add(spec);
            }
        }

        if (specs.isEmpty()) return null;

        Specification<T> result = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            if (logicalNode.getOperator() == LogicalOperator.AND) {
                result = result.and(specs.get(i));
            } else if (logicalNode.getOperator() == LogicalOperator.OR) {
                result = result.or(specs.get(i));
            }
        }

        return result;
    }

    public Specification<T> createSpecification(ComparisonNode comparisonNode) {
        return new GenericRsqlSpecification<>(
                comparisonNode.getSelector(),
                comparisonNode.getOperator(),
                comparisonNode.getArguments()
        );
    }
}
