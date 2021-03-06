package org.hyperfit.hyperresource.controls;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode
@ToString
public class FieldSet {

    private final String name;
    private final List<Field> fields;
    
    private FieldSet(Builder builder) {
        //TODO: can this be null?  should we protect against that?  what about empty string?
        this.name = builder.name;

        //This cannot be null, but builder protects us so no check needed
        this.fields = builder.fields;
    }
    
    public String getName() {
        return name;
    }

    @NotNull
    //TODO: can we have empty field sets?
    public List<Field> getFields() {
        return fields;
    }
    
    public static class Builder {
        
        private String name;
        private List<Field> fields = new ArrayList<Field>();
        
        public FieldSet build() {
            return new FieldSet(this);
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder addField(Field field) {
            this.fields.add(field);
            return this;
        }
        
    }

}