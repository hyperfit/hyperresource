package org.hyperfit.hyperresource.serializer.haljson.jackson2;

import static org.junit.Assert.*;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


import com.fasterxml.jackson.annotation.JsonView;
import org.hyperfit.hyperresource.controls.TemplatedAction;
import org.hamcrest.Matchers;
import org.hyperfit.hyperresource.HyperResource;
import org.hyperfit.hyperresource.annotation.Rel;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONAssert;

import org.hyperfit.hyperresource.controls.Link;


public class HALJSONJacksonSerializerTest {

    private static String readResourceAsString(String resource){
        return new Scanner(
            HALJSONJacksonSerializerTest.class.getClassLoader()
                .getResourceAsStream(resource),
            StandardCharsets.UTF_8.name()
        )
        .useDelimiter("\\A")
        .next();

    }

    private HALJSONJacksonSerializer writer = new HALJSONJacksonSerializer();


    private ByteArrayOutputStream outputStream;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        outputStream = new ByteArrayOutputStream();

    }

    @Test
    public void testGetContentTypes(){
        assertThat(writer.getContentTypes(), Matchers.contains("application/hal+json"));

    }

    @Test
    public void testCanWrite(){

        assertTrue("any HyperResource can be serialized as hal+json", writer.canWrite(HyperResource.class));
        assertTrue("any HyperResource can be serialized as hal+json", writer.canWrite(new HyperResource(){}.getClass()));
    }



    @Test
    public void testWriteResourceNoControls() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public int getVal() {
                return 1;
            }

        };

        writer.write(resource, null, outputStream);

        String expectedString = "{\"val\":1}";

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithOneLinkControl() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link getImage() {
                return new Link("bb:image", "some/url/to/image", "small", "PNG", "A TITLE");
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithOneLinkControl.json");


        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithTwoLinkControls() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link getImage() {
                return new Link("bb:image", "some/url/to/image", "small", "PNG");
            }

            @SuppressWarnings("unused")
            public Link getSelf() {
                return new Link("self", "some/url/to/resource");
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoLinkControls.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithNullLinkControl() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link getLink() {
                return null;
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = "{}";


        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithLinkArray() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link[] getProfile() {
                return new Link[]{
                    new Link("profile", "prof1"), new Link("profile", "prof2")
                };
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayControl.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithLinkArrayNLink() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link[] getProfile() {
                return new Link[]{
                    new Link("profile", "prof1"),
                    new Link("profile", "prof2")
                };
            }

            @SuppressWarnings("unused")
            public Link getSelf() {
                return new Link("self", "some/url/to/resource");
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayNLink.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithLinkArrayNull() throws IOException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link[] getProfile() {
                return null;
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = "{}";

        assertEquals(
            expectedString,
            outputStream.toString()
        );

        assertEquals(
            expectedString,
            writer.writeToString(resource, null)
        );
    }

    @Test
    public void testWriteResourceWithProfileLink() throws IOException, JSONException {
        //we should not be treating profile any more special than any other rel
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link getProfile() {
                return new Link("profile", "prof1");
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithProfileLinkIsArray.json");


        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithLinkArrayWith1Entry() throws IOException, JSONException {
        //If a link is exposed via a method returning an array of links, we should
        //always serialize the rel as an array, as this is how devs indicate they want
        //an array
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link[] getDogs() {
                return new Link[]{
                    new Link("dog", "dog1")
                };
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayWith1Entry.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithLinkArrayWithTwoEntriesDifferentRels() throws IOException, JSONException {
        //Interesting edge case here...if you return links with different rels in an array they also
        //are forced to be serialized as an array
        //i don't forsee anyone doing this...but they might
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public Link[] getAnimals() {
                return new Link[]{
                    new Link("dog", "dog1"),
                    new Link("cat", "cat1")
                };
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayWithTwoEntriesDifferentRels.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithListProperty() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public List<String> getList() {
                List<String> list = new ArrayList<>();
                list.add("foo1");
                list.add("foo2");
                return list;
            }
        };
        writer.write(resource, null, outputStream);

        String expectedString = "{\"list\":[\"foo1\",\"foo2\"]}";

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithOneSubResource() throws IOException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:child")
            public HyperResource getResource() {
                return new HyperResource() {

                    @SuppressWarnings("unused")
                    public String getFoo() {
                        return "foo";
                    }
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = "{\"_embedded\":{\"bb:child\":{\"foo\":\"foo\"}}}";

        assertEquals(
            expectedString,
            outputStream.toString()
        );

        assertEquals(
            expectedString,
            writer.writeToString(resource, null)
        );
    }

    @Test
    public void testWriteResourceWithOneSubResourceNoRelAnnotation() throws IOException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public HyperResource getResource() {
                return new HyperResource() {

                    @SuppressWarnings("unused")
                    public String getFoo() {
                        return "foo";
                    }
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = "{\"_embedded\":{\"resource\":{\"foo\":\"foo\"}}}";

        assertEquals(
            expectedString,
            outputStream.toString()
        );

        assertEquals(
            expectedString,
            writer.writeToString(resource, null)
        );
    }

    @Test
    public void testWriteResourceWithNullSubresource() throws IOException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public HyperResource getResource() {
                return null;
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = "{}";

        assertEquals(
            expectedString,
            outputStream.toString()
        );

        assertEquals(
            expectedString,
            writer.writeToString(resource, null)
        );
    }

    @Test
    public void testWriteResourceWithTwoSubResourcesWithSameRel() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:children")
            public HyperResource getResource1() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
            }

            @SuppressWarnings("unused")
            @Rel("bb:children")
            public HyperResource getResource2() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoSubResourcesWithSameRel.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }


    @Test
    public void testWriteResourceWithTwoSameSubResourcesDifferentRels() throws IOException, JSONException {
        HyperResource sub = new HyperResource() {

            @SuppressWarnings("unused")
            public String getFoo() {
                return "foo";
            }
        };

        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:children1")
            public HyperResource getResource1() {
                return sub;
            }

            @SuppressWarnings("unused")
            @Rel("bb:children2")
            public HyperResource getResource2() {
                return sub;
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoSameSubResourcesDifferentRels.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }




    @Test
    public void testWriteResourceWithSubresourceArray() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:children")
            public HyperResource[] getResource() {
                HyperResource child1 = new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
                HyperResource child2 = new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
                return new HyperResource[]{child1, child2};
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoSubResourcesWithSameRel.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithEmptySubresourceArray() throws IOException, JSONException {
        //By default we exclude empty sub resource arrays
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:children")
            public HyperResource[] getResource() {
                return new HyperResource[]{};
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = "{}";

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithNullSubresourceArray() throws IOException, JSONException {
        //nulls are ignored in general..so this is no different than an empty aray
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:children")
            public HyperResource[] getResource() {
                return null;
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = "{}";

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithOneSubResourceWithOneLink() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:child")
            public HyperResource getResource() {
                return new HyperResource() {
                    public Link getLink() {
                        return new Link("rel", "some/url");
                    }
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithOneSubResourceWithOneLink.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }

    @Test
    public void testWriteResourceWithTwoDepthSubresources() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:child1")
            public HyperResource getResource() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo£";
                    }

                    @SuppressWarnings("unused")
                    @Rel("bb:child2")
                    public HyperResource getResource() {
                        return new HyperResource() {
                            public String getFoo() {
                                return "foo£";
                            }
                        };
                    }
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoDepthSubresources.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }


    @Test
    public void testWriteResourceWithSubresourceArrayWithSingleEntry() throws IOException, JSONException {
        // a subresource returned as part of an array always should serialize as an array
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:child")
            public HyperResource[] getResource() {
                return new HyperResource[]{
                    new HyperResource() {
                        public String getFoo() {
                            return "foo1";
                        }
                    }
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithSubresourceArrayWithSingleEntry.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }


    @Test
    public void testWriteResourceWithSubresourceArrayWithSingleEntryAndNullEntry() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:child")
            public HyperResource[] getResource() {
                return new HyperResource[]{
                    new HyperResource() {
                        public String getFoo() {
                            return "foo1";
                        }
                    },
                    null
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithSubresourceArrayWithSingleEntry.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }


    @Test
    public void testWriteResourceWithSubresourceArrayWithMultiEntry() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:child")
            public HyperResource[] getResource() {
                return new HyperResource[]{
                    new HyperResource() {
                        public String getFoo() {
                            return "foo1";
                        }
                    },
                    new HyperResource() {
                        public String getFoo() {
                            return "foo2";
                        }
                    },
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithSubresourceArrayWithMultipleEntries.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }



    @Test
    public void testWriteResourceWithTypedSubresourceArrayWithMultiEntry() throws IOException, JSONException {
        class TypedResource implements HyperResource {
            private final String foo;

            @SuppressWarnings("unused")
            public String getFoo() {
                return foo;
            }

            TypedResource(String foo) {
                this.foo = foo;
            }
        }

        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            @Rel("bb:child")
            public TypedResource[] getResource() {
                return new TypedResource[]{
                    new TypedResource("foo1"),
                    new TypedResource("foo2"),
                };
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTypedSubresourceArrayWithMultipleEntries.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }


    @Test
    public void testWriteResourceWithTemplatedAction() throws IOException, JSONException {

        //We don't currently write templated actions out
        HyperResource resource = new HyperResource() {

            @SuppressWarnings("unused")
            public TemplatedAction getSomeAction() {
                return new TemplatedAction.Builder()
                    .name("some-action")
                    .href("some-href")
                    .build();
            }
        };
        writer.write(resource, null, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTemplatedAction.json");

        JSONAssert.assertEquals(
            expectedString,
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            expectedString,
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );
    }


    @Test
    public void testWriteResourceWithView() throws IOException, JSONException {
        class ViewX {

        }

        class ViewY {

        }

        @JsonView(ViewY.class)
        class TypedResource implements HyperResource {

            @JsonView(ViewX.class)
            private final String foo;

            private final String bar;

            @SuppressWarnings("unused")
            public String getFoo() {
                return foo;
            }

            @SuppressWarnings("unused")
            public String getBar() {
                return bar;
            }

            private TypedResource(
                String foo,
                String bar
            ) {
                this.foo = foo;
                this.bar = bar;
            }
        }

        HyperResource resource = new TypedResource("foo1", "bar1");

        writer.write(resource, null, outputStream);

        JSONAssert.assertEquals(
            "{foo: 'foo1', bar: 'bar1'}",
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            "{foo: 'foo1', bar: 'bar1'}",
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );


        {
            String actual = writer.writeToString(
                resource,
                null,
                null
            );
            JSONAssert.assertEquals(
                "{foo: 'foo1', bar: 'bar1'}",
                actual,
                NON_EXTENSIBLE
            );
        }


        {
            String actual = writer.writeToString(
                resource,
                null,
                ViewX.class
            );

            JSONAssert.assertEquals(
                "{foo: 'foo1'}",
                actual,
                NON_EXTENSIBLE
            );
        }



        {
            String actual = writer.writeToString(
                resource,
                null,
                ViewY.class
            );

            JSONAssert.assertEquals(
                "{bar: 'bar1'}",
                actual,
                NON_EXTENSIBLE
            );

        }


    }



    @Test
    public void testWriteResourceWithViewAndEscapesNecessary() throws IOException, JSONException {
        class ViewX {

        }

        @JsonView(ViewX.class)
        class TypedResource implements HyperResource {

            private final String foo;

            @SuppressWarnings("unused")
            public String getFoo() {
                return foo;
            }


            private TypedResource(
                String foo
            ) {
                this.foo = foo;
            }
        }

        HyperResource resource = new TypedResource("<tag attr=\"stuff\">stuff</tag>");


        outputStream = new ByteArrayOutputStream();
        writer.write(
            resource,
            null,
            outputStream
        );

        assertEquals(
            "no view no escape",
            "{\"foo\":\"<tag attr=\\\"stuff\\\">stuff</tag>\"}",
            outputStream.toString()
        );

        JSONAssert.assertEquals(
            "{foo: '<tag attr=\"stuff\">stuff</tag>'}",
            outputStream.toString(),
            NON_EXTENSIBLE
        );


        {

            String actual = writer.writeToString(
                resource,
                null,
                null
            );

            assertEquals(
                "view even when null has escape",
                "{\"foo\":\"<tag attr=\\\"stuff\\\">stuff<\\/tag>\"}",
                actual
            );

            JSONAssert.assertEquals(
                "{foo: '<tag attr=\"stuff\">stuff</tag>'}",
                actual,
                NON_EXTENSIBLE
            );
        }


        {

            String actual = writer.writeToString(
                resource,
                null,
                ViewX.class
            );


            assertEquals(
                "view even when present has escape",
                "{\"foo\":\"<tag attr=\\\"stuff\\\">stuff<\\/tag>\"}",
                actual
            );


            JSONAssert.assertEquals(
                "{foo: '<tag attr=\"stuff\">stuff</tag>'}",
                actual,
                NON_EXTENSIBLE
            );
        }

    }



    @Test
    public void testWriteResourceWithViewIgnoringCertainLinks() throws IOException, JSONException {
        class ViewX {

        }

        class TypedResource implements HyperResource {

            public Link getLinkA(){
                return new Link(
                    "bb:a",
                    "https://example.com/a"
                );
            }

            @JsonView(ViewX.class)
            public Link getLinkB(){
                return new Link(
                    "bb:b",
                    "https://example.com/b"
                );
            }
        }

        HyperResource resource = new TypedResource();

        writer.write(resource, null, outputStream);

        JSONAssert.assertEquals(
            "{_links: { 'bb:a' : {href: 'https://example.com/a'},  'bb:b' : {href: 'https://example.com/b'} }}",
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            "{_links: { 'bb:a' : {href: 'https://example.com/a'},  'bb:b' : {href: 'https://example.com/b'} }}",
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );


        {
            String actual = writer.writeToString(
                resource,
                null,
                null
            );
            JSONAssert.assertEquals(
                "{_links: { 'bb:a' : {href: 'https://example.com/a'},  'bb:b' : {href: 'https://example.com/b'} }}",
                actual,
                NON_EXTENSIBLE
            );
        }


        {
            String actual = writer.writeToString(
                resource,
                null,
                ViewX.class
            );

            JSONAssert.assertEquals(
                "bb:a must be excluded since it is not annotated with the view",
                "{_links: { 'bb:b' : {href: 'https://example.com/b'} }}",
                actual,
                NON_EXTENSIBLE
            );
        }
    }



    @Test
    public void testWriteResourceWithViewIgnoringCertainSubresrouces() throws IOException, JSONException {
        class ViewX {

        }


        @JsonView(ViewX.class)
        class Subresource implements HyperResource {
            private final String foo;

            Subresource(
                String foo
            ){

                this.foo = foo;
            }

            public String getFoo() {
                return foo;
            }
        }

        class TypedResource implements HyperResource {

            @Rel("bb:a")
            public Subresource getA(){
                return new Subresource("a");
            }

            @Rel("bb:b")
            @JsonView(ViewX.class)
            public Subresource getB(){
                return new Subresource("b");
            }
        }

        HyperResource resource = new TypedResource();

        writer.write(resource, null, outputStream);

        JSONAssert.assertEquals(
            "{_embedded: { 'bb:a' : {foo:'a'},  'bb:b' : {foo:'b'} }}",
            outputStream.toString(),
            NON_EXTENSIBLE
        );

        JSONAssert.assertEquals(
            "{_embedded: { 'bb:a' : {foo:'a'},  'bb:b' : {foo:'b'} }}",
            writer.writeToString(resource, null),
            NON_EXTENSIBLE
        );


        {
            String actual = writer.writeToString(
                resource,
                null,
                null
            );
            JSONAssert.assertEquals(
                "{_embedded: { 'bb:a' : {foo:'a'},  'bb:b' : {foo:'b'} }}",
                actual,
                NON_EXTENSIBLE
            );
        }


        {
            String actual = writer.writeToString(
                resource,
                null,
                ViewX.class
            );

            JSONAssert.assertEquals(
                "bb:a must be excluded since it is not annotated with the view",
                "{_embedded: { 'bb:b' : {foo:'b'} }}",
                actual,
                NON_EXTENSIBLE
            );
        }
    }

}
