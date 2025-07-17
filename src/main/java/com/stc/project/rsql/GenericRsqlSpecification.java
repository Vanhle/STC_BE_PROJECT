package com.stc.project.rsql;

import com.stc.project.constants.Constants;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by huyvv
 * Date: 4/6/2025
 * Time: 2:37 PM
 * for all issues, contact me: huyvv@vnpt-technology.vn
 **/
public class GenericRsqlSpecification<T> implements Specification<T> {
    private static final Logger log = LoggerFactory.getLogger(GenericRsqlSpecification.class);
    private String property;
    private ComparisonOperator operator;
    private List<String> arguments;
    List<String> entityList = null;
    String lastProperty = null;
    Path<T> pathWithChild = null;
    Join<?,?> join =  null ;
    public GenericRsqlSpecification(
            String property, ComparisonOperator operator, List<String> arguments) {
        super();
        this.property = property;
        this.operator = operator;
        this.arguments = arguments;
    }

    @Override
    public Predicate toPredicate(
            Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (property.contains(".")) {
            return toPredicateWithChilds(root, query, builder);
        }
        List<Object> args = castArguments(root);
        if (entityList != null && entityList.size() > 0) {
            return toPredicateWithChilds(root, query, builder);
        }
        Object argument = args.get(0);
        switch (RsqlSearchOperation.getSimpleOperator(operator)) {

            case EQUAL: {
                Path path = root.<String>get(property);
                if ("expression".equals(property)) {
                    String listArg[] = argument.toString().split(";");
                    for (int i = 0; i < listArg.length; i++) {
                        if (listArg[i].indexOf("groupBy") > -1) {
                            if (!("").equals(listArg[i].split("==")[1])) {
                                query.groupBy(root.<String>get(listArg[i].split("==")[1]));
                            }
                        }
                        if (listArg[i].indexOf("orderBy") > -1) {
                            String condition = listArg[i].split("==")[1];
                            if (!("").equals(condition)) {
                                String conditions[] = condition.split(",");
                                if (conditions.length > 1 && ("desc").equals(conditions[1])) {
                                    query.orderBy(builder.desc(root.<String>get(conditions[0])));
                                }
                            }
                        }
                    }
                    break;
                }

                if (argument instanceof String) {
                    if (("null").equals(argument)) {
                        return builder.isNull(root.get(property));
                    }
                    if (path.getJavaType() == Boolean.class) {
                        String tmp = (String) argument;
                        String val = "1".equals(tmp) ? "true" : ("0".equals(tmp) ? "false" : tmp);
                        return builder.equal(root.get(property), Boolean.valueOf(val));
                    }

                    if (path.getJavaType() == Integer.class || path.getJavaType() == int.class) {
                        return builder.equal(root.get(property), Integer.valueOf(argument.toString()));
                    }

                    if (path.getJavaType() == Double.class) {
                        return builder.equal(root.get(property), Double.valueOf(argument.toString()));
                    }
                    if (path.getJavaType() == BigDecimal.class) {
                        // return builder.equal(root.get(property),BigDecimal.valueOf(Long.parseLong(argument.toString())));
                        return builder.equal(root.get(property), BigDecimal.valueOf(Double.valueOf(argument.toString())));
                    }

                    // check wildcard character of sql
                    // % _ [ ] ^ -

                    if (((String) argument).contains("%") ||
                            ((String) argument).contains("_") ||
                            ((String) argument).contains("[") ||
                            ((String) argument).contains("]") ||
                            ((String) argument).contains("^") ||
                            ((String) argument).contains("-") ||
                            ((String) argument).contains("*")) {
                        String finalArgument = ((String) argument)
                                .replace("%", "!%")
                                .replace("_", "!_")
                                .replace("[", "![")
                                .replace("]", "!]")
                                .replace("^", "!^")
                                .replace("-", "!-");
                        return builder.like(
                                root.<String>get(property), finalArgument.replace("%20", " ").replace('*', '%'), '!');
                    }

                    if (((String) argument).matches(Constants.CHARACTER_NOT_CONTAIN_SPECIAL_CHARACTER)) {
                        return builder.like(
                                root.get(property), argument.toString().replace("%20", " ").replace('*', '%'));
                    } else {
                        return builder.equal(
                                builder.function("BINARY", Byte[].class,root.<String>get(property)),
                                argument.toString().replace("%20", " ").replace('*', '%').getBytes());
                    }
                } else if (argument == null) {
                    return builder.isNull(root.get(property));
                } else {
                    return builder.equal(root.get(property), argument);
                }
            }
            case NOT_EQUAL: {
                if (argument instanceof String) {
                    // check wildcard character of sql
                    // % _ [ ] ^ -

                    if (((String) argument).contains("%") ||
                            ((String) argument).contains("_") ||
                            ((String) argument).contains("[") ||
                            ((String) argument).contains("]") ||
                            ((String) argument).contains("^") ||
                            ((String) argument).contains("-")) {
                        String finalArgument = ((String) argument)
                                .replace("%", "!%")
                                .replace("_", "!_")
                                .replace("[", "![")
                                .replace("]", "!]")
                                .replace("^", "!^")
                                .replace("-", "!-");
                        return builder.notLike(
                                root.<String>get(property), finalArgument.replace("%20", " ").replace('*', '%'), '!');
                    }

                    return builder.notLike(
                            root.<String>get(property), argument.toString().replace("%20", " ").replace('*', '%'));
                } else if (argument == null) {
                    return builder.isNotNull(root.get(property));
                } else {
                    return builder.notEqual(root.get(property), argument);
                }
            }
            case GREATER_THAN: {
                return builder.greaterThan(root.<String>get(property), argument.toString());
            }
            case GREATER_THAN_OR_EQUAL: {
                return builder.greaterThanOrEqualTo(
                        root.<String>get(property), argument.toString());
            }
            case LESS_THAN: {
                return builder.lessThan(root.<String>get(property), argument.toString());
            }
            case LESS_THAN_OR_EQUAL: {
                return builder.lessThanOrEqualTo(
                        root.<String>get(property), argument.toString());
            }
            case IN:
                return root.get(property).in(args);
            case NOT_IN:
                return builder.not(root.get(property).in(args));
        }

        return null;
    }

