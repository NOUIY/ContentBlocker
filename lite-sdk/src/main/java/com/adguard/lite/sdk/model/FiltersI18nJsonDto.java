package com.adguard.lite.sdk.model;

import androidx.annotation.Keep;

import java.util.Map;

/**
 * DTO for filters localizations
 *
 * TODO use all fields
 */
@Keep
public class FiltersI18nJsonDto {

    private Map<Integer, Map<String, NameDesc>> groups;
    private Map<Integer, Map<String, NameDesc>> filters;
    private Map<Integer, Map<String, NameDesc>> tags;

    /**
     * @param groups Groups localizations: {@link Map} of filter ID to {@link Map} of language code to {@link NameDesc}
     */
    public void setGroups(Map<Integer, Map<String, NameDesc>> groups) {
        this.groups = groups;
    }

    /**
     * @param filters Filters localizations: {@link Map} of filter ID to {@link Map} of language code to {@link NameDesc}
     */
    public void setFilters(Map<Integer, Map<String, NameDesc>> filters) {
        this.filters = filters;
    }

    /**
     * @param tags Tags localizations: {@link Map} of filter ID to {@link Map} of language code to {@link NameDesc}
     */
    public void setTags(Map<Integer, Map<String, NameDesc>> tags) {
        this.tags = tags;
    }

    /**
     * @return Groups localizations: {@link Map} of filter ID to {@link Map} of language code to {@link NameDesc}
     */
    public Map<Integer, Map<String, NameDesc>> getGroups() {
        return groups;
    }

    /**
     * @return Filters localizations: {@link Map} of filter ID to {@link Map} of language code to {@link NameDesc}
     */
    public Map<Integer, Map<String, NameDesc>> getFilters() {
        return filters;
    }

    /**
     * @return Tags localizations: {@link Map} of filter ID to {@link Map} of language code to {@link NameDesc}
     */
    public Map<Integer, Map<String, NameDesc>> getTags() {
        return tags;
    }

    /**
     * Pair of filter name and description
     */
    @Keep
    public static class NameDesc {
        private String name;
        private String description;

        /**
         * @param description Filter description
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * @param name Filter name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return Filter name
         */
        public String getName() {
            return name;
        }

        /**
         * @return Filter description
         */
        public String getDescription() {
            return description;
        }
    }
}
