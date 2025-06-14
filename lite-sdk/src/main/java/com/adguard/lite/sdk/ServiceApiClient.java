/*
 * This file is part of AdGuard Content Blocker (https://github.com/AdguardTeam/ContentBlocker).
 * Copyright © 2018 AdGuard Content Blocker. All rights reserved.
 * <p/>
 * AdGuard Content Blocker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * <p/>
 * AdGuard Content Blocker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * AdGuard Content Blocker.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.adguard.lite.sdk;

import com.adguard.lite.sdk.api.HttpServiceClient;
import com.adguard.lite.sdk.commons.web.UrlUtils;
import com.adguard.lite.sdk.model.FilterList;
import com.adguard.lite.sdk.model.FiltersI18nJsonDto;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Special client to communicate with out backend
 */
public class ServiceApiClient extends HttpServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceApiClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Downloads filter rules
     *
     * @param filterId Filter id
     * @return List of rules
     */
    public static List<String> downloadFilterRules(int filterId, String filtersUrl) throws IOException {
        String downloadUrl = filtersUrl.replace("{0}", UrlUtils.urlEncode(Integer.toString(filterId)));

        LOG.info("Sending request to {}", downloadUrl);
        String response = downloadString(downloadUrl);

        LOG.debug("Response length is {}", response.length());
        String[] rules = StringUtils.split(response, "\r\n");
        List<String> filterRules = new ArrayList<>();
        for (String line : rules) {
            String rule = StringUtils.trim(line);
            if (!StringUtils.isEmpty(rule)) {
                filterRules.add(rule);
            }
        }

        return filterRules;
    }

    /**
     * Downloads filters localizations
     *
     * @return {@link FiltersI18nJsonDto}
     * @throws IOException if downloading or mapping failed
     */
    public static FiltersI18nJsonDto downloadFiltersLocalizations(String filterLocalizationsUrl) throws IOException {
        LOG.info("Sending request to {}", filterLocalizationsUrl);
        String response = downloadString(filterLocalizationsUrl);
        if (StringUtils.isBlank(response)) {
            throw new IOException("Failed to download filters localizations. Response is empty.");
        }
        return OBJECT_MAPPER.readValue(response, FiltersI18nJsonDto.class);
    }

    /**
     * Downloads filter versions.
     *
     * @param filters list
     * @return filters list with downloaded versions
     */
    public static List<FilterList> downloadFilterVersions(List<FilterList> filters, String checkFilterVerionsUrl) throws IOException {
        LOG.info("Sending request to {}", checkFilterVerionsUrl);
        String response = downloadString(checkFilterVerionsUrl);
        if (StringUtils.isBlank(response)) {
            return null;
        }

        Set<Integer> filterIds = new HashSet<>(filters.size());
        for (FilterList filter : filters) {
            filterIds.add(filter.getFilterId());
        }

        Map map = readValue(response, Map.class);
        if (map == null || !map.containsKey("filters")) {
            LOG.error("Filters parse error! Response:\n{}", response);
            return null;
        }

        ArrayList filterList = (ArrayList) map.get("filters");
        List<FilterList> result = new ArrayList<>(filters.size());
        String[] parsePatterns = {"yyyy-MM-dd'T'HH:mm:ssZ"};

        for (Object filterObj : filterList) {
            Map filter = (Map) filterObj;
            int filterId = (int) filter.get("filterId");
            if (!filterIds.contains(filterId)) {
                continue;
            }
            FilterList list = new FilterList();
            list.setName((String) filter.get("name"));
            list.setDescription((String) filter.get("description"));
            list.setFilterId(filterId);
            list.setVersion((String) filter.get("version"));
            try {
                String timeUpdated = (String) filter.get("timeUpdated");
                list.setTimeUpdated(DateUtils.parseDate(timeUpdated, parsePatterns));
            } catch (ParseException e) {
                LOG.error("Unable to parse date from filters:\n", e);
            }
            result.add(list);
        }

        return result;

    }

    private static Map readValue(String src, Class<Map> valueType) {
        try {
            return OBJECT_MAPPER.readValue(src, valueType);
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Cannot parse URL={}:\r\n", src, ex);
            }
            return null;
        }
    }
}
