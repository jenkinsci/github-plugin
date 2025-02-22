package org.jenkinsci.plugins.github.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author lanwen (Merkushev Kirill)
 */
class XSSApiTest {

    static Object[][] links() {
        return new Object[][]{
                new Object[]{"javascript:alert(1);//", ""},
                new Object[]{"javascript:alert(1)://", ""},
                new Object[]{"http://abcxyz.com?a=b&c=d';alert(1);//", "http://abcxyz.com?a=b&c=d';alert(1);//"},
                new Object[]{"http://github.com/bla/bla", "http://github.com/bla/bla"},
                new Object[]{"https://github.com/bla/bla", "https://github.com/bla/bla"},
                new Object[]{"https://company.com/bla", "https://company.com/bla"},
                new Object[]{"/company.com/bla", ""},
                new Object[]{"//", ""},
                new Object[]{"//text", ""},
                new Object[]{"//text/", ""},
                new Object[]{"ftp://", "ftp:"},
                new Object[]{"ftp://a", "ftp://a"},
                new Object[]{"text", ""},
                new Object[]{"github.com/bla/bla", ""},
                new Object[]{"http://127.0.0.1/", "http://127.0.0.1/"},
        };
    }

    @ParameterizedTest
    @MethodSource("links")
    void shouldSanitizeUrl(String url, String expected) throws Exception {
        assertThat(format("For %s", url), XSSApi.asValidHref(url), is(expected));
    }
}
