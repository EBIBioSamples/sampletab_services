package uk.ac.ebi.fgpt.webapp.v2;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;


public class SampleDataXMLMessageConverter extends AbstractHttpMessageConverter<SampleData> {

    public SampleDataXMLMessageConverter() {
        super(new MediaType("application", "xml"));
    }

    @Override
    protected boolean supports(Class<?> cls) {
        return SampleData.class.isAssignableFrom(cls);
    }

    @Override
    protected void writeInternal(SampleData sd, HttpOutputMessage output) throws IOException,
            HttpMessageNotWritableException {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    protected SampleData readInternal(Class<? extends SampleData> cls, HttpInputMessage input) throws IOException,
            HttpMessageNotReadableException {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }
    
}
