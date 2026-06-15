package com.trafficsim;

import javafx.application.Application;

/**
 * Plain (non-{@link Application}) entry point.
 *
 * <p>Launching JavaFX through a class that does <em>not</em> extend {@link Application} avoids the
 * "JavaFX runtime components are missing" error you get when the JavaFX modules are on the classpath
 * rather than the module path — which is exactly how the {@code javafx-maven-plugin} runs them. This
 * is the class referenced by {@code <mainClass>} in the POM.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
