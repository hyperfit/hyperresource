package org.hyperfit.hyperresource.serializer;

import org.hyperfit.hyperresource.HyperResource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 * Defines the contract that any serializer of a HyperResource must implement in order to function in the Hyperfit HyperResource ecosystem
 */
public interface HyperResourceSerializer {

    /**
     * @return a list of content types that this serializer supports.  EG text/html application/hal+json
     */
    List<String> getContentTypes();

    /**
     * determines if this serializer can serialize the HyperResource searching for a serializer
     * @param resourceClass the class of the HyperResource that is searching for a serializer
     * @return true if this serializer can serialize the given resource, false otherwise
     */
    boolean canWrite(Class<? extends HyperResource> resourceClass);

    /**
     * Writes the HyperResource in serialized form to the given output stream for the given locale
     * @param resource the resource to be serialized
     * @param locale the locale to use while serializing
     * @param output the output stream the serialized form for the resource is written
     * @throws IOException when serialization can not take place
     */
    void write(
        HyperResource resource,
        Locale locale,
        OutputStream output
    ) throws IOException;

    /**
     * Writes the HyperResource in serialized form for the given locale using the given perspective of viewing the resource as a string.
     * Different HyperResourceSerializers interpret the view differently
     * @param resource the resource to be serialized
     * @param locale the locale to use while serializing
     * @param resourceView the perspective for which the serializer views the resource
     * @throws IOException when serialization can not take place
     */
    String writeToString(
        HyperResource resource,
        Locale locale,
        Class<?> resourceView
    ) throws IOException;


    /**
     * Writes the HyperResource in serialized form for the given locale as a string
     * @param resource the resource to be serialized
     * @param locale the locale to use while serializing
     * @return string representation of the serialized format of the HyperResource
     * @throws IOException when serialization can not take place
     */
    String writeToString(
        HyperResource resource,
        Locale locale
    ) throws IOException;
}
