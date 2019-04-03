package org.hyperfit.hyperresource.serializer.handlebars;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.jknack.handlebars.Context;
import org.hyperfit.hyperresource.HyperResource;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.github.jknack.handlebars.Handlebars;
import test.TestUtils;

import static org.hyperfit.hyperresource.serializer.handlebars.HandlebarsSerializer.HBS_PATH_TO_CONTENT_LANGUAGE;
import static org.hyperfit.hyperresource.serializer.handlebars.HandlebarsSerializer.HBS_PATH_TO_LOCALE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests the HTMLHandlebarsSerializer
 * This is a lot easier than the tests for HALJSONJacksonSerializer since
 * almost all the logic is passed over to handlebars
 *
 * If we start developing handlebars extensions this class will start to grow
 */
public class HandlebarsSerializerTest {


    @Mock
    Handlebars mockHandlebars;

    @Mock
    TemplateLoader mockTemplateLoader;


    @Mock
    OutputStream mockOutputStream;

    @Mock
    Template mockTemplate;

    private HandlebarsSerializer subject;

    private static class HBSContextMatcher extends ArgumentMatcher<Context> {

        private final Context matchee;

        private HBSContextMatcher(Context matchee) {
            this.matchee = matchee;
        }


        @Override
        public boolean matches(Object argument) {
            if(!(argument instanceof Context)){
                return false;
            }

            Context candidate = (Context)argument;


            if(!matchee.model().equals(candidate.model())){
                return false;
            }

            //Context doesnt' seem to expose a list of all keys...or anyway to enumerate the paths of data
            //could access the private members i suppose...but we'll just check the list of KNOWN keys
            String[] knownDataKeys = new String[]{
                "_locale",
                "_contentLanguage"
            };

            for(String key: knownDataKeys){

                if(!Objects.equals(matchee.get(key), candidate.get(key))){
                    return false;
                }
            }

            return true;
        }

    }

    private static Context matchesContext(
        Context c
    ){
        return argThat(new HBSContextMatcher(c));
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mockHandlebars.getLoader())
            .thenReturn(mockTemplateLoader);

