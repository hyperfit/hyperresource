package org.hyperfit.hyperresource.serializer.handlebars.helpers;

import com.github.jknack.handlebars.*;
import org.hyperfit.hyperresource.HyperResource;
import org.hyperfit.hyperresource.serializer.HyperResourceSerializer;
import org.junit.Test;
import test.TestUtils;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.hyperfit.hyperresource.serializer.handlebars.helpers.SerializeAsHelper.HELPER_MARKUP_TAG_NAME;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class SerializeAsHelperTest {


    @Test
    public void testNullContext() throws IOException {

        Handlebars handlebars = new Handlebars();

        handlebars.registerHelper(HELPER_MARKUP_TAG_NAME, new SerializeAsHelper());

        Template template = handlebars.compileInline("{{serializeAs thing}}");

        assertEquals(
            "null context always returns empty",
            "",
            template.apply(null)
        );

    }

    @Test
    public void testTypeRequired() throws IOException {

        Handlebars handlebars = new Handlebars();

        handlebars.registerHelper(HELPER_MARKUP_TAG_NAME, new SerializeAsHelper());

        Template template = handlebars.compileInline("{{serializeAs this }}");

        try{
            template.apply(new DummyHyperResource());
            fail("expected exception not thrown");
        } catch(HandlebarsException e){
            assertThat(
                e.getCause().getMessage(),
                containsString("type must be specified")
            );
        }
    }


    @Test
    public void testNoSerializersForType() throws IOException {

        Handlebars handlebars = new Handlebars();

        handlebars.registerHelper(HELPER_MARKUP_TAG_NAME, new SerializeAsHelper());

        Template template = handlebars.compileInline("{{serializeAs this type=\"application/hal+json\" }}");

        try{
            template.apply(new DummyHyperResource());
            fail("expected exception not thrown");
        } catch(HandlebarsException e){
            assertThat(
                e.getCause().getMessage(),
                containsString("no serializer can handle requested type [application/hal+json]")
            );
        }
    }


    @Test
    public void testSerializersCantHandleResourceType() throws IOException {

        String fakeContentType = TestUtils.uniqueString();

        HyperResourceSerializer serializer = new SerializerFixture(fakeContentType){

            @Override
            public boolean canWrite(
                Class<? extends HyperResource> resourceClass
            ) {
                return resourceClass != DummyHyperResource.class;

            }

            @Override
            public String writeToString(HyperResource resource, Locale locale, Class<?> resourceView) {
                fail("must not be called since it can NOT write");
                return null;
            }


        };

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper(
            HELPER_MARKUP_TAG_NAME,
            new SerializeAsHelper(
                serializer
            )
        );

        Template template = handlebars.compileInline("{{serializeAs this type=\"" + fakeContentType + "\" }}");

        try{
            template.apply(new DummyHyperResource());
            fail("expected exception not thrown");
        } catch(HandlebarsException e){
            assertThat(
                e.getCause().getMessage(),
                containsString("Serializer [org.hyperfit.hyperresource.serializer.handlebars.helpers.SerializeAsHelperTest$1] for type [" + fakeContentType + "] can NOT handle HyperResource type [DummyHyperResource]")
            );
        }
    }



    @Test
    public void testSerializeWithNullView() throws IOException {

        String fakeContentType = TestUtils.uniqueString();
        String fakeResult = TestUtils.uniqueString();
        DummyHyperResource fakeResource = new DummyHyperResource();

        HyperResourceSerializer serializer = new SerializerFixture(fakeContentType){

            @Override
            public String writeToString(
                HyperResource resource,
                Locale locale,
                Class<?> resourceView
            ) {
                assertNull(
                    "view must resolve to null form empty helper hash",
                    resourceView
                );

                assertSame(
                    fakeResource,
                    resource
                );

                assertNull(
                    "locale must not be passed yet",
                    locale
                );

                return fakeResult;

            }

        };

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper(
            HELPER_MARKUP_TAG_NAME,
            new SerializeAsHelper(
                serializer
            )
        );

        Template template = handlebars.compileInline("{{serializeAs this type=\"" + fakeContentType + "\" }}");

        assertEquals(
            fakeResult,
            template.apply(fakeResource)
        );

    }


    @Test
    public void testSerializeWithEmptyView() throws IOException {

        String fakeContentType = TestUtils.uniqueString();
        String fakeResult = TestUtils.uniqueString();
        DummyHyperResource fakeResource = new DummyHyperResource();

        HyperResourceSerializer serializer = new SerializerFixture(fakeContentType){

            @Override
            public String writeToString(
                HyperResource resource,
                Locale locale,
                Class<?> resourceView
            ) {
                assertNull(
                    "view must resolve to null form empty helper hash",
                    resourceView
                );

                assertSame(
                    fakeResource,
                    resource
                );

                assertNull(
                    "locale must not be passed yet",
                    locale
                );

                return fakeResult;

            }

        };

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper(
            HELPER_MARKUP_TAG_NAME,
            new SerializeAsHelper(
                serializer
            )
        );

        {
            Template template = handlebars.compileInline("{{serializeAs this type=\"" + fakeContentType + "\" view=\"  \"}}");

            assertEquals(
                fakeResult,
                template.apply(fakeResource)
            );
        }


        {
            Template template = handlebars.compileInline("{{serializeAs this type=\"" + fakeContentType + "\" view=\"\"}}");

            assertEquals(
                fakeResult,
                template.apply(fakeResource)
            );
        }

    }

    @Test
    public void testSerializeWithViewThatCannotResolveToClass() throws IOException {


        String fakeContentType = TestUtils.uniqueString();
        String fakeResult = TestUtils.uniqueString();

        DummyHyperResource fakeResource = new DummyHyperResource();

        HyperResourceSerializer serializer = new SerializerFixture(fakeContentType){

            @Override
            public String writeToString(
                HyperResource resource,
                Locale locale,
                Class<?> resourceView
            ) {

                fail("must not be called");
                return null;

            }

        };

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper(
            HELPER_MARKUP_TAG_NAME,
            new SerializeAsHelper(
                serializer
            )
        );


        Template template = handlebars.compileInline("{{serializeAs this type=\"" + fakeContentType + "\" view=\"java.lang.NEVERATHINGObject\"}}");

        try{
            template.apply(new DummyHyperResource());
            fail("expected exception not thrown");
        } catch(HandlebarsException e){
            assertThat(
                e.getCause().getMessage(),
                containsString("Could not resolve view [java.lang.NEVERATHINGObject] to a java class")
            );
        }



    }


    @Test
    public void testSerializeWithSpecifiedView() throws IOException {


        String fakeContentType = TestUtils.uniqueString();
        String fakeResult = TestUtils.uniqueString();

        DummyHyperResource fakeResource = new DummyHyperResource();

        HyperResourceSerializer serializer = new SerializerFixture(fakeContentType){

            @Override
            public String writeToString(
                HyperResource resource,
                Locale locale,
                Class<?> resourceView
            ) {

                assertSame(
                    "view must match from helper hash",
                    Object.class,
                    resourceView
                );

                assertSame(
                    fakeResource,
                    resource
                );

                assertNull(
                    "locale must not be passed yet",
                    locale
                );


                return fakeResult;

            }

        };

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper(
            HELPER_MARKUP_TAG_NAME,
            new SerializeAsHelper(
                serializer
            )
        );


        Template template = handlebars.compileInline("{{serializeAs this type=\"" + fakeContentType + "\" view=\"java.lang.Object\"}}");

        assertEquals(
            fakeResult,
            template.apply(fakeResource)
        );


    }


    @Test
    public void testSerializeWithNestedContext() throws IOException {


        String fakeContentType = TestUtils.uniqueString();
        String fakeResult = TestUtils.uniqueString();

        DummyComplexHyperResource fakeResource = new DummyComplexHyperResource(
            new DummyHyperResource()
        );

        HyperResourceSerializer serializer = new SerializerFixture(fakeContentType){


            @Override
            public String writeToString(
                HyperResource resource,
                Locale locale,
                Class<?> resourceView
            ) {
                assertSame(
                    "view must match from helper hash",
                    Object.class,
                    resourceView
                );

                assertSame(
                    fakeResource.getSubresource(),
                    resource
                );

                assertNull(
                    "locale must not be passed yet",
                    locale
                );


                return fakeResult;
            }
        };

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper(
            HELPER_MARKUP_TAG_NAME,
            new SerializeAsHelper(
                serializer
            )
        );


        Template template = handlebars.compileInline("{{serializeAs this.subresource type=\"" + fakeContentType + "\" view=\"java.lang.Object\"}}");

        assertEquals(
            fakeResult,
            template.apply(fakeResource)
        );

    }


    static abstract class SerializerFixture implements HyperResourceSerializer {
        private final List<String> contentTypes;

        SerializerFixture(
            String contentType
        ){
            contentTypes = Collections.singletonList(contentType);
        }


        @Override
        public List<String> getContentTypes() {
            return contentTypes;
        }

        @Override
        public boolean canWrite(Class<? extends HyperResource> resourceClass) {
            return true;
        }

        @Override
        public void write(HyperResource resource, Locale locale, OutputStream output) {
            fail("write must not be called by helper");
        }



        @Override
        public String writeToString(HyperResource resource, Locale locale) {
            fail("writeToString must not be called by helper without view");
            return null;
        }
    }


    private static class DummyHyperResource implements HyperResource{


    }

    private static class DummyComplexHyperResource implements HyperResource{


        private HyperResource resource;

        public DummyComplexHyperResource(HyperResource resource) {
            this.resource = resource;
        }

        public HyperResource getSubresource() {
            return resource;
        }

    }

}