    public Predicate toPredicateWithChilds(
            Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Predicate result = null;
        List<Object> args = castArgumentsWithChilds(root);
        Object argument = args.get(0);
        switch (RsqlSearchOperation.getSimpleOperator(operator)) {

            case EQUAL: {
                Path path = pathWithChild.<String>get(lastProperty);
                if ("expression".equals(property)) {
                    String listArg[] = argument.toString().split(";");
                    for (int i = 0; i < listArg.length; i++) {
                        if (listArg[i].indexOf("groupBy") > -1) {
                            if (!("").equals(listArg[i].split("==")[1])) {
                                query.groupBy(join.<String>get(listArg[i].split("==")[1]));
                            }
                        }
                        if (listArg[i].indexOf("orderBy") > -1) {
                            String condition = listArg[i].split("==")[1];
                            if (!("").equals(condition)) {
                                String conditions[] = condition.split(",");
                                if (conditions.length > 1 && ("desc").equals(conditions[1])) {
                                    query.orderBy(builder.desc(root.<String>get(conditions[0])));
                                }
                            }
                        }
                    }
                    break;
                }

                if (argument instanceof String) {
                    if (("null").equals(argument)) {
                        result = builder.isNull(getPreWithChild(root,join));
                    }
                    if (path.getJavaType() == Boolean.class) {
                        String tmp = (String) argument;
                        String val = "1".equals(tmp) ? "true" : ("0".equals(tmp) ? "false" : tmp);
                        result = builder.equal(getPreWithChild(root,join), Boolean.valueOf(val));
                    }

                    if (path.getJavaType() == Double.class) {
                        return builder.equal(getPreWithChild(root,join), Double.valueOf(argument.toString()));
                    }
                    if (path.getJavaType() == BigDecimal.class) {
                        // return builder.equal(root.get(property),BigDecimal.valueOf(Long.parseLong(argument.toString())));
                        return builder.equal(getPreWithChild(root,join), BigDecimal.valueOf(Double.valueOf(argument.toString())));
                    }

                    // check wildcard character of sql
                    // % _ [ ] ^ -

                    if (((String) argument).contains("%") ||
                            ((String) argument).contains("_") ||
                            ((String) argument).contains("[") ||
                            ((String) argument).contains("]") ||
                            ((String) argument).contains("^") ||
                            ((String) argument).contains("-")) {
                        String finalArgument = ((String) argument)
                                .replace("%", "!%")
                                .replace("_", "!_")
                                .replace("[", "![")
                                .replace("]", "!]")
                                .replace("^", "!^")
                                .replace("-", "!-");
                        result = builder.like(
                                getPreWithChild(root,join), finalArgument.replace("%20", " ").replace('*', '%'), '!');
                    }

                    result = builder.like(
                            getPreWithChild(root,join), argument.toString().replace("%20", " ").replace('*', '%'));
                } else if (argument == null) {
                    result = builder.isNull(getPreWithChild(root,join));
                } else {
                    result = builder.equal(getPreWithChild(root,join), argument);
                }
                break;
            }
            case NOT_EQUAL: {
                if (argument instanceof String) {
                    // check wildcard character of sql
                    // % _ [ ] ^ -

                    if (((String) argument).contains("%") ||
                            ((String) argument).contains("_") ||
                            ((String) argument).contains("[") ||
                            ((String) argument).contains("]") ||
                            ((String) argument).contains("^") ||
                            ((String) argument).contains("-")) {
                        String finalArgument = ((String) argument)
                                .replace("%", "!%")
                                .replace("_", "!_")
                                .replace("[", "![")
                                .replace("]", "!]")
                                .replace("^", "!^")
                                .replace("-", "!-");
                        result = builder.notLike(
                                getPreWithChild(root,join), finalArgument.replace("%20", " ").replace('*', '%'), '!');
                    }

                    result = builder.notLike(
                            getPreWithChild(root,join), argument.toString().replace("%20", " ").replace('*', '%'));
                } else if (argument == null) {
                    result = builder.isNotNull(getPreWithChild(root,join));
                } else {
                    result = builder.notEqual(getPreWithChild(root,join), argument);
                }
                break;
            }
            case GREATER_THAN: {
                result = builder.greaterThan(getPreWithChild(root,join), argument.toString());
                break;
            }
            case GREATER_THAN_OR_EQUAL: {
                result = builder.greaterThanOrEqualTo(
                        getPreWithChild(root,join), argument.toString());
                break;
            }
            case LESS_THAN: {
                result = builder.lessThan(getPreWithChild(root,join), argument.toString());
                break;
            }
            case LESS_THAN_OR_EQUAL: {
                result = builder.lessThanOrEqualTo(
                        getPreWithChild(root,join), argument.toString());
                break;
            }
            case IN:
                result = getPreWithChild(root,join).in(args);
                break;
            case NOT_IN:
                result = builder.not(getPreWithChild(root,join).in(args));
                break;
        }

        entityList = null;
        lastProperty = null;
        pathWithChild = null;
        join =  null ;
        return result;
    }

