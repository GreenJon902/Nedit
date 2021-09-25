package me.nullicorn.nedit.provider;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import me.nullicorn.nedit.provider.annotation.ProvideAllAtOnce;
import me.nullicorn.nedit.provider.annotation.ProvideTagTypes;
import me.nullicorn.nedit.provider.type.ByteArrayProvider;
import me.nullicorn.nedit.provider.type.ByteProvider;
import me.nullicorn.nedit.provider.type.CompoundProvider;
import me.nullicorn.nedit.provider.type.DoubleProvider;
import me.nullicorn.nedit.provider.type.FloatProvider;
import me.nullicorn.nedit.provider.type.IntArrayProvider;
import me.nullicorn.nedit.provider.type.IntProvider;
import me.nullicorn.nedit.provider.type.ListProvider;
import me.nullicorn.nedit.provider.type.LongArrayProvider;
import me.nullicorn.nedit.provider.type.LongProvider;
import me.nullicorn.nedit.provider.type.ShortProvider;
import me.nullicorn.nedit.provider.type.StringProvider;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * A provider for valid NBT values (those that can be stored in an {@link NBTList} or {@link
 * NBTCompound}).
 * <p><br>
 * If a test using this provider is annotated with {@link ProvideTagTypes}, then a second argument
 * will also be provided to indicate the {@link TagType} of the first argument.
 * <p><br>
 * If a test using this provider is annotated with {@link ProvideAllAtOnce}, then only one argument
 * will be provided: a {@code Set<Object>} containing all of the tag values that would otherwise be
 * provided in separate runs. When {@link ProvideTagTypes} is also present, that single argument
 * becomes a {@code Map<Object, TagType>}.
 *
 * @author Nullicorn
 * @see ProvideAllAtOnce
 * @see ProvideTagTypes
 */
public class AllTagsProvider implements ArgumentsProvider {

    @Override
    public Stream<Arguments> provideArguments(ExtensionContext context) {
        // Determine whether or not to include a second TagType argument.
        boolean doIncludeTypes = context
            .getRequiredTestMethod()
            .isAnnotationPresent(ProvideTagTypes.class);

        // Determine whether or not to provide all values at once in a single Map<Object, TagType>.
        boolean doProvideAllAtOnce = context
            .getRequiredTestMethod()
            .isAnnotationPresent(ProvideAllAtOnce.class);

        // Compile all values from each tag-type's provider.
        Map<Object, TagType> tagValues = aggregateTagValues();

        // Provide all tags at once as a single collection.
        if (doProvideAllAtOnce) {
            if (doIncludeTypes) {
                // Provide values and types (Map<Object, TagType>)
                return Stream.of(arguments(tagValues));
            } else {
                // Return only the values (Set<Object>).
                return Stream.of(arguments(tagValues.keySet()));
            }
        }

        // Provide each tag individually.
        Stream.Builder<Arguments> stream = Stream.builder();
        tagValues.forEach((value, type) -> {
            if (doIncludeTypes) {
                // Provide values and types (Object, TagType).
                stream.accept(arguments(value, type));
            } else {
                // Provide only the values (Object).
                stream.accept(arguments(value));
            }
        });
        return stream.build();
    }

    private static Map<Object, TagType> aggregateTagValues() {
        Map<Object, TagType> tags = new HashMap<>();

        for (TagType type : TagType.values()) {
            if (type == TagType.END) {
                continue;
            }

            // 1. Get an array of values directly from the provider.
            // 2. Iterate over each value in the array.
            // 3. Add the value & its type to the map.
            reflectiveForEach(getProviderForType(type).provide(),
                value -> tags.put(value, type)
            );
        }

        return tags;
    }

    /**
     * Determines the corresponding argument provider that should be used to provide NBT values of
     * the supplied {@code type}.
     */
    @SuppressWarnings("unchecked")
    public static <A> TagProvider<A> getProviderForType(TagType type) {
        TagProvider<?> provider;

        switch (type) {
            case BYTE:
                provider = new ByteProvider();
                break;
            case SHORT:
                provider = new ShortProvider();
                break;
            case INT:
                provider = new IntProvider();
                break;
            case LONG:
                provider = new LongProvider();
                break;
            case FLOAT:
                provider = new FloatProvider();
                break;
            case DOUBLE:
                provider = new DoubleProvider();
                break;
            case BYTE_ARRAY:
                provider = new ByteArrayProvider();
                break;
            case INT_ARRAY:
                provider = new IntArrayProvider();
                break;
            case LONG_ARRAY:
                provider = new LongArrayProvider();
                break;
            case STRING:
                provider = new StringProvider();
                break;
            case LIST:
                provider = new ListProvider();
                break;
            case COMPOUND:
                provider = new CompoundProvider();
                break;
            default:
                throw new IllegalArgumentException("Unable to find provider for tag: " + type);
        }

        return (TagProvider<A>) provider;
    }

    /**
     * Reflectively iterates over an object that is assumed to be an {@code array} of "{@code T}"
     * values.
     *
     * @throws IllegalArgumentException If the {@code array} argument is not actually an array.
     * @throws NullPointerException     If either argument is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static <T> void reflectiveForEach(Object array, Consumer<T> action) {
        Objects.requireNonNull(array);
        Objects.requireNonNull(action);
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Not an array: " + array);
        }

        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            action.accept((T) Array.get(array, i));
        }
    }
}