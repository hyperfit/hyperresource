package org.hyperfit.hyperresource.serializer.handlebars.helpers;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.hyperfit.hyperresource.HyperResource;
import org.hyperfit.hyperresource.serializer.HyperResourceSerializer;

import java.io.IOException;
import java.util.*;

/**
 * <p>
 * Basic usage:
 * </p>
 *
 * <pre>
 *  Handlebars hbs = new Handlebars();
 *
 *  hbs.registerHelper(SerializeAsBlockHelper.HELPER_MARKUP_TAG_NAME, new SerializeAsBlockHelper(new HALJSONJacksonSerializer()));
 *
 *  ...
 *  {{serializeAs HyperResource type="application/hal+json" view="com.example.views.ViewX" }}
 * </pre>
 */
public class SerializeAsHelper implements Helper<HyperResource> {

    public static final String HELPER_MARKUP_TAG_NAME = "serializeAs";

    private final Map<String, HyperResourceSerializer> serializersMap;


    public SerializeAsHelper(
        HyperResourceSerializer... serializers
    ) {

        serializersMap = new HashMap<>();
        for (HyperResourceSerializer s : serializers) {
            for (String contentType : s.getContentTypes()) {
                serializersMap.put(contentType, s);
            }
        }
    }


    @Override
    public Object apply(
        HyperResource resource,
        Options options
    ) throws IOException {

        if(resource == null){
            return null;
        }


        String type = options.hash("type");
        if(type == null){
            throw new IOException("type must be specified");
        }

        //TODO: content negotiation some day
        HyperResourceSerializer serializer = serializersMap.get(type);


        if (serializer == null) {
            throw new IOException("no serializer can handle requested type [" + type + "]");
        }

        if(!serializer.canWrite(resource.getClass())){
            throw new IOException("Serializer [" + serializer.getClass().getName() + "] for type [" + type + "] can NOT handle HyperResource type [" + resource.getClass().getSimpleName() + "]");
        }


        Class<?> viewClazz = null;

        String view = options.hash("view");
        if (view != null && view.trim().length() > 0){
            try {
                viewClazz = Class.forName(view);
            } catch (ClassNotFoundException e) {
                throw new IOException(
                    "Could not resolve view [" + view + "] to a java class",
                    e
                );
            }

        }

        return serializer.writeToString(
            resource,
            //TODO: support locale
            null,
            viewClazz
        );

    }


}
