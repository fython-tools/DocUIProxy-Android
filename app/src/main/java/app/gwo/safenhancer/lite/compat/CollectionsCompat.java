package app.gwo.safenhancer.lite.compat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;

import static java.util.Objects.requireNonNull;

public final class CollectionsCompat {

    private CollectionsCompat() {}

    @NonNull
    public static <E, T extends Collection<R>, R> T mapTo(
            @NonNull Collection<E> source,
            @NonNull T destination,
            @NonNull Function<E, R> mapFunc
    ) {
        requireNonNull(source);
        requireNonNull(destination);
        requireNonNull(mapFunc);
        if (!destination.isEmpty()) {
            throw new IllegalArgumentException("Destination should be empty.");
        }
        for (E element : source) {
            destination.add(mapFunc.accept(element));
        }
        return destination;
    }

    @NonNull
    public static <E, R> List<R> mapToList(
            @NonNull Collection<E> source,
            @NonNull Function<E, R> mapFunc
    ) {
        return mapTo(source, new ArrayList<>(), mapFunc);
    }

    @NonNull
    public static <E, T extends Collection<E>> T filterTo(
            @NonNull Collection<E> source,
            @NonNull T destination,
            @NonNull Predicate<E> predicate
    ) {
        requireNonNull(source);
        requireNonNull(destination);
        requireNonNull(predicate);
        if (!destination.isEmpty()) {
            throw new IllegalArgumentException("Destination should be empty.");
        }
        for (E element : source) {
            if (predicate.accept(element)) {
                destination.add(element);
            }
        }
        return destination;
    }

    @NonNull
    public static <E> List<E> filterToList(@NonNull Collection<E> source, @NonNull Predicate<E> p) {
        return filterTo(source, new ArrayList<>(), p);
    }

    @NonNull
    public static <E> boolean anyMatch(@NonNull Collection<E> source, @NonNull Predicate<E> p) {
        requireNonNull(source);
        requireNonNull(p);
        for (E element : source) {
            if (p.accept(element)) {
                return true;
            }
        }
        return false;
    }

}
