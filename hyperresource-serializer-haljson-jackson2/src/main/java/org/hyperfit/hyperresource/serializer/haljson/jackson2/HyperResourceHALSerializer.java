package org.hyperfit.hyperresource.serializer.haljson.jackson2;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hyperfit.hyperresource.HyperResource;
import org.hyperfit.hyperresource.annotation.Rel;

import org.hyperfit.hyperresource.controls.Link;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

/**
 * Serializes {@code HyperResource} objects into HAL+JSON.
 *
 */
class HyperResourceHALSerializer extends BeanSerializerBase {

    private static final String HAL_KEY_EMBEDDED = "_embedded";
    private static final String HAL_KEY_LINKS = "_links";

    public HyperResourceHALSerializer(BeanSerializerBase source) {
        super(source);
    }

    @Override
    public void serialize(Object bean, JsonGenerator gen, SerializerProvider provider) throws IOException {
        //This is stolen from https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/ser/BeanSerializer.java
        if (_objectIdWriter != null) {
            gen.setCurrentValue(bean); // [databind#631]
            _serializeWithObjectId(bean, gen, provider, true);
            return;
        }
        gen.writeStartObject();
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.setCurrentValue(bean);
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, gen, provider);
        } else {
            serializeFields(bean, gen, provider);
        }

        //Here's the special part and why we override and copy pasta this stuff
        this.serializeControls(bean, gen, provider);

        gen.writeEndObject();
    }



    private void serializeControls(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        writeLinks(bean, jgen);

        writeEmbeddedResources(bean, jgen, provider);

    }

    private void writeLinks(
        Object bean,
        JsonGenerator jgen
    ) {

        HashSet<String> forceArrayRels = new HashSet<>();

        // Step 1: Group all links by rel.
        Map<String, List<Link>> linksByRel = Arrays.stream(_props)
            .filter(
                p -> Link.class.isAssignableFrom(p.getPropertyType()) || Link[].class.isAssignableFrom(p.getPropertyType())
            )
            .map(p -> {
                try {
                    return p.get(bean);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .filter(Objects::nonNull)
            .flatMap(o -> {

                if (o instanceof Link) {
                    return Stream.of((Link)o);
                }

                if (o instanceof Link[]) {
                    return Stream.of((Link[])o)
                        //this is a bit ugly and side affect based, but what's another way?
                        .peek(l -> forceArrayRels.add(l.getRel()));
                }

                return Stream.empty();
            })
            .collect(Collectors.groupingBy(
                Link::getRel,
                Collectors.toList()
            ));


        // Step 2: Serialize all links grouped by rel.
        if (linksByRel.size() > 0) {
            try {
                jgen.writeFieldName(HAL_KEY_LINKS);
                jgen.writeStartObject();

                for(Map.Entry<String,List<Link>> e : linksByRel.entrySet()){

                    // Writes rel.
                    jgen.writeFieldName(e.getKey());

                    boolean writingAsArray = e.getValue().size() > 1 || forceArrayRels.contains(e.getKey());

                    if (writingAsArray) {
                        jgen.writeStartArray();
                    }

                    for(Link l : e.getValue()){
                        //TODO: maybe we should just have a serializer for the link type, then the output code
                        //could be de-duped and shared
                        jgen.writeStartObject();
                        jgen.writeStringField("href", l.getHref());

                        if (!isEmpty(l.getName())) {
                            jgen.writeStringField("name", l.getName());
                        }
                        if (!isEmpty(l.getType())) {
                            jgen.writeStringField("type", l.getType());
                        }

                        //TODO: should this actually be based on just null? ie should we let "" values
                        if (!isEmpty(l.getTitle())) {
                            jgen.writeStringField("title", l.getTitle());
                        }
                        jgen.writeEndObject();
                    }
                    if (writingAsArray) {
                        jgen.writeEndArray();
                    }
                }
                jgen.writeEndObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //Since resources don't have rels, we need to track the rel through the stream
    private static class RelJar<T> {
        final String rel;
        final T content;

        private RelJar(String rel, T content) {
            this.rel = rel;
            this.content = content;
        }
    }

    private void writeEmbeddedResources(
        Object bean,
        JsonGenerator jgen,
        SerializerProvider provider
    ) {
        HashSet<String> forceArrayRels = new HashSet<>();

        // 1. Group hyper resources by rel.
        Map<String, List<HyperResource>> resourcesByRel = Arrays.stream(_props)
            .filter(
                p -> HyperResource.class.isAssignableFrom(p.getPropertyType()) || HyperResource[].class.isAssignableFrom(p.getPropertyType())
            )
            .map(p -> {
                Rel rel = p.getAnnotation(Rel.class);
                // Use rel annotation if present, otherwise use property name.
                String relName = (rel != null ? rel.value() : p.getName());

                try {
                    return new RelJar<>(relName, p.get(bean));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            //We don't serialize null sub resources
            //TODO: there probably should be a way to force embedded array even if there are no contents...
            .filter(j -> j.content != null)
            .flatMap(j -> {

                if (j.content instanceof HyperResource) {
                    return Stream.of(new RelJar<>(j.rel, (HyperResource)j.content));
                }

                if (j.content instanceof HyperResource[]) {
                    //If they are returning an array, then we force it to be output as an array
                    forceArrayRels.add(j.rel);

                    return Stream.of((HyperResource[])j.content)
                        //We don't serialize a null resource contained in an array
                        .filter(Objects::nonNull)
                        .map(r -> new RelJar<>(j.rel, r));
                }

                return Stream.empty();
            })
            .collect(Collectors.groupingBy(
                j -> j.rel,
                Collectors.mapping(
                    j -> j.content,
                    Collectors.toList()
                )
            ));


        // 2. Write hyper resource recursively.
        if (resourcesByRel.size() > 0) {
            try {
                jgen.writeFieldName(HAL_KEY_EMBEDDED);
                jgen.writeStartObject();

                for(Map.Entry<String,List<HyperResource>> e : resourcesByRel.entrySet()) {

                    jgen.writeFieldName(e.getKey());

                    List<HyperResource> v = e.getValue();

                    boolean writingAsArray = e.getValue().size() > 1 || forceArrayRels.contains(e.getKey());

                    if (writingAsArray) {
                        jgen.writeStartArray();
                    }

                    for (HyperResource p : v) {
                        provider.defaultSerializeValue(p, jgen);
                    }

                    if (writingAsArray) {
                        jgen.writeEndArray();
                    }
                }

                jgen.writeEndObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public HyperResourceHALSerializer(
        HyperResourceHALSerializer source,
        ObjectIdWriter objectIdWriter
    ) {
        super(source, objectIdWriter);
    }

    public HyperResourceHALSerializer(
        HyperResourceHALSerializer source,
        String[] toIgnore
    ) {
        super(source, toIgnore);
    }

    public HyperResourceHALSerializer(
        HyperResourceHALSerializer source,
        Set<String> toIgnore
    ) {
        super(source, toIgnore);
    }

    public HyperResourceHALSerializer(
        HyperResourceHALSerializer source,
        ObjectIdWriter objectIdWriter,
        Object filterId
    ) {
        super(source, objectIdWriter, filterId);
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(
        ObjectIdWriter objectIdWriter
    ) {
        return new HyperResourceHALSerializer(this, objectIdWriter);
    }

    @Override
    protected BeanSerializerBase withIgnorals(
        Set<String> toIgnore
    ) {
        return new HyperResourceHALSerializer(this, toIgnore);
    }

    @Override
    protected BeanSerializerBase withIgnorals(
        String[] toIgnore
    ) {
        return new HyperResourceHALSerializer(this, toIgnore);
    }

    @Override
    protected BeanSerializerBase asArraySerializer() {
        /* Can not:
         *
         * - have Object Id (may be allowed in future)
         * - have "any getter"
         * - have per-property filters
         */
        if ((_objectIdWriter == null)
            && (_anyGetterWriter == null)
            && (_propertyFilterId == null)
            ) {
            return new BeanAsArraySerializer(this);
        }
        // already is one, so:
        return this;
    }

    @Override
    public BeanSerializerBase withFilterId(Object filterId) {
        return new HyperResourceHALSerializer(this, _objectIdWriter, filterId);
    }


    private static boolean isEmpty(String str){
        return str == null || str.trim().equals("");
    }
}
