package ca.bc.gov.hlth.iam.clientgeneration.model.csv;

import java.util.Arrays;
import java.util.Objects;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

/**
 * Custom class required to add headers and map columns in order specified in the CSV bean 
 *
 * @param <T>
 */
public class CustomColumnPositionStrategy<T> extends ColumnPositionMappingStrategy<T> {
	
    @Override
    public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
        super.generateHeader(bean);

        String [] headers = Arrays.stream(ClientCredentials.class.getDeclaredFields())
    	        .map(field -> field.getAnnotation(CsvBindByName.class))
    	        .filter(Objects::nonNull)
    	        .map(CsvBindByName::column)
    	        .toArray(String[]::new);
    	return headers;
    }
}