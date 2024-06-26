package com.example.activmon;

import java.util.HashSet;

public class DataFilter {
    private String previousData;
    private String currentData;

    public DataFilter() {
        // Initialize previousData and currentData as null initially
        previousData = null;
        currentData = null;
    }

    public String filterData(String newData) {

        // Store the current data as previous data
        previousData = currentData;

        // Update the current data with the new data
        currentData = newData;

        if (previousData == null) {
            return newData;
        }

        // Check if both previousData and currentData exist and if they are similar
        if (previousData.equalsIgnoreCase(currentData)) {
            // If similar data is found, return null to indicate it should not be processed further
            return null;
        } else {
            // Otherwise, return the current data
            return currentData;
        }
    }
}
