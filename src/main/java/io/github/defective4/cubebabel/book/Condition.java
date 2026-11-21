package io.github.defective4.cubebabel.book;

import java.util.function.Function;

import io.github.defective4.cubebabel.LibraryServer;

public enum Condition {
    HAS_DICTIONARY(_ -> LibraryServer.getInstance().getDictionary().isPresent());

    private final Function<Object, Boolean> checker;

    private Condition(Function<Object, Boolean> checker) {
        this.checker = checker;
    }

    public boolean isTrue() {
        return checker.apply(null);
    }
}
