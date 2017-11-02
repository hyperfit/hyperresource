package org.hyperfit.hyperresource.serializer.haljson.jackson2;

import static org.junit.Assert.*;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


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

    static String readResourceAsString(String resource){
        return new Scanner(HALJSONJacksonSerializerTest.class.getClassLoader().getResourceAsStream(resource), "UTF-8").useDelimiter("\\A").next();

    }

    HALJSONJacksonSerializer writer = new HALJSONJacksonSerializer();


    ByteArrayOutputStream outputStream;


    @Before
    public void setUp() throws IOException {
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
            public int getVal() {
                return 1;
            }

            ;
        };

        writer.write(resource, outputStream);

        String expectedString = "{\"val\":1}";
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithOneLinkControl() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            public Link getImage() {
                return new Link("bb:image", "some/url/to/image", "small", "PNG");
            }
        };
        writer.write(resource, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithOneLinkControl.json");

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithTwoLinkControls() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {

            public Link getImage() {
                return new Link("bb:image", "some/url/to/image", "small", "PNG");
            }

            public Link getSelf() {
                return new Link("self", "some/url/to/resource");
            }
        };
        writer.write(resource, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoLinkControls.json");

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithNullLinkControl() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            public Link getLink() {
                return null;
            }
        };
        writer.write(resource, outputStream);

        String expectedString = "{}";

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithLinkArray() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            public Link[] getProfile() {
                return new Link[]{
                    new Link("profile", "prof1"), new Link("profile", "prof2")
                };
            }
        };
        writer.write(resource, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayControl.json");

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithLinkArrayNLink() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            public Link[] getProfile() {
                return new Link[]{
                    new Link("profile", "prof1"),
                    new Link("profile", "prof2")
                };
            }

            public Link getSelf() {
                return new Link("self", "some/url/to/resource");
            }
        };
        writer.write(resource, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayNLink.json");

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithLinkArrayNull() throws IOException {
        HyperResource resource = new HyperResource() {
            public Link[] getProfile() {
                return null;
            }
        };
        writer.write(resource, outputStream);

        String expectedString = "{}";
        String actual = outputStream.toString();
        assertEquals(expectedString, actual);
    }

    @Test
    public void testWriteResourceWithProfileLink() throws IOException, JSONException {
        //we should not be treating profile any more special than any other rel
        HyperResource resource = new HyperResource() {
            public Link getProfile() {
                return new Link("profile", "prof1");
            }
        };
        writer.write(resource, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithProfileLinkIsArray.json");

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithLinkArrayWith1Entry() throws IOException, JSONException {
        //If a link is exposed via a method returning an array of links, we should
        //always serialize the rel as an array, as this is how devs indicate they want
        //an array
        HyperResource resource = new HyperResource() {
            public Link[] getDogs() {
                return new Link[]{
                    new Link("dog", "dog1")
                };
            }
        };
        writer.write(resource, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayWith1Entry.json");

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithLinkArrayWithTwoEntriesDifferentRels() throws IOException, JSONException {
        //Interesting edge case here...if you return links with different rels in an array they also
        //are forced to be serialized as an array
        //i don't forsee anyone doing this...but they might
        HyperResource resource = new HyperResource() {
            public Link[] getAnimals() {
                return new Link[]{
                    new Link("dog", "dog1"),
                    new Link("cat", "cat1")
                };
            }
        };
        writer.write(resource, outputStream);

        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithLinkArrayWithTwoEntriesDifferentRels.json");

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithListProperty() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            public List<String> getList() {
                List<String> list = new ArrayList<String>();
                list.add("foo1");
                list.add("foo2");
                return list;
            }
        };
        writer.write(resource, outputStream);

        String expectedString = "{\"list\":[\"foo1\",\"foo2\"]}";

        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithOneSubResource() throws IOException {
        HyperResource resource = new HyperResource() {
            @Rel("bb:child")
            public HyperResource getResource() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
            }
        };
        writer.write(resource, outputStream);
        String expectedString = "{\"_embedded\":{\"bb:child\":{\"foo\":\"foo\"}}}";
        String actual = outputStream.toString();
        assertEquals(expectedString, actual);
    }

    @Test
    public void testWriteResourceWithOneSubResourceNoRelAnnotation() throws IOException {
        HyperResource resource = new HyperResource() {
            public HyperResource getResource() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
            }
        };
        writer.write(resource, outputStream);
        String expectedString = "{\"_embedded\":{\"resource\":{\"foo\":\"foo\"}}}";
        String actual = outputStream.toString();
        assertEquals(expectedString, actual);
    }

    @Test
    public void testWriteResourceWithNullSubresource() throws IOException {
        HyperResource resource = new HyperResource() {
            public HyperResource getResource() {
                return null;
            }
        };
        writer.write(resource, outputStream);
        String expectedString = "{}";
        String actual = outputStream.toString();
        assertEquals(expectedString, actual);
    }

    @Test
    public void testWriteResourceWithTwoSubResourcesWithSameRel() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            @Rel("bb:children")
            public HyperResource getResource1() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
            }

            @Rel("bb:children")
            public HyperResource getResource2() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo";
                    }
                };
            }
        };
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoSubResourcesWithSameRel.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }


    @Test
    public void testWriteResourceWithTwoSameSubResourcesDifferentRels() throws IOException, JSONException {
        HyperResource sub = new HyperResource() {
            public String getFoo() {
                return "foo";
            }
        };

        HyperResource resource = new HyperResource() {
            @Rel("bb:children1")
            public HyperResource getResource1() {
                return sub;
            }

            @Rel("bb:children2")
            public HyperResource getResource2() {
                return sub;
            }
        };
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoSameSubResourcesDifferentRels.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }




    @Test
    public void testWriteResourceWithSubresouceArray() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
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
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoSubResourcesWithSameRel.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithEmptySubresouceArray() throws IOException, JSONException {
        //By default we exclude empty sub resource arrays
        HyperResource resource = new HyperResource() {

            @Rel("bb:children")
            public HyperResource[] getResource() {
                return new HyperResource[]{};
            }
        };
        writer.write(resource, outputStream);
        String expectedString = "{}";
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithNullSubresouceArray() throws IOException, JSONException {
        //nulls are ignored in general..so this is no different than an empty aray
        HyperResource resource = new HyperResource() {

            @Rel("bb:children")
            public HyperResource[] getResource() {
                return null;
            }
        };
        writer.write(resource, outputStream);
        String expectedString = "{}";
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithOneSubResourceWithOneLink() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            @Rel("bb:child")
            public HyperResource getResource() {
                return new HyperResource() {
                    public Link getLink() {
                        return new Link("rel", "some/url");
                    }
                };
            }
        };
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithOneSubResourceWithOneLink.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

    @Test
    public void testWriteResourceWithTwoDepthSubresources() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
            @Rel("bb:child1")
            public HyperResource getResource() {
                return new HyperResource() {
                    public String getFoo() {
                        return "foo£";
                    }

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
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTwoDepthSubresources.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }


    @Test
    public void testWriteResourceWithSubresourceArrayWithSingleEntry() throws IOException, JSONException {
        // a subresource returned as part of an array always should serialize as an array
        HyperResource resource = new HyperResource() {
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
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithSubresourceArrayWithSingleEntry.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }


    @Test
    public void testWriteResourceWithSubresourceArrayWithSingleEntryAndNullEntry() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
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
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithSubresourceArrayWithSingleEntry.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }


    @Test
    public void testWriteResourceWithSubresourceArrayWithMultiEntry() throws IOException, JSONException {
        HyperResource resource = new HyperResource() {
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
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithSubresourceArrayWithMultipleEntries.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }



    @Test
    public void testWriteResourceWithTypedSubresourceArrayWithMultiEntry() throws IOException, JSONException {
        class TypedResource implements HyperResource {
            final String foo;

            public String getFoo() {
                return foo;
            }

            TypedResource(String foo) {
                this.foo = foo;
            }
        }

        HyperResource resource = new HyperResource() {
            @Rel("bb:child")
            public TypedResource[] getResource() {
                return new TypedResource[]{
                    new TypedResource("foo1"),
                    new TypedResource("foo2"),
                };
            }
        };
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTypedSubresourceArrayWithMultipleEntries.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }


    @Test
    public void testWriteResourceWithTemplatedAction() throws IOException, JSONException {

        //We don't currently write templated actions out
        HyperResource resource = new HyperResource() {
            public TemplatedAction getSomeAction() {
                return new TemplatedAction.Builder()
                    .name("some-action")
                    .href("some-href")
                    .build();
            }
        };
        writer.write(resource, outputStream);
        String expectedString = readResourceAsString("hal-serializer-tests/ResourceWithTemplatedAction.json");
        String actual = outputStream.toString();
        JSONAssert.assertEquals(expectedString, actual, NON_EXTENSIBLE);
    }

}