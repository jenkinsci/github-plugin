package org.jenkinsci.plugins.github.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(DataProviderRunner.class)
public class XSSApiTest {

    @DataProvider
    public static Object[][] links() {
        return new Object[][]{
                new Object[]{"javascript:alert(1);//", ""},
                new Object[]{"http://abcxyz.com?a=b&c=d';alert(1);//", "http://abcxyz.com?a=b&c=d';alert(1);//"},
                new Object[]{"http://github.com/bla/bla", "http://github.com/bla/bla"},
                new Object[]{"https://github.com/bla/bla", "https://github.com/bla/bla"},
                new Object[]{"https://company.com/bla", "https://company.com/bla"}
        };
    }

    @Test
    @UseDataProvider("links")
    public void shouldSanitizeUrl(String url, String expected) throws Exception {
        assertThat(XSSApi.asValidHref(url), is(expected));
    }
}
