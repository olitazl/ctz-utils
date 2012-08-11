package ru.concerteza.util.reflect.named;

import com.google.common.base.*;
import com.google.common.collect.*;
import org.apache.commons.lang.UnhandledException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import ru.concerteza.util.collection.CtzCollectionUtils;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static ru.concerteza.util.string.CtzFormatUtils.format;

/**
 * todo: update docs
 * Guava function, converts unordered data map into object instance using {@link NamedConstructor_OLD}. Designed
 * to use with immutable classes (with final fields) - only constructor invocation is used without any field access.
 * Constructor arguments must be annotated with JSR330 {@link Named} annotations (there are other ways to access
 * constructor names in runtime, see <a href="http://paranamer.codehaus.org/">paranamer project</a>, but we use
 * {@code @Named} annotations only). Constructors without {@code @Named} annotations on arguments will be ignored.
 * Constructors with {@code @Named} annotations must have all they arguments annotated with not blank values without
 * duplicates. {@code Optional} constructor arguments values will be wrapped into {@code Optional} before
 * constructor invocation. All reflection introspection is done on function instantiation.
 * For case insensitive names all {@code @Named} values must be locale insensitive. Null values are allowed
 * only for {@code com.google.common.base.Optional} arguments.
 *
 * @param <T> object type to instantiate
 * @author alexey
 *         Date: 7/6/12
 * @see NamedConstructor_OLD
 * @see ru.concerteza.util.db.springjdbc.named.NamedConstructorMapper
 */
public class NamedConstructor<T> {
    private final Constructor<T> constr;
    private final Map<String, NamedConstructor> children;
    private final boolean leaf;
    private final boolean optional;

    @SuppressWarnings("unchecked") // declared constructor generic type
    public NamedConstructor(Class<T> type, Optional<Class> genericType) {
        checkNotNull(type, "Provided type is null");
        checkNotNull(genericType, "Provided genericType is null");
        this.optional = Optional.class.isAssignableFrom(type);
        List<ConstrHolder> namedList = findNamed(genericType.isPresent() ? genericType.get() : type);
        checkArgument(namedList.size() <= 1, "Only one named constructor per class is allowed, type: '%s', found: '%s'", type, namedList);
        this.leaf = 0 == namedList.size();
        if(leaf) {
            this.constr = null;
            this.children = ImmutableMap.of();
        } else {
            ConstrHolder named = namedList.get(0);
            this.constr = (Constructor) named.constr;
            if(!constr.isAccessible()) constr.setAccessible(true);
            ImmutableMap.Builder<String, NamedConstructor> builder = ImmutableMap.builder();
            for(NamedConstructorArg nca : named.args) {
                builder.put(nca.name, new NamedConstructor(nca.type, nca.genericType));
            }
            this.children = builder.build();
        }
    }

    public static <T> NamedConstructor<T> of(Class<T> clazz) {
        return new NamedConstructor<T>(clazz, Optional.<Class>absent());
    }

    public T invoke(Map<String, ?> map) {
        return invoke(map, true);
    }

    public T invoke(Map<String, ?> map, boolean caseSensitive) {
        checkNotNull(map, "Provided map is null");
        if(!caseSensitive) map = CtzCollectionUtils.toLowerKeysMap(map);
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        for(Map.Entry<String, NamedConstructor> pr : children.entrySet()) {
            String key = caseSensitive ? pr.getKey() : pr.getKey().toLowerCase(Locale.ENGLISH);
            checkArgument(map.containsKey(key), "No value provided for named argument: '%s', constructor: '%s'", key, this);
            Object raw = map.get(key);
            final Object instantiated;
            if(null != raw) instantiated = pr.getValue().invoke(raw);
            else {
                if(pr.getValue().optional) instantiated = Optional.absent();
                else throw new IllegalArgumentException(format("Not optional null value on key: '{}' " +
                        "in provided map: '{}', constructor: '{}'", key, map, this));
            }
            builder.add(instantiated);
        }
        return invokeConstructor(builder.build());
    }

