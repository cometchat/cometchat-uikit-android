package com.cometchat.chatuikit.shared.framework;

import com.cometchat.chatuikit.shared.interfaces.Function1;

import java.util.ArrayList;
import java.util.List;

/**
 * The ChatConfigurator class provides a way to configure and switch between
 * different data sources, allowing for customization and flexibility in the
 * chat functionality.
 */
public class ChatConfigurator {
    private static final String TAG = ChatConfigurator.class.getSimpleName();


    // The default data source implementation
    private static DataSource defaultDataSource = new MessagesDataSource();

    // List to keep track of unique data source IDs
    private static final List<String> ids = new ArrayList<>();

    public static void init() {
        defaultDataSource = new MessagesDataSource();
        ids.clear();
    }

    public static void init(DataSource dataSource) {
        defaultDataSource = dataSource;
        ids.clear();
    }

    /**
     * Enables custom behavior by replacing the current data source with a new one.
     *
     * @param interfaceFunction A function that takes the old data source and returns a new data
     *                          source.
     */
    public static void enable(Function1<DataSource, DataSource> interfaceFunction) {
        if (interfaceFunction != null) {
            DataSource oldDataSource = defaultDataSource;
            DataSource newDataSource = interfaceFunction.apply(oldDataSource);
            if (!ids.contains(newDataSource.getId())) {
                ids.add(newDataSource.getId());
                defaultDataSource = newDataSource;
            }
        }
    }

    /**
     * Returns the current data source.
     *
     * @return The current data source.
     */
    public static DataSource getDataSource() {
        return defaultDataSource;
    }
}
