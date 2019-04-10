package app.gwo.safenhancer.lite.compat;

import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class Optional<T> {

    private static final Optional EMPTY = new Optional<>(null);

    @NonNull
    public static <T> Optional<T> empty() {
        return (Optional<T>) EMPTY;
    }

    @NonNull
    public static <T> Optional<T> of(@NonNull T nonnullValue) {
        return new Optional<>(requireNonNull(nonnullValue));
    }

    @NonNull
    public static <T> Optional<T> ofNullable(@Nullable T value) {
        if (value == null) {
            return empty();
        } else {
            return of(value);
        }
    }

    @NonNull
    public static <T> Optional<T> ofCallable(@NonNull Callable<T> callable) {
        T result = null;
        try {
            result = callable.call();
        } catch (Exception ignored) {
        }
        return ofNullable(result);
    }

    @Nullable
    private T value;

    private Optional(@Nullable T value) {
        this.value = value;
    }

    @NonNull
    public <D> Optional<D> map(@NonNull Function<T, D> function) {
        if (value != null) {
            return ofNullable(function.accept(value));
        } else {
            return empty();
        }
    }

    public Optional<T> filter(@NonNull Predicate<T> predicate) {
        if (value != null && predicate.accept(value)) {
            return this;
        } else {
            return empty();
        }
    }

    public void ifPresent(@NonNull Consumer<T> callable) {
        if (value != null) {
            callable.accept(value);
        }
    }

    public boolean isPresent() {
        return value != null;
    }

    @Nullable
    public T get() {
        return value;
    }

    @NonNull
    public T orElse(@NonNull T defaultValue) {
        if (value != null) {
            return value;
        } else {
            return requireNonNull(defaultValue);
        }
    }

    @NonNull
    public T orElseGet(@NonNull Callable<T> callable) {
        if (value != null) {
            return value;
        } else {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nullable
    public T orElseNullable(@Nullable T defaultValue) {
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

}
