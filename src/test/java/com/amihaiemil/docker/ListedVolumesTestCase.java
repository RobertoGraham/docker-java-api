package com.amihaiemil.docker;

import com.amihaiemil.docker.mock.AssertRequest;
import com.amihaiemil.docker.mock.Condition;
import com.amihaiemil.docker.mock.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableWithSize;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ListedVolumes}.
 * @author Boris Kuzmic (boris.kuzmic@gmail.com)
 * @since 0.0.7
 */
public final class ListedVolumesTestCase {

    /**
     * {@link ListedVolumes} can iterate over them without
     * filters.
     */
    @Test
    public void iterateAll() {
        final Volumes all = new ListedVolumes(
            new AssertRequest(
                new Response(
                    HttpStatus.SC_OK,
                    "[{\"Name\": \"abc1\"}, {\"Name\":\"cde2\"}]"
                ),
                new Condition(
                    "iterate() must send a GET request",
                    req -> "GET".equals(req.getRequestLine().getMethod())
                ),
                new Condition(
                    "iterate() resource URL must be '/volumes'",
                    req -> req.getRequestLine()
                            .getUri().endsWith("/volumes")
                )
            ),
            URI.create("http://localhost/volumes"),
            Mockito.mock(Docker.class)
        );
        MatcherAssert.assertThat(
            "There should be 2 volumes in the list",
            all,
            new IsIterableWithSize<>(
                new IsEqual<>(2)
            )
        );
        final Iterator<Volume> itr = all.iterator();
        MatcherAssert.assertThat(
            "Name should match abc1",
            itr.next().getString("Name"),
            new IsEqual<>("abc1")
        );
        MatcherAssert.assertThat(
            "Name should match cde2",
            itr.next().getString("Name"),
            new IsEqual<>("cde2")
        );
    }

    /**
     * Tests if {@link ListedVolumes} can filter volumes.
     * @throws IOException If something goes wrong.
     */
    @Test
    public void iterateWithFilters() throws IOException {
        new ListedVolumes(
            new AssertRequest(
                new Response(
                    HttpStatus.SC_OK,
                    //@checkstyle LineLength (1 line)
                    "[{\"Name\": \"abc1\"}, {\"Name\": \"def2\"}, {\"Name\": \"ghi3\"}, {\"Name\":\"jkl4\"}]"
                ),
                new Condition(
                    "iterate() must send a GET request",
                    req -> "GET".equals(req.getRequestLine().getMethod())
                ),
                new Condition(
                    //@checkstyle LineLength (1 line)
                    "iterate() query parameters must include the filters provided",
                    req -> {
                        // @checkstyle LineLength (1 line)
                        final List<NameValuePair> params = new UncheckedUriBuilder(
                            req.getRequestLine().getUri()
                        ).getQueryParams();
                        // @checkstyle BooleanExpressionComplexity (5 lines)
                        return params.size() == 1
                            && "filters".equals(params.get(0).getName())
                            && params.get(0).getValue().contains("Name")
                            && params.get(0).getValue().contains("\"def2\"")
                            && params.get(0).getValue().contains("\"jkl4\"");
                    }
                )
            ),
            URI.create("http://localhost/volumes"),
            Mockito.mock(Docker.class),
            Collections.singletonMap("Name", Arrays.asList("def2", "jkl4"))
        ).iterator();
    }
}