        subject = new HandlebarsSerializer(mockHandlebars, "something/nothing");
    }

    @Test
    public void testCanWriteSourceAtReturnsNull() throws IOException {


        assertFalse(subject.canWrite(HyperResource.class));

        assertFalse(subject.canWrite(new HyperResource(){}.getClass()));

        TemplateSource mockSource = mock(TemplateSource.class);

        when(mockTemplateLoader.sourceAt("HyperResource"))
            .thenReturn(mockSource);

        assertFalse("the previous failure should be cached", subject.canWrite(HyperResource.class));

        //Only should have been 1 call to sourceAt since the frist resulted in failure
        verify(mockTemplateLoader, times(1)).sourceAt("HyperResource");
    }

    @Test
    public void testCanWriteSourceAtThrows() throws IOException {


        assertFalse(subject.canWrite(HyperResource.class));

        assertFalse(subject.canWrite(new HyperResource(){}.getClass()));

        when(mockTemplateLoader.sourceAt("HyperResource"))
            .thenThrow(new IOException());

        assertFalse("the previous failure should be cached", subject.canWrite(HyperResource.class));

        //Only should have been 1 call to sourceAt since the frist resulted in failure
        verify(mockTemplateLoader, times(1)).sourceAt("HyperResource");
    }


    @Test
    public void testRequiredConstructorParams(){
        try{
            new HandlebarsSerializer(null);
            fail("expected exception not thrown");
        }catch(IllegalArgumentException e){
            assertThat(e.getMessage(), containsString("handlebars can not be null"));
        }


        try{
            new HandlebarsSerializer(mockHandlebars);
            fail("expected exception not thrown");
        }catch(IllegalArgumentException e){
            assertThat(e.getMessage(), containsString("handledContentTypes can not be null or empty"));
        }

        try{
            new HandlebarsSerializer(
                mockHandlebars,
                (String[])null
            );
            fail("expected exception not thrown");
        }catch(IllegalArgumentException e){
            assertThat(e.getMessage(), containsString("handledContentTypes can not be null or empty"));
        }

        try{
            //noinspection RedundantArrayCreation
            new HandlebarsSerializer(
                mockHandlebars,
                new String[0]
            );
            fail("expected exception not thrown");
        }catch(IllegalArgumentException e){
            assertThat(e.getMessage(), containsString("handledContentTypes can not be null or empty"));
        }
    }

    @Test
    public void testGetContentTypes(){
        assertThat(subject.getContentTypes(), Matchers.contains("something/nothing"));

        String newType = TestUtils.uniqueString();
        subject = new HandlebarsSerializer(mockHandlebars, newType);

        assertThat(subject.getContentTypes(), Matchers.contains(newType));

        String newType2 = TestUtils.uniqueString();
        subject = new HandlebarsSerializer(mockHandlebars, newType2, newType);

        assertThat(subject.getContentTypes(), Matchers.contains(newType2, newType));
    }


    private class FakeHyperResource implements HyperResource {

    }



    @Test
    public void testEnsureStreamIsNotClosedOnException() throws IOException {
        //This is really here to test writer before it gets sets and get through that finally block for coverage
        //mostly just test coverage vanity for now

        FakeHyperResource fakeResource = new FakeHyperResource();

        when(mockHandlebars.compile("FakeHyperResource"))
            .thenReturn(mockTemplate);

        RuntimeException fakeException = new RuntimeException();

        doThrow(
            fakeException
        )
        .when(mockTemplate).apply(
            matchesContext(Context.newContext(fakeResource)),
            Mockito.any(Writer.class)
        );

        try{
            subject.write(fakeResource, null, mockOutputStream);
            fail("expected exception not thrown");
        }catch(Exception e){
            assertSame(fakeException, e);
        }


        verify(mockOutputStream, never()).close();

    }

    @Test
    public void testWriteNoLocale() throws IOException {

        AtomicBoolean flushCalled = new AtomicBoolean(false);


        FakeHyperResource fakeResource = new FakeHyperResource();
        ByteArrayOutputStream fakeOutputStream = new ByteArrayOutputStream(){

            @Override
            public void close() {
                fail("close should not be called");
            }


            @Override
            public void flush() {
                flushCalled.set(true);
            }
        };

        when(mockHandlebars.compile("FakeHyperResource"))
            .thenReturn(mockTemplate);

        final String fakeResult = TestUtils.uniqueString();

        doAnswer(
            i -> {
                ((Writer) i.getArguments()[1]).write(fakeResult);
                return null;
            }
        )
        .when(mockTemplate).apply(
            matchesContext(Context.newContext(fakeResource)),
            Mockito.any(Writer.class)
        );

        subject.write(fakeResource, null, fakeOutputStream);

        assertEquals(
            "The result written to writer must make it to the passed in output stream",
            fakeResult,
            fakeOutputStream.toString()
        );

        assertTrue("flush must be called", flushCalled.get());

        //This feels like cheating

        when(mockTemplate.apply(
            matchesContext(Context.newContext(fakeResource))
        ))
        .thenReturn(fakeResult);

        assertEquals(
            "The result written to writer must make it to the passed in output stream",
            fakeResult,
            subject.writeToString(fakeResource, null)
        );

    }



    @Test
    public void testWriteWithLocale() throws IOException {

        AtomicBoolean flushCalled = new AtomicBoolean(false);

        //TODO: randomize
        Locale fakeLocale = new Locale("pt", "BR");

        FakeHyperResource fakeResource = new FakeHyperResource();
        ByteArrayOutputStream fakeOutputStream = new ByteArrayOutputStream(){

            @Override
            public void close() {
                fail("close should not be called");
            }


            @Override
            public void flush() {
                flushCalled.set(true);
            }
        };

        when(mockHandlebars.compile("FakeHyperResource"))
            .thenReturn(mockTemplate);

        final String fakeResult = TestUtils.uniqueString();

        Context expectedContext = Context.newBuilder(fakeResource)
            .combine(
                HBS_PATH_TO_LOCALE,
                fakeLocale
            )
            .combine(
                HBS_PATH_TO_CONTENT_LANGUAGE,
                fakeLocale.toLanguageTag()
            )
            .build();

        doAnswer(
            i -> {
                ((Writer) i.getArguments()[1]).write(fakeResult);
                return null;
            }
        )
            .when(mockTemplate).apply(
            matchesContext(expectedContext),
            Mockito.any(Writer.class)
        );

        subject.write(fakeResource, fakeLocale, fakeOutputStream);

        assertEquals(
            "The result written to writer must make it to the passed in output stream",
            fakeResult,
            fakeOutputStream.toString()
        );

        assertTrue("flush must be called", flushCalled.get());

        //This feels like cheating

        when(mockTemplate.apply(
            matchesContext(expectedContext)
        ))
        .thenReturn(fakeResult);

        assertEquals(
            "The result written to writer must make it to the passed in output stream",
            fakeResult,
            subject.writeToString(fakeResource, fakeLocale)
        );

    }


    @Test
    public void testCanWriteRespectsHBSTemplateAnnotation() throws IOException {

        @HBSTemplate("XYZ")
        class SomeResource implements HyperResource {

        }

        when(mockTemplateLoader.sourceAt("XYZ"))
            .thenReturn(mock(TemplateSource.class));

        assertTrue(
            subject.canWrite(
                SomeResource.class
            )
        );
    }


    @Test
    public void testWriteRespectsHBSTemplateAnnotation() throws IOException {

        @HBSTemplate("XYZ")
        class SomeResource implements HyperResource {

        }

        AtomicBoolean flushCalled = new AtomicBoolean(false);

        SomeResource fakeResource = new SomeResource();
        ByteArrayOutputStream fakeOutputStream = new ByteArrayOutputStream(){

            @Override
            public void close() {
                fail("close should not be called");
            }


            @Override
            public void flush() {
                flushCalled.set(true);
            }
        };

        when(mockHandlebars.compile("XYZ"))
            .thenReturn(mockTemplate);

        final String fakeResult = TestUtils.uniqueString();

        doAnswer(
            i -> {
                ((Writer) i.getArguments()[1]).write(fakeResult);
                return null;
            }
        ).when(mockTemplate).apply(
            matchesContext(Context.newContext(fakeResource)),
            Mockito.any(Writer.class)
        );

        subject.write(fakeResource, null, fakeOutputStream);

        assertEquals(
            "The result written to writer must make it to the passed in output stream",
            fakeResult,
            fakeOutputStream.toString()
        );

        assertTrue("flush must be called", flushCalled.get());

        //This feels like cheating

        when(mockTemplate.apply(
            matchesContext(Context.newContext(fakeResource))
        ))
        .thenReturn(fakeResult);

        assertEquals(
            "The result written to writer must make it to the passed in output stream",
            fakeResult,
            subject.writeToString(fakeResource, null)
        );

    }



    @Test
    public void verifyContentTypesIsUnmodifiableList(){
        String[] types = new String[]{
            "something/nothing",
            TestUtils.uniqueString()
        };

        subject = new HandlebarsSerializer(
            mockHandlebars,
            types
        );

        List<String> actual = subject.getContentTypes();

        try{
            actual.set(
                0,
                TestUtils.uniqueString()
            );
            fail("Expected exception not thrown");
        } catch (UnsupportedOperationException e){

        }

    }

    @Test
    public void testWriteAsStringWithViewPassesThrough() throws IOException {

        String fakeResult = TestUtils.uniqueString();
        HyperResource fakeResource = new HyperResource() {};
        Locale fakeLocale = Locale.CANADA;

        subject = new HandlebarsSerializer(mockHandlebars, "whatever"){

            @Override
            public String writeToString(
                HyperResource resource,
                Locale locale
            ) {
                assertSame(
                    fakeResource,
                    resource
                );

                assertSame(
                    fakeLocale,
                    locale
                );

                return fakeResult;
            }
        };

        assertEquals(
            fakeResult,
            subject.writeToString(
                fakeResource,
                fakeLocale,
                HandlebarsSerializerTest.class
            )
        );

    }

}