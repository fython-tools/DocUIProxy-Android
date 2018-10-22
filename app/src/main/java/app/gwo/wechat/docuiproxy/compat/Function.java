package app.gwo.wechat.docuiproxy.compat;

public interface Function<T, D> {

    D accept(T value);

}
