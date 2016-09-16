package uk.ac.ebi.subs.submissiongeneration.ArrayExpressModel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.ac.ebi.subs.submissiongeneration.OptionalArrayDeserializer;

import java.util.ArrayList;
import java.util.List;


public class AeFile {

    String location,name,extension,size,lastmodified,url;

    @JsonDeserialize(using = StringListDeserializer.class)
    List<String> kind = new ArrayList<>();

    public static class StringListDeserializer extends OptionalArrayDeserializer<String> {
        protected StringListDeserializer() {
            super(String.class);
        }
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(String lastmodified) {
        this.lastmodified = lastmodified;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getKind() {
        return kind;
    }

    public void setKind(List<String> kind) {
        this.kind = kind;
    }
}