    @SuppressWarnings("unchecked")
    public List<T> invoke(Iterable<?> iter) {
        checkNotNull(iter, "Provided iter is null, constructor: '%s'", this);
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for(Object ob : iter) {
            checkNotNull(ob, "Null value in provided iter: '%s', constructor: '%S'", iter, this);
            builder.add(invoke(ob));
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private T invoke(Object any) {
        checkNotNull(any, "Provided object is null, constructor: '%s'", this);
        final Object res;
        if(any instanceof Map) res = invoke((Map<String, ?>) any);
        else if(any instanceof Iterable) res = invoke((Iterable<?>) any);
        // must be primitive
        else if(leaf) res = any;
        else throw new IllegalArgumentException(format("Unsupported input type: '{}' provided to non-leaf constructor: '{}'", any, this));
        return (T) (optional ? Optional.of(res) : res);
    }

    private T invokeConstructor(List<?> args) {
        try {
            return constr.newInstance(args.toArray());
        } catch(InstantiationException e) {
            throw new UnhandledException(e);
        } catch(IllegalAccessException e) {
            throw new UnhandledException(e);
        } catch(InvocationTargetException e) {
            throw new UnhandledException(e);
        }
    }

    private  List<ConstrHolder> findNamed(Class<T> type) {
        ImmutableList.Builder<ConstrHolder> builder = ImmutableList.builder();
        for(Constructor<?> co : type.getDeclaredConstructors()) {
            LinkedHashSet<NamedConstructorArg> args = extractNames(co);
            if(args.size() > 0) {
                builder.add(new ConstrHolder(co, args));
            }
        }
        return builder.build();
    }

    private LinkedHashSet<NamedConstructorArg> extractNames(Constructor<?> co) {
        LinkedHashSet<NamedConstructorArg> res = Sets.newLinkedHashSet();
        String coStr = co.toGenericString();
        Iterator<Annotation[]> anIter = Iterators.forArray(co.getParameterAnnotations());
        Class<?>[] typesArr = co.getParameterTypes();
        Iterator<Class<?>> typesIter = Iterators.forArray(typesArr);
        while (anIter.hasNext() || typesIter.hasNext()) {
            Annotation[] anns = anIter.next();
            Class<?> type = typesIter.next();
            List<Annotation> namedList = ImmutableList.copyOf(Collections2.filter(asList(anns), NamedAnnPred.INSTANCE));
            if(0 == namedList.size()) continue; // no named or not consistent constructor
            checkArgument(1 == namedList.size(), "Multiple named annotations: '{}' found for type: '{}'", namedList, type);
            Annotation named = namedList.get(0);
            if(named instanceof Named) {
                Named na = (Named) named;
                checkArgument(isNotBlank(na.value()), "@Named annotation with empty value found, type: '%s'", type);
                boolean unique = res.add(new NamedConstructorArg(na.value(), type)); // matched on name only
                checkArgument(unique, "Not unique @Named or @NamedGenericRef value: '%s', constructor: '%s'", na.value(), coStr);
            } else if(named instanceof NamedGenericRef) {
                NamedGenericRef li = (NamedGenericRef) named;
                checkArgument(isNotBlank(li.name()), "@NamedGenericRef annotation with empty value found, type: '%s'", type);
                checkArgument(null != li.type(), "@NamedGenericRef annotation with null type found, parent type: '%s'", type);
                boolean unique = res.add(new NamedConstructorArg(li.name(), type, li.type())); // matched on name only
                checkArgument(unique, "Not unique @Named or @NamedGenericRef value: '%s', constructor: '%s'", li.name(), coStr);
            }
            else throw new IllegalStateException(format("Unknown annotation to process: '{}'", named));
        }
        checkArgument(0 == res.size() || typesArr.length == res.size(), "Not consistent @Named annotations found for constructor: '%s'", coStr);
        return res;
    }

    private enum NamedAnnPred implements Predicate<Annotation> {
        INSTANCE;
        @Override
        public boolean apply(Annotation input) {
            return input instanceof Named || input instanceof NamedGenericRef;
        }
    }

    private static class ConstrHolder {
        private final Constructor<?> constr;
        private final LinkedHashSet<NamedConstructorArg> args;

        public ConstrHolder(Constructor<?> constr, LinkedHashSet<NamedConstructorArg> args) {
            checkNotNull(constr, "Provided constrcutor is null");
            checkNotNull(args, "Provided constructor args are null");
            checkArgument(args.size() > 0, "Provided constructor args are empty");
            this.constr = constr;
            this.args = args;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
                    append("constr", constr).
                    append("args", args).
                    toString();
        }
    }
}
