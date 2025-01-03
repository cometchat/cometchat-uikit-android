package com.cometchat.chatuikit.shared.interfaces;

public interface Consumer<T, K, S, R, V> {
    void apply(T t, K k, S s, R r, V v);
}