    private Path getPreWithChild(Root<T> root,Join<?,?> join) {
        return pathWithChild.get(lastProperty);
    }

    private List<Object> castArguments(Root<T> root) {
        List<Object> args = new ArrayList<Object>();
        Class<? extends Object> type = root.get(property).getJavaType();

        for (String argument : arguments) {
            if (type.equals(Integer.class)) {
                if (!("null").equals(argument)) {
                    args.add(Integer.parseInt(argument));
                } else {
                    args.add(null);
                }
            } else if (type.equals(Long.class)) {
                if (!("null").equals(argument)) {
                    args.add(Long.parseLong(argument));
                } else {
                    args.add(null);
                }
            } else {
                args.add(argument);
            }
        }

        return args;
    }

    private List<Object> castArgumentsWithChilds(Root<T> root) {
        List<Object> args = new ArrayList<Object>();
        Class<? extends Object> type = null;

        entityList = Arrays.asList(this.property.split(Pattern.quote(".")));
        for (int i = 0; i < entityList.size() - 1; i++) {
            String e = entityList.get(i);

            if(pathWithChild==null){
                pathWithChild = root.get(e);
                join =  root.join(e);
            }else{
                pathWithChild = pathWithChild.get(e);
                join.join(e);
            }
        }
        lastProperty = entityList.get(entityList.size() - 1);
        type = pathWithChild.get(lastProperty).getJavaType();

        for (String argument : arguments) {
            if (lastProperty != null) {
                if (type.equals(Integer.class)) {
                    if (!("null").equals(argument)) {
                        args.add(Integer.parseInt(argument));
                    } else {
                        args.add(null);
                    }
                } else if (type.equals(Long.class)) {
                    if (!("null").equals(argument)) {
                        args.add(Long.parseLong(argument));
                    } else {
                        args.add(null);
                    }
                } else {
                    args.add(argument);
                }
            }
        }

        return args;
    }
}
