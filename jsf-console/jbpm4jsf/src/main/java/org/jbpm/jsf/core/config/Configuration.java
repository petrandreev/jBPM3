package org.jbpm.jsf.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
public final class Configuration {
    private final List<FileMatcher> matchers;
    private final boolean useJsfActorId;

    public Configuration(final List<FileMatcher> matchers, final boolean useJsfActorId) {
        this.matchers = Collections.unmodifiableList(new ArrayList<FileMatcher>(matchers));
        this.useJsfActorId = useJsfActorId;
    }

    public List<FileMatcher> getMatchers() {
        return matchers;
    }

    public boolean useJsfActorId() {
        return useJsfActorId;
    }

    public static final class FileMatcher {
        private final Pattern pattern;
        private final String contentType;
        private final String file;

        public FileMatcher(final Pattern pattern, final String contentType, final String file) {
            this.pattern = pattern;
            this.contentType = contentType;
            this.file = file;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFile() {
            return file;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }
}
